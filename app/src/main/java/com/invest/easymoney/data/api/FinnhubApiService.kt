package com.invest.easymoney.data.api

import com.invest.easymoney.data.api.dto.YahooChartResponseDto
import com.invest.easymoney.data.api.dto.YahooSearchResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface YahooFinanceApiService {

    /**
     * Fetch a quote via the CHART endpoint — works WITHOUT a crumb or cookie.
     * The /v8/finance/quote endpoint is now blocked by Yahoo for non-browser
     * clients (returns the "sad panda" HTML page), so we use chart instead.
     * One symbol per request.
     */
    @GET("v8/finance/chart/{symbol}")
    suspend fun getChart(
        @Path("symbol") symbol: String,
        @Query("range") range: String = "1d",
        @Query("interval") interval: String = "1d"
    ): YahooChartResponseDto

    /**
     * Fetch latest news for a symbol via the search endpoint.
     * quotesCount=0 skips quote results so we only get news.
     */
    @GET("v1/finance/search")
    suspend fun searchNews(
        @Query("q") symbol: String,
        @Query("quotesCount") quotesCount: Int = 0,
        @Query("newsCount") newsCount: Int = 10,
        @Query("enableFuzzyQuery") enableFuzzyQuery: Boolean = false
    ): YahooSearchResponseDto
}
