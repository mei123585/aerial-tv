package app.orchard.tv.ui.home

import android.view.KeyEvent as NativeKeyEvent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.orchard.tv.LiveSource
import app.orchard.tv.system.Sounds
import app.orchard.tv.ui.components.liquidGlass
import app.orchard.tv.ui.theme.Glyph

private val tileColors = listOf(
    Color(0xFFD32F2F), Color(0xFF1976D2), Color(0xFF388E3C), Color(0xFFF57C00),
    Color(0xFF7B1FA2), Color(0xFF00796B), Color(0xFF5D4037), Color(0xFF455A64),
)

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
    val focusRequesters = remember { List(sources.size) { FocusRequester() } }
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
                LiveSourceTile(
                    source = source,
                    tileW = tileW,
                    tileH = tileH,
                    isFocused = focusedIndex == index && menuForId == null,
                    isCurrent = source.id == currentId,
                    onClick = { onSelect(source.id); onDismiss() },
                    onMenu = { menuForId = source.id },
                    onBack = { if (menuForId != null) menuForId = null else onDismiss() },
                    onFocusChange = { if (it) { focusedIndex = index; Sounds.focusMoved() } },
                    focusRequester = focusRequesters[index],
                )
            }
        }
        if (menuForId != null) {
            val source = sources.find { it.id == menuForId } ?: return@Box
            val first = remember { FocusRequester() }
            LaunchedEffect(menuForId) { first.requestFocus() }
            Column(
                Modifier
                    .width(250.dp)
                    .liquidGlass(20.dp)
                    .padding(8.dp)
                    .align(Alignment.Center),
            ) {
                BasicText(source.name, style = TextStyle(Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 6.dp))
                MenuRow(Glyph.Pencil, "重命名", { onRename(source.id); menuForId = null }, focusRequester = first)
                MenuRow(Glyph.Globe, "编辑地址", { onEditUrl(source.id); menuForId = null })
                if (!source.isPreset) {
                    MenuRow(Glyph.Trash, "删除", { onDelete(source.id); menuForId = null }, destructive = true)
                }
                MenuRow(Glyph.Move, "取消", { menuForId = null })
            }
        }
    }
}

@Composable
private fun LiveSourceTile(
    source: LiveSource,
    tileW: androidx.compose.ui.unit.Dp,
    tileH: androidx.compose.ui.unit.Dp,
    isFocused: Boolean,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onMenu: () -> Unit,
    onBack: () -> Unit,
    onFocusChange: (Boolean) -> Unit,
    focusRequester: FocusRequester,
) {
    val scale by animateFloatAsState(if (isFocused) 1.1f else 1f, spring(0.86f, 300f), label = "scale")
    val bgColor = tileColors[source.colorIndex % tileColors.size]

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .scale(scale)
            .focusRequester(focusRequester)
            .onFocusChanged { onFocusChange(it.isFocused) }
            .focusable()
            .onKeyEvent { e ->
                when (e.nativeKeyEvent.keyCode) {
                    NativeKeyEvent.KEYCODE_MENU -> { if (e.type == KeyEventType.KeyUp) onMenu(); true }
                    NativeKeyEvent.KEYCODE_BACK -> { if (e.type == KeyEventType.KeyUp) onBack(); true }
                    NativeKeyEvent.KEYCODE_DPAD_CENTER, NativeKeyEvent.KEYCODE_ENTER -> {
                        if (e.type == KeyEventType.KeyUp) { Sounds.select(); onClick(); true } else false
                    }
                    else -> false
                }
            },
    ) {
        Box(
            modifier = Modifier
                .width(tileW)
                .height(tileH)
                .clip(RoundedCornerShape(28.dp))
                .background(bgColor),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                if (isCurrent) "▶" else source.name.take(1),
                style = TextStyle(Color.White, fontSize = 48.sp, fontWeight = FontWeight.Bold),
            )
        }
        BasicText(
            text = source.name,
            style = TextStyle(
                color = Color.White,
                textAlign = TextAlign.Center,
                fontSize = 15.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
            ),
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}
