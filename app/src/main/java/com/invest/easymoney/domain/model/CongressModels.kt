package com.invest.easymoney.domain.model

data class CongressFiling(
    val docId: String,
    val year: Int,
    val name: String,
    val district: String,
    val filed: String?,
    val pdfUrl: String?,
)

data class CongressTrade(
    val owner: String,
    val isPurchase: Boolean,
    val typeLabel: String,
    val asset: String,
    val ticker: String?,
    val amount: String,
    val tradeDate: String?,
    val daysToDisclose: Int?,
    val late: Boolean,
    val description: String?,
    /** e.g. "100 call options, $100 strike, expires 2027-06-17"; null for plain share trades. */
    val optionSummary: String? = null,
    val shares: Int? = null,
)

data class CongressFilingDetail(
    val available: Boolean,
    val pdfUrl: String?,
    val trades: List<CongressTrade>,
)

