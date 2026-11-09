package com.invest.easymoney.data.repository

import com.google.gson.JsonParseException
import com.invest.easymoney.data.api.ArticleApiService
import com.invest.easymoney.domain.model.Article
import com.invest.easymoney.domain.model.NetworkResult
import com.invest.easymoney.domain.repository.ArticleRepository
import java.io.IOException
import java.net.URI
import java.util.Locale
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import javax.inject.Inject

class ArticleRepositoryImpl @Inject constructor(
    private val api: ArticleApiService
) : ArticleRepository {
    override suspend fun searchArticles(query: String): NetworkResult<List<Article>> {
        val normalized = query.trim().replace(Regex("\\s+"), " ")
        if (normalized.length !in 2..200) {
            return NetworkResult.Error(
                IllegalArgumentException("Enter a search query between 2 and 200 characters.")
            )
        }
        return try {
            val response = api.searchArticles(normalized)
            require(response.ok) { "Article search is unavailable. Please try again." }
            val articles = requireNotNull(response.articles) { "Invalid article search response." }
                .map { item ->
                    val dto = requireNotNull(item) { "Invalid article." }
                    val url = requireNotNull(dto.url) { "Missing article URL." }
                    val uri = URI(url)
                    require(uri.scheme?.lowercase(Locale.ROOT) in listOf("http", "https") &&
                        !uri.host.isNullOrBlank() && uri.userInfo == null) {
                        "Invalid article URL."
                    }
                    Article(
                        id = requireNotNull(dto.id).also { require(it.isNotBlank()) },
                        headline = requireNotNull(dto.headline).also { require(it.isNotBlank()) },
                        source = requireNotNull(dto.source).also { require(it.isNotBlank()) },
                        url = url,
                        datetime = requireNotNull(dto.datetime).also { require(it in 0..253402300799L) }
                    )
                }
            NetworkResult.Success(articles.distinctBy { it.id }.distinctBy { it.url })
        } catch (error: CancellationException) {
            throw error
        } catch (error: HttpException) {
            NetworkResult.Error(IOException("Article search is unavailable. Please try again.", error))
        } catch (error: IOException) {
            NetworkResult.Error(IOException("Couldn't connect to article search. Please try again.", error))
        } catch (error: JsonParseException) {
            NetworkResult.Error(IllegalStateException("Invalid article search response.", error))
        } catch (error: IllegalArgumentException) {
            NetworkResult.Error(IllegalStateException("Invalid article search response.", error))
        } catch (error: java.net.URISyntaxException) {
            NetworkResult.Error(IllegalStateException("Invalid article search response.", error))
        }
    }
}
