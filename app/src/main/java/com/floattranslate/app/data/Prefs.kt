package com.floattranslate.app.data

import android.content.Context

object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("floattranslate", Context.MODE_PRIVATE)

    fun screenConsent(c: Context) = sp(c).getBoolean("screen_consent", false)
    fun setScreenConsent(c: Context, v: Boolean) = sp(c).edit().putBoolean("screen_consent", v).apply()

    fun apiKey(c: Context) = sp(c).getString("llm_key", "") ?: ""
    fun baseUrl(c: Context) = sp(c).getString("llm_base", "https://api.openai.com/v1/") ?: "https://api.openai.com/v1/"
    fun model(c: Context) = sp(c).getString("llm_model", "gpt-4o-mini") ?: "gpt-4o-mini"

    fun saveLlm(c: Context, key: String, base: String, model: String) {
        val b = base.trim().let { if (it.endsWith("/")) it else "$it/" }
        sp(c).edit().putString("llm_key", key.trim()).putString("llm_base", b).putString("llm_model", model.trim()).apply()
    }
}
