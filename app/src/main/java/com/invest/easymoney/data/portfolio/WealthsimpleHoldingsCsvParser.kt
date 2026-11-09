package com.invest.easymoney.data.portfolio

import com.invest.easymoney.domain.portfolio.Broker
import com.invest.easymoney.domain.portfolio.CurrencyCode
import com.invest.easymoney.domain.portfolio.PortfolioAccount
import com.invest.easymoney.domain.portfolio.PortfolioAccountType
import com.invest.easymoney.domain.portfolio.PortfolioCashBalance
import com.invest.easymoney.domain.portfolio.PortfolioFingerprint
import com.invest.easymoney.domain.portfolio.PortfolioImportException
import com.invest.easymoney.domain.portfolio.PortfolioImportResult
import com.invest.easymoney.domain.portfolio.PortfolioImportWarningCode
import com.invest.easymoney.domain.portfolio.PortfolioImportWarningCode.*
import com.invest.easymoney.domain.portfolio.PortfolioPosition
import com.invest.easymoney.domain.portfolio.PortfolioRejectedRow
import com.invest.easymoney.domain.portfolio.PortfolioSecurityType
import com.invest.easymoney.domain.portfolio.PositionDirection
import com.invest.easymoney.domain.portfolio.cashKey
import com.invest.easymoney.domain.portfolio.hashAccountNumber
import com.invest.easymoney.domain.portfolio.maskAccountNumber
import com.invest.easymoney.domain.portfolio.securityKey
import java.io.InputStream
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.util.UUID

interface PortfolioCsvParser {
    fun canParse(headers: List<String>): Boolean

    fun parse(
        input: InputStream,
        sourceFileName: String?,
        importedAt: Instant
    ): PortfolioImportResult
}

/** Minimal RFC-4180 reader: quoted fields, embedded commas/newlines, escaped quotes, CRLF/LF. */
internal object CsvReader {
    fun parse(text: String): List<List<String>> {
        val records = ArrayList<List<String>>()
        var row = ArrayList<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < text.length && text[i + 1] == '"') {
                        field.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    field.append(c)
                }
            } else {
                when (c) {
                    '"' -> inQuotes = true
                    ',' -> {
                        row.add(field.toString()); field.setLength(0)
                    }
                    '\r' -> Unit
                    '\n' -> {
                        row.add(field.toString()); field.setLength(0)
                        records.add(row); row = ArrayList()
                    }
                    else -> field.append(c)
                }
            }
            i++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) {
            row.add(field.toString())
            records.add(row)
        }
        return records
    }
}

/** Deterministic parser for the Wealthsimple "holdings report" CSV. No logging of any content. */
class WealthsimpleHoldingsCsvParser : PortfolioCsvParser {

    override fun canParse(headers: List<String>): Boolean {
        val normalized = headers.map(::normalizeHeader).toSet()
        return REQUIRED_HEADERS.all { it.lowercase() in normalized }
    }

