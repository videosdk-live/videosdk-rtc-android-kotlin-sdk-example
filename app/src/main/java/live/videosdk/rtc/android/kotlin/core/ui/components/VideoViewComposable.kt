package live.videosdk.rtc.android.kotlin.core.ui.components

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import live.videosdk.rtc.android.VideoView
import org.webrtc.VideoTrack

private const val TAG = "VideoViewComposable"

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
            try {
                videoView.addTrack(videoTrack)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to add track in DisposableEffect: ${e.message}")
            }
        }
        onDispose {
            try {
                videoView.removeTrack()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to remove track in onDispose: ${e.message}")
            }
        }
    }

    AndroidView(
        factory = { videoView },
        modifier = modifier,
        update = { view ->
            try {
                view.setMirror(isMirrored)
                if (videoTrack != null) {
                    view.addTrack(videoTrack)
                } else {
                    view.removeTrack()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to update VideoView: ${e.message}")
            }
        }
    )
}
