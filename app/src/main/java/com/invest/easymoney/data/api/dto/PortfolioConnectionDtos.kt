package com.invest.easymoney.data.api.dto

/** Wire DTOs for the read-only portfolio connection API. No order/trade models exist. */
data class CreateConnectionSessionRequestDto(val redirectUri: String)

data class ConnectionSessionResponseDto(
    val connectionSessionId: String?,
    val portalUrl: String?,
    val expiresAt: String?,
    val requestId: String?
)

data class ConnectionDto(
    val connectionId: String?,
    val provider: String?,
    val institutionName: String?,
    val status: String?,
    val connectionType: String?,
    val createdAt: String?,
    val lastSuccessfulSyncAt: String?,
    val lastSyncErrorCode: String?,
    val accountCount: Int?,
    val positionCount: Int?,
    val cashBalanceCount: Int?
)

data class ConnectionsResponseDto(
    val connections: List<ConnectionDto>?,
    val requestId: String?
)

data class SyncedAccountDto(
    val providerAccountId: String?,
    val displayName: String?,
    val accountType: String?,
    val providerCategory: String?,
    val institutionName: String?,
    val maskedAccountNumber: String?,
    val currency: String?,
    val status: String?
)

data class SyncedPositionDto(
    val providerPositionId: String?,
    val accountId: String?,
    val providerSecurityId: String?,
    val symbol: String?,
    val securityName: String?,
    val exchange: String?,
    val mic: String?,
    val quantity: String?,
    val marketPrice: String?,
    val marketValue: String?,
    val currency: String?,
    val averageCost: String?,
    val securityType: String?
)

data class SyncedCashBalanceDto(
    val accountId: String?,
    val currency: String?,
    val amount: String?
)

data class SyncedPortfolioDto(
    val snapshotId: String?,
    val effectiveAt: String?,
    val accounts: List<SyncedAccountDto>?,
    val positions: List<SyncedPositionDto>?,
    val cashBalances: List<SyncedCashBalanceDto>?,
    val warnings: List<String>?
)

data class ConnectedPortfolioResponseDto(
    val connection: ConnectionDto?,
    val portfolio: SyncedPortfolioDto?,
    val requestId: String?
)
