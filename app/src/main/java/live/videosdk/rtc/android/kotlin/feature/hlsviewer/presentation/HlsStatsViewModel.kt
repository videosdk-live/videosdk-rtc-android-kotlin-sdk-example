package live.videosdk.rtc.android.kotlin.feature.hlsviewer.presentation

import android.content.Context
import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import live.videosdk.rtc.android.Meeting
import live.videosdk.rtc.android.VideoSDK
import live.videosdk.rtc.android.hlsstats.models.HlsPlaybackStats
import live.videosdk.rtc.android.kotlin.feature.hlsviewer.domain.hlsstats.HlsStatsCollector
import live.videosdk.rtc.android.kotlin.feature.hlsviewer.domain.hlsstats.HlsStatsListener
import live.videosdk.rtc.android.listeners.MeetingEventListener
import live.hms.stats.PlayerEventsCollector
import live.hms.stats.PlayerStatsListener
import live.hms.stats.model.PlayerStatsModel
import live.hms.stats.model.InitConfig


/**
 * ViewModel for HLS Viewer with stats collection and meeting integration
 * Supports both Host mode (start/stop HLS) and Viewer mode (watch HLS with stats)
 */
@UnstableApi
class HlsStatsViewModel : ViewModel() {

    // Custom implementation stats
    private val _customUiState = MutableStateFlow(HlsStatsUiState())
    val customUiState: StateFlow<HlsStatsUiState> = _customUiState.asStateFlow()
    
    // 100ms implementation stats
    private val _hmsUiState = MutableStateFlow(HlsStatsUiState())
    val hmsUiState: StateFlow<HlsStatsUiState> = _hmsUiState.asStateFlow()
    
    // Legacy single state (uses custom)
    val uiState: StateFlow<HlsStatsUiState> = _customUiState.asStateFlow()

    private var exoPlayer: ExoPlayer? = null
    private var customStatsCollector: HlsStatsCollector? = null
    private var hmsPlayerEventsCollector: PlayerEventsCollector? = null
    private var meeting: Meeting? = null

    @OptIn(UnstableApi::class)
    fun initializePlayer(player: ExoPlayer) {
        exoPlayer = player
        
        // Create CUSTOM stats collector
        customStatsCollector = HlsStatsCollector(player)
        
        // Create 100ms PlayerEventsCollector with null HMSSDK (standalone stats)
        hmsPlayerEventsCollector = PlayerEventsCollector(null, InitConfig(eventRate = 1000))
        hmsPlayerEventsCollector?.setExoPlayer(player)
        
        // Add CUSTOM stats listener - maps all comprehensive stats
        customStatsCollector?.addListener(object : HlsStatsListener {
            override fun onStatsUpdate(stats: HlsPlaybackStats) {
                viewModelScope.launch {
                    _customUiState.update { currentState ->
                        currentState.copy(
                            // Playback State
                            isPlaying = stats.isPlaying,
                            isBuffering = stats.isBuffering,
                            playbackState = stats.playbackState,
                            
                            // Timing Information
                            currentPositionMs = stats.currentPositionMs,
                            durationMs = stats.durationMs,
                            bufferedPositionMs = stats.bufferedPositionMs,
                            
                            // Quality Metrics
                            videoWidth = stats.videoResolution?.width ?: 0,
                            videoHeight = stats.videoResolution?.height ?: 0,
                            frameRate = stats.videoResolution?.frameRate ?: 0f,
                            bitrate = stats.bitrate,
                            estimatedBandwidth = stats.bandwidth.estimatedBandwidthBps,
                            totalBytesLoaded = stats.bandwidth.totalBytesLoaded,
                            
                            // Performance Metrics
                            droppedFrames = stats.droppedFrames,
                            totalFramesRendered = stats.totalFramesRendered,
                            audioBufferMs = stats.bufferInfo.audioBufferMs,
                            videoBufferMs = stats.bufferInfo.videoBufferMs,
                            targetBufferMs = stats.bufferInfo.targetBufferMs,
                            
                            // HLS-specific
                            isLive = stats.isLive,
                            liveOffsetMs = stats.liveOffsetMs,
                            
                            // Session metrics
                            joinTimeMs = stats.joinTimeMs,
                            totalRebufferDurationMs = stats.totalRebufferDurationMs,
                            rebufferCount = stats.rebufferCount,
                            
                            // Error tracking
                            errorCount = stats.errors.size
                        )
                    }
                }
            }

            override fun onSessionEnded(finalStats: HlsPlaybackStats?) {
                viewModelScope.launch {
                    finalStats?.let { stats ->
                        _customUiState.update {
                            it.copy(
                                sessionEnded = true,
                                totalRebuffers = stats.rebufferCount
                            )
                        }
                    }
                }
            }
        })
        
        
        // Add 100ms PlayerStatsListener
        hmsPlayerEventsCollector?.addStatsListener(object : PlayerStatsListener {
            override fun onEventUpdate(playerStats: PlayerStatsModel) {
                viewModelScope.launch {
                    _hmsUiState.update { currentState ->
                        currentState.copy(
                            // 100ms only provides: bandwidth, videoInfo, frameInfo, bufferedDuration, distanceFromLive
                            videoWidth = playerStats.videoInfo?.videoWidth ?: 0,
                            videoHeight = playerStats.videoInfo?.videoHeight ?: 0,
                            frameRate = playerStats.videoInfo?.frameRate ?: 0f,
                            bitrate = playerStats.videoInfo?.averageBitrate?.toLong() ?: 0L,
                            estimatedBandwidth = playerStats.bandwidth?.bandWidthEstimate ?: 0L,
                            totalBytesLoaded = playerStats.bandwidth?.totalBytesLoaded ?: 0L,
                            droppedFrames = playerStats.frameInfo?.droppedFrameCount ?: 0,
                            totalFramesRendered = playerStats.frameInfo?.totalFrameCount ?: 0,
                            videoBufferMs = playerStats.bufferedDuration,
                            isLive = playerStats.distanceFromLive > 0,
                            liveOffsetMs = playerStats.distanceFromLive
                        )
                    }
                }
            }
            
            override fun onError(error: live.hms.video.error.HMSException) {
                // 100ms SDK error - log or update error count
                viewModelScope.launch {
                    _hmsUiState.update { it.copy(errorCount = it.errorCount + 1) }
                }
            }
        })
    }

