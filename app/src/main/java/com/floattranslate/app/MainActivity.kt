package com.floattranslate.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.floattranslate.app.access.ScreenTextService
import com.floattranslate.app.data.Prefs
import com.floattranslate.app.overlay.OverlayService
import com.floattranslate.app.ui.FloatTheme
import com.floattranslate.app.ui.LavaColors

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { FloatTheme { Home() } }
    }
}

private fun openChannel(ctx: Context) {
    val i = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/sina_zino")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { ctx.startActivity(i) }
}

@Composable
private fun Home() {
    val ctx = LocalContext.current
    val cs = MaterialTheme.colorScheme
    var canDraw by remember { mutableStateOf(Settings.canDrawOverlays(ctx)) }
    var running by remember { mutableStateOf(OverlayService.running) }
    var a11yOn by remember { mutableStateOf(ScreenTextService.instance != null) }
    var consent by remember { mutableStateOf(Prefs.screenConsent(ctx)) }
    var key by remember { mutableStateOf(Prefs.apiKey(ctx)) }
    var base by remember { mutableStateOf(Prefs.baseUrl(ctx)) }
    var model by remember { mutableStateOf(Prefs.model(ctx)) }
    var saved by remember { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        canDraw = Settings.canDrawOverlays(ctx)
        running = OverlayService.running
        a11yOn = ScreenTextService.instance != null
    }

    val notifPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    Surface(Modifier.fillMaxSize(), color = cs.background) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(0f to cs.background, 0.65f to cs.background, 1f to cs.primary.copy(alpha = 0.16f))
            )
        ) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    // سربرگ
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Image(painterResource(R.drawable.logo_zino_round), null, Modifier.size(104.dp).clip(CircleShape))
                        Text("مترجم شناور", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            "هر جای گوشی هستی، بدون بیرون آمدن از برنامه ترجمه کن",
                            style = MaterialTheme.typography.bodyMedium,
                            color = cs.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }

                    StepCard("۱. اجازه‌ی نمایش روی برنامه‌ها", if (canDraw) "داده شده ✓" else "برای نمایش حباب شناور لازم است") {
                        if (!canDraw) Button(onClick = {
                            ctx.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}")))
                        }) { Text("دادن اجازه") }
                    }

                    StepCard("۲. روشن کردن حباب", if (running) "حباب روشن است" else "حباب شناور روی همه‌ی برنامه‌ها نمایش داده می‌شود") {
                        Button(enabled = canDraw, onClick = {
                            if (Build.VERSION.SDK_INT >= 33 &&
                                ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                            ) notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
                            if (running) {
                                ctx.stopService(Intent(ctx, OverlayService::class.java)); running = false
                            } else {
                                ContextCompat.startForegroundService(ctx, Intent(ctx, OverlayService::class.java)); running = true
                            }
                        }) { Text(if (running) "خاموش کن" else "روشن کن") }
                    }

                    StepCard(
                        "۳. ترجمه‌ی متن‌های روی صفحه (اختیاری)",
                        "برای ترجمه‌ی پیام‌ها و متن‌هایی که نمی‌توانی کپی کنی. متن فقط وقتی خوانده می‌شود که خودت دکمه‌ی «خواندن متن صفحه» یا «متن انتخاب‌شده» را در پنل بزنی، و تا ترجمه را نزنی جایی فرستاده نمی‌شود. فیلد رمز عبور خوانده نمی‌شود.",
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("با خواندن متن صفحه موافقم", Modifier.weight(1f))
                            Switch(checked = consent, onCheckedChange = { consent = it; Prefs.setScreenConsent(ctx, it) })
                        }
                        Text(
                            if (a11yOn) "سرویس دسترسی‌پذیری: روشن ✓" else "سرویس دسترسی‌پذیری: خاموش",
                            style = MaterialTheme.typography.labelLarge,
                        )
                        OutlinedButton(onClick = { ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }) {
                            Text("باز کردن تنظیمات دسترسی‌پذیری")
                        }
                        Text(
                            "اگر گزینه‌ی FloatTranslate خاکستری بود: اطلاعات برنامه ← سه‌نقطه‌ی بالا ← «اجازه‌ی تنظیمات محدودشده»",
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant,
                        )
                        OutlinedButton(onClick = {
                            ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}")))
                        }) { Text("باز کردن اطلاعات برنامه") }
                    }

                    StepCard(
                        "۴. سبک‌های ترجمه (اختیاری)",
                        "سبک‌های رسمی، خودمونی و حرفه‌ای به یک API سازگار با OpenAI نیاز دارند. حالت استاندارد بدون کلید کار می‌کند.",
                    ) {
                        LtrField(key, "کلید API") { key = it; saved = false }
                        LtrField(base, "آدرس پایه (Base URL)") { base = it; saved = false }
                        LtrField(model, "مدل") { model = it; saved = false }
                        Button(onClick = { Prefs.saveLlm(ctx, key, base, model); saved = true }) {
                            Text(if (saved) "ذخیره شد ✓" else "ذخیره")
                        }
                    }
                }
                TelegramButton(Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
            }
        }
    }
}

@Composable
private fun LtrField(value: String, label: String, onChange: (String) -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        OutlinedTextField(value, onChange, label = { Text(label) }, singleLine = true, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun StepCard(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    val cs = MaterialTheme.colorScheme
    OutlinedCard(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(containerColor = cs.surface),
        border = BorderStroke(1.dp, cs.outlineVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = cs.secondary)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            content()
        }
    }
}

@Composable
private fun TelegramButton(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.horizontalGradient(listOf(LavaColors.Magma, LavaColors.Ember)))
            .clickable { openChannel(ctx) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Image(painterResource(R.drawable.logo_zino_round), null, Modifier.size(46.dp).clip(CircleShape))
        Column(Modifier.weight(1f)) {
            Text("ورود به کانال زینو", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
            Text("\u200E@sina_zino", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f))
        }
        Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.White)
    }
}
