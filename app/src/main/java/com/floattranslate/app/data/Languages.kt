package com.floattranslate.app.data

data class Lang(val code: String, val name: String, val native: String)

object Languages {
    val AUTO = Lang("auto", "Auto-detect", "Auto")
    val all = listOf(
        Lang("fa", "Persian", "فارسی"),
        Lang("en", "English", "English"),
        Lang("ar", "Arabic", "العربية"),
        Lang("fr", "French", "Français"),
        Lang("de", "German", "Deutsch"),
        Lang("es", "Spanish", "Español"),
        Lang("zh-CN", "Chinese", "中文"),
        Lang("ja", "Japanese", "日本語"),
        Lang("ko", "Korean", "한국어"),
        Lang("ru", "Russian", "Русский"),
        Lang("tr", "Turkish", "Türkçe"),
        Lang("it", "Italian", "Italiano"),
        Lang("pt", "Portuguese", "Português"),
        Lang("hi", "Hindi", "हिन्दी"),
    )
    val sources = listOf(AUTO) + all
    fun byCode(c: String) = sources.first { it.code == c }
}

enum class Style(val label: String, val hint: String) {
    STANDARD("Standard", ""),
    FORMAL("Formal", "Use a formal, polite register."),
    INFORMAL("Informal", "Use a casual, conversational register like talking to a friend."),
    PROFESSIONAL("Professional", "Use precise, professional business language."),
}
