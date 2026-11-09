package com.invest.easymoney.domain.model

import java.util.Locale

enum class AgentReportStatus {
    COMPLETED,
    PARTIAL,
    FAILED,
    UNKNOWN,
}

enum class AgentResearchStage {
    STARTING,
    COLLECTING_QUOTE,
    REVIEWING_NEWS,
    SEARCHING_DOCUMENTS,
    PREPARING_REPORT,
    SUBMITTING_APPROVAL,
    RESEARCHING_AVAILABLE_INFORMATION,
}

data class AgentQuote(
    val symbol: String,
    val currentPrice: Double,
    val priceChangePercent: Double,
    val currency: String,
    val dataTimestamp: String?
)

data class FinancialCitation(
    val citationId: String,
    val documentId: String,
    val symbol: String,
    val title: String,
    val documentType: String,
    val sectionName: String,
    val publishedAt: String,
    val sourceName: String,
    val sourceUrl: String?,
    val accessionNumber: String?
)

data class ToolApproval(
    val approvalId: String,
    val toolName: String,
    val symbol: String,
    val reportSummary: String,
    val reason: String,
    val createdAt: String?,
    val expiresAt: String?
)

data class AgentHeadline(
    val headline: String,
    val source: String,
    val publishedAt: String?
)

data class AgentResearchReport(
    val symbol: String,
    val status: AgentReportStatus,
    val summary: String,
    val quote: AgentQuote?,
    val newsCount: Int,
    val toolsUsed: List<String>,
    val evidenceSummary: List<String>,
    val citations: List<FinancialCitation>,
    val agentUsed: Boolean,
    val errorCode: String?,
    val headlines: List<AgentHeadline> = emptyList()
)

sealed interface AgentResearchResult {
    data class Completed(val report: AgentResearchReport) : AgentResearchResult
    data class ApprovalRequired(
        val threadId: String,
        val approval: ToolApproval,
        val requestId: String?
    ) : AgentResearchResult
    data class Failed(
        val message: String,
        val requestId: String?,
        val retryable: Boolean
    ) : AgentResearchResult
}

fun String.toAgentReportStatus(): AgentReportStatus = when (trim().lowercase(Locale.US)) {
    "completed" -> AgentReportStatus.COMPLETED
    "partial" -> AgentReportStatus.PARTIAL
    "failed" -> AgentReportStatus.FAILED
    else -> AgentReportStatus.UNKNOWN
}


