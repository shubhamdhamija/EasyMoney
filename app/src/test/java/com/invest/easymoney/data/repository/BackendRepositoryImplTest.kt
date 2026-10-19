package com.invest.easymoney.data.repository

import com.google.gson.Gson
import com.invest.easymoney.data.api.BackendApiService
import com.invest.easymoney.data.api.dto.StockExplanationApiResponse
import com.invest.easymoney.data.api.dto.StockExplanationMetadata
import com.invest.easymoney.data.api.dto.StockExplanationResponse
import com.invest.easymoney.domain.model.NetworkResult
import com.invest.easymoney.domain.model.EvidenceSentiment
import com.invest.easymoney.domain.model.MovementDirection
import com.invest.easymoney.domain.model.News
import com.invest.easymoney.domain.model.Stock
import com.invest.easymoney.domain.model.StockExplanation
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class BackendRepositoryImplTest {

    private lateinit var backendApiService: BackendApiService
    private lateinit var repository: BackendRepositoryImpl

    @Before
    fun setUp() {
        backendApiService = mockk()
        repository = BackendRepositoryImpl(
            backendApiService = backendApiService,
            gson = Gson()
        )
    }

    @Test
    fun `complete structured response maps correctly`() = runTest {
        val requestSlot = slot<com.invest.easymoney.data.api.dto.StockExplanationRequest>()

        coEvery {
            backendApiService.explainStockMovement(capture(requestSlot))
        } returns StockExplanationApiResponse(
            ok = true,
            explanation = StockExplanationResponse(
                symbol = "AAPL",
                direction = "increase",
                evidenceSentiment = "mixed",
                summary = "Revenue evidence may have contributed.",
                factors = listOf(
                    "Revenue exceeded expectations.",
                    "Broader market tailwinds helped sentiment."
                ),
                evidenceSufficient = false,
                disclaimer = "For informational purposes only."
            ),
            metadata = StockExplanationMetadata(
                promptVersion = "stock-explanation-v1",
                providerCalled = true
            ),
            requestId = "test-request-id"
        )

        val result = repository.explainMove(
            stock = sampleStock(),
            news = sampleNews(count = 8)
        )

        assertTrue(result is NetworkResult.Success)
        val explanation = (result as NetworkResult.Success).data
        assertEquals(
            StockExplanation(
                symbol = "AAPL",
                direction = MovementDirection.INCREASE,
                evidenceSentiment = EvidenceSentiment.MIXED,
                summary = "Revenue evidence may have contributed.",
                factors = listOf(
                    "Revenue exceeded expectations.",
                    "Broader market tailwinds helped sentiment."
                ),
                evidenceSufficient = false,
                disclaimer = "For informational purposes only.",
                requestId = "test-request-id"
            ),
            explanation
        )

        assertEquals("AAPL", requestSlot.captured.symbol)
        assertEquals("Apple Inc.", requestSlot.captured.companyName)
        assertEquals(2.4, requestSlot.captured.priceChangePercent, 0.0)
        assertNull(requestSlot.captured.marketChangePercent)
        assertEquals(220.5, requestSlot.captured.dayLow ?: 0.0, 0.0)
        assertEquals(228.3, requestSlot.captured.dayHigh ?: 0.0, 0.0)
        assertEquals(7, requestSlot.captured.evidence.size)
        assertEquals("news", requestSlot.captured.evidence.first().type)
        assertEquals("Headline 1", requestSlot.captured.evidence.first().headline)
        assertEquals("Source 1", requestSlot.captured.evidence.first().source)
        assertNull(requestSlot.captured.evidence[1].source)
    }

    @Test
    fun `increase direction is mapped defensively`() = runTest {
        coEvery {
            backendApiService.explainStockMovement(any())
        } returns structuredResponse(direction = "increase", sentiment = "positive")

        val result = repository.explainMove(sampleStock(), emptyList())

        assertTrue(result is NetworkResult.Success)
        val explanation = (result as NetworkResult.Success).data
        assertEquals(MovementDirection.INCREASE, explanation.direction)
    }

    @Test
    fun `mixed sentiment is mapped defensively`() = runTest {
        coEvery {
            backendApiService.explainStockMovement(any())
        } returns structuredResponse(direction = "unchanged", sentiment = "mixed")

        val result = repository.explainMove(sampleStock(), emptyList())

        assertTrue(result is NetworkResult.Success)
        val explanation = (result as NetworkResult.Success).data
        assertEquals(EvidenceSentiment.MIXED, explanation.evidenceSentiment)
    }

    @Test
    fun `unknown direction returns a mapping error`() = runTest {
        coEvery {
            backendApiService.explainStockMovement(any())
        } returns structuredResponse(direction = "upwards", sentiment = "positive")

        val result = repository.explainMove(sampleStock(), emptyList())

        assertTrue(result is NetworkResult.Error)
        val throwable = (result as NetworkResult.Error).throwable
        assertTrue(throwable is IllegalArgumentException)
        assertEquals("Unsupported movement direction: upwards", throwable.message)
    }

    @Test
    fun `unknown sentiment returns a mapping error`() = runTest {
        coEvery {
            backendApiService.explainStockMovement(any())
        } returns structuredResponse(direction = "increase", sentiment = "super_positive")

        val result = repository.explainMove(sampleStock(), emptyList())

        assertTrue(result is NetworkResult.Error)
        val throwable = (result as NetworkResult.Error).throwable
        assertTrue(throwable is IllegalArgumentException)
        assertEquals("Unsupported evidence sentiment: super_positive", throwable.message)
    }

    @Test
    fun `missing explanation produces a controlled error`() = runTest {
        coEvery {
            backendApiService.explainStockMovement(any())
        } returns StockExplanationApiResponse(
            ok = true,
            explanation = null,
            metadata = StockExplanationMetadata(
                promptVersion = "stock-explanation-v1",
                providerCalled = true
            ),
            requestId = "req-missing"
        )

        val result = repository.explainMove(sampleStock(), emptyList())

        assertTrue(result is NetworkResult.Error)
        val throwable = (result as NetworkResult.Error).throwable
        assertTrue(throwable is IllegalArgumentException)
        assertEquals("Missing stock explanation", throwable.message)
    }

    @Test
    fun `request id is preserved and disclaimer is retained`() = runTest {
        coEvery {
            backendApiService.explainStockMovement(any())
        } returns structuredResponse(requestId = "req-123")

        val result = repository.explainMove(sampleStock(), emptyList())

        assertTrue(result is NetworkResult.Success)
        val explanation = (result as NetworkResult.Success).data
        assertEquals("req-123", explanation.requestId)
        assertEquals("For informational purposes only.", explanation.disclaimer)
    }

    @Test
    fun `explainMove maps backend error body into a readable failure`() = runTest {
        val errorBody = """{"ok":false,"error":{"code":"AI_LIVE_CALLS_DISABLED","message":"AI explanation generation is currently disabled."},"requestId":"abc"}"""
            .toResponseBody("application/json".toMediaType())

        coEvery {
            backendApiService.explainStockMovement(any())
        } throws HttpException(Response.error<Any>(503, errorBody))

        val result = repository.explainMove(
            stock = sampleStock(),
            news = emptyList()
        )

        assertTrue(result is NetworkResult.Error)
        val throwable = (result as NetworkResult.Error).throwable
        assertEquals("AI explanation generation is currently disabled.", throwable.message)
        assertTrue(throwable is BackendApiException)
        assertEquals("AI_LIVE_CALLS_DISABLED", (throwable as BackendApiException).code)
        assertEquals("abc", throwable.requestId)
    }

    private fun structuredResponse(
        direction: String = "increase",
        sentiment: String = "positive",
        requestId: String = "test-request-id"
    ) = StockExplanationApiResponse(
        ok = true,
        explanation = StockExplanationResponse(
            symbol = "AAPL",
            direction = direction,
            evidenceSentiment = sentiment,
            summary = "Revenue evidence may have contributed.",
            factors = listOf("Revenue exceeded expectations."),
            evidenceSufficient = true,
            disclaimer = "For informational purposes only."
        ),
        metadata = StockExplanationMetadata(
            promptVersion = "stock-explanation-v1",
            providerCalled = true
        ),
        requestId = requestId
    )

    private fun sampleStock() = Stock(
        symbol = "AAPL",
        name = "Apple Inc.",
        currentPrice = 227.45,
        change = 5.32,
        changePercent = 2.4,
        highPrice = 228.3,
        lowPrice = 220.5,
        openPrice = 223.12,
        previousClose = 222.13,
        marketCap = 3.6e12,
        exchange = "NASDAQ",
        industry = "Technology"
    )

    private fun sampleNews(count: Int) = (1..count).map { index ->
        News(
            id = index.toLong(),
            headline = "Headline $index",
            source = if (index == 2) "" else "Source $index",
            url = "https://example.com/$index",
            summary = "Summary $index",
            imageUrl = "",
            datetime = 0L
        )
    }
}



