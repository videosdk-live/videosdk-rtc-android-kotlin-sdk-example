package live.videosdk.rtc.android.kotlin.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import live.videosdk.rtc.android.Participant
import live.videosdk.rtc.android.kotlin.core.ui.theme.Black
import live.videosdk.rtc.android.kotlin.core.ui.theme.GreyMd700
import live.videosdk.rtc.android.kotlin.core.ui.theme.White
import org.webrtc.VideoTrack

/**
 * Participant tile showing video or avatar
 * 
 * @param participant The participant to display
 * @param videoTrack The participant's video track (if available)
 * @param isLocal Whether this is the local participant
 * @param hasHandRaised Whether the participant has raised their hand
 * @param modifier Composable modifier
 */
@Composable
fun ParticipantTile(
    participant: Participant?,
    videoTrack: VideoTrack?,
    isLocal: Boolean = false,
    hasHandRaised: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .height(200.dp)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GreyMd700)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(GreyMd700)
        ) {
            if (videoTrack != null) {
                VideoViewComposable(
                    videoTrack = videoTrack,
                    isMirrored = isLocal,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Show participant initial when no video
                Text(
                    text = participant?.displayName?.take(1)?.uppercase() ?: "?",
                    fontSize = 40.sp,
                    color = White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // Hand raise indicator
            if (hasHandRaised) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(Color.Yellow, CircleShape)
                        .size(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PanTool,
                        contentDescription = "Hand raised",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Participant name overlay
            Text(
                text = if (isLocal) "You" else participant?.displayName ?: "Unknown",
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .background(Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                color = White,
                fontSize = 12.sp
            )
        }
    }
}
