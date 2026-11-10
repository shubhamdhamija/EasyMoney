@file:OptIn(ExperimentalMaterial3Api::class)

package com.invest.easymoney.ui.portfolio

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.invest.easymoney.domain.portfolio.DuplicateStatus
import com.invest.easymoney.domain.portfolio.PortfolioAccount
import com.invest.easymoney.domain.portfolio.PortfolioImportPreview
import com.invest.easymoney.domain.portfolio.PortfolioPosition
import com.invest.easymoney.domain.portfolio.PortfolioReplacementDecision
import com.invest.easymoney.domain.portfolio.PortfolioSummaryCalculator
import com.invest.easymoney.ui.theme.EasyMoneyTradingColors
import androidx.compose.ui.platform.LocalContext
import com.invest.easymoney.ui.portfolio.connection.ActivePortfolioSource
import com.invest.easymoney.ui.portfolio.connection.ConnectConsentDialog
import com.invest.easymoney.ui.portfolio.connection.ConnectDemoBrokerageCard
import com.invest.easymoney.ui.portfolio.connection.ConnectedPortfolioPanel
import com.invest.easymoney.ui.portfolio.connection.ConnectionDetailsSheet
import com.invest.easymoney.ui.portfolio.connection.ConnectionFlowBanner
import com.invest.easymoney.ui.portfolio.connection.DeleteSyncedDataDialog
import com.invest.easymoney.ui.portfolio.connection.DisconnectConfirmDialog
import com.invest.easymoney.ui.portfolio.connection.PortfolioConnectionState
import com.invest.easymoney.ui.portfolio.connection.PortfolioConnectionViewModel
import com.invest.easymoney.ui.portfolio.connection.SourceLabel
import com.invest.easymoney.ui.portfolio.connection.SourceSelector
import com.invest.easymoney.ui.portfolio.connection.openConnectionPortal
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

private val csvMimeTypes = arrayOf("text/csv", "text/comma-separated-values", "application/csv", "text/plain")
private val snapshotDateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm a", Locale.US)
    .withZone(ZoneId.of("America/Toronto"))
private val shortDateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)
    .withZone(ZoneId.of("America/Toronto"))
private val moneyFormatter = DecimalFormat("#,##0.00")
private val zero = BigDecimal.ZERO

private fun formatSnapshotDate(i: Instant): String = snapshotDateFormatter.format(i)
private fun formatPortfolioDate(i: Instant?): String = i?.let { shortDateFormatter.format(it) } ?: "—"
private fun moneyValue(value: BigDecimal?, signed: Boolean = false, showSymbol: Boolean = true): String {
    if (value == null) return "—"
    val abs = value.abs().setScale(2, RoundingMode.HALF_UP)
    val sign = when {
        signed && value > zero -> "+"
        signed && value < zero -> "-"
        else -> ""
    }
    val prefix = if (showSymbol) "$" else ""
    return "$sign$prefix${moneyFormatter.format(abs)}"
}
private fun moneyWithCurrency(value: BigDecimal?, currency: String?): String =
    if (value == null || currency.isNullOrBlank()) "—" else "${moneyValue(value, false, true)} ${currency.uppercase()}"
private fun quantity(v: BigDecimal) = v.stripTrailingZeros().toPlainString()
private fun maskedAccountNumber(account: PortfolioAccount): String = account.maskedAccountNumber.trim()
private fun accountTypeLabel(account: PortfolioAccount): String = account.accountType.name
private fun securityBadgeText(position: PortfolioPosition): String =
    (position.symbol ?: position.securityName.takeIf { it.isNotBlank() } ?: "—").take(2).uppercase()
