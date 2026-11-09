package com.invest.easymoney.ui.articles

import androidx.lifecycle.SavedStateHandle
import com.invest.easymoney.MainDispatcherRule
import com.invest.easymoney.domain.model.Article
import com.invest.easymoney.domain.model.NetworkResult
import com.invest.easymoney.domain.repository.ArticleRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ArticleSearchViewModelTest {
    @get:Rule
    val dispatcherRule = MainDispatcherRule()
    private val repository = mockk<ArticleRepository>()
    private val apple = Article("apple", "Apple earnings", "Test", "https://example.com/apple", System.currentTimeMillis() / 1000 - 60)
    private val microsoft = apple.copy(id = "microsoft", headline = "Microsoft earnings", url = "https://example.com/microsoft")

    @Test
    fun `debounces typing then displays articles across stocks`() = runTest {
        coEvery { repository.searchArticles("earnings") } returns NetworkResult.Success(listOf(apple, microsoft))
        val vm = ArticleSearchViewModel(repository, SavedStateHandle())
        vm.onIntent(ArticleSearchIntent.QueryChanged("earn"))
        advanceTimeBy(200)
        vm.onIntent(ArticleSearchIntent.QueryChanged("earnings"))
        advanceTimeBy(349)
        coVerify(exactly = 0) { repository.searchArticles(any()) }
        advanceUntilIdle()
        assertEquals(listOf(apple, microsoft), vm.uiState.value.articles)
        assertFalse(vm.uiState.value.isLoading)
        assertTrue(vm.uiState.value.hasSearched)
    }

    @Test
    fun `ignores late old responses even from a noncancellable repository`() = runTest {
        coEvery { repository.searchArticles("apple") } coAnswers {
            withContext(NonCancellable) {
                delay(2000)
                NetworkResult.Success(listOf(apple))
            }
        }
        coEvery { repository.searchArticles("microsoft") } returns NetworkResult.Success(listOf(microsoft))
        val vm = ArticleSearchViewModel(repository, SavedStateHandle())
        vm.onIntent(ArticleSearchIntent.QueryChanged("apple"))
        advanceTimeBy(351)
        vm.onIntent(ArticleSearchIntent.QueryChanged("microsoft"))
        advanceUntilIdle()
        assertEquals(listOf(microsoft), vm.uiState.value.articles)
    }

    @Test
    fun `clearing query cancels pending search and removes results`() = runTest {
        coEvery { repository.searchArticles("apple") } returns NetworkResult.Success(listOf(apple))
        val vm = ArticleSearchViewModel(repository, SavedStateHandle())
        vm.onIntent(ArticleSearchIntent.QueryChanged("apple"))
        advanceUntilIdle()
        vm.onIntent(ArticleSearchIntent.QueryChanged("microsoft"))
        vm.onIntent(ArticleSearchIntent.QueryChanged(""))
        advanceUntilIdle()
        assertTrue(vm.uiState.value.articles.isEmpty())
        assertFalse(vm.uiState.value.hasSearched)
        assertFalse(vm.uiState.value.isLoading)
        coVerify(exactly = 0) { repository.searchArticles("microsoft") }
    }

    @Test
    fun `filters fetched articles without another request and restores all dates`() = runTest {
        val old = microsoft.copy(datetime = apple.datetime - 8 * 24 * 60 * 60)
        coEvery { repository.searchArticles("earnings") } returns NetworkResult.Success(listOf(apple, old))
        val savedState = SavedStateHandle(mapOf("articleQuery" to "earnings"))
        val vm = ArticleSearchViewModel(repository, savedState)
        advanceUntilIdle()
        vm.onIntent(ArticleSearchIntent.FilterChanged(ArticleDateFilter.PAST_WEEK))
        assertEquals(listOf(apple), vm.uiState.value.articles)
        assertEquals("PAST_WEEK", savedState.get<String>("articleDateFilter"))
        vm.onIntent(ArticleSearchIntent.FilterChanged(ArticleDateFilter.ANY_TIME))
        assertEquals(listOf(apple, old), vm.uiState.value.articles)
        coVerify(exactly = 1) { repository.searchArticles(any()) }
    }

    @Test
    fun `shows errors and retry submits immediately`() = runTest {
        coEvery { repository.searchArticles("earnings") } returns
            NetworkResult.Error(IllegalStateException("Search unavailable")) andThen
            NetworkResult.Success(emptyList())
        val vm = ArticleSearchViewModel(repository, SavedStateHandle())
        vm.onIntent(ArticleSearchIntent.QueryChanged("earnings"))
        advanceUntilIdle()
        assertEquals("Search unavailable", vm.uiState.value.error)
        vm.onIntent(ArticleSearchIntent.Retry)
        runCurrent()
        assertEquals(null, vm.uiState.value.error)
        assertTrue(vm.uiState.value.hasSearched)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `short and oversized queries do not search`() = runTest {
        val vm = ArticleSearchViewModel(repository, SavedStateHandle())
        for (query in listOf("a", "  ", "a".repeat(201))) {
            vm.onIntent(ArticleSearchIntent.QueryChanged(query))
            advanceUntilIdle()
            assertFalse(vm.uiState.value.isLoading)
        }
        assertEquals("Use 200 characters or fewer.", vm.uiState.value.error)
        coVerify(exactly = 0) { repository.searchArticles(any()) }
    }
}
