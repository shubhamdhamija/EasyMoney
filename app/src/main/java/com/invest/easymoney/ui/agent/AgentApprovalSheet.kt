package com.invest.easymoney.ui.agent

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.invest.easymoney.domain.model.ToolApproval

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentApprovalSheet(
    approval: ToolApproval,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Text("Save this research report?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text("Symbol: ${approval.symbol}", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(6.dp))
            Text("Prepared report summary:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(approval.reportSummary, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Text("Reason: ${approval.reason}", style = MaterialTheme.typography.bodyMedium)
            if (!approval.expiresAt.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text("Expiration: ${approval.expiresAt}", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "This saves the generated research report. It does not place a trade or modify your portfolio.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
            OutlinedButton(
                onClick = onReject,
                enabled = !isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Not now") }
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onApprove,
                enabled = !isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (isSubmitting) "Saving..." else "Approve and save") }
            Spacer(Modifier.height(12.dp))
        }
    }
}

