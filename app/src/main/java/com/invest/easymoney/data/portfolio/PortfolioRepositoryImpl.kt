package com.invest.easymoney.data.portfolio

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.invest.easymoney.data.local.dao.PortfolioDao
import com.invest.easymoney.data.local.entity.PortfolioAccountEntity
import com.invest.easymoney.data.local.entity.PortfolioCashBalanceEntity
import com.invest.easymoney.data.local.entity.PortfolioPositionEntity
import com.invest.easymoney.data.local.entity.PortfolioSnapshotEntity
import com.invest.easymoney.di.DeviceId
import com.invest.easymoney.domain.model.NetworkResult
import com.invest.easymoney.domain.portfolio.Broker
import com.invest.easymoney.domain.portfolio.CurrencyCode
import com.invest.easymoney.domain.portfolio.DuplicateStatus
import com.invest.easymoney.domain.portfolio.Portfolio
import com.invest.easymoney.domain.portfolio.PortfolioAccount
import com.invest.easymoney.domain.portfolio.PortfolioAccountType
import com.invest.easymoney.domain.portfolio.PortfolioCashBalance
import com.invest.easymoney.domain.portfolio.PortfolioFingerprint
import com.invest.easymoney.domain.portfolio.PortfolioImportException
import com.invest.easymoney.domain.portfolio.PortfolioImportPreview
import com.invest.easymoney.domain.portfolio.PortfolioImportResult
import com.invest.easymoney.domain.portfolio.PortfolioImportSummary
import com.invest.easymoney.domain.portfolio.PortfolioImportWarningCode
import com.invest.easymoney.domain.portfolio.PortfolioPosition
import com.invest.easymoney.domain.portfolio.PortfolioReplacementDecision
import com.invest.easymoney.domain.portfolio.PortfolioRepository
import com.invest.easymoney.domain.portfolio.PortfolioSecurityType
import com.invest.easymoney.domain.portfolio.PortfolioSnapshot
import com.invest.easymoney.domain.portfolio.PositionDirection
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PortfolioRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: PortfolioDao,
    @DeviceId private val ownerId: String
) : PortfolioRepository {

    private val parser: PortfolioCsvParser = WealthsimpleHoldingsCsvParser()

    /** Previews issued by this repository; confirmImport only accepts these. Never persisted. */
    private val issued = object : LinkedHashMap<String, PortfolioImportPreview>() {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, PortfolioImportPreview>) = size > 3
    }

    override suspend fun previewWealthsimpleImport(uri: Uri): NetworkResult<PortfolioImportPreview> =
        withContext(Dispatchers.IO) {
            try {
                val bytes = readBounded(uri)
                val result = try {
                    parser.parse(ByteArrayInputStream(bytes), displayName(uri), Instant.now())
                } finally {
                    bytes.fill(0) // drop temporary raw content
                }
                val preview = result.toPreview(duplicateStatus(result))
                synchronized(issued) { issued[preview.previewToken] = preview }
                NetworkResult.Success(preview)
            } catch (e: CancellationException) {
                throw e
            } catch (e: PortfolioImportException) {
                NetworkResult.Error(e)
            } catch (e: Exception) {
                NetworkResult.Error(PortfolioImportException("Unable to read the selected portfolio file."))
            }
        }

    override suspend fun confirmImport(
        preview: PortfolioImportPreview,
        replacementDecision: PortfolioReplacementDecision
    ): NetworkResult<PortfolioSnapshot> = withContext(Dispatchers.IO) {
        try {
            val trusted = synchronized(issued) { issued[preview.previewToken] }
                ?: throw PortfolioImportException("This preview is no longer valid. Please select the file again.")
            val recomputed = PortfolioFingerprint.compute(
                trusted.broker, trusted.effectiveAt, trusted.accounts, trusted.positions, trusted.cashBalances
            )
            if (recomputed != trusted.fingerprint) {
                throw PortfolioImportException("The import preview was modified. Please select the file again.")
            }
            val status = duplicateStatus(trusted.toResult())
            when {
                status == DuplicateStatus.IDENTICAL_IMPORT ->
                    throw PortfolioImportException("This portfolio snapshot has already been imported.")
                replacementDecision == PortfolioReplacementDecision.CANCEL ->
                    throw PortfolioImportException("Import cancelled.")
                status == DuplicateStatus.OLDER_SNAPSHOT &&
                    replacementDecision != PortfolioReplacementDecision.IMPORT_AS_ACTIVE ->
                    throw PortfolioImportException("An older snapshot requires explicit confirmation.")
            }
            val snapshot = PortfolioSnapshot(
                trusted.snapshotId, trusted.broker, trusted.importedAt, trusted.effectiveAt, trusted.sourceFileName,
                trusted.accounts.size, trusted.positions.size, trusted.cashBalances.size, trusted.fingerprint
            )
            dao.replaceAndInsert(
                ownerId = ownerId,
                hashes = trusted.accounts.map { it.accountNumberHash },
                snapshot = snapshot.toEntity(ownerId),
                accounts = trusted.accounts.map { it.toEntity(ownerId) },
                positions = trusted.positions.map { it.toEntity(ownerId) },
                cash = trusted.cashBalances.map { it.toEntity(ownerId) }
            )
            synchronized(issued) { issued.remove(trusted.previewToken) }
            NetworkResult.Success(snapshot)
        } catch (e: CancellationException) {
            throw e
        } catch (e: PortfolioImportException) {
            NetworkResult.Error(e)
        } catch (e: Exception) {
            NetworkResult.Error(PortfolioImportException("Unable to save the portfolio. Your previous data is unchanged."))
        }
    }

    override fun observeActivePortfolio(): Flow<Portfolio?> = combine(
        dao.observeSnapshots(ownerId), dao.observeAccounts(ownerId),
        dao.observePositions(ownerId), dao.observeCash(ownerId)
    ) { snapshots, accounts, positions, cash ->
        if (accounts.isEmpty()) null
        else Portfolio(
            snapshots.map { it.toDomain() }, accounts.map { it.toDomain() },
            positions.map { it.toDomain() }, cash.map { it.toDomain() }
        )
    }

    override fun observePortfolioAccounts(): Flow<List<PortfolioAccount>> =
        dao.observeAccounts(ownerId).map { list -> list.map { it.toDomain() } }

    override fun observePortfolioPositions(): Flow<List<PortfolioPosition>> =
        dao.observePositions(ownerId).map { list -> list.map { it.toDomain() } }

    override suspend fun deletePortfolioData(): NetworkResult<Unit> = safeWrite { dao.deleteEverything(ownerId) }

    override suspend fun removeSnapshot(snapshotId: String): NetworkResult<Unit> =
        safeWrite { dao.removeSnapshot(ownerId, snapshotId) }

    private suspend fun safeWrite(block: suspend () -> Unit): NetworkResult<Unit> = try {
        block(); NetworkResult.Success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        NetworkResult.Error(PortfolioImportException("Unable to update portfolio data."))
    }

    private suspend fun duplicateStatus(result: PortfolioImportResult): DuplicateStatus {
        if (dao.countFingerprint(ownerId, result.fingerprint) > 0) return DuplicateStatus.IDENTICAL_IMPORT
        val existing = dao.accountsByHashes(ownerId, result.accounts.map { it.accountNumberHash })
        if (existing.isEmpty()) {
            return if (dao.countAccounts(ownerId) == 0) DuplicateStatus.FIRST_IMPORT else DuplicateStatus.DIFFERENT_ACCOUNT
        }
        return if (result.effectiveAt.toEpochMilli() < existing.maxOf { it.effectiveAtMs }) {
            DuplicateStatus.OLDER_SNAPSHOT
        } else {
            DuplicateStatus.NEWER_SNAPSHOT
        }
    }

    private fun readBounded(uri: Uri): ByteArray {
        val stream = context.contentResolver.openInputStream(uri)
            ?: throw PortfolioImportException("Unable to read the selected file.")
        stream.use { input ->
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(8 * 1024)
            var total = 0
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                total += n
                if (total > MAX_BYTES) throw PortfolioImportException("The selected portfolio file is too large.")
                out.write(buffer, 0, n)
            }
            buffer.fill(0)
            return out.toByteArray()
        }
    }

    private fun displayName(uri: Uri): String? = try {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    } catch (e: Exception) {
        null
    }

    private companion object {
        const val MAX_BYTES = 10 * 1024 * 1024
    }
}

