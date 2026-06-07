package com.invest.easymoney.domain.model

/** Lightweight model for search results */
data class StockSearchResult(
    val symbol: String,
    val name: String,
    val exchange: String
)

