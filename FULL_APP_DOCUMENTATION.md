# EasyMoney - Complete App Documentation

_Generated on 2026-09-09._

## How the app works (end-to-end)

1. Android app starts from `EasyMoneyApp` and `MainActivity`, initializes DI, theme, and periodic alert checks via WorkManager.
2. UI is built with Jetpack Compose. Navigation routes users through Home, Stock Detail, Watchlist, Alerts, and AI-assisted views.
3. ViewModels orchestrate data fetching and business actions through repositories.
4. Data layer combines remote market/AI APIs and local Room storage for watchlist and alerts.
5. Alert workflows evaluate thresholds in background workers and trigger local notifications.
6. Firebase Cloud Messaging service receives backend push notifications.
7. Node.js backend modules handle insights, watchlist context, retrieval, notifications, and event processing.
8. Background workers on backend and app support asynchronous ingestion and alerting.

## Included source files

- `app/build.gradle.kts`
- `app/src/androidTest/java/com/invest/easymoney/ExampleInstrumentedTest.kt`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/invest/easymoney/data/api/dto/CompanyProfileDto.kt`
- `app/src/main/java/com/invest/easymoney/data/api/dto/IexMostActiveDto.kt`
- `app/src/main/java/com/invest/easymoney/data/api/dto/NewsItemDto.kt`
- `app/src/main/java/com/invest/easymoney/data/api/dto/OpenAiDto.kt`
- `app/src/main/java/com/invest/easymoney/data/api/dto/StockQuoteDto.kt`
- `app/src/main/java/com/invest/easymoney/data/api/FinnhubApiService.kt`
- `app/src/main/java/com/invest/easymoney/data/api/IexApiService.kt`
- `app/src/main/java/com/invest/easymoney/data/api/OpenAiApiService.kt`
- `app/src/main/java/com/invest/easymoney/data/local/AppDatabase.kt`
- `app/src/main/java/com/invest/easymoney/data/local/dao/AlertDao.kt`
- `app/src/main/java/com/invest/easymoney/data/local/dao/WatchlistDao.kt`
- `app/src/main/java/com/invest/easymoney/data/local/entity/AlertEntity.kt`
- `app/src/main/java/com/invest/easymoney/data/local/entity/WatchlistEntity.kt`
- `app/src/main/java/com/invest/easymoney/data/repository/AiInsightRepositoryImpl.kt`
- `app/src/main/java/com/invest/easymoney/data/repository/StockRepositoryImpl.kt`
- `app/src/main/java/com/invest/easymoney/di/AiModule.kt`
- `app/src/main/java/com/invest/easymoney/di/AppModule.kt`
- `app/src/main/java/com/invest/easymoney/di/DatabaseModule.kt`
- `app/src/main/java/com/invest/easymoney/di/NetworkModule.kt`
- `app/src/main/java/com/invest/easymoney/di/OpenAiQualifiers.kt`
- `app/src/main/java/com/invest/easymoney/domain/model/AiPick.kt`
- `app/src/main/java/com/invest/easymoney/domain/model/Alert.kt`
- `app/src/main/java/com/invest/easymoney/domain/model/IntradayPricePoint.kt`
- `app/src/main/java/com/invest/easymoney/domain/model/News.kt`
- `app/src/main/java/com/invest/easymoney/domain/model/Stock.kt`
- `app/src/main/java/com/invest/easymoney/domain/model/StockInsight.kt`
- `app/src/main/java/com/invest/easymoney/domain/model/StockSearchResult.kt`
- `app/src/main/java/com/invest/easymoney/domain/repository/AiInsightRepository.kt`
- `app/src/main/java/com/invest/easymoney/domain/repository/StockRepository.kt`
- `app/src/main/java/com/invest/easymoney/EasyMoneyApp.kt`
- `app/src/main/java/com/invest/easymoney/MainActivity.kt`
- `app/src/main/java/com/invest/easymoney/service/EasyMoneyFCMService.kt`
- `app/src/main/java/com/invest/easymoney/ui/alerts/AlertsScreen.kt`
- `app/src/main/java/com/invest/easymoney/ui/alerts/AlertsViewModel.kt`
- `app/src/main/java/com/invest/easymoney/ui/charts/ChartMapper.kt`
- `app/src/main/java/com/invest/easymoney/ui/charts/ChartMarkerView.kt`
- `app/src/main/java/com/invest/easymoney/ui/charts/IntradayLineChart.kt`
- `app/src/main/java/com/invest/easymoney/ui/charts/MPLineChart.kt`
- `app/src/main/java/com/invest/easymoney/ui/charts/StockChart.kt`
- `app/src/main/java/com/invest/easymoney/ui/detail/StockDetailScreen.kt`
- `app/src/main/java/com/invest/easymoney/ui/detail/StockDetailViewModel.kt`
- `app/src/main/java/com/invest/easymoney/ui/home/HomeScreen.kt`
- `app/src/main/java/com/invest/easymoney/ui/home/HomeViewModel.kt`
- `app/src/main/java/com/invest/easymoney/ui/navigation/AppNavGraph.kt`
- `app/src/main/java/com/invest/easymoney/ui/theme/Color.kt`
- `app/src/main/java/com/invest/easymoney/ui/theme/shapes.kt`
- `app/src/main/java/com/invest/easymoney/ui/theme/Theme.kt`
- `app/src/main/java/com/invest/easymoney/ui/theme/Type.kt`
- `app/src/main/java/com/invest/easymoney/ui/watchlist/WatchlistScreen.kt`
- `app/src/main/java/com/invest/easymoney/ui/watchlist/WatchlistViewModel.kt`
- `app/src/main/java/com/invest/easymoney/util/Constants.kt`
- `app/src/main/java/com/invest/easymoney/util/Resource.kt`
- `app/src/main/java/com/invest/easymoney/worker/AlertCheckWorker.kt`
- `app/src/main/res/drawable/ic_launcher_background.xml`
- `app/src/main/res/drawable/ic_launcher_foreground.xml`
- `app/src/main/res/drawable/ic_money_stack.xml`
- `app/src/main/res/drawable/marker_bg.xml`
- `app/src/main/res/layout/marker_view.xml`
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`
- `app/src/main/res/values-night/themes.xml`
- `app/src/main/res/values/colors.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values/themes.xml`
- `app/src/main/res/xml/backup_rules.xml`
- `app/src/main/res/xml/data_extraction_rules.xml`
- `app/src/test/java/com/invest/easymoney/ExampleUnitTest.kt`
- `app/src/test/java/com/invest/easymoney/ui/charts/ChartMapperTest.kt`
- `BACKEND_COMMANDS.sh`
- `build.gradle.kts`
- `gradle.properties`
- `README.md`
- `server/ai/eventDetector.js`
- `server/ai/eventInsightService.js`
- `server/ai/eventProcessor.js`
- `server/ai/eventRetriever.js`
- `server/ai/eventTypes.js`
- `server/db/schema.sql`
- `server/embeddingService.js`
- `server/eventProcessor.js`
- `server/index.js`
- `server/insightService.js`
- `server/investorTracker.js`
- `server/notificationService.js`
- `server/optimizedRetriever.js`
- `server/package.json`
- `server/portfolioService.js`
- `server/README.md`
- `server/sectorAnalysis.js`
- `server/signalEngine.js`
- `server/simulationEngine.js`
- `server/usersService.js`
- `server/vectorStore.js`
- `server/watchlistPromptBuilder.js`
- `server/watchlistRetriever.js`
- `server/watchlistService.js`
- `server/worker.js`
- `server/worker/eventWorker.js`
- `settings.gradle.kts`
- `start-backend.sh`

## Complete classes/modules and code

### `app/build.gradle.kts`

```kotlin
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.google.services)
}

val localProperties = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(f.inputStream())
}

android {
    namespace = "com.invest.easymoney"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.invest.easymoney"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "OPENAI_API_KEY", "\"${localProperties["OPENAI_API_KEY"] ?: ""}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.compose.animation.core)
    implementation(libs.androidx.compose.foundation)
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.ui)

    // Compose
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.activity.compose)

    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.ext.compiler)

    // Retrofit + OkHttp
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Coroutines
    implementation(libs.coroutines.android)

    // Coil
    implementation(libs.coil.compose)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.analytics)

    // WorkManager
    implementation(libs.work.runtime.ktx)

    // Gson
    implementation(libs.gson)

    // MPAndroidChart - interactive, mature charting library
  //  implementation("com.github.PhilJay:MPAndroidChart:3.1.0")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    debugImplementation(libs.androidx.compose.ui.tooling)


    implementation("com.patrykandpatrick.vico:compose:1.16.1")
    implementation("com.patrykandpatrick.vico:compose-m3:1.16.1")
    implementation("com.patrykandpatrick.vico:core:1.16.1")









}
```

### `app/src/androidTest/java/com/invest/easymoney/ExampleInstrumentedTest.kt`

```kotlin
package com.invest.easymoney

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.invest.easymoney", appContext.packageName)
    }
}
```

### `app/src/main/AndroidManifest.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <application
        android:name="com.invest.easymoney.EasyMoneyApp"
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:networkSecurityConfig="@xml/network_security_config"
        android:theme="@style/Theme.EasyMoney"
        tools:targetApi="36">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.EasyMoney">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!-- Firebase Cloud Messaging -->
        <service
            android:name=".service.EasyMoneyFCMService"
            android:exported="false">
            <intent-filter>
                <action android:name="com.google.firebase.MESSAGING_EVENT" />
            </intent-filter>
        </service>

        <!-- Disable default WorkManager initializer so Hilt can provide custom one -->
        <provider
            android:name="androidx.startup.InitializationProvider"
            android:authorities="${applicationId}.androidx-startup"
            android:exported="false"
            tools:node="merge">
            <meta-data
                android:name="androidx.work.WorkManagerInitializer"
                android:value="androidx.startup"
                tools:node="remove" />
        </provider>

    </application>

</manifest>
```

### `app/src/main/java/com/invest/easymoney/data/api/dto/CompanyProfileDto.kt`

```kotlin
package com.invest.easymoney.data.api.dto

import com.google.gson.annotations.SerializedName

/** Top-level wrapper returned by /v1/finance/search (news + quotes) */
// Renamed to avoid duplicate class name collisions during build
data class YahooSearchResultDto(
    @SerializedName("news") val news: List<YahooNewsItemDto>? = emptyList(),
    @SerializedName("quotes") val quotes: List<YahooQuoteDto>? = emptyList()
)

/** Minimal quote item returned by the search endpoint */
data class YahooQuoteDto(
    @SerializedName("symbol") val symbol: String = "",
    @SerializedName("shortname") val shortName: String = "",
    @SerializedName("longname") val longName: String = "",
    @SerializedName("exchange") val exchange: String = "",
    @SerializedName("quoteType") val quoteType: String = ""
)


data class YahooNewsDto(
    @SerializedName(  "uuid") val uuid: String = "",
    @SerializedName( "title") val title: String = "",
    @SerializedName( "publisher") val publisher: String = "",
@SerializedName("link") val link: String = "",
@SerializedName("providerPublishTime") val publishTime: Long = 0L
)
```

### `app/src/main/java/com/invest/easymoney/data/api/dto/IexMostActiveDto.kt`

```kotlin
package com.invest.easymoney.data.api.dto

import com.google.gson.annotations.SerializedName


data class IexStockDto(
    @SerializedName("symbol") val symbol: String = "",
    @SerializedName("companyName") val companyName: String = "",
    @SerializedName("latestPrice") val latestPrice: Double = 0.0,
    @SerializedName("latestChange") val latestChange: Double = 0.0,
    @SerializedName("latestChangePercent") val latestChangePercent: Double = 0.0
)
```

### `app/src/main/java/com/invest/easymoney/data/api/dto/NewsItemDto.kt`

```kotlin
package com.invest.easymoney.data.api.dto

import com.google.gson.annotations.SerializedName

data class YahooNewsItemDto(
    @SerializedName("uuid") val uuid: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("publisher") val publisher: String = "",
    @SerializedName("link") val link: String = "",
    @SerializedName("providerPublishTime") val publishTime: Long = 0L,
    @SerializedName("type") val type: String = ""
)
```

### `app/src/main/java/com/invest/easymoney/data/api/dto/OpenAiDto.kt`

```kotlin
package com.invest.easymoney.data.api.dto

import com.google.gson.annotations.SerializedName

// ── Request ───────────────────────────────────────────────────────────────────

data class OpenAiRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
    @SerializedName("response_format") val responseFormat: ResponseFormat,
    @SerializedName("max_tokens") val maxTokens: Int = 300,
    val temperature: Double = 0.3
)

data class OpenAiMessage(
    val role: String,
    val content: String
)

data class ResponseFormat(
    val type: String = "json_object"
)

// ── Response ──────────────────────────────────────────────────────────────────

data class OpenAiResponse(
    val choices: List<OpenAiChoice>?
)

data class OpenAiChoice(
    val message: OpenAiMessage?
)

// ── Parsed insight JSON from model content ────────────────────────────────────

data class InsightJson(
    val sentiment: String?,
    val insight: String?,
    @SerializedName("short_term") val shortTerm: String?,
    @SerializedName("long_term") val longTerm: String?
)

// ── Parsed picks JSON from model content ─────────────────────────────────────

data class AiPicksJson(
    val picks: List<AiPickJson>?
)

data class AiPickJson(
    val symbol: String?,
    val reason: String?
)
```

### `app/src/main/java/com/invest/easymoney/data/api/dto/StockQuoteDto.kt`

```kotlin
package com.invest.easymoney.data.api.dto

import com.google.gson.annotations.SerializedName

/** Top-level wrapper returned by /v8/finance/chart/{symbol} */
data class YahooChartResponseDto(
    @SerializedName("chart") val chart: ChartBody?
)

data class ChartBody(
    @SerializedName("result") val result: List<ChartResult>? = emptyList(),
    @SerializedName("error") val error: Any? = null
)

data class ChartResult(
    @SerializedName("meta") val meta: ChartMeta = ChartMeta(),
    @SerializedName("timestamp") val timestamp: List<Long>? = emptyList(),
    @SerializedName("indicators") val indicators: Indicators? = null
)

data class Indicators(
    @SerializedName("quote") val quote: List<QuoteIndicator>? = emptyList()
)

data class QuoteIndicator(
    @SerializedName("close") val close: List<Double?>? = emptyList()
)

data class ChartMeta(
    @SerializedName("symbol") val symbol: String = "",
    @SerializedName("shortName") val shortName: String = "",
    @SerializedName("longName") val longName: String = "",
    @SerializedName("regularMarketPrice") val regularMarketPrice: Double = 0.0,
    @SerializedName("chartPreviousClose") val chartPreviousClose: Double = 0.0,
    @SerializedName("previousClose") val previousClose: Double = 0.0,
    @SerializedName("regularMarketDayHigh") val dayHigh: Double = 0.0,
    @SerializedName("regularMarketDayLow") val dayLow: Double = 0.0,
    @SerializedName("fullExchangeName") val exchange: String = "",
    @SerializedName("currency") val currency: String = ""
)
```

### `app/src/main/java/com/invest/easymoney/data/api/FinnhubApiService.kt`

```kotlin
package com.invest.easymoney.data.api

import com.invest.easymoney.data.api.dto.YahooChartResponseDto
import com.invest.easymoney.data.api.dto.YahooSearchResultDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface YahooFinanceApiService {

    /**
     * Fetch a quote via the CHART endpoint — works WITHOUT a crumb or cookie.
     * The /v8/finance/quote endpoint is now blocked by Yahoo for non-browser
     * clients (returns the "sad panda" HTML page), so we use chart instead.
     * One symbol per request.
     */
    @GET("v8/finance/chart/{symbol}")
    suspend fun getChart(
        @Path("symbol") symbol: String,
        @Query("range") range: String = "1d",
        @Query("interval") interval: String = "1m"
    ): YahooChartResponseDto

    /**
     * Fetch latest news for a symbol via the search endpoint.
     * quotesCount=0 skips quote results so we only get news.
     */
    @GET("v1/finance/search")
    suspend fun searchNews(
        @Query("q") symbol: String,
        @Query("quotesCount") quotesCount: Int = 0,
        @Query("newsCount") newsCount: Int = 10,
        @Query("enableFuzzyQuery") enableFuzzyQuery: Boolean = false
    ): YahooSearchResultDto

    @GET("v1/finance/search")
    suspend fun searchSymbols(
        @Query("q") query: String = "stocks",
        @Query("quotesCount") quotesCount: Int = 100,
        @Query("newsCount") newsCount: Int = 0,
        @Query("enableFuzzyQuery") enableFuzzyQuery: Boolean = true
    ): YahooSearchResultDto


    }
```

### `app/src/main/java/com/invest/easymoney/data/api/IexApiService.kt`

```kotlin
package com.invest.easymoney.data.api

import com.invest.easymoney.data.api.dto.IexStockDto
import retrofit2.http.GET
import retrofit2.http.Query

interface IexApiService {

    @GET("stock/market/list/mostactive")
    suspend fun getMostActiveStocks(
        @Query("token") token: String
    ): List<IexStockDto>
}
```

### `app/src/main/java/com/invest/easymoney/data/api/OpenAiApiService.kt`

```kotlin
package com.invest.easymoney.data.api

import com.invest.easymoney.data.api.dto.OpenAiRequest
import com.invest.easymoney.data.api.dto.OpenAiResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface OpenAiApiService {

    // Pollinations.ai exposes an OpenAI-compatible endpoint at /openai (no key)
    @POST("openai")
    suspend fun chatCompletion(
        @Body request: OpenAiRequest
    ): OpenAiResponse
}
```

### `app/src/main/java/com/invest/easymoney/data/local/AppDatabase.kt`

```kotlin
package com.invest.easymoney.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.invest.easymoney.data.local.dao.AlertDao
import com.invest.easymoney.data.local.dao.WatchlistDao
import com.invest.easymoney.data.local.entity.AlertEntity
import com.invest.easymoney.data.local.entity.WatchlistEntity

@Database(
    entities = [WatchlistEntity::class, AlertEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun watchlistDao(): WatchlistDao
    abstract fun alertDao(): AlertDao
}
```

### `app/src/main/java/com/invest/easymoney/data/local/dao/AlertDao.kt`

```kotlin
package com.invest.easymoney.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.invest.easymoney.data.local.entity.AlertEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertDao {

    @Query("SELECT * FROM alerts")
    fun getAll(): Flow<List<AlertEntity>>

    @Query("SELECT * FROM alerts")
    suspend fun getAllOnce(): List<AlertEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AlertEntity)

    @Query("DELETE FROM alerts WHERE id = :id")
    suspend fun delete(id: Int)
}
```

### `app/src/main/java/com/invest/easymoney/data/local/dao/WatchlistDao.kt`

```kotlin
package com.invest.easymoney.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.invest.easymoney.data.local.entity.WatchlistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchlistDao {

    @Query("SELECT * FROM watchlist")
    fun getAll(): Flow<List<WatchlistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: WatchlistEntity)

    @Query("DELETE FROM watchlist WHERE symbol = :symbol")
    suspend fun delete(symbol: String)

    @Query("SELECT COUNT(*) FROM watchlist WHERE symbol = :symbol")
    suspend fun count(symbol: String): Int
}
```

### `app/src/main/java/com/invest/easymoney/data/local/entity/AlertEntity.kt`

```kotlin
package com.invest.easymoney.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val symbol: String,
    val percentage: Float,
    val type: String  // "INCREASE" or "DECREASE"
)
```

### `app/src/main/java/com/invest/easymoney/data/local/entity/WatchlistEntity.kt`

```kotlin
package com.invest.easymoney.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watchlist")
data class WatchlistEntity(
    @PrimaryKey
    val symbol: String
)
```

### `app/src/main/java/com/invest/easymoney/data/repository/AiInsightRepositoryImpl.kt`

```kotlin
package com.invest.easymoney.data.repository

