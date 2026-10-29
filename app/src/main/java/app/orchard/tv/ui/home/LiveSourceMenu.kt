package app.orchard.tv.ui.home

import android.content.ComponentName
import android.view.KeyEvent as NativeKeyEvent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.orchard.tv.LiveSource
import app.orchard.tv.apps.AppEntry
import app.orchard.tv.apps.TileArtFactory
import app.orchard.tv.settings.AerialSettings
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
    onMove: (Int, Int) -> Unit,
    onHide: (String) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    if (!visible) return
    val currentIndex = sources.indexOfFirst { it.id == currentId }.coerceAtLeast(0)
    var focusedIndex by remember { mutableIntStateOf(currentIndex) }
    var menuForId by remember { mutableStateOf<String?>(null) }
    var movingId by remember { mutableStateOf<String?>(null) }
    // 每个source.id对应一个FocusRequester
    val focusMap = remember { mutableMapOf<String, FocusRequester>() }
    LaunchedEffect(visible) {
        kotlinx.coroutines.delay(150)
        sources.find { it.id == currentId }?.let { focusMap[it.id]?.requestFocus() }
    }

    val gridMargin = 56.dp
    val gridGap = 28.dp
    val columns = 5
    val showAllNames = AerialSettings.showAllNames

    androidx.compose.foundation.layout.BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenW = maxWidth
        val tileW = (screenW - gridMargin * 2 - gridGap * (columns - 1)) / columns
        val tileH = tileW * 9f / 16f
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onPreviewKeyEvent { e ->
                    if (e.nativeKeyEvent.keyCode == NativeKeyEvent.KEYCODE_MENU && e.type == KeyEventType.KeyUp && menuForId == null && movingId == null) {
                        sources.getOrNull(focusedIndex)?.let { menuForId = it.id }
                        true
                    } else if (e.nativeKeyEvent.keyCode == NativeKeyEvent.KEYCODE_BACK && e.type == KeyEventType.KeyUp) {
                        when {
                            menuForId != null -> { menuForId = null; true }
                            movingId != null -> { movingId = null; true }
                            else -> false
                        }
                    } else if (movingId != null) {
                        val from = sources.indexOfFirst { it.id == movingId }
                        when {
                            e.nativeKeyEvent.keyCode == NativeKeyEvent.KEYCODE_DPAD_LEFT && e.type == KeyEventType.KeyUp && from > 0 -> {
                                onMove(from, from - 1); true
                            }
                            e.nativeKeyEvent.keyCode == NativeKeyEvent.KEYCODE_DPAD_RIGHT && e.type == KeyEventType.KeyUp && from < sources.size - 1 -> {
                                onMove(from, from + 1); true
                            }
                            e.nativeKeyEvent.keyCode == NativeKeyEvent.KEYCODE_DPAD_CENTER && e.type == KeyEventType.KeyUp -> {
                                movingId = null; true
                            }
                            else -> true
                        }
                    } else false
                },
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(gridGap),
                modifier = Modifier.align(Alignment.BottomStart).padding(bottom = 100.dp).padding(horizontal = gridMargin),
            ) {
                sources.forEachIndexed { index, source ->
                    val app = remember(source.id, source.name) { liveSourceAppEntry(source) }
                    val showName = showAllNames || index == focusedIndex
                    val req = remember { FocusRequester() }
                    LaunchedEffect(source.id) { focusMap[source.id] = req }
                    androidx.compose.runtime.key(source.id) {
                        AppTile(
                            app = app,
                            width = tileW,
                            height = tileH,
                            focusRequester = req,
                            enabled = menuForId == null,
                            moving = movingId == source.id,
                            appearDelayMs = null,
                            onFocused = { focusedIndex = index; Sounds.focusMoved() },
                            onClick = { if (movingId == null) { onSelect(source.id); onDismiss() } },
                            onLongPress = { if (movingId == null) menuForId = source.id },
                            rowEnd = index == sources.size - 1,
                            showName = showName,
                        )
                    }
                }
            }
            if (movingId != null) {
                BasicText(
                    "用方向键移动·按确定键完成",
                    style = TextStyle(Color.White, fontSize = 14.sp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 28.dp)
                        .liquidGlass(20.dp)
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                )
            }
            if (menuForId != null) {
                val source = sources.find { it.id == menuForId } ?: return@BoxWithConstraints
                val sourceIndex = sources.indexOf(source)
                val first = remember { FocusRequester() }
                LaunchedEffect(menuForId) { first.requestFocus() }
                Column(
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = gridMargin + (tileW + gridGap) * sourceIndex + tileW / 2 - 125.dp, bottom = 100.dp + tileH + 20.dp)
                        .width(250.dp)
                        .liquidGlass(20.dp)
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    BasicText(source.name, style = TextStyle(Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 6.dp))
                    MenuRow(Glyph.Move, "移动", { movingId = source.id; menuForId = null; focusMap[source.id]?.requestFocus() }, focusRequester = first)
                    MenuRow(Glyph.EyeSlash, "隐藏", { onHide(source.id); menuForId = null })
                    if (!source.isPreset) {
                        MenuRow(Glyph.Trash, "删除", { onDelete(source.id); menuForId = null }, destructive = true)
                    }
                }
            }
        }
    }
}
