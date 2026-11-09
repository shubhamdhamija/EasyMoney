package com.invest.easymoney.data.api

import com.invest.easymoney.data.api.dto.EarningsReportDto
import com.invest.easymoney.data.api.dto.StockExplanationApiResponse
import com.invest.easymoney.data.api.dto.StockExplanationRequest
import com.invest.easymoney.data.api.dto.StockInsightApiResponse
import com.invest.easymoney.data.api.dto.StockInsightRequest
import com.invest.easymoney.data.api.dto.StockPicksApiResponse
import com.invest.easymoney.data.api.dto.StockPicksRequest
import com.invest.easymoney.data.api.dto.AgentResearchRequestDto
import com.invest.easymoney.data.api.dto.ToolApprovalDecisionRequestDto
import com.invest.easymoney.data.api.dto.UpcomingEarningDto
import okhttp3.ResponseBody
import retrofit2.Response
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
    @POST("ai/explain")
    suspend fun explainStockMovement(@Body request: StockExplanationRequest): StockExplanationApiResponse

    /**
     * Structured stock insight (replaces the legacy direct-provider call)
     */
    @POST("ai/stock-insight")
    suspend fun getStockInsight(@Body request: StockInsightRequest): StockInsightApiResponse

    /**
     * Notable stocks (market highlights) from a supplied stock set
     */
    @POST("ai/picks")
    suspend fun getAiPicks(@Body request: StockPicksRequest): StockPicksApiResponse

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

    @POST("agent/research")
    suspend fun startAgentResearch(
        @Body request: AgentResearchRequestDto
    ): Response<ResponseBody>

    @POST("agent/research/{threadId}/approval")
    suspend fun submitAgentApproval(
        @Path("threadId") threadId: String,
        @Body request: ToolApprovalDecisionRequestDto
    ): Response<ResponseBody>

    @GET("congress/filings")
    suspend fun getCongressFilings(
        @Query("member") member: String? = null,
        @Query("year") year: Int? = null,
        @Query("limit") limit: Int = 30
    ): com.invest.easymoney.data.api.dto.CongressFilingsResponseDto

    @GET("congress/filings/{year}/{docId}")
    suspend fun getCongressFilingDetail(
        @Path("year") year: Int,
        @Path("docId") docId: String,
        @Query("filed") filed: String? = null
    ): com.invest.easymoney.data.api.dto.CongressFilingDetailDto
}

data class WatchlistChangePayload(
    val userId: String,
    val symbol: String,
    val action: String
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

