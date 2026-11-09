package com.invest.easymoney.ui.detail

import androidx.lifecycle.SavedStateHandle
import com.invest.easymoney.MainDispatcherRule
import com.invest.easymoney.data.repository.BackendApiException
import com.invest.easymoney.domain.model.EvidenceSentiment
import com.invest.easymoney.domain.model.MovementDirection
import com.invest.easymoney.domain.model.NetworkResult
import com.invest.easymoney.domain.model.News
import com.invest.easymoney.domain.model.Stock
import com.invest.easymoney.domain.model.StockExplanation
import com.invest.easymoney.domain.repository.AiInsightRepository
import com.invest.easymoney.domain.repository.BackendRepository
import com.invest.easymoney.domain.repository.StockRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StockDetailViewModelExplainTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var stockRepository: StockRepository
    private lateinit var aiRepository: AiInsightRepository
    private lateinit var backendRepository: BackendRepository

    private val stock = Stock(
        symbol = "AAPL",
        name = "Apple Inc.",
        currentPrice = 227.45,
        change = 5.32,
        changePercent = 2.4,
        highPrice = 228.3,
        lowPrice = 220.5
    )

    private val news = listOf(
        News(
            id = 1L,
            headline = "Revenue exceeded the estimate",
            source = "Source",
            url = "https://example.com/1",
            summary = "",
            imageUrl = "",
            datetime = 0L
        )
    )

    @Before
    fun setUp() {
        stockRepository = mockk(relaxed = true)
        aiRepository = mockk(relaxed = true)
        backendRepository = mockk(relaxed = true)

        coEvery { stockRepository.isInWatchlist(any()) } returns false
        coEvery { stockRepository.getNews("AAPL") } returns NetworkResult.Success(news)
        coEvery {
            aiRepository.getStockInsight(any(), any(), any())
        } returns NetworkResult.Error(IllegalStateException("insight unavailable"))
    }

    private fun stockLoaded() {
        coEvery { stockRepository.getStockDetail("AAPL") } returns NetworkResult.Success(stock)
    }

    private fun createViewModel() = StockDetailViewModel(
        repository = stockRepository,
        aiRepository = aiRepository,
        backendRepository = backendRepository,
        savedStateHandle = SavedStateHandle(mapOf("symbol" to "AAPL")),
        deviceId = "test-device"
    )

    private fun explanation(
        evidenceSufficient: Boolean,
        sentiment: EvidenceSentiment = EvidenceSentiment.POSITIVE,
        factors: List<String> = listOf("Revenue exceeded the estimate."),
        requestId: String? = "req-1"
    ) = StockExplanation(
        symbol = "AAPL",
        direction = MovementDirection.INCREASE,
        evidenceSentiment = sentiment,
        summary = "Revenue evidence may have contributed.",
        factors = factors,
        evidenceSufficient = evidenceSufficient,
        disclaimer = "For informational purposes only.",
        requestId = requestId
    )

    @Test
    fun `initial explain state is idle`() = runTest {
        stockLoaded()

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(ExplainMoveUiState.Idle, viewModel.explainState.value)
    }

    @Test
    fun `sufficient evidence produces Success state`() = runTest {
        stockLoaded()
        val expected = explanation(evidenceSufficient = true)
        coEvery { backendRepository.explainMove(stock, news) } returns NetworkResult.Success(expected)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.explainMove()
        advanceUntilIdle()

        assertEquals(ExplainMoveUiState.Success(expected), viewModel.explainState.value)
    }

    @Test
    fun `insufficient evidence produces InsufficientEvidence state not Error`() = runTest {
        stockLoaded()
        val expected = explanation(
            evidenceSufficient = false,
            sentiment = EvidenceSentiment.INSUFFICIENT,
            factors = emptyList()
        )
        coEvery { backendRepository.explainMove(stock, news) } returns NetworkResult.Success(expected)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.explainMove()
        advanceUntilIdle()

        val state = viewModel.explainState.value
        assertTrue(state is ExplainMoveUiState.InsufficientEvidence)
        assertEquals(expected, (state as ExplainMoveUiState.InsufficientEvidence).explanation)
    }

    @Test
    fun `repository failure produces Error state`() = runTest {
        stockLoaded()
        coEvery {
            backendRepository.explainMove(stock, news)
        } returns NetworkResult.Error(IllegalStateException("The AI provider is unavailable."))

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.explainMove()
        advanceUntilIdle()

        val state = viewModel.explainState.value
        assertTrue(state is ExplainMoveUiState.Error)
        state as ExplainMoveUiState.Error
        assertEquals("The AI provider is unavailable.", state.message)
        assertNull(state.requestId)
    }

    @Test
    fun `backend failure carries the request id for technical details`() = runTest {
        stockLoaded()
        coEvery {
            backendRepository.explainMove(stock, news)
        } returns NetworkResult.Error(
            BackendApiException(
                code = "AI_PROVIDER_UNAVAILABLE",
                requestId = "req-42",
                message = "The AI provider is temporarily unavailable."
            )
        )

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.explainMove()
        advanceUntilIdle()

        val state = viewModel.explainState.value as ExplainMoveUiState.Error
        assertEquals("The AI provider is temporarily unavailable.", state.message)
        assertEquals("req-42", state.requestId)
    }

    @Test
    fun `missing stock makes no repository call and produces Error state`() = runTest {
        coEvery {
            stockRepository.getStockDetail("AAPL")
        } returns NetworkResult.Error(IllegalStateException("Stock unavailable"))

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.explainMove()
        advanceUntilIdle()

        assertTrue(viewModel.explainState.value is ExplainMoveUiState.Error)
        coVerify(exactly = 0) { backendRepository.explainMove(any(), any()) }
    }

    @Test
    fun `dismissing returns to idle`() = runTest {
        stockLoaded()
        coEvery {
            backendRepository.explainMove(stock, news)
        } returns NetworkResult.Success(explanation(evidenceSufficient = true))

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.explainMove()
        advanceUntilIdle()
        viewModel.dismissExplain()

        assertEquals(ExplainMoveUiState.Idle, viewModel.explainState.value)
    }

    @Test
    fun `news is sent as evidence when available`() = runTest {
        stockLoaded()
        coEvery {
            backendRepository.explainMove(any(), any())
        } returns NetworkResult.Success(explanation(evidenceSufficient = true))

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.explainMove()
        advanceUntilIdle()

        coVerify(exactly = 1) { backendRepository.explainMove(stock, news) }
    }
}

