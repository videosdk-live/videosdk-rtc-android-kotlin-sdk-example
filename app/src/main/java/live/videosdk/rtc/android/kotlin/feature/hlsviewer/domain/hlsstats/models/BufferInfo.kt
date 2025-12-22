package live.videosdk.rtc.android.hlsstats.models

/**
 * Represents buffer state information
 */
data class BufferInfo(
    /**
     * Audio buffer duration in milliseconds
     */
    val audioBufferMs: Long,
    
    /**
     * Video buffer duration in milliseconds
     */
    val videoBufferMs: Long,
    
    /**
     * Target buffer duration in milliseconds
     */
    val targetBufferMs: Long
)
