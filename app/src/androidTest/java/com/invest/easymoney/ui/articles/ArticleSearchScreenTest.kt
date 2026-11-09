package com.invest.easymoney.ui.articles

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.invest.easymoney.domain.model.Article
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ArticleSearchScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun suggestedSearchSendsQueryIntent() {
        var intent: ArticleSearchIntent? = null
        composeRule.setContent {
            MaterialTheme {
                ArticleSearchContent(ArticleSearchUiState(), { intent = it }, {}, {})
            }
        }
        composeRule.onNodeWithText("Earnings").performClick()
        assertEquals(ArticleSearchIntent.QueryChanged("Earnings"), intent)
    }

    @Test
    fun typingAndClearingSearchUpdatesField() {
        composeRule.setContent {
            var state by remember { mutableStateOf(ArticleSearchUiState()) }
            MaterialTheme {
                ArticleSearchContent(
                    state = state,
                    onIntent = {
                        if (it is ArticleSearchIntent.QueryChanged) state = state.copy(query = it.query)
                    },
                    onBack = {},
                    onOpenArticle = {}
                )
            }
        }
        composeRule.onNodeWithTag("ArticleSearchInput").performTextInput("earnings")
        composeRule.onNodeWithTag("ArticleSearchInput").assertTextContains("earnings")
        composeRule.onNodeWithContentDescription("Clear article search").performClick()
        composeRule.onNodeWithTag("ArticleSearchInput").assertTextContains("")
        composeRule.onNodeWithContentDescription("Clear article search").assertDoesNotExist()
    }

    @Test
    fun resultsAcrossStocksHaveTopHitsAndOpenSelectedArticle() {
        val articles = (1..4).map { index ->
            Article("$index", "Company $index earnings", "Test publisher", "https://example.com/$index", 1700000000)
        }
        var opened: Article? = null
        composeRule.setContent {
            MaterialTheme {
                ArticleSearchContent(
                    ArticleSearchUiState(query = "earnings", articles = articles, hasSearched = true),
                    onIntent = {},
                    onBack = {},
                    onOpenArticle = { opened = it }
                )
            }
        }
        composeRule.onNodeWithText("Top Hits").assertIsDisplayed()
        composeRule.onNodeWithTag("ArticleSearchResults").performScrollToNode(
            androidx.compose.ui.test.hasTestTag("Article_4")
        )
        composeRule.onNodeWithTag("Article_4").performClick()
        assertEquals(articles.last(), opened)
    }

    @Test
    fun dateFilterAndRetrySendIntents() {
        var intent: ArticleSearchIntent? = null
        composeRule.setContent {
            MaterialTheme {
                ArticleSearchContent(
                    ArticleSearchUiState(query = "earnings", hasSearched = true, error = "Search unavailable"),
                    onIntent = { intent = it },
                    onBack = {},
                    onOpenArticle = {}
                )
            }
        }
        composeRule.onNodeWithText("Search unavailable").assertIsDisplayed()
        composeRule.onNodeWithText("Retry article search").performClick()
        assertEquals(ArticleSearchIntent.Retry, intent)
        composeRule.onNodeWithText("Past week").performClick()
        assertEquals(ArticleSearchIntent.FilterChanged(ArticleDateFilter.PAST_WEEK), intent)
    }
}
