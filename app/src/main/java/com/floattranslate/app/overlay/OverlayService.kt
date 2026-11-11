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
import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
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
            .setSmallIcon(R.drawable.ic_stat_translate)
            .setContentTitle("مترجم شناور فعال است")
            .setContentText(if (bubbleVisible) "حباب نمایان است" else "حباب پنهان است")
            .setContentIntent(open)
            .setOngoing(true)
            .addAction(0, if (bubbleVisible) "پنهان کردن حباب" else "نمایش حباب", action(ACTION_TOGGLE, 1))
            .addAction(0, "توقف", action(ACTION_STOP, 2))
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
                Image(
                    painterResource(R.drawable.bubble_logo),
                    contentDescription = "ترجمه",
                    modifier = Modifier.size(56.dp).shadow(8.dp, CircleShape),
                )
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
    private var panelParams: WindowManager.LayoutParams? = null
    private var lastX = -1
    private var lastY = -1
    private var lastW = 0
    private var lastH = 0

    /** Panel window root: back key, drag the header to move, drag the bottom-right corner to resize. */
    private inner class PanelFrame(c: Context, var onBack: () -> Unit) : FrameLayout(c) {
        private val slop = ViewConfiguration.get(c).scaledTouchSlop
        private var armed = 0 // 1 = move, 2 = resize
        private var mode = 0
        private var downX = 0f
        private var downY = 0f
        private var startX = 0
        private var startY = 0
        private var startW = 0
        private var startH = 0

        override fun dispatchKeyEvent(e: KeyEvent): Boolean {
            if (e.keyCode == KeyEvent.KEYCODE_BACK && e.action == KeyEvent.ACTION_UP) { onBack(); return true }
            return super.dispatchKeyEvent(e)
        }

        override fun onInterceptTouchEvent(e: MotionEvent): Boolean {
            val p = panelParams ?: return false
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = e.rawX; downY = e.rawY
                    startX = p.x; startY = p.y; startW = p.width; startH = p.height
                    armed = when {
                        e.x > width - 48.dp() && e.y > height - 48.dp() -> 2
                        e.y < 64.dp() -> 1
                        else -> 0
                    }
                    mode = 0
                }
                MotionEvent.ACTION_MOVE -> {
                    if (armed != 0 && mode == 0 && (abs(e.rawX - downX) > slop || abs(e.rawY - downY) > slop)) {
                        mode = armed
                        return true
                    }
                }
            }
            return false
        }

        override fun onTouchEvent(e: MotionEvent): Boolean {
            val p = panelParams ?: return false
            val dm = resources.displayMetrics
            when (e.actionMasked) {
                MotionEvent.ACTION_MOVE -> {
                    val dx = (e.rawX - downX).toInt()
                    val dy = (e.rawY - downY).toInt()
                    if (mode == 1) {
                        p.x = (startX + dx).coerceIn(0, maxOf(0, dm.widthPixels - p.width))
                        p.y = (startY + dy).coerceIn(0, maxOf(0, dm.heightPixels - 120.dp()))
                    } else if (mode == 2) {
                        p.width = (startW + dx).coerceIn(minOf(260.dp(), dm.widthPixels), maxOf(260.dp(), dm.widthPixels - p.x))
                        p.height = (startH + dy).coerceIn(minOf(260.dp(), dm.heightPixels), maxOf(260.dp(), dm.heightPixels - p.y))
                    }
                    if (mode != 0) wm.updateViewLayout(this, p)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> mode = 0
            }
            return true
        }
    }

    private fun openPanel() {
        if (panel != null) return
        val dm = resources.displayMetrics
        val w = (if (lastW > 0) lastW else (dm.widthPixels * 0.92f).toInt()).coerceAtMost(dm.widthPixels)
        val h = (if (lastH > 0) lastH else minOf((dm.heightPixels * 0.72f).toInt(), 640.dp())).coerceAtMost(dm.heightPixels)
        val p = WindowManager.LayoutParams(
            w, h, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (if (lastX >= 0) lastX else (dm.widthPixels - w) / 2).coerceIn(0, maxOf(0, dm.widthPixels - w))
            y = (if (lastY >= 0) lastY else 72.dp()).coerceIn(0, maxOf(0, dm.heightPixels - 120.dp()))
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }
        panelParams = p
        val root = PanelFrame(this) { closePanel() }
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
        panelParams?.let { lastX = it.x; lastY = it.y; lastW = it.width; lastH = it.height }
        panel?.let { runCatching { wm.removeView(it) } }
        panel = null
        panelParams = null
    }

    // ---------- Actions ----------
    private fun share(text: String) {
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
        startActivity(Intent.createChooser(send, "اشتراک‌گذاری ترجمه").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        closePanel()
    }

    /** Runs only when the user taps the button AND has consented in the app. */
    private fun fetchScreen(selection: Boolean) {
        if (!Prefs.screenConsent(this)) { vm.showMessage("اول در برنامه، گزینه‌ی «موافقم» را برای خواندن متن صفحه روشن کن."); return }
        val svc = ScreenTextService.instance
        if (svc == null) { vm.showMessage("سرویس دسترسی‌پذیری FloatTranslate روشن نیست. از تنظیمات دسترسی‌پذیری روشنش کن."); return }
        if (selection) {
            val t = svc.selectedText()
            if (t.isNullOrBlank()) vm.showMessage("متن انتخاب‌شده‌ای پیدا نشد. اول متن را انتخاب کن.") else vm.useText(t)
        } else {
            val list = svc.visibleTexts()
            if (list.isEmpty()) vm.showMessage("متنی روی صفحه پیدا نشد.") else vm.setScreenTexts(list)
        }
    }
}
