package com.invest.easymoney.ui.congress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.invest.easymoney.domain.model.CongressFiling
import com.invest.easymoney.domain.model.CongressFilingDetail
import com.invest.easymoney.domain.repository.CongressRepository
import com.invest.easymoney.domain.repository.StockRepository
import com.invest.easymoney.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CongressUiState(
    val member: String = DEFAULT_MEMBER,
    val filings: Resource<List<CongressFiling>> = Resource.Loading,
    /** Detail per filing doc id: Loading while fetching, Success/Error afterwards. */
    val details: Map<String, Resource<CongressFilingDetail>> = emptyMap(),
    val expandedDocId: String? = null,
    val watchlist: Set<String> = emptySet(),
) {
    companion object {
        const val DEFAULT_MEMBER = "Pelosi"
    }
}

@HiltViewModel
class CongressViewModel @Inject constructor(
    private val repository: CongressRepository,
    private val stockRepository: StockRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(CongressUiState())
    val state: StateFlow<CongressUiState> = _state

    private var loadJob: Job? = null

    init {
        search()
        viewModelScope.launch {
            stockRepository.getWatchlistSymbols().collect { symbols ->
                _state.update { it.copy(watchlist = symbols.map(String::uppercase).toSet()) }
            }
        }
    }

    /** Empty member = every member's filings. */
    fun showAllMembers() {
        _state.update { it.copy(member = "") }
        search()
    }

    fun onMemberChange(value: String) {
        _state.update { it.copy(member = value.take(60)) }
    }

    fun search() {
        loadJob?.cancel()
        val member = _state.value.member
        loadJob = viewModelScope.launch {
            _state.update { it.copy(filings = Resource.Loading, expandedDocId = null, details = emptyMap()) }
            val result = repository.getFilings(member)
            _state.update { it.copy(filings = result) }
        }
    }

    fun toggle(filing: CongressFiling) {
        val current = _state.value
        if (current.expandedDocId == filing.docId) {
            _state.update { it.copy(expandedDocId = null) }
            return
        }
        _state.update { it.copy(expandedDocId = filing.docId) }
        val existing = current.details[filing.docId]
        if (existing is Resource.Success || existing is Resource.Loading) return
        loadDetail(filing)
    }

    fun retryDetail(filing: CongressFiling) = loadDetail(filing)

    private fun loadDetail(filing: CongressFiling) {
        viewModelScope.launch {
            _state.update { it.copy(details = it.details + (filing.docId to Resource.Loading)) }
            val result = repository.getFilingDetail(filing)
            _state.update { it.copy(details = it.details + (filing.docId to result)) }
        }
    }
}

