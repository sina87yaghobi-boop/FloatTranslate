package com.floattranslate.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.floattranslate.app.access.ScreenTextService
import com.floattranslate.app.data.Prefs
import com.floattranslate.app.overlay.OverlayService
import com.floattranslate.app.ui.FloatTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FloatTheme { Surface(Modifier.fillMaxSize()) { Home() } } }
    }
}

@Composable
private fun Home() {
    val ctx = LocalContext.current
    var canDraw by remember { mutableStateOf(Settings.canDrawOverlays(ctx)) }
    var running by remember { mutableStateOf(OverlayService.running) }
    var a11yOn by remember { mutableStateOf(ScreenTextService.instance != null) }
    var consent by remember { mutableStateOf(Prefs.screenConsent(ctx)) }
    var key by remember { mutableStateOf(Prefs.apiKey(ctx)) }
    var base by remember { mutableStateOf(Prefs.baseUrl(ctx)) }
    var model by remember { mutableStateOf(Prefs.model(ctx)) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        canDraw = Settings.canDrawOverlays(ctx)
        running = OverlayService.running
        a11yOn = ScreenTextService.instance != null
    }

    val notifPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    Column(
        Modifier.verticalScroll(rememberScrollState()).statusBarsPadding().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("FloatTranslate", style = MaterialTheme.typography.headlineMedium)
        Text("A translator bubble that floats over every app.", style = MaterialTheme.typography.bodyMedium)

        StepCard("1. Display over other apps", if (canDraw) "Granted" else "Required for the floating bubble") {
            if (!canDraw) Button(onClick = {
                ctx.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}")))
            }) { Text("Grant permission") }
        }

        StepCard("2. Start the bubble", if (running) "Bubble is running" else "Starts a foreground service") {
            Button(enabled = canDraw, onClick = {
                if (Build.VERSION.SDK_INT >= 33 &&
                    ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                ) notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
                if (running) {
                    ctx.stopService(Intent(ctx, OverlayService::class.java)); running = false
                } else {
                    ContextCompat.startForegroundService(ctx, Intent(ctx, OverlayService::class.java)); running = true
                }
            }) { Text(if (running) "Stop" else "Start") }
        }

        StepCard("3. Screen text (optional)", "Reads selected or visible text only when you tap the button in the panel. Nothing is sent anywhere until you press Translate.") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("I consent to screen text access", Modifier.weight(1f))
                Switch(checked = consent, onCheckedChange = { consent = it; Prefs.setScreenConsent(ctx, it) })
            }
            Text(if (a11yOn) "Accessibility service: ON" else "Accessibility service: OFF", style = MaterialTheme.typography.labelLarge)
            OutlinedButton(onClick = { ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }) { Text("Open accessibility settings") }
        }

        StepCard("4. Translation styles (optional)", "Formal / Informal / Professional need an OpenAI-compatible API. Standard works without a key.") {
            OutlinedTextField(key, { key = it }, label = { Text("API key") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(base, { base = it }, label = { Text("Base URL") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(model, { model = it }, label = { Text("Model") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Button(onClick = { Prefs.saveLlm(ctx, key, base, model) }) { Text("Save") }
        }
    }
}

@Composable
private fun StepCard(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
            content()
        }
    }
}
