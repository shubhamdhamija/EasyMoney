package com.invest.easymoney.data.repository

import com.invest.easymoney.data.api.BackendApiService
import com.invest.easymoney.data.api.dto.CongressFilingDetailDto
import com.invest.easymoney.data.api.dto.CongressFilingDto
import com.invest.easymoney.data.api.dto.CongressTradeDto
import com.invest.easymoney.domain.model.CongressFiling
import com.invest.easymoney.domain.model.CongressFilingDetail
import com.invest.easymoney.domain.model.CongressTrade
import com.invest.easymoney.domain.repository.CongressRepository
import com.invest.easymoney.util.Resource
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

class CongressRepositoryImpl @Inject constructor(
    private val api: BackendApiService,
) : CongressRepository {

    override suspend fun getFilings(member: String?): Resource<List<CongressFiling>> = try {
        val response = api.getCongressFilings(member = member?.trim()?.takeIf { it.isNotEmpty() })
        Resource.Success(response.filings.orEmpty().mapNotNull { it.toDomain() })
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Resource.Error("Congress trade data is unavailable right now. Please try again.")
    }

    override suspend fun getFilingDetail(filing: CongressFiling): Resource<CongressFilingDetail> = try {
        Resource.Success(api.getCongressFilingDetail(filing.year, filing.docId, filing.filed).toDomain(filing))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Resource.Error("Could not read this filing. Try opening the PDF instead.")
    }
}

internal fun CongressFilingDto.toDomain(): CongressFiling? {
    val id = docId?.takeIf { it.isNotBlank() } ?: return null
    return CongressFiling(
        docId = id,
        year = year ?: return null,
        name = name.orEmpty(),
        district = district.orEmpty(),
        filed = filed,
        pdfUrl = url,
    )
}

internal fun CongressFilingDetailDto.toDomain(filing: CongressFiling): CongressFilingDetail {
    val trades = trades.orEmpty().mapNotNull { it.toDomain() }
    return CongressFilingDetail(
        available = (available == true) && trades.isNotEmpty(),
        pdfUrl = pdfUrl ?: filing.pdfUrl,
        trades = trades,
    )
}

internal fun CongressTradeDto.toDomain(): CongressTrade? {
    val assetName = asset?.trim().orEmpty()
    if (assetName.isEmpty() && ticker.isNullOrBlank()) return null
    return CongressTrade(
        owner = ownerName.orEmpty().ifBlank { "self" },
        isPurchase = type == "P",
        typeLabel = typeName.orEmpty().ifBlank { type.orEmpty() },
        asset = assetName,
        ticker = ticker?.trim()?.takeIf { it.isNotEmpty() },
        amount = amount.orEmpty(),
        tradeDate = date,
        daysToDisclose = daysToDisclose,
        late = late == true,
        description = description?.takeIf { it.isNotBlank() },
        optionSummary = option?.let { o ->
            val strike = o.strike?.let { s -> ", $" + java.text.DecimalFormat("#,##0.##").format(s) + " strike" }.orEmpty()
            val exp = o.expires?.let { e -> ", expires $e" }.orEmpty()
            "${o.contracts ?: "?"} ${o.kind.orEmpty()} options$strike$exp"
        },
        shares = shares,
    )
}

