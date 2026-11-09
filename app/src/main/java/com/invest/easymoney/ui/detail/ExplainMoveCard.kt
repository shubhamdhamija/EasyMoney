package com.invest.easymoney.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.invest.easymoney.domain.model.StockExplanation
import com.invest.easymoney.ui.theme.TradingShapes

/** Test tags for the stock explanation card. */
object ExplainMoveTestTags {
    const val CARD = "stock_explanation_card"
    const val ACTION = "stock_explanation_action"
    const val LOADING = "stock_explanation_loading"
    const val HEADLINE = "stock_explanation_headline"
    const val SUMMARY = "stock_explanation_summary"
    const val FACTORS = "stock_explanation_factors"
    const val DISCLAIMER = "stock_explanation_disclaimer"
    const val ERROR = "stock_explanation_error"
    const val REQUEST_ID = "stock_explanation_request_id"
    const val RETRY = "explain_move_retry"
}

@Composable
internal fun ExplainMoveCard(
    state: ExplainMoveUiState,
    onExplain: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().testTag(ExplainMoveTestTags.CARD), shape = TradingShapes.Card, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Why did it move?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                }
                if (state !is ExplainMoveUiState.Idle && state !is ExplainMoveUiState.Loading) {
                    TextButton(onClick = onDismiss, contentPadding = PaddingValues(0.dp)) { Text("Clear", style = MaterialTheme.typography.labelSmall) }
                }
            }
            when (state) {
                ExplainMoveUiState.Idle -> {
                    Text(
                        "Use AI + retrieved context to explain the drivers behind the stock’s recent move.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.78f)
                    )
                    Button(onClick = onExplain, modifier = Modifier.fillMaxWidth().testTag(ExplainMoveTestTags.ACTION), shape = TradingShapes.Input, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Explain This Move")
                    }
                }
                ExplainMoveUiState.Loading -> {
                    Row(modifier = Modifier.testTag(ExplainMoveTestTags.LOADING), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text("Retrieving context and generating explanation...", style = MaterialTheme.typography.bodySmall)
                    }
                }
                is ExplainMoveUiState.Error -> {
                    Text(
                        state.message,
                        modifier = Modifier.testTag(ExplainMoveTestTags.ERROR),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    state.requestId?.let { requestId ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Reference ID: $requestId",
                            modifier = Modifier.testTag(ExplainMoveTestTags.REQUEST_ID),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.72f)
                        )
                    }
                    TextButton(
                        onClick = onExplain,
                        modifier = Modifier.testTag(ExplainMoveTestTags.RETRY),
                        contentPadding = PaddingValues(0.dp)
                    ) { Text("Retry", style = MaterialTheme.typography.labelSmall) }
                }
                is ExplainMoveUiState.Success -> {
                    StockExplanationContent(explanation = state.explanation)
                }
                is ExplainMoveUiState.InsufficientEvidence -> {
                    // A valid, honest result: shown neutrally, not as an error.
                    StockExplanationInsufficientContent(explanation = state.explanation)
                }
            }
        }
    }
}

@Composable
private fun StockExplanationContent(explanation: StockExplanation) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = explanation.headline(),
            modifier = Modifier.testTag(ExplainMoveTestTags.HEADLINE),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.tertiary
        )

        Text(
            text = explanation.summary,
            modifier = Modifier.testTag(ExplainMoveTestTags.SUMMARY),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer
        )

        if (explanation.factors.isNotEmpty()) {
            Column(
                modifier = Modifier.testTag(ExplainMoveTestTags.FACTORS),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Key factors",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                explanation.factors.forEach { factor ->
                    Text(
                        text = "• $factor",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.88f)
                    )
                }
            }
        }

        ExplanationDisclaimer(explanation.disclaimer)
    }
}

@Composable
private fun StockExplanationInsufficientContent(explanation: StockExplanation) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = explanation.headline(),
            modifier = Modifier.testTag(ExplainMoveTestTags.HEADLINE),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
        )
        Text(
            text = explanation.summary,
            modifier = Modifier.testTag(ExplainMoveTestTags.SUMMARY),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer
        )
        ExplanationDisclaimer(explanation.disclaimer)
    }
}

@Composable
private fun ExplanationDisclaimer(disclaimer: String) {
    if (disclaimer.isBlank()) return

    Text(
        text = disclaimer,
        modifier = Modifier.testTag(ExplainMoveTestTags.DISCLAIMER),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.72f)
    )
}
