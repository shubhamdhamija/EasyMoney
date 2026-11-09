package com.invest.easymoney.domain.repository

import com.invest.easymoney.domain.model.Article
import com.invest.easymoney.domain.model.NetworkResult

interface ArticleRepository {
    suspend fun searchArticles(query: String): NetworkResult<List<Article>>
}
