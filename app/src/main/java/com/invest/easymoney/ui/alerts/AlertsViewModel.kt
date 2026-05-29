package com.invest.easymoney.ui.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.invest.easymoney.domain.model.Alert
import com.invest.easymoney.domain.repository.StockRepository
import com.invest.easymoney.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val repository: StockRepository
) : ViewModel() {

    val alertsState: StateFlow<Resource<List<Alert>>> = repository.getAlerts()
        .map { Resource.Success(it) as Resource<List<Alert>> }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Resource.Loading)

    fun deleteAlert(alertId: Int) {
        viewModelScope.launch {
            repository.deleteAlert(alertId)
        }
    }
}
