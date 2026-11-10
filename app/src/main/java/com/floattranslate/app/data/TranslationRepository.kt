package com.floattranslate.app.data

import android.content.Context
import android.text.Html
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class TranslationRepository(private val ctx: Context) {

    private val myMemory: MyMemoryApi = Retrofit.Builder()
        .baseUrl("https://api.mymemory.translated.net/")
        .addConverterFactory(GsonConverterFactory.create())
        .build().create(MyMemoryApi::class.java)

    private var chatCache: Pair<String, ChatApi>? = null
    private fun chatApi(base: String): ChatApi {
        chatCache?.let { if (it.first == base) return it.second }
        val api = Retrofit.Builder().baseUrl(base)
            .addConverterFactory(GsonConverterFactory.create())
            .build().create(ChatApi::class.java)
        chatCache = base to api
        return api
    }

    /** Pair(translation, note). note is non-null when we fell back to Standard. */
    suspend fun translate(text: String, from: Lang, to: Lang, style: Style): Result<Pair<String, String?>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val key = Prefs.apiKey(ctx)
                if (style != Style.STANDARD && key.isNotBlank()) {
                    llm(text, from, to, style, key) to null
                } else {
                    val note = if (style != Style.STANDARD) "Add an AI API key in the app to enable ${style.label} style. Showing Standard." else null
                    standard(text, from, to) to note
                }
            }
        }

    private suspend fun standard(text: String, from: Lang, to: Lang): String {
        val src = if (from.code == "auto") "Autodetect" else from.code
        return chunk(text, 450).joinToString("") { part ->
            val r = myMemory.translate(part, "$src|${to.code}")
            val out = r.responseData?.translatedText ?: error("No translation returned")
            if (out.contains("MYMEMORY WARNING", true)) error("Daily free limit reached. Try again later or set an AI key.")
            Html.fromHtml(out, Html.FROM_HTML_MODE_LEGACY).toString()
        }
    }

    private suspend fun llm(text: String, from: Lang, to: Lang, style: Style, key: String): String {
        val fromPart = if (from.code == "auto") "Detect the source language automatically." else "Source language: ${from.name}."
        val system = "You are a translation engine. $fromPart Translate into ${to.name}. ${style.hint} " +
            "Keep meaning, names and formatting. Output ONLY the translation."
        val res = chatApi(Prefs.baseUrl(ctx)).chat(
            "Bearer $key",
            ChatRequest(Prefs.model(ctx), listOf(ChatMsg("system", system), ChatMsg("user", text)))
        )
        return res.choices?.firstOrNull()?.message?.content?.trim() ?: error("Empty response from AI service")
    }

    private fun chunk(text: String, max: Int): List<String> {
        if (text.length <= max) return listOf(text)
        val out = mutableListOf<String>()
        var rest = text
        while (rest.length > max) {
            var cut = rest.lastIndexOfAny(charArrayOf('.', '!', '?', '؟', '\n', ' '), max)
            if (cut <= 0) cut = max
            out += rest.substring(0, cut + 1)
            rest = rest.substring(cut + 1)
        }
        if (rest.isNotEmpty()) out += rest
        return out
    }
}
