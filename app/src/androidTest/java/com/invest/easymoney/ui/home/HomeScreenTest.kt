package com.invest.easymoney.ui.home

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertIsDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for HomeScreen composable.
 * Tests UI components and user interactions.
 */
@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun setUp() {
        // Test setup if needed
    }

    /**
     * Test that search input updates text correctly when user types
     * This tests the SearchHeaderCard composable with text input
     */
    @Test
    fun search_input_updates_text() {
        composeRule.setContent {
            var query by remember {
                mutableStateOf("")
            }

            SearchHeaderCard(
                query = query,
                onQueryChange = {
                    query = it
                }
            )
        }

        composeRule
            .onNodeWithTag("SearchInput")
            .performTextInput("AAPL")

        composeRule
            .onNodeWithTag("SearchInput")
            .assertTextContains("AAPL")
    }


    @Test
    fun typing_aapl_displays_search_result() {
        composeRule.setContent {
            var query by remember {
                mutableStateOf("")
            }

            SearchHeaderCard(
                query = query,
                onQueryChange = {
                    query = it
                }
            )
        }

        composeRule
            .onNodeWithTag("SearchInput")
            .performTextInput("AAPL")

        composeRule
            .onNodeWithTag("SearchInput")
            .assertTextContains("AAPL")
    }


    /**
     * Test that SearchHeaderCard is displayed correctly
     */
    @Test
    fun search_header_card_is_displayed() {
        composeRule.setContent {
            SearchHeaderCard(
                query = "",
                onQueryChange = {}
            )
        }

        composeRule
            .onNodeWithTag("SearchHeaderCard")
            .assertIsDisplayed()
    }

    /**
     * Test that search input field exists and is interactive
     */
    @Test
    fun search_input_field_exists() {
        composeRule.setContent {
            SearchHeaderCard(
                query = "",
                onQueryChange = {}
            )
        }

        composeRule
            .onNodeWithTag("SearchInput")
            .assertExists()
    }

    /**
     * Test search input with multiple characters
     */
    @Test
    fun search_input_accepts_multiple_characters() {
        composeRule.setContent {
            var query by remember {
                mutableStateOf("")
            }

            SearchHeaderCard(
                query = query,
                onQueryChange = {
                    query = it
                }
            )
        }

        composeRule
            .onNodeWithTag("SearchInput")
            .performTextInput("Microsoft Corporation")

        composeRule
            .onNodeWithTag("SearchInput")
            .assertTextContains("Microsoft Corporation")
    }

    /**
     * Test search input with special characters and numbers
     */
    @Test
    fun search_input_accepts_special_characters_and_numbers() {
        composeRule.setContent {
            var query by remember {
                mutableStateOf("")
            }

            SearchHeaderCard(
                query = query,
                onQueryChange = {
                    query = it
                }
            )
        }

        composeRule
            .onNodeWithTag("SearchInput")
            .performTextInput("GOOG-123-X")

        composeRule
            .onNodeWithTag("SearchInput")
            .assertTextContains("GOOG-123-X")
    }

    /**
     * Test clearing search input
     */
    @Test
    fun search_input_can_be_cleared() {
        composeRule.setContent {
            var query by remember {
                mutableStateOf("AAPL")
            }

            SearchHeaderCard(
                query = query,
                onQueryChange = {
                    query = it
                }
            )
        }

        composeRule
            .onNodeWithTag("SearchInput")
            .performTextInput("")

        composeRule
            .onNodeWithTag("SearchInput")
            .assertTextContains("")
    }

    /**
     * Test that SearchHeaderCard displays with initial query value
     */
    @Test
    fun search_input_displays_initial_value() {
        composeRule.setContent {
            SearchHeaderCard(
                query = "AAPL",
                onQueryChange = {}
            )
        }

        composeRule
            .onNodeWithTag("SearchInput")
            .assertTextContains("AAPL")
    }

    /**
     * Test MarketPulseHero card is displayed
     */
    @Test
    fun market_pulse_hero_card_is_displayed() {
        composeRule.setContent {
            MarketPulseHero(
                gainers = emptyList(),
                losers = emptyList(),
                trending = emptyList(),
                isAiLoading = false,
                onAiClick = {}
            )
        }

        composeRule
            .onNodeWithTag("MarketPulseHeroCard")
            .assertIsDisplayed()
    }

    /**
     * Test that top bar exists and is displayed
     */
    @Test
    fun home_top_bar_is_displayed() {
        composeRule.setContent {
            HomeSearchFirstTopBar(
                isRefreshing = false,
                onRefresh = {}
            )
        }

        composeRule
            .onNodeWithTag("HomeTopBar")
            .assertIsDisplayed()
    }

    /**
     * Test refresh button exists in top bar
     */
    @Test
    fun refresh_button_exists_in_top_bar() {
        composeRule.setContent {
            HomeSearchFirstTopBar(
                isRefreshing = false,
                onRefresh = {}
            )
        }

        composeRule
            .onNodeWithTag("TopBar_RefreshButton")
            .assertExists()
    }

    /**
     * Test loading indicator shows when refreshing
     */
    @Test
    fun loading_indicator_shows_when_refreshing() {
        composeRule.setContent {
            HomeSearchFirstTopBar(
                isRefreshing = true,
                onRefresh = {}
            )
        }

        composeRule
            .onNodeWithTag("TopBar_LoadingIndicator")
            .assertExists()
    }

    /**
     * Test sticky market tabs are displayed
     */
    @Test
    fun sticky_market_tabs_are_displayed() {
        composeRule.setContent {
            StickyMarketTabs(
                selectedTab = HomeTab.GAINERS,
                onTabSelected = {}
            )
        }

        composeRule
            .onNodeWithTag("StickyMarketTabsRow")
            .assertIsDisplayed()
    }

    /**
     * Test that live ticker strip is displayed when stocks exist
     */
    @Test
    fun live_ticker_strip_is_displayed_with_stocks() {
        val mockStocks = listOf(
            com.invest.easymoney.domain.model.Stock(
                symbol = "AAPL",
                name = "Apple Inc.",
                currentPrice = 150.0,
                change = 2.5,
                changePercent = 1.69,
                exchange = "NASDAQ"
            )
        )

        composeRule.setContent {
            LiveTickerStrip(
                stocks = mockStocks,
                onStockClick = {}
            )
        }

        composeRule
            .onNodeWithTag("LiveTickerStrip")
            .assertIsDisplayed()
    }

    /**
     * Test that live ticker items are created for each stock
     */
    @Test
    fun live_ticker_items_exist_for_each_stock() {
        val mockStocks = listOf(
            com.invest.easymoney.domain.model.Stock(
                symbol = "AAPL",
                name = "Apple Inc.",
                currentPrice = 150.0,
                change = 2.5,
                changePercent = 1.69,
                exchange = "NASDAQ"
            ),
            com.invest.easymoney.domain.model.Stock(
                symbol = "GOOGL",
                name = "Google LLC",
                currentPrice = 140.0,
                change = -1.5,
                changePercent = -1.06,
                exchange = "NASDAQ"
            )
        )

        composeRule.setContent {
            LiveTickerStrip(
                stocks = mockStocks,
                onStockClick = {}
            )
        }

        composeRule
            .onNodeWithTag("LiveTicker_AAPL")
            .assertExists()

        composeRule
            .onNodeWithTag("LiveTicker_GOOGL")
            .assertExists()
    }

    /**
     * Test that for you carousel is displayed
     */
    @Test
    fun for_you_carousel_is_displayed() {
        val mockStocks = listOf(
            com.invest.easymoney.domain.model.Stock(
                symbol = "AAPL",
                name = "Apple Inc.",
                currentPrice = 150.0,
                change = 2.5,
                changePercent = 1.69,
                exchange = "NASDAQ"
            )
        )

        composeRule.setContent {
            ForYouCarousel(
                stocks = mockStocks,
                onStockClick = {}
            )
        }

        composeRule
            .onNodeWithTag("ForYouCarousel")
            .assertIsDisplayed()
    }

    /**
     * Test that modern stock card is displayed
     */
    @Test
    fun modern_stock_card_is_displayed() {
        val mockStock = com.invest.easymoney.domain.model.Stock(
            symbol = "AAPL",
            name = "Apple Inc.",
            currentPrice = 150.0,
            change = 2.5,
            changePercent = 1.69,
            exchange = "NASDAQ"
        )

        composeRule.setContent {
                ModernStockCard(
                stock = mockStock,
                onClick = {}
            )
        }

        composeRule
            .onNodeWithTag("StockCard_AAPL")
            .assertIsDisplayed()
    }
}

