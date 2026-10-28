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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
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
    onRename: (String) -> Unit,
    onEditUrl: (String) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    if (!visible) return
    val currentIndex = sources.indexOfFirst { it.id == currentId }.coerceAtLeast(0)
    var focusedIndex by remember { mutableIntStateOf(currentIndex) }
    var menuForId by remember { mutableStateOf<String?>(null) }
    val focusRequesters = remember { List(sources.size) { androidx.compose.ui.focus.FocusRequester() } }
    LaunchedEffect(visible) {
        kotlinx.coroutines.delay(150)
        focusRequesters.getOrNull(currentIndex)?.requestFocus()
    }

    val screenW = LocalConfiguration.current.screenWidthDp.dp
    val gridMargin = 56.dp
    val gridGap = 28.dp
    val columns = 5
    val tileW = (screenW - gridMargin * 2 - gridGap * (columns - 1)) / columns
    val tileH = tileW * 9f / 16f

    Box(
        modifier = Modifier.fillMaxSize(),
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
        if (menuForId != null) {
            val source = sources.find { it.id == menuForId } ?: return@Box
            val first = remember { androidx.compose.ui.focus.FocusRequester() }
            LaunchedEffect(menuForId) { first.requestFocus() }
            Column(
                Modifier
                    .width(250.dp)
                    .liquidGlass(20.dp)
                    .padding(8.dp)
                    .align(Alignment.Center),
            ) {
                BasicText(source.name, style = TextStyle(androidx.compose.ui.graphics.Color.White, fontSize = 17.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                    modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 6.dp))
                MenuRow(Glyph.Pencil, "重命名", { onRename(source.id); menuForId = null }, focusRequester = first)
                MenuRow(Glyph.Globe, "编辑地址", { onEditUrl(source.id); menuForId = null })
                if (!source.isPreset) {
                    MenuRow(Glyph.Trash, "删除", { onDelete(source.id); menuForId = null }, destructive = true)
                }
                MenuRow(Glyph.ChevronRight, "取消", { menuForId = null })
            }
        }
    }
}
