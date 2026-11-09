package com.invest.easymoney.domain.repository

import com.invest.easymoney.domain.model.CongressFiling
import com.invest.easymoney.domain.model.CongressFilingDetail
import com.invest.easymoney.util.Resource

/** Public House Periodic Transaction Report (STOCK Act) disclosures. Informational only. */
interface CongressRepository {
    suspend fun getFilings(member: String?): Resource<List<CongressFiling>>
    suspend fun getFilingDetail(filing: CongressFiling): Resource<CongressFilingDetail>
}

