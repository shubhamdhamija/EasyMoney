package com.invest.easymoney.domain.model

data class Article(
    val id: String,
    val headline: String,
    val source: String,
    val url: String,
    val datetime: Long
)
