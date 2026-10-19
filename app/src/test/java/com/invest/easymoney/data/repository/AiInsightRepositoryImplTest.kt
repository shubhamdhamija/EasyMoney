package com.invest.easymoney.data.repository

import com.google.gson.Gson
import com.invest.easymoney.data.api.OpenAiApiService
import com.invest.easymoney.domain.model.NetworkResult
import com.invest.easymoney.domain.model.News
import com.invest.easymoney.domain.model.Stock
import com.invest.easymoney.util.Resource
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class AiInsightRepositoryImplTest {

    private lateinit var openAiApi: OpenAiApiService
    private lateinit var repository: AiInsightRepositoryImpl

    @Before
    fun setUp() {
        openAiApi = mockk()
        repository = AiInsightRepositoryImpl(
            openAiApi = openAiApi,
            gson = Gson()
        )
    }

    @Test
    fun `getStockInsight falls back when provider returns http 500`() = runTest {
        coEvery { openAiApi.chatCompletion(any()) } throws httpException(500)

        val result = repository.getStockInsight(
            symbol = "AAPL",
            stock = sampleStock(),
            news = sampleNews()
        )

        assertTrue(result is NetworkResult.Success)
        val insight = (result as NetworkResult.Success).data
        assertEquals("AAPL", insight.symbol)
        assertTrue(insight.insight.contains("AI offline"))
    }

    @Test
    fun `getAiPicks falls back when provider returns http 500`() = runTest {
        coEvery { openAiApi.chatCompletion(any()) } throws httpException(500)

        val result = repository.getAiPicks(
            listOf(
                sampleStock(symbol = "NVDA", changePercent = 4.5),
                sampleStock(symbol = "AAPL", changePercent = 2.0),
                sampleStock(symbol = "TSLA", changePercent = -3.2)
            )
        )

        assertTrue(result is Resource.Success)
        val picks = (result as Resource.Success).data
        assertEquals(3, picks.size)
        assertTrue(picks.all { it.reason.contains("AI offline") })
    }

    private fun httpException(code: Int): HttpException {
        val errorBody = "{\"error\":\"provider failure\"}"
            .toResponseBody("application/json".toMediaType())
        return HttpException(Response.error<String>(code, errorBody))
    }

    private fun sampleStock(
        symbol: String = "AAPL",
        changePercent: Double = 2.39
    ) = Stock(
        symbol = symbol,
        name = "$symbol Inc.",
        currentPrice = 150.0,
        change = 3.0,
        changePercent = changePercent,
        highPrice = 151.0,
        lowPrice = 148.0,
        exchange = "NASDAQ"
    )

    private fun sampleNews() = listOf(
        News(
            id = 1L,
            headline = "Apple rises after strong demand report",
            source = "Reuters",
            url = "https://example.com/aapl",
            summary = "Demand trends remain solid.",
            imageUrl = "",
            datetime = 0L
        )
    )
}

