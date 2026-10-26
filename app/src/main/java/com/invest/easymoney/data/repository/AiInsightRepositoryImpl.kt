package com.invest.easymoney.data.repository

import android.util.Log
import com.invest.easymoney.data.api.BackendApiService
import com.invest.easymoney.domain.model.AiPick
import com.invest.easymoney.domain.model.NetworkResult
import com.invest.easymoney.domain.model.News
import com.invest.easymoney.domain.model.Stock
import com.invest.easymoney.domain.model.StockInsight
import com.invest.easymoney.domain.repository.AiInsightRepository
import com.invest.easymoney.util.Resource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

private const val CACHE_TTL_MS = 30 * 60 * 1_000L // 30 minutes
private const val TAG = "AiInsightRepository"

/** Both stock insight and AI picks are served by the EasyMoney backend. */
@Singleton
class AiInsightRepositoryImpl @Inject constructor(
    private val backendApiService: BackendApiService
) : AiInsightRepository {

    private val cacheMutex = Mutex()
    private val insightCache = LinkedHashMap<String, Pair<StockInsight, Long>>()
    private var picksCache: Pair<List<AiPick>, Long>? = null

    // ── Stock Insight (POST /ai/stock-insight) ───────────────────────────────

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

        return try {
            val response = backendApiService.getStockInsight(
                createStockInsightRequest(stock = stock, news = news)
            )
            val insight = response.toDomain()

            cacheMutex.withLock {
                insightCache[symbol] = Pair(insight, System.currentTimeMillis())
            }

            NetworkResult.Success(insight)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            // Raw details go to debug logs only, never to the UI.
            Log.d(TAG, "AI insight request failed", e)
            NetworkResult.Error(IllegalStateException(e.toAiInsightMessage(), e))
        }
    }

    // ── AI Picks (POST /ai/picks) ─────────────────────────────────────────────

    override suspend fun getAiPicks(stocks: List<Stock>): Resource<List<AiPick>> {
        cacheMutex.withLock {
            val cached = picksCache
            if (cached != null && System.currentTimeMillis() - cached.second < CACHE_TTL_MS) {
                return Resource.Success(cached.first)
            }
        }

        val request = createStockPicksRequest(stocks)
        if (request.stocks.isEmpty()) {
            return Resource.Success(emptyList())
        }

        return try {
            val picks = backendApiService.getAiPicks(request).toDomain()

            cacheMutex.withLock {
                picksCache = Pair(picks, System.currentTimeMillis())
            }

            Resource.Success(picks)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Log.d(TAG, "AI picks request failed", e)
            Resource.Error(e.toAiPicksMessage())
        }
    }
}

