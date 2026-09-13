package com.invest.easymoney.domain.model

sealed interface NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>
    data class Error(val throwable: Throwable) : NetworkResult<Nothing>
}

