package live.videosdk.rtc.android.hlsstats.models

/**
 * Comprehensive HLS playback statistics
 */
data class HlsPlaybackStats(
    // Playback State
    /**
     * Whether playback is currently active
     */
    val isPlaying: Boolean,
    
    /**
     * Whether player is currently buffering
     */
    val isBuffering: Boolean,
    
    /**
     * Current playback state as string (e.g., "playing", "paused", "buffering", "idle", "ended")
     */
    val playbackState: String,
    
    // Timing Information
    /**
     * Current playback position in milliseconds
     */
    val currentPositionMs: Long,
    
    /**
     * Total duration of the content in milliseconds
     */
    val durationMs: Long,
    
    /**
     * Buffered position in milliseconds (how far ahead is buffered)
     */
    val bufferedPositionMs: Long,
    
    // Quality Metrics
    /**
     * Current video resolution and frame rate, null if video not available
     */
    val videoResolution: VideoQuality?,
    
    /**
     * Current bitrate in bits per second
     */
    val bitrate: Long,
    
    /**
     * Network bandwidth information
     */
    val bandwidth: NetworkInfo,
    
    // Performance Metrics
    /**
     * Number of dropped video frames
     */
    val droppedFrames: Int,
    
    /**
     * Total number of video frames rendered
     */
    val totalFramesRendered: Int,
    
    /**
     * Buffer state information
     */
    val bufferInfo: BufferInfo,
    
    // HLS-specific
    /**
     * Whether this is a live stream
     */
    val isLive: Boolean,
    
    /**
     * Live offset in milliseconds (distance from live edge), null if not live
     */
    val liveOffsetMs: Long?,
    
    // Error tracking
    /**
     * List of errors encountered during playback
     */
    val errors: List<PlaybackError>,
    
    // Session metrics
    /**
     * Time taken to join/start playback in milliseconds
     */
    val joinTimeMs: Long,
    
    /**
     * Total time spent rebuffering in milliseconds
     */
    val totalRebufferDurationMs: Long,
    
    /**
     * Number of rebuffer events (excluding initial join)
     */
    val rebufferCount: Int,
    
    // Timestamp
    /**
     * Timestamp when these stats were captured (Unix epoch in milliseconds)
     */
    val timestampMs: Long = System.currentTimeMillis()
)
