package com.invest.easymoney.data.repository

import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class AiInsightErrorMapperTest {

    private fun http(code: Int) =
        HttpException(Response.error<Any>(code, "raw provider body".toResponseBody(null)))

    @Test
    fun `402 maps to a safe message without the status code`() {
        val message = http(402).toAiInsightMessage()

        assertEquals("AI insight is temporarily unavailable.", message)
        assertFalse(message.contains("402"))
        assertFalse(message.contains("HTTP", ignoreCase = true))
    }

    @Test
    fun `401 and 403 map to temporarily unavailable`() {
        assertEquals("AI insight is temporarily unavailable.", http(401).toAiInsightMessage())
        assertEquals("AI insight is temporarily unavailable.", http(403).toAiInsightMessage())
    }

    @Test
    fun `429 maps to too many requests`() {
        assertEquals(
            "AI insight is receiving too many requests. Please try again later.",
            http(429).toAiInsightMessage()
        )
    }

    @Test
    fun `5xx maps to service unavailable`() {
        assertEquals("AI insight service is temporarily unavailable.", http(500).toAiInsightMessage())
        assertEquals("AI insight service is temporarily unavailable.", http(502).toAiInsightMessage())
    }

    @Test
    fun `503 maps to temporarily unavailable (live AI disabled)`() {
        assertEquals("AI insight is temporarily unavailable.", http(503).toAiInsightMessage())
    }

    @Test
    fun `other http codes map to generic message`() {
        assertEquals("AI insight could not be loaded.", http(418).toAiInsightMessage())
    }

    @Test
    fun `timeout and offline map to friendly messages`() {
        assertEquals("The AI insight request timed out.", SocketTimeoutException("x").toAiInsightMessage())
        assertEquals(
            "Check your internet connection and try again.",
            UnknownHostException("host").toAiInsightMessage()
        )
    }

    @Test
    fun `unknown errors never leak their message`() {
        val message = IllegalStateException("secret internal detail").toAiInsightMessage()

        assertEquals("AI insight could not be loaded.", message)
        assertFalse(message.contains("secret"))
    }
}

