package com.invest.easymoney.data.repository

import com.google.gson.Gson
import com.invest.easymoney.data.api.AgentAccessTokenProvider
import com.invest.easymoney.data.api.BackendApiService
import com.invest.easymoney.data.api.dto.AgentApprovalRequiredResponseDto
import com.invest.easymoney.data.api.dto.AgentErrorResponseDto
import com.invest.easymoney.data.api.dto.AgentResearchCompletedResponseDto
import com.invest.easymoney.data.api.dto.AgentResearchRequestDto
import com.invest.easymoney.data.api.dto.AgentResearchReportDto
import com.invest.easymoney.data.api.dto.FinancialCitationDto
import com.invest.easymoney.data.api.dto.ToolApprovalDecisionRequestDto
import com.invest.easymoney.domain.model.AgentHeadline
import com.invest.easymoney.domain.model.AgentQuote
import com.invest.easymoney.domain.model.AgentReportStatus
import com.invest.easymoney.domain.model.AgentResearchReport
import com.invest.easymoney.domain.model.AgentResearchResult
import com.invest.easymoney.domain.model.FinancialCitation
import com.invest.easymoney.domain.model.NetworkResult
import com.invest.easymoney.domain.model.ToolApproval
import com.invest.easymoney.domain.model.toAgentReportStatus
import com.invest.easymoney.domain.repository.AgentResearchRepository
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgentResearchRepositoryImpl @Inject constructor(
    private val backendApiService: BackendApiService,
    private val tokenProvider: AgentAccessTokenProvider,
    private val gson: Gson,
) : AgentResearchRepository {

    override suspend fun startResearch(symbol: String): NetworkResult<AgentResearchResult> {
        return try {
            val normalized = symbol.trim().uppercase()
            if (normalized.isBlank()) {
                return NetworkResult.Error(IllegalArgumentException("Symbol is required."))
            }
            val response = backendApiService.startAgentResearch(
                AgentResearchRequestDto(
                    symbol = normalized,
                    maximumNewsItems = 3,
                    maximumToolCalls = 5,
                    maximumDecisions = 8,
                )
            )
            mapResearchResponse(response)
        } catch (e: CancellationException) {
            throw e
        } catch (e: SocketTimeoutException) {
            NetworkResult.Error(IllegalStateException("The AI research request timed out."))
        } catch (e: IOException) {
            NetworkResult.Error(IllegalStateException("Check your connection and try again."))
        } catch (e: HttpException) {
            NetworkResult.Error(IllegalStateException(mapHttpError(e)))
        } catch (e: Throwable) {
            NetworkResult.Error(IllegalStateException("AI research could not be completed."))
        }
    }

    override suspend fun submitApproval(
        threadId: String,
        approvalId: String,
        approved: Boolean,
        reviewerNote: String?
    ): NetworkResult<AgentResearchResult> {
        return try {
            val response = backendApiService.submitAgentApproval(
                threadId = threadId,
                request = ToolApprovalDecisionRequestDto(
                    approvalId = approvalId,
                    approved = approved,
                    reviewerNote = reviewerNote,
                )
            )
            mapResearchResponse(response)
        } catch (e: CancellationException) {
            throw e
        } catch (e: SocketTimeoutException) {
            NetworkResult.Error(IllegalStateException("The AI research request timed out."))
        } catch (e: IOException) {
            NetworkResult.Error(IllegalStateException("Check your connection and try again."))
        } catch (e: HttpException) {
            NetworkResult.Error(IllegalStateException(mapHttpError(e)))
        } catch (e: Throwable) {
            NetworkResult.Error(IllegalStateException("AI research could not be completed."))
        }
    }

    override suspend fun retryResearch(symbol: String): NetworkResult<AgentResearchResult> = startResearch(symbol)

    private fun mapResearchResponse(response: Response<okhttp3.ResponseBody>): NetworkResult<AgentResearchResult> {
        val statusCode = response.code()
        if (response.isSuccessful) {
            val payload = response.body()?.string().orEmpty()
            if (payload.isBlank()) {
                return NetworkResult.Error(IllegalStateException("AI research could not be completed."))
            }
            val approvalParsed = runCatching { gson.fromJson(payload, AgentApprovalRequiredResponseDto::class.java) }.getOrNull()
            if (approvalParsed?.approval != null || approvalParsed?.status.equals("approval_required", ignoreCase = true)) {
                return approvalParsed!!.toDomainResult()
            }
            val parsed = runCatching { gson.fromJson(payload, AgentResearchCompletedResponseDto::class.java) }.getOrNull()
                ?: return NetworkResult.Error(IllegalStateException("AI research could not be completed."))
            return parsed.toDomainResult()
        }
        val errorBody = response.errorBody()?.string().orEmpty()
        val safe = runCatching { gson.fromJson(errorBody, AgentErrorResponseDto::class.java) }.getOrNull()
        val requestId = safe?.requestId
        val message = when (statusCode) {
            401 -> "Sign in again to continue AI research."
            403 -> "You do not have permission to perform this action."
            404 -> "This research session is no longer available."
            409 -> "This approval is expired or has already been used."
            429 -> "AI research is receiving too many requests. Please try again later."
            503 -> "AI research is temporarily unavailable."
            in 500..599 -> "AI research is temporarily unavailable."
            else -> "AI research could not be completed."
        }
        return NetworkResult.Error(AgentResearchException(message, requestId, retryable = statusCode !in setOf(401, 403, 404, 409)))
    }

    private fun mapHttpError(exception: HttpException): String {
        val code = exception.code()
        return when (code) {
            401 -> "Sign in again to continue AI research."
            403 -> "You do not have permission to perform this action."
            404 -> "This research session is no longer available."
            409 -> "This approval is expired or has already been used."
            429 -> "AI research is receiving too many requests. Please try again later."
            503 -> "AI research is temporarily unavailable."
            in 500..599 -> "AI research is temporarily unavailable."
            else -> "AI research could not be completed."
        }
    }

    private fun AgentResearchCompletedResponseDto.toDomainResult(): NetworkResult<AgentResearchResult> {
        val status = this.status.orEmpty()
        val report = this.report
        if (report != null) {
            return NetworkResult.Success(AgentResearchResult.Completed(report = report.toDomain()))
        }
        // Backend may return the report fields flat (no nested "report" object).
        if (!this.summary.isNullOrBlank() && !status.equals("failed", ignoreCase = true)) {
            val flat = AgentResearchReportDto(
                symbol = this.symbol,
                status = this.status,
                summary = this.summary,
                quote = this.quote,
                newsCount = this.newsCount,
                toolsUsed = this.toolsUsed,
                errorCode = this.errorCode,
                agentUsed = this.agentUsed,
                evidenceSummary = this.evidenceSummary,
                citations = this.citations,
                disclaimer = this.disclaimer,
                headlines = this.headlines,
            )
            return NetworkResult.Success(AgentResearchResult.Completed(report = flat.toDomain()))
        }
        val failure = describeResearchFailure(this.errorCode, this.symbol)
        return NetworkResult.Success(AgentResearchResult.Failed(
            message = failure.message,
            requestId = this.requestId,
            retryable = failure.retryable,
        ))
    }

    private fun AgentApprovalRequiredResponseDto.toDomainResult(): NetworkResult<AgentResearchResult> {
        val approval = approval ?: return NetworkResult.Error(IllegalStateException("AI research could not be completed."))
        val threadIdValue = threadId ?: return NetworkResult.Error(IllegalStateException("AI research could not be completed."))
        val approvalId = approval.approvalId ?: return NetworkResult.Error(IllegalStateException("AI research could not be completed."))
        val toolName = approval.toolName ?: return NetworkResult.Error(IllegalStateException("AI research could not be completed."))
        val safeSymbol = approval.safeArguments?.symbol ?: ""
        val summary = approval.safeArguments?.reportSummary ?: ""
        val mapped = ToolApproval(
            approvalId = approvalId,
            toolName = toolName,
            symbol = safeSymbol,
            reportSummary = summary,
            reason = approval.reason ?: "Save the completed research report.",
            createdAt = approval.createdAt,
            expiresAt = approval.expiresAt,
        )
        return NetworkResult.Success(AgentResearchResult.ApprovalRequired(threadId = threadIdValue, approval = mapped, requestId = requestId))
    }

    private fun AgentResearchReportDto.toDomain(): AgentResearchReport = AgentResearchReport(
        symbol = (symbol ?: "").trim().uppercase(),
        status = (status ?: "failed").toAgentReportStatus(),
        summary = (summary ?: "").trim(),
        quote = quote?.toQuote(symbol ?: ""),
        newsCount = newsCount ?: 0,
        toolsUsed = (toolsUsed ?: emptyList()).map { it.trim() }.filter { it.isNotEmpty() }.take(5),
        evidenceSummary = (evidenceSummary ?: emptyList()).map { it.trim() }.filter { it.isNotBlank() }.take(5),
        citations = (citations ?: emptyList()).map { it.toDomain() }.distinctBy { it.citationId }.take(10),
        agentUsed = agentUsed ?: false,
        errorCode = errorCode?.trim()?.ifEmpty { null },
        headlines = (headlines ?: emptyList()).mapNotNull { dto ->
            val title = dto.headline?.trim().orEmpty()
            if (title.isEmpty()) null
            else AgentHeadline(title, dto.source?.trim().orEmpty(), dto.publishedAt?.trim()?.takeIf { it.isNotEmpty() })
        }.take(5),
    )

    private fun FinancialCitationDto.toDomain(): FinancialCitation = FinancialCitation(
        citationId = citationId?.trim().orEmpty(),
        documentId = documentId?.trim().orEmpty(),
        symbol = (symbol ?: "").trim().uppercase(),
        title = (title ?: "").trim(),
        documentType = (documentType ?: "unknown").trim(),
        sectionName = (sectionName ?: "").trim(),
        publishedAt = publishedAt?.trim().orEmpty(),
        sourceName = (sourceName ?: "").trim(),
        sourceUrl = sourceUrl?.trim()?.takeIf { it.isNotBlank() },
        accessionNumber = accessionNumber?.trim()?.takeIf { it.isNotBlank() },
    )

    private fun Map<String, Any?>.toQuote(defaultSymbol: String): AgentQuote? {
        val currentPrice = (this["current_price"] as? Number)?.toDouble() ?: (this["currentPrice"] as? Number)?.toDouble() ?: return null
        val changePercent = (this["price_change_percent"] as? Number)?.toDouble() ?: (this["priceChangePercent"] as? Number)?.toDouble() ?: 0.0
        val currency = (this["currency"] as? String)?.takeIf { it.isNotBlank() } ?: "USD"
        val timestamp = this["data_timestamp"] as? String ?: this["dataTimestamp"] as? String
        return AgentQuote(
            symbol = (this["symbol"] as? String ?: defaultSymbol).trim().uppercase(),
            currentPrice = currentPrice,
            priceChangePercent = changePercent,
            currency = currency,
            dataTimestamp = timestamp,
        )
    }
}

class AgentResearchException(
    message: String,
    val requestId: String?,
    val retryable: Boolean = true,
) : Exception(message)






