package com.floattranslate.app.data

data class Lang(val code: String, val name: String, val native: String, val en: String)

object Languages {
    val AUTO = Lang("auto", "تشخیص خودکار", "Auto", "Auto-detect")
    val all = listOf(
        Lang("fa", "فارسی", "فارسی", "Persian"),
        Lang("en", "انگلیسی", "English", "English"),
        Lang("ar", "عربی", "العربية", "Arabic"),
        Lang("fr", "فرانسوی", "Français", "French"),
        Lang("de", "آلمانی", "Deutsch", "German"),
        Lang("es", "اسپانیایی", "Español", "Spanish"),
        Lang("zh-CN", "چینی", "中文", "Chinese"),
        Lang("ja", "ژاپنی", "日本語", "Japanese"),
        Lang("ko", "کره‌ای", "한국어", "Korean"),
        Lang("ru", "روسی", "Русский", "Russian"),
        Lang("tr", "ترکی", "Türkçe", "Turkish"),
        Lang("it", "ایتالیایی", "Italiano", "Italian"),
        Lang("pt", "پرتغالی", "Português", "Portuguese"),
        Lang("hi", "هندی", "हिन्दी", "Hindi"),
    )
    val sources = listOf(AUTO) + all
    fun byCode(c: String) = sources.first { it.code == c }
}

enum class Style(val label: String, val hint: String) {
    STANDARD("استاندارد", ""),
    FORMAL("رسمی", "Use a formal, polite register."),
    INFORMAL("خودمونی", "Use a casual, conversational register like talking to a friend."),
    PROFESSIONAL("حرفه‌ای", "Use precise, professional business language."),
}
