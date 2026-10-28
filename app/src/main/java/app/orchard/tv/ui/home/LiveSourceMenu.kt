package app.orchard.tv.ui.home

import android.content.ComponentName
import android.view.KeyEvent as NativeKeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.orchard.tv.LiveSource
import app.orchard.tv.apps.AppEntry
import app.orchard.tv.apps.TileArtFactory
import app.orchard.tv.system.Sounds
import app.orchard.tv.ui.components.liquidGlass
import app.orchard.tv.ui.theme.Glyph

private val liveColors = listOf(
    Pair(0xFFD32F2F.toInt(), 0xFF8E0000.toInt()),
    Pair(0xFF1976D2.toInt(), 0xFF0D47A1.toInt()),
    Pair(0xFF388E3C.toInt(), 0xFF1B5E20.toInt()),
    Pair(0xFFF57C00.toInt(), 0xFFE65100.toInt()),
    Pair(0xFF7B1FA2.toInt(), 0xFF4A148C.toInt()),
    Pair(0xFF00796B.toInt(), 0xFF004D40.toInt()),
)

private fun liveSourceAppEntry(source: LiveSource): AppEntry {
    val (top, bottom) = liveColors[source.colorIndex % liveColors.size]
    val art = TileArtFactory.glyphTile(top, bottom, Glyph.Play)
    return AppEntry(
        packageName = "live_${source.id}",
        label = source.name,
        component = ComponentName("app.orchard.tv", "app.orchard.tv.MainActivity"),
        leanback = true,
        removable = !source.isPreset,
        art = art,
    )
}

@Composable
fun LiveSourceMenu(
    visible: Boolean,
    sources: List<LiveSource>,
    currentId: String,
    onSelect: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onEditUrl: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    if (!visible) return
    val currentIndex = sources.indexOfFirst { it.id == currentId }.coerceAtLeast(0)
    var focusedIndex by remember { mutableIntStateOf(currentIndex) }
    var menuForId by remember { mutableStateOf<String?>(null) }
    var editMode by remember { mutableStateOf<EditMode?>(null) }
    val focusRequesters = remember { List(sources.size) { FocusRequester() } }
    LaunchedEffect(visible) {
        kotlinx.coroutines.delay(150)
        focusRequesters.getOrNull(currentIndex)?.requestFocus()
    }

    val gridMargin = 56.dp
    val gridGap = 28.dp
    val columns = 5

    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
    ) {
        val screenW = maxWidth
        val tileW = (screenW - gridMargin * 2 - gridGap * (columns - 1)) / columns
        val tileH = tileW * 9f / 16f
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onKeyEvent { e ->
                    if (e.nativeKeyEvent.keyCode == NativeKeyEvent.KEYCODE_MENU && e.type == KeyEventType.KeyUp && menuForId == null && editMode == null) {
                        sources.getOrNull(focusedIndex)?.let { menuForId = it.id }
                        true
                    } else false
                },
            contentAlignment = Alignment.BottomCenter,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(gridGap),
                modifier = Modifier.padding(bottom = 80.dp).padding(horizontal = gridMargin),
            ) {
            sources.forEachIndexed { index, source ->
                val app = remember(source.id, source.name) { liveSourceAppEntry(source) }
                AppTile(
                    app = app,
                    width = tileW,
                    height = tileH,
                    focusRequester = focusRequesters[index],
                    enabled = true,
                    moving = false,
                    appearDelayMs = null,
                    onFocused = { focusedIndex = index; Sounds.focusMoved() },
                    onClick = { onSelect(source.id); onDismiss() },
                    onLongPress = { menuForId = source.id },
                    rowEnd = index == sources.size - 1,
                )
            }
        }
        if (menuForId != null && editMode == null) {
            val source = sources.find { it.id == menuForId } ?: return@Box
            val first = remember { FocusRequester() }
            LaunchedEffect(menuForId) { first.requestFocus() }
            Column(
                Modifier.width(250.dp).liquidGlass(20.dp).padding(8.dp).align(Alignment.Center),
            ) {
                BasicText(source.name, style = TextStyle(Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 6.dp))
                MenuRow(Glyph.Pencil, "重命名", { editMode = EditMode.Rename(source.id) }, focusRequester = first)
                MenuRow(Glyph.Globe, "编辑地址", { editMode = EditMode.Url(source.id) })
                if (!source.isPreset) {
                    MenuRow(Glyph.Trash, "删除", { onDelete(source.id); menuForId = null }, destructive = true)
                }
                MenuRow(Glyph.ChevronRight, "取消", { menuForId = null })
            }
        }
        editMode?.let { mode ->
            val (id, isRename) = when (mode) {
                is EditMode.Rename -> mode.id to true
                is EditMode.Url -> mode.id to false
            }
            val source = sources.find { it.id == id } ?: return@let
            TextEditDialog(
                title = if (isRename) "重命名" else "编辑地址",
                initial = if (isRename) source.name else source.url,
                onConfirm = { value ->
                    if (isRename) onRename(id, value) else onEditUrl(id, value)
                    editMode = null; menuForId = null
                },
                onDismiss = { editMode = null },
            )
        }
        }
    }
}

private sealed class EditMode {
    data class Rename(val id: String) : EditMode()
    data class Url(val id: String) : EditMode()
}

@Composable
private fun TextEditDialog(
    title: String,
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    val confirmRequester = remember { FocusRequester() }
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.width(420.dp).clip(RoundedCornerShape(20.dp)).liquidGlass(20.dp).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BasicText(title, style = TextStyle(Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold), modifier = Modifier.padding(bottom = 16.dp))
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                textStyle = TextStyle(Color.White, fontSize = 15.sp),
                modifier = Modifier.fillMaxWidth().background(Color.White.copy(0.15f), RoundedCornerShape(10.dp)).padding(12.dp),
            )
            Row(modifier = Modifier.padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Color.White.copy(0.15f)).padding(horizontal = 24.dp, vertical = 10.dp).focusable()
                        .onKeyEvent { e -> if (e.nativeKeyEvent.keyCode == NativeKeyEvent.KEYCODE_DPAD_CENTER && e.type == androidx.compose.ui.input.key.KeyEventType.KeyUp) { onDismiss(); true } else false },
                ) { BasicText("取消", style = TextStyle(Color.White, fontSize = 14.sp)) }
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Color(0xFF0A84FF)).padding(horizontal = 24.dp, vertical = 10.dp).focusRequester(confirmRequester).focusable()
                        .onKeyEvent { e -> if (e.nativeKeyEvent.keyCode == NativeKeyEvent.KEYCODE_DPAD_CENTER && e.type == androidx.compose.ui.input.key.KeyEventType.KeyUp) { Sounds.select(); onConfirm(text); true } else false },
                ) { BasicText("确定", style = TextStyle(Color.White, fontSize = 14.sp)) }
            }
        }
    }
    LaunchedEffect(Unit) { confirmRequester.requestFocus() }
}
