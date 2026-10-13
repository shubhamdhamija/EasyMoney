package com.invest.easymoney.ui.home

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeSearchFirstTopBarTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun topBar_is_displayed() {
        composeRule.setContent {
            ThemedTopBar(isRefreshing = false, onRefresh = {})
        }

        composeRule.onNodeWithTag("HomeTopBar").assertIsDisplayed()
    }

    @Test
    fun refresh_button_is_displayed_when_not_refreshing() {
        composeRule.setContent {
            ThemedTopBar(isRefreshing = false, onRefresh = {})
        }

        composeRule.onNodeWithTag("TopBar_RefreshButton").assertIsDisplayed()
    }

    @Test
    fun loading_indicator_is_displayed_when_refreshing() {
        composeRule.setContent {
            ThemedTopBar(isRefreshing = true, onRefresh = {})
        }

        composeRule.onNodeWithTag("TopBar_LoadingIndicator").assertIsDisplayed()
    }

    @Test
    fun refresh_button_triggers_callback() {
        var clicked = false

        composeRule.setContent {
            ThemedTopBar(isRefreshing = false, onRefresh = { clicked = true })
        }

        composeRule.onNodeWithTag("TopBar_RefreshButton").performClick()

        assertTrue(clicked)
    }

    @Composable
    private fun ThemedTopBar(isRefreshing: Boolean, onRefresh: () -> Unit) {
        MaterialTheme {
            HomeSearchFirstTopBar(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh
            )
        }
    }
}

