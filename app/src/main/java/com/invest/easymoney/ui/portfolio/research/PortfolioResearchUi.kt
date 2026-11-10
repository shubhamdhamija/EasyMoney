@file:OptIn(ExperimentalMaterial3Api::class)

package com.invest.easymoney.ui.portfolio.research

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.invest.easymoney.domain.portfolio.research.PortfolioResearchReport
import com.invest.easymoney.domain.portfolio.research.ResearchDataCategory
import com.invest.easymoney.domain.portfolio.research.ResearchPortfolioTarget
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val snapshotFormat = DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a", Locale.US)
    .withZone(ZoneId.systemDefault())

private fun formatSnapshot(i: Instant?): String = i?.let(snapshotFormat::format) ?: "Unknown"

const val CONSENT_BODY =
    "EasyMoney will use your selected portfolio snapshot to prepare research. " +
        "No trades or account changes will be made."

/**
 * Hosts the whole research flow. The only way to start is the explicit button followed by an
 * explicit "Continue" in the consent dialog. No trading, transfer or rebalance controls exist.
 */
@Composable
fun PortfolioResearchSection(
    state: PortfolioResearchUiState,
    target: ResearchPortfolioTarget?,
    onResearch: (ResearchPortfolioTarget) -> Unit,
    onConfirm: (Set<ResearchDataCategory>) -> Unit,
    onCancelConsent: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when (state) {
            is PortfolioResearchUiState.Idle, is PortfolioResearchUiState.ConsentRequired -> {
                if (target != null) {
                    Button(onClick = { onResearch(target) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Research my portfolio")
                    }
                }
            }
            is PortfolioResearchUiState.CreatingConsent -> ProgressCard("Preparing your request…")
            is PortfolioResearchUiState.Researching -> ProgressCard("Researching your portfolio…")
            is PortfolioResearchUiState.Success -> ResearchResultCard(state.report, onDismiss)
            is PortfolioResearchUiState.Error -> ResearchErrorCard(state, onRetry, onDismiss)
        }
    }
    if (state is PortfolioResearchUiState.ConsentRequired) {
        ResearchConsentDialog(state.target, onConfirm, onCancelConsent)
    }
}

@Composable
fun ResearchConsentDialog(
    target: ResearchPortfolioTarget,
    onConfirm: (Set<ResearchDataCategory>) -> Unit,
    onCancel: () -> Unit
) {
    var cash by rememberSaveable { mutableStateOf(ResearchDataCategory.CASH_BALANCES.defaultOn) }
    var book by rememberSaveable { mutableStateOf(ResearchDataCategory.BOOK_VALUES.defaultOn) }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Research your portfolio?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(CONSENT_BODY, style = MaterialTheme.typography.bodyMedium)
                Text("Source: ${target.sourceLabel}", style = MaterialTheme.typography.bodySmall)
                Text("Snapshot: ${formatSnapshot(target.snapshotAt)}", style = MaterialTheme.typography.bodySmall)
                Text("Data to include", fontWeight = FontWeight.SemiBold)
                ToggleRow(ResearchDataCategory.ACCOUNTS.label, checked = true, enabled = false) {}
                ToggleRow(ResearchDataCategory.POSITIONS.label, checked = true, enabled = false) {}
                ToggleRow(ResearchDataCategory.CASH_BALANCES.label, checked = cash, enabled = true) { cash = it }
                ToggleRow(ResearchDataCategory.BOOK_VALUES.label, checked = book, enabled = true) { book = it }
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(
                    buildSet {
                        add(ResearchDataCategory.ACCOUNTS)
                        add(ResearchDataCategory.POSITIONS)
                        if (cash) add(ResearchDataCategory.CASH_BALANCES)
                        if (book) add(ResearchDataCategory.BOOK_VALUES)
                    }
                )
            }) { Text("Continue") }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancel") } }
    )
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

@Composable
private fun ProgressCard(text: String) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator(modifier = Modifier.padding(2.dp))
            Text(text)
        }
    }
}

@Composable
private fun ResearchErrorCard(state: PortfolioResearchUiState.Error, onRetry: () -> Unit, onDismiss: () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(state.message, color = MaterialTheme.colorScheme.onErrorContainer)
            state.requestId?.let { Text("Reference: $it", style = MaterialTheme.typography.bodySmall) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.retryable && state.target != null) OutlinedButton(onClick = onRetry) { Text("Try again") }
                TextButton(onClick = onDismiss) { Text("Dismiss") }
            }
        }
    }
}

@Composable
fun ResearchResultCard(report: PortfolioResearchReport, onDismiss: () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Portfolio research", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Source: ${report.source.label()}", style = MaterialTheme.typography.bodySmall)
            Text("Snapshot: ${formatSnapshot(report.snapshotAt)}", style = MaterialTheme.typography.bodySmall)
            if (report.partial) Text("Some steps could not finish, so this result is partial.", style = MaterialTheme.typography.bodySmall)
            Text(report.summary, style = MaterialTheme.typography.bodyMedium)

            report.currencySummaries.forEach { s ->
                Section("${s.currency} summary") {
                    Line("Investments", s.investmentMarketValue)
                    s.cashValue?.let { Line("Cash", it) }
                    s.bookValue?.let { Line("Book value", it) }
                    s.unrealizedReturn?.let { Line("Unrealized return", it) }
                    Line("Positions", s.positionCount.toString())
                }
            }
            if (report.positions.isNotEmpty()) {
                Section("Holdings") {
                    report.positions.take(10).forEach { p ->
                        Line(p.symbol ?: p.securityName, "${p.marketValue ?: "—"} ${p.currency}")
                    }
                }
            }
            report.concentration.forEach { c ->
                Section("${c.currency} concentration") {
                    c.largestPosition?.let { Line("Largest: ${it.label}", "${it.weightPercent}%") }
                    c.topPositionsWeightPercent?.let { Line("Top ${c.topPositions.size} combined", "$it%") }
                    c.cashAllocationPercent?.let { Line("Cash", "$it%") }
                    if (c.excludedPositionCount > 0) Text("${c.excludedPositionCount} position(s) excluded (missing value).", style = MaterialTheme.typography.bodySmall)
                }
            }
            if (report.evidence.isNotEmpty()) {
                Section("Evidence") { report.evidence.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) } }
            }
            report.warnings.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
            Text(report.statement, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    }
}

private fun com.invest.easymoney.domain.portfolio.PortfolioSource.label(): String = when (this) {
    com.invest.easymoney.domain.portfolio.PortfolioSource.WEALTHSIMPLE_CSV -> "Wealthsimple CSV import"
    com.invest.easymoney.domain.portfolio.PortfolioSource.SNAPTRADE_SANDBOX -> "SnapTrade Sandbox (simulated)"
    else -> "Portfolio"
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, fontWeight = FontWeight.SemiBold)
        content()
    }
}

@Composable
private fun Line(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
