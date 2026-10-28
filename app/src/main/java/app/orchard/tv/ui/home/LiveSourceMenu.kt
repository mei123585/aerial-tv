package app.orchard.tv.ui.home

import android.view.KeyEvent as NativeKeyEvent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.orchard.tv.LiveSource
import app.orchard.tv.system.Sounds

// 预设源的配色
private val presetColors = listOf(
    Color(0xFFD32F2F), Color(0xFF1976D2), Color(0xFF388E3C), Color(0xFFF57C00),
    Color(0xFF7B1FA2), Color(0xFF00796B), Color(0xFF5D4037), Color(0xFF455A64),
)

@Composable
fun LiveSourceMenu(
    visible: Boolean,
    sources: List<LiveSource>,
    currentId: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    if (!visible) return
    val currentIndex = sources.indexOfFirst { it.id == currentId }.coerceAtLeast(0)
    var focusedIndex by remember { mutableIntStateOf(currentIndex) }
    val focusRequesters = remember { List(sources.size) { FocusRequester() } }
    val gridState = rememberLazyGridState()

    LaunchedEffect(visible) {
        kotlinx.coroutines.delay(150)
        focusRequesters.getOrNull(currentIndex)?.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BasicText(
                "选择直播源",
                style = TextStyle(Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 30.dp),
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                state = gridState,
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp),
                modifier = Modifier.padding(horizontal = 60.dp),
            ) {
                itemsIndexed(sources) { index, source ->
                    LiveSourceTile(
                        source = source,
                        isFocused = focusedIndex == index,
                        isCurrent = source.id == currentId,
                        onClick = { onSelect(source.id); onDismiss() },
                        onFocusChange = { if (it) { focusedIndex = index; Sounds.focusMoved() } },
                        focusRequester = focusRequesters[index],
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveSourceTile(
    source: LiveSource,
    isFocused: Boolean,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onFocusChange: (Boolean) -> Unit,
    focusRequester: FocusRequester,
) {
    val tileSize = 130.dp
    val scale by animateFloatAsState(if (isFocused) 1.1f else 1f, spring(0.86f, 300f), label = "scale")
    val bgColor = presetColors[source.colorIndex % presetColors.size]

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .scale(scale)
            .focusRequester(focusRequester)
            .onFocusChanged { onFocusChange(it.isFocused) }
            .focusable()
            .onKeyEvent { e ->
                if ((e.nativeKeyEvent.keyCode == NativeKeyEvent.KEYCODE_DPAD_CENTER ||
                            e.nativeKeyEvent.keyCode == NativeKeyEvent.KEYCODE_ENTER) &&
                    e.type == KeyEventType.KeyUp) {
                    Sounds.select()
                    onClick()
                    true
                } else false
            },
    ) {
        Box(
            modifier = Modifier
                .size(tileSize)
                .clip(RoundedCornerShape(28.dp))
                .background(bgColor)
                .border(if (isFocused) 3.dp else 0.dp, Color.White, RoundedCornerShape(28.dp)),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                if (isCurrent) "▶" else source.name.take(1),
                style = TextStyle(Color.White, fontSize = 44.sp, fontWeight = FontWeight.Bold),
            )
        }
        BasicText(
            text = source.name,
            style = TextStyle(
                color = Color.White,
                textAlign = TextAlign.Center,
                fontSize = 16.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
            ),
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}
