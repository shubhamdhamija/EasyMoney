package com.invest.easymoney.ui.home
import app.cash.turbine.test
import com.invest.easymoney.MainDispatcherRule
import com.invest.easymoney.data.repository.FakeAiRepository
import com.invest.easymoney.data.repository.FakeStockRepository
import com.invest.easymoney.domain.model.StockSearchResult
import com.invest.easymoney.domain.repository.AiInsightRepository
import com.invest.easymoney.domain.repository.StockRepository
import com.invest.easymoney.util.Resource
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    @Test
    fun search_success_mockk() = runTest {
        val stockRepository = mockk<StockRepository>()
        val aiRepository = mockk<AiInsightRepository>(relaxed = true)
        every {
            stockRepository.searchSymbols("AAPL")
        } returns flowOf(
            Resource.Success(
                listOf(
                    StockSearchResult("AAPL", "Apple Inc.", "NASDAQ")
                )
            )
        )
        coEvery {
            stockRepository.getTopStocks()
        } returns Resource.Success(emptyList())
        val viewModel = HomeViewModel(stockRepository, aiRepository)
        advanceUntilIdle()
        viewModel.uiState.test {
            awaitItem()
            viewModel.onIntent(HomeIntent.SearchChanged("AAPL"))
            assertEquals("AAPL", awaitItem().search.query)
            advanceTimeBy(501.milliseconds)
            advanceUntilIdle()
            val successState = awaitItem()
            assertFalse(successState.search.isLoading)
            assertEquals(1, successState.search.results.size)
            assertEquals("AAPL", successState.search.results.first().symbol)
        }
    }
    @Test
    fun search_loading_then_success() = runTest {
        val stockRepository = FakeStockRepository()
        val aiRepository = FakeAiRepository()
        val viewModel = HomeViewModel(stockRepository, aiRepository)
        advanceUntilIdle()
        viewModel.uiState.test {
            awaitItem()
            viewModel.onIntent(HomeIntent.SearchChanged("AAPL"))
            assertEquals("AAPL", awaitItem().search.query)
            advanceTimeBy(501.milliseconds)
            assertTrue(awaitItem().search.isLoading)
            advanceTimeBy(1000.milliseconds)
            advanceUntilIdle()
            val successState = awaitItem()
            assertFalse(successState.search.isLoading)
            assertEquals("AAPL", successState.search.results.first().symbol)
            assertEquals("AAPL Inc", successState.search.results.first().name)
        }
    }
    @Test
    fun blank_query_clears_results() = runTest {
        val stockRepository = FakeStockRepository()
        val aiRepository = FakeAiRepository()
        val viewModel = HomeViewModel(stockRepository, aiRepository)
        advanceUntilIdle()
        viewModel.uiState.test {
            awaitItem()
            viewModel.onIntent(HomeIntent.SearchChanged("AAPL"))
            assertEquals("AAPL", awaitItem().search.query)
            advanceTimeBy(501.milliseconds)
            assertTrue(awaitItem().search.isLoading)
            advanceTimeBy(1000.milliseconds)
            advanceUntilIdle()
            assertEquals("AAPL", awaitItem().search.results.first().symbol)
            viewModel.onIntent(HomeIntent.SearchChanged(""))
            assertEquals("", awaitItem().search.query)
            advanceTimeBy(501.milliseconds)
            advanceUntilIdle()
            val clearedState = awaitItem()
            assertFalse(clearedState.search.isLoading)
            assertTrue(clearedState.search.results.isEmpty())
        }
    }
}