private fun positiveText(value: BigDecimal?, currency: String?) =
    if (value == null) "Value unavailable" else "${moneyValue(value, true, true)} ${currency.orEmpty().uppercase()}".trim()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioScreen(
    importViewModel: PortfolioImportViewModel = hiltViewModel(),
    portfolioViewModel: PortfolioViewModel = hiltViewModel(),
    connectionViewModel: PortfolioConnectionViewModel = hiltViewModel()
) {
    val importState by importViewModel.state.collectAsStateWithLifecycle()
    val ui by portfolioViewModel.uiState.collectAsStateWithLifecycle()
    val connectionState by connectionViewModel.state.collectAsStateWithLifecycle()
    val connected by connectionViewModel.connectedPortfolio.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val csvPortfolio = ui.portfolio
    val sandboxPortfolio = connected?.portfolio
    var confirmDelete by remember { mutableStateOf(false) }
    var showHoldingsSheet by remember { mutableStateOf(false) }
    var showOlderSnapshotDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showConsent by remember { mutableStateOf(false) }
    var showDetails by remember { mutableStateOf(false) }
    var confirmDisconnect by remember { mutableStateOf(false) }
    var confirmDeleteSynced by remember { mutableStateOf(false) }
    var selectedCurrency by rememberSaveable { mutableStateOf("CAD") }
    var preferredSource by rememberSaveable { mutableStateOf(ActivePortfolioSource.CSV.name) }

    // Sources are shown separately and never merged. The user picks when both exist.
    val availableSources = buildList {
        if (csvPortfolio != null) add(ActivePortfolioSource.CSV)
        if (sandboxPortfolio != null) add(ActivePortfolioSource.SNAPTRADE_SANDBOX)
    }
    val activeSource = availableSources.firstOrNull { it.name == preferredSource } ?: availableSources.firstOrNull()
    val currentPortfolio = when (activeSource) {
        ActivePortfolioSource.CSV -> csvPortfolio
        ActivePortfolioSource.SNAPTRADE_SANDBOX -> sandboxPortfolio
        null -> null
    }
    val activeSummary = when (activeSource) {
        ActivePortfolioSource.SNAPTRADE_SANDBOX -> sandboxPortfolio?.let { PortfolioSummaryCalculator.calculate(it) }
        else -> ui.summary
    }
    val activeHoldings = when (activeSource) {
        ActivePortfolioSource.SNAPTRADE_SANDBOX ->
            sandboxPortfolio?.positions.orEmpty().sortedByDescending { it.marketValue ?: BigDecimal.ZERO }
        else -> ui.holdings
    }

    val currencies = activeSummary?.byCurrency?.map { it.currency } ?: emptyList()
    LaunchedEffect(currencies, selectedCurrency) {
        if (currencies.isNotEmpty() && selectedCurrency !in currencies) {
            selectedCurrency = currencies.first()
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) importViewModel.selectFile(uri)
    }

    // Opens the portal once per session: the state moves on immediately, so recomposition and
    // rotation cannot reopen it.
    (connectionState as? PortfolioConnectionState.OpeningPortal)?.let { opening ->
        LaunchedEffect(opening.sessionId) {
            openConnectionPortal(context, opening.portalUri)
            connectionViewModel.onPortalOpened()
        }
    }

    val connectionHeader: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ConnectionFlowBanner(
                state = connectionState,
                onFinished = connectionViewModel::completeConnection,
                onCancelWaiting = connectionViewModel::cancelPending,
                onReconnect = connectionViewModel::reconnect,
                onDismissError = connectionViewModel::dismissError
            )
            if (activeSource != null) SourceLabel(activeSource)
            if (activeSource != null) {
                SourceSelector(
                    available = availableSources,
                    selected = activeSource,
                    onSelect = { preferredSource = it.name }
                )
            }
            val connection = connected?.connection
            if (activeSource == ActivePortfolioSource.SNAPTRADE_SANDBOX && connection != null) {
                ConnectedPortfolioPanel(
                    connection = connection,
                    syncing = connectionState is PortfolioConnectionState.Syncing,
                    onSync = connectionViewModel::sync,
                    onDetails = { showDetails = true },
                    onDisconnect = { confirmDisconnect = true }
                )
            } else if (sandboxPortfolio == null && csvPortfolio != null &&
                connectionState is PortfolioConnectionState.Idle
            ) {
                ConnectDemoBrokerageCard(enabled = true, onConnect = { showConsent = true })
            }
        }
    }

    when (val state = importState) {
        is PortfolioImportState.Idle -> {
            if (currentPortfolio == null) {
                EmptyPortfolioState(
                    onImportClick = { picker.launch(csvMimeTypes) },
                    extra = {
                        ConnectionFlowBanner(
                            state = connectionState,
                            onFinished = connectionViewModel::completeConnection,
                            onCancelWaiting = connectionViewModel::cancelPending,
                            onReconnect = connectionViewModel::reconnect,
                            onDismissError = connectionViewModel::dismissError
                        )
                        if (connectionState is PortfolioConnectionState.Idle ||
                            connectionState is PortfolioConnectionState.Error
                        ) {
                            ConnectDemoBrokerageCard(enabled = true, onConnect = { showConsent = true })
                        }
                    }
                )
            } else {
                PortfolioOverview(
                    portfolio = currentPortfolio,
                    summary = activeSummary,
                    selectedCurrency = selectedCurrency,
                    onCurrencyChange = { selectedCurrency = it },
                    onImportClick = { picker.launch(csvMimeTypes) },
                    onDeletePortfolio = {
                        if (activeSource == ActivePortfolioSource.SNAPTRADE_SANDBOX) confirmDeleteSynced = true
                        else confirmDelete = true
                    },
                    onShowAllHoldings = { showHoldingsSheet = true },
                    showMenu = showMenu,
                    onShowMenuChange = { showMenu = it },
                    onImportNewerSnapshot = { picker.launch(csvMimeTypes) },
                    holdings = activeHoldings,
                    summaryByCurrency = activeSummary?.byCurrency.orEmpty(),
                    header = connectionHeader
                )
            }
        }
        is PortfolioImportState.ReadingFile -> ReadingPortfolioState(fileName = state.fileName)
        is PortfolioImportState.Preview -> ImportPreviewScreen(
            preview = state.preview,
            decision = state.selectedReplacementDecision,
            onDecision = importViewModel::chooseReplacementDecision,
            onCancel = importViewModel::cancelImport,
            onConfirm = {
                val status = state.preview.duplicateStatus
                if (status == DuplicateStatus.OLDER_SNAPSHOT) {
                    showOlderSnapshotDialog = true
                } else {
                    importViewModel.confirmImport()
                }
            },
            onShowAllHoldings = { showHoldingsSheet = true }
        )
        is PortfolioImportState.Importing -> ImportingState(
            preview = state.preview,
            onCancel = importViewModel::cancelImport
        )
        is PortfolioImportState.Success -> SuccessState(
            snapshot = state.snapshot,
            onViewPortfolio = importViewModel::dismissSuccess
        )
        is PortfolioImportState.Error -> ErrorState(
            message = state.message,
            retryable = state.retryable,
            onRetry = importViewModel::retry,
            onDismiss = importViewModel::clearError
        )
    }

    if (showHoldingsSheet && (importState is PortfolioImportState.Preview || currentPortfolio != null)) {
        val preview = (importState as? PortfolioImportState.Preview)?.preview
        val positions = if (preview != null) preview.positions.sortedByDescending { it.marketValue ?: BigDecimal.ZERO } else activeHoldings
        ModalBottomSheet(
            onDismissRequest = { showHoldingsSheet = false },
            dragHandle = null
        ) {
            HoldingsSheetContent(
                title = if (preview != null) "Holdings" else "Portfolio holdings",
                positions = positions,
                onDismiss = { showHoldingsSheet = false }
            )
        }
    }

    if (showOlderSnapshotDialog) {
        val current = importState as? PortfolioImportState.Preview ?: return
        AlertDialog(
            onDismissRequest = { showOlderSnapshotDialog = false },
            title = { Text("Older snapshot") },
            text = { Text("This report is older than your current portfolio snapshot.") },
            confirmButton = {
                TextButton(onClick = {
                    showOlderSnapshotDialog = false
                    importViewModel.chooseReplacementDecision(PortfolioReplacementDecision.IMPORT_AS_ACTIVE)
                    importViewModel.confirmImport()
                }) { Text("Use older snapshot") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showOlderSnapshotDialog = false
                    importViewModel.chooseReplacementDecision(PortfolioReplacementDecision.CANCEL)
                    importViewModel.cancelImport()
                }) { Text("Keep current portfolio") }
            }
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete portfolio data?") },
            text = { Text("This removes the imported portfolio snapshot from this device.") },
            confirmButton = { TextButton(onClick = { portfolioViewModel.deleteAll(); confirmDelete = false }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }

    // --- Read-only SnapTrade Sandbox connection dialogs ---
    if (showConsent) {
        ConnectConsentDialog(
            onCancel = { showConsent = false },
            onContinue = {
                showConsent = false
                connectionViewModel.connectSandbox()
            }
        )
    }
    if (showDetails) {
        connected?.connection?.let { connection ->
            ConnectionDetailsSheet(
                connection = connection,
                onReconnect = { showDetails = false; connectionViewModel.reconnect() },
                onDisconnect = { showDetails = false; confirmDisconnect = true },
                onDeleteData = { showDetails = false; confirmDeleteSynced = true },
                onDismiss = { showDetails = false }
            )
        } ?: run { showDetails = false }
    }
    if (confirmDisconnect) {
        DisconnectConfirmDialog(
            onCancel = { confirmDisconnect = false },
            onConfirm = { confirmDisconnect = false; connectionViewModel.disconnect() }
        )
    }
    if (confirmDeleteSynced) {
        DeleteSyncedDataDialog(
            onCancel = { confirmDeleteSynced = false },
            onConfirm = { confirmDeleteSynced = false; connectionViewModel.deleteSynchronizedData() }
        )
    }
}

@Composable
private fun EmptyPortfolioState(onImportClick: () -> Unit, extra: @Composable () -> Unit = {}) {
    Scaffold(topBar = {
        TopAppBar(title = { Text("Portfolio") }, actions = {
            IconButton(onClick = {}) { Icon(Icons.Default.MoreVert, contentDescription = "More options") }
        })
    }) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item { Spacer(modifier = Modifier.height(16.dp)) }
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.UploadFile,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Text(
                            text = "See your investments in one place",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Import a Wealthsimple holdings report to view your accounts, holdings and currency totals. EasyMoney cannot access or change your brokerage account.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = onImportClick,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Import Wealthsimple CSV") }
                        OutlinedButton(
                            onClick = {},
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("How to export your report") }
                        TextButton(onClick = {}) { Text("Try demo portfolio") }
                    }
                }
            }
            item {
                Text(
                    text = "Your file is processed on this device and is not uploaded.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { extra() }
            }
        }
    }
}

