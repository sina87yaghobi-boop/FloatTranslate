package com.floattranslate.app.access

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Passive: never reads anything on its own. Text is read only when the user taps a button
 * in the translator panel (and has consented in the app). Nothing is stored or sent until
 * the user presses Translate. Password fields are skipped.
 */
class ScreenTextService : AccessibilityService() {

    companion object { @Volatile var instance: ScreenTextService? = null }

    override fun onServiceConnected() { instance = this }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
    override fun onUnbind(intent: Intent?): Boolean { instance = null; return super.onUnbind(intent) }

    private fun roots(): List<AccessibilityNodeInfo> {
        val fromWindows = windows.mapNotNull { it.root }.filter { it.packageName != packageName }
        return fromWindows.ifEmpty { listOfNotNull(rootInActiveWindow) }
    }

    fun selectedText(): String? {
        var found: String? = null
        fun walk(n: AccessibilityNodeInfo?, depth: Int) {
            if (n == null || found != null || depth > 40 || n.isPassword) return
            val t = n.text
            val s = n.textSelectionStart
            val e = n.textSelectionEnd
            if (t != null && s >= 0 && e > s && e <= t.length) { found = t.subSequence(s, e).toString(); return }
            for (i in 0 until n.childCount) walk(n.getChild(i), depth + 1)
        }
        roots().forEach { walk(it, 0) }
        return found
    }

    /** Every separate text block on screen (messages, labels...) in reading order. */
    fun visibleTexts(): List<String> {
        val first = collect(true)
        return if (first.isNotEmpty()) first else collect(false)
    }

    private fun collect(onlyVisible: Boolean): List<String> {
        val out = LinkedHashSet<String>()
        fun walk(n: AccessibilityNodeInfo?, depth: Int) {
            if (n == null || depth > 40 || n.isPassword || out.size >= 80) return
            if (onlyVisible && !n.isVisibleToUser) return
            n.text?.toString()?.trim()?.takeIf { it.length >= 2 }?.let { out.add(it.take(1500)) }
            for (i in 0 until n.childCount) walk(n.getChild(i), depth + 1)
        }
        roots().forEach { walk(it, 0) }
        return out.toList()
    }
}
