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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.orchard.tv.LiveSource

@Composable
fun LiveSourceMenu(
    visible: Boolean,
    sources: List<LiveSource>,
    currentIndex: Int,
    onSelect: (Int) -> Unit,
    onCustomSave: (name: String, url: String) -> Unit,
    onDismiss: () -> Unit,
) {
    if (!visible) return
    var focusedIndex by remember { mutableIntStateOf(currentIndex) }
    var showInputDialog by remember { mutableStateOf(false) }
    val focusRequesters = remember { List(5) { FocusRequester() } }
    LaunchedEffect(visible) { focusRequesters.getOrNull(currentIndex)?.requestFocus() }

    Box(modifier = Modifier.fillMaxSize().padding(bottom = 30.dp), contentAlignment = Alignment.BottomCenter) {
        Row(horizontalArrangement = Arrangement.spacedBy(28.dp), verticalAlignment = Alignment.CenterVertically) {
            sources.forEachIndexed { index, source ->
                LiveSourceCard(
                    source = source,
                    isFocused = focusedIndex == index,
                    onClick = {
                        if (index == 4 && source.url.isBlank()) showInputDialog = true
                        else { onSelect(index); onDismiss() }
                    },
                    onFocusChange = { if (it) focusedIndex = index },
                    focusRequester = focusRequesters[index],
                )
            }
        }
    }
    if (showInputDialog) {
        CustomSourceInputDialog(
            onConfirm = { n, u -> onCustomSave(n, u); showInputDialog = false; onSelect(4); onDismiss() },
            onDismiss = { showInputDialog = false },
        )
    }
}

@Composable
private fun LiveSourceCard(
    source: LiveSource,
    isFocused: Boolean,
    onClick: () -> Unit,
    onFocusChange: (Boolean) -> Unit,
    focusRequester: FocusRequester,
) {
    // 和原生图标一样的大小：宽高比9:16，焦点时放大1.1倍
    val cardWidth = 180.dp
    val cardHeight = cardWidth * 9f / 16f
    val scale by animateFloatAsState(if (isFocused) 1.1f else 1f, spring(0.86f, 300f), label="scale")
    
    Box(
        modifier = Modifier
            .scale(scale)
            .width(cardWidth)
            .size(width = cardWidth, height = cardHeight)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isFocused) Color.White.copy(0.2f) else Color.Transparent)
            .border(if (isFocused) 2.dp else 0.dp, Color.White, RoundedCornerShape(16.dp))
            .focusRequester(focusRequester)
            .onFocusChanged { onFocusChange(it.isFocused) }
            .focusable()
            .onKeyEvent { e ->
                if ((e.nativeKeyEvent.keyCode == NativeKeyEvent.KEYCODE_DPAD_CENTER ||
                            e.nativeKeyEvent.keyCode == NativeKeyEvent.KEYCODE_ENTER) &&
                    e.type == KeyEventType.KeyUp) {
                    onClick()
                    true
                } else false
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // 图标区域，和原生图标一样大
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFCC0000)),
                contentAlignment = Alignment.Center,
            ) {
                BasicText("直播", style = TextStyle(Color.White, fontSize = 24.sp))
            }
            BasicText(
                text = source.name,
                style = TextStyle(
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp,
                ),
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun CustomSourceInputDialog(
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(0.5f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(400.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BasicText("添加自定义直播源", style = TextStyle(Color.Black, fontSize = 18.sp))
            BasicText(
                "名称",
                style = TextStyle(Color.Gray, fontSize = 12.sp),
                modifier = Modifier.padding(top = 16.dp).fillMaxWidth(),
            )
            BasicTextField(
                value = name,
                onValueChange = { name = it },
                textStyle = TextStyle(Color.Black, fontSize = 14.sp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .background(Color(0xFFF0F0F0), RoundedCornerShape(8.dp))
                    .padding(12.dp),
            )
            BasicText(
                "直播地址（b23.tv短链或m3u8）",
                style = TextStyle(Color.Gray, fontSize = 12.sp),
                modifier = Modifier.padding(top = 16.dp).fillMaxWidth(),
            )
            BasicTextField(
                value = url,
                onValueChange = { url = it },
                textStyle = TextStyle(Color.Black, fontSize = 14.sp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .background(Color(0xFFF0F0F0), RoundedCornerShape(8.dp))
                    .padding(12.dp),
            )
            Row(
                modifier = Modifier.padding(top = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE0E0E0))
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                        .focusable()
                        .onKeyEvent { e ->
                            if ((e.nativeKeyEvent.keyCode == NativeKeyEvent.KEYCODE_DPAD_CENTER ||
                                        e.nativeKeyEvent.keyCode == NativeKeyEvent.KEYCODE_ENTER) &&
                                e.type == KeyEventType.KeyUp) {
                                onDismiss()
                                true
                            } else false
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText("取消", style = TextStyle(Color.Black, fontSize = 14.sp))
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFCC0000))
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                        .focusable()
                        .onKeyEvent { e ->
                            if ((e.nativeKeyEvent.keyCode == NativeKeyEvent.KEYCODE_DPAD_CENTER ||
                                        e.nativeKeyEvent.keyCode == NativeKeyEvent.KEYCODE_ENTER) &&
                                e.type == KeyEventType.KeyUp) {
                                onConfirm(name, url)
                                true
                            } else false
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText("保存", style = TextStyle(Color.White, fontSize = 14.sp))
                }
            }
        }
    }
}
