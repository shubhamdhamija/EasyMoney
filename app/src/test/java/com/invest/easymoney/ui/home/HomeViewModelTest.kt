package com.invest.easymoney.ui.home

import app.cash.turbine.test
import com.invest.easymoney.data.repository.FakeAiRepository
import com.invest.easymoney.data.repository.FakeStockRepository
import com.invest.easymoney.domain.model.*
import com.invest.easymoney.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private lateinit var viewModel: HomeViewModel
    private lateinit var stockRepository: FakeStockRepository
    private lateinit var aiRepository: FakeAiRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        stockRepository = FakeStockRepository()
        aiRepository = FakeAiRepository()
        viewModel = HomeViewModel(stockRepository, aiRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `Search query changes, Results returned, UiState updated`() = runTest {
        val searchResults = listOf(
            StockSearchResult("AAPL", "Apple Inc.", "NASDAQ")
        )
        stockRepository.searchResult = Resource.Success(searchResults)

        val viewModel =

        HomeViewModel(

                    repository = stockRepository,
                    aiRepository = aiRepository

        )
            viewModel.onIntent(HomeIntent.SearchChanged("AAPL"))


            advanceTimeBy(501.milliseconds)
        advanceUntilIdle()

        println(viewModel.uiState.value.search)
        assertTrue(
                    viewModel.uiState.value.search.results.isNotEmpty()
        )
        assertEquals(

            "AAPL",

                    viewModel.uiState.value

                        .search

                .results.first()
                .symbol

        )

    }

    @Test
    fun search_loading_then_success() = runTest {

        stockRepository.searchResult =
            Resource.Success(
                listOf(
                    StockSearchResult(
                        "AAPL",
                        "Apple",
                        "NASDAQ"
                    )
                )
            )

        viewModel.uiState.test {

            awaitItem() // Initial state

            viewModel.onIntent(
                HomeIntent.SearchChanged(
                    "AAPL"
                )
            )

            val queryUpdatedState = awaitItem()
            assertEquals(
                "AAPL",
                queryUpdatedState.search.query
            )
            assertFalse(
                queryUpdatedState.search.isLoading
            )

            advanceTimeBy(501.milliseconds)

            val loadingState =
                awaitItem()

            assertTrue(
                loadingState.search.isLoading
            )

            advanceUntilIdle()

            val successState =
                awaitItem()

            assertFalse(
                successState.search.isLoading
            )

            assertEquals(
                "AAPL",
                successState.search
                    .results
                    .first()
                    .symbol
            )

        }
    }

    @Test
    fun `Repository returns error, UiState error updated`() = runTest {
        val errorMessage = "Network Error"
        stockRepository.searchResult = Resource.Error(errorMessage)

        viewModel.uiState.test {
            awaitItem() // Initial
            viewModel.onIntent(HomeIntent.SearchChanged("AAPL"))
            awaitItem() // query change

            advanceTimeBy(501.milliseconds)

            assertTrue(awaitItem().search.isLoading) // loading

            val errorState = awaitItem()
            assertEquals(errorMessage, errorState.search.error)
            assertFalse(errorState.search.isLoading)
        }
    }

    @Test
    fun `Search query blank, Results cleared`() = runTest {
        viewModel.uiState.test {
            awaitItem() // Initial

            // 1. Set to non-blank to get some results first
            stockRepository.searchResult = Resource.Success(listOf(StockSearchResult("A", "Alpha", "X")))
            viewModel.onIntent(HomeIntent.SearchChanged("A"))
            awaitItem() // query A
            advanceTimeBy(501.milliseconds)
            assertTrue(awaitItem().search.isLoading)
            assertEquals("Alpha", awaitItem().search.results.first().name)

            // 2. Now clear it
            viewModel.onIntent(HomeIntent.SearchChanged(""))
            assertEquals("", awaitItem().search.query)

            advanceTimeBy(501.milliseconds)

            val state = awaitItem()
            assertEquals(emptyList<StockSearchResult>(), state.search.results)
            assertFalse(state.search.isLoading)
        }
    }

    @Test
    fun `AAPL then TSLA quickly, Only TSLA results visible`() = runTest {
        val tslaResults = listOf(StockSearchResult("TSLA", "Tesla", "NASDAQ"))
        stockRepository.searchResult = Resource.Success(tslaResults)

        viewModel.uiState.test {
            awaitItem() // Initial

            // Search AAPL
            viewModel.onIntent(HomeIntent.SearchChanged("AAPL"))
            awaitItem() // AAPL query

            advanceTimeBy(200.milliseconds)

            // Search TSLA
            viewModel.onIntent(HomeIntent.SearchChanged("TSLA"))
            awaitItem() // TSLA query

            advanceTimeBy(501.milliseconds)

            assertTrue(awaitItem().search.isLoading) // Loading for TSLA

            val finalState = awaitItem()
            assertEquals(tslaResults, finalState.search.results)
            assertEquals("TSLA", finalState.search.query)
        }
    }
}
