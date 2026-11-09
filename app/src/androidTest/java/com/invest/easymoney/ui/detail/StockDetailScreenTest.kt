package com.invest.easymoney.ui.detail

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.invest.easymoney.data.local.entity.WatchlistEntity
import com.invest.easymoney.domain.model.Alert
import com.invest.easymoney.domain.model.AiPick
import com.invest.easymoney.domain.model.EarningsReport
import com.invest.easymoney.domain.model.EvidenceSentiment
import com.invest.easymoney.domain.model.MovementDirection
import com.invest.easymoney.domain.model.NetworkResult
import com.invest.easymoney.domain.model.News
import com.invest.easymoney.domain.model.Stock
import com.invest.easymoney.domain.model.StockExplanation
import com.invest.easymoney.domain.model.StockInsight
import com.invest.easymoney.domain.model.StockSearchResult
import com.invest.easymoney.domain.model.UpcomingEarning
import com.invest.easymoney.domain.repository.AiInsightRepository
import com.invest.easymoney.domain.repository.BackendRepository
import com.invest.easymoney.domain.repository.StockRepository
import com.invest.easymoney.util.Resource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger

/**
 * Screen-level tests for tab selection and the two distinct Retry controls.
 * Uses hand-written fakes so call counts are observable.
 */
@RunWith(AndroidJUnit4::class)
class StockDetailScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

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
            url = "",
            summary = "",
            imageUrl = "",
            datetime = 0L
        )
    )

    private class FakeStockRepository(
        private val stock: Stock,
        private val news: List<News>
    ) : StockRepository {
        override suspend fun getTopStocks(): Resource<List<Stock>> = Resource.Success(emptyList())
        override suspend fun getStockDetail(symbol: String): NetworkResult<Stock> = NetworkResult.Success(stock)
        override suspend fun getNews(symbol: String): NetworkResult<List<News>> = NetworkResult.Success(news)
        override suspend fun fetchStocksForSymbols(symbols: List<String>): Resource<List<Stock>> =
            Resource.Success(emptyList())
        override fun searchSymbols(query: String): Flow<Resource<List<StockSearchResult>>> = emptyFlow()
        override suspend fun getPopularStocksFromApi(): Resource<List<String>> = Resource.Success(emptyList())
        override fun getWatchlist(): Flow<List<WatchlistEntity>> = emptyFlow()
        override fun getWatchlistSymbols(): Flow<List<String>> = emptyFlow()
        override suspend fun addToWatchlist(symbol: String, companyName: String) = Unit
        override suspend fun removeFromWatchlist(symbol: String) = Unit
        override suspend fun isInWatchlist(symbol: String): Boolean = false
        override fun getAlerts(): Flow<List<Alert>> = emptyFlow()
        override suspend fun addAlert(alert: Alert) = Unit
        override suspend fun deleteAlert(alertId: Int) = Unit
        override suspend fun getAllAlertsOnce(): List<Alert> = emptyList()
    }

    /** Legacy insight always fails so its own Retry control is visible. */
    private class FakeAiInsightRepository : AiInsightRepository {
        val insightCalls = AtomicInteger(0)

        override suspend fun getStockInsight(
            symbol: String,
            stock: Stock,
            news: List<News>
        ): NetworkResult<StockInsight> {
            insightCalls.incrementAndGet()
            return NetworkResult.Error(IllegalStateException("AI insight is temporarily unavailable."))
        }

        override suspend fun getAiPicks(stocks: List<Stock>): Resource<List<AiPick>> =
            Resource.Success(emptyList())
    }

    private class FakeBackendRepository(
        private val results: (call: Int) -> NetworkResult<StockExplanation>
    ) : BackendRepository {
        val explainCalls = AtomicInteger(0)

        /** Calls numbered >= this value suspend until [gate] completes. */
        var gateFromCall: Int = Int.MAX_VALUE
        val gate = CompletableDeferred<Unit>()

        override suspend fun notifyWatchlistChange(deviceId: String, symbol: String, action: String) = Unit

        override suspend fun explainMove(stock: Stock, news: List<News>): NetworkResult<StockExplanation> {
            val call = explainCalls.incrementAndGet()
            if (call >= gateFromCall) gate.await()
            return results(call)
        }

        override suspend fun getEarningsReport(symbol: String): Resource<EarningsReport> =
            Resource.Error("unused")

        override suspend fun getUpcomingEarnings(symbols: List<String>): Resource<List<UpcomingEarning>> =
            Resource.Success(emptyList())
    }

    private fun successExplanation() = StockExplanation(
        symbol = "AAPL",
        direction = MovementDirection.INCREASE,
        evidenceSentiment = EvidenceSentiment.POSITIVE,
        summary = "Revenue evidence may have contributed.",
        factors = listOf("Revenue exceeded the estimate."),
        evidenceSufficient = true,
        disclaimer = "For informational purposes only.",
        requestId = null
    )

    private fun createViewModel(
        ai: FakeAiInsightRepository,
        backend: FakeBackendRepository
    ): StockDetailViewModel {
        var viewModel: StockDetailViewModel? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            viewModel = StockDetailViewModel(
                repository = FakeStockRepository(stock, news),
                aiRepository = ai,
                backendRepository = backend,
                savedStateHandle = SavedStateHandle(mapOf("symbol" to "AAPL")),
                deviceId = "test-device"
            )
        }
        return checkNotNull(viewModel)
    }

    private fun waitForChip() {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTagExists(StockDetailTestTags.EXPLAIN_MOVE_CHIP)
        }
    }

    private fun androidx.compose.ui.test.junit4.ComposeTestRule.onAllNodesWithTagExists(tag: String): Boolean {
        // The AI tab's items are lazily composed; scroll the card into the composition first.
        if (tag != StockDetailTestTags.EXPLAIN_MOVE_CHIP &&
            onAllNodes(androidx.compose.ui.test.hasTestTag(StockDetailTestTags.TAB_AI_INSIGHT))
                .fetchSemanticsNodes().isNotEmpty()
        ) {
            runCatching {
                onNodeWithTag(StockDetailTestTags.LIST)
                    .performScrollToNode(androidx.compose.ui.test.hasTestTag(ExplainMoveTestTags.CARD))
            }
        }
        return onAllNodes(androidx.compose.ui.test.hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()
    }

    @Test
    fun explain_move_chip_selects_ai_tab_and_starts_request() {
        val ai = FakeAiInsightRepository()
        val backend = FakeBackendRepository { NetworkResult.Success(successExplanation()) }
        val viewModel = createViewModel(ai, backend)

        composeRule.setContent {
            MaterialTheme { StockDetailScreen(onBack = {}, viewModel = viewModel) }
        }
        waitForChip()

        composeRule.onNodeWithTag(StockDetailTestTags.TAB_OVERVIEW).assertIsSelected()
        assertEquals(0, backend.explainCalls.get())

        composeRule.onNodeWithTag(StockDetailTestTags.EXPLAIN_MOVE_CHIP).performScrollTo().performClick()

        composeRule.onNodeWithTag(StockDetailTestTags.TAB_AI_INSIGHT).assertIsSelected()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTagExists(ExplainMoveTestTags.HEADLINE)
        }
        composeRule.onNodeWithTag(ExplainMoveTestTags.CARD).performScrollTo().assertIsDisplayed()
        assertEquals(1, backend.explainCalls.get())
    }

    @Test
    fun selecting_ai_tab_alone_does_not_request_an_explanation() {
        val ai = FakeAiInsightRepository()
        val backend = FakeBackendRepository { NetworkResult.Success(successExplanation()) }
        val viewModel = createViewModel(ai, backend)

        composeRule.setContent {
            MaterialTheme { StockDetailScreen(onBack = {}, viewModel = viewModel) }
        }
        waitForChip()

        composeRule.onNodeWithTag(StockDetailTestTags.TAB_AI_INSIGHT).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(StockDetailTestTags.TAB_AI_INSIGHT).assertIsSelected()
        assertEquals(0, backend.explainCalls.get())
    }

    @Test
    fun selected_tab_and_explanation_survive_recreation() {
        val ai = FakeAiInsightRepository()
        val backend = FakeBackendRepository { NetworkResult.Success(successExplanation()) }
        val viewModel = createViewModel(ai, backend)
        val restoration = StateRestorationTester(composeRule)

        restoration.setContent {
            MaterialTheme { StockDetailScreen(onBack = {}, viewModel = viewModel) }
        }
        waitForChip()

        InstrumentationRegistry.getInstrumentation().runOnMainSync { viewModel.explainMove() }
        composeRule.onNodeWithTag(StockDetailTestTags.TAB_AI_INSIGHT).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTagExists(ExplainMoveTestTags.HEADLINE)
        }

        restoration.emulateSavedInstanceStateRestore()

        composeRule.onNodeWithTag(StockDetailTestTags.TAB_AI_INSIGHT).assertIsSelected()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTagExists(ExplainMoveTestTags.HEADLINE)
        }
        composeRule.onNodeWithTag(ExplainMoveTestTags.HEADLINE).assertIsDisplayed()
        assertEquals(1, backend.explainCalls.get())
    }

    @Test
    fun explanation_retry_invokes_only_the_explanation_retry() {
        val ai = FakeAiInsightRepository()
        val backend = FakeBackendRepository { call ->
            if (call == 1) NetworkResult.Error(IllegalStateException("Explanation unavailable."))
            else NetworkResult.Success(successExplanation())
        }
        val viewModel = createViewModel(ai, backend)

        composeRule.setContent {
            MaterialTheme { StockDetailScreen(onBack = {}, viewModel = viewModel) }
        }
        waitForChip()
        val legacyCallsBefore = ai.insightCalls.get()

        composeRule.onNodeWithTag(StockDetailTestTags.EXPLAIN_MOVE_CHIP).performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTagExists(ExplainMoveTestTags.RETRY)
        }
        assertEquals(1, backend.explainCalls.get())

        composeRule.onNodeWithTag(ExplainMoveTestTags.RETRY).performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTagExists(ExplainMoveTestTags.HEADLINE)
        }

        assertEquals(2, backend.explainCalls.get())
        assertEquals(legacyCallsBefore, ai.insightCalls.get())
    }

    @Test
    fun retry_replaces_the_error_with_loading_until_the_result_arrives() {
        val ai = FakeAiInsightRepository()
        val backend = FakeBackendRepository { call ->
            if (call == 1) NetworkResult.Error(IllegalStateException("Explanation unavailable."))
            else NetworkResult.Success(successExplanation())
        }
        backend.gateFromCall = 2
        val viewModel = createViewModel(ai, backend)

        composeRule.setContent {
            MaterialTheme { StockDetailScreen(onBack = {}, viewModel = viewModel) }
        }
        waitForChip()

        composeRule.onNodeWithTag(StockDetailTestTags.EXPLAIN_MOVE_CHIP).performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTagExists(ExplainMoveTestTags.RETRY)
        }
        composeRule.onNodeWithTag(ExplainMoveTestTags.ERROR).assertExists()

        composeRule.onNodeWithTag(ExplainMoveTestTags.RETRY).performScrollTo().performClick()

        // The request is held open: only the loading state may be visible.
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTagExists(ExplainMoveTestTags.LOADING)
        }
        composeRule.onNodeWithTag(ExplainMoveTestTags.ERROR).assertDoesNotExist()
        composeRule.onNodeWithTag(ExplainMoveTestTags.RETRY).assertDoesNotExist()

        backend.gate.complete(Unit)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTagExists(ExplainMoveTestTags.HEADLINE)
        }
        composeRule.onNodeWithTag(ExplainMoveTestTags.LOADING).assertDoesNotExist()
        composeRule.onNodeWithTag(ExplainMoveTestTags.ERROR).assertDoesNotExist()
    }
}

