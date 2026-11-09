import asyncio
import re

from fastapi import APIRouter, Query
from fastapi.responses import JSONResponse

from app.services import congress_trades as ct

router = APIRouter(prefix="/congress", tags=["congress"])

DISCLAIMER = (
    "Public disclosure under the STOCK Act, filed up to 45 days after the trade. "
    "Amounts are ranges. Not inside information. For informational purposes only."
)


def _error(status: int, code: str, message: str) -> JSONResponse:
    return JSONResponse(status_code=status, content={"error": {"code": code, "message": message}})


@router.get("/filings")
async def list_filings(
    member: str | None = Query(default=None, max_length=60, pattern=r"^[A-Za-z ,.'\-]*$"),
    year: int = Query(default=None, ge=2008, le=2100),
    since: str | None = Query(default=None, pattern=r"^\d{4}-\d{2}-\d{2}$"),
    limit: int = Query(default=30, ge=1, le=100),
):
    import datetime as _dt

    year = year or _dt.date.today().year
    try:
        rows = await asyncio.to_thread(ct.load_index, year)
    except ct.CongressDataError as exc:
        return _error(502, exc.code, exc.message)
    picked = ct.select(rows, member=member, kind="P", since=since)[:limit]
    return {
        "year": year,
        "member": member,
        "filings": picked,
        "pdf_text_available": ct.pdftotext_available(),
        "disclaimer": DISCLAIMER,
    }


@router.get("/filings/{year}/{doc_id}")
async def filing_trades(year: int, doc_id: str, filed: str | None = Query(default=None, pattern=r"^\d{4}-\d{2}-\d{2}$")):
    if not re.fullmatch(r"\d{4,12}", doc_id) or not 2008 <= year <= 2100:
        return _error(422, "INVALID_DOC_ID", "Invalid filing id.")
    try:
        result = await asyncio.to_thread(ct.fetch_trades, year, doc_id, filed)
    except ct.CongressDataError as exc:
        return _error(502, exc.code, exc.message)
    result["disclaimer"] = DISCLAIMER
    return result

