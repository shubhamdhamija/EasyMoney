package com.invest.easymoney.ui.articles

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.invest.easymoney.domain.model.Article
import com.invest.easymoney.ui.webview.WebViewBottomSheet
import java.text.DateFormat
import java.util.Date

@Composable
fun ArticleSearchScreen(
    onBack: () -> Unit,
    viewModel: ArticleSearchViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var articleUrl by rememberSaveable { mutableStateOf<String?>(null) }

    ArticleSearchContent(
        state = state,
        onIntent = viewModel::onIntent,
        onBack = onBack,
        onOpenArticle = { articleUrl = it.url }
    )
    articleUrl?.let { url ->
        WebViewBottomSheet(url = url, onDismiss = { articleUrl = null })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ArticleSearchContent(
    state: ArticleSearchUiState,
    onIntent: (ArticleSearchIntent) -> Unit,
    onBack: () -> Unit,
    onOpenArticle: (Article) -> Unit
) {
    val keyboard = LocalSoftwareKeyboardController.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search articles") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Market news across all stocks", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = state.query,
                onValueChange = { onIntent(ArticleSearchIntent.QueryChanged(it)) },
                label = { Text("Search by company, topic, or keyword") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { onIntent(ArticleSearchIntent.QueryChanged("")) }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear article search")
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    keyboard?.hide()
                    onIntent(ArticleSearchIntent.Submit)
                }),
                modifier = Modifier.fillMaxWidth().testTag("ArticleSearchInput")
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ArticleDateFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = state.dateFilter == filter,
                        onClick = { onIntent(ArticleSearchIntent.FilterChanged(filter)) },
                        label = { Text(filter.label) }
                    )
                }
            }
            Text(
                "Up to 30 provider-ranked results, not a complete news archive. Date filters apply to these results.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LazyColumn(
                modifier = Modifier.weight(1f).testTag("ArticleSearchResults"),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when {
                    state.isLoading -> item {
                        CircularProgressIndicator(modifier = Modifier.testTag("ArticleSearchLoading"))
                    }
                    state.error != null -> item {
                        Column {
                            Text(state.error, color = MaterialTheme.colorScheme.error)
                            if (state.hasSearched) {
                                TextButton(onClick = { onIntent(ArticleSearchIntent.Retry) }) {
                                    Text("Retry article search")
                                }
                            }
                        }
                    }
                    !state.hasSearched -> item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Enter at least 2 characters, or try a suggested search.")
                            Text("Suggested searches", style = MaterialTheme.typography.titleSmall)
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("Earnings", "Interest rates", "Technology").forEach { topic ->
                                    AssistChip(
                                        onClick = { onIntent(ArticleSearchIntent.QueryChanged(topic)) },
                                        label = { Text(topic) }
                                    )
                                }
                            }
                        }
                    }
                    state.articles.isEmpty() -> item {
                        Text(
                            if (state.dateFilter == ArticleDateFilter.PAST_WEEK)
                                "No matching articles from the past week in the returned results. Try Any time or another search."
                            else "No matching articles found. Try another company or topic."
                        )
                    }
                    else -> {
                        item {
                            Text("${state.articles.size} articles", style = MaterialTheme.typography.labelLarge)
                            Text("Top Hits", style = MaterialTheme.typography.titleLarge)
                        }
                        items(state.articles.take(3), key = { it.id }) { article ->
                            ArticleResultCard(article, state.query, onOpenArticle)
                        }
                        if (state.articles.size > 3) {
                            item { Text("More Results", style = MaterialTheme.typography.titleLarge) }
                            items(state.articles.drop(3), key = { it.id }) { article ->
                                ArticleResultCard(article, state.query, onOpenArticle)
                            }
                        }
                        item {
                            Text(
                                "For informational purposes only, not financial advice.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArticleResultCard(
    article: Article,
    query: String,
    onOpenArticle: (Article) -> Unit
) {
    val date = remember(article.datetime) {
        DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(article.datetime * 1000))
    }
    val highlightStyle = SpanStyle(
        background = MaterialTheme.colorScheme.tertiaryContainer,
        color = MaterialTheme.colorScheme.onTertiaryContainer,
        fontWeight = FontWeight.Bold
    )
    Card(
        onClick = { onOpenArticle(article) },
        modifier = Modifier.fillMaxWidth().testTag("Article_${article.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                highlightArticleText(article.headline, query, highlightStyle),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                highlightArticleText(article.source, query, highlightStyle),
                style = MaterialTheme.typography.labelMedium
            )
            Text(date, style = MaterialTheme.typography.bodySmall)
        }
    }
}

internal fun highlightArticleText(text: String, query: String, style: SpanStyle): AnnotatedString =
    buildAnnotatedString {
        append(text)
        query.trim().split(Regex("\\s+")).filter(String::isNotEmpty).distinct().forEach { term ->
            var start = text.indexOf(term, ignoreCase = true)
            while (start >= 0) {
                addStyle(style, start, start + term.length)
                start = text.indexOf(term, start + term.length, ignoreCase = true)
            }
        }
    }
