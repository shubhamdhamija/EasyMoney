package com.invest.easymoney.ui.articles

import com.invest.easymoney.domain.model.Article

enum class ArticleDateFilter(val label: String) {
    ANY_TIME("Any time"),
    PAST_WEEK("Past week")
}

data class ArticleSearchUiState(
    val query: String = "",
    val dateFilter: ArticleDateFilter = ArticleDateFilter.ANY_TIME,
    val articles: List<Article> = emptyList(),
    val isLoading: Boolean = false,
    val hasSearched: Boolean = false,
    val error: String? = null
)

sealed interface ArticleSearchIntent {
    data class QueryChanged(val query: String) : ArticleSearchIntent
    data class FilterChanged(val filter: ArticleDateFilter) : ArticleSearchIntent
    data object Submit : ArticleSearchIntent
    data object Retry : ArticleSearchIntent
}
