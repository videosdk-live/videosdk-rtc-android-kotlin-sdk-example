package live.videosdk.rtc.android.kotlin.feature.createjoin.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.VideocamOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import live.videosdk.rtc.android.kotlin.core.ui.components.ControlButton
import live.videosdk.rtc.android.kotlin.core.ui.components.VideoViewComposable
import live.videosdk.rtc.android.kotlin.core.ui.theme.Black
import live.videosdk.rtc.android.kotlin.core.ui.theme.GreyMd700
import live.videosdk.rtc.android.kotlin.core.ui.theme.RedMd400
import live.videosdk.rtc.android.kotlin.core.ui.theme.White
import org.webrtc.VideoTrack

/**
 * Camera preview card with mic/webcam controls
 */
@Composable
fun CameraPreviewCard(
    videoTrack: VideoTrack?,
    micEnabled: Boolean,
    webcamEnabled: Boolean,
    onToggleMic: () -> Unit,
    onToggleWebcam: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = GreyMd700)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .size(width = 250.dp, height = 340.dp)
        ) {
            // Video or placeholder
            if (webcamEnabled && videoTrack != null) {
                VideoViewComposable(
                    videoTrack = videoTrack,
                    isMirrored = true,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(GreyMd700),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Camera is turned off",
                        color = White
                    )
                }
            }

            // Control buttons
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ControlButton(
                    icon = if (micEnabled) Icons.Rounded.Mic else Icons.Rounded.MicOff,
                    onClick = onToggleMic,
                    backgroundColor = if (micEnabled) White else RedMd400,
                    iconColor = if (micEnabled) Black else White
                )

                ControlButton(
                    icon = if (webcamEnabled) Icons.Rounded.Videocam else Icons.Rounded.VideocamOff,
                    onClick = onToggleWebcam,
                    backgroundColor = if (webcamEnabled) White else RedMd400,
                    iconColor = if (webcamEnabled) Black else White
                )
            }
        }
    }
}
