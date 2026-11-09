package com.invest.easymoney.domain.portfolio

import java.math.BigDecimal
import java.security.MessageDigest
import java.time.Instant
import java.util.TreeMap

/** Account-number privacy helpers. Never log inputs or outputs of these functions. */
private const val PRIVACY_SALT = "easymoney.portfolio.v1"

fun hashAccountNumber(broker: Broker, rawAccountNumber: String): String =
    sha256Hex("$PRIVACY_SALT|${broker.name}|${rawAccountNumber.trim()}")

/** "••••1234"; fewer than four characters yields bullets only. */
fun maskAccountNumber(rawAccountNumber: String): String {
    val value = rawAccountNumber.trim()
    return if (value.length < 4) "••••" else "••••" + value.takeLast(4)
}

internal fun sha256Hex(input: String): String =
    MessageDigest.getInstance("SHA-256")
        .digest(input.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

/**
 * Deterministic instrument identity. Ticker alone is never used:
 * XNAS|AAPL|USD, XTSE|WCN.TO|CAD, CASH|CAD.
 */
fun securityKey(
    mic: String?,
    exchange: String?,
    symbol: String?,
    currency: String?,
    name: String
): String {
    val venue = mic?.trim()?.uppercase()?.takeIf { it.isNotEmpty() }
        ?: exchange?.trim()?.uppercase()?.takeIf { it.isNotEmpty() }
    val id = symbol?.trim()?.uppercase()?.takeIf { it.isNotEmpty() }
        ?: name.trim().uppercase().replace(Regex("\\s+"), " ")
    return listOfNotNull(venue, id, currency?.trim()?.uppercase()?.takeIf { it.isNotEmpty() })
        .joinToString("|")
}

fun cashKey(currency: String): String = "CASH|${currency.trim().uppercase()}"

/** Canonical JSON (sorted keys, compact, plain decimals) -> SHA-256. No raw account numbers. */
object PortfolioFingerprint {

    fun compute(
        broker: Broker,
        effectiveAt: Instant,
        accounts: List<PortfolioAccount>,
        positions: List<PortfolioPosition>,
        cashBalances: List<PortfolioCashBalance>
    ): String {
        val hashByAccountId = accounts.associate { it.accountId to it.accountNumberHash }
        val positionJson = positions.map { p ->
            obj(
                "account" to str(hashByAccountId[p.accountId].orEmpty()),
                "bookValueCad" to dec(p.bookValueCad),
                "bookValueCadCurrency" to str(p.bookValueCadCurrency?.value),
                "bookValueMarket" to dec(p.bookValueMarket),
                "bookValueMarketCurrency" to str(p.bookValueMarketCurrency?.value),
                "direction" to str(p.direction.name),
                "exchange" to str(p.exchange),
                "marketPrice" to dec(p.marketPrice),
                "marketPriceCurrency" to str(p.marketPriceCurrency?.value),
                "marketValue" to dec(p.marketValue),
                "marketValueCurrency" to str(p.marketValueCurrency?.value),
                "mic" to str(p.mic),
                "name" to str(p.securityName),
                "quantity" to dec(p.quantity),
                "securityType" to str(p.securityType.name),
                "symbol" to str(p.symbol),
                "unrealizedReturn" to dec(p.unrealizedReturn),
                "unrealizedReturnCurrency" to str(p.unrealizedReturnCurrency?.value)
            )
        }.sorted()
        val cashJson = cashBalances.map { c ->
            obj(
                "account" to str(hashByAccountId[c.accountId].orEmpty()),
                "amount" to dec(c.amount),
                "currency" to str(c.currency.value)
            )
        }.sorted()
        val json = obj(
            "accounts" to accounts.map { str(it.accountNumberHash) }.sorted().joinToString(",", "[", "]"),
            "broker" to str(broker.name),
            "cash" to cashJson.joinToString(",", "[", "]"),
            "effectiveAt" to str(effectiveAt.toString()),
            "positions" to positionJson.joinToString(",", "[", "]")
        )
        return sha256Hex(json)
    }

    private fun obj(vararg entries: Pair<String, String>): String {
        val sorted = TreeMap<String, String>()
        entries.forEach { sorted[it.first] = it.second }
        return sorted.entries.joinToString(",", "{", "}") { "${str(it.key)}:${it.value}" }
    }

    private fun str(value: String?): String {
        if (value == null) return "null"
        val sb = StringBuilder("\"")
        value.forEach { ch ->
            when {
                ch == '\\' -> sb.append("\\\\")
                ch == '"' -> sb.append("\\\"")
                ch.code < 0x20 -> sb.append("\\u%04x".format(ch.code))
                else -> sb.append(ch)
            }
        }
        return sb.append('"').toString()
    }

    private fun dec(value: BigDecimal?): String =
        if (value == null) "null" else str(value.stripTrailingZeros().toPlainString())
}

