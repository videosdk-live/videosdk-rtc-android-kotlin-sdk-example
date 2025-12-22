package live.videosdk.rtc.android.kotlin.feature.hlsviewer.domain.hlsstats

/**
 * Configuration for HLS stats collection
 */
data class HlsStatsConfig(
    /**
     * Rate in milliseconds at which stats updates are delivered to listeners
     * Default: 1000ms (1 seconds)
     */
    val updateIntervalMs: Long = 1000L,
    
    /**
     * Whether to keep full history of events in PlaybackStats
     * Default: false (for memory efficiency)
     * Note: Even when false, key metrics like rebuffer count are still tracked
     */
    val keepHistory: Boolean = false
)
