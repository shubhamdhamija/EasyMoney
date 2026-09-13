package com.invest.easymoney.data.repository

import com.invest.easymoney.data.api.BackendApiService
import com.invest.easymoney.data.api.WatchlistChangePayload
import com.invest.easymoney.domain.model.EarningsReport
import com.invest.easymoney.domain.model.NetworkResult
import com.invest.easymoney.domain.model.UpcomingEarning
import com.invest.easymoney.domain.repository.BackendRepository
import com.invest.easymoney.util.Resource
import javax.inject.Inject

class BackendRepositoryImpl @Inject constructor(
    private val backendApiService: BackendApiService
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

    override suspend fun explainMove(symbol: String): NetworkResult<String> {
        return try {
            val response = backendApiService.explainMove(symbol)
            if (response.ok && response.explanation != null) {
                NetworkResult.Success(response.explanation)
            } else {
                NetworkResult.Error(IllegalStateException(response.error ?: "Unknown error"))
            }
        } catch (e: Exception) {
            NetworkResult.Error(e)
        }
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

