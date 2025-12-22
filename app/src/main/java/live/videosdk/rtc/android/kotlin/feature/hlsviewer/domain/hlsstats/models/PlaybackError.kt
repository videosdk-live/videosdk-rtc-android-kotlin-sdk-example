package live.videosdk.rtc.android.hlsstats.models

/**
 * Represents a playback error
 */
data class PlaybackError(
    /**
     * Error code
     */
    val code: Int,
    
    /**
     * Error message
     */
    val message: String,
    
    /**
     * Timestamp when error occurred (Unix epoch in milliseconds)
     */
    val timestamp: Long
)
