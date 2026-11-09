package com.invest.easymoney.data.repository

import com.google.gson.Gson
import com.invest.easymoney.data.api.AgentAccessTokenProvider
import com.invest.easymoney.data.api.BackendApiService
import com.invest.easymoney.domain.model.AgentResearchResult
import com.invest.easymoney.domain.model.NetworkResult
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class AgentResearchRepositoryImplTest {

    private val backend = mockk<BackendApiService>()
    private val repo = AgentResearchRepositoryImpl(backend, mockk<AgentAccessTokenProvider>(relaxed = true), Gson())

    @Test
    fun `flat completed payload maps to Completed`() = runTest {
        val json = """{"symbol":"AAPL","status":"completed","summary":"AAPL is up.","quote":{"symbol":"AAPL","current_price":228.3,"price_change_percent":2.4,"currency":"USD"},"news_count":3,"tools_used":["get_stock_quote"],"error_code":null,"agent_used":true}"""
        coEvery { backend.startAgentResearch(any()) } returns Response.success(json.toResponseBody("application/json".toMediaType()))

        val result = repo.startResearch("aapl")

        assertTrue(result is NetworkResult.Success)
        val data = (result as NetworkResult.Success).data
        assertTrue(data is AgentResearchResult.Completed)
        assertEquals("AAPL", (data as AgentResearchResult.Completed).report.symbol)
    }
}

