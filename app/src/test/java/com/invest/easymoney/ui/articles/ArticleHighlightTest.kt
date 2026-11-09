package com.invest.easymoney.ui.articles

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.assertEquals
import org.junit.Test

class ArticleHighlightTest {
    @Test
    fun `highlights repeated case insensitive literal terms without regex interpretation`() {
        val text = "C++ earnings and c++ EARNINGS"
        val highlighted = highlightArticleText(text, " C++ earnings ", SpanStyle(fontWeight = FontWeight.Bold))
        assertEquals(text, highlighted.text)
        assertEquals(
            listOf("C++", "c++", "earnings", "EARNINGS"),
            highlighted.spanStyles.map { text.substring(it.start, it.end) }
        )
    }

    @Test
    fun `blank query leaves text unchanged and unstyled`() {
        val highlighted = highlightArticleText("Apple earnings", "  ", SpanStyle())
        assertEquals("Apple earnings", highlighted.text)
        assertEquals(emptyList<Any>(), highlighted.spanStyles)
    }
}
