package live.videosdk.rtc.android.hlsstats.models

/**
 * Represents network and bandwidth information
 */
data class NetworkInfo(
    /**
     * Estimated bandwidth in bits per second
     */
    val estimatedBandwidthBps: Long,
    
    /**
     * Total bytes loaded/transferred
     */
    val totalBytesLoaded: Long
)
