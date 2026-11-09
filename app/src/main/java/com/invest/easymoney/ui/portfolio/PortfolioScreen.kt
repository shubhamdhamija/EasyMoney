package com.invest.easymoney.ui.portfolio

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.invest.easymoney.domain.portfolio.DuplicateStatus
import com.invest.easymoney.domain.portfolio.PortfolioAccount
import com.invest.easymoney.domain.portfolio.PortfolioImportPreview
import com.invest.easymoney.domain.portfolio.PortfolioPosition
import com.invest.easymoney.domain.portfolio.PortfolioReplacementDecision
import com.invest.easymoney.domain.portfolio.PortfolioSecurityType
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val csvMimeTypes = arrayOf("text/csv", "text/comma-separated-values", "application/csv", "text/plain")
private val dateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm a z", Locale.getDefault())
    .withZone(ZoneId.of("America/Toronto"))

private fun fmtDate(i: Instant) = dateFormatter.format(i)
private fun money(v: BigDecimal?, cur: String?) =
    if (v == null) "—" else "${v.setScale(2, RoundingMode.HALF_UP).toPlainString()} ${cur.orEmpty()}".trim()
private fun qty(v: BigDecimal) = v.stripTrailingZeros().toPlainString()
private fun accountLabel(a: PortfolioAccount) = "${a.accountType.name} · ${a.maskedAccountNumber}"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioScreen(
    importViewModel: PortfolioImportViewModel = hiltViewModel(),
    portfolioViewModel: PortfolioViewModel = hiltViewModel()
) {
    val importState by importViewModel.state.collectAsStateWithLifecycle()
    val ui by portfolioViewModel.uiState.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }

    // Storage Access Framework: no storage permission, no file path.
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) importViewModel.selectFile(uri)
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("Portfolio", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 16.dp)) }
        item {
            Text(
                "EasyMoney imports a read-only portfolio snapshot. It does not receive brokerage credentials " +
                    "and cannot place trades, transfer funds, or modify a Wealthsimple account.",
                style = MaterialTheme.typography.bodySmall
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { picker.launch(csvMimeTypes) }) {
                    Text(if (ui.portfolio == null) "Import Wealthsimple CSV" else "Import newer snapshot")
                }
                OutlinedButton(onClick = {}, enabled = false) { Text("Connect automatically · Coming later") }
            }
        }

        when (val s = importState) {
            PortfolioImportState.Idle -> Unit
            is PortfolioImportState.ReadingFile, is PortfolioImportState.Importing ->
                item { CircularProgressIndicator() }
            is PortfolioImportState.Preview -> item {
                PreviewCard(
                    preview = s.preview,
                    decision = s.selectedReplacementDecision,
                    onDecision = importViewModel::chooseReplacementDecision,
                    onCancel = importViewModel::cancelImport,
                    onConfirm = importViewModel::confirmImport
                )
            }
            is PortfolioImportState.Success -> item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Portfolio imported.")
                        TextButton(onClick = importViewModel::dismissSuccess) { Text("OK") }
                    }
                }
            }
            is PortfolioImportState.Error -> item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(s.message, color = MaterialTheme.colorScheme.error)
                        Row {
                            if (s.retryable) TextButton(onClick = importViewModel::retry) { Text("Retry") }
                            TextButton(onClick = importViewModel::clearError) { Text("Dismiss") }
                        }
                    }
                }
            }
        }

        val portfolio = ui.portfolio
        if (portfolio == null) {
            item { Text("No portfolio imported yet.") }
        } else {
            item {
                Text("Last imported: ${portfolio.latestEffectiveAt?.let(::fmtDate).orEmpty()}")
                Text(
                    "Portfolio data is a snapshot from the imported report and does not update automatically.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            item {
                Text("Accounts", style = MaterialTheme.typography.titleMedium)
                portfolio.accounts.forEach { Text(accountLabel(it)) }
            }
            item {
                Text("Currency totals", style = MaterialTheme.typography.titleMedium)
                ui.summary?.byCurrency?.forEach { t ->
                    Text("${t.currency} investments: ${money(t.marketValue, t.currency)}")
                    Text("${t.currency} cash: ${money(t.cash, t.currency)}")
                    Text("${t.currency} unrealized return: ${money(t.unrealizedReturn, t.currency)}")
                }
            }
            item {
                Text("Filters", style = MaterialTheme.typography.titleMedium)
                ChipRow(
                    options = portfolio.accounts.map { it.accountId to accountLabel(it) },
                    selected = ui.filters.accountId,
                    onSelect = portfolioViewModel::setAccount
                )
                ChipRow(
                    options = ui.summary?.byCurrency.orEmpty().map { it.currency to it.currency },
                    selected = ui.filters.currency,
                    onSelect = portfolioViewModel::setCurrency
                )
                ChipRow(
                    options = portfolio.positions.map { it.securityType }.distinct().map { it.name to it.name },
                    selected = ui.filters.securityType?.name,
                    onSelect = { portfolioViewModel.setSecurityType(it?.let(PortfolioSecurityType::valueOf)) }
                )
                Text("Sort", style = MaterialTheme.typography.titleSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(HoldingSort.entries) { sort ->
                        FilterChip(
                            selected = ui.filters.sort == sort,
                            onClick = { portfolioViewModel.setSort(sort) },
                            label = { Text(sort.name.lowercase().replace('_', ' ')) }
                        )
                    }
                }
            }
            item { Text("Holdings", style = MaterialTheme.typography.titleMedium) }
            items(ui.holdings, key = { it.positionId }) { p ->
                val account = portfolio.accounts.firstOrNull { it.accountId == p.accountId }
                HoldingRow(p, account?.let(::accountLabel))
            }
            item {
                TextButton(onClick = { confirmDelete = true }) {
                    Text("Delete portfolio data", color = MaterialTheme.colorScheme.error)
                }
            }
        }
        item { Text("", modifier = Modifier.padding(24.dp)) }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete portfolio data?") },
            text = { Text("All imported portfolio data stored on this device will be removed.") },
            confirmButton = {
                TextButton(onClick = { portfolioViewModel.deleteAll(); confirmDelete = false }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChipRow(options: List<Pair<String, String>>, selected: String?, onSelect: (String?) -> Unit) {
    if (options.size < 2) return
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(options) { (id, label) ->
            FilterChip(
                selected = selected == id,
                onClick = { onSelect(if (selected == id) null else id) },
                label = { Text(label) }
            )
        }
    }
}

@Composable
private fun HoldingRow(p: PortfolioPosition, account: String?) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text("${p.symbol ?: "—"} · ${p.securityName}", style = MaterialTheme.typography.titleSmall)
            Text("Quantity ${qty(p.quantity)}")
            Text(
                if (p.marketPrice == null) "Market value ${money(p.marketValue, p.marketValueCurrency?.value)} (price unavailable)"
                else "Market value ${money(p.marketValue, p.marketValueCurrency?.value)}"
            )
            Text("Unrealized return ${money(p.unrealizedReturn, p.unrealizedReturnCurrency?.value)}")
            if (account != null) Text(account, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun PreviewCard(
    preview: PortfolioImportPreview,
    decision: PortfolioReplacementDecision,
    onDecision: (PortfolioReplacementDecision) -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    val duplicate = preview.duplicateStatus == DuplicateStatus.IDENTICAL_IMPORT
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Wealthsimple portfolio import", style = MaterialTheme.typography.titleMedium)
            preview.accounts.forEach { Text("Account: ${accountLabel(it)}") }
            Text("Snapshot: ${fmtDate(preview.effectiveAt)}")
            val s = preview.summary
            Text("Accounts ${s.accountCount} · Holdings ${s.positionCount} · Cash balances ${s.cashBalanceCount}")
            Text("Currencies: ${s.currencies.joinToString()} · Rejected rows: ${s.rejectedRowCount}")
            val counts = preview.warnings.groupingBy { it }.eachCount()
            if (counts.isNotEmpty()) {
                Text("Warnings (${preview.warnings.size})", style = MaterialTheme.typography.titleSmall)
                counts.forEach { (code, n) -> Text("$code × $n", style = MaterialTheme.typography.bodySmall) }
            }
            preview.rejectedRows.take(10).forEach {
                Text("Row ${it.rowNumber}: ${it.safeMessage}", color = MaterialTheme.colorScheme.error)
            }
            Text("Holdings", style = MaterialTheme.typography.titleSmall)
            preview.positions.take(50).forEach { p ->
                Text(
                    "${p.symbol ?: "—"} · ${p.securityName} · ${qty(p.quantity)} · " +
                        money(p.marketValue, p.marketValueCurrency?.value) +
                        (if (p.marketPrice == null) " · no price" else "") +
                        " · book ${money(p.bookValueMarket ?: p.bookValueCad, (p.bookValueMarketCurrency ?: p.bookValueCadCurrency)?.value)}" +
                        " · return ${money(p.unrealizedReturn, p.unrealizedReturnCurrency?.value)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (preview.positions.size > 50) Text("+${preview.positions.size - 50} more", style = MaterialTheme.typography.bodySmall)
            preview.cashBalances.forEach { Text("Cash ${money(it.amount, it.currency.value)}", style = MaterialTheme.typography.bodySmall) }

            when (preview.duplicateStatus) {
                DuplicateStatus.IDENTICAL_IMPORT -> Text("Already imported", color = MaterialTheme.colorScheme.error)
                DuplicateStatus.OLDER_SNAPSHOT -> Text(
                    "This snapshot is older than the one currently stored. Importing it will replace newer data.",
                    color = MaterialTheme.colorScheme.error
                )
                DuplicateStatus.NEWER_SNAPSHOT -> Text("This will replace the previous snapshot for these accounts.")
                else -> Unit
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onCancel) { Text("Cancel") }
                if (preview.duplicateStatus == DuplicateStatus.OLDER_SNAPSHOT) {
                    Button(onClick = {
                        onDecision(PortfolioReplacementDecision.IMPORT_AS_ACTIVE); onConfirm()
                    }) { Text("Import as active snapshot") }
                } else {
                    Button(onClick = onConfirm, enabled = !duplicate) {
                        Text(if (preview.duplicateStatus == DuplicateStatus.NEWER_SNAPSHOT) "Replace previous snapshot" else "Import portfolio")
                    }
                }
            }
        }
    }
}

