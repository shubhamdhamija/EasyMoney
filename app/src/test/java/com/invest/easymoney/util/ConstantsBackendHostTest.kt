package com.invest.easymoney.util

import org.junit.Assert.assertTrue
import org.junit.Test

class ConstantsBackendHostTest {
    @Test
    fun `backend host resolves to a valid http endpoint`() {
        val backendUrl = Constants.BACKEND_BASE_URL
        assertTrue(backendUrl.startsWith("http://"))
        assertTrue(backendUrl.endsWith("/"))
        assertTrue(backendUrl.contains(":"))
    }
}