import com.google.gson.Gson
import com.invest.easymoney.data.api.OpenAiApiService
import com.invest.easymoney.data.api.dto.AiPicksJson
import com.invest.easymoney.data.api.dto.InsightJson
import com.invest.easymoney.data.api.dto.OpenAiMessage
import com.invest.easymoney.data.api.dto.OpenAiRequest
import com.invest.easymoney.data.api.dto.ResponseFormat
import com.invest.easymoney.domain.model.AiPick
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
    ): Resource<StockInsight> {
        cacheMutex.withLock {
            val cached = insightCache[symbol]
            if (cached != null && System.currentTimeMillis() - cached.second < CACHE_TTL_MS) {
                return Resource.Success(cached.first)
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
                ?: return Resource.Error("Empty response from AI")

            val parsed = parseInsight(content)
                ?: return Resource.Error("Failed to parse AI response")

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

            Resource.Success(insight)
        }.getOrElse { e ->
            if (isNetworkError(e)) {
                Resource.Success(buildFallbackInsight(symbol, stock, news))
            } else {
                Resource.Error(mapApiError(e))
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
```

### `app/src/main/java/com/invest/easymoney/data/repository/StockRepositoryImpl.kt`

```kotlin
package com.invest.easymoney.data.repository

import android.util.Log
import com.invest.easymoney.data.api.YahooFinanceApiService
import com.invest.easymoney.data.local.dao.AlertDao
import com.invest.easymoney.data.local.dao.WatchlistDao
import com.invest.easymoney.data.local.entity.AlertEntity
import com.invest.easymoney.data.local.entity.WatchlistEntity
import com.invest.easymoney.domain.model.Alert
import com.invest.easymoney.domain.model.AlertType
import com.invest.easymoney.domain.model.News
import com.invest.easymoney.domain.model.Stock
import com.invest.easymoney.domain.model.IntradayPricePoint
import com.invest.easymoney.domain.repository.StockRepository
import com.invest.easymoney.util.Constants
import com.invest.easymoney.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import com.invest.easymoney.data.api.IexApiService

@Singleton
class StockRepositoryImpl @Inject constructor(
    private val iexApi: IexApiService,
    private val api: YahooFinanceApiService,
    private val watchlistDao: WatchlistDao,
    private val alertDao: AlertDao
) : StockRepository {

    // ── Stocks ────────────────────────────────────────────────────────────────



    override suspend fun getStockDetail(symbol: String): Resource<Stock> = runCatching {
        val result = api.getChart(symbol).chart?.result?.firstOrNull()
            ?: return Resource.Error("No data for $symbol")
        val meta = result.meta
        // Build intraday points from timestamps + indicators
        val timestamps = result.timestamp ?: emptyList()
        val closes = result.indicators?.quote?.firstOrNull()?.close ?: emptyList()
        val points = mutableListOf<IntradayPricePoint>()
        val size = minOf(timestamps.size, closes.size)
        for (i in 0 until size) {
            val ts = timestamps[i]
            val c = closes[i]
            if (c != null) points.add(IntradayPricePoint(time = ts.toString(), price = c.toFloat()))
        }
        Log.d("StockRepository", "getStockDetail: $symbol -> points=${points.size}, timestamps=${timestamps.size}, closes=${closes.size}")
        val stock = meta.toStock().copy(intradayPrices = points)
        Resource.Success(stock)
    }.getOrElse { e -> Resource.Error(e.message ?: "Failed to fetch $symbol") }

    override suspend fun fetchStocksForSymbols(symbols: List<String>): Resource<List<Stock>> {
        if (symbols.isEmpty()) return Resource.Success(emptyList())
        return runCatching {
            Resource.Success(fetchCharts(symbols))
        }.getOrElse { e -> Resource.Error(e.message ?: "Failed to fetch stocks") }
    }

    /** Fetches each symbol's chart concurrently and maps to [Stock]. */
    private suspend fun fetchCharts(symbols: List<String>): List<Stock> = coroutineScope {
        symbols.map { symbol ->
            async {
                runCatching {
                        api.getChart(symbol).chart?.result?.firstOrNull()?.let { res ->
                        val meta = res.meta
                        val timestamps = res.timestamp ?: emptyList()
                        val closes = res.indicators?.quote?.firstOrNull()?.close ?: emptyList()
                        val points = mutableListOf<IntradayPricePoint>()
                        val size = minOf(timestamps.size, closes.size)
                        for (i in 0 until size) {
                            val ts = timestamps[i]
                            val c = closes[i]
                            if (c != null) points.add(IntradayPricePoint(time = ts.toString(), price = c.toFloat()))
                        }
                        meta.toStock().copy(intradayPrices = points)
                    }
                }.getOrNull()
            }
        }.awaitAll().filterNotNull()
    }

    override suspend fun getTopStocks(): Resource<List<Stock>> = runCatching {
        val popularSymbols = when (val result = getPopularStocksFromApi()) {
            is Resource.Success -> result.data ?: emptyList()
            is Resource.Error -> Constants.POPULAR_STOCKS
            is Resource.Loading -> emptyList()
        }

        val stocks = fetchCharts(popularSymbols)
            .filter { it.currentPrice > 0 }

        Resource.Success(stocks)
    }.getOrElse { e -> Resource.Error(e.message ?: "Failed to fetch stocks") }


    override suspend fun getNews(symbol: String): Resource<List<News>> = runCatching {
        val response = api.searchNews(symbol)
        val news = response.news
            ?.filter { it.title.isNotEmpty() }
            ?.map { dto ->
                News(
                    id = dto.uuid.hashCode().toLong(),
                    headline = dto.title,
                    source = dto.publisher,
                    url = dto.link,
                    summary = "",
                    imageUrl = "",
                    datetime = dto.publishTime
                )
            } ?: emptyList()
        Resource.Success(news)
    }.getOrElse { e -> Resource.Error(e.message ?: "Failed to fetch news") }

    // Search endpoint — map quotes to lightweight domain model
    override suspend fun searchSymbols(query: String): Resource<List<com.invest.easymoney.domain.model.StockSearchResult>> = runCatching {
        val resp = api.searchNews(query, quotesCount = 10, newsCount = 0, enableFuzzyQuery = true)
        val quotes = resp.quotes ?: emptyList()
        val results = quotes.map { q ->
            com.invest.easymoney.domain.model.StockSearchResult(
                symbol = q.symbol,
                name = if (q.longName.isNotEmpty()) q.longName else q.shortName.ifEmpty { q.symbol },
                exchange = q.exchange
            )
        }
        Resource.Success(results)
    }.getOrElse { e -> Resource.Error(e.message ?: "Search failed") }

    // ── Watchlist ─────────────────────────────────────────────────────────────

    override fun getWatchlistSymbols(): Flow<List<String>> =
        watchlistDao.getAll().map { list -> list.map { it.symbol } }

    override suspend fun addToWatchlist(symbol: String) =
        watchlistDao.insert(WatchlistEntity(symbol = symbol))

    override suspend fun removeFromWatchlist(symbol: String) =
        watchlistDao.delete(symbol)

    override suspend fun isInWatchlist(symbol: String): Boolean =
        watchlistDao.count(symbol) > 0

    // ── Alerts ────────────────────────────────────────────────────────────────

    override fun getAlerts(): Flow<List<Alert>> =
        alertDao.getAll().map { list -> list.map { it.toDomain() } }

    override suspend fun addAlert(alert: Alert) =
        alertDao.insert(AlertEntity(symbol = alert.symbol, percentage = alert.percentage, type = alert.type.name))

    override suspend fun deleteAlert(alertId: Int) = alertDao.delete(alertId)

    override suspend fun getAllAlertsOnce(): List<Alert> =
        alertDao.getAllOnce().map { it.toDomain() }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private fun com.invest.easymoney.data.api.dto.ChartMeta.toStock(): Stock {
        val prevClose = if (chartPreviousClose > 0) chartPreviousClose else previousClose
        val change = if (prevClose > 0) regularMarketPrice - prevClose else 0.0
        val changePercent = if (prevClose > 0) change / prevClose * 100 else 0.0
        return Stock(
            symbol = symbol,
            name = shortName.ifEmpty { longName.ifEmpty { symbol } },
            currentPrice = regularMarketPrice,
            change = change,
            changePercent = changePercent,
            highPrice = dayHigh,
            lowPrice = dayLow,
            openPrice = prevClose,
            previousClose = prevClose,
            marketCap = 0.0,
            exchange = exchange,
            industry = ""
        )
    }

    private fun AlertEntity.toDomain() = Alert(
        id = id,
        symbol = symbol,
        percentage = percentage,
        type = if (type == "INCREASE") AlertType.INCREASE else AlertType.DECREASE
    )


    override suspend fun getPopularStocksFromApi(): Resource<List<String>> = try {
        val response = iexApi.getMostActiveStocks(Constants.IEX_API_TOKEN)
        val symbols = response.mapNotNull { it.symbol.trim().ifEmpty { null } }.distinct().take(30)
        if (symbols.isNotEmpty()) Resource.Success(symbols)
        else Resource.Error("No symbols from IEX")
    } catch (e: Exception) {
        Resource.Success(Constants.POPULAR_STOCKS)
    }
    }
```

### `app/src/main/java/com/invest/easymoney/di/AiModule.kt`

```kotlin
package com.invest.easymoney.di

import com.google.gson.Gson
import com.invest.easymoney.data.api.OpenAiApiService
import com.invest.easymoney.data.repository.AiInsightRepositoryImpl
import com.invest.easymoney.domain.repository.AiInsightRepository
import com.invest.easymoney.util.Constants
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AiModule {

    @Binds
    @Singleton
    abstract fun bindAiInsightRepository(impl: AiInsightRepositoryImpl): AiInsightRepository

    companion object {

        @Provides
        @Singleton
        @OpenAiClient
        fun provideOpenAiOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                // Pollinations.ai requires no API key
                val request = chain.request().newBuilder()
                    .header("Content-Type", "application/json")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS) // LLM can be slow
            .build()

        @Provides
        @Singleton
        @OpenAiRetrofit
        fun provideOpenAiRetrofit(@OpenAiClient client: OkHttpClient, gson: Gson): Retrofit = Retrofit.Builder()
            .baseUrl(Constants.OPENAI_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        @Provides
        @Singleton
        fun provideOpenAiApiService(@OpenAiRetrofit retrofit: Retrofit): OpenAiApiService =
            retrofit.create(OpenAiApiService::class.java)
    }
}
```

### `app/src/main/java/com/invest/easymoney/di/AppModule.kt`

```kotlin
package com.invest.easymoney.di

import com.invest.easymoney.data.repository.StockRepositoryImpl
import com.invest.easymoney.domain.repository.StockRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindStockRepository(impl: StockRepositoryImpl): StockRepository
}
```

### `app/src/main/java/com/invest/easymoney/di/DatabaseModule.kt`

```kotlin
package com.invest.easymoney.di

import android.content.Context
import androidx.room.Room
import com.invest.easymoney.data.local.AppDatabase
import com.invest.easymoney.data.local.dao.AlertDao
import com.invest.easymoney.data.local.dao.WatchlistDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "easymoney.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideWatchlistDao(db: AppDatabase): WatchlistDao = db.watchlistDao()

    @Provides
    fun provideAlertDao(db: AppDatabase): AlertDao = db.alertDao()
}
```

### `app/src/main/java/com/invest/easymoney/di/NetworkModule.kt`

```kotlin
package com.invest.easymoney.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.invest.easymoney.data.api.IexApiService
import com.invest.easymoney.data.api.YahooFinanceApiService
import com.invest.easymoney.util.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class YahooRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IexRetrofit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder().create()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @YahooRetrofit
    fun provideYahooRetrofit(
        okHttpClient: OkHttpClient,
        gson: Gson
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    @IexRetrofit
    fun provideIexRetrofit(
        okHttpClient: OkHttpClient,
        gson: Gson
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.IEX_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideYahooFinanceApiService(
        @YahooRetrofit retrofit: Retrofit
    ): YahooFinanceApiService {
        return retrofit.create(YahooFinanceApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideIexApiService(
        @IexRetrofit retrofit: Retrofit
    ): IexApiService {
        return retrofit.create(IexApiService::class.java)
    }
}
```

### `app/src/main/java/com/invest/easymoney/di/OpenAiQualifiers.kt`

```kotlin
package com.invest.easymoney.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class OpenAiClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class OpenAiRetrofit
```

### `app/src/main/java/com/invest/easymoney/domain/model/AiPick.kt`

```kotlin
package com.invest.easymoney.domain.model

data class AiPick(
    val symbol: String,
    val reason: String
)
```

### `app/src/main/java/com/invest/easymoney/domain/model/Alert.kt`

```kotlin
package com.invest.easymoney.domain.model

data class Alert(
    val id: Int = 0,
    val symbol: String,
    val percentage: Float,
    val type: AlertType
)

enum class AlertType {
    INCREASE, DECREASE
}
```

### `app/src/main/java/com/invest/easymoney/domain/model/IntradayPricePoint.kt`

```kotlin
package com.invest.easymoney.domain.model

data class IntradayPricePoint(
    val time: String,
    val price: Float
)
```

### `app/src/main/java/com/invest/easymoney/domain/model/News.kt`

```kotlin
package com.invest.easymoney.domain.model

data class News(
    val id: Long,
    val headline: String,
    val source: String,
    val url: String,
    val summary: String,
    val imageUrl: String,
    val datetime: Long
)
```

### `app/src/main/java/com/invest/easymoney/domain/model/Stock.kt`

```kotlin
package com.invest.easymoney.domain.model

data class Stock(
    val symbol: String,
    val name: String = "",
    val currentPrice: Double,
    val change: Double,
    val changePercent: Double,
    val highPrice: Double = 0.0,
    val lowPrice: Double = 0.0,
    val openPrice: Double = 0.0,
    val previousClose: Double = 0.0,
    val marketCap: Double = 0.0,
    val logoUrl: String = "",
    val exchange: String = "",
    val industry: String = "",
    val intradayPrices: List<IntradayPricePoint> = emptyList()
)
```

### `app/src/main/java/com/invest/easymoney/domain/model/StockInsight.kt`

```kotlin
package com.invest.easymoney.domain.model

data class StockInsight(
    val symbol: String,
    /** "bullish", "bearish", or "neutral" */
    val sentiment: String,
    val insight: String,
    val shortTermOutlook: String,
    val longTermOutlook: String
)
```

### `app/src/main/java/com/invest/easymoney/domain/model/StockSearchResult.kt`

```kotlin
package com.invest.easymoney.domain.model

/** Lightweight model for search results */
data class StockSearchResult(
    val symbol: String,
    val name: String,
    val exchange: String
)
```

### `app/src/main/java/com/invest/easymoney/domain/repository/AiInsightRepository.kt`

```kotlin
package com.invest.easymoney.domain.repository

import com.invest.easymoney.domain.model.AiPick
import com.invest.easymoney.domain.model.News
import com.invest.easymoney.domain.model.Stock
import com.invest.easymoney.domain.model.StockInsight
import com.invest.easymoney.util.Resource

interface AiInsightRepository {

    /**
     * Returns an AI-generated insight for the given stock.
     * Results are cached for 30 minutes to avoid repeated API calls.
     */
    suspend fun getStockInsight(
        symbol: String,
        stock: Stock,
        news: List<News>
    ): Resource<StockInsight>

    /**
     * Ranks the given stocks and returns the top AI picks of the day.
     * Results are cached for 30 minutes.
     */
    suspend fun getAiPicks(stocks: List<Stock>): Resource<List<AiPick>>
}
```

### `app/src/main/java/com/invest/easymoney/domain/repository/StockRepository.kt`

```kotlin
package com.invest.easymoney.domain.repository

import com.invest.easymoney.domain.model.Alert
import com.invest.easymoney.domain.model.News
import com.invest.easymoney.domain.model.Stock
import com.invest.easymoney.domain.model.StockSearchResult
import com.invest.easymoney.util.Resource
import kotlinx.coroutines.flow.Flow

interface StockRepository {
    suspend fun getTopStocks(): Resource<List<Stock>>
    suspend fun getStockDetail(symbol: String): Resource<Stock>
    suspend fun getNews(symbol: String): Resource<List<News>>
    suspend fun fetchStocksForSymbols(symbols: List<String>): Resource<List<Stock>>
    suspend fun searchSymbols(query: String): Resource<List<StockSearchResult>>

    suspend fun getPopularStocksFromApi(): Resource<List<String>>
    // Watchlist
    fun getWatchlistSymbols(): Flow<List<String>>
    suspend fun addToWatchlist(symbol: String)
    suspend fun removeFromWatchlist(symbol: String)
    suspend fun isInWatchlist(symbol: String): Boolean

    // Alerts
    fun getAlerts(): Flow<List<Alert>>
    suspend fun addAlert(alert: Alert)
    suspend fun deleteAlert(alertId: Int)
    suspend fun getAllAlertsOnce(): List<Alert>
}
```

### `app/src/main/java/com/invest/easymoney/EasyMoneyApp.kt`

```kotlin
package com.invest.easymoney

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.invest.easymoney.util.Constants
import com.invest.easymoney.worker.AlertCheckWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class EasyMoneyApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        scheduleAlertChecks()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                Constants.NOTIFICATION_CHANNEL_ID,
                Constants.NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for stock price alerts"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun scheduleAlertChecks() {
        val request = PeriodicWorkRequestBuilder<AlertCheckWorker>(15, TimeUnit.MINUTES)
            .addTag(Constants.ALERT_WORK_TAG)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            Constants.ALERT_WORK_TAG,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
```

### `app/src/main/java/com/invest/easymoney/MainActivity.kt`

```kotlin
package com.invest.easymoney

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.invest.easymoney.ui.navigation.AppNavGraph
import com.invest.easymoney.ui.theme.EasyMoneyTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Permission result handled silently */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()
        setContent {
            EasyMoneyTheme {
                AppNavGraph()
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
```

### `app/src/main/java/com/invest/easymoney/service/EasyMoneyFCMService.kt`

```kotlin
package com.invest.easymoney.service

import android.app.NotificationManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.invest.easymoney.R
import com.invest.easymoney.util.Constants

class EasyMoneyFCMService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        Log.d(TAG, "FCM token refreshed: $token")
        // TODO: Send token to your backend server for targeted push notifications
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Log.d(TAG, "FCM message received from: ${message.from}")
        val title = message.notification?.title ?: message.data["title"] ?: "EasyMoney Alert"
        val body = message.notification?.body ?: message.data["body"] ?: ""
        if (body.isNotEmpty()) showNotification(title, body)
    }

    private fun showNotification(title: String, body: String) {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val notification = NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    companion object {
        private const val TAG = "EasyMoneyFCM"
    }
}
```

### `app/src/main/java/com/invest/easymoney/ui/alerts/AlertsScreen.kt`

```kotlin
package com.invest.easymoney.ui.alerts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.invest.easymoney.domain.model.Alert
import com.invest.easymoney.domain.model.AlertType
import com.invest.easymoney.ui.theme.GainGreen
import com.invest.easymoney.ui.theme.LossRed
import com.invest.easymoney.util.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(viewModel: AlertsViewModel = hiltViewModel()) {
    val state by viewModel.alertsState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Alerts", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        when (state) {
            is Resource.Loading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            is Resource.Error -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { Text("⚠️ Failed to load alerts") }

            is Resource.Success -> {
                val alerts = (state as Resource.Success<List<Alert>>).data
                if (alerts.isEmpty()) {
                    Box(
                        Modifier.fillMaxSize().padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.NotificationsActive,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "No alerts set",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Open a stock detail and tap 🔔\nto set a price alert",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                "Active Alerts",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        items(alerts, key = { it.id }) { alert ->
                            AlertItem(
                                alert = alert,
                                onDelete = { viewModel.deleteAlert(alert.id) }
                            )
                        }
                        item {
                            Spacer(Modifier.height(8.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Alerts trigger based on daily % change.\nChecked automatically every 15 minutes.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertItem(alert: Alert, onDelete: () -> Unit) {
    val isIncrease = alert.type == AlertType.INCREASE
    val accentColor = if (isIncrease) GainGreen else LossRed
    val arrow = if (isIncrease) "📈" else "📉"
    val direction = if (isIncrease) "rises" else "drops"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(arrow, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = alert.symbol,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Notify when $direction ${String.format("%.1f", alert.percentage)}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = accentColor
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete Alert",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
```

### `app/src/main/java/com/invest/easymoney/ui/alerts/AlertsViewModel.kt`

```kotlin
package com.invest.easymoney.ui.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.invest.easymoney.domain.model.Alert
import com.invest.easymoney.domain.repository.StockRepository
import com.invest.easymoney.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val repository: StockRepository
) : ViewModel() {

    val alertsState: StateFlow<Resource<List<Alert>>> = repository.getAlerts()
        .map { Resource.Success(it) as Resource<List<Alert>> }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Resource.Loading)

    fun deleteAlert(alertId: Int) {
        viewModelScope.launch {
            repository.deleteAlert(alertId)
        }
    }
}
```

### `app/src/main/java/com/invest/easymoney/ui/charts/ChartMapper.kt`

```kotlin
package com.invest.easymoney.ui.charts

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Small mapper utilities for charting.
 */
object ChartMapper {
    private val utcFmt = SimpleDateFormat("HH:mm", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }

    fun toLabelList(times: List<Long>? = null, size: Int = 0): List<String> {
        if (times == null || times.size != size || times.isEmpty()) return List(size) { it.toString() }
        return times.map { ts ->
            utcFmt.format(Date(ts * 1000))
        }
    }
}
```

### `app/src/main/java/com/invest/easymoney/ui/charts/ChartMarkerView.kt`

```kotlin
/*
package com.invest.easymoney.ui.charts

import android.content.Context
import android.view.View
import android.widget.TextView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.ColorTemplate
import com.github.mikephil.charting.utils.Utils
import com.invest.easymoney.R

class ChartMarkerView(context: Context) : com.github.mikephil.charting.components.MarkerView(context, R.layout.marker_view) {
    private val priceText: TextView = findViewById(R.id.marker_price)
    private val timeText: TextView = findViewById(R.id.marker_time)
    private val volumeText: TextView = findViewById(R.id.marker_volume)
    private val sparkline: LineChart = findViewById(R.id.marker_sparkline)

    private var pricesSeries: List<Float> = emptyList()
    private var volumesSeries: List<Long>? = null

    init {
        // initialize the small chart
        sparkline.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        sparkline.setTouchEnabled(false)
        sparkline.setDrawGridBackground(false)
        sparkline.axisLeft.isEnabled = false
        sparkline.axisRight.isEnabled = false
        sparkline.xAxis.isEnabled = false
        val d = Description()
        d.text = ""
        sparkline.description = d
        sparkline.legend.isEnabled = false
    }

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        e?.let {
            priceText.text = String.format("%.2f", e.y)
            // x is index; time label should be set from outside by caller
            updateSparklineHighlight(e.x.toInt())
        }
        super.refreshContent(e, highlight)
    }

    fun setTimeLabel(label: String) {
        timeText.text = label
    }

    fun setSeries(prices: List<Float>, volumes: List<Long>? = null) {
        this.pricesSeries = prices
        this.volumesSeries = volumes
        // draw sparkline data
        updateSparkline(0)
    }

    private fun updateSparkline(highlightIndex: Int) {
        if (pricesSeries.isEmpty()) {
            sparkline.clear()
            sparkline.invalidate()
            volumeText.text = "Vol: N/A"
            return
        }
        val windowSize = 12 // show last 12 points around highlight when possible
        val size = pricesSeries.size
        val center = highlightIndex.coerceIn(0, size - 1)
        val from = (center - windowSize / 2).coerceAtLeast(0)
        val to = (from + windowSize).coerceAtMost(size)
        val sub = pricesSeries.subList(from, to)
        val entries = sub.mapIndexed { i, p -> Entry(i.toFloat(), p) }
        val set = LineDataSet(entries, "s").apply {
            color = ColorTemplate.getHoloBlue()
            setDrawValues(false)
            setDrawCircles(false)
            lineWidth = 1.2f
            mode = LineDataSet.Mode.LINEAR
            setDrawFilled(true)
            fillAlpha = 60
            fillColor = ColorTemplate.getHoloBlue()
        }
        sparkline.data = LineData(set)
        sparkline.invalidate()

        // update volume label for the highlighted index if available
        volumesSeries?.let { vols ->
            val volIdx = (from + (center - from)).coerceIn(0, vols.size - 1)
            volumeText.text = "Vol: ${formatVolume(vols[volIdx])}"
        } ?: run { volumeText.text = "Vol: N/A" }
    }

    private fun updateSparklineHighlight(index: Int) {
        // refresh sparkline window around this index
        updateSparkline(index)
    }

    private fun formatVolume(v: Long): String {
        return when {
            v >= 1_000_000_000 -> String.format("%.1fB", v / 1_000_000_000.0)
            v >= 1_000_000 -> String.format("%.1fM", v / 1_000_000.0)
            v >= 1_000 -> String.format("%.1fk", v / 1_000.0)
            else -> v.toString()
        }
    }
}
*/
```

### `app/src/main/java/com/invest/easymoney/ui/charts/IntradayLineChart.kt`

```kotlin
package com.invest.easymoney.ui.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.coroutineScope
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Interactive intraday line chart with gradient fill/stroke and touch tooltip.
 * - prices: list of Float
 * - times: optional list of epoch seconds
 */
@Composable
fun IntradayLineChart(
    prices: List<Float>,
    times: List<Long>? = null,
    modifier: Modifier = Modifier
) {
    if (prices.isEmpty()) {
        Box(
            modifier = modifier
                .height(200.dp)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(text = "No intraday data", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        return
    }

    val values = prices.map { it.toDouble() }
    val minY = values.minOrNull() ?: 0.0
    val maxY = values.maxOrNull() ?: minY + 1.0
    val range = if ((maxY - minY) == 0.0) 1.0 else (maxY - minY)
    val lastValue = values.last()

    val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())

    // Interaction state
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var tooltipIndex by remember { mutableStateOf<Int?>(null) }
    var tooltipPx by remember { mutableStateOf(0f) }
    var tooltipPy by remember { mutableStateOf(0f) }

    Box(modifier = modifier.fillMaxWidth()) {
        // Pointer input needs access to canvas size, so attach to this Box/Canvas
        Canvas(modifier = Modifier
            .height(260.dp)
            .fillMaxWidth()
            .onSizeChanged { canvasSize = it }
            .pointerInput(values, canvasSize) {
                if (canvasSize.width == 0) return@pointerInput
                coroutineScope {
                    detectDragGestures(onDragStart = { offset ->
                        // map to index
                        val paddingLeft = 12f
                        val paddingRight = 12f
                        val chartW = canvasSize.width.toFloat() - paddingLeft - paddingRight
                        val count = values.size
                        val xStep = if (count > 1) chartW / (count - 1) else chartW
                        val relativeX = (offset.x - paddingLeft).coerceIn(0f, chartW)
                        val idx = (relativeX / xStep).roundToInt().coerceIn(0, count - 1)
                        tooltipIndex = idx
                        tooltipPx = offset.x
                        tooltipPy = offset.y
                    }, onDrag = { change, _ ->
                        val offset = change.position
                        val paddingLeft = 12f
                        val paddingRight = 12f
                        val chartW = canvasSize.width.toFloat() - paddingLeft - paddingRight
                        val count = values.size
                        val xStep = if (count > 1) chartW / (count - 1) else chartW
                        val relativeX = (offset.x - paddingLeft).coerceIn(0f, chartW)
                        val idx = (relativeX / xStep).roundToInt().coerceIn(0, count - 1)
                        tooltipIndex = idx
                        tooltipPx = offset.x
                        tooltipPy = offset.y
                    }, onDragEnd = {
                        // keep tooltip visible short time? For now keep until next interaction
                    }, onDragCancel = {
                        // clear on cancel
                        tooltipIndex = null
                    })
                }
            }
        ) {
            val w = size.width
            val h = size.height
            val paddingLeft = 12f
            val paddingRight = 12f
            val paddingTop = 16f
            val paddingBottom = 32f
            val chartW = w - paddingLeft - paddingRight
            val chartH = h - paddingTop - paddingBottom

            val count = values.size
            val xStep = if (count > 1) chartW / (count - 1) else chartW

            // grid
            val gridColor = Color(0x15000000)
            val gridLines = 3
            for (i in 0..gridLines) {
                val y = paddingTop + chartH * i / gridLines
                drawLine(color = gridColor, start = Offset(paddingLeft, y), end = Offset(w - paddingRight, y), strokeWidth = 1f)
            }

            // path
            val linePath = Path()
            for (i in values.indices) {
                val v = values[i]
                val x = paddingLeft + i * xStep
                val y = paddingTop + (chartH - (((v - minY) / range) * chartH)).toFloat()
                if (i == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
            }

            // gradient fill
            val fillPath = Path().apply { addPath(linePath) }
            fillPath.lineTo(w - paddingRight, paddingTop + chartH)
            fillPath.lineTo(paddingLeft, paddingTop + chartH)
            fillPath.close()
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0x803297FF), Color.Transparent),
                    startY = paddingTop,
                    endY = paddingTop + chartH
                )
            )

            // gradient stroke for the line
            drawPath(
                path = linePath,
                brush = Brush.horizontalGradient(listOf(Color(0xFF00C6A7), Color(0xFF2979FF))),
                style = Stroke(width = 3f, cap = StrokeCap.Round)
            )

            // last marker
            val lastX = paddingLeft + (values.size - 1) * xStep
            val lastY = paddingTop + (chartH - (((lastValue - minY) / range) * chartH)).toFloat()
            drawCircle(color = Color(0xFF2979FF), radius = 6f, center = Offset(lastX, lastY))

            // if tooltipIndex present, draw a vertical indicator line
            tooltipIndex?.let { idx ->
                val px = paddingLeft + idx * xStep
                drawLine(color = Color(0xAA444444), start = Offset(px, paddingTop), end = Offset(px, paddingTop + chartH), strokeWidth = 1f)
            }
        }

        // Overlay top-left current price and top-right max.
        Text(
            text = String.format(Locale.getDefault(), "%.2f", lastValue),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 6.dp),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = String.format(Locale.getDefault(), "%.2f", maxY),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 16.dp, top = 6.dp),
            fontSize = 12.sp,
            color = Color.DarkGray
        )

        Text(
            text = String.format(Locale.getDefault(), "%.2f", minY),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 36.dp),
            fontSize = 12.sp,
            color = Color.DarkGray
        )

        // Tooltip composable overlay
        tooltipIndex?.let { idx ->
            val price = values[idx]
            val timeLabel = times?.getOrNull(idx)?.let { timeFmt.format(Date(it * 1000)) } ?: ""
            // Determine tooltip position in Dp using canvasSize & tooltipIndex
            val paddingLeftPx = 12f
            val paddingRightPx = 12f
            val chartWpx = (canvasSize.width - paddingLeftPx - paddingRightPx).coerceAtLeast(1f)
            val xStep = if (values.size > 1) chartWpx / (values.size - 1) else chartWpx
            val px = paddingLeftPx + idx * xStep
            // position tooltip above chart
            val tooltipOffsetDpX = with(LocalDensity.current) { (px - 60f).toDp() }
            val tooltipOffsetDpY = 40.dp

            Surface(
                tonalElevation = 4.dp,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .offset(x = tooltipOffsetDpX, y = tooltipOffsetDpY)
                    .width(120.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(text = String.format(Locale.getDefault(), "%.2f", price), fontWeight = FontWeight.Bold)
                    if (timeLabel.isNotEmpty()) Text(text = timeLabel, fontSize = 12.sp, color = Color.DarkGray)
                }
            }
        }

        // Time labels row at bottom
        Row(modifier = Modifier
            .align(Alignment.BottomStart)
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, bottom = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            if (times != null && times.size == prices.size && times.isNotEmpty()) {
                Text(text = timeFmt.format(Date(times.first() * 1000)), color = Color.DarkGray, fontSize = 12.sp)
                Text(text = timeFmt.format(Date(times[times.size / 2] * 1000)), color = Color.DarkGray, fontSize = 12.sp)
                Text(text = timeFmt.format(Date(times.last() * 1000)), color = Color.DarkGray, fontSize = 12.sp)
            } else {
                Text(text = "Start", color = Color.DarkGray, fontSize = 12.sp)
                Text(text = "Mid", color = Color.DarkGray, fontSize = 12.sp)
                Text(text = "End", color = Color.DarkGray, fontSize = 12.sp)
            }
        }
    }
}
```

### `app/src/main/java/com/invest/easymoney/ui/charts/MPLineChart.kt`

```kotlin
package com.invest.easymoney.ui.charts

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

enum class MarketStatus(val label: String) {
    OPEN("Open"),
    CLOSED("Closed"),
    PRE_MARKET("Pre-Market"),
    AFTER_HOURS("After Hours")
}

private enum class ChartRange(val label: String) {
    OneDay("1D"),
    OneWeek("1W"),
    OneMonth("1M"),
    ThreeMonths("3M"),
    OneYear("1Y"),
    All("ALL")
}

private data class ChartSlice(
    val prices: List<Float>,
    val times: List<Long>?,
    val volumes: List<Long>?
)

@Composable
fun MPLineChart(
    prices: List<Float>,
    times: List<Long>? = null,
    modifier: Modifier = Modifier,

    // Optional richer UI data
    symbol: String = "STOCK",
    companyName: String? = null,
    volumes: List<Long>? = null,
    marketStatus: MarketStatus = MarketStatus.OPEN,
    watchlisted: Boolean = false,

    // Portfolio info (optional)
    holdingQuantity: Float = 0f,
    averageCost: Float = 0f,

    // Actions
    onToggleWatchlist: (Boolean) -> Unit = {},
    onSetAlert: () -> Unit = {},
    onBuy: () -> Unit = {},
    onSell: () -> Unit = {}
) {
    if (prices.isEmpty()) {
        EmptyTradingChart(modifier)
        return
    }

    var selectedRange by remember { mutableStateOf(ChartRange.All) }

    val slice = remember(prices, times, volumes, selectedRange) {
        buildSlice(
            prices = prices,
            times = times,
            volumes = volumes,
            range = selectedRange
        )
    }

    if (slice.prices.isEmpty()) {
        EmptyTradingChart(modifier)
        return
    }

    val currentPrices = slice.prices
    val currentTimes = slice.times
    val currentVolumes = slice.volumes

    val isPositive = currentPrices.last() >= currentPrices.first()
    val trendColorTarget = if (isPositive) Color(0xFF16A34A) else Color(0xFFDC2626)
    val trendColor by animateColorAsState(trendColorTarget, label = "trend-color")

    val open = currentPrices.first()
    val current = currentPrices.last()
    val high = currentPrices.maxOrNull() ?: current
    val low = currentPrices.minOrNull() ?: current
    val change = current - open
    val changePercent = if (open != 0f) (change / open) * 100f else 0f
    val rangeAmount = high - low
    val volatility = if (low != 0f) (rangeAmount / low) * 100f else 0f

    val hasHolding = holdingQuantity > 0f
    val currentHoldingValue = current * holdingQuantity
    val investedValue = averageCost * holdingQuantity
    val pnl = currentHoldingValue - investedValue
    val pnlPercent = if (investedValue != 0f) (pnl / investedValue) * 100f else 0f
    val pnlColor = if (pnl >= 0f) Color(0xFF16A34A) else Color(0xFFDC2626)

    val marketStatusColor = when (marketStatus) {
        MarketStatus.OPEN -> Color(0xFF16A34A)
        MarketStatus.CLOSED -> MaterialTheme.colorScheme.onSurfaceVariant
        MarketStatus.PRE_MARKET -> Color(0xFF2563EB)
        MarketStatus.AFTER_HOURS -> Color(0xFF7C3AED)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Top header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = symbol,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    companyName?.takeIf { it.isNotBlank() }?.let {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = formatPrice(current),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = buildChangeText(change, changePercent),
                        style = MaterialTheme.typography.bodyMedium,
                        color = trendColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    StatusChip(
                        label = marketStatus.label,
                        color = marketStatusColor
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    FilterChip(
                        selected = watchlisted,
                        onClick = { onToggleWatchlist(!watchlisted) },
                        label = {
                            Text(if (watchlisted) "Watchlisted" else "Watchlist")
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Range selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChartRange.entries.forEach { range ->
                    val selected = selectedRange == range
                    FilterChip(
                        selected = selected,
                        onClick = { selectedRange = range },
                        label = {
                            Text(
                                text = range.label,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = trendColor.copy(alpha = 0.14f),
                            selectedLabelColor = trendColor
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main chart + optional volume bars
            TradingChartPanel(
                prices = currentPrices,
                times = currentTimes,
                volumes = currentVolumes,
                trendColor = trendColor,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Quick stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InsightStat("Open", formatPrice(open))
                InsightStat("High", formatPrice(high), valueColor = Color(0xFF16A34A))
                InsightStat("Low", formatPrice(low), valueColor = Color(0xFFDC2626))
                InsightStat("Volatility", "${formatPercent(volatility)}%")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Portfolio mini card
            if (hasHolding) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Your Position",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            PortfolioItem("Qty", trimTrailingZeros(holdingQuantity))
                            PortfolioItem("Avg Cost", formatPrice(averageCost))
                            PortfolioItem("Value", formatPrice(currentHoldingValue))
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = pnlColor.copy(alpha = 0.10f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Unrealized P&L",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${formatPrice(pnl)} (${formatPercent(pnlPercent)}%)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = pnlColor
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onSetAlert,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Set Alert")
                }

                Button(
                    onClick = onSell,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFDC2626)
                    )
                ) {
                    Text("Sell")
                }

                Button(
                    onClick = onBuy,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF16A34A)
                    )
                ) {
                    Text("Buy")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetaText("Trend", if (isPositive) "Bullish" else "Bearish", trendColor)
                    MetaText("Range", formatPrice(rangeAmount), MaterialTheme.colorScheme.primary)
                    MetaText("Points", currentPrices.size.toString(), MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
private fun TradingChartPanel(
    prices: List<Float>,
    times: List<Long>?,
    volumes: List<Long>?,
    trendColor: Color,
    modifier: Modifier = Modifier
) {
    val minPrice = prices.minOrNull() ?: 0f
    val maxPrice = prices.maxOrNull() ?: 0f
    val priceRange = (maxPrice - minPrice).takeIf { it > 0f } ?: 1f

    val showVolumes = volumes != null && volumes.size == prices.size && volumes.isNotEmpty()
    val maxVolume = volumes?.maxOrNull()?.takeIf { it > 0L } ?: 1L

    var lineChartSize by remember { mutableStateOf(IntSize.Zero) }
    var volumeChartSize by remember { mutableStateOf(IntSize.Zero) }
    var selectedIndex by remember { mutableIntStateOf(prices.lastIndex) }

    val chartSurface = MaterialTheme.colorScheme.surface
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    val lineChartHeight = 280.dp
    val volumeChartHeight = if (showVolumes) 64.dp else 0.dp

    val selectedPrice = prices[selectedIndex]
    val selectedTime = formatTimeLabel(selectedIndex, prices.size, times)
    val selectedDelta = selectedPrice - prices.first()
    val selectedDeltaPercent =
        if (prices.first() != 0f) (selectedDelta / prices.first()) * 100f else 0f

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = chartSurface
    ) {
        Column(
            modifier = modifier.padding(horizontal = 10.dp, vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(lineChartHeight)
                    .onSizeChanged { lineChartSize = it }
                    .pointerInput(prices) {
                        detectTapGestures { offset ->
                            selectedIndex = nearestIndex(
                                x = offset.x,
                                width = lineChartSize.width.toFloat(),
                                count = prices.size,
                                xPaddingPx = 16f
                            )
                        }
                    }
                    .pointerInput(prices) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                selectedIndex = nearestIndex(
                                    x = offset.x,
                                    width = lineChartSize.width.toFloat(),
                                    count = prices.size,
                                    xPaddingPx = 16f
                                )
                            },
                            onDrag = { change, _ ->
                                selectedIndex = nearestIndex(
                                    x = change.position.x,
                                    width = lineChartSize.width.toFloat(),
                                    count = prices.size,
                                    xPaddingPx = 16f
                                )
                            }
                        )
                    }
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(lineChartHeight)
                ) {
                    val width = size.width
                    val height = size.height
                    val leftPadding = 16f
                    val rightPadding = 16f
                    val topPadding = 30f
                    val bottomPadding = 22f
                    val chartWidth = width - leftPadding - rightPadding
                    val chartHeight = height - topPadding - bottomPadding

                    // Grid
                    repeat(5) { i ->
                        val y = topPadding + chartHeight * (i / 4f)
                        drawLine(
                            color = gridColor,
                            start = Offset(leftPadding, y),
                            end = Offset(width - rightPadding, y),
                            strokeWidth = 1f
                        )
                    }

                    val points = prices.mapIndexed { index, _ ->
                        chartPoint(
                            index = index,
                            prices = prices,
                            width = width,
                            height = height,
                            minPrice = minPrice,
                            priceRange = priceRange,
                            leftPadding = leftPadding,
                            rightPadding = rightPadding,
                            topPadding = topPadding,
                            bottomPadding = bottomPadding
                        )
                    }

                    val linePath = smoothPath(points)
                    val fillPath = Path().apply {
                        addPath(linePath)
                        lineTo(points.last().x, height - bottomPadding)
                        lineTo(points.first().x, height - bottomPadding)
                        close()
                    }

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                trendColor.copy(alpha = 0.26f),
                                trendColor.copy(alpha = 0.02f)
                            ),
                            startY = topPadding,
                            endY = height - bottomPadding
                        ),
                        style = Fill
                    )

                    drawPath(
                        path = linePath,
                        brush = Brush.horizontalGradient(
                            listOf(trendColor.copy(alpha = 0.90f), trendColor)
                        ),
                        style = Stroke(
                            width = 4f,
                            cap = StrokeCap.Round
                        )
                    )

                    val selectedPoint = points[selectedIndex]

                    // Crosshair
                    drawLine(
                        color = trendColor.copy(alpha = 0.40f),
                        start = Offset(selectedPoint.x, topPadding),
                        end = Offset(selectedPoint.x, height - bottomPadding),
                        strokeWidth = 1.5f
                    )

                    // Selected point glow
                    drawCircle(
                        color = trendColor.copy(alpha = 0.18f),
                        radius = 18f,
                        center = selectedPoint
                    )

                    drawCircle(
                        color = trendColor,
                        radius = 7f,
                        center = selectedPoint
                    )

                    drawCircle(
                        color = Color.White,
                        radius = 3f,
                        center = selectedPoint
                    )

                    // Last point marker
                    val lastPoint = points.last()
                    drawCircle(
                        color = trendColor,
                        radius = 5f,
                        center = lastPoint
                    )
                }

                // Current selected price
                Text(
                    text = formatPrice(selectedPrice),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 12.dp, top = 6.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = buildChangeText(selectedDelta, selectedDeltaPercent),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 12.dp, top = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selectedDelta >= 0f) Color(0xFF16A34A) else Color(0xFFDC2626),
                    fontWeight = FontWeight.SemiBold
                )

                // Floating tooltip
                if (lineChartSize.width > 0) {
                    val selectedPoint = chartPoint(
                        index = selectedIndex,
                        prices = prices,
                        width = lineChartSize.width.toFloat(),
                        height = lineChartSize.height.toFloat(),
                        minPrice = minPrice,
                        priceRange = priceRange,
                        leftPadding = 16f,
                        rightPadding = 16f,
                        topPadding = 30f,
                        bottomPadding = 22f
                    )

                    val tooltipWidthPx = 148f
                    val tooltipX = (selectedPoint.x - tooltipWidthPx / 2f)
                        .coerceIn(8f, lineChartSize.width - tooltipWidthPx - 8f)

                    val tooltipY = if (selectedPoint.y < 90f) {
                        selectedPoint.y + 18f
                    } else {
                        selectedPoint.y - 80f
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        tonalElevation = 6.dp,
                        shadowElevation = 10.dp,
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.offset {
                            IntOffset(tooltipX.roundToInt(), tooltipY.roundToInt())
                        }
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = selectedTime,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = formatPrice(selectedPrice),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = buildChangeText(selectedDelta, selectedDeltaPercent),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (selectedDelta >= 0f) Color(0xFF16A34A) else Color(0xFFDC2626),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Edge time labels
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatBottomEdgeLabel(times?.firstOrNull(), "Start"),
                        style = MaterialTheme.typography.labelSmall,
                        color = labelColor
                    )
                    Text(
                        text = formatBottomEdgeLabel(times?.getOrNull((times?.size ?: 1) / 2), "Middle"),
                        style = MaterialTheme.typography.labelSmall,
                        color = labelColor
                    )
                    Text(
                        text = formatBottomEdgeLabel(times?.lastOrNull(), "End"),
                        style = MaterialTheme.typography.labelSmall,
                        color = labelColor
                    )
                }
            }

            if (showVolumes) {
                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Volume",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 6.dp, bottom = 4.dp)
                )

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(volumeChartHeight)
                        .onSizeChanged { volumeChartSize = it }
                ) {
                    val width = size.width
                    val height = size.height
                    val leftPadding = 16f
                    val rightPadding = 16f
                    val chartWidth = width - leftPadding - rightPadding
                    val xStep = if (prices.size <= 1) 0f else chartWidth / prices.lastIndex.toFloat()
                    val barWidth = (xStep * 0.55f).coerceAtLeast(3f)

                    volumes!!.forEachIndexed { index, volume ->
                        val normalized = volume / maxVolume.toFloat()
                        val barHeight = normalized * (height - 6f)

                        val x = leftPadding + (index * xStep) - barWidth / 2f
                        val y = height - barHeight

                        val barColor = if (index == selectedIndex) {
                            trendColor
                        } else {
                            trendColor.copy(alpha = 0.30f)
                        }

                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset(x, y),
                            size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(
    label: String,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
    }
}

@Composable
private fun InsightStat(
    title: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}

@Composable
private fun PortfolioItem(
    title: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun MetaText(
    title: String,
    value: String,
    valueColor: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$title: ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}

@Composable
private fun EmptyTradingChart(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No chart data",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun buildSlice(
    prices: List<Float>,
    times: List<Long>?,
    volumes: List<Long>?,
    range: ChartRange
): ChartSlice {
    if (prices.isEmpty()) return ChartSlice(emptyList(), null, null)

    val validTimes = times != null && times.size == prices.size && times.isNotEmpty()
    val validVolumes = volumes != null && volumes.size == prices.size && volumes.isNotEmpty()

    if (range == ChartRange.All) {
        return ChartSlice(
            prices = prices,
            times = if (validTimes) times else null,
            volumes = if (validVolumes) volumes else null
        )
    }

    if (validTimes) {
        val ts = times!!
        val end = ts.last()
        val durationSeconds = when (range) {
            ChartRange.OneDay -> 1L * 24 * 60 * 60
            ChartRange.OneWeek -> 7L * 24 * 60 * 60
            ChartRange.OneMonth -> 30L * 24 * 60 * 60
            ChartRange.ThreeMonths -> 90L * 24 * 60 * 60
            ChartRange.OneYear -> 365L * 24 * 60 * 60
            ChartRange.All -> Long.MAX_VALUE
        }

        val threshold = end - durationSeconds
        val fromIndex = ts.indexOfFirst { it >= threshold }.let { if (it == -1) 0 else it }

        return ChartSlice(
            prices = prices.drop(fromIndex),
            times = ts.drop(fromIndex),
            volumes = if (validVolumes) volumes!!.drop(fromIndex) else null
        )
    }

    val keepCount = when (range) {
        ChartRange.OneDay -> max(12, prices.size / 8)
        ChartRange.OneWeek -> max(18, prices.size / 6)
        ChartRange.OneMonth -> max(24, prices.size / 4)
        ChartRange.ThreeMonths -> max(32, prices.size / 2)
        ChartRange.OneYear -> max(40, (prices.size * 3) / 4)
        ChartRange.All -> prices.size
    }.coerceAtMost(prices.size)

    return ChartSlice(
        prices = prices.takeLast(keepCount),
        times = if (validTimes) times!!.takeLast(keepCount) else null,
        volumes = if (validVolumes) volumes!!.takeLast(keepCount) else null
    )
}

private fun chartPoint(
    index: Int,
    prices: List<Float>,
    width: Float,
    height: Float,
    minPrice: Float,
    priceRange: Float,
    leftPadding: Float,
    rightPadding: Float,
    topPadding: Float,
    bottomPadding: Float
): Offset {
    val chartWidth = width - leftPadding - rightPadding
    val chartHeight = height - topPadding - bottomPadding
    val xStep = if (prices.size <= 1) 0f else chartWidth / prices.lastIndex.toFloat()

    val x = leftPadding + (index * xStep)
    val normalized = (prices[index] - minPrice) / priceRange
    val y = topPadding + chartHeight - (normalized * chartHeight)

    return Offset(x, y)
}

private fun nearestIndex(
    x: Float,
    width: Float,
    count: Int,
    xPaddingPx: Float
): Int {
    if (count <= 1 || width <= 0f) return 0
    val chartWidth = width - (xPaddingPx * 2f)
    val clamped = (x - xPaddingPx).coerceIn(0f, chartWidth)
    val ratio = clamped / chartWidth
    return (ratio * (count - 1)).roundToInt().coerceIn(0, count - 1)
}

private fun smoothPath(points: List<Offset>): Path {
    val path = Path()
    if (points.isEmpty()) return path
    if (points.size == 1) {
        path.moveTo(points[0].x, points[0].y)
        return path
    }

    path.moveTo(points.first().x, points.first().y)

    for (i in 1 until points.size) {
        val previous = points[i - 1]
        val current = points[i]
        val midX = (previous.x + current.x) / 2f
        val midY = (previous.y + current.y) / 2f
        path.quadraticTo(previous.x, previous.y, midX, midY)
    }

    val last = points.last()
    path.lineTo(last.x, last.y)
    return path
}

private fun formatTimeLabel(
    index: Int,
    size: Int,
    times: List<Long>?
): String {
    if (times == null || times.size != size || index !in times.indices) {
        return "Point ${index + 1}"
    }

    val formatter = chooseTimeFormatter(times)
    return formatter.format(Date(times[index] * 1000))
}

private fun formatBottomEdgeLabel(
    time: Long?,
    fallback: String
): String {
    if (time == null) return fallback
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return formatter.format(Date(time * 1000))
}

private fun chooseTimeFormatter(times: List<Long>): SimpleDateFormat {
    val spanSeconds = abs((times.lastOrNull() ?: 0L) - (times.firstOrNull() ?: 0L))
    return if (spanSeconds < 24 * 60 * 60) {
        SimpleDateFormat("HH:mm", Locale.getDefault())
    } else {
        SimpleDateFormat("dd MMM", Locale.getDefault())
    }
}

private fun formatPrice(value: Float): String {
    val sign = if (value < 0) "-" else ""
    return "$sign$" + String.format(Locale.US, "%.2f", abs(value))
}

private fun formatPercent(value: Float): String {
    val sign = if (value > 0) "+" else ""
    return sign + String.format(Locale.US, "%.2f", value)
}

private fun buildChangeText(change: Float, percent: Float): String {
    val sign = if (change >= 0f) "+" else ""
    return "$sign${String.format(Locale.US, "%.2f", change)} (${sign}${String.format(Locale.US, "%.2f", percent)}%)"
}

private fun trimTrailingZeros(value: Float): String {
    return if (value % 1f == 0f) {
        value.toInt().toString()
    } else {
        String.format(Locale.US, "%.2f", value)
    }
}
```

### `app/src/main/java/com/invest/easymoney/ui/charts/StockChart.kt`

```kotlin
/*
package com.invest.easymoney.ui.charts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ✅ Vico 3.x imports (IMPORTANT)
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.entry.entryModelOf
import com.patrykandpatrick.vico.compose.m3.marker.rememberMarker

*/
/**
 * ✅ Premium Stock Chart (Vico 3.x)
 *//*

@Composable
fun StockChart(
    prices: List<Float>,
    modifier: Modifier = Modifier
) {

    if (prices.isEmpty()) {
        EmptyChart(modifier)
        return
    }

    // ✅ Bull / Bear detection
    val isPositive = prices.last() >= prices.first()

    val lineColor = if (isPositive) {
        Color(0xFF00C853) // Green
    } else {
        Color(0xFFD50000) // Red
    }

    val model = entryModelOf(*prices.toTypedArray())

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {

        Column(modifier = Modifier.padding(16.dp)) {

            Text(
                text = "Price Chart",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            Chart(
                chart = lineChart(
                    lines = listOf(
                        lineChart().lines.first().copy(
                            lineColor = lineColor
                        )
                    )
                ),
                model = model,
                startAxis = rememberStartAxis(),
                bottomAxis = rememberBottomAxis(),
                marker = rememberMarker(), // ✅ works in compose-m3
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Text(
                    text = "Open: ${formatPrice(prices.first())}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Current: ${formatPrice(prices.last())}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = lineColor
                )
            }
        }
    }
}

@Composable
private fun EmptyChart(modifier: Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("No chart data")
    }
}

private fun formatPrice(value: Float): String {
    return "₹%.2f".format(value)
}*/
```

### `app/src/main/java/com/invest/easymoney/ui/detail/StockDetailScreen.kt`

```kotlin
package com.invest.easymoney.ui.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.invest.easymoney.domain.model.AlertType
import com.invest.easymoney.domain.model.News
import com.invest.easymoney.domain.model.Stock
import com.invest.easymoney.domain.model.StockInsight
import com.invest.easymoney.ui.charts.MPLineChart
import com.invest.easymoney.ui.theme.GainGreen
import com.invest.easymoney.ui.theme.LossRed
import com.invest.easymoney.ui.theme.TradingShapes
import com.invest.easymoney.ui.theme.TradingTextStyles
import com.invest.easymoney.ui.webview.WebViewBottomSheet
import com.invest.easymoney.util.Resource
import java.util.Locale

private enum class PremiumSection(val label: String) {
    OVERVIEW("Overview"),
    AI("AI Insight"),
    NEWS("News")
}

private enum class ValuationHealth(val label: String, val color: Color) {
    CHEAP("Cheap", GainGreen),
    FAIR("Fair", Color(0xFFB8860B)),
    EXPENSIVE("Expensive", LossRed),
    UNAVAILABLE("Unavailable", Color(0xFF6B7280))
}

private data class AnalystRatingUi(
    val consensus: String,
    val buyPercent: Int,
    val holdPercent: Int,
    val sellPercent: Int,
    val lowTarget: String,
    val averageTarget: String,
    val highTarget: String,
    val upsideText: String
)

private data class FinancialRatiosUi(
    val peRatio: String? = null,
    val pbRatio: String? = null,
    val eps: String? = null,
    val dividendYield: String? = null,
    val roe: String? = null,
    val debtToEquity: String? = null
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun StockDetailScreen(
    onBack: () -> Unit,
    viewModel: StockDetailViewModel = hiltViewModel()
) {
    val stockState by viewModel.stockState.collectAsStateWithLifecycle()
    val newsState by viewModel.newsState.collectAsStateWithLifecycle()
    val insightState by viewModel.insightState.collectAsStateWithLifecycle()
    val explainState by viewModel.explainState.collectAsStateWithLifecycle()
    val isInWatchlist by viewModel.isInWatchlist.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showAlertDialog by remember { mutableStateOf(false) }
    var webViewUrl by remember { mutableStateOf<String?>(null) }
    var selectedSection by remember { mutableStateOf(PremiumSection.OVERVIEW) }

    webViewUrl?.let { url -> WebViewBottomSheet(url = url, onDismiss = { webViewUrl = null }) }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    if (showAlertDialog) {
        SetPriceAlertDialog(
            onDismissDialog = { showAlertDialog = false },
            onConfirm = { pct, type ->
                viewModel.setAlert(pct, type)
                showAlertDialog = false
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(viewModel.symbol, style = TradingTextStyles.Ticker, fontWeight = FontWeight.Bold)
                        Text(
                            "Premium Stock Detail",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleWatchlist() }) {
                        Icon(
                            imageVector = if (isInWatchlist) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Watchlist",
                            tint = if (isInWatchlist) Color(0xFFFFD54F) else MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    IconButton(onClick = { showAlertDialog = true }) {
                        Icon(Icons.Default.Notifications, contentDescription = "Set Alert")
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        when (stockState) {
            is Resource.Loading -> PremiumShimmerLoadingState(modifier = Modifier.fillMaxSize().padding(padding))
            is Resource.Error -> PremiumErrorState(
                message = (stockState as Resource.Error).message,
                onRetry = { viewModel.loadDetail() },
                modifier = Modifier.fillMaxSize().padding(padding)
            )
            is Resource.Success -> {
                val stock = (stockState as Resource.Success<Stock>).data
                val news = (newsState as? Resource.Success)?.data ?: emptyList()
                val prices = stock.intradayPrices.map { it.price }
                val timesList = stock.intradayPrices.mapNotNull { it.time.toLongOrNull() }
                val timesForChart = if (timesList.size == prices.size) timesList else null
                val analystRatingUi = buildAnalystRatingUi(stock)
                val financialRatiosUi = buildFinancialRatiosUi(stock)

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        PremiumHeroCard(
                            stock = stock,
                            isInWatchlist = isInWatchlist,
                            onToggleWatchlist = { viewModel.toggleWatchlist() },
                            onSetAlert = { showAlertDialog = true },
                            onExplainMove = { viewModel.explainMove() }
                        )
                    }
                    item {
                        PremiumChartContainer(
                            prices = prices,
                            timesForChart = timesForChart,
                            insightState = insightState
                        )
                    }
                    stickyHeader {
                        StickySectionTabs(
                            selected = selectedSection,
                            onSelected = { selectedSection = it }
                        )
                    }
                    when (selectedSection) {
                        PremiumSection.OVERVIEW -> {
                            item { OverviewHighlightsRow(stock = stock) }
                            item { EnhancedMarketDataCard(stock) }
                            item { QuickStatsGrid(stock) }
                            item { AnalystRatingCard(data = analystRatingUi, currentPrice = stock.currentPrice) }
                            item { FinancialRatiosExpandableCard(data = financialRatiosUi) }
                            item { CompanyProfileCard(stock) }
                            if (news.isNotEmpty()) {
                                item { SectionHeader(title = "Top Headlines", subtitle = "Latest news and context around ${stock.symbol}") }
                                items(news.take(3), key = { it.id }) { newsItem ->
                                    PremiumNewsCard(news = newsItem, onOpenUrl = { if (newsItem.url.isNotBlank()) webViewUrl = newsItem.url })
                                }
                            }
                        }
                        PremiumSection.AI -> {
                            item { AiSentimentMeter(insightState = insightState) }
                            item { AiInsightCard(insightState = insightState, onRetry = { viewModel.loadDetail() }) }
                            item {
                                ExplainMoveCard(
                                    state = explainState,
                                    onExplain = { viewModel.explainMove() },
                                    onDismiss = { viewModel.dismissExplain() }
                                )
                            }
                        }
                        PremiumSection.NEWS -> {
                            if (news.isEmpty()) {
                                item { EmptyNewsState() }
                            } else {
                                item { SectionHeader(title = "Latest News", subtitle = "Tap any article to open it in the in-app browser") }
                                items(news, key = { it.id }) { newsItem ->
                                    PremiumNewsCard(news = newsItem, onOpenUrl = { if (newsItem.url.isNotBlank()) webViewUrl = newsItem.url })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumHeroCard(
    stock: Stock,
    isInWatchlist: Boolean,
    onToggleWatchlist: () -> Unit,
    onSetAlert: () -> Unit,
    onExplainMove: () -> Unit
) {
    val isPositive = stock.changePercent >= 0
    val trendColor = if (isPositive) GainGreen else LossRed
    val arrowIcon = if (isPositive) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = TradingShapes.HeroCard,
        elevation = CardDefaults.cardElevation(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stock.symbol, style = TradingTextStyles.Ticker, fontWeight = FontWeight.Bold)
                    if (stock.name.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(stock.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("$${String.format("%.2f", stock.currentPrice)}", style = TradingTextStyles.PriceHero, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(arrowIcon, contentDescription = null, tint = trendColor, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${if (isPositive) "+" else ""}${String.format("%.2f", stock.change)} • ${if (isPositive) "+" else ""}${String.format("%.2f", stock.changePercent)}%",
                            style = TradingTextStyles.PriceChange,
                            color = trendColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Surface(shape = TradingShapes.Pill, color = trendColor.copy(alpha = 0.12f)) {
                    Text(
                        text = if (isPositive) "Bullish Day" else "Bearish Day",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        color = trendColor,
                        style = TradingTextStyles.Chip,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AssistChip(
                    onClick = onToggleWatchlist,
                    label = { Text(if (isInWatchlist) "In Watchlist" else "Add Watchlist") },
                    leadingIcon = {
                        Icon(
                            imageVector = if (isInWatchlist) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    shape = TradingShapes.Pill
                )
                AssistChip(
                    onClick = onSetAlert,
                    label = { Text("Price Alert") },
                    leadingIcon = { Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    shape = TradingShapes.Pill
                )
                AssistChip(
                    onClick = onExplainMove,
                    label = { Text("Explain Move") },
                    leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    shape = TradingShapes.Pill
                )
            }
        }
    }
}

@Composable
private fun PremiumChartContainer(
    prices: List<Float>,
    timesForChart: List<Long>?,
    insightState: Resource<StockInsight>
) {
    val sentiment = (insightState as? Resource.Success)?.data?.sentiment?.lowercase()
    val sentimentText = when (sentiment) {
        "bullish" -> "AI sees bullish momentum"
        "bearish" -> "AI sees caution in trend"
        else -> "AI sentiment unavailable or neutral"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = TradingShapes.ChartContainer,
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column {
                    Text("Interactive Price Chart", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Scrub, inspect, and review stock movement",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(shape = TradingShapes.Pill, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) {
                    Text(
                        text = sentimentText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            MPLineChart(prices = prices, times = timesForChart, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun StickySectionTabs(selected: PremiumSection, onSelected: (PremiumSection) -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.background) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 8.dp).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PremiumSection.entries.forEach { section ->
                FilterChip(
                    selected = selected == section,
                    onClick = { onSelected(section) },
                    label = { Text(section.label, fontWeight = if (selected == section) FontWeight.SemiBold else FontWeight.Medium) },
                    shape = TradingShapes.Pill,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

@Composable
private fun OverviewHighlightsRow(stock: Stock) {
    val isPositive = stock.changePercent >= 0
    val changeColor = if (isPositive) GainGreen else LossRed
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        HighlightTile("Trend", if (isPositive) "Upward" else "Downward", Modifier.weight(1f), valueColor = changeColor)
        HighlightTile("Exchange", stock.exchange.ifBlank { "N/A" }, Modifier.weight(1f))
        HighlightTile("Industry", stock.industry.ifBlank { "N/A" }, Modifier.weight(1f))
    }
}

@Composable
private fun HighlightTile(title: String, value: String, modifier: Modifier = Modifier, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Surface(modifier = modifier, shape = TradingShapes.StatTile, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = TradingTextStyles.StatLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = TradingTextStyles.StatValue, fontWeight = FontWeight.SemiBold, color = valueColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun EnhancedMarketDataCard(stock: Stock) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = TradingShapes.Card,
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ShowChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Market Data", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            InfoRowModern("Open", "$${format(stock.openPrice)}")
            InfoRowModern("Prev Close", "$${format(stock.previousClose)}")
            InfoRowModern("Day High", "$${format(stock.highPrice)}", valueColor = GainGreen)
            InfoRowModern("Day Low", "$${format(stock.lowPrice)}", valueColor = LossRed)
            if (stock.marketCap > 0) InfoRowModern("Market Cap", formatMarketCap(stock.marketCap))
            if (stock.exchange.isNotBlank()) InfoRowModern("Exchange", stock.exchange)
            if (stock.industry.isNotBlank()) InfoRowModern("Industry", stock.industry)
        }
    }
}

@Composable
private fun QuickStatsGrid(stock: Stock) {
    val stats = buildList {
        add("Current" to "$${format(stock.currentPrice)}")
        add("Change" to "${if (stock.change >= 0) "+" else ""}${format(stock.change)}")
        add("Open" to "$${format(stock.openPrice)}")
        add("Previous Close" to "$${format(stock.previousClose)}")
        add("High" to "$${format(stock.highPrice)}")
        add("Low" to "$${format(stock.lowPrice)}")
        if (stock.marketCap > 0) add("Market Cap" to formatMarketCap(stock.marketCap))
        if (stock.exchange.isNotBlank()) add("Exchange" to stock.exchange)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = TradingShapes.Card,
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Quick Stats", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(14.dp))
            stats.chunked(2).forEachIndexed { index, row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { (label, value) ->
                        Surface(modifier = Modifier.weight(1f), shape = TradingShapes.StatTile, color = MaterialTheme.colorScheme.surface) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(label, style = TradingTextStyles.StatLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(7.dp))
                                Text(value, style = TradingTextStyles.StatValue, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
                if (index != stats.chunked(2).lastIndex) Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun AnalystRatingCard(data: AnalystRatingUi?, currentPrice: Double, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = TradingShapes.Card,
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Analyst Rating", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Consensus, target range, and recommendation mix", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (data == null) {
                Surface(shape = TradingShapes.StatTile, color = MaterialTheme.colorScheme.surface) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Analyst data unavailable", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Connect analyst target and recommendation data from your backend or market API to unlock target range visuals and consensus breakdown.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        DisabledTargetRangeMeter(currentPrice = currentPrice)
                    }
                }
            } else {
                val consensusColor = when (data.consensus.lowercase()) {
                    "buy", "strong buy", "bullish" -> GainGreen
                    "sell", "strong sell", "bearish" -> LossRed
                    else -> Color(0xFFB8860B)
                }
                Surface(shape = TradingShapes.StatTile, color = consensusColor.copy(alpha = 0.12f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Consensus", style = TradingTextStyles.StatLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(data.consensus, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = consensusColor)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Avg Target", style = TradingTextStyles.StatLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(data.averageTarget, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(data.upsideText, style = TradingTextStyles.Chip, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                AnalystTargetRangeMeter(currentPrice = currentPrice, lowTarget = data.lowTarget, averageTarget = data.averageTarget, highTarget = data.highTarget)
                SentimentBreakdownBar("Buy", data.buyPercent, GainGreen)
                SentimentBreakdownBar("Hold", data.holdPercent, Color(0xFFB8860B))
                SentimentBreakdownBar("Sell", data.sellPercent, LossRed)
            }
        }
    }
}

@Composable
private fun FinancialRatiosExpandableCard(data: FinancialRatiosUi?, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(true) }
    val valuationHealth = data?.let { computeValuationHealth(it.peRatio) } ?: ValuationHealth.UNAVAILABLE
    val ratioChips = remember(data) { buildRatioInterpretationChips(data) }

    Card(
        modifier = modifier.fillMaxWidth().animateContentSize(),
        shape = TradingShapes.Card,
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ShowChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Financial Ratios", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Valuation and profitability snapshot", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                TextButton(onClick = { expanded = !expanded }) {
                    Icon(if (expanded) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (expanded) "Collapse" else "Expand")
                }
            }
            ValuationHealthBadge(valuationHealth)
            RatioInterpretationChipRow(chips = ratioChips)
            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (data == null) {
                        Surface(shape = TradingShapes.StatTile, color = MaterialTheme.colorScheme.surface) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Financial ratio data unavailable", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "P/E, P/B, EPS, dividend yield, ROE, and debt metrics will appear here after you connect them from your data source.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        val items = listOf(
                            "P/E Ratio" to (data.peRatio ?: "N/A"),
                            "P/B Ratio" to (data.pbRatio ?: "N/A"),
                            "EPS" to (data.eps ?: "N/A"),
                            "Dividend Yield" to (data.dividendYield ?: "N/A"),
                            "ROE" to (data.roe ?: "N/A"),
                            "Debt / Equity" to (data.debtToEquity ?: "N/A")
                        )
                        items.chunked(2).forEachIndexed { idx, row ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                row.forEach { (label, value) -> RatioTile(label, value, Modifier.weight(1f)) }
                                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                            }
                            if (idx != items.chunked(2).lastIndex) Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                    Text(
                        "These interpretation badges are quick heuristics for UI guidance only and should not be treated as financial advice.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ValuationHealthBadge(valuationHealth: ValuationHealth) {
    Surface(shape = TradingShapes.Pill, color = valuationHealth.color.copy(alpha = 0.12f)) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).background(valuationHealth.color, CircleShape))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Valuation Health: ${valuationHealth.label}",
                style = TradingTextStyles.Chip,
                fontWeight = FontWeight.SemiBold,
                color = valuationHealth.color
            )
        }
    }
}

@Composable
private fun RatioInterpretationChipRow(chips: List<Pair<String, Color>>) {
    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        chips.forEach { (label, color) ->
            Surface(shape = TradingShapes.Pill, color = color.copy(alpha = 0.12f)) {
                Text(label, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), style = TradingTextStyles.Chip, color = color, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun AnalystTargetRangeMeter(currentPrice: Double, lowTarget: String, averageTarget: String, highTarget: String) {
    val low = parseNumericValue(lowTarget)
    val average = parseNumericValue(averageTarget)
    val high = parseNumericValue(highTarget)
    if (low == null || average == null || high == null || high <= low) {
        DisabledTargetRangeMeter(currentPrice = currentPrice)
        return
    }
    val currentPosition = ((currentPrice - low) / (high - low)).coerceIn(0.0, 1.0).toFloat()
    val averagePosition = ((average - low) / (high - low)).coerceIn(0.0, 1.0).toFloat()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Target Range", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Surface(shape = TradingShapes.Pill, color = MaterialTheme.colorScheme.surface) {
            Box(modifier = Modifier.fillMaxWidth().height(14.dp)) {
                Box(modifier = Modifier.fillMaxWidth().height(14.dp).background(MaterialTheme.colorScheme.surfaceVariant, TradingShapes.Pill))
                Box(modifier = Modifier.fillMaxWidth(averagePosition).height(14.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), TradingShapes.Pill))
                Box(modifier = Modifier.fillMaxWidth(currentPosition).height(14.dp).background(GainGreen.copy(alpha = 0.70f), TradingShapes.Pill))
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            MeterLabel("Low", lowTarget)
            MeterLabel("Avg", averageTarget)
            MeterLabel("High", highTarget)
        }
        Text("Current: $${String.format(Locale.US, "%.2f", currentPrice)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DisabledTargetRangeMeter(currentPrice: Double) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Target Range", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Surface(shape = TradingShapes.Pill, color = MaterialTheme.colorScheme.surface) {
            Box(modifier = Modifier.fillMaxWidth().height(14.dp).background(MaterialTheme.colorScheme.surfaceVariant, TradingShapes.Pill))
        }
        Text(
            "Current: $${String.format(Locale.US, "%.2f", currentPrice)} • Connect analyst targets to activate the range meter",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MeterLabel(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = TradingTextStyles.StatLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, style = TradingTextStyles.StatValue, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SentimentBreakdownBar(label: String, percentage: Int, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("$percentage%", style = TradingTextStyles.Chip, fontWeight = FontWeight.SemiBold)
        }
        Surface(shape = TradingShapes.Pill, color = MaterialTheme.colorScheme.surface) {
            Box(modifier = Modifier.fillMaxWidth().height(10.dp)) {
                Box(modifier = Modifier.fillMaxWidth((percentage.coerceIn(0, 100)) / 100f).height(10.dp).background(color, TradingShapes.Pill))
            }
        }
    }
}

@Composable
private fun RatioTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = TradingShapes.StatTile, color = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, style = TradingTextStyles.StatLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = TradingTextStyles.StatValue, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun CompanyProfileCard(stock: Stock) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = TradingShapes.Card,
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Business, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Company Profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Surface(shape = TradingShapes.StatTile, color = MaterialTheme.colorScheme.surface) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ProfileRow("Symbol", stock.symbol)
                    ProfileRow("Company", stock.name.ifBlank { "N/A" })
                    ProfileRow("Exchange", stock.exchange.ifBlank { "N/A" })
                    ProfileRow("Industry", stock.industry.ifBlank { "N/A" })
                    ProfileRow("Estimated Size", if (stock.marketCap > 0) formatMarketCap(stock.marketCap) else "N/A")
                }
            }
        }
    }
}

@Composable
private fun ProfileRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = TradingTextStyles.StatLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun AiSentimentMeter(insightState: Resource<StockInsight>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = TradingShapes.Card,
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("AI Sentiment Meter", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            when (insightState) {
                is Resource.Success -> {
                    val sentiment = insightState.data.sentiment.lowercase()
                    val score = when (sentiment) {
                        "bullish" -> 0.82f
                        "bearish" -> 0.22f
                        else -> 0.50f
                    }
                    val meterColor = when (sentiment) {
                        "bullish" -> GainGreen
                        "bearish" -> LossRed
                        else -> Color(0xFFB8860B)
                    }
                    Surface(shape = TradingShapes.Pill, color = MaterialTheme.colorScheme.surface) {
                        Box(modifier = Modifier.fillMaxWidth().height(12.dp)) {
                            Box(modifier = Modifier.fillMaxWidth(score).height(12.dp).background(meterColor, TradingShapes.Pill))
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Bearish", style = TradingTextStyles.StatLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(insightState.data.sentiment, style = TradingTextStyles.Chip, fontWeight = FontWeight.Bold, color = meterColor)
                        Text("Bullish", style = TradingTextStyles.StatLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                is Resource.Loading -> {
                    Text("Scanning signals and estimating sentiment...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                is Resource.Error -> {
                    Text("Unable to load AI sentiment at the moment.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun AiInsightCard(insightState: Resource<StockInsight>, onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = TradingShapes.Card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("AI Insight", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            when (insightState) {
                is Resource.Loading -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text("Analyzing market data...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
                is Resource.Error -> {
                    Text("⚠️ ${insightState.message}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = onRetry, contentPadding = PaddingValues(0.dp)) {
                        Text("Retry", style = MaterialTheme.typography.labelSmall)
                    }
                }
                is Resource.Success -> {
                    val insight = insightState.data
                    SentimentBadge(insight.sentiment)
                    Text(insight.insight, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.15f))
                    InsightOutlookRow("📅 Short-term", insight.shortTermOutlook)
                    InsightOutlookRow("📈 Long-term", insight.longTermOutlook)
                    Text(
                        "⚠️ For informational purposes only. Not financial advice.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.65f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SentimentBadge(sentiment: String) {
    val (emoji, color) = when (sentiment.lowercase()) {
        "bullish" -> "🟢 Bullish" to GainGreen
        "bearish" -> "🔴 Bearish" to LossRed
        else -> "🟡 Neutral" to Color(0xFFB8860B)
    }
    Surface(shape = TradingShapes.Pill, color = color.copy(alpha = 0.15f)) {
        Text(emoji, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = TradingTextStyles.Chip, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun InsightOutlookRow(label: String, text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = TradingTextStyles.StatLabel, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f))
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

@Composable
private fun InfoRowModern(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, TradingShapes.Input).padding(horizontal = 12.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = TradingTextStyles.StatValue, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}

@Composable
private fun PremiumNewsCard(news: News, onOpenUrl: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = TradingShapes.Card,
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        onClick = { if (news.url.isNotBlank()) onOpenUrl(news.url) }
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Surface(shape = TradingShapes.StatTile, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), modifier = Modifier.size(46.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Business, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(news.headline, style = TradingTextStyles.NewsHeadline, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(6.dp))
                Text(news.source, style = TradingTextStyles.NewsMeta, color = MaterialTheme.colorScheme.primary)
                if (news.summary.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(news.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            }
            if (news.url.isNotBlank()) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open article", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 8.dp).size(18.dp).align(Alignment.CenterVertically))
            }
        }
    }
}

@Composable
private fun EmptyNewsState() {
    Card(modifier = Modifier.fillMaxWidth(), shape = TradingShapes.Card, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("No news available", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text("Latest headlines will appear here when available.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PremiumShimmerLoadingState(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(durationMillis = 900),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "alpha"
    )
    LazyColumn(modifier = modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { ShimmerCard(height = 180.dp, alpha = alpha) }
        item { ShimmerCard(height = 320.dp, alpha = alpha) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ShimmerCard(modifier = Modifier.weight(1f), height = 90.dp, alpha = alpha)
                ShimmerCard(modifier = Modifier.weight(1f), height = 90.dp, alpha = alpha)
            }
        }
        item { ShimmerCard(height = 220.dp, alpha = alpha) }
        item { ShimmerCard(height = 180.dp, alpha = alpha) }
    }
}

@Composable
private fun ShimmerCard(modifier: Modifier = Modifier, height: Dp, alpha: Float) {
    Card(modifier = modifier.fillMaxWidth(), shape = TradingShapes.Card, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Box(modifier = Modifier.fillMaxWidth().height(height).alpha(alpha).background(MaterialTheme.colorScheme.surfaceVariant))
    }
}

@Composable
private fun PremiumErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Card(shape = TradingShapes.Dialog, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("⚠️ $message", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onRetry, shape = TradingShapes.Input) { Text("Retry") }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SetPriceAlertDialog(onDismissDialog: () -> Unit, onConfirm: (Float, AlertType) -> Unit) {
    var percentage by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(AlertType.INCREASE) }
    AlertDialog(
        onDismissRequest = onDismissDialog,
        title = { Text("Set Price Alert") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = percentage,
                    onValueChange = { if (it.isEmpty() || it.toFloatOrNull() != null) percentage = it },
                    label = { Text("Percentage (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = TradingShapes.Input
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = selectedType == AlertType.INCREASE, onClick = { selectedType = AlertType.INCREASE }, label = { Text("📈 Gain") }, shape = TradingShapes.Pill)
                    FilterChip(selected = selectedType == AlertType.DECREASE, onClick = { selectedType = AlertType.DECREASE }, label = { Text("📉 Drop") }, shape = TradingShapes.Pill)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { percentage.toFloatOrNull()?.let { pct -> onConfirm(pct, selectedType) } },
                enabled = percentage.toFloatOrNull() != null && (percentage.toFloatOrNull() ?: 0f) > 0f,
                shape = TradingShapes.Input
            ) { Text("Set Alert") }
        },
        dismissButton = { TextButton(onClick = onDismissDialog) { Text("Cancel") } },
        shape = TradingShapes.Dialog
    )
}

@Composable
private fun ExplainMoveCard(state: Resource<String>?, onExplain: () -> Unit, onDismiss: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = TradingShapes.Card, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Why did it move?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                }
                if (state != null && state !is Resource.Loading) {
                    TextButton(onClick = onDismiss, contentPadding = PaddingValues(0.dp)) { Text("Clear", style = MaterialTheme.typography.labelSmall) }
                }
            }
            when (state) {
                null -> {
                    Text(
                        "Use AI + retrieved context to explain the drivers behind the stock’s recent move.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.78f)
                    )
                    Button(onClick = onExplain, modifier = Modifier.fillMaxWidth(), shape = TradingShapes.Input, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Explain This Move")
                    }
                }
                is Resource.Loading -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text("Retrieving context and generating explanation...", style = MaterialTheme.typography.bodySmall)
                    }
                }
                is Resource.Error -> {
                    Text("⚠️ ${state.message}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = onExplain, contentPadding = PaddingValues(0.dp)) { Text("Retry", style = MaterialTheme.typography.labelSmall) }
                }
                is Resource.Success -> {
                    Text(state.data, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
                    Text(
                        "⚠️ AI-generated. Not financial advice.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.65f)
                    )
                }
            }
        }
    }
}

private fun buildAnalystRatingUi(stock: Stock): AnalystRatingUi? = null
private fun buildFinancialRatiosUi(stock: Stock): FinancialRatiosUi? = null

private fun computeValuationHealth(peRatio: String?): ValuationHealth {
    val pe = parseNumericValue(peRatio)
    return when {
        pe == null -> ValuationHealth.UNAVAILABLE
        pe <= 15.0 -> ValuationHealth.CHEAP
        pe <= 28.0 -> ValuationHealth.FAIR
        else -> ValuationHealth.EXPENSIVE
    }
}

private fun buildRatioInterpretationChips(data: FinancialRatiosUi?): List<Pair<String, Color>> {
    if (data == null) return listOf("Ratios unavailable" to Color(0xFF6B7280))
    val chips = mutableListOf<Pair<String, Color>>()
    val pe = parseNumericValue(data.peRatio)
    chips += when {
        pe == null -> "P/E unavailable" to Color(0xFF6B7280)
        pe <= 15.0 -> "P/E looks value-oriented" to GainGreen
        pe <= 28.0 -> "P/E in fair zone" to Color(0xFFB8860B)
        else -> "P/E looks rich" to LossRed
    }
    val pb = parseNumericValue(data.pbRatio)
    chips += when {
        pb == null -> "P/B unavailable" to Color(0xFF6B7280)
        pb <= 3.0 -> "P/B conservative" to GainGreen
        pb <= 8.0 -> "P/B moderate" to Color(0xFFB8860B)
        else -> "P/B elevated" to LossRed
    }
    val dividend = parseNumericValue(data.dividendYield)
    chips += when {
        dividend == null -> "Yield unavailable" to Color(0xFF6B7280)
        dividend >= 3.0 -> "Yield income-friendly" to GainGreen
        dividend >= 1.0 -> "Yield modest" to Color(0xFFB8860B)
        else -> "Yield low" to LossRed
    }
    val roe = parseNumericValue(data.roe)
    chips += when {
        roe == null -> "ROE unavailable" to Color(0xFF6B7280)
        roe >= 15.0 -> "ROE strong" to GainGreen
        roe >= 8.0 -> "ROE stable" to Color(0xFFB8860B)
        else -> "ROE weak" to LossRed
    }
    val debt = parseNumericValue(data.debtToEquity)
    chips += when {
        debt == null -> "Debt unavailable" to Color(0xFF6B7280)
        debt <= 0.5 -> "Debt light" to GainGreen
        debt <= 1.5 -> "Debt manageable" to Color(0xFFB8860B)
        else -> "Debt heavy" to LossRed
    }
    return chips
}

private fun parseNumericValue(value: String?): Double? {
    if (value.isNullOrBlank()) return null
    return value.replace("$", "").replace(",", "").replace("%", "").trim().toDoubleOrNull()
}

private fun format(value: Double): String = String.format(Locale.US, "%.2f", value)
private fun formatMarketCap(cap: Double): String = when {
    cap >= 1_000_000 -> "$${String.format(Locale.US, "%.1f", cap / 1_000_000)}T"
    cap >= 1_000 -> "$${String.format(Locale.US, "%.1f", cap / 1_000)}B"
    else -> "$${String.format(Locale.US, "%.1f", cap)}M"
}
```

### `app/src/main/java/com/invest/easymoney/ui/detail/StockDetailViewModel.kt`

```kotlin
package com.invest.easymoney.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.invest.easymoney.di.DeviceId
import com.invest.easymoney.domain.model.Alert
import com.invest.easymoney.domain.model.AlertType
import com.invest.easymoney.domain.model.News
import com.invest.easymoney.domain.model.Stock
import com.invest.easymoney.domain.model.StockInsight
import com.invest.easymoney.domain.repository.AiInsightRepository
import com.invest.easymoney.domain.repository.BackendRepository
import com.invest.easymoney.domain.repository.StockRepository
import com.invest.easymoney.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StockDetailViewModel @Inject constructor(
    private val repository: StockRepository,
    private val aiRepository: AiInsightRepository,
    private val backendRepository: BackendRepository,
    savedStateHandle: SavedStateHandle,
    @DeviceId private val deviceId: String
) : ViewModel() {

    val symbol: String = checkNotNull(savedStateHandle["symbol"])

    private val _stockState = MutableStateFlow<Resource<Stock>>(Resource.Loading)
    val stockState: StateFlow<Resource<Stock>> = _stockState

    private val _newsState = MutableStateFlow<Resource<List<News>>>(Resource.Loading)
    val newsState: StateFlow<Resource<List<News>>> = _newsState

    private val _insightState = MutableStateFlow<Resource<StockInsight>>(Resource.Loading)
    val insightState: StateFlow<Resource<StockInsight>> = _insightState

    private val _explainState = MutableStateFlow<Resource<String>?>(null)
    val explainState: StateFlow<Resource<String>?> = _explainState

    private val _isInWatchlist = MutableStateFlow(false)
    val isInWatchlist: StateFlow<Boolean> = _isInWatchlist

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage

    private var loadingJob: Job? = null

    init {
        loadDetail()
    }

    fun loadDetail() {
        loadingJob?.cancel()
        loadingJob = viewModelScope.launch {
            _stockState.value = Resource.Loading
            _newsState.value = Resource.Loading
            _insightState.value = Resource.Loading

            // Load stock, news, and watchlist status concurrently
            val stockDeferred = async { repository.getStockDetail(symbol) }
            val newsDeferred = async { repository.getNews(symbol) }
            val watchlistDeferred = async { repository.isInWatchlist(symbol) }

            val stockResult = stockDeferred.await()
            val newsResult = newsDeferred.await()
            _isInWatchlist.value = watchlistDeferred.await()

            _stockState.value = stockResult
            _newsState.value = newsResult

            // Fetch AI insight after stock+news data is available
            val stock = (stockResult as? Resource.Success)?.data
            val newsList = (newsResult as? Resource.Success)?.data ?: emptyList()

            _insightState.value = if (stock != null) {
                aiRepository.getStockInsight(symbol, stock, newsList)
            } else {
                Resource.Error("AI insight requires stock data")
            }
        }
    }

    fun toggleWatchlist() {
        viewModelScope.launch {
            if (_isInWatchlist.value) {
                repository.removeFromWatchlist(symbol)
                _isInWatchlist.value = false
                _snackbarMessage.value = "$symbol removed from watchlist"
                // Notify community (fire-and-forget)
                launch { backendRepository.notifyWatchlistChange(deviceId, symbol, "remove") }
            } else {
                repository.addToWatchlist(symbol)
                _isInWatchlist.value = true
                _snackbarMessage.value = "$symbol added to watchlist"
                // Notify community (fire-and-forget)
                launch { backendRepository.notifyWatchlistChange(deviceId, symbol, "add") }
            }
        }
    }

    fun setAlert(percentage: Float, type: AlertType) {
        viewModelScope.launch {
            repository.addAlert(Alert(symbol = symbol, percentage = percentage, type = type))
            _snackbarMessage.value = "Alert set for $symbol: ${if (type == AlertType.INCREASE) "+" else "-"}${percentage}%"
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun explainMove() {
        if (_explainState.value is Resource.Loading) return
        viewModelScope.launch {
            _explainState.value = Resource.Loading
            _explainState.value = backendRepository.explainMove(symbol)
        }
    }

    fun dismissExplain() {
        _explainState.value = null
    }
}
```

### `app/src/main/java/com/invest/easymoney/ui/home/HomeScreen.kt`

```kotlin
package com.invest.easymoney.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.invest.easymoney.domain.model.AiPick
import com.invest.easymoney.domain.model.Stock
import com.invest.easymoney.ui.theme.GainGreen
import com.invest.easymoney.ui.theme.LossRed
import com.invest.easymoney.ui.theme.TradingShapes
import com.invest.easymoney.ui.theme.TradingTextStyles
import com.invest.easymoney.util.Resource
import kotlin.math.absoluteValue

private enum class HomeTab(val title: String) {
    GAINERS("Gainers"),
    LOSERS("Losers"),
    TRENDING("Trending")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@Composable
fun HomeScreen(
    onStockClick: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.topStocksState.collectAsStateWithLifecycle()
    val gainers by viewModel.gainers.collectAsStateWithLifecycle()
    val losers by viewModel.losers.collectAsStateWithLifecycle()
    val trending by viewModel.trending.collectAsStateWithLifecycle()
    val aiPicksState by viewModel.aiPicksState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchState by viewModel.searchState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by rememberSaveable { mutableStateOf(HomeTab.GAINERS) }

    if (aiPicksState != null) {
        AiPicksBottomSheetUltra(
            aiPicksState = aiPicksState!!,
            onDismiss = { viewModel.dismissAiPicks() }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            HomeSearchFirstTopBar(
                isRefreshing = state is Resource.Loading,
                onRefresh = { viewModel.loadStocks() }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        when (state) {
            is Resource.Loading -> SearchFirstLoadingState(
                modifier = Modifier.fillMaxSize().padding(padding)
            )
            is Resource.Error -> SearchFirstErrorState(
                message = (state as Resource.Error).message,
                onRetry = { viewModel.loadStocks() },
                modifier = Modifier.fillMaxSize().padding(padding)
            )
            is Resource.Success -> {
                val visibleStocks = when (selectedTab) {
                    HomeTab.GAINERS -> gainers
                    HomeTab.LOSERS -> losers
                    HomeTab.TRENDING -> trending
                }
                val marketPool = remember(gainers, losers, trending) {
                    (gainers + losers + trending).distinctBy { it.symbol }
                }
                val recommendedStocks = remember(marketPool) { marketPool.take(10) }

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        SearchHeaderCard(
                            query = searchQuery,
                            onQueryChange = { viewModel.setSearchQuery(it) },
                            onSearch = { viewModel.searchSymbols() }
                        )
                    }

                    when (searchState) {
                        is Resource.Loading -> item { InlineLoadingCard(text = "Searching symbols...") }
                        is Resource.Error -> item {
                            InlineErrorCard(
                                message = (searchState as Resource.Error).message ?: "Search error"
                            )
                        }
                        is Resource.Success -> {
                            val results = (searchState as Resource.Success).data
                            if (results.isNotEmpty()) {
                                item {
                                    SectionHeader(
                                        title = "Search Results",
                                        subtitle = "Tap a stock to open details"
                                    )
                                }
                                items(results, key = { it.symbol }) { result ->
                                    SearchResultCard(
                                        symbol = result.symbol,
                                        name = result.name,
                                        exchange = result.exchange,
                                        onClick = { onStockClick(result.symbol) }
                                    )
                                }
                            }
                        }
                        else -> Unit
                    }

                    item {
                        MarketPulseHero(
                            gainers = gainers,
                            losers = losers,
                            trending = trending,
                            isAiLoading = aiPicksState is Resource.Loading,
                            onAiClick = { viewModel.loadAiPicks() }
                        )
                    }

                    if (marketPool.isNotEmpty()) {
                        item {
                            LiveTickerStrip(
                                stocks = marketPool.take(10),
                                onStockClick = onStockClick
                            )
                        }
                    }

                    stickyHeader {
                        StickyMarketTabs(
                            selectedTab = selectedTab,
                            onTabSelected = { selectedTab = it }
                        )
                    }

                    item {
                        SectionHeader(
                            title = "Top Movers",
                            subtitle = "Quick-glance movers in the selected category"
                        )
                    }

                    item {
                        TopMoverChips(
                            stocks = visibleStocks,
                            onStockClick = onStockClick
                        )
                    }

                    item {
                        SectionHeader(
                            title = selectedTab.title,
                            subtitle = when (selectedTab) {
                                HomeTab.GAINERS -> "Stocks moving strongly upward today"
                                HomeTab.LOSERS -> "Stocks under selling pressure today"
                                HomeTab.TRENDING -> "Names drawing the most market attention"
                            }
                        )
                    }

                    item {
                        AnimatedContent(
                            targetState = selectedTab,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "home-tab-animated-content"
                        ) { tab ->
                            val tabStocks = when (tab) {
                                HomeTab.GAINERS -> gainers
                                HomeTab.LOSERS -> losers
                                HomeTab.TRENDING -> trending
                            }
                            if (tabStocks.isEmpty()) {
                                EmptyStocksState(message = "No data available")
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    tabStocks.forEach { stock ->
                                        ModernStockCard(stock = stock, onClick = { onStockClick(stock.symbol) })
                                    }
                                }
                            }
                        }
                    }

                    if (recommendedStocks.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "For You",
                                subtitle = "Names you may want to explore next"
                            )
                        }
                        item {
                            ForYouCarousel(
                                stocks = recommendedStocks,
                                onStockClick = onStockClick
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeSearchFirstTopBar(isRefreshing: Boolean, onRefresh: () -> Unit) {
    LargeTopAppBar(
        title = {
            Column {
                Text("EasyMoney", style = TradingTextStyles.Ticker, fontWeight = FontWeight.Bold)
                Text(
                    "Search-first investing dashboard",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f)
                )
            }
        },
        actions = {
            if (isRefreshing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp).padding(end = 6.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }
        },
        colors = TopAppBarDefaults.largeTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

@Composable
private fun SearchHeaderCard(query: String, onQueryChange: (String) -> Unit, onSearch: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = TradingShapes.HeroCard,
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text("Search the market", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Find stocks by symbol or company name first, then explore movers and ideas.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Search by symbol or company") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    shape = TradingShapes.Input
                )
                Button(
                    onClick = onSearch,
                    modifier = Modifier.height(56.dp),
                    shape = TradingShapes.Input,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Search", style = TradingTextStyles.Chip)
                }
            }
        }
    }
}

@Composable
private fun MarketPulseHero(
    gainers: List<Stock>,
    losers: List<Stock>,
    trending: List<Stock>,
    isAiLoading: Boolean,
    onAiClick: () -> Unit
) {
    val bullishCount = gainers.size
    val bearishCount = losers.size
    val totalBreadth = (bullishCount + bearishCount).coerceAtLeast(1)
    val bullishRatio = bullishCount.toFloat() / totalBreadth.toFloat()
    val topGainer = gainers.maxByOrNull { it.changePercent }
    val topLoser = losers.minByOrNull { it.changePercent }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(enabled = !isAiLoading, onClick = onAiClick),
        shape = TradingShapes.HeroCard,
        elevation = CardDefaults.cardElevation(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                )
            )
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Surface(shape = TradingShapes.Card, color = MaterialTheme.colorScheme.tertiaryContainer) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f),
                            modifier = Modifier.size(50.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (isAiLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isAiLoading) "Analyzing market..." else "AI Picks Today",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isAiLoading) {
                                    "Scanning leaders, laggards, and momentum names"
                                } else {
                                    "Get AI-generated ideas with quick reasoning and market context"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.84f)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text("Market Breadth", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Surface(shape = TradingShapes.Pill, color = MaterialTheme.colorScheme.surface) {
                    Box(modifier = Modifier.fillMaxWidth().height(14.dp)) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(14.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, TradingShapes.Pill)
                        )
                        Box(
                            modifier = Modifier.fillMaxWidth(bullishRatio).height(14.dp)
                                .background(GainGreen, TradingShapes.Pill)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Bullish: $bullishCount", style = MaterialTheme.typography.labelLarge, color = GainGreen, fontWeight = FontWeight.SemiBold)
                    Text("Bearish: $bearishCount", style = MaterialTheme.typography.labelLarge, color = LossRed, fontWeight = FontWeight.SemiBold)
                    Text("Trending: ${trending.size}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MarketSnapshotCard(
                        title = "Top Gainer",
                        symbol = topGainer?.symbol ?: "--",
                        movement = topGainer?.let { "+${String.format("%.2f", it.changePercent)}%" } ?: "--",
                        color = GainGreen,
                        modifier = Modifier.weight(1f)
                    )
                    MarketSnapshotCard(
                        title = "Top Loser",
                        symbol = topLoser?.symbol ?: "--",
                        movement = topLoser?.let { "${String.format("%.2f", it.changePercent)}%" } ?: "--",
                        color = LossRed,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MarketSnapshotCard(title: String, symbol: String, movement: String, color: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = TradingShapes.StatTile, color = color.copy(alpha = 0.10f)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = color)
            Text(symbol, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(movement, style = TradingTextStyles.PriceChange, color = color)
        }
    }
}

@Composable
private fun LiveTickerStrip(stocks: List<Stock>, onStockClick: (String) -> Unit) {
    if (stocks.isEmpty()) return
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        stocks.forEach { stock ->
            val isPositive = stock.changePercent >= 0
            val accent = if (isPositive) GainGreen else LossRed
            Surface(
                modifier = Modifier.clickable { onStockClick(stock.symbol) },
                shape = TradingShapes.Pill,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stock.symbol, style = TradingTextStyles.Chip, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${if (isPositive) "+" else ""}${String.format("%.2f", stock.changePercent)}%",
                        style = TradingTextStyles.Chip,
                        color = accent,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun StickyMarketTabs(selectedTab: HomeTab, onTabSelected: (HomeTab) -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.background) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HomeTab.entries.forEach { tab ->
                FilterChip(
                    selected = selectedTab == tab,
                    onClick = { onTabSelected(tab) },
                    label = { Text(tab.title, fontWeight = if (selectedTab == tab) FontWeight.SemiBold else FontWeight.Medium) },
                    shape = TradingShapes.Pill,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

@Composable
private fun TopMoverChips(stocks: List<Stock>, onStockClick: (String) -> Unit) {
    val compact = stocks.take(6)
    if (compact.isEmpty()) {
        EmptyStocksState(message = "No movers available")
        return
    }
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        compact.forEach { stock ->
            val isPositive = stock.changePercent >= 0
            val chipColor = if (isPositive) GainGreen else LossRed
            Surface(
                modifier = Modifier.clickable { onStockClick(stock.symbol) },
                shape = TradingShapes.Pill,
                color = chipColor.copy(alpha = 0.12f)
            ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stock.symbol, style = TradingTextStyles.Chip, fontWeight = FontWeight.Bold, color = chipColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${if (isPositive) "+" else ""}${String.format("%.2f", stock.changePercent)}%",
                        style = TradingTextStyles.Chip,
                        color = chipColor
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SearchResultCard(symbol: String, name: String, exchange: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = TradingShapes.Card,
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(symbol, style = TradingTextStyles.Ticker, fontWeight = FontWeight.Bold)
                if (name.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (exchange.isNotBlank()) {
                Surface(shape = TradingShapes.Pill, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)) {
                    Text(exchange, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun ModernStockCard(stock: Stock, onClick: () -> Unit) {
    val isPositive = stock.changePercent >= 0
    val changeColor = if (isPositive) GainGreen else LossRed
    val trendIcon = if (isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown
    val movementText = "${if (isPositive) "+" else ""}${String.format("%.2f", stock.changePercent)}%"

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = TradingShapes.HeroCard,
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(58.dp).background(
                            color = if (isPositive) GainGreen.copy(alpha = 0.15f) else LossRed.copy(alpha = 0.15f),
                            shape = TradingShapes.StatTile
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(trendIcon, contentDescription = null, tint = changeColor, modifier = Modifier.size(30.dp))
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stock.symbol, style = TradingTextStyles.Ticker, fontWeight = FontWeight.Bold)
                        if (stock.name.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(stock.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (stock.exchange.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(shape = TradingShapes.Pill, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)) {
                                Text(stock.exchange, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("$${String.format("%.2f", stock.currentPrice)}", style = TradingTextStyles.PriceHero, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(shape = TradingShapes.Pill, color = changeColor.copy(alpha = 0.12f)) {
                        Text(movementText, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = TradingTextStyles.Chip, color = changeColor, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            MiniMomentumBar(value = stock.changePercent, positiveColor = GainGreen, negativeColor = LossRed)
        }
    }
}

@Composable
private fun MiniMomentumBar(value: Double, positiveColor: Color, negativeColor: Color) {
    val positive = value >= 0
    val color = if (positive) positiveColor else negativeColor
    val normalized = (value.absoluteValue / 10.0).coerceIn(0.08, 1.0).toFloat()

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Momentum", style = TradingTextStyles.StatLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(shape = TradingShapes.Pill, color = MaterialTheme.colorScheme.surface) {
            Box(modifier = Modifier.fillMaxWidth().height(10.dp)) {
                Box(
                    modifier = Modifier.fillMaxWidth(normalized).height(10.dp).background(color, TradingShapes.Pill)
                )
            }
        }
    }
}

@Composable
private fun ForYouCarousel(stocks: List<Stock>, onStockClick: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(stocks, key = { it.symbol }) { stock ->
            ForYouCard(stock = stock, onClick = { onStockClick(stock.symbol) })
        }
    }
}

@Composable
private fun ForYouCard(stock: Stock, onClick: () -> Unit) {
    val isPositive = stock.changePercent >= 0
    val accent = if (isPositive) GainGreen else LossRed
    Card(
        modifier = Modifier.width(195.dp).clickable(onClick = onClick),
        shape = TradingShapes.CarouselCard,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(shape = TradingShapes.StatTile, color = accent.copy(alpha = 0.12f), modifier = Modifier.size(46.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Text(stock.symbol, style = TradingTextStyles.Ticker, fontWeight = FontWeight.Bold)
            if (stock.name.isNotBlank()) {
                Text(stock.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Text("$${String.format("%.2f", stock.currentPrice)}", style = TradingTextStyles.StatValue, fontWeight = FontWeight.SemiBold)
            Surface(shape = TradingShapes.Pill, color = accent.copy(alpha = 0.12f)) {
                Text(
                    text = "${if (isPositive) "+" else ""}${String.format("%.2f", stock.changePercent)}%",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = TradingTextStyles.Chip,
                    color = accent,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiPicksBottomSheetUltra(aiPicksState: Resource<List<AiPick>>, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Surface(shape = TradingShapes.BottomSheet, color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("AI Picks Today", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                when (aiPicksState) {
                    is Resource.Loading -> {
                        Box(modifier = Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                CircularProgressIndicator()
                                Text("Analyzing market data...", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    is Resource.Error -> InlineErrorCard(message = "⚠️ ${aiPicksState.message}")
                    is Resource.Success -> {
                        if (aiPicksState.data.isEmpty()) {
                            Text("No picks available right now.", style = MaterialTheme.typography.bodyMedium)
                        } else {
                            aiPicksState.data.forEachIndexed { index, pick ->
                                AiPickPremiumCard(rank = index + 1, pick = pick)
                            }
                        }
                        Text(
                            "⚠️ For informational purposes only. Not financial advice.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AiPickPremiumCard(rank: Int, pick: AiPick) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = TradingShapes.Card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(shape = TradingShapes.StatTile, color = MaterialTheme.colorScheme.tertiary) {
                Text(
                    text = "#$rank",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = TradingTextStyles.Chip,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiary
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(pick.symbol, style = TradingTextStyles.Ticker, fontWeight = FontWeight.Bold)
                Text(pick.reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
    }
}

@Composable
private fun InlineLoadingCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = TradingShapes.Card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun InlineErrorCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = TradingShapes.Card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(14.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

@Composable
private fun EmptyStocksState(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = TradingShapes.Card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SearchFirstLoadingState(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "home-loading")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "home-loading-alpha"
    )
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { ShimmerBlock(height = 120.dp, alpha = alpha) }
        item { ShimmerBlock(height = 250.dp, alpha = alpha) }
        item { ShimmerBlock(height = 56.dp, alpha = alpha) }
        item { ShimmerBlock(height = 54.dp, alpha = alpha) }
        item { ShimmerBlock(height = 70.dp, alpha = alpha) }
        items(5) { ShimmerBlock(height = 110.dp, alpha = alpha) }
        item { ShimmerBlock(height = 180.dp, alpha = alpha) }
    }
}

@Composable
private fun ShimmerBlock(height: Dp, alpha: Float) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = TradingShapes.Card,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(height).alpha(alpha)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
    }
}

@Composable
private fun SearchFirstErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Card(
            shape = TradingShapes.Dialog,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("⚠️ $message", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onErrorContainer)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onRetry, shape = TradingShapes.Input) { Text("Retry") }
            }
        }
    }
}
```

### `app/src/main/java/com/invest/easymoney/ui/home/HomeViewModel.kt`

```kotlin
package com.invest.easymoney.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.invest.easymoney.domain.model.AiPick
import com.invest.easymoney.domain.model.Stock
import com.invest.easymoney.domain.repository.AiInsightRepository
import com.invest.easymoney.domain.repository.StockRepository
import com.invest.easymoney.util.Constants
import com.invest.easymoney.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: StockRepository,
    private val aiRepository: AiInsightRepository
) : ViewModel() {

    private val _topStocksState = MutableStateFlow<Resource<List<Stock>>>(Resource.Loading)
    val topStocksState: StateFlow<Resource<List<Stock>>> = _topStocksState

    val gainers: StateFlow<List<Stock>> = _topStocksState
        .map { res -> (res as? Resource.Success)?.data?.sortedByDescending { it.changePercent }?.take(5) ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val losers: StateFlow<List<Stock>> = _topStocksState
        .map { res -> (res as? Resource.Success)?.data?.sortedBy { it.changePercent }?.take(5) ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val trending: StateFlow<List<Stock>> = _topStocksState
        .map { res ->
            (res as? Resource.Success)?.data
                ?.filter { it.symbol in Constants.TRENDING_STOCKS }
                ?.take(5) ?: emptyList()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** null = not requested yet; Loading/Success/Error = in-flight or done */
    private val _aiPicksState = MutableStateFlow<Resource<List<AiPick>>?>(null)
    val aiPicksState: StateFlow<Resource<List<AiPick>>?> = _aiPicksState

    private var aiPicksJob: Job? = null

    // Search state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery

    private val _searchState = MutableStateFlow<Resource<List<com.invest.easymoney.domain.model.StockSearchResult>>>(Resource.Success(emptyList()))
    val searchState = _searchState

    private val _popularSymbols = MutableStateFlow<List<String>>(emptyList())
    val popularSymbols: StateFlow<List<String>> = _popularSymbols

    init {
        loadStocks()
    }

    fun loadStocks() {
        viewModelScope.launch {
            _topStocksState.value = Resource.Loading
            _topStocksState.value = repository.getTopStocks()
        }
    }

    fun loadAiPicks() {
        if (aiPicksJob?.isActive == true) return
        aiPicksJob = viewModelScope.launch {
            _aiPicksState.value = Resource.Loading
            val stocks = (topStocksState.value as? Resource.Success)?.data
            if (stocks.isNullOrEmpty()) {
                _aiPicksState.value = Resource.Error("Load stock data first")
                return@launch
            }
            val topStocks = stocks.sortedByDescending { it.changePercent }.take(8)
            _aiPicksState.value = aiRepository.getAiPicks(topStocks)
        }
    }

    fun dismissAiPicks() {
        _aiPicksState.value = null
    }

    fun setSearchQuery(q: String) {
        _searchQuery.value = q
    }

    fun searchSymbols() {
        val q = _searchQuery.value.trim()
        if (q.isEmpty()) return
        viewModelScope.launch {
            _searchState.value = Resource.Loading
            _searchState.value = repository.searchSymbols(q)
        }
    }

    fun loadPopularSymbols() {
        viewModelScope.launch {
            when (val result = repository.getPopularStocksFromApi()) {
                is Resource.Success -> _popularSymbols.value = result.data ?: emptyList()
                else -> _popularSymbols.value = Constants.POPULAR_STOCKS
            }
        }
    }
}
```

### `app/src/main/java/com/invest/easymoney/ui/navigation/AppNavGraph.kt`

```kotlin
package com.invest.easymoney.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.invest.easymoney.ui.aihub.AiHubScreen
import com.invest.easymoney.ui.alerts.AlertsScreen
import com.invest.easymoney.ui.detail.StockDetailScreen
import com.invest.easymoney.ui.detail.StockDetailViewModel
import com.invest.easymoney.ui.home.HomeScreen
import com.invest.easymoney.ui.watchlist.WatchlistScreen

sealed class Screen(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Home : Screen("home", "Home", Icons.Filled.Home, Icons.Outlined.Home)
    data object Watchlist : Screen("watchlist", "Watchlist", Icons.Filled.Star, Icons.Outlined.StarBorder)
    data object AiHub : Screen("aihub", "AI Hub", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome)
    data object Alerts : Screen("alerts", "Alerts", Icons.Filled.Notifications, Icons.Outlined.Notifications)
}

val bottomNavItems = listOf(Screen.Home, Screen.Watchlist, Screen.AiHub, Screen.Alerts)

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { AppBottomBar(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onStockClick = { symbol ->
                        navController.navigate("detail/$symbol")
                    }
                )
            }
            composable(Screen.Watchlist.route) {
                WatchlistScreen(
                    onStockClick = { symbol ->
                        navController.navigate("detail/$symbol")
                    }
                )
            }
            composable(Screen.AiHub.route) {
                AiHubScreen()
            }
            composable(Screen.Alerts.route) {
                AlertsScreen()
            }
            composable(
                route = "detail/{symbol}",
                arguments = listOf(navArgument("symbol") { type = NavType.StringType })
            ) { backStackEntry ->
                val vm: StockDetailViewModel = hiltViewModel(backStackEntry)
                StockDetailScreen(onBack = { navController.popBackStack() }, viewModel = vm)
            }
        }
    }
}

@Composable
private fun AppBottomBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = bottomNavItems.any { screen ->
        currentDestination?.hierarchy?.any { it.route == screen.route } == true
    }
    if (!showBottomBar) return

    NavigationBar {
        bottomNavItems.forEach { screen ->
            val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
            NavigationBarItem(
                icon = {
                    Icon(
                        if (selected) screen.selectedIcon else screen.unselectedIcon,
                        contentDescription = screen.label
                    )
                },
                label = { Text(screen.label) },
                selected = selected,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}
```

### `app/src/main/java/com/invest/easymoney/ui/theme/Color.kt`

```kotlin
package com.invest.easymoney.ui.theme

import androidx.compose.ui.graphics.Color

// Brand
val NavyBlue = Color(0xFF0D1B3E)
val RoyalBlue = Color(0xFF1565C0)
val LightBlue = Color(0xFF42A5F5)

// Financial
val GainGreenLight = Color(0xFF43A047)


val GainGreen = Color(0xFF16C784)
val LossRed = Color(0xFFEA3943)
val LossRedLight = Color(0xFFE53935)

// Neutral
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceDark = Color(0xFF121212)
val BackgroundLight = Color(0xFFF5F7FA)
val BackgroundDark = Color(0xFF0A0A0A)
val CardLight = Color(0xFFFFFFFF)
val CardDark = Color(0xFF1E1E1E)

val OnPrimaryLight = Color(0xFFFFFFFF)
val OnSurfaceLight = Color(0xFF1A1A2E)
val OnSurfaceDark = Color(0xFFE0E0E0)
val SubtextLight = Color(0xFF757575)
val SubtextDark = Color(0xFF9E9E9E)

val Teal700 = Color(0xFF018786)
val Teal200 = Color(0xFF03DAC5)
val Green500 = Color(0xFF43A047)
val Green700 = Color(0xFF388E3C)
val Mint = Color(0xFFA8FFEB)
val Rose = Color(0xFFFF5C5C)
val Red500 = Color(0xFFE53935)
val Red700 = Color(0xFFB71C1C)
val Accent = Color(0xFF00BFAE)
val CardBg = Color(0xFFE0F2F1)
val Background = Color(0xFFF4F8FB)
val Surface = Color(0xFFFFFFFF)
val PrimaryText = Color(0xFF212121)
val SecondaryText = Color(0xFF757575)
val Divider = Color(0xFFBDBDBD)
```

### `app/src/main/java/com/invest/easymoney/ui/theme/shapes.kt`

```kotlin
package com.invest.easymoney.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.dp

/**
 * Premium shape system for EasyMoney.
 *
 * Design goals:
 * - Softer, more premium corners for cards and bottom sheets
 * - Consistent rounded language across chips, dialogs, lists, and chart containers
 * - Slightly larger defaults than Material 3 standard for a modern trading-app look
 */

val EasyMoneyShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

/**
 * Optional semantic shape tokens for feature-specific usage.
 *
 * These are useful when you want very intentional UI patterns:
 * - Hero cards
 * - Stat chips
 * - Search bars
 * - Bottom sheets
 * - Dialogs
 * - Chart containers
 */
@Immutable
object TradingShapes {
    /** Small inline elements like tags, compact chips, tiny badges */
    val Pill = RoundedCornerShape(999.dp)

    /** Input fields, compact cards, small sheets */
    val Input = RoundedCornerShape(18.dp)

    /** Standard list rows and medium cards */
    val Card = RoundedCornerShape(22.dp)

    /** Hero sections, featured surfaces, large tiles */
    val HeroCard = RoundedCornerShape(28.dp)

    /** Chart containers and premium dashboard panels */
    val ChartContainer = RoundedCornerShape(30.dp)

    /** Modal bottom sheets / large dialogs */
    val BottomSheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

    /** Dialogs with softer premium appearance */
    val Dialog = RoundedCornerShape(24.dp)

    /** Small stat tiles and compact summary cards */
    val StatTile = RoundedCornerShape(18.dp)

    /** Watchlist / recommendation carousel item */
    val CarouselCard = RoundedCornerShape(26.dp)

    /** Navigation rail / segmented backgrounds if needed later */
    val Panel = RoundedCornerShape(20.dp)
}
```

### `app/src/main/java/com/invest/easymoney/ui/theme/Theme.kt`

```kotlin
package com.invest.easymoney.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Brand / accent
private val Emerald = Color(0xFF00C896)
private val EmeraldDark = Color(0xFF00A67E)
private val ElectricBlue = Color(0xFF4C8DFF)
private val PremiumGold = Color(0xFFFFC857)

// Semantic trading colors
private val WarningAmber = Color(0xFFF5B700)

// Neutral surfaces (light)
private val LightBackground = Color(0xFFF6F8FB)
private val LightSurface = Color(0xFFFFFFFF)
private val LightSurfaceVariant = Color(0xFFEFF3F8)
private val LightTextPrimary = Color(0xFF111827)
private val LightTextSecondary = Color(0xFF6B7280)
private val LightOutline = Color(0xFFD0D7E2)
private val LightOutlineVariant = Color(0xFFE2E8F0)

// Neutral surfaces (dark)
private val DarkBackground = Color(0xFF0B1020)
private val DarkSurface = Color(0xFF111827)
private val DarkSurfaceVariant = Color(0xFF162033)
private val DarkTextPrimary = Color(0xFFF8FAFC)
private val DarkTextSecondary = Color(0xFF94A3B8)
private val DarkOutline = Color(0xFF334155)
private val DarkOutlineVariant = Color(0xFF243244)

// Error colors
private val ErrorLight = Color(0xFFD92D20)
private val ErrorDark = Color(0xFFFF6B6B)

private val EasyMoneyLightColorScheme = lightColorScheme(
    primary = Emerald,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7FFF0),
    onPrimaryContainer = Color(0xFF003829),
    secondary = ElectricBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE8FF),
    onSecondaryContainer = Color(0xFF0A2A66),
    tertiary = PremiumGold,
    onTertiary = Color(0xFF3F2A00),
    tertiaryContainer = Color(0xFFFFEAB5),
    onTertiaryContainer = Color(0xFF5A3D00),
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    error = ErrorLight,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    inverseSurface = DarkSurface,
    inverseOnSurface = DarkTextPrimary,
    inversePrimary = EmeraldDark,
    surfaceTint = Emerald,
    scrim = Color(0x66000000)
)

private val EasyMoneyDarkColorScheme = darkColorScheme(
    primary = Emerald,
    onPrimary = Color(0xFF00291E),
    primaryContainer = Color(0xFF004F39),
    onPrimaryContainer = Color(0xFFD7FFF0),
    secondary = ElectricBlue,
    onSecondary = Color(0xFF08214D),
    secondaryContainer = Color(0xFF163A7A),
    onSecondaryContainer = Color(0xFFDCE8FF),
    tertiary = PremiumGold,
    onTertiary = Color(0xFF3C2A00),
    tertiaryContainer = Color(0xFF6E4C00),
    onTertiaryContainer = Color(0xFFFFEAB5),
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    error = ErrorDark,
    onError = Color(0xFF410002),
    errorContainer = Color(0xFF5F1A18),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    inverseSurface = LightSurface,
    inverseOnSurface = LightTextPrimary,
    inversePrimary = EmeraldDark,
    surfaceTint = Emerald,
    scrim = Color(0x99000000)
)

@Immutable
data class TradingExtraColors(
    val gain: Color,
    val loss: Color,
    val neutral: Color,
    val bullishGlow: Color,
    val bearishGlow: Color,
    val warning: Color
)

private val LightTradingExtraColors = TradingExtraColors(
    gain = GainGreen,
    loss = LossRed,
    neutral = ElectricBlue,
    bullishGlow = GainGreen.copy(alpha = 0.12f),
    bearishGlow = LossRed.copy(alpha = 0.12f),
    warning = WarningAmber
)

private val DarkTradingExtraColors = TradingExtraColors(
    gain = GainGreen,
    loss = LossRed,
    neutral = ElectricBlue,
    bullishGlow = GainGreen.copy(alpha = 0.18f),
    bearishGlow = LossRed.copy(alpha = 0.18f),
    warning = WarningAmber
)

val LocalTradingExtraColors = staticCompositionLocalOf { LightTradingExtraColors }

val EasyMoneyTradingColors: TradingExtraColors
    @Composable
    get() = LocalTradingExtraColors.current

@Composable
fun EasyMoneyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> EasyMoneyDarkColorScheme
        else -> EasyMoneyLightColorScheme
    }

    val extras = if (darkTheme) DarkTradingExtraColors else LightTradingExtraColors

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()

            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(
        LocalTradingExtraColors provides extras
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = EasyMoneyShapes,
            content = content
        )
    }
}
```

### `app/src/main/java/com/invest/easymoney/ui/theme/Type.kt`

```kotlin
package com.invest.easymoney.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp

/**
 * Premium typography tuned for a modern trading / investing UI.
 *
 * Goals:
 * - Strong hierarchy for prices, tickers, and section headers
 * - Compact but readable body styles for dense financial data
 * - Clear labels for chips, stats, badges, and tables
 * - Good readability in both light and dark themes
 *
 * Notes:
 * - Uses the system/default font family to avoid dependency changes.
 * - If you later add a custom font (e.g. Inter / Plus Jakarta Sans),
 *   just replace AppFontFamily below.
 */
private val AppFontFamily = FontFamily.Default

val Typography = Typography(
    // DISPLAY — use sparingly for hero prices / high-impact values
    displayLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 40.sp,
        lineHeight = 46.sp,
        letterSpacing = (-0.5).sp
    ),
    displayMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.4).sp
    ),
    displaySmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.3).sp
    ),

    // HEADLINES — page titles, stock prices, strong summaries
    headlineLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.2).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.15).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 21.sp,
        lineHeight = 27.sp,
        letterSpacing = (-0.1).sp
    ),

    // TITLES — section headers, card titles, modal headers
    titleLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 23.sp,
        letterSpacing = 0.sp
    ),
    titleSmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.05.sp
    ),

    // BODY — descriptions, news summaries, chart annotations
    bodyLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.12.sp
    ),
    bodySmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.2.sp
    ),

    // LABELS — chips, tabs, badges, helper text, stat captions
    labelLarge = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.2.sp
    ),
    labelMedium = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.3.sp
    ),
    labelSmall = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.35.sp
    )
)

/**
 * Optional helper styles for especially dense financial UIs.
 *
 * These are not part of MaterialTheme.typography directly, but you can use them
 * in specific places where trading apps usually need tighter typography.
 */
object TradingTextStyles {
    val Ticker = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.2.sp
    )

    val PriceHero = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.35).sp
    )

    val PriceChange = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp
    )

    val StatValue = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.05.sp
    )

    val StatLabel = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.3.sp
    )

    val NewsHeadline = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.05.sp
    )

    val NewsMeta = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.3.sp
    )

    val Chip = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp
    )

    val Tiny = TextStyle(
        fontFamily = AppFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 12.sp,
        letterSpacing = 0.3.sp
    )
}
```

### `app/src/main/java/com/invest/easymoney/ui/watchlist/WatchlistScreen.kt`

```kotlin
package com.invest.easymoney.ui.watchlist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.invest.easymoney.domain.model.EarningsQuarter
import com.invest.easymoney.domain.model.EarningsReport
import com.invest.easymoney.domain.model.Stock
import com.invest.easymoney.domain.model.UpcomingEarning
import com.invest.easymoney.ui.theme.GainGreen
import com.invest.easymoney.ui.theme.LossRed
import com.invest.easymoney.util.Resource
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistScreen(
    onStockClick: (String) -> Unit,
    viewModel: WatchlistViewModel = hiltViewModel()
) {
    val state by viewModel.stocksState.collectAsStateWithLifecycle()
    val earningsMap by viewModel.earningsMap.collectAsStateWithLifecycle()
    val upcomingEarnings by viewModel.upcomingEarnings.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Watchlist", "Earnings")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Watchlist", fontWeight = FontWeight.Bold) },
                actions = {
                    if (state !is Resource.Loading) {
                        IconButton(onClick = { viewModel.refresh() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> WatchlistTab(
                    state = state,
                    onStockClick = onStockClick,
                    onRemove = { viewModel.removeFromWatchlist(it) }
                )
                1 -> EarningsTab(
                    stocksState = state,
                    earningsMap = earningsMap,
                    upcomingEarnings = upcomingEarnings
                )
            }
        }
    }
}

// ── Watchlist Tab ─────────────────────────────────────────────────────────────

@Composable
private fun WatchlistTab(
    state: Resource<List<Stock>>,
    onStockClick: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    when (state) {
        is Resource.Loading -> Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) { CircularProgressIndicator() }

        is Resource.Error -> Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("⚠️ ${(state as Resource.Error).message}")
        }

        is Resource.Success -> {
            val stocks = (state as Resource.Success<List<Stock>>).data
            if (stocks.isEmpty()) {
                EmptyWatchlistPlaceholder()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(stocks, key = { it.symbol }) { stock ->
                        WatchlistItem(
                            stock = stock,
                            onClick = { onStockClick(stock.symbol) },
                            onRemove = { onRemove(stock.symbol) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyWatchlistPlaceholder() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("⭐", style = MaterialTheme.typography.headlineLarge)
            Text(
                "Your watchlist is empty",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Tap ★ on any stock detail\nto add it here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WatchlistItem(stock: Stock, onClick: () -> Unit, onRemove: () -> Unit) {
    val isPositive = stock.changePercent >= 0
    val changeColor = if (isPositive) GainGreen else LossRed

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { it == SwipeToDismissBoxValue.EndToStart }
    )

    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            onRemove()
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(end = 20.dp)
                )
            }
        },
        enableDismissFromStartToEnd = false
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            onClick = onClick
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stock.symbol,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (stock.name.isNotEmpty()) {
                        Text(
                            text = stock.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (stock.currentPrice > 0) "$${String.format("%.2f", stock.currentPrice)}" else "—",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isPositive) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                            contentDescription = null,
                            tint = changeColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${if (isPositive) "+" else ""}${String.format("%.2f", stock.changePercent)}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = changeColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

// ── Earnings Tab ──────────────────────────────────────────────────────────────

@Composable
private fun EarningsTab(
    stocksState: Resource<List<Stock>>,
    earningsMap: Map<String, Resource<EarningsReport>>,
    upcomingEarnings: Resource<List<UpcomingEarning>>
) {
    val stocks = (stocksState as? Resource.Success)?.data ?: emptyList()
    val stockNames = stocks.associate { it.symbol to it.name }

    if (stocks.isEmpty()) {
        EmptyWatchlistPlaceholder()
        return
    }

    val upcoming = (upcomingEarnings as? Resource.Success)?.data ?: emptyList()
    val upcomingMap = upcoming.associate { it.symbol.uppercase() to it }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (upcoming.isNotEmpty()) {
            item { UpcomingEarningsBanner(upcoming) }
        }

        items(stocks, key = { "earnings_${it.symbol}" }) { stock ->
            val earningsState = earningsMap[stock.symbol]
            StockEarningsCard(
                symbol = stock.symbol,
                companyName = stockNames[stock.symbol] ?: "",
                earningsState = earningsState,
                upcomingEarning = upcomingMap[stock.symbol.uppercase()]
            )
        }
    }
}

@Composable
private fun UpcomingEarningsBanner(upcoming: List<UpcomingEarning>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "📅 Upcoming Earnings (Next 7 Days)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            upcoming.forEach { earning ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.tertiary
                        ) {
                            Text(
                                text = earning.symbol,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiary
                            )
                        }
                        Text(
                            text = formatDate(earning.date),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        earning.time?.let { time ->
                            Text(
                                text = when (time) {
                                    "bmo" -> "Pre-market"
                                    "amc" -> "After-close"
                                    else -> time
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                    earning.epsEstimate?.let { est ->
                        Text(
                            text = "Est. $${String.format("%.2f", est)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StockEarningsCard(
    symbol: String,
    companyName: String,
    earningsState: Resource<EarningsReport>?,
    upcomingEarning: UpcomingEarning?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Card header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = symbol,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    if (companyName.isNotEmpty()) {
                        Text(
                            text = companyName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
                Text(
                    "📊 EPS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Earnings content
            when (earningsState) {
                null, is Resource.Loading -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text(
                            "Loading earnings data…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is Resource.Error -> {
                    Text(
                        "⚠️ ${(earningsState as Resource.Error).message}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                is Resource.Success -> {
                    val report = (earningsState as Resource.Success<EarningsReport>).data
                    if (report.quarters.isEmpty()) {
                        Text(
                            "No earnings data available",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        EarningsQuartersTable(report.quarters)
                    }
                }
            }

            // Upcoming earnings date
            upcomingEarning?.let { upcoming ->
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "📅 Next Report",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = formatDate(upcoming.date),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                        upcoming.epsEstimate?.let { est ->
                            Text(
                                text = "Est. $${String.format("%.2f", est)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EarningsQuartersTable(quarters: List<EarningsQuarter>) {
    // Column headers
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        listOf("Quarter" to 1.2f, "Actual" to 1f, "Estimate" to 1f, "Result" to 1f).forEach { (label, weight) ->
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(weight),
                textAlign = if (label == "Quarter") TextAlign.Start else if (label == "Result") TextAlign.End else TextAlign.Center
            )
        }
    }

    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

    quarters.forEachIndexed { idx, quarter ->
        EarningsQuarterRow(quarter)
        if (idx < quarters.lastIndex) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        }
    }
}

@Composable
private fun EarningsQuarterRow(quarter: EarningsQuarter) {
    val beat = when {
        quarter.epsActual == null || quarter.epsEstimate == null -> null
        quarter.epsActual > quarter.epsEstimate -> true
        quarter.epsActual < quarter.epsEstimate -> false
        else -> null
    }
    val exactly = quarter.epsActual != null && quarter.epsEstimate != null &&
            quarter.epsActual == quarter.epsEstimate

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formatPeriodToQuarter(quarter.period),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1.2f)
        )
        Text(
            text = quarter.epsActual?.let { "$${String.format("%.2f", it)}" } ?: "—",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = when {
                beat == true -> GainGreen
                beat == false -> LossRed
                else -> MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        Text(
            text = quarter.epsEstimate?.let { "$${String.format("%.2f", it)}" } ?: "—",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            when {
                beat == true -> EarningsBadge("🟢 Beat", GainGreen)
                beat == false -> EarningsBadge("🔴 Miss", LossRed)
                exactly -> EarningsBadge("🟡 Met", Color(0xFFB8860B))
                else -> Text("—", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun EarningsBadge(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun formatPeriodToQuarter(period: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val date = sdf.parse(period) ?: return period
        val cal = java.util.Calendar.getInstance().apply { time = date }
        val month = cal.get(java.util.Calendar.MONTH) + 1
        val year = cal.get(java.util.Calendar.YEAR)
        val quarter = when (month) {
            in 1..3 -> "Q1"
            in 4..6 -> "Q2"
            in 7..9 -> "Q3"
            else -> "Q4"
        }
        "$quarter $year"
    } catch (e: Exception) {
        period
    }
}

private fun formatDate(dateStr: String): String {
    return try {
        val input = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val output = SimpleDateFormat("MMM d, yyyy", Locale.US)
        val date = input.parse(dateStr) ?: return dateStr
        output.format(date)
    } catch (e: Exception) {
        dateStr
    }
}
```

### `app/src/main/java/com/invest/easymoney/ui/watchlist/WatchlistViewModel.kt`

```kotlin
package com.invest.easymoney.ui.watchlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.invest.easymoney.domain.model.EarningsReport
import com.invest.easymoney.domain.model.Stock
import com.invest.easymoney.domain.model.UpcomingEarning
import com.invest.easymoney.domain.repository.BackendRepository
import com.invest.easymoney.domain.repository.StockRepository
import com.invest.easymoney.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WatchlistViewModel @Inject constructor(
    private val repository: StockRepository,
    private val backendRepository: BackendRepository
) : ViewModel() {

    private val _stocksState = MutableStateFlow<Resource<List<Stock>>>(Resource.Loading)
    val stocksState: StateFlow<Resource<List<Stock>>> = _stocksState

    private val _earningsMap = MutableStateFlow<Map<String, Resource<EarningsReport>>>(emptyMap())
    val earningsMap: StateFlow<Map<String, Resource<EarningsReport>>> = _earningsMap

    private val _upcomingEarnings = MutableStateFlow<Resource<List<UpcomingEarning>>>(Resource.Loading)
    val upcomingEarnings: StateFlow<Resource<List<UpcomingEarning>>> = _upcomingEarnings

    private val earningsJobs = mutableMapOf<String, Job>()

    init {
        viewModelScope.launch {
            repository.getWatchlistSymbols()
                .distinctUntilChanged()
                .collect { symbols ->
                    _stocksState.value = Resource.Loading
                    _stocksState.value = if (symbols.isEmpty()) {
                        Resource.Success(emptyList())
                    } else {
                        repository.fetchStocksForSymbols(symbols)
                    }

                    // Prune earnings for removed symbols
                    val symbolSet = symbols.toSet()
                    val removed = _earningsMap.value.keys - symbolSet
                    if (removed.isNotEmpty()) {
                        removed.forEach { sym -> earningsJobs[sym]?.cancel(); earningsJobs.remove(sym) }
                        _earningsMap.update { current -> current.filterKeys { it in symbolSet } }
                    }

                    // Load earnings for new symbols
                    symbols.forEach { symbol ->
                        if (!_earningsMap.value.containsKey(symbol)) {
                            earningsJobs[symbol]?.cancel()
                            earningsJobs[symbol] = launch {
                                _earningsMap.update { it + (symbol to Resource.Loading) }
                                val result = backendRepository.getEarningsReport(symbol)
                                _earningsMap.update { it + (symbol to result) }
                            }
                        }
                    }

                    // Load upcoming earnings for all watchlisted symbols
                    if (symbols.isNotEmpty()) {
                        launch {
                            _upcomingEarnings.value = Resource.Loading
                            _upcomingEarnings.value = backendRepository.getUpcomingEarnings(symbols)
                        }
                    } else {
                        _upcomingEarnings.value = Resource.Success(emptyList())
                    }
                }
        }
    }

    fun removeFromWatchlist(symbol: String) {
        viewModelScope.launch {
            repository.removeFromWatchlist(symbol)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val current = (_stocksState.value as? Resource.Success)?.data?.map { it.symbol } ?: return@launch
            if (current.isNotEmpty()) {
                _stocksState.value = Resource.Loading
                _stocksState.value = repository.fetchStocksForSymbols(current)

                // Reload all earnings
                earningsJobs.values.forEach { it.cancel() }
                earningsJobs.clear()
                _earningsMap.value = emptyMap()
                current.forEach { symbol ->
                    earningsJobs[symbol] = launch {
                        _earningsMap.update { it + (symbol to Resource.Loading) }
                        val result = backendRepository.getEarningsReport(symbol)
                        _earningsMap.update { it + (symbol to result) }
                    }
                }

                launch {
                    _upcomingEarnings.value = Resource.Loading
                    _upcomingEarnings.value = backendRepository.getUpcomingEarnings(current)
                }
            }
        }
    }
}
```

### `app/src/main/java/com/invest/easymoney/util/Constants.kt`

```kotlin
package com.invest.easymoney.util

object Constants {
    const val BASE_URL = "https://query1.finance.yahoo.com/"

    // Free, keyless OpenAI-compatible AI endpoint (Pollinations.ai)
    const val OPENAI_BASE_URL = "https://text.pollinations.ai/"

    // EasyMoney backend server (auto-detects emulator vs physical device)
    val BACKEND_BASE_URL: String
        get() {
            val isEmulator = android.os.Build.FINGERPRINT.startsWith("generic")
                    || android.os.Build.FINGERPRINT.startsWith("unknown")
                    || android.os.Build.MODEL.contains("google_sdk")
                    || android.os.Build.MODEL.contains("Emulator")
                    || android.os.Build.MODEL.contains("Android SDK built for x86")
                    || android.os.Build.MANUFACTURER.contains("Genymotion")
                    || android.os.Build.PRODUCT.contains("sdk_google")
                    || android.os.Build.PRODUCT.contains("google_sdk")
                    || android.os.Build.PRODUCT.contains("sdk")
                    || android.os.Build.PRODUCT.contains("sdk_x86")
                    || android.os.Build.PRODUCT.contains("vbox86p")
                    || android.os.Build.PRODUCT.contains("emulator")
                    || android.os.Build.PRODUCT.contains("simulator")
            return if (isEmulator) {
                "http://10.0.2.2:8080/"
            } else {
                "http://10.53.215.50:8080/"
            }
        }

    val POPULAR_STOCKS = listOf(
        "AAPL", "MSFT", "GOOGL", "AMZN", "NVDA", "META", "TSLA",
        "NFLX", "AMD", "INTC", "CRM", "ORCL", "ADBE", "JPM", "BAC",
        "WMT", "DIS", "SPOT", "UBER", "PYPL"
    )

    const val IEX_BASE_URL = "https://cloud.iexapis.com/stable/"
    const val IEX_API_TOKEN = "YOUR_IEX_TOKEN"

    val TRENDING_STOCKS = listOf("NVDA", "TSLA", "AAPL", "AMZN", "META")

    const val NOTIFICATION_CHANNEL_ID = "stock_alerts_channel"
    const val NOTIFICATION_CHANNEL_NAME = "Stock Price Alerts"
    const val ALERT_WORK_TAG = "alert_check_work"
}
```

### `app/src/main/java/com/invest/easymoney/util/Resource.kt`

```kotlin
package com.invest.easymoney.util

sealed class Resource<out T> {
    data class Success<T>(val data: T) : Resource<T>()
    data class Error(val message: String) : Resource<Nothing>()
    data object Loading : Resource<Nothing>()
}
```

### `app/src/main/java/com/invest/easymoney/worker/AlertCheckWorker.kt`

```kotlin
package com.invest.easymoney.worker

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.invest.easymoney.R
import com.invest.easymoney.domain.model.AlertType
import com.invest.easymoney.domain.repository.StockRepository
import com.invest.easymoney.util.Constants
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class AlertCheckWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val repository: StockRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val alerts = repository.getAllAlertsOnce()
            if (alerts.isEmpty()) return Result.success()

            // Group alerts by symbol to minimize API calls
            val symbolGroups = alerts.groupBy { it.symbol }
            val stockResults = repository.fetchStocksForSymbols(symbolGroups.keys.toList())
            val stocks = (stockResults as? com.invest.easymoney.util.Resource.Success)?.data ?: return Result.success()

            stocks.forEach { stock ->
                val stockAlerts = symbolGroups[stock.symbol] ?: return@forEach
                stockAlerts.forEach { alert ->
                    val triggered = when (alert.type) {
                        AlertType.INCREASE -> stock.changePercent >= alert.percentage
                        AlertType.DECREASE -> stock.changePercent <= -alert.percentage
                    }
                    if (triggered) {
                        val direction = if (alert.type == AlertType.INCREASE) "up" else "down"
                        showNotification(
                            title = "🔔 ${stock.symbol} Alert",
                            body = "${stock.symbol} is $direction ${String.format("%.2f", stock.changePercent)}% today!"
                        )
                    }
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun showNotification(title: String, body: String) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val notification = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
```

### `app/src/main/res/drawable/ic_launcher_background.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#0D1B3E"
        android:pathData="M0,0h108v108h-108z" />
    <path
        android:fillColor="#00000000"
        android:pathData="M9,0L9,108"
        android:strokeWidth="0.8"
        android:strokeColor="#1AFFFFFF" />
    <path
        android:fillColor="#00000000"
        android:pathData="M19,0L19,108"
        android:strokeWidth="0.8"
        android:strokeColor="#1AFFFFFF" />
    <path
        android:fillColor="#00000000"
        android:pathData="M29,0L29,108"
        android:strokeWidth="0.8"
        android:strokeColor="#1AFFFFFF" />
    <path
        android:fillColor="#00000000"
        android:pathData="M39,0L39,108"
        android:strokeWidth="0.8"
        android:strokeColor="#1AFFFFFF" />
    <path
        android:fillColor="#00000000"
        android:pathData="M49,0L49,108"
        android:strokeWidth="0.8"
        android:strokeColor="#1AFFFFFF" />
    <path
        android:fillColor="#00000000"
        android:pathData="M59,0L59,108"
        android:strokeWidth="0.8"
        android:strokeColor="#1AFFFFFF" />
    <path
        android:fillColor="#00000000"
        android:pathData="M69,0L69,108"
        android:strokeWidth="0.8"
        android:strokeColor="#1AFFFFFF" />
    <path
        android:fillColor="#00000000"
        android:pathData="M79,0L79,108"
        android:strokeWidth="0.8"
        android:strokeColor="#1AFFFFFF" />
    <path
        android:fillColor="#00000000"
        android:pathData="M89,0L89,108"
        android:strokeWidth="0.8"
        android:strokeColor="#1AFFFFFF" />
    <path
        android:fillColor="#00000000"
        android:pathData="M99,0L99,108"
        android:strokeWidth="0.8"
        android:strokeColor="#1AFFFFFF" />
    <path
        android:fillColor="#00000000"
        android:pathData="M0,9L108,9"
        android:strokeWidth="0.8"
        android:strokeColor="#1AFFFFFF" />
    <path
        android:fillColor="#00000000"
        android:pathData="M0,19L108,19"
        android:strokeWidth="0.8"
        android:strokeColor="#1AFFFFFF" />
    <path
        android:fillColor="#00000000"
        android:pathData="M0,29L108,29"
        android:strokeWidth="0.8"
        android:strokeColor="#1AFFFFFF" />
    <path
        android:fillColor="#00000000"
        android:pathData="M0,39L108,39"
        android:strokeWidth="0.8"
        android:strokeColor="#1AFFFFFF" />
    <path
        android:fillColor="#00000000"
        android:pathData="M0,49L108,49"
        android:strokeWidth="0.8"
        android:strokeColor="#1AFFFFFF" />
    <path
        android:fillColor="#00000000"
        android:pathData="M0,59L108,59"
        android:strokeWidth="0.8"
        android:strokeColor="#1AFFFFFF" />
    <path
        android:fillColor="#00000000"
        android:pathData="M0,69L108,69"
        android:strokeWidth="0.8"
        android:strokeColor="#1AFFFFFF" />
    <path
        android:fillColor="#00000000"
        android:pathData="M0,79L108,79"
        android:strokeWidth="0.8"
        android:strokeColor="#1AFFFFFF" />
    <path
        android:fillColor="#00000000"
        android:pathData="M0,89L108,89"
        android:strokeWidth="0.8"
        android:strokeColor="#1AFFFFFF" />
    <path
        android:fillColor="#00000000"
        android:pathData="M0,99L108,99"
        android:strokeWidth="0.8"
        android:strokeColor="#1AFFFFFF" />
</vector>
```

### `app/src/main/res/drawable/ic_launcher_foreground.xml`

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <!-- Trending Up Arrow/Graph for Investing -->
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M24,76L24,70L36,70L36,76L24,76ZM42,76L42,58L54,58L54,76L42,76ZM60,76L60,46L72,46L72,76L60,76ZM78,76L78,34L90,34L90,76L78,76Z" />
    <path
        android:strokeColor="#FFFFFF"
        android:strokeWidth="4"
        android:strokeLineCap="round"
        android:strokeLineJoin="round"
        android:pathData="M24,64L40,48L56,56L84,28" />
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M84,28L74,28L84,38Z" />
</vector>
```

### `app/src/main/res/drawable/ic_money_stack.xml`

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#43A047"
        android:pathData="M2,17h20v2H2zM2,11h20v2H2zM2,5h20v2H2z" />
    <path
        android:fillColor="#2E7D32"
        android:pathData="M7,12c0,0.55-0.45,1-1,1s-1-0.45-1-1s0.45-1,1-1s1,0.45,1,1zM18,12c0,0.55-0.45,1-1,1s-1-0.45-1-1s0.45-1,1-1s1,0.45,1,1zM7,6c0,0.55-0.45,1-1,1S5,6.55,5,6s0.45-1,1-1S7,5.45,7,6zM18,6c0,0.55-0.45,1-1,1s-1-0.45-1-1s0.45-1,1-1s1,0.45,1,1z" />
</vector>
```

### `app/src/main/res/drawable/marker_bg.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="#CC222222" />
    <corners android:radius="8dp" />
    <padding android:left="6dp" android:top="6dp" android:right="6dp" android:bottom="6dp" />
</shape>
```

### `app/src/main/res/layout/marker_view.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:background="@drawable/marker_bg"
    android:padding="8dp">

    <TextView
        android:id="@+id/marker_price"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:textColor="@android:color/white"
        android:textStyle="bold"
        android:textSize="14sp"
        tools:text="123.45" />

    <TextView
        android:id="@+id/marker_time"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:textColor="@android:color/white"
        android:textSize="12sp"
        tools:text="10:15" />

    <!-- Mini sparkline chart inside the marker -->
    <com.github.mikephil.charting.charts.LineChart
        android:id="@+id/marker_sparkline"
        android:layout_width="120dp"
        android:layout_height="48dp"
        android:layout_marginTop="6dp" />

    <TextView
        android:id="@+id/marker_volume"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:textColor="@android:color/white"
        android:textSize="12sp"
        android:layout_marginTop="4dp"
        tools:text="Vol: 1.2M" />

</LinearLayout>
```

### `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
```

### `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
```

### `app/src/main/res/values-night/themes.xml`

```xml
<resources>
    <style name="Theme.EasyMoney" parent="Theme.AppCompat.DayNight.NoActionBar">
        <item name="android:windowBackground">@android:color/black</item>
    </style>
</resources>
```

### `app/src/main/res/values/colors.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Teal and Green (primary/secondary) -->
    <color name="teal_200">#03DAC5</color>
    <color name="teal_700">#018786</color>
    <color name="green_500">#43A047</color>
    <color name="green_700">#388E3C</color>
    <color name="mint">#A8FFEB</color>

    <!-- Red (error/alert/accent) -->
    <color name="red_500">#E53935</color>
    <color name="red_700">#B71C1C</color>
    <color name="rose">#FF5C5C</color>

    <!-- White and backgrounds -->
    <color name="white">#FFFFFF</color>
    <color name="off_white">#F8F9FA</color>
    <color name="background">#F4F8FB</color>
    <color name="surface">#FFFFFF</color>
    <color name="card_bg">#E0F2F1</color>

    <!-- Accent and text -->
    <color name="accent">#00BFAE</color>
    <color name="primary_text">#212121</color>
    <color name="secondary_text">#757575</color>
    <color name="divider">#BDBDBD</color>

    <!-- Old colors for compatibility -->
    <color name="black">#000000</color>
    <color name="purple_200">#BB86FC</color>
    <color name="purple_500">#6200EE</color>
    <color name="purple_700">#3700B3</color>
</resources>
```

### `app/src/main/res/values/strings.xml`

```xml
<resources>
    <string name="app_name">EasyMoney</string>
</resources>
```

### `app/src/main/res/values/themes.xml`

```xml
<resources>
    <!-- Base theme for Compose app: AppCompat NoActionBar keeps it simple -->
    <style name="Theme.EasyMoney" parent="Theme.AppCompat.DayNight.NoActionBar">
        <item name="colorPrimary">@color/teal_700</item>
        <item name="colorPrimaryDark">@color/teal_700</item>
        <item name="colorAccent">@color/accent</item>
        <item name="android:windowBackground">@color/background</item>
        <item name="android:textColorPrimary">@color/primary_text</item>
        <item name="android:textColorSecondary">@color/secondary_text</item>
        <item name="colorError">@color/red_500</item>
    </style>
</resources>
```

### `app/src/main/res/xml/backup_rules.xml`

```xml
<?xml version="1.0" encoding="utf-8"?><!--
   Sample backup rules file; uncomment and customize as necessary.
   See https://developer.android.com/guide/topics/data/autobackup
   for details.
   Note: This file is ignored for devices older than API 31
   See https://developer.android.com/about/versions/12/backup-restore
-->
<full-backup-content>
    <!--
   <include domain="sharedpref" path="."/>
   <exclude domain="sharedpref" path="device.xml"/>
-->
</full-backup-content>
```

### `app/src/main/res/xml/data_extraction_rules.xml`

```xml
<?xml version="1.0" encoding="utf-8"?><!--
   Sample data extraction rules file; uncomment and customize as necessary.
   See https://developer.android.com/about/versions/12/backup-restore#xml-changes
   for details.
-->
<data-extraction-rules>
    <cloud-backup>
        <!-- TODO: Use <include> and <exclude> to control what is backed up.
        <include .../>
        <exclude .../>
        -->
    </cloud-backup>
    <!--
    <device-transfer>
        <include .../>
        <exclude .../>
    </device-transfer>
    -->
</data-extraction-rules>
```

### `app/src/test/java/com/invest/easymoney/ExampleUnitTest.kt`

```kotlin
package com.invest.easymoney

import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }
}
```

### `app/src/test/java/com/invest/easymoney/ui/charts/ChartMapperTest.kt`

```kotlin
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
```

### `BACKEND_COMMANDS.sh`

```bash
#!/bin/bash
# Quick Backend Commands

echo "EasyMoney Backend Quick Commands"
echo "================================"
echo ""
echo "1️⃣  START BACKEND"
echo "   /Users/Shubham.Dhamija/AndroidStudioProjects/EasyMoney/start-backend.sh"
echo ""
echo "2️⃣  TEST BACKEND"
echo "   curl http://localhost:8080/"
echo "   curl http://localhost:8080/ai/portfolio?risk=medium"
echo ""
echo "3️⃣  CHECK IF SERVER IS RUNNING"
echo "   ps aux | grep 'node index.js'"
echo ""
echo "4️⃣  STOP SERVER"
echo "   pkill -f 'node index.js'"
echo ""
echo "5️⃣  RESTART SERVER"
echo "   pkill -f 'node index.js'; sleep 2; /Users/Shubham.Dhamija/AndroidStudioProjects/EasyMoney/start-backend.sh"
echo ""
echo "6️⃣  VIEW SERVER LOGS (if running in background)"
echo "   tail -f /tmp/server.log"
echo ""
echo "7️⃣  GET YOUR MAC IP (for physical devices)"
echo "   ifconfig | grep 'inet ' | grep -v 127.0.0.1"
echo ""
echo "8️⃣  KILL PROCESS ON PORT 8080"
echo "   lsof -ti:8080 | xargs kill -9"
echo ""
echo "================================"
echo "Backend URL (Emulator): http://10.0.2.2:8080/"
echo "Backend URL (Device):   http://YOUR_MAC_IP:8080/"
echo "================================"
```

### `build.gradle.kts`

```kotlin
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.ksp) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.google.services) apply false
}
```

### `gradle.properties`

```properties
# Project-wide Gradle settings.
# IDE (e.g. Android Studio) users:
# Gradle settings configured through the IDE *will override*
# any settings specified in this file.
# For more details on how to configure your build environment visit
# http://www.gradle.org/docs/current/userguide/build_environment.html
# Specifies the JVM arguments used for the daemon process.
# The setting is particularly useful for tweaking memory settings.
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
# When configured, Gradle will run in incubating parallel mode.
# This option should only be used with decoupled projects. For more details, visit
# https://developer.android.com/r/tools/gradle-multi-project-decoupled-projects
# org.gradle.parallel=true
# AndroidX package structure to make it clearer which packages are bundled with the
# Android operating system, and which are packaged with your app's APK
# https://developer.android.com/topic/libraries/support-library/androidx-rn
android.useAndroidX=true
# Kotlin code style for this project: "official" or "obsolete":
kotlin.code.style=official
# Enables namespacing of each library's R class so that its R class includes only the
# resources declared in the library itself and none from the library's dependencies,
# thereby reducing the size of the R class for that library
android.nonTransitiveRClass=true
```

### `README.md`

```markdown
# EasyMoney
# EasyMoney
# EasyMoney
# EasyMoney
# EasyMoney
```

### `server/ai/eventDetector.js`

```javascript
const { EVENT_TYPES } = require('./eventTypes');

function detectPriceEvent(symbol, prices) {
  if (!prices || prices.length < 2) return null;
  const latest = prices[prices.length - 1];
  const previous = prices[0];
  const changePercent = ((latest - previous) / previous) * 100;
  if (changePercent >= 5) {
    return {
      type: EVENT_TYPES.PRICE_SPIKE,
      symbol,
      changePercent
    };
  }
  if (changePercent <= -5) {
    return {
      type: EVENT_TYPES.PRICE_DROP,
      symbol,
      changePercent
    };
  }
  return null;
}

// Add more detectors (analyst, earnings, news) as needed

module.exports = { detectPriceEvent };
```

### `server/ai/eventInsightService.js`

```javascript
const axios = require('axios');
const { getEventContext } = require('./eventRetriever');

async function generateEventInsight(event) {
  const { symbol, changePercent, type } = event;
  const context = await getEventContext(symbol);
  const prompt = `Stock: ${symbol}\nEvent: ${type}\nPrice Change: ${changePercent ? changePercent.toFixed(2) : 'N/A'}%\nContext:${context}\nTasks:\n1. Explain why this event happened\n2. Identify sentiment (bullish/bearish)\n3. Provide short-term outlook\nKeep it concise and factual.\n\nDisclaimer: This is AI-generated analysis, not financial advice.`;

  // Use OpenAI or Pollinations
  const provider = process.env.LLM_PROVIDER || (process.env.OPENAI_API_KEY ? 'openai' : 'pollinations');
  if (provider === 'openai') {
    const urlBase = process.env.OPENAI_URL || 'https://api.openai.com';
    const resp = await axios.post(`${urlBase}/v1/chat/completions`, {
      model: process.env.LLM_MODEL || 'gpt-4o-mini',
      messages: [
        { role: 'system', content: 'You are a financial AI assistant.' },
        { role: 'user', content: prompt }
      ],
      max_tokens: 300
    }, {
      headers: { Authorization: `Bearer ${process.env.OPENAI_API_KEY}` },
      timeout: 30000
    });
    return resp.data.choices?.[0]?.message?.content;
  }
  // Pollinations fallback
  const url = (process.env.OPENAI_URL || 'https://text.pollinations.ai') + '/openai';
  const resp = await axios.post(url, {
    model: process.env.LLM_MODEL || 'openai',
    messages: [
      { role: 'system', content: 'You are a financial AI assistant.' },
      { role: 'user', content: prompt }
    ]
  }, { timeout: 30000 });
  return resp.data?.choices?.[0]?.message?.content || resp.data;
}

module.exports = { generateEventInsight };
```

### `server/ai/eventProcessor.js`

```javascript
const { generateEventInsight } = require('./eventInsightService');
const { sendNotification } = require('../notificationService');

// Deduplication cache (in-memory, for demo)
const recentEvents = new Map();
const EVENT_TTL_MS = 60 * 60 * 1000; // 1 hour

function isDuplicate(event) {
  const key = `${event.type}:${event.symbol}`;
  const now = Date.now();
  if (recentEvents.has(key) && now - recentEvents.get(key) < EVENT_TTL_MS) return true;
  recentEvents.set(key, now);
  return false;
}

async function handleEvent(event, users) {
  try {
    if (isDuplicate(event)) {
      console.log('Duplicate event, skipping:', event);
      return;
    }
    const insight = await generateEventInsight(event);
    for (const user of users) {
      if (!user.deviceToken) continue;
      await sendNotification(
        user.deviceToken,
        `📊 ${event.symbol} Alert`,
        (insight || '').substring(0, 200)
      );
    }
    // Log event
    console.log('Event processed:', event, 'Insight:', insight);
  } catch (err) {
    console.error('Event processing failed:', err);
  }
}

module.exports = { handleEvent };
```

### `server/ai/eventRetriever.js`

```javascript
const { retrieveOptimized } = require('../optimizedRetriever');

async function getEventContext(symbol) {
  const docs = await retrieveOptimized(symbol);
  return docs.join('\n');
}

module.exports = { getEventContext };
```

### `server/ai/eventTypes.js`

```javascript
const EVENT_TYPES = {
  PRICE_SPIKE: 'price_spike',
  PRICE_DROP: 'price_drop',
  ANALYST_UPDATE: 'analyst_update',
  EARNINGS: 'earnings',
  NEWS: 'news'
};

module.exports = { EVENT_TYPES };
```

### `server/db/schema.sql`

```sql
-- Documents table for RAG
CREATE TABLE documents (
  id UUID PRIMARY KEY,
  stock_symbol TEXT,
  content TEXT,
  embedding BYTEA,
  type TEXT,
  created_at TIMESTAMP DEFAULT now()
);

-- Events table (optional)
CREATE TABLE events (
  id UUID PRIMARY KEY,
  type TEXT,
  payload JSONB,
  processed_at TIMESTAMP
);
```

### `server/embeddingService.js`

```javascript
const axios = require('axios');

/**
 * createEmbedding(text)
 * - Uses OpenAI embeddings when OPENAI_API_KEY is provided.
 * - Otherwise returns a deterministic dummy vector for local dev.
 *
 * Inputs: text string
 * Outputs: Float32Array (JS array of numbers)
 */
async function createEmbedding(text, opts = {}) {
  const provider = process.env.EMBEDDING_PROVIDER || (process.env.OPENAI_API_KEY ? 'openai' : 'dummy');

  if (provider === 'openai') {
    const urlBase = process.env.OPENAI_URL || 'https://api.openai.com';
    const model = opts.model || process.env.EMBEDDING_MODEL || 'text-embedding-3-small';
    const resp = await axios.post(`${urlBase}/v1/embeddings`, {
      input: text,
      model
    }, {
      headers: {
        'Authorization': `Bearer ${process.env.OPENAI_API_KEY}`,
        'Content-Type': 'application/json'
      },
      timeout: 15000
    });

    // OpenAI response shape: { data: [ { embedding: [...] } ] }
    const embedding = resp.data?.data?.[0]?.embedding;
    if (!embedding) throw new Error('No embedding returned from OpenAI');
    return embedding;
  }

  // Dummy deterministic embedding: map chars to small floats. Dimension 1536 by default.
  const dim = opts.dim || parseInt(process.env.EMBEDDING_DIM || '1536', 10);
  const out = new Array(dim);
  for (let i = 0; i < dim; i++) {
    const code = text.charCodeAt(i % text.length) || 0;
    out[i] = Math.sin((code + i) % 100) / 100.0;
  }
  return out;
}

module.exports = { createEmbedding };
```

### `server/eventProcessor.js`

```javascript
const cron = require('node-cron');
const insightService = require('./insightService');

// Example events: "earnings", "analyst_update", "price_spike", "breaking_news"

async function handleEvent(type, payload) {
  console.log('Event received', type, payload);
  // Basic routing — fetch context, run RAG, generate insight, notify
  switch (type) {
    case 'earnings':
    case 'analyst_update':
    case 'breaking_news':
    case 'price_spike':
      return await processMarketEvent(type, payload);
    default:
      throw new Error('Unknown event type: ' + type);
  }
}

async function processMarketEvent(type, payload) {
  // payload should include { symbol }
  const symbol = payload.symbol;
  // 1. gather context (news, docs, past insights)
  const context = await insightService.fetchContextForSymbol(symbol);
  // 2. run RAG / generate insight
  const insight = await insightService.generateInsight(symbol, { eventType: type, payload, context });
  // 3. TODO: send notification (push / email)
  console.log('Generated insight for', symbol, insight.shortSummary || insight);
  return insight;
}

// Example cron job to run every 5 minutes and check custom conditions
cron.schedule('*/5 * * * *', async () => {
  console.log('Cron: checking market events...');
  try {
    // TODO: implement polling logic (price movers, news feed, analyst API)
  } catch (e) {
    console.error('Cron error', e);
  }
});

module.exports = { handleEvent };
```

### `server/index.js`

```javascript
require('dotenv').config();
const express = require('express');
const bodyParser = require('body-parser');
const axios = require('axios');
const cron = require('node-cron');
const eventProcessor = require('./eventProcessor');
const insightService = require('./insightService');
const explainService = require('./explainService');
const watchlistService = require('./watchlistService');
const portfolioService = require('./portfolioService');
const signalEngine = require('./signalEngine');
const investorTracker = require('./investorTracker');
const sectorAnalysis = require('./sectorAnalysis');
const simulationEngine = require('./simulationEngine');
// Phase 5
const copilotService = require('./copilotService');
const userProfileService = require('./userProfileService');
const communityService = require('./communityService');
const predictionEngine = require('./predictionEngine');
// New services
const earningsService = require('./earningsService');
const moversService = require('./moversService');
const { buildFullContext } = require('./systemOrchestrator');

const app = express();
app.use(bodyParser.json());

app.get('/', (req, res) => res.send('EasyMoney AI Server'));

// Trigger an event for testing
app.post('/event', async (req, res) => {
  try {
    const { type, payload } = req.body;
    const result = await eventProcessor.handleEvent(type, payload);
    res.json({ ok: true, result });
  } catch (e) {
    console.error(e);
    res.status(500).json({ ok: false, error: e.message });
  }
});

// Generate insight endpoint
app.post('/ai/insight', async (req, res) => {
  try {
    const { symbol, context } = req.body;
    const insight = await insightService.generateInsight(symbol, context || {});
    res.json({ ok: true, insight });
  } catch (e) {
    console.error(e);
    res.status(500).json({ ok: false, error: e.message });
  }
});

// Ingest news articles for a symbol and store as vectors
app.post('/ai/process-news', async (req, res) => {
  try {
    const { symbol, news } = req.body;
    if (!symbol || !Array.isArray(news)) return res.status(400).json({ ok: false, error: 'Missing symbol or news array' });
    const result = await insightService.processNews(news, symbol);
    res.json({ ok: true, result });
  } catch (e) {
    console.error(e);
    res.status(500).json({ ok: false, error: e.message });
  }
});

// Watchlist summary
app.get('/ai/watchlist-summary', async (req, res) => {
  try {
    const userId = req.query.userId;
    const summary = await insightService.watchlistSummary(userId);
    res.json({ ok: true, summary });
  } catch (e) {
    console.error(e);
    res.status(500).json({ ok: false, error: e.message });
  }
});

// Watchlist summary (per-user)
app.get('/ai/watchlist-summary/:userId', async (req, res) => {
  try {
    const { userId } = req.params;
    const summary = await watchlistService.generateWatchlistSummary(userId);
    res.json({ ok: true, summary });
  } catch (e) {
    console.error(e);
    res.status(500).json({ ok: false, error: e.message });
  }
});

// Portfolio recommendation
app.get('/ai/portfolio', async (req, res) => {
  try {
    const { risk = 'medium', symbols, userId = 'default' } = req.query;
    const symbolList = symbols ? symbols.split(',').map(s => s.trim()) : [];
    const out = await portfolioService.generatePortfolio({ risk, symbols: symbolList, userId });
    res.json({ ok: true, ...out });
  } catch (e) {
    res.status(500).json({ ok: false, error: e.message });
  }
});

// Signals (RAG-enhanced)
app.post('/ai/signal', async (req, res) => {
  try {
    const { symbol, marketData } = req.body;
    const out = await signalEngine.analyzeSymbol(symbol, marketData || {});
    res.json({ ok: true, ...out });
  } catch (e) {
    res.status(500).json({ ok: false, error: e.message });
  }
});

// Investor tracker
app.get('/ai/investor/:name', async (req, res) => {
  try {
    const out = await investorTracker.getTopBuysByInstitution(req.params.name);
    res.json({ ok: true, out });
  } catch (e) {
    res.status(500).json({ ok: false, error: e.message });
  }
});

// Sector analysis
app.get('/ai/sectors', async (req, res) => {
  try {
    const out = await sectorAnalysis.analyzeSectors();
    res.json({ ok: true, out });
  } catch (e) {
    res.status(500).json({ ok: false, error: e.message });
  }
});

// Stock search (Finnhub-backed)
app.get('/stocks/search', async (req, res) => {
  try {
    const { q } = req.query;
    if (!q) return res.status(400).json({ ok: false, error: 'Missing query param q' });
    const apiKey = process.env.FINNHUB_API_KEY;
    if (!apiKey) return res.status(503).json({ ok: false, error: 'FINNHUB_API_KEY not configured' });
    const response = await axios.get(
      `https://finnhub.io/api/v1/search?q=${encodeURIComponent(q)}&token=${apiKey}`,
      { timeout: 10000 }
    );
    const results = (response.data.result || [])
      .filter(s => s.type === 'Common Stock' || !s.type)
      .slice(0, 20)
      .map(s => ({ symbol: s.symbol, name: s.description, exchange: s.primaryExchange || '' }));
    res.json({ ok: true, results });
  } catch (e) {
    console.error(e);
    res.status(500).json({ ok: false, error: e.message });
  }
});

// "Why did this stock move?" — RAG-powered explanation
app.get('/ai/explain/:symbol', async (req, res) => {
  try {
    const { symbol } = req.params;
    const explanation = await explainService.explainStockMove(symbol.toUpperCase());
    res.json({ ok: true, explanation });
  } catch (e) {
    console.error(e);
    res.status(500).json({ ok: false, error: e.message });
  }
});

// Simulation endpoints
app.post('/ai/simulate/past', async (req, res) => {
  try {
    const { prices, investment } = req.body;
    const out = simulationEngine.simulatePastReturn(prices, investment || 1000);
    res.json({ ok: true, value: out });
  } catch (e) {
    res.status(500).json({ ok: false, error: e.message });
  }
});

app.post('/ai/simulate/future', async (req, res) => {
  try {
    const { currentPrice, expectedReturnPct, investment } = req.body;
    const out = simulationEngine.projectSimpleReturn(currentPrice, expectedReturnPct || 5, investment || 1000);
    res.json({ ok: true, out });
  } catch (e) {
    res.status(500).json({ ok: false, error: e.message });
  }
});

// ── Phase 5: AI Copilot ───────────────────────────────────────────────────────
app.post('/ai/copilot', async (req, res) => {
  try {
    const { userId = 'default', question, messages = [], watchlist = [], riskProfile = 'medium' } = req.body;
    if (!question) return res.status(400).json({ ok: false, error: 'Missing question' });
    const result = await copilotService.answerQuestion({ userId, question, messages, watchlist, riskProfile });
    res.json({ ok: true, ...result });
  } catch (e) {
    console.error(e);
    res.status(500).json({ ok: false, error: e.message });
  }
});

// ── Phase 5: User Profile ─────────────────────────────────────────────────────
app.get('/user/profile/:userId', (req, res) => {
  try {
    const profile = userProfileService.getProfile(req.params.userId);
    res.json({ ok: true, profile });
  } catch (e) {
    res.status(500).json({ ok: false, error: e.message });
  }
});

app.post('/user/profile/:userId', (req, res) => {
  try {
    const profile = userProfileService.updateProfile(req.params.userId, req.body);
    res.json({ ok: true, profile });
  } catch (e) {
    res.status(500).json({ ok: false, error: e.message });
  }
});

app.post('/user/search/:userId', (req, res) => {
  try {
    const { symbol } = req.body;
    userProfileService.recordSearch(req.params.userId, symbol);
    res.json({ ok: true });
  } catch (e) {
    res.status(500).json({ ok: false, error: e.message });
  }
});

// ── Phase 5: Community ────────────────────────────────────────────────────────
app.get('/community/trending', (req, res) => {
  try {
    const limit = parseInt(req.query.limit || '10', 10);
    const trending = communityService.getTrending(limit);
    res.json({ ok: true, trending });
  } catch (e) {
    res.status(500).json({ ok: false, error: e.message });
  }
});

app.post('/community/watchlist', (req, res) => {
  try {
    const { userId, symbol, action } = req.body;
    if (!userId || !symbol || !['add', 'remove'].includes(action)) {
      return res.status(400).json({ ok: false, error: 'Missing userId, symbol, or action (add|remove)' });
    }
    communityService.recordWatchlistChange(userId, symbol, action);
    res.json({ ok: true });
  } catch (e) {
    res.status(500).json({ ok: false, error: e.message });
  }
});

// ── Phase 5: Prediction Engine ────────────────────────────────────────────────
app.get('/ai/predict/:symbol', async (req, res) => {
  try {
    const result = await predictionEngine.predictSymbol(req.params.symbol);
    res.json({ ok: true, ...result });
  } catch (e) {
    res.status(500).json({ ok: false, error: e.message });
  }
});

const port = process.env.PORT || 8080;
app.listen(port, '0.0.0.0', () => console.log('EasyMoney AI Server listening on', port));

// ── New Endpoints ─────────────────────────────────────────────────────────────

// Full AI context for a symbol (orchestrator)
app.get('/ai/context/:symbol', async (req, res) => {
  try {
    const { symbol } = req.params;
    const { userId = 'default' } = req.query;
    const ctx = await buildFullContext(symbol.toUpperCase(), userId);
    res.json({ ok: true, symbol: symbol.toUpperCase(), ...ctx });
  } catch (e) {
    console.error(e);
    res.status(500).json({ ok: false, error: e.message });
  }
});

// Upcoming earnings calendar — must be registered BEFORE the :symbol wildcard route
app.get('/ai/earnings/upcoming', async (req, res) => {
  try {
    const today = new Date();
    const from = req.query.from || today.toISOString().split('T')[0];
    const toDate = new Date(today);
    toDate.setDate(toDate.getDate() + 7);
    const to = req.query.to || toDate.toISOString().split('T')[0];
    const result = await earningsService.getUpcomingEarnings(from, to);
    res.json({ ok: true, ...result });
  } catch (e) {
    console.error(e);
    res.status(500).json({ ok: false, error: e.message });
  }
});

// Historical earnings for a symbol
app.get('/ai/earnings/:symbol', async (req, res) => {
  try {
    const result = await earningsService.getEarnings(req.params.symbol.toUpperCase());
    res.json({ ok: true, ...result });
  } catch (e) {
    console.error(e);
    res.status(500).json({ ok: false, error: e.message });
  }
});

// Ingest earnings into RAG for a symbol
app.post('/ai/earnings/ingest/:symbol', async (req, res) => {
  try {
    const result = await earningsService.ingestEarningsToRAG(req.params.symbol.toUpperCase());
    res.json({ ok: true, ...result });
  } catch (e) {
    console.error(e);
    res.status(500).json({ ok: false, error: e.message });
  }
});

// Live market movers (gainers + losers + most active)
app.get('/stocks/movers', (req, res) => {
  try {
    const limit = parseInt(req.query.limit || '10', 10);
    const data = moversService.getMovers(limit);
    res.json({ ok: true, ...data });
  } catch (e) {
    res.status(500).json({ ok: false, error: e.message });
  }
});

app.get('/stocks/gainers', (req, res) => {
  try {
    const limit = parseInt(req.query.limit || '10', 10);
    res.json({ ok: true, gainers: moversService.getGainers(limit), updatedAt: moversService.getCacheAge() });
  } catch (e) {
    res.status(500).json({ ok: false, error: e.message });
  }
});

app.get('/stocks/losers', (req, res) => {
  try {
    const limit = parseInt(req.query.limit || '10', 10);
    res.json({ ok: true, losers: moversService.getLosers(limit), updatedAt: moversService.getCacheAge() });
  } catch (e) {
    res.status(500).json({ ok: false, error: e.message });
  }
});

// ── Cron Jobs ─────────────────────────────────────────────────────────────────

// Refresh gainers/losers every 5 minutes during market hours (Mon-Fri)
cron.schedule('*/5 9-16 * * 1-5', async () => {
  try { await moversService.updateMovers(); }
  catch (e) { console.error('[cron] updateMovers failed:', e.message); }
});

// Initial fetch on startup (non-blocking)
moversService.updateMovers().catch(e => console.warn('[startup] movers initial fetch:', e.message));

// Ingest earnings for watchlist symbols daily at 6am
cron.schedule('0 6 * * 1-5', async () => {
  try {
    const watchlist = process.env.SAMPLE_WATCHLIST
      ? process.env.SAMPLE_WATCHLIST.split(',').map(s => s.trim()).filter(Boolean)
      : ['AAPL', 'NVDA', 'TSLA', 'MSFT', 'AMZN'];
    console.log('[cron] Ingesting earnings for', watchlist);
    for (const sym of watchlist) {
      await earningsService.ingestEarningsToRAG(sym).catch(e => console.warn(`earnings ingest ${sym}:`, e.message));
    }
  } catch (e) {
    console.error('[cron] earnings ingestion failed:', e.message);
  }
});
```

### `server/insightService.js`

```javascript
const embeddingService = require('./embeddingService');
const vectorStore = require('./vectorStore');
const { v4: uuidv4 } = require('uuid');

// Simple RAG flow: embed query, get top docs, call LLM (user can wire provider)

async function fetchContextForSymbol(symbol) {
  // Retrieve top 5 relevant docs from vector store
  const queryEmbedding = await embeddingService.createEmbedding(symbol);
  const matches = await vectorStore.querySimilar(queryEmbedding, 5, { symbol: { "$eq": symbol } });
  return matches.map(m => ({ id: m.id, text: m.metadata?.text || '', score: m.score }));
}

async function generateInsight(symbol, opts = {}) {
  const contextDocs = opts.context || (await fetchContextForSymbol(symbol));
  const contextText = contextDocs.map(d => d.text).join('\n').slice(0, 16_000);
  const shortSummary = `Auto-insight for ${symbol}: ${contextText ? contextText.substring(0, 240) + (contextText.length > 240 ? '...' : '') : 'No context available.'}`;
  return { shortSummary, context: contextDocs };
}

function normalizeText(symbol, headline, summary, source) {
  return `Stock: ${symbol}\nHeadline: ${headline}\nSummary: ${summary}\nSource: ${source}`;
}

async function processNews(newsList, symbol) {
  for (const article of newsList) {
    // Avoid duplicates by id if provided
    const id = article.id ? article.id.toString() : uuidv4();
    const type = article.type || 'news';
    const sentiment = article.sentiment || 'neutral';
    const timestamp = article.timestamp || article.datetime || Date.now();
    const source = article.source || 'unknown';
    const headline = article.headline || '';
    const summary = article.summary || article.content || '';
    const text = normalizeText(symbol, headline, summary, source);
    const embedding = await embeddingService.createEmbedding(text);
    const metadata = { symbol, type, sentiment, timestamp, source, text, summary };
    await vectorStore.upsertDocument(id, embedding, metadata);
  }
  return { ok: true, count: newsList.length };
}

async function watchlistSummary(userId) {
  // Placeholder: in production, fetch user watchlist and create aggregated RAG per symbol
  return { userId, summary: 'Your watchlist summary is not configured yet.' };
}

module.exports = { fetchContextForSymbol, generateInsight, processNews, watchlistSummary };
```

### `server/investorTracker.js`

```javascript
// Very small stub for investor tracking — in production you'd ingest 13F filings

async function getTopBuysByInstitution(institution = 'Berkshire') {
  // Placeholder: return sample data
  return [
    { symbol: 'OXY', shares: 1000000, action: 'buy' },
    { symbol: 'BRK.B', shares: 50000, action: 'buy' }
  ];
}

module.exports = { getTopBuysByInstitution };
```

### `server/notificationService.js`

```javascript
const admin = require('firebase-admin');
const fs = require('fs');

let initialized = false;
function initFirebase() {
  if (initialized) return;
  const keyPath = process.env.FIREBASE_SERVICE_ACCOUNT_JSON || '';
  if (keyPath && fs.existsSync(keyPath)) {
    const serviceAccount = require(keyPath);
    admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });
    initialized = true;
  }
}

async function sendFCM(token, message) {
  try {
    initFirebase();
    if (!initialized) {
      console.log('FCM not configured; message would be:', token, message);
      return { ok: false, reason: 'not-configured' };
    }
    const payload = {
      token,
      notification: {
        title: message.title,
        body: message.body
      }
    };
    const resp = await admin.messaging().send(payload);
    return { ok: true, resp };
  } catch (e) {
    console.error('FCM error', e.message || e);
    return { ok: false, reason: e.message };
  }
}

async function sendNotification(token, title, body) {
  // For FCM, message = { title, body }
  return sendFCM(token, { title, body });
}

module.exports = { sendFCM, sendNotification };
```

### `server/optimizedRetriever.js`

```javascript
const embeddingService = require('./embeddingService');
const vectorStore = require('./vectorStore');

// Advanced retriever: multi-query, dedup, re-rank, recency boost, context compression
async function retrieveOptimized(symbol) {
  const queries = [
    `latest news about ${symbol}`,
    `${symbol} analyst rating changes`,
    `${symbol} earnings report impact`
  ];
  const types = ['news', 'analyst', 'earnings'];
  // Run all queries in parallel for each type
  const results = await Promise.all(queries.map((q, i) => (
    embeddingService.createEmbedding(q).then(embedding =>
      vectorStore.querySimilar(embedding, 3, { symbol: { "$eq": symbol }, type: { "$in": [types[i]] } }, types[i])
    )
  )));
  let allResults = results.flat();
  // Deduplicate by id
  const unique = Object.values(
    allResults.reduce((acc, item) => {
      acc[item.id] = item;
      return acc;
    }, {})
  );
  // Re-rank: similarity, recency, importance
  const now = Date.now();
  unique.sort((a, b) => {
    const recencyA = now - (a.metadata?.timestamp || 0);
    const recencyB = now - (b.metadata?.timestamp || 0);
    const importanceA = a.metadata?.type === 'analyst' ? 2 : a.metadata?.type === 'earnings' ? 1.5 : 1;
    const importanceB = b.metadata?.type === 'analyst' ? 2 : b.metadata?.type === 'earnings' ? 1.5 : 1;
    const scoreA = (a.score || 0) * 0.6 - recencyA * 0.000001 + importanceA * 0.1;
    const scoreB = (b.score || 0) * 0.6 - recencyB * 0.000001 + importanceB * 0.1;
    return scoreB - scoreA;
  });
  // Context compression: take top 5, use summary if present, else first 200 chars
  return unique.slice(0, 5).map(m => m.metadata?.summary || (m.metadata?.text || '').substring(0, 200));
}

module.exports = { retrieveOptimized };
```

### `server/package.json`

```json
{
  "name": "easymoney-ai-server",
  "version": "0.1.0",
  "description": "Backend services for EasyMoney: event processor, RAG, embeddings, vector store",
  "main": "index.js",
  "scripts": {
    "start": "node index.js",
    "dev": "nodemon index.js"
  },
  "dependencies": {
    "@pinecone-database/pinecone": "^3.0.0",
    "axios": "^1.5.0",
    "body-parser": "^1.20.2",
    "dotenv": "^16.3.1",
    "express": "^4.18.2",
    "firebase-admin": "^11.9.0",
    "node-cron": "^3.0.2",
    "pg": "^8.11.0",
    "uuid": "^9.0.0"
  },
  "engines": {
    "node": ">=18"
  },
  "devDependencies": {
    "nodemon": "^3.1.14"
  }
}
```

### `server/portfolioService.js`

```javascript
const { getWatchlistContext } = require('./watchlistRetriever');
const { buildWatchlistPrompt } = require('./watchlistPromptBuilder');
const watchlistService = require('./watchlistService');
const watchlistRetriever = require('./watchlistRetriever');
const userProfileService = require('./userProfileService');
const { getStockPrice } = require('./systemOrchestrator');
const axios = require('axios');

// Resolves after `ms` milliseconds with an empty array — used to race against slow RAG calls
const withTimeout = (promise, ms) =>
  Promise.race([promise, new Promise(resolve => setTimeout(() => resolve([]), ms))]);

/**
 * Portfolio generator.
 * - userId: for personalized risk profile
 * - risk: 'low' | 'medium' | 'high' (falls back to user profile if not passed)
 * - returns an object with named portfolios and rationale
 */
async function generatePortfolio({ risk = 'medium', symbols = [], userId = 'default' } = {}) {
  // Load user profile — override risk if profile has a preference
  try {
    const profile = userProfileService.getProfile(userId);
    if (profile && profile.risk && risk === 'medium') risk = profile.risk;
  } catch (e) {
    console.warn('portfolioService: could not load user profile:', e.message);
  }

  // If symbols not provided, use sample watchlist
  if (!symbols || symbols.length === 0) {
    symbols = await (watchlistService.getUserWatchlist ? watchlistService.getUserWatchlist(userId) : ['AAPL','NVDA','TSLA']);
  }

  // Limit symbols to 30 for cost
  symbols = symbols.slice(0, 30);

  // Fetch small RAG context for each symbol — capped at 2s so we never hang
  const contexts = await withTimeout(watchlistRetriever.getWatchlistContext(symbols, 2), 2000);

  // Heuristic scoring: prefer strong positive sentiment (rudimentary)
  // Assign base scores from context presence + simple keyword checks
  const scores = {};
  for (const s of symbols) scores[s] = 0;
  for (const doc of contexts) {
    const sym = doc.symbol;
    const text = (doc.text || '').toLowerCase();
    if (text.includes('surge') || text.includes('strong') || text.includes('beat') || text.includes('upgrade')) scores[sym] += 3;
    if (text.includes('gain') || text.includes('rise') || text.includes('positive')) scores[sym] += 2;
    if (text.includes('downgrade') || text.includes('miss') || text.includes('concern') || text.includes('drop')) scores[sym] -= 3;
    // recency weight
    scores[sym] += (doc.score || 0) * 1.0;
  }

  // Layer in live price movement signals (non-blocking)
  if (process.env.FINNHUB_API_KEY) {
    const priceResults = await withTimeout(
      Promise.allSettled(symbols.map(async sym => {
        const q = await getStockPrice(sym);
        return q ? { sym, changePct: q.changePct } : null;
      })),
      4000
    );
    for (const r of (Array.isArray(priceResults) ? priceResults : [])) {
      if (r?.status === 'fulfilled' && r.value) {
        // Positive momentum adds up to +2, negative deducts up to -2
        scores[r.value.sym] += Math.max(-2, Math.min(2, r.value.changePct * 0.2));
      }
    }
  }

  // Build candidate lists
  const sorted = Object.keys(scores).sort((a,b) => (scores[b]||0) - (scores[a]||0));

  const take = (n) => sorted.slice(0,n);

  const portfolios = {
    aggressive: take(10),
    balanced: take(6),
    conservative: take(4)
  };

  // Adjust based on requested risk
  let chosen;
  if (risk === 'low') chosen = portfolios.conservative;
  else if (risk === 'high') chosen = portfolios.aggressive;
  else chosen = portfolios.balanced;

  const rationale = `Generated using RAG signals + live price momentum across ${symbols.length} symbols. Risk=${risk}. Top picks: ${chosen.slice(0,5).join(', ')}.`;

  return { risk, portfolio: chosen, rationale, userId };
}

module.exports = { generatePortfolio };
```

### `server/README.md`

```markdown
# EasyMoney AI Server (PHASE 3 Skeleton)

This lightweight Node backend gives you the skeleton for Smart Event-Based RAG and AI-driven features.

Files created:
- `index.js` - Express server + endpoints
- `eventProcessor.js` - event routing + cron scaffold
- `insightService.js` - RAG/insight generation stub
- `embeddingService.js` - embedding abstraction
- `vectorStore.js` - vector DB abstraction
- `db/schema.sql` - minimal schema

Getting started (macOS):

1. Node 18+ & npm installed
2. From project root:

` ` `bash
cd server
npm install
cp .env.example .env  # create appropriate API keys and settings
npm run dev
` ` `

.env variables you may set:
- `OPENAI_API_KEY` - for OpenAI embeddings (optional)
- `OPENAI_URL` - custom OpenAI-compatible endpoint (e.g., Pollinations)
- `EMBEDDING_PROVIDER` - `openai` | `pinecone` | `dummy`
- `VECTOR_INDEX` - index name for your vector DB

PHASE 3 implementation checklist (priority):
1. Implement event sources (news feed, price monitor, analyst API)
2. Wire embeddings and a vector DB (Pinecone / Weaviate / FAISS)
3. Implement RAG prompt templates and LLM provider selection
4. Add notification wiring (FCM / APNs)
5. Build Android UI screens and notification handlers

This scaffold is intentionally minimal. I can wire Pinecone or Weaviate and provide working integration next — tell me which vector DB you prefer.
```

### `server/sectorAnalysis.js`

```javascript
const vectorStore = require('./vectorStore');
const embeddingService = require('./embeddingService');
const { getStockPrice } = require('./systemOrchestrator');

// Stock-to-sector mapping — use 3 representative stocks per sector to stay within
// Finnhub free-tier rate limit (60 req/min) when analyzing all sectors at once
const SECTOR_STOCKS = {
  'AI & Tech':     ['NVDA', 'MSFT', 'GOOGL'],
  'Consumer Tech': ['AAPL', 'AMZN', 'TSLA'],
  'Finance':       ['JPM', 'BAC', 'V'],
  'Energy':        ['XOM', 'CVX', 'COP'],
  'Healthcare':    ['JNJ', 'PFE', 'LLY'],
  'Industrials':   ['BA', 'CAT', 'RTX'],
  'Consumer':      ['WMT', 'COST', 'NKE']
};

// Keywords to boost/penalise sector sentiment score from RAG docs
const POSITIVE_KEYWORDS = ['upgrade', 'beat', 'surge', 'strong', 'bullish', 'outperform', 'record', 'gain'];
const NEGATIVE_KEYWORDS = ['downgrade', 'miss', 'drop', 'weak', 'bearish', 'underperform', 'loss', 'concern'];

/**
 * Score a sector using:
 *   1. RAG sentiment from vector store docs for its stocks (weight: 0.5)
 *   2. Average % price change for its stocks today (weight: 0.5, capped)
 */
async function scoreSector(sector, stocks) {
  let ragScore = 0;
  let ragDocCount = 0;

  // RAG sentiment per stock (parallel)
  const ragResults = await Promise.allSettled(
    stocks.map(async symbol => {
      try {
        const q = `${symbol} stock news sentiment`;
        const embedding = await embeddingService.createEmbedding(q);
        const matches = await vectorStore.querySimilar(embedding, 3, { symbol: { '$eq': symbol } });
        let s = 0;
        for (const m of matches) {
          const text = (m.metadata?.text || '').toLowerCase();
          const sentMeta = (m.metadata?.sentiment || '').toLowerCase();
          if (sentMeta === 'positive' || POSITIVE_KEYWORDS.some(k => text.includes(k))) s += 1;
          if (sentMeta === 'negative' || NEGATIVE_KEYWORDS.some(k => text.includes(k))) s -= 1;
        }
        return { count: matches.length, score: s };
      } catch { return { count: 0, score: 0 }; }
    })
  );

  for (const r of ragResults) {
    if (r.status === 'fulfilled') {
      ragScore += r.value.score;
      ragDocCount += r.value.count;
    }
  }

  // Live price movement per stock (parallel, only if Finnhub key available)
  let priceScore = 0;
  if (process.env.FINNHUB_API_KEY) {
    const priceResults = await Promise.allSettled(
      stocks.map(sym => getStockPrice(sym))
    );
    let priceCount = 0;
    for (const r of priceResults) {
      if (r.status === 'fulfilled' && r.value) {
        priceScore += r.value.changePct || 0;
        priceCount++;
      }
    }
    if (priceCount > 0) priceScore = priceScore / priceCount; // average %
  }

  // Normalize: ragScore per doc, then combine
  const normalizedRag = ragDocCount > 0 ? ragScore / ragDocCount : 0;
  const combined = normalizedRag * 0.5 + Math.max(-5, Math.min(5, priceScore)) * 0.1;

  return {
    sector,
    score: +combined.toFixed(3),
    ragDocs: ragDocCount,
    avgPriceChange: priceScore !== 0 ? +priceScore.toFixed(2) : null,
    stocks: stocks.slice(0, 5)
  };
}

/**
 * Analyze all sectors and return ranked results.
 * Runs sequentially with a small delay to respect Finnhub free-tier rate limits.
 * @param {string[]} [sectorList] - optional override of sectors to analyze
 */
async function analyzeSectors(sectorList) {
  const sectors = sectorList
    ? sectorList.reduce((acc, s) => { acc[s] = SECTOR_STOCKS[s] || [s]; return acc; }, {})
    : SECTOR_STOCKS;

  const results = [];
  for (const [sector, stocks] of Object.entries(sectors)) {
    try {
      const scored = await scoreSector(sector, stocks);
      results.push(scored);
    } catch (e) {
      console.warn(`sectorAnalysis: scoreSector failed for ${sector}:`, e.message);
    }
    // 300ms gap between sectors keeps us under 60 req/min on Finnhub free tier
    await new Promise(r => setTimeout(r, 300));
  }
  return results.sort((a, b) => b.score - a.score);
}

module.exports = { analyzeSectors, SECTOR_STOCKS };
```

### `server/signalEngine.js`

```javascript
const embeddingService = require('./embeddingService');
const vectorStore = require('./vectorStore');

function computeMomentum(prices) {
  if (!prices || prices.length < 2) return 0;
  const recent = prices.slice(-10);
  const first = recent[0];
  const last = recent[recent.length - 1];
  return ((last - first) / (first || 1)) * 100;
}

async function computeRagSentiment(symbol) {
  try {
    const queryEmbedding = await embeddingService.createEmbedding(`${symbol} stock news analyst sentiment`);
    const matches = await vectorStore.querySimilar(queryEmbedding, 5, { symbol: { '$eq': symbol } });
    let boost = 0;
    const reasons = [];
    for (const m of matches) {
      const text = (m.metadata?.text || '').toLowerCase();
      const sent = (m.metadata?.sentiment || '').toLowerCase();
      if (sent === 'positive' || /upgrade|beat|surge|strong|bullish/.test(text)) {
        boost += 1;
        reasons.push('positive news');
      } else if (sent === 'negative' || /downgrade|miss|drop|weak|bearish/.test(text)) {
        boost -= 1;
        reasons.push('negative news');
      }
    }
    const unique = [...new Set(reasons)];
    const label = boost > 0 ? 'positive' : boost < 0 ? 'negative' : 'neutral';
    const summary = matches.length > 0
      ? ` News sentiment: ${label} (${unique.join(', ') || 'mixed signals'}, ${matches.length} docs).`
      : '';
    return { boost, summary };
  } catch {
    return { boost: 0, summary: '' };
  }
}

async function analyzeSymbol(symbol, marketData = {}) {
  const momentum = computeMomentum(marketData.prices || []);
  const { boost, summary } = await computeRagSentiment(symbol);

  const adjusted = momentum + boost * 1.5;
  const confidence = Math.min(0.95, Math.abs(adjusted) / 10);
  const signal = adjusted > 2 ? 'bullish' : (adjusted < -2 ? 'bearish' : 'neutral');
  const reason = `Momentum ${momentum.toFixed(2)}% over window.${summary}`;

  return { symbol, signal, confidence: +confidence.toFixed(2), reason };
}

module.exports = { analyzeSymbol };
```

### `server/simulationEngine.js`

```javascript
// Small simulation engine: compute historical performance and project simple returns

function simulatePastReturn(prices, investment = 1000) {
  if (!prices || prices.length < 2) return investment;
  const start = prices[0];
  const end = prices[prices.length-1];
  const ret = (end - start) / (start || 1);
  return investment * (1 + ret);
}

function projectSimpleReturn(currentPrice, expectedReturnPct, investment = 1000) {
  const future = investment * (1 + expectedReturnPct/100);
  return { futureValue: future, expectedReturnPct };
}

module.exports = { simulatePastReturn, projectSimpleReturn };
```

### `server/usersService.js`

```javascript
// Simple users service for demo. Replace with your DB-backed users.

const users = [
  { id: 'user1', deviceToken: '', email: 'alice@example.com' },
  { id: 'user2', deviceToken: '', email: 'bob@example.com' }
];

async function getAllUsers() {
  // In production, query your user DB
  return users;
}

async function getUserById(id) {
  return users.find(u => u.id === id);
}

// For event engine: get all users watching a symbol
async function getUsersWatching(symbol) {
  // In production, query your DB (watchlist join users)
  // Here, return all users for demo
  return users;
}

module.exports = { getAllUsers, getUserById, getUsersWatching };
```

### `server/vectorStore.js`

```javascript
// Small abstraction that supports multiple vector backends: Pinecone, Weaviate, FAISS (local)

const fs = require('fs');
const path = require('path');
const { PineconeClient } = require('@pinecone-database/pinecone');

const INDEX_NAME = process.env.VECTOR_INDEX || 'stock-index';
const STORAGE_DIR = path.join(__dirname, 'local_vectors');
const NEWS_INDEX = process.env.PINECONE_NEWS_INDEX || 'stock-news-index';
const SIGNAL_INDEX = process.env.PINECONE_SIGNAL_INDEX || 'stock-signal-index';

// Ensure local dir exists
if (!fs.existsSync(STORAGE_DIR)) fs.mkdirSync(STORAGE_DIR, { recursive: true });

// Pinecone client (lazy)
let pinecone = null;
async function getPineconeClient() {
  if (!pinecone) {
    if (!process.env.PINECONE_API_KEY || !process.env.PINECONE_ENVIRONMENT) return null;
    pinecone = new PineconeClient();
    await pinecone.init({ apiKey: process.env.PINECONE_API_KEY, environment: process.env.PINECONE_ENVIRONMENT });
  }
  return pinecone;
}

function cosine(a, b) {
  let dot = 0, na = 0, nb = 0;
  for (let i = 0; i < a.length; i++) {
    dot += a[i] * b[i];
    na += a[i] * a[i];
    nb += b[i] * b[i];
  }
  return dot / (Math.sqrt(na) * Math.sqrt(nb) + 1e-12);
}

// Local storage format: one JSON file per document in local_vectors/{id}.json
async function upsertLocal(id, embedding, metadata) {
  const file = path.join(STORAGE_DIR, `${id}.json`);
  const payload = { id, embedding, metadata, ts: Date.now() };
  fs.writeFileSync(file, JSON.stringify(payload));
  return { ok: true };
}

async function queryLocal(queryEmbedding, topK = 5, filter = {}) {
  const files = fs.readdirSync(STORAGE_DIR).filter(f => f.endsWith('.json'));
  const results = [];
  for (const f of files) {
    try {
      const data = JSON.parse(fs.readFileSync(path.join(STORAGE_DIR, f), 'utf8'));
      // filter by metadata if provided
      if (filter && filter.symbol && filter.symbol['$eq']) {
        if (data.metadata?.symbol !== filter.symbol['$eq']) continue;
      }
      const score = cosine(queryEmbedding, data.embedding);
      results.push({ id: data.id, score, metadata: data.metadata, text: data.metadata?.text });
    } catch (e) { }
  }
  results.sort((a, b) => b.score - a.score);
  return results.slice(0, topK);
}

function getIndexName(type) {
  if (type === 'analyst' || type === 'earnings' || type === 'signal') return SIGNAL_INDEX;
  return NEWS_INDEX;
}

async function upsertDocument(id, embedding, metadata) {
  // metadata: { symbol, type, sentiment, timestamp, source, text, summary }
  const usePinecone = !!process.env.PINECONE_API_KEY;
  const type = metadata.type || 'news';
  const indexName = getIndexName(type);
  if (usePinecone) {
    const client = await getPineconeClient();
    if (!client) return upsertLocal(id, embedding, metadata);
    const index = client.Index(indexName);
    await index.upsert({ upsertRequest: { vectors: [{ id, values: embedding, metadata }] } });
    return { ok: true };
  }
  return upsertLocal(id, embedding, metadata);
}

async function querySimilar(embedding, topK = 5, filter = {}, type = 'news') {
  const usePinecone = !!process.env.PINECONE_API_KEY;
  const indexName = getIndexName(type);
  if (usePinecone) {
    const client = await getPineconeClient();
    if (!client) return queryLocal(embedding, topK, filter);
    const index = client.Index(indexName);
    const q = {
      vector: embedding,
      topK,
      includeMetadata: true,
      filter
    };
    const resp = await index.query({ queryRequest: q });
    return resp.matches.map(m => ({ id: m.id, score: m.score, metadata: m.metadata }));
  }
  return queryLocal(embedding, topK, filter);
}

module.exports = { upsertDocument, querySimilar, getIndexName };
```

### `server/watchlistPromptBuilder.js`

```javascript
function buildWatchlistPrompt(symbols, contextDocs) {
  const grouped = symbols.map(symbol => {
    const docs = contextDocs
      .filter(d => d.symbol === symbol)
      .map(d => `- ${d.text}`)
      .join('\n');
    return `Stock: ${symbol}\n${docs}`;
  }).join('\n\n');

  return `You are a financial AI assistant. Analyze the following watchlist stocks:\n\n${grouped}\n\nTasks:\n1. Overall watchlist sentiment (bullish, bearish, neutral)\n2. Highlight best performing stocks\n3. Highlight risky or volatile stocks\n4. Give a short overall outlook\n\nResponse format:\n- Summary (2-3 lines)\n- Bullet points per key stock\n- Final outlook\n\nKeep it concise and clear. Do NOT give financial advice.`;
}

module.exports = { buildWatchlistPrompt };
```

### `server/watchlistRetriever.js`

```javascript
const { retrieveOptimized } = require('./optimizedRetriever');

/**
 * For each symbol, retrieve up to `perSymbol` top docs from vector store.
 * Returns array of { symbol, text, score, id }
 */
async function getWatchlistContext(symbols, perSymbol = 3) {
  const allDocs = [];
  for (const symbol of symbols) {
    try {
      const docs = await retrieveOptimized(symbol);
      allDocs.push(...docs.map(text => ({ symbol, text })));
    } catch (e) {
      console.warn('Retriever error for', symbol, e.message);
    }
  }
  return allDocs;
}

module.exports = { getWatchlistContext };
```

### `server/watchlistService.js`

```javascript
const { getWatchlistContext } = require('./watchlistRetriever');
const { buildWatchlistPrompt } = require('./watchlistPromptBuilder');
const axios = require('axios');
const embeddingService = require('./embeddingService');

// Simple in-memory cache. For production, use Redis.
const cache = new Map();
const CACHE_TTL_MS = 15 * 60 * 1000; // 15 minutes

async function getUserWatchlist(userId) {
  // TODO: replace with DB query. For now return sample list or read from env
  if (process.env.SAMPLE_WATCHLIST) return process.env.SAMPLE_WATCHLIST.split(',').map(s => s.trim()).filter(Boolean);
  return ['AAPL', 'NVDA', 'TSLA'];
}

async function callLLM(prompt) {
  const provider = process.env.LLM_PROVIDER || (process.env.OPENAI_API_KEY ? 'openai' : 'pollinations');

  if (provider === 'openai') {
    const urlBase = process.env.OPENAI_URL || 'https://api.openai.com';
    const resp = await axios.post(`${urlBase}/v1/chat/completions`, {
      model: process.env.LLM_MODEL || 'gpt-4o-mini',
      messages: [
        { role: 'system', content: 'You are a financial AI assistant.' },
        { role: 'user', content: prompt }
      ],
      max_tokens: 400
    }, {
      headers: { Authorization: `Bearer ${process.env.OPENAI_API_KEY}` },
      timeout: 30000
    });
    return resp.data.choices?.[0]?.message?.content;
  }

  // Pollinations or other open endpoint (OpenAI-compatible)
  if (provider === 'pollinations' || provider === 'openai_compat') {
    const url = (process.env.OPENAI_URL || 'https://text.pollinations.ai') + '/openai';
    const resp = await axios.post(url, {
      model: process.env.LLM_MODEL || 'openai',
      messages: [
        { role: 'system', content: 'You are a financial AI assistant.' },
        { role: 'user', content: prompt }
      ]
    }, { timeout: 30000 });
    return resp.data?.choices?.[0]?.message?.content || resp.data;
  }

  // Fallback: lightweight local summary without LLM (deterministic)
  return localSummary(prompt);
}

function localSummary(prompt) {
  // Very naive extraction: return first 300 chars as "summary" as fallback
  return prompt.substring(0, 300) + (prompt.length > 300 ? '...' : '');
}

async function generateWatchlistSummary(userId) {
  const cacheKey = `watchlist:${userId}`;
  const cached = cache.get(cacheKey);
  if (cached && (Date.now() - cached.ts) < CACHE_TTL_MS) return cached.value;

  const symbols = await getUserWatchlist(userId);
  if (!symbols || symbols.length === 0) return 'No stocks in your watchlist.';

  // Limit to first N symbols to bound cost
  const maxSymbols = parseInt(process.env.MAX_WATCHLIST_SYMBOLS || '10', 10);
  const limited = symbols.slice(0, maxSymbols);

  // Retrieve RAG context
  const contextDocs = await getWatchlistContext(limited, 3);
  if (!contextDocs || contextDocs.length === 0) return 'No significant updates in your watchlist today.';

  // Build prompt and call LLM
  const prompt = buildWatchlistPrompt(limited, contextDocs);
  const llmResp = await callLLM(prompt);

  // Cache and return
  cache.set(cacheKey, { value: llmResp, ts: Date.now() });
  return llmResp;
}

module.exports = { generateWatchlistSummary, getUserWatchlist };
```

### `server/worker.js`

```javascript
const cron = require('node-cron');
const { getAllUsers } = require('./usersService');
const { generateWatchlistSummary } = require('./watchlistService');
const portfolioService = require('./portfolioService');
const sectorAnalysis = require('./sectorAnalysis');
const { sendFCM } = require('./notificationService');

async function runDailyBriefing() {
  console.log('Daily briefing started');
  const users = await getAllUsers();
  for (const user of users) {
    try {
      const summary = await generateWatchlistSummary(user.id);
      const portfolio = await portfolioService.generatePortfolio({ risk: 'medium' });
      const sectors = await sectorAnalysis.analyzeSectors();
      const sectorSummary = sectors.sort((a,b) => b.score - a.score).slice(0,3).map(s => s.sector).join(', ');

      const message = {
        title: '🧠 Daily Market Brief',
        body: `${summary}\nTop Picks: ${portfolio.portfolio.slice(0,3).join(', ')}\nSectors: ${sectorSummary}`
      };

      if (user.deviceToken) {
        await sendFCM(user.deviceToken, message);
      } else {
        console.log(`Would notify ${user.id}:`, message);
      }
    } catch (e) {
      console.error('Error generating briefing for', user.id, e.message || e);
    }
  }
}

// schedule: 9am server local time (cron format)
cron.schedule('0 9 * * *', async () => {
  try {
    await runDailyBriefing();
  } catch (e) {
    console.error('Daily briefing failed', e.message || e);
  }
});

// Allow manual run
if (require.main === module) {
  runDailyBriefing().then(() => console.log('Manual run complete')).catch(e => console.error(e));
}

module.exports = { runDailyBriefing };
```

### `server/worker/eventWorker.js`

```javascript
const cron = require('node-cron');
const { detectPriceEvent } = require('../ai/eventDetector');
const { handleEvent } = require('../ai/eventProcessor');
const { getUsersWatching } = require('../usersService');

// Replace with real price API in production
global.priceHistory = {
  AAPL: [180, 182, 185, 190, 195, 200],
  TSLA: [700, 690, 680, 670, 660, 650],
  NVDA: [400, 410, 420, 430, 440, 450]
};

async function getRecentPrices(symbol) {
  // Replace with real API call
  return global.priceHistory[symbol] || [];
}

async function checkMarket() {
  const symbols = Object.keys(global.priceHistory); // Replace with dynamic list
  for (const symbol of symbols) {
    const prices = await getRecentPrices(symbol);
    const event = detectPriceEvent(symbol, prices);
    if (event) {
      const users = await getUsersWatching(symbol);
      await handleEvent(event, users);
    }
  }
}

// Run every 5 minutes
cron.schedule('*/5 * * * *', checkMarket);

// Allow manual run
if (require.main === module) {
  checkMarket().then(() => console.log('Manual event check complete')).catch(e => console.error(e));
}
```

### `settings.gradle.kts`

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        // MPAndroidChart is available via JitPack
        maven("https://jitpack.io")
    }
}

rootProject.name = "EasyMoney"
include(":app")
```

### `start-backend.sh`

```bash
#!/bin/bash

# Kill any existing node processes on port 8080
echo "🔄 Stopping any existing server..."
pkill -f "node index.js" 2>/dev/null
sleep 1

# Navigate to server directory
cd /Users/Shubham.Dhamija/AndroidStudioProjects/EasyMoney/server

# Start the server
echo "🚀 Starting EasyMoney Backend Server..."
npm start &
SERVER_PID=$!

# Give it time to start
sleep 3

# Check if server is running
if ps -p $SERVER_PID > /dev/null; then
    echo "✅ Server is running (PID: $SERVER_PID)"

    # Test the endpoint
    echo "📡 Testing backend endpoint..."
    RESPONSE=$(curl -s http://localhost:8080/ 2>&1)
    if [ $? -eq 0 ]; then
        echo "✅ Backend is responding: $RESPONSE"
        echo ""
        echo "🎯 Backend URL for emulator: http://10.0.2.2:8080/"
        echo "🎯 Backend URL for physical device: http://YOUR_MAC_IP:8080/"
        echo ""
        echo "To find your Mac IP:"
        echo "  ifconfig | grep \"inet \" | grep -v 127.0.0.1"
    else
        echo "⚠️  Backend test failed"
    fi
else
    echo "❌ Failed to start server"
    exit 1
fi

# Keep the script running
wait $SERVER_PID
```

