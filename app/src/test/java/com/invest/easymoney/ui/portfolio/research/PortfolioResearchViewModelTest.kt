package com.invest.easymoney.ui.portfolio.research

import com.invest.easymoney.domain.model.NetworkResult
import com.invest.easymoney.domain.portfolio.research.PortfolioResearchException
import com.invest.easymoney.domain.portfolio.research.PortfolioResearchReport
import com.invest.easymoney.domain.portfolio.research.PortfolioResearchRepository
import com.invest.easymoney.domain.portfolio.PortfolioSource
import com.invest.easymoney.domain.portfolio.research.PORTFOLIO_RESEARCH_NO_TRADES_STATEMENT
import com.invest.easymoney.domain.portfolio.research.ResearchConsentHandle
import com.invest.easymoney.domain.portfolio.research.ResearchDataCategory
import com.invest.easymoney.domain.portfolio.research.ResearchPortfolioTarget
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PortfolioResearchViewModelTest {

    private class FakeRepo : PortfolioResearchRepository {
        var consentCalls = 0
        var researchCalls = 0
        var revokeCalls = 0
        var consentResult: NetworkResult<ResearchConsentHandle> = NetworkResult.Success(handle())
        var gate: CompletableDeferred<Unit>? = null
        var researchResult: NetworkResult<PortfolioResearchReport> = NetworkResult.Success(report())
        var lastCategories: Set<ResearchDataCategory> = emptySet()

        override suspend fun createResearchConsent(
            target: ResearchPortfolioTarget,
            categories: Set<ResearchDataCategory>
        ): NetworkResult<ResearchConsentHandle> {
            consentCalls++
            lastCategories = categories
            return consentResult
        }

        override suspend fun startPortfolioResearch(
            handle: ResearchConsentHandle,
            categories: Set<ResearchDataCategory>
        ): NetworkResult<PortfolioResearchReport> {
            researchCalls++
            gate?.await()
            return researchResult
        }

        override suspend fun revokeResearchConsent(handle: ResearchConsentHandle): NetworkResult<Unit> {
            revokeCalls++
            return NetworkResult.Success(Unit)
        }
    }

    companion object {
        fun handle() = ResearchConsentHandle(
            "pc_1", "pcn_1", PortfolioSource.SNAPTRADE_SANDBOX,
            setOf(ResearchDataCategory.ACCOUNTS, ResearchDataCategory.POSITIONS), null
        )

        fun report() = PortfolioResearchReport(
            summary = "ok", partial = false, source = PortfolioSource.SNAPTRADE_SANDBOX, snapshotAt = null,
            currencySummaries = emptyList(), positions = emptyList(), concentration = emptyList(),
            warnings = emptyList(), evidence = emptyList(), statement = PORTFOLIO_RESEARCH_NO_TRADES_STATEMENT
        )
    }

    private val target = ResearchPortfolioTarget.Connected("pc_1", null)
    private lateinit var repo: FakeRepo
    private lateinit var vm: PortfolioResearchViewModel

    @Before fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repo = FakeRepo()
        vm = PortfolioResearchViewModel(repo)
    }

    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun nothingIsRequestedOnCreation() {
        assertEquals(PortfolioResearchUiState.Idle, vm.state.value)
        assertEquals(0, repo.consentCalls + repo.researchCalls)
    }

    @Test fun buttonOnlyOpensConsentDialogAndSendsNothing() {
        vm.requestResearch(target)
        assertTrue(vm.state.value is PortfolioResearchUiState.ConsentRequired)
        assertEquals(0, repo.consentCalls)
        vm.cancelConsent()
        assertEquals(PortfolioResearchUiState.Idle, vm.state.value)
        assertEquals(0, repo.consentCalls + repo.researchCalls)
    }

    @Test fun confirmingRunsResearchThenRevokesConsent() {
        vm.requestResearch(target)
        vm.confirmConsent(setOf(ResearchDataCategory.CASH_BALANCES))
        assertTrue(vm.state.value is PortfolioResearchUiState.Success)
        assertEquals(1, repo.consentCalls)
        assertEquals(1, repo.researchCalls)
        assertEquals(1, repo.revokeCalls)
        // Accounts and holdings are always part of the consent; book values stay off unless chosen.
        assertTrue(ResearchDataCategory.POSITIONS in repo.lastCategories)
        assertFalse(ResearchDataCategory.BOOK_VALUES in repo.lastCategories)
    }

    @Test fun confirmWithoutDialogDoesNothing() {
        vm.confirmConsent(setOf(ResearchDataCategory.BOOK_VALUES))
        assertEquals(0, repo.consentCalls)
        assertEquals(PortfolioResearchUiState.Idle, vm.state.value)
    }

    @Test fun repeatedConfirmWhileRunningStartsOnlyOneRun() {
        repo.gate = CompletableDeferred()
        vm.requestResearch(target)
        vm.confirmConsent(emptySet())
        assertEquals(PortfolioResearchUiState.Researching, vm.state.value)
        vm.confirmConsent(emptySet())
        vm.requestResearch(target)
        repo.gate!!.complete(Unit)
        assertEquals(1, repo.consentCalls)
        assertEquals(1, repo.researchCalls)
    }

    @Test fun consentFailureShowsSafeErrorWithoutResearching() {
        repo.consentResult = NetworkResult.Error(
            PortfolioResearchException("PORTFOLIO_NOT_FOUND", "This portfolio is no longer available.", retryable = false)
        )
        vm.requestResearch(target)
        vm.confirmConsent(emptySet())
        val error = vm.state.value as PortfolioResearchUiState.Error
        assertEquals("This portfolio is no longer available.", error.message)
        assertFalse(error.retryable)
        assertEquals(0, repo.researchCalls)
        assertEquals(0, repo.revokeCalls) // no consent was created
    }

    @Test fun researchFailureStillRevokesConsentAndRetryReopensDialog() {
        repo.researchResult = NetworkResult.Error(PortfolioResearchException("UNAVAILABLE", "Temporarily unavailable."))
        vm.requestResearch(target)
        vm.confirmConsent(emptySet())
        assertTrue(vm.state.value is PortfolioResearchUiState.Error)
        assertEquals(1, repo.revokeCalls)
        vm.retry() // never re-sends silently: the user must confirm again
        assertTrue(vm.state.value is PortfolioResearchUiState.ConsentRequired)
        assertEquals(1, repo.consentCalls)
    }

    @Test fun resetReturnsToIdleAndStatesNeverExposeConsentIds() {
        vm.requestResearch(target)
        vm.confirmConsent(emptySet())
        vm.reset()
        assertEquals(PortfolioResearchUiState.Idle, vm.state.value)
        assertFalse(handle().toString().contains("pcn_1"))
        assertFalse(target.toString().contains("pc_1"))
    }
}
