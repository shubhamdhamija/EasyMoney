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
