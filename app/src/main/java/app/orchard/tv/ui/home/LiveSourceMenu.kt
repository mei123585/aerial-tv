package app.orchard.tv.ui.home

import android.content.ComponentName
import android.view.KeyEvent as NativeKeyEvent
import androidx.compose.foundation.focusable
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
import androidx.compose.ui.focus.focusRequester
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
    var movingIndex by remember { mutableIntStateOf(-1) } // 移动模式
    val focusRequesters = remember { List(sources.size) { FocusRequester() } }
    LaunchedEffect(visible) {
        kotlinx.coroutines.delay(150)
        focusRequesters.getOrNull(currentIndex)?.requestFocus()
    }

    val gridMargin = 56.dp
    val gridGap = 28.dp
    val columns = 5
    val showAllNames = AerialSettings.showAllNames

    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
    ) {
        val screenW = maxWidth
        val tileW = (screenW - gridMargin * 2 - gridGap * (columns - 1)) / columns
        val tileH = tileW * 9f / 16f
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onPreviewKeyEvent { e ->
                    // 菜单键：呼出当前卡片上下文菜单（拦截，不让HomeScreen隐藏UI）
                    if (e.nativeKeyEvent.keyCode == NativeKeyEvent.KEYCODE_MENU && e.type == KeyEventType.KeyUp && menuForId == null && movingIndex < 0) {
                        sources.getOrNull(focusedIndex)?.let { menuForId = it.id }
                        true
                    } else if (e.nativeKeyEvent.keyCode == NativeKeyEvent.KEYCODE_BACK && e.type == KeyEventType.KeyUp) {
                        when {
                            menuForId != null -> { menuForId = null; true }
                            movingIndex >= 0 -> { movingIndex = -1; true }
                            else -> false
                        }
                    } else if (movingIndex >= 0) {
                        // 移动模式：方向键移动，OK完成
                        when {
                            e.nativeKeyEvent.keyCode == NativeKeyEvent.KEYCODE_DPAD_LEFT && e.type == KeyEventType.KeyUp && movingIndex > 0 -> {
                                onMove(movingIndex, movingIndex - 1)
                                movingIndex--
                                focusedIndex = movingIndex
                                true
                            }
                            e.nativeKeyEvent.keyCode == NativeKeyEvent.KEYCODE_DPAD_RIGHT && e.type == KeyEventType.KeyUp && movingIndex < sources.size - 1 -> {
                                onMove(movingIndex, movingIndex + 1)
                                movingIndex++
                                focusedIndex = movingIndex
                                true
                            }
                            e.nativeKeyEvent.keyCode == NativeKeyEvent.KEYCODE_DPAD_CENTER && e.type == KeyEventType.KeyUp -> {
                                focusRequesters.getOrNull(movingIndex)?.requestFocus()
                                movingIndex = -1
                                true
                            }
                            else -> true
                        }
                    } else false
                },
            contentAlignment = Alignment.BottomStart,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(gridGap),
                modifier = Modifier.padding(bottom = 100.dp).padding(horizontal = gridMargin),
            ) {
                sources.forEachIndexed { index, source ->
                    val app = remember(source.id, source.name) { liveSourceAppEntry(source) }
                    val showName = showAllNames || index == focusedIndex
                    AppTile(
                        app = app,
                        width = tileW,
                        height = tileH,
                        focusRequester = focusRequesters[index],
                        enabled = menuForId == null,
                        moving = movingIndex == index,
                        appearDelayMs = null,
                        onFocused = { focusedIndex = index; Sounds.focusMoved() },
                        onClick = { if (movingIndex < 0) { onSelect(source.id); onDismiss() } },
                        onLongPress = { if (movingIndex < 0) menuForId = source.id },
                        rowEnd = index == sources.size - 1,
                        showName = showName,
                    )
                }
            }
            // 移动模式提示
            if (movingIndex >= 0) {
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
                val source = sources.find { it.id == menuForId } ?: return@Box
                val sourceIndex = sources.indexOf(source)
                val first = remember { FocusRequester() }
                LaunchedEffect(menuForId) { first.requestFocus() }
                val tileCenterX = gridMargin + (tileW + gridGap) * sourceIndex + (tileW / 2)
                val menuWidth = 250.dp
                val menuOffsetX = tileCenterX - (menuWidth / 2)
                Column(
                    Modifier
                        .padding(start = menuOffsetX, bottom = 100.dp + tileH + 20.dp)
                        .width(menuWidth)
                        .liquidGlass(20.dp)
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    BasicText(source.name, style = TextStyle(Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 6.dp))
                    MenuRow(Glyph.Move, "移动", {
                        movingIndex = sourceIndex
                        menuForId = null
                        focusRequesters.getOrNull(sourceIndex)?.requestFocus()
                    }, focusRequester = first)
                    MenuRow(Glyph.EyeSlash, "隐藏", { onHide(source.id); menuForId = null })
                    if (!source.isPreset) {
                        MenuRow(Glyph.Trash, "删除", { onDelete(source.id); menuForId = null }, destructive = true)
                    }
                }
            }
        }
    }
}
