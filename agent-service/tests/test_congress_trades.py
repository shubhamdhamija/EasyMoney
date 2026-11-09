import zipfile
import io

from fastapi.testclient import TestClient

from app.main import app
from app.services import congress_trades as ct

INDEX_XML = b"""<?xml version="1.0"?>
<FinancialDisclosure>
  <Member><Prefix>Hon.</Prefix><Last>Pelosi</Last><First>Nancy</First><Suffix></Suffix>
    <FilingType>P</FilingType><StateDst>CA11</StateDst><Year>2026</Year>
    <FilingDate>8/21/2026</FilingDate><DocID>20035143</DocID></Member>
  <Member><Last>Pelosi</Last><First>Nancy</First><FilingType>O</FilingType>
    <StateDst>CA11</StateDst><Year>2026</Year><FilingDate>5/15/2026</FilingDate><DocID>10000001</DocID></Member>
  <Member><Last>Other</Last><First>Sam</First><FilingType>P</FilingType>
    <StateDst>TX01</StateDst><Year>2026</Year><FilingDate>9/01/2026</FilingDate><DocID>20035999</DocID></Member>
  <Member><Last>Bad</Last><First>Id</First><FilingType>P</FilingType>
    <StateDst>NY01</StateDst><Year>2026</Year><FilingDate>9/01/2026</FilingDate><DocID>../etc</DocID></Member>
</FinancialDisclosure>"""

PTR_TEXT = """Name: Hon. Nancy Pelosi
Status: Member
State/District: CA11
SP NVIDIA Corporation - Common Stock (NVDA) [ST]   P   07/01/2026   07/02/2026   $1,000,001 - $5,000,000
F S: New
D: Purchased 100 shares of stock.
Digitally Signed: Hon. Nancy Pelosi , 08/21/2026
"""


def test_parse_index_filters_and_validates_doc_ids():
    rows = ct.parse_index(INDEX_XML, 2026)
    assert [r["doc_id"] for r in rows] == ["20035999", "20035143", "10000001"]
    picked = ct.select(rows, member="pelosi", kind="P")
    assert [r["doc_id"] for r in picked] == ["20035143"]
    assert picked[0]["url"].endswith("/2026/20035143.pdf")


def test_index_reads_from_zip():
    buf = io.BytesIO()
    with zipfile.ZipFile(buf, "w") as z:
        z.writestr("2026FD.xml", INDEX_XML)
    assert len(ct.load_index(2026, offline_bytes=buf.getvalue())) == 3


def test_parse_ptr_text_extracts_trade_and_lateness():
    report = ct.parse_ptr_text(PTR_TEXT)
    assert report["name"] == "Hon. Nancy Pelosi"
    trade = report["rows"][0]
    assert trade["ticker"] == "NVDA"
    assert trade["type_name"] == "purchase"
    assert trade["owner_name"] == "spouse"
    assert trade["amount_low"] == 1_000_001 and trade["amount_high"] == 5_000_000
    assert trade["shares"] == 100
    assert trade["days_to_disclose"] == 51 and trade["late"] is True


def test_filings_route_uses_cached_index(monkeypatch):
    rows = ct.parse_index(INDEX_XML, 2026)
    monkeypatch.setattr(ct, "load_index", lambda year: rows)
    client = TestClient(app)
    body = client.get("/congress/filings", params={"member": "pelosi", "year": 2026}).json()
    assert [f["doc_id"] for f in body["filings"]] == ["20035143"]
    assert "Not inside information" in body["disclaimer"]


def test_trades_route_rejects_bad_doc_id():
    client = TestClient(app)
    assert client.get("/congress/filings/2026/abc").status_code == 422


def test_trades_route_reports_unavailable_without_pdftotext(monkeypatch):
    monkeypatch.setattr(ct, "_download", lambda url, timeout=30: b"%PDF-1.4 fake")
    monkeypatch.setattr(ct, "pdf_bytes_to_text", lambda data: None)
    body = TestClient(app).get("/congress/filings/2026/20035143").json()
    assert body["available"] is False and body["trades"] == []
    assert body["pdf_url"].endswith("20035143.pdf")

def test_small_caps_labels_are_normalized():
    assert ct._normalize_small_caps("D\u0287\u0295\u0285\u0514") .startswith("Des")
    assert ct._normalize_small_caps("Plain 123") == "Plain 123"
