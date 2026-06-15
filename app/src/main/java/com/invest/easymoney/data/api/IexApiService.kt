package com.invest.easymoney.data.api

import com.invest.easymoney.data.api.dto.IexStockDto
import retrofit2.http.GET
import retrofit2.http.Query

interface IexApiService {

    @GET("stock/market/list/mostactive")
    suspend fun getMostActiveStocks(
        @Query("token") token: String
    ): List<IexStockDto>
}