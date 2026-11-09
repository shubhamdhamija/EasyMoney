"""House Periodic Transaction Report (PTR) tracker.

Reads the House Clerk's public financial-disclosure index and, when `pdftotext`
(poppler) is available, the individual PTR PDFs. This is public STOCK Act data:
it is filed up to 45 days after a trade and amounts are ranges. It is not
inside information and is informational only.
"""

from __future__ import annotations

import datetime as _dt
import io
import os
import re
import shutil
import subprocess
import time
import xml.etree.ElementTree as ET
import zipfile
from typing import Any

INDEX_URL = "https://disclosures-clerk.house.gov/public_disc/financial-pdfs/%d%s.zip"
PTR_URL = "https://disclosures-clerk.house.gov/public_disc/ptr-pdfs/%d/%s.pdf"
UA = "EasyMoney-congress-tracker/1.0"
MAX_DOWNLOAD_BYTES = 15 * 1024 * 1024
INDEX_TTL_SECONDS = 3600
LATE_DAYS = 45

TXN_TYPES = {"P": "purchase", "S": "sale", "S (partial)": "partial sale", "E": "exchange"}
OWNERS = {"SP": "spouse", "JT": "joint", "DC": "dependent child", "": "self"}


class CongressDataError(Exception):
    def __init__(self, code: str, message: str):
        super().__init__(message)
        self.code = code
        self.message = message


def iso(mdy: str | None) -> str | None:
    m = re.match(r"^\s*(\d{1,2})/(\d{1,2})/(\d{2,4})\s*$", mdy or "")
    if not m:
        return None
    month, day, year = int(m.group(1)), int(m.group(2)), int(m.group(3))
    if year < 100:
        year += 2000
    try:
        return _dt.date(year, month, day).isoformat()
    except ValueError:
        return None


def _download(url: str, timeout: int = 30) -> bytes:
    # httpx ships with certifi, so TLS is verified even where the system Python has no CA bundle.
    import httpx

    try:
        with httpx.Client(timeout=timeout, headers={"User-Agent": UA}, follow_redirects=True) as client:
            with client.stream("GET", url) as resp:
                if resp.status_code != 200:
                    raise CongressDataError(
                        "SOURCE_HTTP_ERROR", "Disclosure source returned HTTP %s." % resp.status_code
                    )
                data = bytearray()
                for chunk in resp.iter_bytes():
                    data.extend(chunk)
                    if len(data) > MAX_DOWNLOAD_BYTES:
                        raise CongressDataError("SOURCE_TOO_LARGE", "Disclosure file is too large.")
    except httpx.HTTPError:
        raise CongressDataError("SOURCE_UNAVAILABLE", "Could not reach the House disclosure source.")
    return bytes(data)


def read_index_bytes(data: bytes) -> bytes:
    if data[:2] == b"PK":
        with zipfile.ZipFile(io.BytesIO(data)) as z:
            names = [n for n in z.namelist() if n.lower().endswith(".xml")]
            if not names:
                raise CongressDataError("INDEX_INVALID", "No XML inside the index archive.")
            return z.read(names[0])
    return data


def parse_index(xml_bytes: bytes, year: int) -> list[dict[str, Any]]:
    try:
        root = ET.fromstring(xml_bytes)
    except ET.ParseError:
        raise CongressDataError("INDEX_INVALID", "Disclosure index is not valid XML.")
    out = []
    for m in root.iter("Member"):
        f = {c.tag: (c.text or "").strip() for c in m}
        doc = f.get("DocID", "")
        if not doc.isdigit():
            continue
        kind = f.get("FilingType", "")
        y = int(f.get("Year") or year)
        name = " ".join(x for x in (f.get("Prefix"), f.get("First"), f.get("Last"), f.get("Suffix")) if x)
        out.append({
            "doc_id": doc, "type": kind, "last": f.get("Last", ""), "first": f.get("First", ""),
            "name": name, "district": f.get("StateDst", ""), "year": y,
            "filed": iso(f.get("FilingDate", "")),
            "url": PTR_URL % (y, doc) if kind == "P" else None,
        })
    out.sort(key=lambda r: (r["filed"] or "", r["doc_id"]), reverse=True)
    return out


_index_cache: dict[int, tuple[float, list[dict[str, Any]]]] = {}


def load_index(year: int, offline_bytes: bytes | None = None) -> list[dict[str, Any]]:
    if offline_bytes is not None:
        return parse_index(read_index_bytes(offline_bytes), year)
    cached = _index_cache.get(year)
    if cached and time.time() - cached[0] < INDEX_TTL_SECONDS:
        return cached[1]
    try:
        rows = parse_index(read_index_bytes(_download(INDEX_URL % (year, "FD"))), year)
    except zipfile.BadZipFile:
        raise CongressDataError("INDEX_INVALID", "Disclosure index archive is corrupt.")
    _index_cache[year] = (time.time(), rows)
    return rows


