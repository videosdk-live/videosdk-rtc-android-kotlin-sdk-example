package live.videosdk.rtc.android.kotlin.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import live.videosdk.rtc.android.VideoView
import org.webrtc.VideoTrack

/**
 * Wrapper for VideoSDK's VideoView in Compose
 * 
 * @param videoTrack The video track to display
 * @param isMirrored Whether to mirror the video (for front camera)
 * @param modifier Composable modifier
 */
@Composable
fun VideoViewComposable(
    videoTrack: VideoTrack?,
    isMirrored: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    val videoView = remember {
        VideoView(context).apply {
            setMirror(isMirrored)
        }
    }

    DisposableEffect(videoTrack) {
        if (videoTrack != null) {
            videoView.addTrack(videoTrack)
        }
        onDispose {
            videoView.removeTrack()
        }
    }

    AndroidView(
        factory = { videoView },
        modifier = modifier,
        update = { view ->
            view.setMirror(isMirrored)
            if (videoTrack != null) {
                view.addTrack(videoTrack)
            } else {
                view.removeTrack()
            }
        }
    )
}
