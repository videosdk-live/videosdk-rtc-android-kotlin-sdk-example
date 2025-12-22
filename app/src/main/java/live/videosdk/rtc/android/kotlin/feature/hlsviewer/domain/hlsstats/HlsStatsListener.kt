package live.videosdk.rtc.android.kotlin.feature.hlsviewer.domain.hlsstats

import live.videosdk.rtc.android.hlsstats.models.HlsPlaybackStats

/**
 * Listener interface for receiving HLS playback statistics updates
 */
interface HlsStatsListener {
    /**
     * Called periodically with updated playback statistics
     * @param stats Current playback statistics
     */
    fun onStatsUpdate(stats: HlsPlaybackStats)
    
    /**
     * Called when playback session ends
     * @param finalStats Final statistics for the completed session, null if session was never active
     */
    fun onSessionEnded(finalStats: HlsPlaybackStats?)
}
