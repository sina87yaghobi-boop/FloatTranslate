package com.floattranslate.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.floattranslate.app.data.Lang
import com.floattranslate.app.data.Languages
import com.floattranslate.app.data.Style

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
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
    var picker by remember { mutableStateOf<Int?>(null) } // 0 = source, 1 = target

    FloatTheme {
        Surface(
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 6.dp,
            shadowElevation = 16.dp,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                Modifier
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
                    .animateContentSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Translate, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("FloatTranslate", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    IconButton(onClick = onUseSelection) { Icon(Icons.Default.TextFields, "Use selected text") }
                    IconButton(onClick = onUseScreen) { Icon(Icons.Default.Visibility, "Translate screen text") }
                    IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Close") }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = { picker = if (picker == 0) null else 0 }, modifier = Modifier.weight(1f)) {
                        Text(s.from.name, maxLines = 1)
                    }
                    IconButton(onClick = vm::swap) { Icon(Icons.Default.SwapHoriz, "Swap languages") }
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
                                label = { Text(if (l.code == "auto") l.name else "${l.native} · ${l.name}") },
                            )
                        }
                    }
                }

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Style.values().forEach { st ->
                        FilterChip(selected = s.style == st, onClick = { vm.setStyle(st) }, label = { Text(st.label) })
                    }
                }

                OutlinedTextField(
                    value = s.input,
                    onValueChange = vm::onInput,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Type or paste text…") },
                    minLines = 3,
                    maxLines = 6,
                    shape = RoundedCornerShape(16.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    AssistChip(
                        onClick = { clip.getText()?.text?.let(vm::setInput) },
                        label = { Text("Paste") },
                        leadingIcon = { Icon(Icons.Default.ContentPaste, null, Modifier.size(18.dp)) },
                    )
                    AssistChip(
                        onClick = vm::clear,
                        label = { Text("Clear") },
                        leadingIcon = { Icon(Icons.Default.Delete, null, Modifier.size(18.dp)) },
                    )
                    Spacer(Modifier.weight(1f))
                    Button(onClick = vm::translateNow, enabled = s.input.isNotBlank() && !s.loading) { Text("Translate") }
                }

                if (s.loading) LinearProgressIndicator(Modifier.fillMaxWidth())

                AnimatedVisibility(s.output.isNotBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        shape = RoundedCornerShape(20.dp),
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text(s.output, style = MaterialTheme.typography.bodyLarge)
                            Row {
                                Spacer(Modifier.weight(1f))
                                IconButton(onClick = { clip.setText(AnnotatedString(s.output)) }) { Icon(Icons.Default.ContentCopy, "Copy") }
                                IconButton(onClick = { onShare(s.output) }) { Icon(Icons.Default.Share, "Share") }
                            }
                        }
                    }
                }

                s.message?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}