private fun PortfolioImportResult.toPreview(status: DuplicateStatus) = PortfolioImportPreview(
    previewToken = UUID.randomUUID().toString(),
    snapshotId = snapshotId, broker = broker, sourceFileName = sourceFileName,
    importedAt = importedAt, effectiveAt = effectiveAt, accounts = accounts, positions = positions,
    cashBalances = cashBalances, warnings = warnings, rejectedRows = rejectedRows,
    fingerprint = fingerprint, duplicateStatus = status,
    summary = PortfolioImportSummary(
        accountCount = accounts.size,
        positionCount = positions.size,
        cashBalanceCount = cashBalances.size,
        rejectedRowCount = rejectedRows.size,
        currencies = (positions.mapNotNull { it.marketValueCurrency?.value } + cashBalances.map { it.currency.value })
            .distinct().sorted(),
        securityTypes = positions.map { it.securityType }.distinct()
    )
)

private fun PortfolioImportPreview.toResult() = PortfolioImportResult(
    snapshotId, broker, sourceFileName, importedAt, effectiveAt, accounts, positions, cashBalances,
    warnings, rejectedRows, fingerprint
)

private fun String?.dec(): BigDecimal? = this?.let(::BigDecimal)
private fun String?.cur(): CurrencyCode? = this?.let(::CurrencyCode)
private fun BigDecimal?.str(): String? = this?.toPlainString()

