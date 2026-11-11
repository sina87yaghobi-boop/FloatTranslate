package com.floattranslate.app.data

import com.google.gson.JsonElement
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

// MyMemory (free, no key): Standard translation
data class MyMemoryResponse(val responseData: MyMemoryData?, val responseStatus: Any?)
data class MyMemoryData(val translatedText: String?)

interface MyMemoryApi {
    @GET("get")
    suspend fun translate(@Query("q") text: String, @Query("langpair") pair: String): MyMemoryResponse
}

// OpenAI-compatible chat API: Formal / Informal / Professional
data class ChatMsg(val role: String, val content: String)
data class ChatRequest(val model: String, val messages: List<ChatMsg>, val temperature: Double = 0.3)
data class ChatChoice(val message: ChatMsg?)
data class ChatResponse(val choices: List<ChatChoice>?)

interface ChatApi {
    @POST("chat/completions")
    suspend fun chat(@Header("Authorization") auth: String, @Body body: ChatRequest): ChatResponse
}

// Google's free web translate endpoint (no key): much better quality than MyMemory
interface GoogleApi {
    @GET("translate_a/single")
    suspend fun translate(
        @Query("client") client: String,
        @Query("sl") sl: String,
        @Query("tl") tl: String,
        @Query("dt") dt: String,
        @Query("q") q: String,
    ): JsonElement
}
