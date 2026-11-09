package com.invest.easymoney.domain.portfolio

import java.math.BigDecimal
import java.math.RoundingMode

data class WeightedPosition(val position: PortfolioPosition, val weightPercent: BigDecimal)

/** Per-currency totals. Currencies are never added together (no FX conversion). */
data class CurrencyTotals(
    val currency: String,
    val positionCount: Int,
    val marketValue: BigDecimal,
    val bookValue: BigDecimal,
    val unrealizedReturn: BigDecimal,
    val cash: BigDecimal,
    /** Weights are among investment positions in this currency only; cash is excluded. */
    val securityTypeAllocation: Map<PortfolioSecurityType, BigDecimal>,
    val accountAllocation: Map<String, BigDecimal>,
    val largestPositions: List<WeightedPosition>
)

data class PortfolioSummary(
    val accountCount: Int,
    val positionCount: Int,
    val cashBalanceCount: Int,
    val byCurrency: List<CurrencyTotals>
)

object PortfolioSummaryCalculator {

    fun calculate(portfolio: Portfolio, largestCount: Int = 5): PortfolioSummary =
        calculate(portfolio.accounts, portfolio.positions, portfolio.cashBalances, largestCount)

    fun calculate(
        accounts: List<PortfolioAccount>,
        positions: List<PortfolioPosition>,
        cashBalances: List<PortfolioCashBalance>,
        largestCount: Int = 5
    ): PortfolioSummary {
        val priced = positions.filter { it.marketValue != null && it.marketValueCurrency != null }
        val currencies = (priced.map { it.marketValueCurrency!!.value } + cashBalances.map { it.currency.value })
            .distinct().sorted()

        val totals = currencies.map { cur ->
            val inCur = priced.filter { it.marketValueCurrency!!.value == cur }
            val mv = inCur.sumOf { it.marketValue!! }
            val book = positions.filter { (it.bookValueMarketCurrency ?: it.marketValueCurrency)?.value == cur }
                .mapNotNull { it.bookValueMarket }.fold(BigDecimal.ZERO, BigDecimal::add)
            val unrealized = positions.filter { it.unrealizedReturnCurrency?.value == cur }
                .mapNotNull { it.unrealizedReturn }.fold(BigDecimal.ZERO, BigDecimal::add)
            val cash = cashBalances.filter { it.currency.value == cur }
                .fold(BigDecimal.ZERO) { acc, c -> acc.add(c.amount) }
            fun pct(part: BigDecimal): BigDecimal =
                if (mv.signum() == 0) BigDecimal.ZERO else part.multiply(HUNDRED).divide(mv, 2, RoundingMode.HALF_UP)

            CurrencyTotals(
                currency = cur,
                positionCount = inCur.size,
                marketValue = mv,
                bookValue = book,
                unrealizedReturn = unrealized,
                cash = cash,
                securityTypeAllocation = inCur.groupBy { it.securityType }
                    .mapValues { (_, v) -> pct(v.sumOf { it.marketValue!! }) },
                accountAllocation = inCur.groupBy { it.accountId }
                    .mapValues { (_, v) -> pct(v.sumOf { it.marketValue!! }) },
                largestPositions = inCur.sortedByDescending { it.marketValue }
                    .take(largestCount).map { WeightedPosition(it, pct(it.marketValue!!)) }
            )
        }
        return PortfolioSummary(accounts.size, positions.size, cashBalances.size, totals)
    }

    private val HUNDRED = BigDecimal(100)
}

