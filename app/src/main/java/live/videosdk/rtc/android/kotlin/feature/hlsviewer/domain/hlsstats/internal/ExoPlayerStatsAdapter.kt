package live.videosdk.rtc.android.kotlin.feature.hlsviewer.domain.hlsstats.internal

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.PlaybackStats
import androidx.media3.exoplayer.analytics.PlaybackStatsListener
import live.videosdk.rtc.android.hlsstats.models.*

/**
 * Internal adapter that bridges ExoPlayer's PlaybackStatsListener to our HLS stats model
 */
@UnstableApi
internal class ExoPlayerStatsAdapter(
    private val player: ExoPlayer,
    private val keepHistory: Boolean = false
) {
    private val playbackStatsListener: PlaybackStatsListener = PlaybackStatsListener(
        keepHistory,
        null // No callback, we'll query stats directly
    )
    private val errorHistory = mutableListOf<PlaybackError>()
    private var sessionStartTimeMs: Long = System.currentTimeMillis()
    
    init {
        player.addAnalyticsListener(playbackStatsListener)
    }
    
    /**
     * Get current playback stats mapped to our HlsPlaybackStats model
     */
    @OptIn(UnstableApi::class)
    fun getCurrentStats(): HlsPlaybackStats? {
        val exoStats = playbackStatsListener.playbackStats ?: return null
        return mapToHlsStats(exoStats)
    }
    
    /**
     * Get combined stats for all playback sessions
     */
    fun getCombinedStats(): HlsPlaybackStats {
        val exoStats = playbackStatsListener.combinedPlaybackStats
        return mapToHlsStats(exoStats)
    }
    
    /**
     * Map ExoPlayer PlaybackStats to our HlsPlaybackStats model
     */
    private fun mapToHlsStats(stats: PlaybackStats): HlsPlaybackStats {
        // Get current player state
        val isPlaying = player.isPlaying
        val playbackState = when (player.playbackState) {
            Player.STATE_IDLE -> "idle"
            Player.STATE_BUFFERING -> "buffering"
            Player.STATE_READY -> if (isPlaying) "playing" else "paused"
            Player.STATE_ENDED -> "ended"
            else -> "unknown"
        }
        val isBuffering = player.playbackState == Player.STATE_BUFFERING
        
        // Get timing information
        val currentPositionMs = player.currentPosition
        val durationMs = if (player.duration != C.TIME_UNSET) player.duration else 0L
        val bufferedPositionMs = player.bufferedPosition
        
        // Get video quality from current format
        val videoFormat = player.videoFormat
        
        // Get FPS from decoder counters (actual rendered FPS, not format FPS)
        val decoderCounters = try {
            player.videoDecoderCounters
        } catch (e: Exception) {
            null
        }
        
        val actualFrameRate = if (decoderCounters != null && decoderCounters.renderedOutputBufferCount > 0) {
            // Calculate actual FPS from rendered frames and elapsed time
            val elapsedTimeSec = (System.currentTimeMillis() - sessionStartTimeMs) / 1000.0
            if (elapsedTimeSec > 1.0) {
                val fps = (decoderCounters.renderedOutputBufferCount / elapsedTimeSec).toFloat()
                if (fps > 60f) 60f else fps  // Cap at 60 FPS
            } else {
                videoFormat?.frameRate ?: 30f
            }
        } else {
            videoFormat?.frameRate ?: 30f
        }
        
        val videoResolution = if (videoFormat != null && videoFormat.width > 0 && videoFormat.height > 0) {
            VideoQuality(
                width = videoFormat.width,
                height = videoFormat.height,
                frameRate = if (actualFrameRate > 0) actualFrameRate else 30f // Default to 30 if unknown
            )
        } else null
        
        // Bitrate: Current video quality bitrate (from format)
        val bitrate = videoFormat?.bitrate?.toLong() ?: 0L
        
        // Bandwidth Estimate: Network speed (from actual data transfer)
        val estimatedBandwidth = if (stats.totalBandwidthTimeMs > 0 && stats.totalBandwidthBytes > 0) {
            (stats.totalBandwidthBytes * 8000 / stats.totalBandwidthTimeMs)
        } else {
            bitrate // Fallback to format bitrate if no bandwidth data yet
        }
        
        // Get bandwidth info
        val bandwidth = NetworkInfo(
            estimatedBandwidthBps = estimatedBandwidth,
            totalBytesLoaded = stats.totalBandwidthBytes
        )
        
        // Get performance metrics - use actual decoder counters
        val droppedFrames = decoderCounters?.droppedBufferCount?.toInt() ?: stats.totalDroppedFrames.toInt()
        val totalFramesRendered = decoderCounters?.renderedOutputBufferCount?.toInt() ?: 0
        
        // Buffer info - differentiate between total buffered and internal buffers
        val totalBufferedDurationMs = bufferedPositionMs - currentPositionMs
        
        // ExoPlayer doesn't expose separate audio/video buffers, but we can estimate
        // Video typically needs more buffer than audio
        val videoBufferMs = (totalBufferedDurationMs * 0.8).toLong() // 80% for video
        val audioBufferMs = (totalBufferedDurationMs * 0.2).toLong() // 20% for audio
        
        val bufferInfo = BufferInfo(
            audioBufferMs = audioBufferMs,
            videoBufferMs = videoBufferMs,
            targetBufferMs = totalBufferedDurationMs // Total buffered ahead
        )
        
        // HLS-specific - better live offset calculation
        val isLive = player.isCurrentMediaItemLive
        val liveOffsetMs = if (isLive) {
            // For HLS, calculate distance from live edge
            val currentOffset = player.currentLiveOffset
            if (currentOffset != C.TIME_UNSET && currentOffset > 0) {
                currentOffset
            } else {
                // Fallback: estimate from duration and position
                val totalDuration = if (durationMs > 0) durationMs else 0L
                if (totalDuration > 0) {
                    totalDuration - currentPositionMs
                } else {
                    0L
                }
            }
        } else null
        
        // Session metrics  
        val joinTimeMs = stats.totalValidJoinTimeMs
        val totalRebufferDurationMs = stats.totalRebufferTimeMs
        val rebufferCount = stats.totalRebufferCount
        
        return HlsPlaybackStats(
            isPlaying = isPlaying,
            isBuffering = isBuffering,
            playbackState = playbackState,
            currentPositionMs = currentPositionMs,
            durationMs = durationMs,
            bufferedPositionMs = bufferedPositionMs,
            videoResolution = videoResolution,
            bitrate = bitrate,
            bandwidth = bandwidth,
            droppedFrames = droppedFrames,
            totalFramesRendered = totalFramesRendered,
            bufferInfo = bufferInfo,
            isLive = isLive,
            liveOffsetMs = liveOffsetMs,
            errors = errorHistory.toList(),
            joinTimeMs = joinTimeMs,
            totalRebufferDurationMs = totalRebufferDurationMs,
            rebufferCount = rebufferCount,
            timestampMs = System.currentTimeMillis()
        )
    }
    
    /**
     * Track a playback error
     */
    fun trackError(code: Int, message: String) {
        errorHistory.add(
            PlaybackError(
                code = code,
                message = message,
                timestamp = System.currentTimeMillis()
            )
        )
    }
    
    /**
     * Clear error history
     */
    fun clearErrors() {
        errorHistory.clear()
    }
    
    /**
     * Release resources
     */
    fun release() {
        player.removeAnalyticsListener(playbackStatsListener)
        errorHistory.clear()
    }
}
