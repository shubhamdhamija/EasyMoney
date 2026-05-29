package com.invest.easymoney.domain.repository

import com.invest.easymoney.domain.model.AiPick
import com.invest.easymoney.domain.model.News
import com.invest.easymoney.domain.model.Stock
import com.invest.easymoney.domain.model.StockInsight
import com.invest.easymoney.util.Resource

interface AiInsightRepository {

    /**
     * Returns an AI-generated insight for the given stock.
     * Results are cached for 30 minutes to avoid repeated API calls.
     */
    suspend fun getStockInsight(
        symbol: String,
        stock: Stock,
        news: List<News>
    ): Resource<StockInsight>

    /**
     * Ranks the given stocks and returns the top AI picks of the day.
     * Results are cached for 30 minutes.
     */
    suspend fun getAiPicks(stocks: List<Stock>): Resource<List<AiPick>>
}
