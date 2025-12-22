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
import live.videosdk.rtc.android.listeners.MeetingEventListener
import live.videosdk.rtc.android.hlsstats.HlsStatsCollector
import live.videosdk.rtc.android.hlsstats.HlsStatsListener
import live.videosdk.rtc.android.hlsstats.models.HlsPlaybackStats

/**
 * ViewModel for HLS Viewer with stats collection and meeting integration
 * Supports both Host mode (start/stop HLS) and Viewer mode (watch HLS with stats)
 */
class HlsStatsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HlsStatsUiState())
    val uiState: StateFlow<HlsStatsUiState> = _uiState.asStateFlow()

    private var exoPlayer: ExoPlayer? = null
    private var statsCollector: HlsStatsCollector? = null
    private var meeting: Meeting? = null

    @OptIn(UnstableApi::class)
    fun initializePlayer(player: ExoPlayer) {
        exoPlayer = player
        
        // Use ExoPlayer's built-in analytics listener instead of HlsStatsCollector
        // This runs on the main thread and avoids threading issues
        player.addAnalyticsListener(object : androidx.media3.exoplayer.analytics.AnalyticsListener {
            override fun onPlaybackStateChanged(
                eventTime: androidx.media3.exoplayer.analytics.AnalyticsListener.EventTime,
                state: Int
            ) {
                updatePlaybackStats()
            }
            
            override fun onIsPlayingChanged(
                eventTime: androidx.media3.exoplayer.analytics.AnalyticsListener.EventTime,
                isPlaying: Boolean
            ) {
                updatePlaybackStats()
            }
            
            override fun onVideoSizeChanged(
                eventTime: androidx.media3.exoplayer.analytics.AnalyticsListener.EventTime,
                videoSize: androidx.media3.common.VideoSize
            ) {
                updatePlaybackStats()
            }
            
            override fun onBandwidthEstimate(
                eventTime: androidx.media3.exoplayer.analytics.AnalyticsListener.EventTime,
                totalLoadTimeMs: Int,
                totalBytesLoaded: Long,
                bitrateEstimate: Long
            ) {
                viewModelScope.launch {
                    _uiState.update { it.copy(bitrate = bitrateEstimate) }
                }
            }
        })
        
        // Also add a regular Player.Listener for additional events
        player.addListener(object : androidx.media3.common.Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updatePlaybackStats()
            }
            
            override fun onPlaybackStateChanged(playbackState: Int) {
                updatePlaybackStats()
            }
        })
    }
    
    @OptIn(UnstableApi::class)
    private fun updatePlaybackStats() {
        exoPlayer?.let { player ->
            viewModelScope.launch {
                val isBuffering = player.playbackState == androidx.media3.common.Player.STATE_BUFFERING
                val isPlaying = player.isPlaying
                val currentFormat = player.videoFormat
                val bitrate = currentFormat?.bitrate?.toLong() ?: _uiState.value.bitrate
                
                _uiState.update { 
                    it.copy(
                        isBuffering = isBuffering,
                        isPlaying = isPlaying,
                        bitrate = bitrate
                    )
                }
            }
        }
    }

    fun setMode(isHost: Boolean) {
        _uiState.update { it.copy(isHost = isHost) }
    }

    fun joinMeeting(context: Context, token: String, meetingId: String, participantName: String) {
        if (meetingId.isBlank()) return
        
        _uiState.update { it.copy(meetingId = meetingId) }
        
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
                _uiState.update { it.copy(isMeetingJoined = true) }
            }

            override fun onHlsStateChanged(hlsState: org.json.JSONObject) {
                viewModelScope.launch {
                    try {
                        val  status = hlsState.optString("status", "IDLE")
                        val playbackUrl = hlsState.optString("playbackHlsUrl", "")
                        val livestreamUrl = hlsState.optString("livestreamUrl", "")
                        
                        _uiState.update { it.copy(hlsState = status) }
                        
                        // Auto-play when HLS is playable (for both host and viewer)
                        if (status == "HLS_PLAYABLE" && playbackUrl.isNotEmpty()) {
                            _uiState.update { it.copy(playbackHlsUrl = playbackUrl) }
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
                _uiState.update { it.copy(isMeetingJoined = false) }
            }
        })
        
        // Join the meeting
        meeting?.join()
    }

    fun startHls() {
        val config = org.json.JSONObject()
        meeting?.startHls(config, null)
        _uiState.update { it.copy(hlsState = "HLS_STARTING") }
    }

    fun stopHls() {
        meeting?.stopHls()
        _uiState.update { it.copy(hlsState = "HLS_STOPPING") }
    }

    fun leaveMeeting() {
        meeting?.leave()
        meeting = null
        _uiState.update {
            it.copy(
                isMeetingJoined = false,
                hlsState = "IDLE",
                playbackHlsUrl = ""
            )
        }
    }

    private fun playHlsStream(url: String) {
        if (url.isBlank()) return
        
        _uiState.update { it.copy(playbackUrl = url, isPlaying = true) }
        
        exoPlayer?.apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            play()
        }
    }

    private fun stopPlayback() {
        exoPlayer?.stop()
        _uiState.update { it.copy(isPlaying = false) }
    }

    override fun onCleared() {
        super.onCleared()
        // Cleanup
        meeting?.leave()
        statsCollector?.release()
        exoPlayer?.release()
        meeting = null
        statsCollector = null
        exoPlayer = null
    }
}

/**
 * UI State for HLS Viewer with stats and meeting info
 */
data class HlsStatsUiState(
    val playbackUrl: String = "",
    val bitrate: Long = 0,
    val isBuffering: Boolean = false,
    val rebufferCount: Int = 0,
    val isPlaying: Boolean = false,
    val sessionEnded: Boolean = false,
    val totalRebuffers: Int = 0,
    // Meeting-related
    val meetingId: String = "",
    val hlsState: String = "IDLE",
    val playbackHlsUrl: String = "",
    val isHost: Boolean = false,
    val isMeetingJoined: Boolean = false
)
