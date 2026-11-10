@file:OptIn(ExperimentalMaterial3Api::class)

package com.invest.easymoney.ui.portfolio.connection

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.invest.easymoney.domain.portfolio.connection.ConnectionStatus
import com.invest.easymoney.domain.portfolio.connection.PortfolioConnection
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateTime = DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a", Locale.US)
    .withZone(ZoneId.systemDefault())

private fun fmt(i: Instant?): String = i?.let(dateTime::format) ?: "Not yet"

/** Which portfolio the screen shows. Sources are never merged. */
enum class ActivePortfolioSource(val label: String) {
    CSV("Wealthsimple CSV"),
    SNAPTRADE_SANDBOX("SnapTrade Sandbox")
}

/** Opens the Connection Portal in a Custom Tab. No WebView; nothing is intercepted or inspected. */
fun openConnectionPortal(context: Context, portalUrl: String) {
    val uri = Uri.parse(portalUrl)
    if (uri.scheme != "https") return
    CustomTabsIntent.Builder().build().launchUrl(context, uri)
}

@Composable
fun ConnectDemoBrokerageCard(enabled: Boolean, onConnect: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Simulated account", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(
                "Try automatic read-only synchronization using SnapTrade Sandbox. No real brokerage credentials are required.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(onClick = onConnect, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
                Text("Connect demo brokerage")
            }
        }
    }
}

@Composable
fun ConnectConsentDialog(onCancel: () -> Unit, onContinue: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Connect a read-only demo account?") },
        text = {
            Text(
                "SnapTrade Sandbox provides simulated portfolio data for testing. EasyMoney will be able to read " +
                    "simulated accounts, balances and holdings.\n\nEasyMoney will not place trades, move money or " +
                    "modify a brokerage account."
            )
        },
        confirmButton = { TextButton(onClick = onContinue) { Text("Continue") } },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancel") } }
    )
}

/** Transient/status banner for the connection flow. Renders nothing for Idle/Connected. */
@Composable
fun ConnectionFlowBanner(
    state: PortfolioConnectionState,
    onFinished: () -> Unit,
    onCancelWaiting: () -> Unit,
    onReconnect: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (state) {
        PortfolioConnectionState.CreatingSession ->
            BannerCard(modifier) { ProgressRow("Starting the connection") }
        PortfolioConnectionState.CompletingConnection ->
            BannerCard(modifier) { ProgressRow("Connecting your demo portfolio") }
        is PortfolioConnectionState.OpeningPortal ->
            BannerCard(modifier) { ProgressRow("Opening SnapTrade Sandbox") }
        is PortfolioConnectionState.WaitingForReturn -> BannerCard(modifier) {
            Text("Finish connecting in your browser", fontWeight = FontWeight.SemiBold)
            Text(
                "Choose SnapTrade Sandbox and a simulated account, then return to EasyMoney.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onFinished) { Text("I've finished") }
                TextButton(onClick = onCancelWaiting) { Text("Cancel") }
            }
        }
        is PortfolioConnectionState.ReconnectRequired -> BannerCard(modifier) {
            Text("Reconnect the portfolio to continue updating it.", fontWeight = FontWeight.SemiBold)
            Button(onClick = onReconnect) { Text("Reconnect") }
        }
        is PortfolioConnectionState.Error -> BannerCard(modifier, error = true) {
            Text(state.message, color = MaterialTheme.colorScheme.onErrorContainer)
            state.requestId?.let {
                Text(
                    "Reference: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            TextButton(onClick = onDismissError) { Text("Dismiss") }
        }
        else -> Unit
    }
}

@Composable
private fun BannerCard(modifier: Modifier, error: Boolean = false, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (error) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
    }
}

@Composable
private fun ProgressRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CircularProgressIndicator(modifier = Modifier.padding(2.dp))
        Text(text)
    }
}