def member_match(row: dict[str, Any], member: str | None) -> bool:
    member = (member or "").strip().lower()
    if not member:
        return True
    if "," in member:
        last, first = [x.strip() for x in member.split(",", 1)]
    else:
        parts = member.split()
        last, first = parts[-1], " ".join(parts[:-1])
    if row["last"].lower() != last:
        return False
    return not first or row["first"].lower().startswith(first)


def select(rows, member=None, kind="P", since=None):
    return [
        r for r in rows
        if (not kind or r["type"] == kind)
        and member_match(r, member)
        and (not since or (r["filed"] or "") >= since)
    ]


# ------------------------------------------------------------ PTR text parsing

TXN = re.compile(
    r"^\s*(?:(?P<owner>SP|JT|DC)\s+)?(?P<asset>.*?)\s+(?P<type>S \(partial\)|P|S|E)\s+"
    r"(?P<date>\d{2}/\d{2}/\d{4})\s+(?P<notified>\d{2}/\d{2}/\d{4})\s+(?P<amount>\S.*?)\s*$")
LABELS = [
    ("status", re.compile(r"^\s*F(?:ILING)?\s+S(?:TATUS)?\s*:\s*(.*)$", re.I)),
    ("description", re.compile(r"^\s*D(?:ESCRIPTION)?\s*:\s*(.*)$", re.I)),
    ("account", re.compile(r"^\s*S(?:UBHOLDING)?\s+O(?:F)?\s*:\s*(.*)$", re.I)),
    ("location", re.compile(r"^\s*L(?:OCATION)?\s*:\s*(.*)$", re.I)),
]
PAGE_HEADER = re.compile(r"^\s*(ID\s+Owner\s+Asset|Type\s+Date\b|Type$|Date$|Gains >$|\$200\?$)", re.I)
END = re.compile(r"^\s*(\* For the complete|I CERTIFY|Digitally Signed|Filing ID #)", re.I)
AMOUNT = re.compile(r"\$([\d,]+)")
OPTION = re.compile(r"([\d,]+)\s+(call|put)\s+options?\b.*?strike price of \$([\d,.]+)"
                    r".*?expiration date of (\d{1,2}/\d{1,2}/\d{2,4})", re.I | re.S)
SHARES = re.compile(r"(?:purchased|sold|bought)\s+([\d,]+)\s+shares", re.I)


def _amount(text: str):
    nums = [int(x.replace(",", "")) for x in AMOUNT.findall(text or "")]
    return (nums[0] if nums else None), (nums[1] if len(nums) > 1 else None)


def _finish(row: dict[str, Any], filed: str | None) -> dict[str, Any]:
    asset = " ".join(row.pop("_asset").split())
    tick = re.findall(r"\(([A-Z][A-Z0-9.\-/]{0,9})\)", asset)
    code = re.findall(r"\[([A-Z]{2})\]", asset)
    row["ticker"] = tick[-1] if tick else None
    row["asset_type"] = code[-1] if code else None
    name = re.sub(r"\s*\[[A-Z]{2}\]", "", asset)
    if tick:
        name = re.sub(r"\s*\(%s\)" % re.escape(tick[-1]), "", name)
    row["asset"] = name.strip(" -")
    row["amount"] = " ".join(row["amount"].split())
    row["amount_low"], row["amount_high"] = _amount(row["amount"])
    row["description"] = " ".join(row.get("description", "").split()) or None
    opt = OPTION.search(row["description"] or "")
    if opt:
        row["option"] = {"contracts": int(opt.group(1).replace(",", "")), "kind": opt.group(2).lower(),
                         "strike": float(opt.group(3).replace(",", "")), "expires": iso(opt.group(4))}
    sh = SHARES.search(row["description"] or "")
    if sh:
        row["shares"] = int(sh.group(1).replace(",", ""))
    if filed and row["date"]:
        days = (_dt.date.fromisoformat(filed) - _dt.date.fromisoformat(row["date"])).days
        row["days_to_disclose"] = days
        row["late"] = days > LATE_DAYS
    return row


