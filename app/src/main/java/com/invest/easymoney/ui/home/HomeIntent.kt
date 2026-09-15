package com.invest.easymoney.ui.home

sealed interface HomeIntent {
    data class SearchChanged(
        val query: String
    ) : HomeIntent

    data object LoadStocks : HomeIntent
    data object LoadAiPicks : HomeIntent
    data object DismissAiPicks : HomeIntent

}