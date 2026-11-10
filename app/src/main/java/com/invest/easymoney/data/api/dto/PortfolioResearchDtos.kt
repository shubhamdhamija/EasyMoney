package com.invest.easymoney.data.api.dto

import com.google.gson.annotations.SerializedName

/** Wire DTOs for read-only portfolio research. No order/trade models exist. */

data class LocalPositionDto(
    @SerializedName("symbol") val symbol: String?,
    @SerializedName("security_name") val securityName: String,
    @SerializedName("security_type") val securityType: String,
    @SerializedName("account_type") val accountType: String,
    @SerializedName("quantity") val quantity: String,
    @SerializedName("market_value") val marketValue: String?,
    @SerializedName("currency") val currency: String,
    @SerializedName("book_value") val bookValue: String?
)

/** Normalized, bounded CSV context. No file names, URIs, account numbers or hashes. */
data class LocalPortfolioContextDto(
    @SerializedName("source") val source: String,
    @SerializedName("snapshot_at") val snapshotAt: String,
    @SerializedName("positions") val positions: List<LocalPositionDto>
)

data class LocalContextResponseDto(
    @SerializedName("portfolio_id") val portfolioId: String?,
    @SerializedName("request_id") val requestId: String?
)

data class ResearchConsentRequestDto(
    @SerializedName("included_data_categories") val categories: List<String>
)

data class ResearchConsentResponseDto(
    @SerializedName("consent_id") val consentId: String?,
    @SerializedName("portfolio_id") val portfolioId: String?,
    @SerializedName("expires_at") val expiresAt: String?,
    @SerializedName("request_id") val requestId: String?
)

/** Pointer + toggles only. Never user/tenant IDs, provider IDs, account numbers or holdings. */
data class PortfolioContextRequestDto(
    @SerializedName("portfolio_id") val portfolioId: String,
    @SerializedName("source") val source: String,
    @SerializedName("include_positions") val includePositions: Boolean,
    @SerializedName("include_cash") val includeCash: Boolean,
    @SerializedName("include_book_value") val includeBookValue: Boolean,
    @SerializedName("maximum_positions") val maximumPositions: Int,
    @SerializedName("consent_id") val consentId: String
)

data class PortfolioResearchRequestDto(
    @SerializedName("portfolio_context") val portfolioContext: PortfolioContextRequestDto,
    @SerializedName("maximum_news_items") val maximumNewsItems: Int = 3,
    @SerializedName("maximum_tool_calls") val maximumToolCalls: Int = 6,
    @SerializedName("maximum_decisions") val maximumDecisions: Int = 8
)

data class ResearchCurrencySummaryDto(
    @SerializedName("currency") val currency: String?,
    @SerializedName("investment_market_value") val investmentMarketValue: String?,
    @SerializedName("cash_value") val cashValue: String?,
    @SerializedName("book_value") val bookValue: String?,
    @SerializedName("unrealized_return") val unrealizedReturn: String?,
    @SerializedName("position_count") val positionCount: Int?
)

data class ResearchPositionDto(
    @SerializedName("symbol") val symbol: String?,
    @SerializedName("security_name") val securityName: String?,
    @SerializedName("security_type") val securityType: String?,
    @SerializedName("account_type") val accountType: String?,
    @SerializedName("quantity") val quantity: String?,
    @SerializedName("market_value") val marketValue: String?,
    @SerializedName("currency") val currency: String?,
    @SerializedName("book_value") val bookValue: String?
)

data class ResearchWeightedDto(
    @SerializedName("symbol") val symbol: String?,
    @SerializedName("security_name") val securityName: String?,
    @SerializedName("market_value") val marketValue: String?,
    @SerializedName("weight_percent") val weightPercent: String?
)

data class ResearchConcentrationDto(
    @SerializedName("currency") val currency: String?,
    @SerializedName("eligible_market_value") val eligibleMarketValue: String?,
    @SerializedName("excluded_position_count") val excludedPositionCount: Int?,
    @SerializedName("largest_position") val largestPosition: ResearchWeightedDto?,
    @SerializedName("top_positions") val topPositions: List<ResearchWeightedDto>?,
    @SerializedName("top_positions_weight_percent") val topPositionsWeightPercent: String?,
    @SerializedName("cash_allocation_percent") val cashAllocationPercent: String?
)

data class ResearchWarningDto(
    @SerializedName("code") val code: String?,
    @SerializedName("currency") val currency: String?,
    @SerializedName("count") val count: Int?
)

data class PortfolioResearchReportDto(
    @SerializedName("status") val status: String?,
    @SerializedName("summary") val summary: String?,
    @SerializedName("error_code") val errorCode: String?,
    @SerializedName("evidence_summary") val evidenceSummary: List<String>?,
    @SerializedName("portfolio_source") val portfolioSource: String?,
    @SerializedName("portfolio_snapshot_at") val snapshotAt: String?,
    @SerializedName("portfolio_currency_summaries") val currencySummaries: List<ResearchCurrencySummaryDto>?,
    @SerializedName("portfolio_positions") val positions: List<ResearchPositionDto>?,
    @SerializedName("portfolio_concentration") val concentration: List<ResearchConcentrationDto>?,
    @SerializedName("portfolio_warnings") val warnings: List<ResearchWarningDto>?,
    @SerializedName("portfolio_disclaimer") val disclaimer: String?,
    @SerializedName("request_id") val requestId: String?
)

data class PortfolioResearchErrorDto(
    @SerializedName("error") val error: PortfolioResearchErrorBodyDto?,
    @SerializedName("request_id") val requestId: String?
)

data class PortfolioResearchErrorBodyDto(
    @SerializedName("code") val code: String?,
    @SerializedName("message") val message: String?
)
