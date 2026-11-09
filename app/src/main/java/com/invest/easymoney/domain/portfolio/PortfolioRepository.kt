package com.invest.easymoney.domain.portfolio

import android.net.Uri
import com.invest.easymoney.domain.model.NetworkResult
import kotlinx.coroutines.flow.Flow

/** Read-only portfolio access. There is intentionally no trading capability. */
interface PortfolioRepository {
    suspend fun previewWealthsimpleImport(uri: Uri): NetworkResult<PortfolioImportPreview>

    suspend fun confirmImport(
        preview: PortfolioImportPreview,
        replacementDecision: PortfolioReplacementDecision
    ): NetworkResult<PortfolioSnapshot>

    fun observeActivePortfolio(): Flow<Portfolio?>
    fun observePortfolioAccounts(): Flow<List<PortfolioAccount>>
    fun observePortfolioPositions(): Flow<List<PortfolioPosition>>

    suspend fun deletePortfolioData(): NetworkResult<Unit>
    suspend fun removeSnapshot(snapshotId: String): NetworkResult<Unit>
}

