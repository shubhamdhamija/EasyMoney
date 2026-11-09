package com.invest.easymoney.ui.detail

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.invest.easymoney.domain.model.EvidenceSentiment
import com.invest.easymoney.domain.model.MovementDirection
import com.invest.easymoney.domain.model.StockExplanation
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExplainMoveCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val disclaimer = "For informational purposes only."

    private fun explanation(
        sufficient: Boolean = true,
        sentiment: EvidenceSentiment = EvidenceSentiment.POSITIVE,
        direction: MovementDirection = MovementDirection.INCREASE,
        summary: String = "Revenue exceeded the supplied estimate, which may have contributed to the increase.",
        factors: List<String> = listOf(
            "Quarterly revenue exceeded the supplied estimate.",
            "Lower guidance may temper the upside."
        )
    ) = StockExplanation(
        symbol = "AAPL",
        direction = direction,
        evidenceSentiment = sentiment,
        summary = summary,
        factors = factors,
        evidenceSufficient = sufficient,
        disclaimer = disclaimer,
        requestId = "internal-request-id-should-not-render"
    )

    private fun show(
        state: ExplainMoveUiState,
        onExplain: () -> Unit = {},
        onDismiss: () -> Unit = {}
    ) {
        composeRule.setContent {
            MaterialTheme {
                ExplainMoveCard(
                    state = state,
                    onExplain = onExplain,
                    onDismiss = onDismiss
                )
            }
        }
    }

    // --- Success ------------------------------------------------------------

    @Test
    fun success_shows_card_headline_summary_and_factors() {
        val value = explanation()
        show(ExplainMoveUiState.Success(value))

        composeRule.onNodeWithTag(ExplainMoveTestTags.CARD).assertIsDisplayed()

        composeRule.onNodeWithTag(ExplainMoveTestTags.HEADLINE)
            .assertIsDisplayed()
            .assertTextEquals("Positive evidence • Stock increased")

        composeRule.onNodeWithTag(ExplainMoveTestTags.SUMMARY)
            .assertIsDisplayed()
            .assertTextContains(value.summary)

        composeRule.onNodeWithTag(ExplainMoveTestTags.FACTORS).assertIsDisplayed()
        composeRule.onNodeWithText("Key factors").assertIsDisplayed()
    }

    @Test
    fun success_renders_each_factor_as_a_separate_bullet() {
        val value = explanation()
        show(ExplainMoveUiState.Success(value))

        value.factors.forEach { factor ->
            composeRule.onNodeWithText("• $factor").assertIsDisplayed()
        }
    }

    @Test
    fun success_shows_the_disclaimer_exactly_once() {
        show(ExplainMoveUiState.Success(explanation()))

        composeRule.onAllNodesWithTag(ExplainMoveTestTags.DISCLAIMER).assertCountEquals(1)
        composeRule.onAllNodesWithText(disclaimer).assertCountEquals(1)
        composeRule.onNodeWithTag(ExplainMoveTestTags.DISCLAIMER).assertTextEquals(disclaimer)
    }

    @Test
    fun success_headline_reflects_decrease_and_negative_evidence() {
        show(
            ExplainMoveUiState.Success(
                explanation(
                    sentiment = EvidenceSentiment.NEGATIVE,
                    direction = MovementDirection.DECREASE
                )
            )
        )

        composeRule.onNodeWithTag(ExplainMoveTestTags.HEADLINE)
            .assertTextEquals("Negative evidence • Stock decreased")
    }

    @Test
    fun success_headline_reflects_mixed_evidence() {
        show(ExplainMoveUiState.Success(explanation(sentiment = EvidenceSentiment.MIXED)))

        composeRule.onNodeWithTag(ExplainMoveTestTags.HEADLINE)
            .assertTextEquals("Mixed evidence • Stock increased")
    }

    @Test
    fun success_hides_the_factors_section_when_there_are_no_factors() {
        show(ExplainMoveUiState.Success(explanation(factors = emptyList())))

        composeRule.onNodeWithTag(ExplainMoveTestTags.SUMMARY).assertIsDisplayed()
        composeRule.onNodeWithTag(ExplainMoveTestTags.FACTORS).assertDoesNotExist()
        composeRule.onNodeWithText("Key factors").assertDoesNotExist()
    }

    @Test
    fun long_summary_is_fully_available_to_the_user() {
        val longSummary = "Revenue exceeded the supplied estimate. ".repeat(12).trim()
        show(ExplainMoveUiState.Success(explanation(summary = longSummary)))

        composeRule.onNodeWithTag(ExplainMoveTestTags.SUMMARY)
            .assertIsDisplayed()
            .assertTextEquals(longSummary)
    }

    @Test
    fun clear_button_dismisses_a_result() {
        var dismissed = 0
        show(
            state = ExplainMoveUiState.Success(explanation()),
            onDismiss = { dismissed++ }
        )

        composeRule.onNodeWithText("Clear").performClick()

        assertEquals(1, dismissed)
    }

    // --- Insufficient evidence ---------------------------------------------

    @Test
    fun insufficient_evidence_shows_an_honest_neutral_result() {
        val value = explanation(
            sufficient = false,
            sentiment = EvidenceSentiment.INSUFFICIENT,
            summary = "The available evidence is insufficient to reliably explain this movement.",
            factors = emptyList()
        )
        show(ExplainMoveUiState.InsufficientEvidence(value))

        composeRule.onNodeWithTag(ExplainMoveTestTags.CARD).assertIsDisplayed()

        composeRule.onNodeWithTag(ExplainMoveTestTags.HEADLINE)
            .assertTextEquals("Insufficient evidence • Stock increased")

        composeRule.onNodeWithTag(ExplainMoveTestTags.SUMMARY)
            .assertIsDisplayed()
            .assertTextContains(value.summary)
    }

    @Test
    fun insufficient_evidence_is_not_presented_as_an_error() {
        show(
            ExplainMoveUiState.InsufficientEvidence(
                explanation(
                    sufficient = false,
                    sentiment = EvidenceSentiment.INSUFFICIENT,
                    factors = emptyList()
                )
            )
        )

        composeRule.onNodeWithTag(ExplainMoveTestTags.ERROR).assertDoesNotExist()
        composeRule.onNodeWithTag(ExplainMoveTestTags.REQUEST_ID).assertDoesNotExist()
        composeRule.onNodeWithText("Retry").assertDoesNotExist()
    }

    @Test
    fun insufficient_evidence_hides_factors_and_keeps_one_disclaimer() {
        show(
            ExplainMoveUiState.InsufficientEvidence(
                explanation(
                    sufficient = false,
                    sentiment = EvidenceSentiment.INSUFFICIENT,
                    factors = emptyList()
                )
            )
        )

        composeRule.onNodeWithTag(ExplainMoveTestTags.FACTORS).assertDoesNotExist()
        composeRule.onAllNodesWithTag(ExplainMoveTestTags.DISCLAIMER).assertCountEquals(1)
        composeRule.onAllNodesWithText(disclaimer).assertCountEquals(1)
    }

    // --- Error ----------------------------------------------------------------

    @Test
    fun error_shows_message_request_id_and_retry() {
        var retried = 0
        show(
            state = ExplainMoveUiState.Error(
                message = "The AI provider is temporarily unavailable.",
                requestId = "req-42"
            ),
            onExplain = { retried++ }
        )

        composeRule.onNodeWithTag(ExplainMoveTestTags.ERROR)
            .assertIsDisplayed()
            .assertTextEquals("The AI provider is temporarily unavailable.")

        composeRule.onNodeWithTag(ExplainMoveTestTags.REQUEST_ID)
            .assertIsDisplayed()
            .assertTextEquals("Reference ID: req-42")

        composeRule.onNodeWithText("Retry").performClick()

        assertEquals(1, retried)
    }

    @Test
    fun error_without_request_id_hides_the_reference() {
        show(ExplainMoveUiState.Error(message = "Explanation unavailable"))

        composeRule.onNodeWithTag(ExplainMoveTestTags.ERROR).assertIsDisplayed()
        composeRule.onNodeWithTag(ExplainMoveTestTags.REQUEST_ID).assertDoesNotExist()
    }

    @Test
    fun error_state_shows_no_success_content() {
        show(ExplainMoveUiState.Error(message = "Explanation unavailable", requestId = "req-1"))

        composeRule.onNodeWithTag(ExplainMoveTestTags.SUMMARY).assertDoesNotExist()
        composeRule.onNodeWithTag(ExplainMoveTestTags.FACTORS).assertDoesNotExist()
        composeRule.onNodeWithTag(ExplainMoveTestTags.HEADLINE).assertDoesNotExist()
        composeRule.onNodeWithTag(ExplainMoveTestTags.DISCLAIMER).assertDoesNotExist()
    }

    // --- Idle and loading -------------------------------------------------------

    @Test
    fun idle_shows_the_action_and_calls_back_once_when_tapped() {
        var requested = 0
        show(state = ExplainMoveUiState.Idle, onExplain = { requested++ })

        composeRule.onNodeWithTag(ExplainMoveTestTags.ACTION).assertIsDisplayed()
        assertEquals(0, requested)

        composeRule.onNodeWithTag(ExplainMoveTestTags.ACTION).performClick()

        assertEquals(1, requested)
    }

    @Test
    fun loading_shows_progress_and_no_result_content() {
        show(ExplainMoveUiState.Loading)

        composeRule.onNodeWithTag(ExplainMoveTestTags.LOADING).assertIsDisplayed()
        composeRule.onNodeWithTag(ExplainMoveTestTags.SUMMARY).assertDoesNotExist()
        composeRule.onNodeWithTag(ExplainMoveTestTags.ACTION).assertDoesNotExist()
        composeRule.onNodeWithText("Clear").assertDoesNotExist()
    }

    @Test
    fun rendering_alone_never_triggers_a_request() {
        var requested = 0
        show(
            state = ExplainMoveUiState.Success(explanation()),
            onExplain = { requested++ }
        )

        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(2_000)

        assertEquals(0, requested)
    }

    // --- Privacy of internals -----------------------------------------------------

    @Test
    fun internal_metadata_is_not_rendered_on_success() {
        show(ExplainMoveUiState.Success(explanation()))

        // Case-sensitive on purpose: raw enum names must not leak, while
        // user-facing wording such as "Stock increased" is expected.
        listOf(
            "stock-explanation",
            "promptVersion",
            "providerCalled",
            "evidenceAccepted",
            "evidenceRejected",
            "internal-request-id-should-not-render",
            "INCREASE",
            "POSITIVE",
            "INSUFFICIENT",
            "relativePerformance"
        ).forEach { internal ->
            composeRule
                .onAllNodesWithText(internal, substring = true, ignoreCase = false)
                .assertCountEquals(0)
        }
    }

    @Test
    fun request_id_is_not_rendered_for_insufficient_evidence() {
        show(
            ExplainMoveUiState.InsufficientEvidence(
                explanation(
                    sufficient = false,
                    sentiment = EvidenceSentiment.INSUFFICIENT,
                    factors = emptyList()
                )
            )
        )

        composeRule
            .onAllNodesWithText("internal-request-id-should-not-render", substring = true)
            .assertCountEquals(0)
        composeRule.onAllNodesWithText("Reference ID", substring = true).assertCountEquals(0)
    }

    private val internalTerms = listOf(
        "stock-explanation",
        "promptVersion",
        "providerCalled",
        "evidenceAccepted",
        "evidenceRejected",
        "internal-request-id-should-not-render",
        "untrusted evidence"
    )

    private fun assertNoInternalTerms() {
        internalTerms.forEach { term ->
            composeRule
                .onAllNodesWithText(term, substring = true, ignoreCase = true)
                .assertCountEquals(0)
        }
    }

    @Test
    fun internal_terms_are_not_rendered_for_insufficient_evidence() {
        show(
            ExplainMoveUiState.InsufficientEvidence(
                explanation(
                    sufficient = false,
                    sentiment = EvidenceSentiment.INSUFFICIENT,
                    factors = emptyList()
                )
            )
        )

        assertNoInternalTerms()
    }

    @Test
    fun internal_terms_are_not_rendered_for_error() {
        show(ExplainMoveUiState.Error(message = "Explanation unavailable", requestId = "req-9"))

        assertNoInternalTerms()
    }

    @Test
    fun insufficient_evidence_uses_no_failure_wording_or_factors_heading() {
        show(
            ExplainMoveUiState.InsufficientEvidence(
                explanation(
                    sufficient = false,
                    sentiment = EvidenceSentiment.INSUFFICIENT,
                    factors = emptyList()
                )
            )
        )

        composeRule.onAllNodesWithText("Key factors").assertCountEquals(0)
        listOf("Error", "Failed", "failure", "went wrong").forEach { word ->
            composeRule.onAllNodesWithText(word, substring = true, ignoreCase = true)
                .assertCountEquals(0)
        }
    }

    @Test
    fun request_id_is_not_rendered_on_success() {
        show(ExplainMoveUiState.Success(explanation()))

        composeRule.onNodeWithTag(ExplainMoveTestTags.REQUEST_ID).assertDoesNotExist()
        composeRule.onAllNodesWithText("Reference ID", substring = true).assertCountEquals(0)
    }
}