    override fun parse(
        input: InputStream,
        sourceFileName: String?,
        importedAt: Instant
    ): PortfolioImportResult {
        val text = input.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val records = CsvReader.parse(text.removePrefix("\uFEFF"))
            .withIndex()
            .filter { (_, r) -> r.any { it.isNotBlank() } }
        val headerRecord = records.firstOrNull()?.value
            ?: throw PortfolioImportException("The selected file is empty.")
        if (!canParse(headerRecord)) {
            throw PortfolioImportException("This file is not a supported Wealthsimple holdings report.")
        }

        val warnings = mutableListOf<PortfolioImportWarningCode>()
        val rejected = mutableListOf<PortfolioRejectedRow>()
        val headerIndex = headerRecord.mapIndexed { i, h -> normalizeHeader(h) to i }.toMap()
        if (headerRecord.map(::normalizeHeader).any { it !in REQUIRED_HEADERS.map(String::lowercase) }) {
            warnings += EXTRA_COLUMNS_IGNORED
        }

        // Footer ("As of ...") is metadata, never a holding.
        var footer: String? = null
        val dataRecords = records.drop(1).filter { (_, r) ->
            val isFooter = r.first().trim().startsWith("As of", ignoreCase = true) &&
                r.drop(1).all { it.isBlank() }
            if (isFooter) footer = r.first().trim()
            !isFooter
        }
        val effectiveAt = parseSnapshotTimestamp(footer, warnings) ?: importedAt

        val snapshotId = UUID.randomUUID().toString()
        val accounts = LinkedHashMap<String, PortfolioAccount>()
        val positions = LinkedHashMap<String, PortfolioPosition>()
        val cash = LinkedHashMap<String, PortfolioCashBalance>()

        for ((index, record) in dataRecords) {
            val rowNumber = index + 1
            fun cell(name: String): String = record.getOrNull(headerIndex[name.lowercase()] ?: -1)?.trim().orEmpty()
            val symbolRaw = cell("Symbol")
            val name = cell("Name")
            fun reject(code: String, message: String) {
                rejected += PortfolioRejectedRow(
                    rowNumber, symbolRaw.take(32).ifBlank { null }, name.take(100).ifBlank { null }, code, message
                )
            }

            val accountName = cell("Account Name")
            val accountNumber = cell("Account Number") // short-lived; hashed immediately below
            val securityTypeRaw = cell("Security Type")
            if (accountName.isBlank() || accountNumber.isBlank() || securityTypeRaw.isBlank()) {
                reject("MISSING_FIELD", "A required field is missing."); continue
            }
            if (accountName.length > 200 || symbolRaw.length > 32 || name.length > 500) {
                reject("FIELD_TOO_LONG", "A field exceeds the supported length."); continue
            }

            val rowWarnings = mutableListOf<PortfolioImportWarningCode>()
            val securityType = parseSecurityType(securityTypeRaw)
            if (securityType == null) rowWarnings += UNKNOWN_SECURITY_TYPE
            val type = securityType ?: PortfolioSecurityType.OTHER
            val isCash = type == PortfolioSecurityType.CURRENCY

            fun currency(column: String): CurrencyCode? {
                val c = cell(column).uppercase().ifBlank { return null }
                if (c !in KNOWN_CURRENCIES) rowWarnings += UNKNOWN_CURRENCY
                return CurrencyCode(c)
            }

            val quantityParsed: BigDecimal?
            val marketPrice: BigDecimal?
            val bookCad: BigDecimal?
            val bookMarket: BigDecimal?
            val marketValue: BigDecimal?
            val unrealized: BigDecimal?
            try {
                quantityParsed = parseDecimal(cell("Quantity"), MAX_QUANTITY)
                marketPrice = parseDecimal(cell("Market Price"), MAX_MONEY)
                bookCad = parseDecimal(cell("Book Value (CAD)"), MAX_MONEY)
                bookMarket = parseDecimal(cell("Book Value (Market)"), MAX_MONEY)
                marketValue = parseDecimal(cell("Market Value"), MAX_MONEY)
                unrealized = parseDecimal(cell("Market Unrealized Returns"), MAX_MONEY)
            } catch (e: NumberFormatException) {
                reject("INVALID_NUMBER", "A numeric value is invalid or out of range."); continue
            }
            if (quantityParsed == null) {
                reject("MISSING_QUANTITY", "Quantity is missing."); continue
            }
            val quantity: BigDecimal = quantityParsed

            val directionRaw = cell("Position Direction")
            if (!isCash && (directionRaw.isBlank() || (symbolRaw.isBlank() && name.isBlank()))) {
                reject("MISSING_FIELD", "A required field is missing."); continue
            }
            val direction = when (directionRaw.uppercase()) {
                "LONG" -> PositionDirection.LONG
                "SHORT" -> PositionDirection.SHORT
                else -> PositionDirection.UNKNOWN
            }
            if (quantity.signum() < 0 && direction != PositionDirection.SHORT) {
                rowWarnings += DIRECTION_QUANTITY_MISMATCH
            }

            // Account (raw number never leaves this scope).
            val accountHash = hashAccountNumber(Broker.WEALTHSIMPLE, accountNumber)
            val account = accounts.getOrPut(accountHash) {
                val accountType = parseAccountType(cell("Account Type"))
                if (accountType == PortfolioAccountType.UNKNOWN) warnings += UNKNOWN_ACCOUNT_TYPE
                PortfolioAccount(
                    accountId = "$snapshotId-${accountHash.take(12)}",
                    snapshotId = snapshotId,
                    broker = Broker.WEALTHSIMPLE,
                    accountName = accountName,
                    accountType = accountType,
                    accountClassification = cell("Account Classification").ifBlank { null },
                    maskedAccountNumber = maskAccountNumber(accountNumber),
                    accountNumberHash = accountHash,
                    baseCurrency = null,
                    effectiveAt = effectiveAt
                )
            }

            val marketValueCurrency = currency("Market Value Currency")
            if (isCash) {
                val cur = symbolRaw.uppercase().ifBlank { null }?.let(::CurrencyCode) ?: marketValueCurrency
                if (cur == null) { reject("MISSING_FIELD", "Cash currency is missing."); continue }
                if (cur.value !in KNOWN_CURRENCIES) warnings += UNKNOWN_CURRENCY
                val key = "$accountHash|${cashKey(cur.value)}"
                val existing = cash[key]
                if (existing != null) {
                    if (existing.amount.compareTo(quantity) == 0) warnings += DUPLICATE_CASH_BALANCE
                    else reject("CONFLICTING_DUPLICATE", "Conflicting duplicate cash balance.")
                    continue
                }
                cash[key] = PortfolioCashBalance(
                    UUID.randomUUID().toString(), snapshotId, account.accountId, cur, quantity, effectiveAt
                )
                continue
            }

            if (marketPrice == null) rowWarnings += MISSING_MARKET_PRICE
            if (marketValue == null) rowWarnings += MISSING_MARKET_VALUE
            if (marketValue != null && marketValue.signum() == 0) rowWarnings += ZERO_MARKET_VALUE
            if (bookCad == null && bookMarket == null) rowWarnings += MISSING_BOOK_VALUE

            val position = PortfolioPosition(
                positionId = UUID.randomUUID().toString(),
                snapshotId = snapshotId,
                accountId = account.accountId,
                symbol = symbolRaw.ifBlank { null },
                exchange = cell("Exchange").ifBlank { null },
                mic = cell("MIC").ifBlank { null },
                securityName = name.ifBlank { symbolRaw },
                securityType = type,
                quantity = quantity,
                direction = direction,
                marketPrice = marketPrice,
                marketPriceCurrency = currency("Market Price Currency"),
                bookValueCad = bookCad,
                bookValueCadCurrency = currency("Book Value Currency (CAD)"),
                bookValueMarket = bookMarket,
                bookValueMarketCurrency = currency("Book Value Currency (Market)"),
                marketValue = marketValue,
                marketValueCurrency = marketValueCurrency,
                unrealizedReturn = unrealized,
                unrealizedReturnCurrency = currency("Market Unrealized Returns Currency"),
                effectiveAt = effectiveAt,
                warnings = rowWarnings.distinct()
            )
            val key = "$accountHash|" + securityKey(
                position.mic, position.exchange, position.symbol, marketValueCurrency?.value, position.securityName
            ) + "|${direction.name}|${marketValueCurrency?.value}"
            val existing = positions[key]
            if (existing != null) {
                if (existing.copy(positionId = "") == position.copy(positionId = "")) warnings += DUPLICATE_POSITION
                else reject("CONFLICTING_DUPLICATE", "Conflicting duplicate position; not merged.")
                continue
            }
            positions[key] = position
            warnings += position.warnings
        }

        if (accounts.isEmpty()) {
            throw PortfolioImportException("No valid holdings were found in the selected file.")
        }
        val currencies = positions.values.mapNotNull { it.marketValueCurrency?.value }.toSet()
        if (currencies.size > 1) warnings += MIXED_CURRENCIES
        if (Duration.between(effectiveAt, importedAt) > STALE_AFTER) warnings += STALE_SNAPSHOT
        warnings += SENSITIVE_ACCOUNT_DATA_REDACTED

        val accountList = accounts.values.toList()
        val positionList = positions.values.toList()
        val cashList = cash.values.toList()
        return PortfolioImportResult(
            snapshotId = snapshotId,
            broker = Broker.WEALTHSIMPLE,
            sourceFileName = sourceFileName,
            importedAt = importedAt,
            effectiveAt = effectiveAt,
            accounts = accountList,
            positions = positionList,
            cashBalances = cashList,
            warnings = warnings,
            rejectedRows = rejected,
            fingerprint = PortfolioFingerprint.compute(Broker.WEALTHSIMPLE, effectiveAt, accountList, positionList, cashList)
        )
    }

