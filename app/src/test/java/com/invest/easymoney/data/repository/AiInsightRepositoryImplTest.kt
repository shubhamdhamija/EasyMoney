package com.invest.easymoney.data.repository

import com.invest.easymoney.data.api.BackendApiService
import com.invest.easymoney.data.api.dto.StockInsightApiResponse
import com.invest.easymoney.data.api.dto.StockInsightRequest
import com.invest.easymoney.data.api.dto.StockInsightResponse
import com.invest.easymoney.data.api.dto.StockPickResponse
import com.invest.easymoney.data.api.dto.StockPicksApiResponse
import com.invest.easymoney.data.api.dto.StockPicksRequest
import com.invest.easymoney.domain.model.NetworkResult
import com.invest.easymoney.domain.model.News
import com.invest.easymoney.domain.model.Stock
import com.invest.easymoney.util.Resource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class AiInsightRepositoryImplTest {

    private lateinit var backend: BackendApiService
    private lateinit var repository: AiInsightRepositoryImpl

    @Before
    fun setUp() {
        backend = mockk()
        repository = AiInsightRepositoryImpl(backendApiService = backend)
    }

    // ── Insight request mapping ──────────────────────────────────────────────

    @Test
    fun `request trims and uppercases the symbol`() {
        val request = createStockInsightRequest(sampleStock(symbol = "  aapl "), sampleNews())
        assertEquals("AAPL", request.symbol)
    }

    @Test
    fun `blank company name maps to null`() {
        assertNull(createStockInsightRequest(sampleStock(name = "   "), sampleNews()).companyName)
        assertEquals(
            "Apple Inc.",
            createStockInsightRequest(sampleStock(name = " Apple Inc. "), sampleNews()).companyName
        )
    }

    @Test
    fun `price and day range are mapped and negatives become null`() {
        val request = createStockInsightRequest(sampleStock(), sampleNews())
        assertEquals(150.0, request.currentPrice!!, 0.0)
        assertEquals(148.0, request.dayLow!!, 0.0)
        assertEquals(151.0, request.dayHigh!!, 0.0)
        assertEquals(2.39, request.priceChangePercent, 0.0)

        val invalid = createStockInsightRequest(
            sampleStock().copy(currentPrice = -1.0, lowPrice = -2.0, highPrice = -3.0),
            sampleNews()
        )
        assertNull(invalid.currentPrice)
        assertNull(invalid.dayLow)
        assertNull(invalid.dayHigh)
    }

    @Test
    fun `at most seven evidence items are sent`() {
        val news = (1..12).map { sampleNewsItem(headline = "Headline $it") }
        val request = createStockInsightRequest(sampleStock(), news)
        assertEquals(7, request.evidence.size)
        assertEquals("Headline 1", request.evidence.first().headline)
    }

    @Test
    fun `blank headlines are excluded and blank sources become null`() {
        val news = listOf(
            sampleNewsItem(headline = "   "),
            sampleNewsItem(headline = " Real headline ", source = "  "),
            sampleNewsItem(headline = "Sourced", source = " Reuters ")
        )
        val evidence = createStockInsightRequest(sampleStock(), news).evidence
        assertEquals(2, evidence.size)
        assertEquals("Real headline", evidence[0].headline)
        assertNull(evidence[0].source)
        assertEquals("Reuters", evidence[1].source)
        assertTrue(evidence.all { it.type == "news" })
    }

    @Test
    fun `market change percent is never invented`() {
        assertNull(createStockInsightRequest(sampleStock(), sampleNews()).marketChangePercent)
    }

    // ── Insight response mapping and errors ──────────────────────────────────

    @Test
    fun `structured backend response maps to StockInsight`() = runTest {
        coEvery { backend.getStockInsight(any()) } returns insightResponse(
            sentiment = "Positive",
            summary = "  Summary text. ",
            shortTerm = " Short. ",
            longTerm = " Long. "
        )

        val result = repository.getStockInsight("AAPL", sampleStock(), sampleNews())

        assertTrue(result is NetworkResult.Success)
        val insight = (result as NetworkResult.Success).data
        assertEquals("AAPL", insight.symbol)
        assertEquals("bullish", insight.sentiment)
        assertEquals("Summary text.", insight.insight)
        assertEquals("Short.", insight.shortTermOutlook)
        assertEquals("Long.", insight.longTermOutlook)
    }

    @Test
    fun `backend sentiments map onto the existing UI sentiments`() {
        fun map(value: String) = insightResponse(sentiment = value).toDomain().sentiment
        assertEquals("bullish", map("positive"))
        assertEquals("bearish", map("negative"))
        assertEquals("neutral", map("neutral"))
        assertEquals("neutral", map("mixed"))
        assertEquals("neutral", map("insufficient"))
    }

    @Test
    fun `missing insight produces a controlled error`() = runTest {
        coEvery { backend.getStockInsight(any()) } returns
            StockInsightApiResponse(ok = true, insight = null, requestId = "r1")

        val result = repository.getStockInsight("AAPL", sampleStock(), sampleNews())

        assertTrue(result is NetworkResult.Error)
        assertEquals(
            "AI insight could not be loaded.",
            (result as NetworkResult.Error).throwable.message
        )
    }

    @Test
    fun `unknown sentiment produces a controlled mapping error`() = runTest {
        coEvery { backend.getStockInsight(any()) } returns insightResponse(sentiment = "euphoric")

        val result = repository.getStockInsight("AAPL", sampleStock(), sampleNews())

        assertTrue(result is NetworkResult.Error)
        assertEquals(
            "AI insight could not be loaded.",
            (result as NetworkResult.Error).throwable.message
        )
    }

    @Test
    fun `backend 503 exposes a safe message and no raw HTTP details`() = runTest {
        coEvery { backend.getStockInsight(any()) } throws httpException(503)

        val result = repository.getStockInsight("AAPL", sampleStock(), sampleNews())

        assertTrue(result is NetworkResult.Error)
        val message = (result as NetworkResult.Error).throwable.message.orEmpty()
        assertEquals("AI insight is temporarily unavailable.", message)
        assertFalse(message.contains("503"))
        assertFalse(message.contains("HTTP"))
        assertFalse(message.contains("provider failure"))
    }

    @Test
    fun `insight cancellation is rethrown`() = runTest {
        coEvery { backend.getStockInsight(any()) } throws CancellationException("cancelled")

        try {
            repository.getStockInsight("AAPL", sampleStock(), sampleNews())
            fail("Expected CancellationException")
        } catch (expected: CancellationException) {
            assertEquals("cancelled", expected.message)
        }
    }

    @Test
    fun `insight is sent to the backend once and cached`() = runTest {
        val captured = slot<StockInsightRequest>()
        coEvery { backend.getStockInsight(capture(captured)) } returns insightResponse()

        repository.getStockInsight("AAPL", sampleStock(), sampleNews())
        repository.getStockInsight("AAPL", sampleStock(), sampleNews())

        coVerify(exactly = 1) { backend.getStockInsight(any()) }
        assertNotNull(captured.captured)
        assertEquals("AAPL", captured.captured.symbol)
    }

    // ── Picks request mapping ────────────────────────────────────────────────

    @Test
    fun `picks request normalises symbols and maps fields`() {
        val request = createStockPicksRequest(
            listOf(sampleStock(symbol = "  nvda ", name = " NVIDIA ", changePercent = 4.5))
        )

        val item = request.stocks.single()
        assertEquals("NVDA", item.symbol)
        assertEquals("NVIDIA", item.companyName)
        assertEquals(150.0, item.currentPrice!!, 0.0)
        assertEquals(4.5, item.priceChangePercent, 0.0)
        assertEquals(148.0, item.dayLow!!, 0.0)
        assertEquals(151.0, item.dayHigh!!, 0.0)
        assertEquals(3, request.maximumPicks)
    }

    @Test
    fun `picks request removes duplicates, blanks and non-finite changes`() {
        val request = createStockPicksRequest(
            listOf(
                sampleStock(symbol = "AAPL"),
                sampleStock(symbol = " aapl "),
                sampleStock(symbol = "  "),
                sampleStock(symbol = "BAD", changePercent = Double.NaN),
                sampleStock(symbol = "MSFT")
            )
        )
        assertEquals(listOf("AAPL", "MSFT"), request.stocks.map { it.symbol })
    }

    @Test
    fun `picks request is limited to the backend maximum`() {
        val stocks = (1..45).map { sampleStock(symbol = "S$it") }
        assertEquals(30, createStockPicksRequest(stocks).stocks.size)
    }

    @Test
    fun `blank company name and negative prices map to null`() {
        val item = createStockPicksRequest(
            listOf(sampleStock(name = " ").copy(currentPrice = -1.0, lowPrice = -2.0, highPrice = -3.0))
        ).stocks.single()
        assertNull(item.companyName)
        assertNull(item.currentPrice)
        assertNull(item.dayLow)
        assertNull(item.dayHigh)
    }

    // ── Picks response mapping, errors and caching ───────────────────────────

    @Test
    fun `backend response maps to existing AiPick in rank order`() = runTest {
        coEvery { backend.getAiPicks(any()) } returns picksResponse(
            StockPickResponse("tsla", " Second. ", 2),
            StockPickResponse("nvda", " First. ", 1)
        )

        val result = repository.getAiPicks(listOf(sampleStock(symbol = "NVDA"), sampleStock(symbol = "TSLA")))

        assertTrue(result is Resource.Success)
        val picks = (result as Resource.Success).data
        assertEquals(listOf("NVDA", "TSLA"), picks.map { it.symbol })
        assertEquals(listOf("First.", "Second."), picks.map { it.reason })
    }

    @Test
    fun `picks with blank symbol or reason are dropped`() = runTest {
        coEvery { backend.getAiPicks(any()) } returns picksResponse(
            StockPickResponse("NVDA", "Reason.", 1),
            StockPickResponse(" ", "No symbol.", 2),
            StockPickResponse("TSLA", "  ", 3)
        )

        val result = repository.getAiPicks(listOf(sampleStock()))

        assertEquals(1, (result as Resource.Success).data.size)
    }

    @Test
    fun `missing picks produce a safe error`() = runTest {
        coEvery { backend.getAiPicks(any()) } returns
            StockPicksApiResponse(ok = true, picks = null, requestId = "r1")

        val result = repository.getAiPicks(listOf(sampleStock()))

        assertTrue(result is Resource.Error)
        assertEquals("AI picks could not be loaded.", (result as Resource.Error).message)
    }

    @Test
    fun `picks backend 503 exposes a safe message and no raw HTTP details`() = runTest {
        coEvery { backend.getAiPicks(any()) } throws httpException(503)

        val result = repository.getAiPicks(listOf(sampleStock()))

        assertTrue(result is Resource.Error)
        val message = (result as Resource.Error).message
        assertEquals("AI picks are temporarily unavailable.", message)
        assertFalse(message.contains("503"))
        assertFalse(message.contains("HTTP"))
    }

    @Test
    fun `picks cancellation is rethrown`() = runTest {
        coEvery { backend.getAiPicks(any()) } throws CancellationException("cancelled")

        try {
            repository.getAiPicks(listOf(sampleStock()))
            fail("Expected CancellationException")
        } catch (expected: CancellationException) {
            assertEquals("cancelled", expected.message)
        }
    }

    @Test
    fun `picks are requested once and cached`() = runTest {
        val captured = slot<StockPicksRequest>()
        coEvery { backend.getAiPicks(capture(captured)) } returns
            picksResponse(StockPickResponse("AAPL", "Reason.", 1))

        repository.getAiPicks(listOf(sampleStock()))
        repository.getAiPicks(listOf(sampleStock()))

        coVerify(exactly = 1) { backend.getAiPicks(any()) }
        assertEquals("AAPL", captured.captured.stocks.single().symbol)
    }

    @Test
    fun `no usable stocks returns empty picks without a backend call`() = runTest {
        val result = repository.getAiPicks(emptyList())

        assertTrue(result is Resource.Success)
        assertTrue((result as Resource.Success).data.isEmpty())
        coVerify(exactly = 0) { backend.getAiPicks(any()) }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun picksResponse(vararg picks: StockPickResponse) = StockPicksApiResponse(
        ok = true,
        picks = picks.toList(),
        disclaimer = "For informational purposes only.",
        requestId = "request-1"
    )

    private fun insightResponse(
        sentiment: String = "positive",
        summary: String = "Summary.",
        shortTerm: String = "Short.",
        longTerm: String = "Long."
    ) = StockInsightApiResponse(
        ok = true,
        insight = StockInsightResponse(
            symbol = "aapl",
            direction = "increase",
            evidenceSentiment = sentiment,
            summary = summary,
            shortTermContext = shortTerm,
            longTermContext = longTerm,
            factors = listOf("Factor"),
            evidenceSufficient = true,
            disclaimer = "For informational purposes only."
        ),
        requestId = "request-1"
    )

    private fun httpException(code: Int): HttpException {
        val errorBody = "{\"error\":\"provider failure\"}"
            .toResponseBody("application/json".toMediaType())
        return HttpException(Response.error<String>(code, errorBody))
    }

    private fun sampleStock(
        symbol: String = "AAPL",
        name: String = "$symbol Inc.",
        changePercent: Double = 2.39
    ) = Stock(
        symbol = symbol,
        name = name,
        currentPrice = 150.0,
        change = 3.0,
        changePercent = changePercent,
        highPrice = 151.0,
        lowPrice = 148.0,
        exchange = "NASDAQ"
    )

    private fun sampleNewsItem(
        headline: String = "Apple rises after strong demand report",
        source: String = "Reuters"
    ) = News(
        id = 1L,
        headline = headline,
        source = source,
        url = "https://example.com/aapl",
        summary = "Demand trends remain solid.",
        imageUrl = "",
        datetime = 0L
    )

    private fun sampleNews() = listOf(sampleNewsItem())
}

