package live.videosdk.rtc.android.hlsstats.models

/**
 * Represents video quality information
 */
data class VideoQuality(
    /**
     * Video width in pixels
     */
    val width: Int,
    
    /**
     * Video height in pixels
     */
    val height: Int,
    
    /**
     * Frame rate in frames per second
     */
    val frameRate: Float
)
