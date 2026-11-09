package com.invest.easymoney.data.api.dto

import com.google.gson.annotations.SerializedName

data class AgentResearchRequestDto(
    @SerializedName("symbol") val symbol: String,
    @SerializedName("maximum_news_items") val maximumNewsItems: Int = 3,
    @SerializedName("maximum_tool_calls") val maximumToolCalls: Int = 5,
    @SerializedName("maximum_decisions") val maximumDecisions: Int = 8,
)

data class AgentResearchCompletedResponseDto(
    @SerializedName("status") val status: String? = null,
    @SerializedName("thread_id") val threadId: String? = null,
    @SerializedName("report") val report: AgentResearchReportDto? = null,
    @SerializedName("request_id") val requestId: String? = null,
    @SerializedName("symbol") val symbol: String? = null,
    @SerializedName("summary") val summary: String? = null,
    @SerializedName("quote") val quote: Map<String, Any?>? = null,
    @SerializedName("news_count") val newsCount: Int? = null,
    @SerializedName("tools_used") val toolsUsed: List<String>? = null,
    @SerializedName("error_code") val errorCode: String? = null,
    @SerializedName("agent_used") val agentUsed: Boolean? = null,
    @SerializedName("evidence_summary") val evidenceSummary: List<String>? = null,
    @SerializedName("citations") val citations: List<FinancialCitationDto>? = null,
    @SerializedName("disclaimer") val disclaimer: String? = null,
    @SerializedName("headlines") val headlines: List<AgentHeadlineDto>? = null,
)

data class AgentHeadlineDto(
    @SerializedName("headline") val headline: String? = null,
    @SerializedName("source") val source: String? = null,
    @SerializedName("published_at") val publishedAt: String? = null,
)

data class AgentResearchReportDto(
    @SerializedName("symbol") val symbol: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("summary") val summary: String? = null,
    @SerializedName("quote") val quote: Map<String, Any?>? = null,
    @SerializedName("news_count") val newsCount: Int? = null,
    @SerializedName("tools_used") val toolsUsed: List<String>? = null,
    @SerializedName("error_code") val errorCode: String? = null,
    @SerializedName("agent_used") val agentUsed: Boolean? = null,
    @SerializedName("evidence_summary") val evidenceSummary: List<String>? = null,
    @SerializedName("citations") val citations: List<FinancialCitationDto>? = null,
    @SerializedName("disclaimer") val disclaimer: String? = null,
    @SerializedName("headlines") val headlines: List<AgentHeadlineDto>? = null,
)

data class FinancialCitationDto(
    @SerializedName("citation_id") val citationId: String? = null,
    @SerializedName("document_id") val documentId: String? = null,
    @SerializedName("symbol") val symbol: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("document_type") val documentType: String? = null,
    @SerializedName("section_name") val sectionName: String? = null,
    @SerializedName("published_at") val publishedAt: String? = null,
    @SerializedName("source_name") val sourceName: String? = null,
    @SerializedName("source_url") val sourceUrl: String? = null,
    @SerializedName("accession_number") val accessionNumber: String? = null,
)

data class AgentApprovalRequiredResponseDto(
    @SerializedName("status") val status: String? = null,
    @SerializedName("thread_id") val threadId: String? = null,
    @SerializedName("approval") val approval: ToolApprovalDto? = null,
    @SerializedName("request_id") val requestId: String? = null,
)

data class ToolApprovalDto(
    @SerializedName("approval_id") val approvalId: String? = null,
    @SerializedName("tool_name") val toolName: String? = null,
    @SerializedName("safe_arguments") val safeArguments: SaveResearchReportApprovalArgumentsDto? = null,
    @SerializedName("reason") val reason: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("expires_at") val expiresAt: String? = null,
)

data class SaveResearchReportApprovalArgumentsDto(
    @SerializedName("symbol") val symbol: String? = null,
    @SerializedName("report_summary") val reportSummary: String? = null,
)

data class ToolApprovalDecisionRequestDto(
    @SerializedName("approval_id") val approvalId: String,
    @SerializedName("approved") val approved: Boolean,
    @SerializedName("reviewer_note") val reviewerNote: String? = null,
)

data class AgentErrorResponseDto(
    @SerializedName("error") val error: AgentErrorDto? = null,
    @SerializedName("request_id") val requestId: String? = null,
)

data class AgentErrorDto(
    @SerializedName("code") val code: String? = null,
    @SerializedName("message") val message: String? = null,
)



