package com.invest.easymoney.domain.model

data class News(
    val id: Long,
    val headline: String,
    val source: String,
    val url: String,
    val summary: String,
    val imageUrl: String,
    val datetime: Long
)
