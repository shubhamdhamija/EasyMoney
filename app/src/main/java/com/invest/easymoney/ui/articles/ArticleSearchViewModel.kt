package com.invest.easymoney.ui.articles

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.invest.easymoney.domain.model.Article
import com.invest.easymoney.domain.model.NetworkResult
import com.invest.easymoney.domain.repository.ArticleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ArticleSearchViewModel @Inject constructor(
    private val repository: ArticleRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        ArticleSearchUiState(
            query = savedStateHandle["articleQuery"] ?: "",
            dateFilter = ArticleDateFilter.entries.firstOrNull {
                it.name == savedStateHandle.get<String>("articleDateFilter")
            } ?: ArticleDateFilter.ANY_TIME
        )
    )
    val uiState = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var allArticles: List<Article> = emptyList()

    init {
        if (_uiState.value.query.isNotBlank()) search(debounce = false)
    }

    fun onIntent(intent: ArticleSearchIntent) {
        when (intent) {
            is ArticleSearchIntent.QueryChanged -> {
                savedStateHandle["articleQuery"] = intent.query
                _uiState.update { it.copy(query = intent.query) }
                search(debounce = true)
            }
            is ArticleSearchIntent.FilterChanged -> {
                savedStateHandle["articleDateFilter"] = intent.filter.name
                _uiState.update {
                    it.copy(dateFilter = intent.filter, articles = filterArticles(allArticles, intent.filter))
                }
            }
            ArticleSearchIntent.Submit, ArticleSearchIntent.Retry -> search(debounce = false)
        }
    }

    private fun search(debounce: Boolean) {
        searchJob?.cancel()
        val query = _uiState.value.query.trim().replace(Regex("\\s+"), " ")
        allArticles = emptyList()
        val valid = query.length in 2..200
        _uiState.update {
            it.copy(
                articles = emptyList(),
                hasSearched = false,
                isLoading = valid,
                error = if (query.length > 200) "Use 200 characters or fewer." else null
            )
        }
        if (!valid) return

        searchJob = viewModelScope.launch {
            if (debounce) delay(350)
            val result = repository.searchArticles(query)
            ensureActive()
            when (result) {
                is NetworkResult.Success -> {
                    allArticles = result.data
                    _uiState.update {
                        it.copy(
                            articles = filterArticles(allArticles, it.dateFilter),
                            isLoading = false,
                            hasSearched = true
                        )
                    }
                }
                is NetworkResult.Error -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        hasSearched = true,
                        error = result.throwable.message ?: "Unable to search articles."
                    )
                }
            }
        }
    }

    private fun filterArticles(articles: List<Article>, filter: ArticleDateFilter): List<Article> {
        if (filter == ArticleDateFilter.ANY_TIME) return articles
        val now = System.currentTimeMillis() / 1000
        return articles.filter { it.datetime in (now - 7 * 24 * 60 * 60)..now }
    }
}
