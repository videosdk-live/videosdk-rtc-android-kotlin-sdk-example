package live.videosdk.rtc.android.kotlin.feature.hlsviewer.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import live.videosdk.rtc.android.kotlin.feature.hlsviewer.presentation.HlsStatsUiState
import kotlin.math.round

/**
 * Comprehensive HLS Stats Panel displaying all 100ms metrics
 */
@Composable
fun ComprehensiveHlsStatsPanel(
    stats: HlsStatsUiState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 600.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A1A)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Text(
                text = "HLS Stream Analytics",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Divider(color = Color.Gray.copy(alpha = 0.3f))
            
            // Network Section
            StatsSection(title = "📶 Network") {
                StatRow(
                    label = "Bandwidth Estimate",
                    value = formatBandwidth(stats.estimatedBandwidth),
                    description = "Current network speed",
                    color = getBandwidthColor(stats.estimatedBandwidth)
                )
                StatRow(
                    label = "Bitrate",
                    value = formatBitrate(stats.bitrate),
                    description = "Current layer bitrate"
                )
                StatRow(
                    label = "Bytes Downloaded",
                    value = formatBytes(stats.totalBytesLoaded),
                    description = "Total data transferred"
                )
            }
            
            // Quality Section
            StatsSection(title = "🎬 Video Quality") {
                StatRow(
                    label = "Resolution",
                    value = "${stats.videoWidth} x ${stats.videoHeight}",
                    description = "Current video resolution",
                    color = getResolutionColor(stats.videoHeight)
                )
                StatRow(
                    label = "Frame Rate",
                    value = "${round(stats.frameRate * 10) / 10} fps",
                    description = "Frames per second"
                )
            }
            
            // Performance Section
            StatsSection(title = "⚡ Performance") {
                StatRow(
                    label = "Dropped Frames",
                    value = stats.droppedFrames.toString(),
                    description = "Frames dropped since start",
                    color = getDroppedFramesColor(stats.droppedFrames, stats.totalFramesRendered)
                )
                StatRow(
                    label = "Total Frames",
                    value = stats.totalFramesRendered.toString(),
                    description = "Frames rendered"
                )
                StatRow(
                    label = "Buffered Duration",
                    value = formatDuration(stats.bufferedPositionMs - stats.currentPositionMs),
                    description = "Data buffered ahead",
                    color = getBufferColor(stats.bufferedPositionMs - stats.currentPositionMs)
                )
            }
            
            // Buffer Health Section
            StatsSection(title = "📊 Buffer Health") {
                StatRow(
                    label = "Video Buffer",
                    value = formatDuration(stats.videoBufferMs),
                    description = "Video buffer duration"
                )
                StatRow(
                    label = "Audio Buffer",
                    value = formatDuration(stats.audioBufferMs),
                    description = "Audio buffer duration"
                )
                StatRow(
                    label = "Target Buffer",
                    value = formatDuration(stats.targetBufferMs),
                    description = "Target buffer size"
                )
            }
            
            // HLS Specific Section
            if (stats.isLive) {
                StatsSection(title = "🔴 Live Stream") {
                    StatRow(
                        label = "Distance from Live Edge",
                        value = formatDuration(stats.liveOffsetMs ?: 0),
                        description = "Latency behind live",
                        color = getLiveOffsetColor(stats.liveOffsetMs ?: 0)
                    )
                }
            }
            
            // Session Metrics Section
            StatsSection(title = "📈 Session Metrics") {
                StatRow(
                    label = "Join Time",
                    value = formatDuration(stats.joinTimeMs),
                    description = "Time to first frame"
                )
                StatRow(
                    label = "Rebuffers",
                    value = stats.rebufferCount.toString(),
                    description = "Number of rebuffer events",
                    color = getRebufferColor(stats.rebufferCount)
                )
                StatRow(
                    label = "Total Rebuffer Duration",
                    value = formatDuration(stats.totalRebufferDurationMs),
                    description = "Time spent buffering"
                )
            }
            
            // Playback State
            StatsSection(title = "▶️ Playback State") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StateChip(
                        label = if (stats.isPlaying) "Playing" else "Paused",
                        color = if (stats.isPlaying) Color.Green else Color.Gray
                    )
                    StateChip(
                        label = if (stats.isBuffering) "Buffering" else "Loaded",
                        color = if (stats.isBuffering) Color.Yellow else Color.Green
                    )
                    if (stats.errorCount > 0) {
                        StateChip(
                            label = "${stats.errorCount} Errors",
                            color = Color.Red
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.9f)
        )
        content()
    }
}

@Composable
private fun StatRow(
    label: String,
    value: String,
    description: String,
    color: Color = Color.White
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
            Text(
                text = description,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun StateChip(label: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontSize = 12.sp,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

// Helper Functions
private fun formatBandwidth(bps: Long): String {
    return when {
        bps >= 1_000_000 -> String.format("%.1f Mbps", bps / 1_000_000.0)
        bps >= 1_000 -> String.format("%.0f Kbps", bps / 1_000.0)
        else -> "$bps bps"
    }
}

private fun formatBitrate(bps: Long): String = formatBandwidth(bps)

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1_073_741_824 -> String.format("%.2f GB", bytes / 1_073_741_824.0)
        bytes >= 1_048_576 -> String.format("%.1f MB", bytes / 1_048_576.0)
        bytes >= 1_024 -> String.format("%.0f KB", bytes / 1_024.0)
        else -> "$bytes B"
    }
}

private fun formatDuration(ms: Long): String {
    val seconds = ms / 1000.0
    return when {
        seconds >= 60 -> String.format("%.1f min", seconds / 60)
        else -> String.format("%.1f s", seconds)
    }
}

// Color Helpers
private fun getBandwidthColor(bps: Long): Color {
    return when {
        bps >= 5_000_000 -> Color.Green  // >5 Mbps - Excellent
        bps >= 2_000_000 -> Color.Yellow // 2-5 Mbps - Good
        else -> Color.Red                // <2 Mbps - Poor
    }
}

private fun getResolutionColor(height: Int): Color {
    return when {
        height >= 1080 -> Color.Green    // 1080p+ - HD
        height >= 720 -> Color.Yellow    // 720p - HD Ready
        else -> Color(0xFFFFA500)        // <720p - SD
    }
}

private fun getDroppedFramesColor(dropped: Int, total: Int): Color {
    if (total == 0) return Color.White
    val dropRate = dropped.toFloat() / total
    return when {
        dropRate < 0.01 -> Color.Green   // <1% - Excellent
        dropRate < 0.05 -> Color.Yellow  // 1-5% - Acceptable
        else -> Color.Red                // >5% - Poor
    }
}

private fun getBufferColor(ms: Long): Color {
    return when {
        ms >= 10_000 -> Color.Green      // >10s - Excellent
        ms >= 5_000 -> Color.Yellow      // 5-10s - Good
        else -> Color.Red                // <5s - Poor
    }
}

private fun getLiveOffsetColor(ms: Long): Color {
    return when {
        ms < 5_000 -> Color.Green        // <5s - Near live
        ms < 15_000 -> Color.Yellow      // 5-15s - Acceptable
        else -> Color.Red                // >15s - High latency
    }
}

private fun getRebufferColor(count: Int): Color {
    return when {
        count == 0 -> Color.Green        // 0 - Perfect
        count <= 2 -> Color.Yellow       // 1-2 - Acceptable
        else -> Color.Red                // >2 - Poor
    }
}
