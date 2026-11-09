package com.invest.easymoney.data.repository

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentResearchFailureTest {
    @Test
    fun quoteNotFound_isNotRetryableAndNamesSymbol() {
        val failure = describeResearchFailure("QUOTE_NOT_FOUND", "ORCL")
        assertFalse(failure.retryable)
        assertTrue(failure.message.contains("ORCL"))
    }

    @Test
    fun unknownCode_isRetryable() {
        assertTrue(describeResearchFailure("SOMETHING_ELSE", "AAPL").retryable)
    }
}

