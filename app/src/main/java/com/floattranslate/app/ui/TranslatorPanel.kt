package com.floattranslate.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.floattranslate.app.R
import com.floattranslate.app.data.Lang
import com.floattranslate.app.data.Languages
import com.floattranslate.app.data.Style

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TranslatorPanel(
    vm: TranslatorViewModel,
    onClose: () -> Unit,
    onShare: (String) -> Unit,
    onUseSelection: () -> Unit,
    onUseScreen: () -> Unit,
) {
    val s by vm.state.collectAsState()
    val clip = LocalClipboardManager.current
    var picker by remember { mutableStateOf<Int?>(null) } // 0 = مبدا, 1 = مقصد

    FloatTheme {
        val cs = MaterialTheme.colorScheme
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = cs.surface,
            shadowElevation = 16.dp,
            border = BorderStroke(1.2.dp, Brush.linearGradient(listOf(cs.secondary, cs.primary))),
        ) {
            Column(
                Modifier
                    .heightIn(max = 600.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
                    .animateContentSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // سربرگ
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(R.drawable.bubble_logo), null, Modifier.size(32.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("مترجم شناور", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    IconButton(onClick = onClose) { Icon(Icons.Default.Close, "بستن") }
                }

                // ترجمه‌ی متن‌های دیگران (غیرقابل کپی)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = onUseSelection,
                        label = { Text("متن انتخاب‌شده") },
                        leadingIcon = { Icon(Icons.Default.TextFields, null, Modifier.size(18.dp)) },
                    )
                    AssistChip(
                        onClick = onUseScreen,
                        label = { Text("خواندن متن صفحه") },
                        leadingIcon = { Icon(Icons.Default.Visibility, null, Modifier.size(18.dp)) },
                    )
                }

                // انتخاب زبان
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { picker = if (picker == 0) null else 0 }, modifier = Modifier.weight(1f)) {
                        Text(s.from.name, maxLines = 1)
                    }
                    IconButton(onClick = vm::swap) { Icon(Icons.Default.SwapHoriz, "جابه‌جایی زبان‌ها") }
                    FilledTonalButton(onClick = { picker = if (picker == 1) null else 1 }, modifier = Modifier.weight(1f)) {
                        Text(s.to.name, maxLines = 1)
                    }
                }

                AnimatedVisibility(picker != null, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                    val list = if (picker == 0) Languages.sources else Languages.all
                    val selected = if (picker == 0) s.from else s.to
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        list.forEach { l: Lang ->
                            FilterChip(
                                selected = l.code == selected.code,
                                onClick = { if (picker == 0) vm.setFrom(l) else vm.setTo(l); picker = null },
                                label = { Text(if (l.code == "auto" || l.name == l.native) l.name else "${l.name} · ${l.native}") },
                            )
                        }
                    }
                }

                // سبک ترجمه
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Style.values().forEach { st ->
                        FilterChip(selected = s.style == st, onClick = { vm.setStyle(st) }, label = { Text(st.label) })
                    }
                }

                // لیست متن‌های روی صفحه
                AnimatedVisibility(s.screenTexts.isNotEmpty(), enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "متن‌های روی صفحه — روی هرکدام بزن",
                                style = MaterialTheme.typography.labelLarge,
                                color = cs.secondary,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = { vm.useText(s.screenTexts.joinToString("\n")) }) { Text("ترجمه‌ی همه") }
                            IconButton(onClick = vm::clearScreenTexts) { Icon(Icons.Default.Close, "بستن لیست") }
                        }
                        Column(
                            Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            s.screenTexts.forEach { t ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { vm.useText(t) },
                                    shape = RoundedCornerShape(14.dp),
                                    color = cs.surfaceVariant,
                                ) {
                                    Text(
                                        t,
                                        Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }
                        }
                    }
                }

                // ورودی
                OutlinedTextField(
                    value = s.input,
                    onValueChange = vm::onInput,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("متن را بنویس یا بچسبان…") },
                    minLines = 3,
                    maxLines = 6,
                    shape = RoundedCornerShape(16.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    AssistChip(
                        onClick = { clip.getText()?.text?.let(vm::useText) },
                        label = { Text("چسباندن") },
                        leadingIcon = { Icon(Icons.Default.ContentPaste, null, Modifier.size(18.dp)) },
                    )
                    AssistChip(
                        onClick = vm::clear,
                        label = { Text("پاک کردن") },
                        leadingIcon = { Icon(Icons.Default.Delete, null, Modifier.size(18.dp)) },
                    )
                    Spacer(Modifier.weight(1f))
                    Button(onClick = vm::translateNow, enabled = s.input.isNotBlank() && !s.loading) { Text("ترجمه") }
                }

                if (s.loading) LinearProgressIndicator(Modifier.fillMaxWidth())

                // خروجی
                AnimatedVisibility(s.output.isNotBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = cs.secondaryContainer),
                        shape = RoundedCornerShape(20.dp),
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text(s.output, style = MaterialTheme.typography.bodyLarge)
                            Row {
                                Spacer(Modifier.weight(1f))
                                IconButton(onClick = { clip.setText(AnnotatedString(s.output)) }) { Icon(Icons.Default.ContentCopy, "کپی") }
                                IconButton(onClick = { onShare(s.output) }) { Icon(Icons.Default.Share, "اشتراک‌گذاری") }
                            }
                        }
                    }
                }

                s.message?.let { Text(it, color = cs.error, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}