@Composable
fun SourceSelector(
    available: List<ActivePortfolioSource>,
    selected: ActivePortfolioSource,
    onSelect: (ActivePortfolioSource) -> Unit,
    modifier: Modifier = Modifier
) {
    if (available.size < 2) return
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Portfolio source", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            available.forEach { source ->
                FilterChip(selected = source == selected, onClick = { onSelect(source) }, label = { Text(source.label) })
            }
        }
        Text(
            "Portfolio sources are displayed separately to prevent duplicate holdings.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Source label shown above the portfolio so CSV and sandbox data are never confused. */
@Composable
fun SourceLabel(source: ActivePortfolioSource, modifier: Modifier = Modifier) {
    Text(
        text = when (source) {
            ActivePortfolioSource.CSV -> "Imported CSV"
            ActivePortfolioSource.SNAPTRADE_SANDBOX -> "SnapTrade Sandbox · Simulated data"
        },
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
    )
}

@Composable
fun ConnectedPortfolioPanel(
    connection: PortfolioConnection,
    syncing: Boolean,
    onSync: () -> Unit,
    onDetails: () -> Unit,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Connected portfolio", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(connection.institutionName, style = MaterialTheme.typography.bodyLarge)
            Text("Read-only", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(
                "Last synced: ${fmt(connection.lastSuccessfulSyncAt)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (syncing) LinearProgressIndicator(Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSync, enabled = !syncing) { Text("Sync now") }
                OutlinedButton(onClick = onDetails) { Text("Details") }
                TextButton(onClick = onDisconnect, enabled = !syncing) { Text("Disconnect") }
            }
        }
    }
}

@Composable
fun ConnectionDetailsSheet(
    connection: PortfolioConnection,
    onReconnect: () -> Unit,
    onDisconnect: () -> Unit,
    onDeleteData: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Connection details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            DetailRow("Provider", "SnapTrade")
            DetailRow("Institution", connection.institutionName)
            DetailRow("Access", "Read-only")
            DetailRow("Status", connection.status.label())
            DetailRow("Connected", fmt(connection.createdAt))
            DetailRow("Last sync", fmt(connection.lastSuccessfulSyncAt))
            DetailRow("Accounts", connection.accountCount.toString())
            DetailRow("Positions", connection.positionCount.toString())
            if (connection.status == ConnectionStatus.RECONNECT_REQUIRED) {
                Button(onClick = onReconnect, modifier = Modifier.fillMaxWidth()) { Text("Reconnect") }
            }
            OutlinedButton(onClick = onDisconnect, modifier = Modifier.fillMaxWidth()) { Text("Disconnect") }
            TextButton(onClick = onDeleteData, modifier = Modifier.fillMaxWidth()) {
                Text("Delete synchronized data", color = MaterialTheme.colorScheme.error)
            }
            Text(
                "SnapTrade Sandbox uses simulated data. EasyMoney cannot place trades or move money.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value)
    }
}

private fun ConnectionStatus.label(): String = when (this) {
    ConnectionStatus.READY -> "Ready"
    ConnectionStatus.CONNECTED -> "Connected"
    ConnectionStatus.SYNCING -> "Syncing"
    ConnectionStatus.RECONNECT_REQUIRED -> "Reconnect required"
    ConnectionStatus.ERROR -> "Needs attention"
    ConnectionStatus.DISABLED -> "Disabled"
    ConnectionStatus.DISCONNECTED -> "Disconnected"
    ConnectionStatus.PENDING -> "Pending"
    ConnectionStatus.UNKNOWN -> "Unknown"
}

@Composable
fun DisconnectConfirmDialog(onCancel: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Disconnect demo brokerage?") },
        text = {
            Text(
                "EasyMoney will stop reading this simulated account. Your Wealthsimple CSV portfolio is not affected."
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Disconnect") } },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancel") } }
    )
}

@Composable
fun DeleteSyncedDataDialog(onCancel: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Delete synchronized data?") },
        text = { Text("Synchronized demo portfolio data will be removed. Your CSV portfolio is not affected.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Delete") } },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancel") } }
    )
}
