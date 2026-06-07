package com.invest.easymoney.ui.charts

import org.junit.Assert.assertEquals
import org.junit.Test

class ChartMapperTest {
    @Test
    fun `toLabelList returns index labels when times null or mismatch`() {
        val labels = ChartMapper.toLabelList(null, 3)
        assertEquals(listOf("0","1","2"), labels)
    }

    @Test
    fun `toLabelList formats times correctly`() {
        // epoch seconds for 10:15 and 11:30 UTC on an arbitrary day (use seconds since epoch for those times)
        val t1 = 10L * 3600 + 15L * 60 // 36900
        val t2 = 11L * 3600 + 30L * 60 // 41400
        val labels = ChartMapper.toLabelList(listOf(t1, t2), 2)
        assertEquals(listOf("10:15","11:30"), labels)
    }
}
