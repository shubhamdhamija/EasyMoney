package com.invest.easymoney.data.repository

import com.google.gson.Gson
import com.invest.easymoney.data.api.BackendApiService
import com.invest.easymoney.data.api.WatchlistChangePayload
import com.invest.easymoney.data.api.dto.StockEvidenceRequest
import com.invest.easymoney.data.api.dto.StockExplanationErrorResponse
import com.invest.easymoney.data.api.dto.StockExplanationApiResponse
import com.invest.easymoney.data.api.dto.StockExplanationRequest
import com.invest.easymoney.domain.model.EarningsReport
import com.invest.easymoney.domain.model.EvidenceSentiment
import com.invest.easymoney.domain.model.NetworkResult
import com.invest.easymoney.domain.model.News
import com.invest.easymoney.domain.model.MovementDirection
import com.invest.easymoney.domain.model.Stock
import com.invest.easymoney.domain.model.StockExplanation
import com.invest.easymoney.domain.model.UpcomingEarning
import com.invest.easymoney.domain.repository.BackendRepository
import com.invest.easymoney.util.Resource
import retrofit2.HttpException
import javax.inject.Inject

class BackendRepositoryImpl @Inject constructor(
    private val backendApiService: BackendApiService,
    private val gson: Gson
) : BackendRepository {

    override suspend fun notifyWatchlistChange(deviceId: String, symbol: String, action: String) {
        try {
            backendApiService.notifyWatchlistChange(
                WatchlistChangePayload(userId = deviceId, symbol = symbol, action = action)
            )
        } catch (e: Exception) {
            // Fire-and-forget, so we log but don't throw
            e.printStackTrace()
        }
    }

    override suspend fun explainMove(stock: Stock, news: List<News>): NetworkResult<StockExplanation> {
        return try {
            val response = backendApiService.explainStockMovement(createExplanationRequest(stock, news))
            if (response.ok) {
                NetworkResult.Success(response.toDomain())
            } else {
                val errorMessage = response.error?.message ?: "Unknown error"
                NetworkResult.Error(IllegalStateException(errorMessage))
            }
        } catch (e: IllegalArgumentException) {
            NetworkResult.Error(e)
        } catch (e: IllegalStateException) {
            NetworkResult.Error(e)
        } catch (e: HttpException) {
            NetworkResult.Error(mapBackendHttpException(e))
        } catch (e: Exception) {
            NetworkResult.Error(e)
        }
    }

    private fun createExplanationRequest(stock: Stock, news: List<News>): StockExplanationRequest {
        return StockExplanationRequest(
            symbol = stock.symbol.trim().uppercase(),
            companyName = stock.name.takeIf(String::isNotBlank),
            priceChangePercent = stock.changePercent,
            marketChangePercent = null,
            dayLow = stock.lowPrice,
            dayHigh = stock.highPrice,
            evidence = news.take(7).map { article ->
                StockEvidenceRequest(
                    type = "news",
                    headline = article.headline,
                    source = article.source.takeIf(String::isNotBlank)
                )
            }
        )
    }

    private fun mapBackendHttpException(exception: HttpException): Throwable {
        val errorBody = exception.response()?.errorBody()?.string().orEmpty()
        val parsed = runCatching {
            gson.fromJson(errorBody, StockExplanationErrorResponse::class.java)
        }.getOrNull()

        val message = parsed?.error?.message
            ?: exception.message()
            ?: "Unable to explain move"

        return BackendApiException(
            code = parsed?.error?.code,
            requestId = parsed?.requestId,
            message = message,
            cause = exception
        )
    }

    override suspend fun getEarningsReport(symbol: String): Resource<EarningsReport> {
        return try {
            val response = backendApiService.getEarningsReport(symbol)
            if (response.ok && response.earnings != null) {
                val dto = response.earnings
                val model = EarningsReport(
                    symbol = dto.symbol,
                    date = dto.date,
                    epsEstimate = dto.epsEstimate,
                    epsActual = dto.epsActual,
                    revenueEstimate = dto.revenueEstimate,
                    revenueActual = dto.revenueActual,
                    surprise = dto.surprise,
                    surprisePercent = dto.surprisePercent
                )
                Resource.Success(model)
            } else {
                Resource.Error(response.error ?: "Unknown error")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An error occurred")
        }
    }

    override suspend fun getUpcomingEarnings(symbols: List<String>): Resource<List<UpcomingEarning>> {
        return try {
            val response = backendApiService.getUpcomingEarnings()
            if (response.ok && response.earnings != null) {
                val symbolSet = symbols.toSet()
                val filtered = response.earnings
                    .filter { it.symbol in symbolSet }
                    .map { dto ->
                        UpcomingEarning(
                            symbol = dto.symbol,
                            companyName = dto.companyName,
                            earningsDate = dto.earningsDate,
                            fiscalPeriod = dto.fiscalPeriod,
                            fiscalEndDate = dto.fiscalEndDate
                        )
                    }
                Resource.Success(filtered)
            } else {
                Resource.Error(response.error ?: "Unknown error")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An error occurred")
        }
    }
}

class BackendApiException(
    val code: String?,
    val requestId: String?,
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

private fun StockExplanationApiResponse.toDomain(): StockExplanation {
    val value = requireNotNull(explanation) {
        "Missing stock explanation"
    }

    return StockExplanation(
        symbol = value.symbol,
        direction = value.direction.toMovementDirection(),
        evidenceSentiment = value.evidenceSentiment.toEvidenceSentiment(),
        summary = value.summary,
        factors = value.factors,
        evidenceSufficient = value.evidenceSufficient,
        disclaimer = value.disclaimer,
        requestId = requestId
    )
}

private fun String.toMovementDirection(): MovementDirection =
    when (trim().lowercase()) {
        "increase" -> MovementDirection.INCREASE
        "decrease" -> MovementDirection.DECREASE
        "unchanged" -> MovementDirection.UNCHANGED
        else -> throw IllegalArgumentException("Unsupported movement direction: $this")
    }

private fun String.toEvidenceSentiment(): EvidenceSentiment =
    when (trim().lowercase()) {
        "positive" -> EvidenceSentiment.POSITIVE
        "negative" -> EvidenceSentiment.NEGATIVE
        "mixed" -> EvidenceSentiment.MIXED
        "neutral" -> EvidenceSentiment.NEUTRAL
        "insufficient" -> EvidenceSentiment.INSUFFICIENT
        else -> throw IllegalArgumentException("Unsupported evidence sentiment: $this")
    }

