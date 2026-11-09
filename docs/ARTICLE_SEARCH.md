# Global article search

Open **Home > Search articles** (the article icon in the top bar). Search by
company, topic, or keyword across stocks, independently of the current stock
detail page.

## Apple Notes reference and evidence

The reference is the installed `/System/Applications/Notes.app`, version 4.13,
not a third-party notes app. No personal notes, accounts, or Notes database were
read. No Apple implementation code was copied.

| Finding | Evidence | Confidence and boundary |
| --- | --- | --- |
| Search can span all accounts or be scoped to the current account. Suggested searches narrow the content type. | Apple's [Search your notes on Mac](https://support.apple.com/guide/notes/search-your-notes-not18ab658ed/mac), sections "How search works in Notes" and "Search all notes". | Documented behavior, not an observed runtime session. Account capabilities vary. |
| Top Hits precede other results. Apple names title matches and recent updates as ranking factors. | Same guide, "Search all notes". | Documented factors, not a recovered scoring formula. |
| The installed executable imports search operations, suggestion context, and tokens, and links CoreSpotlight and NotesShared. | REA `inspect_macho` Evidence `ev_3658af8fab81733a024441bc2101f4e924bbf23ecc509b4297eaa8ce66a7dce3`; imports include `_OBJC_CLASS_$_ICSearchQueryOperation`, `_OBJC_CLASS_$_ICSearchSuggestionsContext`, `_OBJC_CLASS_$_ICSearchToken`, and a NotesUI `searchResults` symbol with `queryString` and `rankingQueries`. | Static dependency/import observations. They suggest an indexed, ranked query pipeline but do not establish which path executes or how ranking works. |
| Native UI metadata includes query, scope, suggestions, sections, and cancellation concepts. | Ordinary `strings -a -t x` on the same executable: `ICMSearchQueryController` at file offset `0x56adbd`, `ICMSearchBarIdentifierSearchScope` at `0x541401`, `Suggested Searches` at `0x5411ed`, `_topHitHeader` property metadata at `0x5702a4`, and `cancelSearchRestoringSelection:` at `0x5768b6`. | Static strings, not REA function evidence or proof of execution. These offsets are for this exact universal file, not virtual addresses. |

Artifact identity: SHA-256
`d95c11101f67217d37df1fa084791493b5470ddc437872c503d88ecff8cb7637`,
arm64e slice UUID `302C199D-C688-3AED-A9FF-DE48EFB1FB28`.
The REA record notes that stripped/toolchain-hidden symbols may be absent.

The focused Hopper procedure search timed out waiting for bridge readiness.
No decompilation, runtime timing, exact matching algorithm, or internal ranking
formula was established. REA reported incomplete cleanup of a Hopper document
and private runtime `/tmp/rea-2kQEFh`; that document may need manual closure in
Hopper before another native analysis. MCP configuration was not changed.
Complete identity evidence and the failed-analysis diagnostic are retained in
this Copilot session's `files/notes-macho.json` and
`files/notes-search-procedures.json`.

## Adaptation, not an Apple search clone

- A dedicated global Articles page replaces Notes' all-account scope with
  market news across stocks.
- Suggested topic chips start ordinary keyword searches; they are not generated
  recommendations, personalized history, or a natural-language date parser.
- Query edits debounce for 350 ms; keyboard Search and Retry submit immediately.
  Clearing cancels work and removes previous results. Superseded responses
  cannot replace newer results.
- The first three provider-ranked articles appear under **Top Hits**, followed
  by **More Results**. This intentionally preserves provider relevance rather
  than claiming Apple's title/recency scoring.
- Query words are highlighted literally and case-insensitively in headlines and
  publisher names. Highlighting is presentation only, not the provider's matching
  algorithm.
- **Past week** filters the returned set using publication timestamps. It does
  not request a complete date-bounded archive.
- Article taps use the existing in-app browser. Queries and the selected date
  filter survive saved-state restoration; results are fetched again.

## Data flow and limitations

Compose sends `ArticleSearchIntent` to `ArticleSearchViewModel`, which exposes
immutable `StateFlow<ArticleSearchUiState>`. `ArticleRepository` calls the backend
through Retrofit. The existing stock-symbol search and stock-detail News tab are
unchanged.

`GET /articles/search?q=<query>` delegates to `articleSearchService`. The service
uses the same Yahoo Finance news-search provider already used by stock news,
but with the user's query, no ticker restriction, `quotesCount=0`, and
`newsCount=30`. The endpoint returns:

```json
{
  "ok": true,
  "articles": [
    {
      "id": "provider-article-id",
      "headline": "Provider-supplied headline",
      "source": "Publisher",
      "url": "https://example.com/article",
      "datetime": 1700000000
    }
  ],
  "limit": 30
}
```

This searches the provider's available news across stocks, **not every article
ever published**. The provider can return fewer than 30 items and does not
provide a total count or pagination through this integration. Only headline,
publisher, link, and publication time are available; article bodies, PDF/OCR,
attachments, semantic embeddings, and Notes-style natural-language search are
not implemented. There is no persistent local article archive.

The backend normalizes whitespace and requires 2-200 query characters. Invalid
queries produce HTTP 400. Upstream/network/timeout or malformed article responses
produce HTTP 502 with safe error messages, not an empty success. The provider
timeout is 10 seconds. Both boundaries validate fields and permit only HTTP(S)
article URLs without embedded credentials. Duplicate IDs/URLs are removed while
preserving order. No LLM, new API key, or AI-generated financial content is used.

Run the backend normally with `cd server && npm start`; no new environment
variables or dependency installation are required.

## Focused checks

```bash
node --test server/tests/articleSearch.test.js
./gradlew :app:testDebugUnitTest \
  --tests 'com.invest.easymoney.data.repository.ArticleRepositoryImplTest' \
  --tests 'com.invest.easymoney.ui.articles.*' :app:assembleDebug :app:lintDebug
ANDROID_SERIAL=<compatible-device-serial> ./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.invest.easymoney.ui.articles.ArticleSearchScreenTest,com.invest.easymoney.ui.home.HomeSearchFirstTopBarTest
```

Verification for this change: the debug APK built, 13 article unit tests and
9 Compose/navigation tests passed (the latter on the existing Android 15
Galaxy_S24 AVD). Five article backend tests and two existing dependency tests
passed. A real request through the article router and Yahoo provider returned
HTTP 200 with two validated articles.

The Android 17 emulator could not run Espresso interactions because the
repository's existing Espresso version calls the removed
`InputManager.getInstance` method; this affected both new and existing tests.
The successful Android 15 run did not require dependency changes.

`lintDebug` remains blocked by the existing Compose lint/toolchain
incompatibility: `ComposableFlowOperatorDetector` throws
`IncompatibleClassChangeError` for `KaFunctionCall` (the diagnostic names
`ExampleUnitTest.kt`). The normal lint configuration was not disabled or
changed to hide that failure.
