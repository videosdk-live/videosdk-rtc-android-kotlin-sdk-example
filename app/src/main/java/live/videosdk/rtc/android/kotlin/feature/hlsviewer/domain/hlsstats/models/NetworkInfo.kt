package live.videosdk.rtc.android.hlsstats.models

/**
 * Represents network and bandwidth information
 */
data class NetworkInfo(
    val estimatedBandwidthBps: Long = 0,  // Estimated bandwidth in bits per second
    val totalBytesLoaded: Long = 0,        // Total cumulative bytes downloaded
    val downloadRateBytesPerSec: Long = 0  // Current download rate in bytes per second
)
