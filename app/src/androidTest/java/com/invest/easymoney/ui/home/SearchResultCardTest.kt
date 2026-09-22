package com.invest.easymoney.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import junit.framework.TestCase.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SearchResultCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun search_result_is_displayed() {
        composeRule.setContent {
            MaterialTheme {
                SearchResultCard(
                    symbol = "AAPL",
                    name = "Apple Inc.",
                    exchange = "NASDAQ",
                    onClick = {}
                )
            }
        }

        composeRule
            .onNodeWithTag("SearchResultCard_AAPL")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText("AAPL")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText("Apple Inc.")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText("NASDAQ")
            .assertIsDisplayed()
    }

    @Test
    fun search_result_clicks_stock() {

        var clickedSymbol: String? = null

        composeRule.setContent {

            MaterialTheme {

                SearchResultCard(
                    symbol = "AAPL",
                    name = "Apple Inc.",
                    exchange = "NASDAQ",
                    onClick = {
                        clickedSymbol = "AAPL"
                    }
                )
            }
        }

        composeRule
            .onNodeWithTag("SearchResultCard_AAPL")
            .performClick()

        assertEquals(
            "AAPL",
            clickedSymbol
        )
    }

    @Test
    fun typing_symbol_shows_matching_search_result() {
        composeRule.setContent {
            MaterialTheme {
                SearchContent()
            }
        }

        composeRule
            .onAllNodesWithTag("SearchResultCard_AAPL")
            .assertCountEquals(0)

        composeRule
            .onNodeWithTag("SearchInput")
            .performTextInput("AAPL")

        composeRule
            .onNodeWithTag("SearchResultCard_AAPL")
            .assertIsDisplayed()

        composeRule
            .onNodeWithText("Apple Inc")
            .assertIsDisplayed()
    }

    @Composable
    private fun SearchContent() {
        var query by remember {
            mutableStateOf("")
        }

        Column {
            SearchHeaderCard(
                query = query,
                onQueryChange = {
                    query = it
                }
            )

            if (query == "AAPL") {
                SearchResultCard(
                    symbol = "AAPL",
                    name = "Apple Inc",
                    exchange = "NASDAQ",
                    onClick = {}
                )
            }
        }
    }






}

