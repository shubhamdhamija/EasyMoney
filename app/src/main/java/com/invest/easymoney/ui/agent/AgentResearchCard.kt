package com.invest.easymoney.ui.agent

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.invest.easymoney.domain.model.AgentResearchReport
import com.invest.easymoney.domain.model.FinancialCitation
import java.util.Locale

@Composable
fun AgentResearchCard(report: AgentResearchReport, modifier: Modifier = Modifier, onDismiss: (() -> Unit)? = null) {
    val quote = report.quote
    val asOf = formatAsOf(quote?.dataTimestamp)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag(AgentResearchTestTags.RESULT_CARD),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text("AI Research", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(report.symbol, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                quote?.let {
                    val positive = it.priceChangePercent >= 0
                    val trendColor = if (positive) Color(0xFF2E7D32) else Color(0xFFB00020)
                    Surface(shape = RoundedCornerShape(50), color = trendColor.copy(alpha = 0.12f)) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (positive) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
                                contentDescription = if (positive) "Up" else "Down",
                                tint = trendColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "${it.currency} ${String.format(Locale.US, "%.2f", it.currentPrice)} · ${if (positive) "+" else ""}${String.format(Locale.US, "%.2f", it.priceChangePercent)}%",
                                color = trendColor,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            }
            if (report.summary.isNotBlank()) {
                Text(report.summary, style = MaterialTheme.typography.bodyMedium)
            }
            if (asOf != null) {
                Text(
                    "Price as of $asOf",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (report.headlines.isNotEmpty()) {
                Text("Headlines reviewed", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    report.headlines.forEach { item ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(item.headline, style = MaterialTheme.typography.bodyMedium)
                            val meta = listOfNotNull(
                                item.source.takeIf { it.isNotBlank() },
                                formatAsOf(item.publishedAt)
                            ).joinToString(" · ")
                            if (meta.isNotEmpty()) {
                                Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            } else if (report.newsCount > 0) {
                Text(
                    "${report.newsCount} recent article${if (report.newsCount == 1) "" else "s"} reviewed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (report.evidenceSummary.isNotEmpty()) {
                Text("Evidence", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    report.evidenceSummary.forEach { statement ->
                        Text("• $statement", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            if (report.citations.isNotEmpty()) {
                Text("Citations", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    report.citations.forEachIndexed { index, citation ->
                        AgentCitationListItem(citation = citation, index = index + 1)
                    }
                }
            }
            Text(
                "For informational purposes only. Not financial advice.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            onDismiss?.let {
                TextButton(onClick = it) { Text("Dismiss") }
            }
        }
    }
}


@Composable
private fun AgentCitationListItem(citation: FinancialCitation, index: Int) {
    val uriHandler = LocalUriHandler.current
    val validUrl = safeHttpsUrl(citation.sourceUrl)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Text("$index.", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(6.dp))
            Text(citation.title.ifBlank { "Source" }, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
        Text(
            text = buildString {
                val type = citation.documentType.replace('_', ' ').replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
                append(type)
                if (citation.sourceName.isNotBlank()) append(" · ${citation.sourceName}")
                if (citation.sectionName.isNotBlank()) append(" · ${citation.sectionName}")
                if (citation.publishedAt.isNotBlank()) append(" · ${citation.publishedAt}")
                if (!citation.accessionNumber.isNullOrBlank()) append(" · ${citation.accessionNumber}")
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (validUrl != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { uriHandler.openUri(validUrl) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(validUrl, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        } else if (citation.sourceName.isNotBlank()) {
            Text(citation.sourceName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun safeHttpsUrl(rawUrl: String?): String? {
    val candidate = rawUrl?.trim() ?: return null
    if (candidate.isEmpty()) return null
    val uri = runCatching { Uri.parse(candidate) }.getOrNull() ?: return null
    val scheme = uri.scheme?.lowercase(Locale.US) ?: return null
    if (scheme != "https") return null
    val host = uri.host ?: return null
    if (host.isBlank()) return null
    return uri.toString()
}
