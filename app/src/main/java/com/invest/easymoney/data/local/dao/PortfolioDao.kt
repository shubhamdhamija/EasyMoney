package com.invest.easymoney.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.invest.easymoney.data.local.entity.PortfolioAccountEntity
import com.invest.easymoney.data.local.entity.PortfolioCashBalanceEntity
import com.invest.easymoney.data.local.entity.PortfolioPositionEntity
import com.invest.easymoney.data.local.entity.PortfolioSnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class PortfolioDao {

    @Query("SELECT * FROM portfolio_snapshots WHERE ownerId = :ownerId ORDER BY effectiveAtMs DESC")
    abstract fun observeSnapshots(ownerId: String): Flow<List<PortfolioSnapshotEntity>>

    @Query("SELECT * FROM portfolio_accounts WHERE ownerId = :ownerId ORDER BY accountName")
    abstract fun observeAccounts(ownerId: String): Flow<List<PortfolioAccountEntity>>

    @Query("SELECT * FROM portfolio_positions WHERE ownerId = :ownerId")
    abstract fun observePositions(ownerId: String): Flow<List<PortfolioPositionEntity>>

    @Query("SELECT * FROM portfolio_cash_balances WHERE ownerId = :ownerId")
    abstract fun observeCash(ownerId: String): Flow<List<PortfolioCashBalanceEntity>>

    @Query("SELECT COUNT(*) FROM portfolio_snapshots WHERE ownerId = :ownerId AND fingerprint = :fingerprint")
    abstract suspend fun countFingerprint(ownerId: String, fingerprint: String): Int

    @Query("SELECT COUNT(*) FROM portfolio_accounts WHERE ownerId = :ownerId")
    abstract suspend fun countAccounts(ownerId: String): Int

    @Query("SELECT * FROM portfolio_accounts WHERE ownerId = :ownerId AND accountNumberHash IN (:hashes)")
    abstract suspend fun accountsByHashes(ownerId: String, hashes: List<String>): List<PortfolioAccountEntity>

    @Insert abstract suspend fun insertSnapshot(entity: PortfolioSnapshotEntity)
    @Insert abstract suspend fun insertAccounts(entities: List<PortfolioAccountEntity>)
    @Insert abstract suspend fun insertPositions(entities: List<PortfolioPositionEntity>)
    @Insert abstract suspend fun insertCash(entities: List<PortfolioCashBalanceEntity>)

    @Query("DELETE FROM portfolio_positions WHERE accountId IN (:accountIds)")
    abstract suspend fun deletePositionsByAccounts(accountIds: List<String>)

    @Query("DELETE FROM portfolio_cash_balances WHERE accountId IN (:accountIds)")
    abstract suspend fun deleteCashByAccounts(accountIds: List<String>)

    @Query("DELETE FROM portfolio_accounts WHERE accountId IN (:accountIds)")
    abstract suspend fun deleteAccounts(accountIds: List<String>)

    @Query(
        "DELETE FROM portfolio_snapshots WHERE ownerId = :ownerId " +
            "AND snapshotId NOT IN (SELECT snapshotId FROM portfolio_accounts)"
    )
    abstract suspend fun deleteEmptySnapshots(ownerId: String)

    @Query("DELETE FROM portfolio_positions WHERE ownerId = :ownerId")
    abstract suspend fun deleteAllPositions(ownerId: String)

    @Query("DELETE FROM portfolio_cash_balances WHERE ownerId = :ownerId")
    abstract suspend fun deleteAllCash(ownerId: String)

    @Query("DELETE FROM portfolio_accounts WHERE ownerId = :ownerId")
    abstract suspend fun deleteAllAccounts(ownerId: String)

    @Query("DELETE FROM portfolio_snapshots WHERE ownerId = :ownerId")
    abstract suspend fun deleteAllSnapshots(ownerId: String)

    @Query("SELECT accountId FROM portfolio_accounts WHERE ownerId = :ownerId AND snapshotId = :snapshotId")
    abstract suspend fun accountIdsForSnapshot(ownerId: String, snapshotId: String): List<String>

    /** Atomically replaces every stored account that shares an account hash with the new snapshot. */
    @Transaction
    open suspend fun replaceAndInsert(
        ownerId: String,
        hashes: List<String>,
        snapshot: PortfolioSnapshotEntity,
        accounts: List<PortfolioAccountEntity>,
        positions: List<PortfolioPositionEntity>,
        cash: List<PortfolioCashBalanceEntity>
    ) {
        val oldIds = accountsByHashes(ownerId, hashes).map { it.accountId }
        removeAccounts(oldIds)
        deleteEmptySnapshots(ownerId)
        insertSnapshot(snapshot)
        insertAccounts(accounts)
        insertPositions(positions)
        insertCash(cash)
    }

    @Transaction
    open suspend fun removeSnapshot(ownerId: String, snapshotId: String) {
        removeAccounts(accountIdsForSnapshot(ownerId, snapshotId))
        deleteEmptySnapshots(ownerId)
    }

    @Transaction
    open suspend fun deleteEverything(ownerId: String) {
        deleteAllPositions(ownerId)
        deleteAllCash(ownerId)
        deleteAllAccounts(ownerId)
        deleteAllSnapshots(ownerId)
    }

    private suspend fun removeAccounts(accountIds: List<String>) {
        if (accountIds.isEmpty()) return
        deletePositionsByAccounts(accountIds)
        deleteCashByAccounts(accountIds)
        deleteAccounts(accountIds)
    }
}

