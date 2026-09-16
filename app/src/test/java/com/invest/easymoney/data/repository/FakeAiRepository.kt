package com.invest.easymoney.data.repository

import com.invest.easymoney.domain.model.*
import com.invest.easymoney.domain.repository.AiInsightRepository
import com.invest.easymoney.util.Resource

class FakeAiRepository : AiInsightRepository {

    var result: Resource<List<AiPick>> = Resource.Success(emptyList())

    override suspend fun getStockInsight(
        symbol: String,
        stock: Stock,
        news: List<News>
    ): NetworkResult<StockInsight> {
        TODO("Not yet implemented")
    }

    override suspend fun getAiPicks(stocks: List<Stock>): Resource<List<AiPick>> {
        return result
    }
}
