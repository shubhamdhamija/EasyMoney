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
