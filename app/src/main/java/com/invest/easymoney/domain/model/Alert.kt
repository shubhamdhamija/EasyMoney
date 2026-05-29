package com.invest.easymoney.domain.model

data class Alert(
    val id: Int = 0,
    val symbol: String,
    val percentage: Float,
    val type: AlertType
)

enum class AlertType {
    INCREASE, DECREASE
}