    companion object {
        val REQUIRED_HEADERS = listOf(
            "Account Name", "Account Type", "Account Classification", "Account Number", "Symbol",
            "Exchange", "MIC", "Name", "Security Type", "Quantity", "Position Direction",
            "Market Price", "Market Price Currency", "Book Value (CAD)", "Book Value Currency (CAD)",
            "Book Value (Market)", "Book Value Currency (Market)", "Market Value",
            "Market Value Currency", "Market Unrealized Returns", "Market Unrealized Returns Currency"
        )
        private val KNOWN_CURRENCIES = setOf("CAD", "USD", "EUR", "GBP", "CHF", "JPY", "AUD", "HKD")
        private val MAX_QUANTITY = BigDecimal("1000000000")
        private val MAX_MONEY = BigDecimal("1000000000000")
        private val STALE_AFTER: Duration = Duration.ofDays(7)
        private val NUMBER = Regex("^[+-]?(\\d+(\\.\\d*)?|\\.\\d+)$")
        private val AS_OF = Regex("^As of (\\d{4}-\\d{2}-\\d{2}) (\\d{2}:\\d{2}) GMT([+-]\\d{2}:\\d{2})$", RegexOption.IGNORE_CASE)

        private fun normalizeHeader(h: String) = h.removePrefix("\uFEFF").trim().lowercase()

        /** Null for blank; NumberFormatException for NaN, scientific notation or out-of-bounds values. */
        internal fun parseDecimal(raw: String, bound: BigDecimal): BigDecimal? {
            val v = raw.trim()
            if (v.isEmpty()) return null
            if (!NUMBER.matches(v)) throw NumberFormatException()
            val d = BigDecimal(v)
            if (d.abs() > bound) throw NumberFormatException()
            return d
        }

        internal fun parseSnapshotTimestamp(
            footer: String?,
            warnings: MutableList<PortfolioImportWarningCode>
        ): Instant? {
            if (footer == null) { warnings += MISSING_SNAPSHOT_TIMESTAMP; return null }
            val m = AS_OF.matchEntire(footer.trim())
            if (m == null) { warnings += INVALID_SNAPSHOT_TIMESTAMP; return null }
            return try {
                OffsetDateTime.parse("${m.groupValues[1]}T${m.groupValues[2]}${m.groupValues[3]}").toInstant()
            } catch (e: Exception) {
                warnings += INVALID_SNAPSHOT_TIMESTAMP; null
            }
        }

        private fun parseSecurityType(raw: String): PortfolioSecurityType? =
            when (raw.replace("\\", "").trim().uppercase().replace(' ', '_')) {
                "EQUITY" -> PortfolioSecurityType.EQUITY
                "EXCHANGE_TRADED_FUND" -> PortfolioSecurityType.EXCHANGE_TRADED_FUND
                "CURRENCY" -> PortfolioSecurityType.CURRENCY
                "MUTUAL_FUND" -> PortfolioSecurityType.MUTUAL_FUND
                "OPTION" -> PortfolioSecurityType.OPTION
                "FIXED_INCOME" -> PortfolioSecurityType.FIXED_INCOME
                "CRYPTOCURRENCY", "CRYPTO" -> PortfolioSecurityType.CRYPTO
                else -> null
            }

        private fun parseAccountType(raw: String): PortfolioAccountType {
            val n = raw.uppercase().filter { it.isLetter() }
            return when {
                "TFSA" in n -> PortfolioAccountType.TFSA
                "RRSP" in n -> PortfolioAccountType.RRSP
                "FHSA" in n -> PortfolioAccountType.FHSA
                "RESP" in n -> PortfolioAccountType.RESP
                "LIRA" in n -> PortfolioAccountType.LIRA
                "PERSONAL" in n || "NONREGISTERED" in n -> PortfolioAccountType.PERSONAL
                "CRYPTO" in n -> PortfolioAccountType.CRYPTO
                "CASH" in n -> PortfolioAccountType.CASH
                else -> PortfolioAccountType.UNKNOWN
            }
        }
    }
}