    fun setMode(isHost: Boolean) {
        _customUiState.update { it.copy(isHost = isHost) }
    }

    fun joinMeeting(context: Context, token: String, meetingId: String, participantName: String) {
        if (meetingId.isBlank()) return
        
        _customUiState.update { it.copy(meetingId = meetingId) }
        
        // Configure VideoSDK with token
        VideoSDK.config(token)
        
        // Create and join meeting
        meeting = VideoSDK.initMeeting(
            context,
            meetingId,
            participantName,
            true, // micEnabled
            true, // webcamEnabled  
            null, // participantId
            null, // mode
            false, // multiStream
            null, // customTracks
            null // preferredProtocol
        )
        
        // Add meeting event listener
        meeting?.addEventListener(object : MeetingEventListener() {
            override fun onMeetingJoined() {
                _customUiState.update { it.copy(isMeetingJoined = true) }
            }

            override fun onHlsStateChanged(hlsState: org.json.JSONObject) {
                viewModelScope.launch {
                    try {
                        val  status = hlsState.optString("status", "IDLE")
                        val playbackUrl = hlsState.optString("playbackHlsUrl", "")
                        val livestreamUrl = hlsState.optString("livestreamUrl", "")
                        
                        _customUiState.update { it.copy(hlsState = status) }
                        
                        // Auto-play when HLS is playable (for both host and viewer)
                        if (status == "HLS_PLAYABLE" && playbackUrl.isNotEmpty()) {
                            _customUiState.update { it.copy(playbackHlsUrl = playbackUrl) }
                            playHlsStream(playbackUrl)
                        } else if (status == "HLS_STOPPED") {
                            stopPlayback()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            override fun onMeetingLeft() {
                _customUiState.update { it.copy(isMeetingJoined = false) }
            }
        })
        
        // Join the meeting
        meeting?.join()
    }

    fun startHls() {
        val config = org.json.JSONObject()
        meeting?.startHls(config, null)
        _customUiState.update { it.copy(hlsState = "HLS_STARTING") }
    }

    fun stopHls() {
        meeting?.stopHls()
        _customUiState.update { it.copy(hlsState = "HLS_STOPPING") }
    }

    fun leaveMeeting() {
        meeting?.leave()
        meeting = null
        _customUiState.update {
            it.copy(
                isMeetingJoined = false,
                hlsState = "IDLE",
                playbackHlsUrl = ""
            )
        }
    }

    private fun playHlsStream(url: String) {
        if (url.isBlank()) return
        
        _customUiState.update { it.copy(playbackUrl = url, isPlaying = true) }
        
        exoPlayer?.apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            play()
        }
    }

    private fun stopPlayback() {
        exoPlayer?.stop()
        _customUiState.update { it.copy(isPlaying = false) }
    }

    override fun onCleared() {
        super.onCleared()
        // Cleanup
        meeting?.leave()
        customStatsCollector?.release()
        hmsPlayerEventsCollector?.removeListener()
        exoPlayer?.release()
        meeting = null
        customStatsCollector = null
        hmsPlayerEventsCollector = null
        exoPlayer = null
    }
}

/**
 * UI State for HLS Viewer with comprehensive stats
 */
data class HlsStatsUiState(
    // Playback State
    val playbackUrl: String = "",
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val playbackState: String = "IDLE",
    
    // Timing Information
    val currentPositionMs: Long = 0,
    val durationMs: Long = 0,
    val bufferedPositionMs: Long = 0,
    
    // Quality Metrics
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val frameRate: Float = 0f,
    val bitrate: Long = 0,
    val estimatedBandwidth: Long = 0,
    val totalBytesLoaded: Long = 0,
    
    // Performance Metrics
    val droppedFrames: Int = 0,
    val totalFramesRendered: Int = 0,
    val audioBufferMs: Long = 0,
    val videoBufferMs: Long = 0,
    val targetBufferMs: Long = 0,
    
    // HLS-specific
    val isLive: Boolean = false,
    val liveOffsetMs: Long? = null,
    
    // Session metrics
    val joinTimeMs: Long = 0,
    val totalRebufferDurationMs: Long = 0,
    val rebufferCount: Int = 0,
    
    // Error tracking
    val errorCount: Int = 0,
    
    // Meeting-related
    val meetingId: String = "",
    val hlsState: String = "IDLE",
    val playbackHlsUrl: String = "",
    val isHost: Boolean = false,
    val isMeetingJoined: Boolean = false,
    
    // Session ended
    val sessionEnded: Boolean = false,
    val totalRebuffers: Int = 0
)
