package com.invest.easymoney.data.api

import retrofit2.http.GET
import retrofit2.http.Query

interface ArticleApiService {
    @GET("articles/search")
    suspend fun searchArticles(@Query("q") query: String): ArticleSearchResponse
}

data class ArticleSearchResponse(
    val ok: Boolean = false,
    val articles: List<ArticleDto?>? = null
)

data class ArticleDto(
    val id: String? = null,
    val headline: String? = null,
    val source: String? = null,
    val url: String? = null,
    val datetime: Long? = null
)
