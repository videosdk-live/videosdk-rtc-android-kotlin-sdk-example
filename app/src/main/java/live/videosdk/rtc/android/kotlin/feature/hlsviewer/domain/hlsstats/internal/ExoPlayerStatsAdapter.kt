package live.videosdk.rtc.android.kotlin.feature.hlsviewer.domain.hlsstats.internal

import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.PlaybackStats
import androidx.media3.exoplayer.analytics.PlaybackStatsListener
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import live.videosdk.rtc.android.hlsstats.models.*

/**
 * Optimized adapter for real-time HLS statistics.
 * Focuses on sliding-window FPS and smoothed bandwidth estimation.
 */
@UnstableApi
internal class ExoPlayerStatsAdapter(
    private val player: ExoPlayer,
    private val context: android.content.Context,
    private val keepHistory: Boolean = false
) {
    private val TAG = "ExoPlayerStatsAdapter"

    // ExoPlayer's internal meter for smoothed bitrate estimation
    private val bandwidthMeter = DefaultBandwidthMeter.getSingletonInstance(context)

    private val playbackStatsListener: PlaybackStatsListener = PlaybackStatsListener(
        keepHistory,
        null
    )

    private val errorHistory = mutableListOf<PlaybackError>()

    // FPS Tracking (Differential logic)
    private var lastRenderedFrames: Int = 0
    private var lastFrameCheckTimeMs: Long = System.currentTimeMillis()
    private var lastCalculatedFps: Float = 0f

    // Bandwidth Tracking
    private var lastBandwidthBytes: Long = 0
    private var lastCheckTimeMs: Long = System.currentTimeMillis()
    private var smoothedManualBitrate: Long = 0
    private val SMOOTHING_FACTOR = 0.3 // Weight for new samples

    init {
        player.addAnalyticsListener(playbackStatsListener)
        Log.d(TAG, "Stats Adapter initialized for player: ${player.hashCode()}")
    }

    @OptIn(UnstableApi::class)
    fun getCurrentStats(): HlsPlaybackStats? {
        val exoStats = playbackStatsListener.playbackStats ?: return null
        return mapToHlsStats(exoStats)
    }

    fun getCombinedStats(): HlsPlaybackStats {
        val exoStats = playbackStatsListener.combinedPlaybackStats
        return mapToHlsStats(exoStats)
    }

    private fun mapToHlsStats(stats: PlaybackStats): HlsPlaybackStats {
        val currentTime = System.currentTimeMillis()

        // 1. Playback State Logic
        val isPlaying = player.isPlaying
        val playbackState = when (player.playbackState) {
            Player.STATE_IDLE -> "idle"
            Player.STATE_BUFFERING -> "buffering"
            Player.STATE_READY -> if (isPlaying) "playing" else "paused"
            Player.STATE_ENDED -> "ended"
            else -> "unknown"
        }

        // 2. Real-time FPS Calculation (Sliding Window)
        val decoderCounters = try { player.videoDecoderCounters } catch (e: Exception) { null }
        val currentRendered = decoderCounters?.renderedOutputBufferCount ?: 0
        val frameDeltaTimeSec = (currentTime - lastFrameCheckTimeMs) / 1000.0

        if (frameDeltaTimeSec >= 0.8) { // Update FPS roughly every second
            val framesInInterval = currentRendered - lastRenderedFrames
            lastCalculatedFps = (framesInInterval / frameDeltaTimeSec).toFloat().coerceIn(0f, 60f)

            // Log frame performance if dropping significantly
            val formatFps = player.videoFormat?.frameRate ?: 30f
            if (lastCalculatedFps < (formatFps * 0.8) && isPlaying) {
                Log.w(TAG, "Low FPS detected: $lastCalculatedFps (Target: $formatFps)")
            }

            lastRenderedFrames = currentRendered
            lastFrameCheckTimeMs = currentTime
        }

        val videoFormat = player.videoFormat
        val videoResolution = if (videoFormat != null && videoFormat.width > 0) {
            VideoQuality(
                width = videoFormat.width,
                height = videoFormat.height,
                frameRate = if (lastCalculatedFps > 0) lastCalculatedFps else videoFormat.frameRate
            )
        } else null

        // 3. Bandwidth Estimation
        // Primary: Use ExoPlayer's built-in BandwidthMeter (Weighted moving average)
        var estimatedBandwidth = bandwidthMeter.bitrateEstimate
        
        // Secondary Fallback: Manual smoothing if Meter isn't ready
        val byteDelta = stats.totalBandwidthBytes - lastBandwidthBytes
        val timeDeltaMs = currentTime - lastCheckTimeMs
        
        // Calculate current download rate (bytes per second)
        // Maintain last value to avoid 0 flickering between HLS chunk downloads
        var currentDownloadRate: Long = smoothedManualBitrate / 8  // Start with last known rate

        if (timeDeltaMs >= 1000) {
            if (byteDelta > 0) {
                // Calculate instantaneous download rate
                currentDownloadRate = (byteDelta * 1000 / timeDeltaMs) // bytes per second
                
                // Also update smoothed bitrate for bandwidth estimate fallback
                val instantBps = (byteDelta * 8000 / timeDeltaMs)
                smoothedManualBitrate = if (smoothedManualBitrate == 0L) instantBps
                else ((instantBps * SMOOTHING_FACTOR) + (smoothedManualBitrate * (1 - SMOOTHING_FACTOR))).toLong()
            }
            // Always update tracking values even if no new data
            lastBandwidthBytes = stats.totalBandwidthBytes
            lastCheckTimeMs = currentTime
        }

        // If BandwidthMeter hasn't collected enough samples, use manual smoothed bitrate
        if (estimatedBandwidth == 0L || estimatedBandwidth < 100) {
            estimatedBandwidth = if (smoothedManualBitrate > 0) smoothedManualBitrate else videoFormat?.bitrate?.toLong() ?: 0L
        }

        val bandwidth = NetworkInfo(
            estimatedBandwidthBps = estimatedBandwidth,
            totalBytesLoaded = stats.totalBandwidthBytes,
            downloadRateBytesPerSec = currentDownloadRate  // Maintains last valid rate
        )

        // 4. Buffer & Performance
        val currentPositionMs = player.currentPosition
        val bufferedPositionMs = player.bufferedPosition
        val totalBufferedDurationMs = (bufferedPositionMs - currentPositionMs).coerceAtLeast(0)

        val bufferInfo = BufferInfo(
            audioBufferMs = (totalBufferedDurationMs * 0.5).toLong(), // More realistic 50/50 split for HLS TS
            videoBufferMs = (totalBufferedDurationMs * 0.5).toLong(),
            targetBufferMs = totalBufferedDurationMs
        )

        val droppedFrames = decoderCounters?.droppedBufferCount ?: stats.totalDroppedFrames.toInt()

        // 5. Live Offset
        val isLive = player.isCurrentMediaItemLive
        val liveOffsetMs = if (isLive) {
            val offset = player.currentLiveOffset
            if (offset != C.TIME_UNSET) offset else (player.duration - currentPositionMs).coerceAtLeast(0)
        } else null

        return HlsPlaybackStats(
            isPlaying = isPlaying,
            isBuffering = (player.playbackState == Player.STATE_BUFFERING),
            playbackState = playbackState,
            currentPositionMs = currentPositionMs,
            durationMs = if (player.duration != C.TIME_UNSET) player.duration else 0L,
            bufferedPositionMs = bufferedPositionMs,
            videoResolution = videoResolution,
            bitrate = videoFormat?.bitrate?.toLong() ?: 0L,
            bandwidth = bandwidth,
            droppedFrames = droppedFrames,
            totalFramesRendered = currentRendered,
            bufferInfo = bufferInfo,
            isLive = isLive,
            liveOffsetMs = liveOffsetMs,
            errors = errorHistory.toList(),
            joinTimeMs = stats.totalValidJoinTimeMs,
            totalRebufferDurationMs = stats.totalRebufferTimeMs,
            rebufferCount = stats.totalRebufferCount,
            timestampMs = currentTime
        ).also {
            // Optional: verbose logging for debugging
            // Log.v(TAG, "Stats: FPS=${it.videoResolution?.frameRate}, BW=${it.bandwidth.estimatedBandwidthBps/1000}kbps")
        }
    }

    fun trackError(code: Int, message: String) {
        Log.e(TAG, "Tracking Error: $code - $message")
        errorHistory.add(PlaybackError(code, message, System.currentTimeMillis()))
    }

    fun clearErrors() { errorHistory.clear() }

    fun release() {
        Log.d(TAG, "Releasing Stats Adapter")
        player.removeAnalyticsListener(playbackStatsListener)
        errorHistory.clear()
    }
}