@Composable
private fun ReadingPortfolioState(fileName: String?) {
    var readingMessage by remember { mutableStateOf("This usually takes a few seconds.") }
    LaunchedEffect(Unit) {
        delay(2200)
        readingMessage = "Checking the report format"
        delay(1600)
        readingMessage = "Organizing accounts and holdings"
        delay(1600)
        readingMessage = "Preparing your preview"
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Portfolio") })
    }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Reading your portfolio",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = readingMessage,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!fileName.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = fileName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ImportPreviewScreen(
    preview: PortfolioImportPreview,
    decision: PortfolioReplacementDecision,
    onDecision: (PortfolioReplacementDecision) -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    onShowAllHoldings: () -> Unit
) {
    var showDetails by remember { mutableStateOf(false) }
    val summary = PortfolioSummaryCalculator.calculate(preview.accounts, preview.positions, preview.cashBalances)
    val topPositions = preview.positions.sortedByDescending { it.marketValue ?: BigDecimal.ZERO }.take(8)
    val actionLabel = when (preview.duplicateStatus) {
        DuplicateStatus.IDENTICAL_IMPORT -> "Already imported"
        DuplicateStatus.NEWER_SNAPSHOT -> "Replace with this snapshot"
        else -> "Import portfolio"
    }
    val actionSupport = when (preview.duplicateStatus) {
        DuplicateStatus.IDENTICAL_IMPORT -> "This exact portfolio snapshot is already in EasyMoney."
        DuplicateStatus.NEWER_SNAPSHOT -> "This will replace the older active snapshot for the same account."
        else -> ""
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Review portfolio") },
            navigationIcon = {
                IconButton(onClick = onCancel) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            },
            actions = {
                IconButton(onClick = onCancel) { Icon(Icons.Default.Close, contentDescription = "Close") }
            }
        )
    }) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Wealthsimple holdings", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text("As of ${formatSnapshotDate(preview.effectiveAt)}", style = MaterialTheme.typography.bodyLarge)
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            preview.accounts.forEach { account ->
                                val accountCurrencies = (preview.positions.filter { it.accountId == account.accountId }
                                    .mapNotNull { it.marketValueCurrency?.value } +
                                    preview.cashBalances.filter { it.accountId == account.accountId }.map { it.currency.value })
                                    .distinct().sorted()
                                val masked = maskedAccountNumber(account)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = accountTypeLabel(account),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (masked.isNotBlank()) {
                                            Text(
                                                text = masked,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${preview.positions.count { it.accountId == account.accountId }} holdings",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (accountCurrencies.isNotEmpty()) {
                                    Text(
                                        text = "Investments • ${accountCurrencies.joinToString(" and ")}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                val cashBalances = preview.cashBalances.count { it.accountId == account.accountId }
                                if (cashBalances > 0) {
                                    Text(
                                        text = "$cashBalances cash balances",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Divider()
                            }
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Value by currency", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            IconButton(onClick = {}) {
                                Icon(Icons.Default.Info, contentDescription = "Info")
                            }
                        }
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            summary.byCurrency.forEach { currencySummary ->
                                Surface(
                                    modifier = Modifier.width(240.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(18.dp),
                                    tonalElevation = 0.dp
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(currencySummary.currency, style = MaterialTheme.typography.titleMedium)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Investments", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(moneyValue(currencySummary.marketValue))
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Cash", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(moneyValue(currencySummary.cash))
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Book value", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(moneyValue(currencySummary.bookValue))
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Return", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                text = moneyValue(currencySummary.unrealizedReturn, signed = true),
                                                color = if (currencySummary.unrealizedReturn >= zero) EasyMoneyTradingColors.gain else MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Holdings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        topPositions.forEach { position ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = securityBadgeText(position),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        Text(
                                            text = position.symbol ?: "—",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        val marketValue = position.marketValue
                                        if (marketValue != null && position.marketValueCurrency != null) {
                                            Text(
                                                text = moneyValue(marketValue),
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        } else {
                                            Text(
                                                text = "Value unavailable",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = position.securityName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        val qty = quantity(position.quantity)
                                        Text(
                                            text = "$qty shares",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    val trend = position.unrealizedReturn
                                    if (trend != null && position.unrealizedReturnCurrency != null) {
                                        val gainColor = if (trend >= zero) EasyMoneyTradingColors.gain else MaterialTheme.colorScheme.error
                                        Text(
                                            text = "${moneyValue(trend, signed = true)} ${position.unrealizedReturnCurrency!!.value.uppercase()}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = gainColor
                                        )
                                    } else if (position.marketPrice == null) {
                                        Text(
                                            text = "Price unavailable in report",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        TextButton(onClick = onShowAllHoldings) {
                            Text("View all ${preview.positions.size} holdings")
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Cash", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        val cashByCurrency = preview.cashBalances.groupBy { it.currency.value }
                        if (cashByCurrency.isEmpty()) {
                            Text("No cash balances in report.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            cashByCurrency.forEach { (currency, items) ->
                                val total = items.fold(zero) { acc, cash -> acc + cash.amount }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(currency.uppercase(), style = MaterialTheme.typography.titleSmall)
                                    Text(moneyValue(total), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Import details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                if (preview.warnings.isEmpty() && preview.rejectedRows.isEmpty()) {
                                    Text("Everything looks ready", color = EasyMoneyTradingColors.gain)
                                } else {
                                    TextButton(onClick = { showDetails = !showDetails }) {
                                        Text(if (showDetails) "Hide" else "${preview.warnings.size + preview.rejectedRows.size} items need attention")
                                    }
                                }
                            }
                            if (showDetails || preview.warnings.isNotEmpty() || preview.rejectedRows.isNotEmpty()) {
                                if (preview.warnings.isEmpty() && preview.rejectedRows.isEmpty()) {
                                    Text("Everything looks ready", style = MaterialTheme.typography.bodyMedium)
                                } else {
                                    val visibleWarnings = preview.warnings.distinct().take(6)
                                    if (visibleWarnings.isNotEmpty()) {
                                        visibleWarnings.forEach { warning ->
                                            Text("• ${warning.name.lowercase().replace('_', ' ')}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    if (preview.rejectedRows.isNotEmpty()) {
                                        preview.rejectedRows.take(4).forEach { row ->
                                            Text(
                                                text = "Row ${row.rowNumber}: ${row.symbol ?: row.securityName ?: "item"} — ${row.safeMessage}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Values reflect the ${formatSnapshotDate(preview.effectiveAt)} snapshot.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(84.dp)) }
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                color = MaterialTheme.colorScheme.background,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val primaryEnabled = preview.duplicateStatus != DuplicateStatus.IDENTICAL_IMPORT
                    Button(
                        onClick = onConfirm,
                        enabled = primaryEnabled,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (preview.duplicateStatus == DuplicateStatus.IDENTICAL_IMPORT) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                            contentColor = if (preview.duplicateStatus == DuplicateStatus.IDENTICAL_IMPORT) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(actionLabel)
                    }
                    if (actionSupport.isNotEmpty()) {
                        Text(actionSupport, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
                }
            }
        }
    }
}

@Composable
private fun ImportingState(preview: PortfolioImportPreview, onCancel: () -> Unit) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Review portfolio") },
            navigationIcon = {
                IconButton(onClick = onCancel) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            }
        )
    }) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator()
                Text("Importing portfolio", style = MaterialTheme.typography.headlineSmall)
                Text("Your Wealthsimple snapshot is being saved on this device.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SuccessState(snapshot: com.invest.easymoney.domain.portfolio.PortfolioSnapshot, onViewPortfolio: () -> Unit) {
    Scaffold(topBar = {
        TopAppBar(title = { Text("Portfolio") })
    }) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✓", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    Text("Portfolio imported", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Text("Your Wealthsimple snapshot is ready.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = onViewPortfolio, modifier = Modifier.fillMaxWidth()) { Text("View portfolio") }
                }
            }
        }
    }
}

@Composable
private fun ErrorState(message: String, retryable: Boolean, onRetry: () -> Unit, onDismiss: () -> Unit) {
    Scaffold(topBar = {
        TopAppBar(title = { Text("Portfolio") })
    }) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Unable to read this portfolio", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(message, color = MaterialTheme.colorScheme.onErrorContainer)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (retryable) {
                            Button(onClick = onRetry) { Text("Retry") }
                        }
                        TextButton(onClick = onDismiss) { Text("Dismiss") }
                    }
                }
            }
        }
    }
}

@Composable
private fun PortfolioOverview(
    portfolio: com.invest.easymoney.domain.portfolio.Portfolio,
    summary: com.invest.easymoney.domain.portfolio.PortfolioSummary?,
    selectedCurrency: String,
    onCurrencyChange: (String) -> Unit,
    onImportClick: () -> Unit,
    onDeletePortfolio: () -> Unit,
    onShowAllHoldings: () -> Unit,
    showMenu: Boolean,
    onShowMenuChange: (Boolean) -> Unit,
    onImportNewerSnapshot: () -> Unit,
    holdings: List<PortfolioPosition>,
    summaryByCurrency: List<com.invest.easymoney.domain.portfolio.CurrencyTotals>,
    header: (@Composable () -> Unit)? = null
) {
    val selectedSummary = summaryByCurrency.firstOrNull { it.currency == selectedCurrency }
        ?: summaryByCurrency.firstOrNull()
    val totalReturn = selectedSummary?.unrealizedReturn ?: zero
    val asOfText = formatPortfolioDate(portfolio.latestEffectiveAt)
    val visibleHoldings = holdings.filter { it.marketValueCurrency?.value == selectedCurrency }.sortedByDescending { it.marketValue ?: zero }
    val totalPositions = selectedSummary?.positionCount ?: 0
    val assetDistribution = selectedSummary?.securityTypeAllocation.orEmpty()

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Portfolio") },
            actions = {
                IconButton(onClick = onImportClick) { Icon(Icons.Default.UploadFile, contentDescription = "Import newer snapshot") }
                Box {
                    IconButton(onClick = { onShowMenuChange(true) }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { onShowMenuChange(false) }) {
                        DropdownMenuItem(
                            text = { Text("Import newer snapshot") },
                            onClick = {
                                onShowMenuChange(false)
                                onImportNewerSnapshot()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Portfolio details") },
                            onClick = { onShowMenuChange(false) }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete portfolio data") },
                            onClick = {
                                onShowMenuChange(false)
                                onDeletePortfolio()
                            }
                        )
                    }
                }
            }
        )
    }) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (header != null) item { header() }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Portfolio value", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            summaryByCurrency.map { it.currency }.forEach { currency ->
                                FilterChip(
                                    selected = selectedCurrency == currency,
                                    onClick = { onCurrencyChange(currency) },
                                    label = { Text(currency) }
                                )
                            }
                        }
                        Text(
                            text = moneyValue(selectedSummary?.marketValue ?: zero),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${moneyValue(totalReturn, signed = true)} total return",
                                color = if (totalReturn >= zero) EasyMoneyTradingColors.gain else MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text("As of ${asOfText}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Allocation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        if (assetDistribution.isEmpty()) {
                            Text("No position data available for ${selectedCurrency}.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            val totalWeight = assetDistribution.values.sumOf { it.toDouble() }.toBigDecimal()
                            val bars = assetDistribution.entries.sortedByDescending { it.value }
                            val totalWidth = bars.size.coerceAtLeast(1)
                            val dashWidth = 1f / totalWidth.toFloat()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                bars.forEach { (type, percent) ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(12.dp)
                                            .clip(RoundedCornerShape(99.dp))
                                            .background(
                                                when (type) {
                                                    com.invest.easymoney.domain.portfolio.PortfolioSecurityType.EQUITY -> MaterialTheme.colorScheme.primary
                                                    com.invest.easymoney.domain.portfolio.PortfolioSecurityType.EXCHANGE_TRADED_FUND -> MaterialTheme.colorScheme.secondary
                                                    com.invest.easymoney.domain.portfolio.PortfolioSecurityType.MUTUAL_FUND -> MaterialTheme.colorScheme.tertiary
                                                    com.invest.easymoney.domain.portfolio.PortfolioSecurityType.CURRENCY -> MaterialTheme.colorScheme.primaryContainer
                                                    com.invest.easymoney.domain.portfolio.PortfolioSecurityType.FIXED_INCOME -> MaterialTheme.colorScheme.secondaryContainer
                                                    else -> MaterialTheme.colorScheme.outline
                                                }
                                            )
                                    )
                                }
                            }
                            bars.forEach { (type, percent) ->
                                val displayType = type.name.lowercase().replace('_', ' ')
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(displayType, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${percent.stripTrailingZeros()}%", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Top holdings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    visibleHoldings.take(5).forEach { position ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(securityBadgeText(position), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(position.symbol ?: "—", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(position.securityName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(moneyValue(position.marketValue), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                val ret = position.unrealizedReturn
                                if (ret != null) {
                                    Text(
                                        text = "${moneyValue(ret, signed = true)}",
                                        color = if (ret >= zero) EasyMoneyTradingColors.gain else MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                    if (visibleHoldings.size > 5) {
                        TextButton(onClick = onShowAllHoldings) { Text("View all ${visibleHoldings.size} holdings") }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Accounts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        portfolio.accounts.forEach { account ->
                            val count = portfolio.positions.count { it.accountId == account.accountId }
                            val cashCount = portfolio.cashBalances.count { it.accountId == account.accountId }
                            val currencies = (portfolio.positions.filter { it.accountId == account.accountId }
                                .mapNotNull { it.marketValueCurrency?.value } + portfolio.cashBalances.filter { it.accountId == account.accountId }.map { it.currency.value })
                                .distinct().sorted().joinToString(" and ")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(accountTypeLabel(account), style = MaterialTheme.typography.titleSmall)
                                    if (maskedAccountNumber(account).isNotBlank()) Text(maskedAccountNumber(account), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(currencies.ifBlank { "—" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${count} holdings • $cashCount cash", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun HoldingsSheetContent(title: String, positions: List<PortfolioPosition>, onDismiss: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Close") }
        }
        positions.sortedByDescending { it.marketValue ?: zero }.forEach { p ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(p.symbol ?: "—", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(p.securityName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(moneyValue(p.marketValue), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("${quantity(p.quantity)} shares", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Divider()
        }
    }
}
