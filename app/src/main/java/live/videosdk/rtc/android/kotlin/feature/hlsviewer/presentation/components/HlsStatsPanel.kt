package live.videosdk.rtc.android.kotlin.feature.hlsviewer.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import live.videosdk.rtc.android.kotlin.core.ui.theme.*
import live.videosdk.rtc.android.kotlin.feature.hlsviewer.presentation.HlsStatsUiState

/**
 * Panel displaying HLS playback statistics
 */
@Composable
fun HlsStatsPanel(
    stats: HlsStatsUiState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = GreyMd700.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Text(
                text = "HLS Playback Statistics",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = White,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Divider(color = White.copy(alpha = 0.2f), modifier = Modifier.padding(bottom = 12.dp))

            // Stats Grid
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Bitrate
                StatItem(
                    icon = Icons.Default.Speed,
                    label = "Bitrate",
                    value = formatBitrate(stats.bitrate),
                    iconTint = ColorAccent
                )

                // Buffering Status
                StatItem(
                    icon = if (stats.isBuffering) Icons.Default.PlayCircle else Icons.Default.CheckCircle,
                    label = "Buffering",
                    value = if (stats.isBuffering) "Yes" else "No",
                    iconTint = if (stats.isBuffering) ColorYellow else ColorGreen,
                    valueColor = if (stats.isBuffering) ColorYellow else ColorGreen
                )

                // Rebuffer Count
                StatItem(
                    icon = Icons.Default.Refresh,
                    label = "Rebuffers",
                    value = stats.rebufferCount.toString(),
                    iconTint = if (stats.rebufferCount > 0) ColorOrange else ColorGreen
                )

                // Playing Status
                StatItem(
                    icon = if (stats.isPlaying) Icons.Default.PlayArrow else Icons.Default.Pause,
                    label = "Playing",
                    value = if (stats.isPlaying) "Yes" else "No",
                    iconTint = if (stats.isPlaying) ColorGreen else White.copy(alpha = 0.7f)
                )
            }

            // Session ended indicator
            if (stats.sessionEnded) {
                Divider(
                    color = White.copy(alpha = 0.2f),
                    modifier = Modifier.padding(vertical = 12.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ColorPrimaryVariant, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Session Ended - Total Rebuffers: ${stats.totalRebuffers}",
                        color = White,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: ImageVector,
    label: String,
    value: String,
    iconTint: Color,
    valueColor: Color = White,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                color = White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
        }
        
        Text(
            text = value,
            color = valueColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun formatBitrate(bitrate: Long): String {
    return when {
        bitrate == 0L -> "0 bps"
        bitrate < 1000 -> "$bitrate bps"
        bitrate < 1_000_000 -> "${bitrate / 1000} kbps"
        else -> String.format("%.2f Mbps", bitrate / 1_000_000.0)
    }
}

// Additional colors for stats
private val ColorGreen = Color(0xFF4CAF50)
private val ColorYellow = Color(0xFFFFC107)
private val ColorOrange = Color(0xFFFF9800)
