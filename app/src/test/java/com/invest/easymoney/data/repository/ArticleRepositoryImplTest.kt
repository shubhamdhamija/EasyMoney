package com.invest.easymoney.data.repository

import com.invest.easymoney.data.api.ArticleApiService
import com.invest.easymoney.data.api.ArticleDto
import com.invest.easymoney.data.api.ArticleSearchResponse
import com.invest.easymoney.domain.model.NetworkResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArticleRepositoryImplTest {
    private val api = mockk<ArticleApiService>()
    private val repository = ArticleRepositoryImpl(api)
    private val article = ArticleDto(
        id = "apple",
        headline = "Apple earnings",
        source = "Test publisher",
        url = "https://example.com/apple",
        datetime = 1700000000
    )

    @Test
    fun `normalizes query and maps deduplicated articles`() = runTest {
        coEvery { api.searchArticles("earnings report") } returns
            ArticleSearchResponse(true, listOf(article, article))
        val result = repository.searchArticles("  earnings   report  ") as NetworkResult.Success
        assertEquals(1, result.data.size)
        assertEquals("Apple earnings", result.data.single().headline)
    }

    @Test
    fun `invalid queries never call the backend`() = runTest {
        for (query in listOf("", " ", "a", "a".repeat(201))) {
            assertTrue(repository.searchArticles(query) is NetworkResult.Error)
        }
        coVerify(exactly = 0) { api.searchArticles(any()) }
    }

    @Test
    fun `rejects invalid responses including null articles and unsafe urls`() = runTest {
        for (response in listOf(
            ArticleSearchResponse(false),
            ArticleSearchResponse(true, null),
            ArticleSearchResponse(true, listOf(null)),
            ArticleSearchResponse(true, listOf(article.copy(id = null))),
            ArticleSearchResponse(true, listOf(article.copy(headline = ""))),
            ArticleSearchResponse(true, listOf(article.copy(url = "javascript:alert(1)"))),
            ArticleSearchResponse(true, listOf(article.copy(url = "https://u:p@example.com"))),
            ArticleSearchResponse(true, listOf(article.copy(datetime = -1)))
        )) {
            coEvery { api.searchArticles("earnings") } returns response
            assertTrue(repository.searchArticles("earnings") is NetworkResult.Error)
        }
    }

    @Test
    fun `network failure is an error not empty success`() = runTest {
        coEvery { api.searchArticles("earnings") } throws IOException("offline")
        assertTrue(repository.searchArticles("earnings") is NetworkResult.Error)
    }

    @Test(expected = CancellationException::class)
    fun `cancellation propagates`() = runTest {
        coEvery { api.searchArticles("earnings") } throws CancellationException()
        repository.searchArticles("earnings")
        Unit
    }
}