private fun PortfolioSnapshot.toEntity(owner: String) = PortfolioSnapshotEntity(
    snapshotId, owner, broker.name, importedAt.toEpochMilli(), effectiveAt.toEpochMilli(), sourceFileName,
    accountCount, positionCount, cashBalanceCount, fingerprint
)

private fun PortfolioSnapshotEntity.toDomain() = PortfolioSnapshot(
    snapshotId, Broker.valueOf(broker), Instant.ofEpochMilli(importedAtMs), Instant.ofEpochMilli(effectiveAtMs),
    sourceFileName, accountCount, positionCount, cashBalanceCount, fingerprint
)

private fun PortfolioAccount.toEntity(owner: String) = PortfolioAccountEntity(
    accountId, owner, snapshotId, broker.name, accountName, accountType.name, accountClassification,
    maskedAccountNumber, accountNumberHash, baseCurrency?.value, effectiveAt.toEpochMilli()
)

private fun PortfolioAccountEntity.toDomain() = PortfolioAccount(
    accountId, snapshotId, Broker.valueOf(broker), accountName, PortfolioAccountType.valueOf(accountType),
    accountClassification, maskedAccountNumber, accountNumberHash, baseCurrency.cur(),
    Instant.ofEpochMilli(effectiveAtMs)
)

private fun PortfolioPosition.toEntity(owner: String) = PortfolioPositionEntity(
    positionId, owner, snapshotId, accountId, symbol, exchange, mic, securityName, securityType.name,
    quantity.toPlainString(), direction.name, marketPrice.str(), marketPriceCurrency?.value,
    bookValueCad.str(), bookValueCadCurrency?.value, bookValueMarket.str(), bookValueMarketCurrency?.value,
    marketValue.str(), marketValueCurrency?.value, unrealizedReturn.str(), unrealizedReturnCurrency?.value,
    effectiveAt.toEpochMilli(), warnings.joinToString(",") { it.name }
)

private fun PortfolioPositionEntity.toDomain() = PortfolioPosition(
    positionId, snapshotId, accountId, symbol, exchange, mic, securityName,
    PortfolioSecurityType.valueOf(securityType), BigDecimal(quantity), PositionDirection.valueOf(direction),
    marketPrice.dec(), marketPriceCurrency.cur(), bookValueCad.dec(), bookValueCadCurrency.cur(),
    bookValueMarket.dec(), bookValueMarketCurrency.cur(), marketValue.dec(), marketValueCurrency.cur(),
    unrealizedReturn.dec(), unrealizedReturnCurrency.cur(), Instant.ofEpochMilli(effectiveAtMs),
    warnings.split(",").filter { it.isNotBlank() }.map { PortfolioImportWarningCode.valueOf(it) }
)

private fun PortfolioCashBalance.toEntity(owner: String) = PortfolioCashBalanceEntity(
    cashBalanceId, owner, snapshotId, accountId, currency.value, amount.toPlainString(), effectiveAt.toEpochMilli()
)

private fun PortfolioCashBalanceEntity.toDomain() = PortfolioCashBalance(
    cashBalanceId, snapshotId, accountId, CurrencyCode(currency), BigDecimal(amount),
    Instant.ofEpochMilli(effectiveAtMs)
)

