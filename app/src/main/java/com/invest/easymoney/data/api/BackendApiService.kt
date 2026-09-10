package com.invest.easymoney.data.api

import com.invest.easymoney.data.api.dto.EarningsReportDto
import com.invest.easymoney.data.api.dto.UpcomingEarningDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface BackendApiService {
    /**
     * Notify the backend about a watchlist change
     */
    @POST("community/watchlist")
    suspend fun notifyWatchlistChange(@Body payload: WatchlistChangePayload)

    /**
     * Get explanation for why a stock moved
     */
    @GET("ai/explain/{symbol}")
    suspend fun explainMove(@Path("symbol") symbol: String): ExplainMoveResponse

    /**
     * Get earnings report for a symbol
     */
    @GET("ai/earnings/{symbol}")
    suspend fun getEarningsReport(@Path("symbol") symbol: String): EarningsResponse

    /**
     * Get upcoming earnings
     */
    @GET("ai/earnings/upcoming")
    suspend fun getUpcomingEarnings(
        @Query("from") from: String? = null,
        @Query("to") to: String? = null
    ): UpcomingEarningsResponse
}

data class WatchlistChangePayload(
    val userId: String,
    val symbol: String,
    val action: String
)

data class ExplainMoveResponse(
    val ok: Boolean,
    val explanation: String? = null,
    val error: String? = null
)

data class EarningsResponse(
    val ok: Boolean,
    val earnings: EarningsReportDto? = null,
    val error: String? = null
)

data class UpcomingEarningsResponse(
    val ok: Boolean,
    val earnings: List<UpcomingEarningDto>? = null,
    val error: String? = null
)