def parse_ptr_text(text: str, filed: str | None = None) -> dict[str, Any]:
    head: dict[str, Any] = {"name": None, "status": None, "district": None, "doc_id": None, "filed": filed}
    rows: list[dict[str, Any]] = []
    cur = None
    mode = None
    amount_open = False
    for raw in text.splitlines():
        line = raw.rstrip()
        s = line.strip()
        m = re.match(r"^Name:\s+(.*)$", s)
        if m and not head["name"]:
            head["name"] = m.group(1).strip()
            continue
        m = re.match(r"^Status:\s+(.*)$", s)
        if m and not head["status"]:
            head["status"] = m.group(1).strip()
            continue
        m = re.match(r"^State/District:\s*(\S+)", s)
        if m:
            head["district"] = m.group(1)
            continue
        m = re.search(r"Filing ID #(\d+)", s)
        if m:
            head["doc_id"] = m.group(1)
            continue
        m = re.match(r"^Digitally Signed:.*?,\s*(\d{2}/\d{2}/\d{4})", s)
        if m:
            head["filed"] = head["filed"] or iso(m.group(1))
            continue
        t = TXN.match(line)
        if t and iso(t.group("date")):
            if cur:
                rows.append(cur)
            owner = t.group("owner") or ""
            cur = {"owner": owner, "owner_name": OWNERS.get(owner, owner), "_asset": t.group("asset"),
                   "type": t.group("type"), "type_name": TXN_TYPES.get(t.group("type"), t.group("type")),
                   "date": iso(t.group("date")), "notified": iso(t.group("notified")),
                   "amount": t.group("amount"), "status": None, "account": None}
            amount_open = cur["amount"].endswith("-")
            mode = "asset"
            continue
        if cur is None:
            continue
        if not s:
            if mode in ("description", "asset"):
                mode = "labels" if mode == "description" or "[" in cur["_asset"] else mode
            continue
        if PAGE_HEADER.match(s):
            continue
        if END.match(s):
            mode = "labels"
            continue
        label = next(((k, rx.match(line)) for k, rx in LABELS if rx.match(line)), None)
        if label:
            key, lm = label
            value = lm.group(1).strip()
            if key == "description":
                cur["description"] = value
                mode = "description"
            elif key in ("status", "account"):
                cur[key] = value
                mode = "labels"
            else:
                mode = "labels"
            continue
        if mode == "asset":
            parts = re.split(r"\s{2,}", s)
            if amount_open and re.fullmatch(r"\$[\d,]+", parts[-1]):
                cur["amount"] += " " + parts.pop()
                amount_open = False
            if parts and parts[0]:
                cur["_asset"] += " " + " ".join(parts)
        elif mode == "description":
            cur["description"] = cur.get("description", "") + " " + s
    if cur:
        rows.append(cur)
    head["rows"] = [_finish(r, head["filed"]) for r in rows]
    return head


def pdftotext_available() -> bool:
    return shutil.which("pdftotext") is not None or _pymupdf() is not None


def _pymupdf():
    try:
        import pymupdf  # type: ignore

        return pymupdf
    except Exception:  # noqa: BLE001 - optional dependency
        return None


def _normalize_small_caps(text: str) -> str:
    # The Clerk's PDFs encode small-caps labels ("Description:") as U+0283..U+029C.
    return "".join(chr(ord(c) - 0x222) if "\u0283" <= c <= "\u029c" else c for c in text)


def _pymupdf_layout_text(data: bytes) -> str | None:
    """Approximate `pdftotext -layout` using PyMuPDF word boxes (no system poppler needed)."""
    mu = _pymupdf()
    if mu is None:
        return None
    try:
        doc = mu.open(stream=data, filetype="pdf")
    except Exception:  # noqa: BLE001
        return None
    lines: list[str] = []
    for page in doc:
        rows: dict[int, list[tuple]] = {}
        for w in page.get_text("words"):
            rows.setdefault(round((w[1] + w[3]) / 2 / 3), []).append(w)
        for key in sorted(rows):
            out = ""
            last_x = None
            for w in sorted(rows[key], key=lambda w: w[0]):
                if last_x is not None:
                    out += "   " if w[0] - last_x > 8 else " "
                out += w[4]
                last_x = w[2]
            lines.append(_normalize_small_caps(out))
    return "\n".join(lines)


def pdf_bytes_to_text(data: bytes) -> str | None:
    exe = shutil.which("pdftotext")
    if not exe:
        return _pymupdf_layout_text(data)
    import tempfile

    try:
        with tempfile.TemporaryDirectory() as tmp:
            path = os.path.join(tmp, "ptr.pdf")
            with open(path, "wb") as fh:
                fh.write(data)
            out = subprocess.run([exe, "-layout", path, "-"], capture_output=True, timeout=60)
    except (OSError, subprocess.SubprocessError):
        return None
    return out.stdout.decode("utf-8", "replace") if out.returncode == 0 else None


def fetch_trades(year: int, doc_id: str, filed: str | None = None) -> dict[str, Any]:
    """Download one PTR and parse its transactions.

    Returns {"available": bool, "pdf_url": str, "trades": [...], ...}. When
    pdftotext is missing or the PDF is image-only, available is False and the
    caller should link to the PDF instead.
    """
    if not doc_id.isdigit():
        raise CongressDataError("INVALID_DOC_ID", "Invalid filing id.")
    pdf_url = PTR_URL % (year, doc_id)
    data = _download(pdf_url)
    if not data.startswith(b"%PDF"):
        raise CongressDataError("SOURCE_INVALID", "Filing did not come back as a PDF.")
    text = pdf_bytes_to_text(data)
    if text is None:
        return {"available": False, "reason": "PDF_TEXT_UNAVAILABLE", "pdf_url": pdf_url, "trades": []}
    report = parse_ptr_text(text, filed=filed)
    return {
        "available": bool(report["rows"]),
        "reason": None if report["rows"] else "NO_ROWS_FOUND",
        "pdf_url": pdf_url,
        "name": report["name"], "status": report["status"], "district": report["district"],
        "doc_id": doc_id, "filed": report["filed"], "trades": report["rows"],
    }



