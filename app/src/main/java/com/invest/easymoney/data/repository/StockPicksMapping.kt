package com.invest.easymoney.data.repository

import com.invest.easymoney.data.api.dto.StockPickInput
import com.invest.easymoney.data.api.dto.StockPicksApiResponse
import com.invest.easymoney.data.api.dto.StockPicksRequest
import com.invest.easymoney.domain.model.AiPick
import com.invest.easymoney.domain.model.Stock

/** Backend limit for `POST /ai/picks`. */
internal const val MAX_PICK_STOCKS = 30

/** Number of highlighted stocks requested. */
internal const val MAXIMUM_PICKS = 3

internal fun createStockPicksRequest(stocks: List<Stock>): StockPicksRequest =
    StockPicksRequest(
        stocks = stocks
            .filter { it.symbol.isNotBlank() && it.changePercent.isFinite() }
            // The backend rejects duplicate symbols, so normalise then de-duplicate.
            .distinctBy { it.symbol.trim().uppercase() }
            .take(MAX_PICK_STOCKS)
            .map { stock ->
                StockPickInput(
                    symbol = stock.symbol.trim().uppercase(),
                    companyName = stock.name.trim().takeIf { it.isNotBlank() },
                    currentPrice = stock.currentPrice.takeIf { it.isFinite() && it >= 0.0 },
                    priceChangePercent = stock.changePercent,
                    dayLow = stock.lowPrice.takeIf { it.isFinite() && it >= 0.0 },
                    dayHigh = stock.highPrice.takeIf { it.isFinite() && it >= 0.0 }
                )
            },
        maximumPicks = MAXIMUM_PICKS
    )

/** Maps the backend contract onto the existing [AiPick] model (kept for compatibility). */
internal fun StockPicksApiResponse.toDomain(): List<AiPick> {
    check(ok) { "Backend reported an unsuccessful picks response." }

    val items = requireNotNull(picks) {
        "Backend response did not include picks."
    }

    return items
        .sortedBy { it.rank }
        .filter { it.symbol.isNotBlank() && it.reason.isNotBlank() }
        .map { AiPick(symbol = it.symbol.trim().uppercase(), reason = it.reason.trim()) }
}
