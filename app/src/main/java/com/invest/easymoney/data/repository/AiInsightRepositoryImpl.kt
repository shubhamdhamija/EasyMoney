package com.invest.easymoney.data.repository

import com.google.gson.Gson
import com.invest.easymoney.data.api.OpenAiApiService
import com.invest.easymoney.data.api.dto.AiPicksJson
import com.invest.easymoney.data.api.dto.InsightJson
import com.invest.easymoney.data.api.dto.OpenAiMessage
import com.invest.easymoney.data.api.dto.OpenAiRequest
import com.invest.easymoney.data.api.dto.ResponseFormat
import com.invest.easymoney.domain.model.AiPick
import com.invest.easymoney.domain.model.NetworkResult
import com.invest.easymoney.domain.model.News
import com.invest.easymoney.domain.model.Stock
import com.invest.easymoney.domain.model.StockInsight
import com.invest.easymoney.domain.repository.AiInsightRepository
import com.invest.easymoney.util.Resource
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

private const val CACHE_TTL_MS = 30 * 60 * 1_000L // 30 minutes

// Pollinations.ai free, keyless model id
private const val MODEL = "openai"

@Singleton
class AiInsightRepositoryImpl @Inject constructor(
    private val openAiApi: OpenAiApiService,
    private val gson: Gson
) : AiInsightRepository {

    private val cacheMutex = Mutex()
    private val insightCache = LinkedHashMap<String, Pair<StockInsight, Long>>()
    private var picksCache: Pair<List<AiPick>, Long>? = null

    // ── Stock Insight ─────────────────────────────────────────────────────────

    override suspend fun getStockInsight(
        symbol: String,
        stock: Stock,
        news: List<News>
    ): NetworkResult<StockInsight> {
        cacheMutex.withLock {
            val cached = insightCache[symbol]
            if (cached != null && System.currentTimeMillis() - cached.second < CACHE_TTL_MS) {
                return NetworkResult.Success(cached.first)
            }
        }

        return runCatching {
            val prompt = buildInsightPrompt(symbol, stock, news)
            val response = openAiApi.chatCompletion(
                OpenAiRequest(
                    model = MODEL,
                    messages = listOf(
                        OpenAiMessage(
                            role = "system",
                            content = "You are a financial data analyst. Respond only with valid JSON matching the requested schema. Do not provide personalized financial advice."
                        ),
                        OpenAiMessage(role = "user", content = prompt)
                    ),
                    responseFormat = ResponseFormat(type = "json_object"),
                    maxTokens = 300
                )
            )

            val content = response.choices?.firstOrNull()?.message?.content
                ?: return NetworkResult.Error(IllegalStateException("Empty response from AI"))

            val parsed = parseInsight(content)
                ?: return NetworkResult.Error(IllegalStateException("Failed to parse AI response"))

            val sentiment = parsed.sentiment?.lowercase()?.trim()
                ?.takeIf { it in setOf("bullish", "bearish", "neutral") } ?: "neutral"

            val insight = StockInsight(
                symbol = symbol,
                sentiment = sentiment,
                insight = parsed.insight?.trim() ?: "No insight available.",
                shortTermOutlook = parsed.shortTerm?.trim() ?: "Outlook unavailable.",
                longTermOutlook = parsed.longTerm?.trim() ?: "Outlook unavailable."
            )

            cacheMutex.withLock {
                insightCache[symbol] = Pair(insight, System.currentTimeMillis())
            }

            NetworkResult.Success(insight)
        }.getOrElse { e ->
            if (isNetworkError(e)) {
                NetworkResult.Success(buildFallbackInsight(symbol, stock, news))
            } else {
                NetworkResult.Error(IllegalStateException(mapApiError(e), e))
            }
        }
    }

    // ── AI Picks ──────────────────────────────────────────────────────────────

    override suspend fun getAiPicks(stocks: List<Stock>): Resource<List<AiPick>> {
        cacheMutex.withLock {
            val cached = picksCache
            if (cached != null && System.currentTimeMillis() - cached.second < CACHE_TTL_MS) {
                return Resource.Success(cached.first)
            }
        }

        return runCatching {
            val prompt = buildPicksPrompt(stocks)
            val response = openAiApi.chatCompletion(
                OpenAiRequest(
                    model = MODEL,
                    messages = listOf(
                        OpenAiMessage(
                            role = "system",
                            content = "You are a financial data analyst. Respond only with valid JSON. Do not provide personalized financial advice."
                        ),
                        OpenAiMessage(role = "user", content = prompt)
                    ),
                    responseFormat = ResponseFormat(type = "json_object"),
                    maxTokens = 400
                )
            )

            val content = response.choices?.firstOrNull()?.message?.content
                ?: return Resource.Error("Empty response from AI")

            val parsed = parsePicks(content)
                ?: return Resource.Error("Failed to parse AI picks response")

            val picks = parsed.picks
                ?.filter { !it.symbol.isNullOrBlank() && !it.reason.isNullOrBlank() }
                ?.map { AiPick(symbol = it.symbol!!, reason = it.reason!!) }
                ?: emptyList()

            cacheMutex.withLock {
                picksCache = Pair(picks, System.currentTimeMillis())
            }

            Resource.Success(picks)
        }.getOrElse { e ->
            if (isNetworkError(e)) {
                Resource.Success(buildFallbackPicks(stocks))
            } else {
                Resource.Error(mapApiError(e))
            }
        }
    }

    // ── JSON extraction ───────────────────────────────────────────────────────

    /**
     * Free models sometimes wrap JSON in markdown fences or add stray text.
     * Extract the first {...} block before parsing.
     */
    private fun extractJson(raw: String): String {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        return if (start in 0 until end) raw.substring(start, end + 1) else raw
    }

    private fun parseInsight(content: String): InsightJson? =
        runCatching { gson.fromJson(extractJson(content), InsightJson::class.java) }.getOrNull()

    private fun parsePicks(content: String): AiPicksJson? =
        runCatching { gson.fromJson(extractJson(content), AiPicksJson::class.java) }.getOrNull()

    // ── Prompt Builders ───────────────────────────────────────────────────────

    private fun buildInsightPrompt(symbol: String, stock: Stock, news: List<News>): String {
        val changeSign = if (stock.changePercent >= 0) "+" else ""
        val newsSection = news.take(7).mapIndexed { i, n -> "${i + 1}. ${n.headline}" }
            .joinToString("\n")

        return """
Analyze the following stock data and provide a structured JSON insight.

Stock: $symbol${if (stock.name.isNotEmpty()) " (${stock.name})" else ""}
Current Price: ${"$"}${String.format("%.2f", stock.currentPrice)} ($changeSign${String.format("%.2f", stock.changePercent)}% today)
Day Range: ${"$"}${String.format("%.2f", stock.lowPrice)} – ${"$"}${String.format("%.2f", stock.highPrice)}

Recent News Headlines:
$newsSection

Respond with ONLY this JSON (no extra text):
{
  "sentiment": "<bullish|bearish|neutral>",
  "insight": "<2-3 sentences explaining why the stock is moving today>",
  "short_term": "<1-2 sentences on short-term outlook (days to weeks)>",
  "long_term": "<1-2 sentences on long-term outlook (months+)>"
}
        """.trimIndent()
    }

    private fun buildPicksPrompt(stocks: List<Stock>): String {
        val stockLines = stocks.joinToString("\n") { s ->
            val sign = if (s.changePercent >= 0) "+" else ""
            "- ${s.symbol}: ${"$"}${String.format("%.2f", s.currentPrice)} ($sign${String.format("%.2f", s.changePercent)}%)"
        }

        return """
Based on today's market data, select the top 3 stocks from this list:

$stockLines

Respond with ONLY this JSON (no extra text):
{
  "picks": [
    { "symbol": "<SYMBOL>", "reason": "<1-2 sentence reason why this stock stands out today>" },
    { "symbol": "<SYMBOL>", "reason": "<reason>" },
    { "symbol": "<SYMBOL>", "reason": "<reason>" }
  ]
}
        """.trimIndent()
    }

    // ── Error Mapping ─────────────────────────────────────────────────────────

    private fun isNetworkError(e: Throwable): Boolean {
        val msg = e.message?.lowercase() ?: ""
        return msg.contains("failed to connect") ||
            msg.contains("unable to resolve") ||
            msg.contains("connection refused") ||
            msg.contains("timeout") ||
            msg.contains("no route to host") ||
            e is java.net.ConnectException ||
            e is java.net.SocketTimeoutException ||
            e is java.net.UnknownHostException
    }

    private fun mapApiError(e: Throwable): String = when {
        e.message?.contains("429") == true -> "AI rate limit reached. Please try again in a moment."
        e.message?.contains("timeout", ignoreCase = true) == true -> "AI request timed out. Please try again."
        else -> "AI insight unavailable: ${e.message ?: "Unknown error"}"
    }

    // ── Offline Fallbacks ─────────────────────────────────────────────────────

    private fun buildFallbackInsight(symbol: String, stock: Stock, news: List<News>): StockInsight {
        val sentiment = when {
            stock.changePercent >= 2.0 -> "bullish"
            stock.changePercent <= -2.0 -> "bearish"
            else -> "neutral"
        }
        val directionWord = when (sentiment) {
            "bullish" -> "up"
            "bearish" -> "down"
            else -> "relatively flat"
        }
        val changeSign = if (stock.changePercent >= 0) "+" else ""
        val pctFormatted = "${changeSign}${String.format("%.2f", stock.changePercent)}%"
        val topHeadline = news.firstOrNull()?.headline?.let { ". Recent news: $it" } ?: ""
        return StockInsight(
            symbol = symbol,
            sentiment = sentiment,
            insight = "$symbol is trading ${directionWord} ${pctFormatted} today${topHeadline}. " +
                "This analysis is based on live price data (AI offline).",
            shortTermOutlook = "Short-term trend appears ${directionWord}. Monitor support/resistance levels.",
            longTermOutlook = "Long-term outlook depends on fundamentals and broader market conditions."
        )
    }

    private fun buildFallbackPicks(stocks: List<Stock>): List<AiPick> {
        return stocks
            .filter { it.changePercent != 0.0 }
            .sortedByDescending { kotlin.math.abs(it.changePercent) }
            .take(3)
            .map { s ->
                val dir = if (s.changePercent > 0) "gaining" else "declining"
                val sign = if (s.changePercent >= 0) "+" else ""
                AiPick(
                    symbol = s.symbol,
                    reason = "${s.symbol} is ${dir} ${sign}${String.format("%.2f", s.changePercent)}% today, " +
                        "showing notable price movement worth monitoring. (AI offline — based on live data)"
                )
            }
    }
}

