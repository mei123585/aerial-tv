package app.orchard.tv.ui.home

import android.net.Uri
import android.view.TextureView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import app.orchard.tv.LiveSource
import app.orchard.tv.system.DiagLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

@Composable
fun ChannelFocusPreview(
    visible: Boolean,
    source: LiveSource?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var player by remember { mutableStateOf<ExoPlayer?>(null) }
    var surface by remember { mutableStateOf<TextureView?>(null) }

    // 切换源时释放旧播放器并创建新的
    LaunchedEffect(source?.url) {
        // 先释放旧播放器（在主线程，避免竞态）
        player?.release()
        player = null
        // 清除旧画面，避免残留静态帧
        surface?.let { player?.clearVideoTextureView(it) }

        if (source == null) return@LaunchedEffect

        val streamUrl = withContext(Dispatchers.IO) { resolveStreamUrl(source) }
        DiagLog.i("channel preview: url = $streamUrl")
        if (streamUrl != null) {
            val exo = ExoPlayer.Builder(context).build()
            player = exo
            exo.playWhenReady = visible  // 创建时就根据visible决定是否播放
            exo.volume = if (visible) 1f else 0f
            val factory = DefaultHttpDataSource.Factory()
                .setUserAgent("Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36")
                .setConnectTimeoutMs(10_000).setReadTimeoutMs(10_000)
            if (source.isBilibili || streamUrl.contains("bilibili.com"))
                factory.setDefaultRequestProperties(mapOf("Referer" to "https://live.bilibili.com/"))
            val hls = HlsMediaSource.Factory(factory).createMediaSource(MediaItem.fromUri(Uri.parse(streamUrl)))
            exo.setMediaSource(hls)
            exo.addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) { DiagLog.i("player err: ${error.errorCodeName}") }
            })
            // 如果surface已经存在就绑定，否则等AndroidView创建后再绑定
            surface?.let { exo.setVideoTextureView(it) }
            exo.prepare()
        }
    }

    // visible变化时控制播放/暂停，并清除画面
    LaunchedEffect(visible) {
        if (visible) {
            surface?.let { player?.setVideoTextureView(it) }
            player?.playWhenReady = true
            player?.volume = 1f
        } else {
            // 不可见时清除画面，避免残留静态帧
            surface?.let { player?.clearVideoTextureView(it) }
            player?.playWhenReady = false
            player?.volume = 0f
        }
    }

    DisposableEffect(Unit) {
        val obs = LifecycleEventObserver { _, e ->
            when (e) {
                Lifecycle.Event.ON_STOP -> { player?.pause(); player?.volume = 0f }
                Lifecycle.Event.ON_START -> { player?.playWhenReady = true; player?.volume = 1f }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(obs)
            val p = player; player = null
            p?.release()
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx -> TextureView(ctx).also {
            it.isFocusable = false; it.isFocusableInTouchMode = false
            surface = it
            // 如果播放器已经存在，绑定surface
            player?.setVideoTextureView(it)
        }},
        onRelease = { player?.clearVideoTextureView(it); surface = null },
    )
}

private suspend fun resolveStreamUrl(source: LiveSource): String? = withContext(Dispatchers.IO) {
    try {
        when {
            source.url.endsWith(".m3u8") -> source.url
            source.isBilibili || source.url.contains("b23.tv") || source.url.contains("live.bilibili.com") ->
                extractBilibiliRoomId(source.url)?.let { fetchBilibiliHlsUrl(it) }
            else -> source.url
        }
    } catch (e: Exception) { null }
}

private fun extractBilibiliRoomId(url: String): Long? = runCatching {
    if (url.contains("b23.tv")) {
        val c = java.net.URL(url).openConnection() as java.net.HttpURLConnection
        c.instanceFollowRedirects = false; c.connectTimeout = 8000; c.readTimeout = 8000
        val loc = c.getHeaderField("Location"); c.disconnect()
        loc?.let { extractRoomIdFromUrl(it) }
    } else extractRoomIdFromUrl(url)
}.getOrNull()

private fun extractRoomIdFromUrl(url: String): Long? =
    Regex("live\\.bilibili\\.com/(\\d+)").find(url)?.groupValues?.getOrNull(1)?.toLongOrNull()

private fun fetchBilibiliHlsUrl(roomId: Long): String? = runCatching {
    val api = "https://api.live.bilibili.com/xlive/web-room/v2/index/getRoomPlayInfo?room_id=$roomId&protocol=0,1&format=0,1,2&codec=0&qn=250&platform=web"
    val c = java.net.URL(api).openConnection() as java.net.HttpURLConnection
    c.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36")
    c.setRequestProperty("Referer", "https://live.bilibili.com/")
    c.connectTimeout = 10000; c.readTimeout = 10000
    val body = c.inputStream.bufferedReader().readText(); c.disconnect()
    val streams = JSONObject(body).getJSONObject("data").getJSONObject("playurl_info").getJSONObject("playurl").getJSONArray("stream")
    for (i in 0 until streams.length()) {
        val s = streams.getJSONObject(i); if (s.optString("protocol_name") != "http_hls") continue
        val fmts = s.getJSONArray("format")
        for (j in 0 until fmts.length()) {
            val f = fmts.getJSONObject(j); if (f.optString("format_name") != "ts") continue
            val codecs = f.getJSONArray("codec")
            for (k in 0 until codecs.length()) {
                val codec = codecs.getJSONObject(k)
                val ui = codec.getJSONArray("url_info").getJSONObject(0)
                return ui.getString("host") + codec.getString("base_url") + ui.getString("extra")
            }
        }
    }; null
}.getOrNull()
