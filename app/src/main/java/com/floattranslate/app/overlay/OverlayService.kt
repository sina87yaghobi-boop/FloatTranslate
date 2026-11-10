package com.floattranslate.app.overlay

import android.animation.ValueAnimator
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.ViewModelProvider
import com.floattranslate.app.MainActivity
import com.floattranslate.app.R
import com.floattranslate.app.access.ScreenTextService
import com.floattranslate.app.data.Prefs
import com.floattranslate.app.ui.FloatTheme
import com.floattranslate.app.ui.TranslatorPanel
import com.floattranslate.app.ui.TranslatorViewModel
import kotlin.math.abs

class OverlayService : Service() {

    companion object {
        const val ACTION_TOGGLE = "toggle_bubble"
        const val ACTION_STOP = "stop"
        @Volatile var running = false
        private const val CHANNEL = "float_translate"
        private const val NOTIF_ID = 1
    }

    private lateinit var wm: WindowManager
    private val owner = OverlayOwner()
    private lateinit var vm: TranslatorViewModel
    private var bubble: View? = null
    private var panel: View? = null
    private var bubbleVisible = true

    private fun Int.dp() = (this * resources.displayMetrics.density).toInt()

    override fun onBind(i: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        running = true
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        owner.start()
        vm = ViewModelProvider(
            owner.viewModelStore,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[TranslatorViewModel::class.java]
        createChannel()
        startFg()
        addBubble()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE -> toggleBubble()
            ACTION_STOP -> { stopSelf(); return START_NOT_STICKY }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        running = false
        removePanel()
        bubble?.let { runCatching { wm.removeView(it) } }
        bubble = null
        owner.stop()
        super.onDestroy()
    }

    // ---------- Notification ----------
    private fun createChannel() {
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel(CHANNEL, "FloatTranslate", NotificationManager.IMPORTANCE_LOW))
    }

    private fun buildNotification(): Notification {
        fun action(a: String, code: Int) = PendingIntent.getService(
            this, code, Intent(this, OverlayService::class.java).setAction(a),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_bubble)
            .setContentTitle("FloatTranslate is active")
            .setContentText(if (bubbleVisible) "Bubble is visible" else "Bubble is hidden")
            .setContentIntent(open)
            .setOngoing(true)
            .addAction(0, if (bubbleVisible) "Hide bubble" else "Show bubble", action(ACTION_TOGGLE, 1))
            .addAction(0, "Stop", action(ACTION_STOP, 2))
            .build()
    }

    private fun startFg() {
        val type = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        ServiceCompat.startForeground(this, NOTIF_ID, buildNotification(), type)
    }

    private fun refreshNotification() {
        getSystemService(NotificationManager::class.java).notify(NOTIF_ID, buildNotification())
    }

    // ---------- Bubble ----------
    private fun addBubble() {
        val size = 56.dp()
        val p = WindowManager.LayoutParams(
            size, size, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = 0; y = 300.dp() }

        val v = ComposeView(this).apply {
            owner.attach(this)
            setContent {
                FloatTheme {
                    Box(
                        Modifier.size(56.dp).shadow(8.dp, CircleShape).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Default.Translate, "Translate", tint = MaterialTheme.colorScheme.onPrimary) }
                }
            }
        }
        attachDrag(v, p)
        wm.addView(v, p)
        bubble = v
    }

    private fun attachDrag(v: View, p: WindowManager.LayoutParams) {
        val slop = ViewConfiguration.get(this).scaledTouchSlop
        var startX = 0; var startY = 0; var downX = 0f; var downY = 0f; var moved = false
        v.setOnTouchListener { view, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = p.x; startY = p.y; downX = e.rawX; downY = e.rawY; moved = false; true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - downX; val dy = e.rawY - downY
                    if (abs(dx) > slop || abs(dy) > slop) moved = true
                    if (moved) { p.x = (startX + dx).toInt(); p.y = (startY + dy).toInt(); wm.updateViewLayout(view, p) }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) openPanel() else snapToEdge(view, p)
                    true
                }
                else -> false
            }
        }
    }

    private fun snapToEdge(v: View, p: WindowManager.LayoutParams) {
        val screenW = resources.displayMetrics.widthPixels
        val target = if (p.x + v.width / 2 < screenW / 2) 0 else screenW - v.width
        ValueAnimator.ofInt(p.x, target).apply {
            duration = 200
            addUpdateListener { p.x = it.animatedValue as Int; if (v.isAttachedToWindow) wm.updateViewLayout(v, p) }
            start()
        }
    }

    private fun toggleBubble() {
        bubbleVisible = !bubbleVisible
        if (!bubbleVisible) removePanel()
        bubble?.visibility = if (bubbleVisible) View.VISIBLE else View.GONE
        refreshNotification()
    }

    // ---------- Panel ----------
    private class BackFrame(c: Context, var onBack: () -> Unit) : FrameLayout(c) {
        override fun dispatchKeyEvent(e: KeyEvent): Boolean {
            if (e.keyCode == KeyEvent.KEYCODE_BACK && e.action == KeyEvent.ACTION_UP) { onBack(); return true }
            return super.dispatchKeyEvent(e)
        }
    }

    private fun openPanel() {
        if (panel != null) return
        val screenW = resources.displayMetrics.widthPixels
        val p = WindowManager.LayoutParams(
            (screenW * 0.92f).toInt(), WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 72.dp()
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }
        val root = BackFrame(this) { closePanel() }
        val compose = ComposeView(this).apply {
            owner.attach(this)
            setContent {
                TranslatorPanel(
                    vm = vm,
                    onClose = ::closePanel,
                    onShare = ::share,
                    onUseSelection = { fetchScreen(selection = true) },
                    onUseScreen = { fetchScreen(selection = false) },
                )
            }
        }
        owner.attach(root)
        root.addView(compose)
        wm.addView(root, p)
        panel = root
        bubble?.visibility = View.GONE
    }

    private fun closePanel() {
        removePanel()
        if (bubbleVisible) bubble?.visibility = View.VISIBLE
    }

    private fun removePanel() {
        panel?.let { runCatching { wm.removeView(it) } }
        panel = null
    }

    // ---------- Actions ----------
    private fun share(text: String) {
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
        startActivity(Intent.createChooser(send, "Share translation").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        closePanel()
    }

    /** Runs only when the user taps the button AND has consented in the app. */
    private fun fetchScreen(selection: Boolean) {
        if (!Prefs.screenConsent(this)) { vm.showMessage("Enable screen text access in the FloatTranslate app first."); return }
        val svc = ScreenTextService.instance
        if (svc == null) { vm.showMessage("Turn on the FloatTranslate accessibility service in Settings."); return }
        val text = if (selection) svc.selectedText() else svc.visibleText()
        if (text.isNullOrBlank()) vm.showMessage(if (selection) "No selected text found. Select text in the app first." else "No readable text found on screen.")
        else vm.setInput(text)
    }
}
