package com.floattranslate.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.floattranslate.app.data.Lang
import com.floattranslate.app.data.Languages
import com.floattranslate.app.data.Style
import com.floattranslate.app.data.TranslationRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

data class UiState(
    val input: String = "",
    val output: String = "",
    val from: Lang = Languages.AUTO,
    val to: Lang = Languages.byCode("fa"),
    val style: Style = Style.STANDARD,
    val loading: Boolean = false,
    val message: String? = null,
    val screenTexts: List<String> = emptyList(),
)

class TranslatorViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = TranslationRepository(app)
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state
    private var job: Job? = null

    /** Typing: translate after a short pause. */
    fun onInput(t: String) {
        _state.update { it.copy(input = t, message = null) }
        job?.cancel()
        if (t.isBlank()) { _state.update { it.copy(output = "", loading = false) }; return }
        job = viewModelScope.launch { delay(800); runTranslate() }
    }

    /** Picked/selected text: translate right away. */
    fun useText(t: String) {
        job?.cancel()
        _state.update { it.copy(input = t, output = "", message = null) }
        retry()
    }

    fun setScreenTexts(l: List<String>) = _state.update { it.copy(screenTexts = l, message = null) }
    fun clearScreenTexts() = _state.update { it.copy(screenTexts = emptyList()) }

    fun setFrom(l: Lang) { _state.update { it.copy(from = l) }; retry() }
    fun setTo(l: Lang) { _state.update { it.copy(to = l) }; retry() }
    fun setStyle(s: Style) { _state.update { it.copy(style = s) }; retry() }
    fun clear() { job?.cancel(); _state.update { it.copy(input = "", output = "", loading = false, message = null) } }
    fun showMessage(m: String) = _state.update { it.copy(message = m) }

    fun swap() {
        _state.update { s ->
            val newTo = if (s.from.code == "auto") (if (s.to.code == "en") Languages.byCode("fa") else Languages.byCode("en")) else s.from
            s.copy(from = s.to, to = newTo, input = s.output.ifBlank { s.input }, output = "")
        }
        retry()
    }

    fun retry() { job?.cancel(); if (_state.value.input.isNotBlank()) job = viewModelScope.launch { runTranslate() } }
    fun translateNow() = retry()

    private fun friendly(e: Throwable): String = when (e) {
        is SocketTimeoutException -> "زمان اتصال تمام شد. دوباره امتحان کن."
        is IOException -> "اینترنت وصل نیست یا سرور در دسترس نیست."
        is HttpException -> "خطای سرور (${e.code()})"
        else -> e.message ?: "ترجمه انجام نشد."
    }

    private suspend fun runTranslate() {
        val s = _state.value
        _state.update { it.copy(loading = true, message = null) }
        repo.translate(s.input.trim(), s.from, s.to, s.style)
            .onSuccess { (out, note) -> _state.update { it.copy(output = out, loading = false, message = note) } }
            .onFailure { e -> if (e !is CancellationException) _state.update { it.copy(loading = false, message = friendly(e)) } }
    }
}
