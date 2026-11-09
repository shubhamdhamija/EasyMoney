package com.invest.easymoney.data.repository

import com.invest.easymoney.data.api.dto.StockEvidenceRequest
import com.invest.easymoney.data.api.dto.StockInsightApiResponse
import com.invest.easymoney.data.api.dto.StockInsightRequest
import com.invest.easymoney.domain.model.News
import com.invest.easymoney.domain.model.Stock
import com.invest.easymoney.domain.model.StockInsight

private const val MAX_EVIDENCE_ITEMS = 7

internal fun createStockInsightRequest(stock: Stock, news: List<News>): StockInsightRequest =
    StockInsightRequest(
        symbol = stock.symbol.trim().uppercase(),
        companyName = stock.name.trim().takeIf { it.isNotBlank() },
        currentPrice = stock.currentPrice.takeIf { it >= 0.0 },
        priceChangePercent = stock.changePercent,
        // Never invent a benchmark: stays null until EasyMoney has a genuine one.
        marketChangePercent = null,
        dayLow = stock.lowPrice.takeIf { it >= 0.0 },
        dayHigh = stock.highPrice.takeIf { it >= 0.0 },
        evidence = news
            .take(MAX_EVIDENCE_ITEMS)
            .mapNotNull { item ->
                val headline = item.headline.trim()
                if (headline.isBlank()) {
                    return@mapNotNull null
                }
                StockEvidenceRequest(
                    type = "news",
                    headline = headline,
                    source = item.source.trim().takeIf { it.isNotBlank() }
                )
            }
    )

/**
 * Transitional mapping onto the existing domain model so the UI is unchanged:
 * evidenceSentiment -> sentiment (positive/negative -> bullish/bearish),
 * summary -> insight, shortTermContext -> shortTermOutlook,
 * longTermContext -> longTermOutlook.
 * TODO: rename the domain fields to shortTermContext / longTermContext so
 * Android no longer implies forecasting.
 */
internal fun StockInsightApiResponse.toDomain(): StockInsight {
    check(ok) { "Backend reported an unsuccessful stock insight." }

    val value = requireNotNull(insight) {
        "Backend response did not include a stock insight."
    }

    val sentiment = when (value.evidenceSentiment.trim().lowercase()) {
        "positive" -> "bullish"
        "negative" -> "bearish"
        "neutral", "mixed", "insufficient" -> "neutral"
        else -> throw IllegalStateException("Backend returned an unknown sentiment.")
    }

    return StockInsight(
        symbol = value.symbol.trim().uppercase(),
        sentiment = sentiment,
        insight = value.summary.trim(),
        shortTermOutlook = value.shortTermContext.trim(),
        longTermOutlook = value.longTermContext.trim()
    )
}

