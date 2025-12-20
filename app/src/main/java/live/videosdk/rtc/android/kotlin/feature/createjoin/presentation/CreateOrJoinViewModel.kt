package live.videosdk.rtc.android.kotlin.feature.createjoin.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import live.videosdk.rtc.android.CustomStreamTrack
import live.videosdk.rtc.android.VideoSDK
import live.videosdk.rtc.android.mediaDevice.AudioDeviceInfo
import live.videosdk.rtc.android.mediaDevice.VideoDeviceInfo
import org.webrtc.VideoTrack

/**
 * ViewModel for CreateOrJoin screen
 * Manages state for camera preview, device selection, and permissions
 */
class CreateOrJoinViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CreateOrJoinUiState())
    val uiState: StateFlow<CreateOrJoinUiState> = _uiState.asStateFlow()

    private var videoTrack: CustomStreamTrack? = null

    init {
        initializeDevices()
    }

    private fun initializeDevices() {
        viewModelScope.launch {
            // Set up audio device change listener
            VideoSDK.setAudioDeviceChangeListener { selectedDevice, availableDevices ->
                updateAudioDevices(selectedDevice, availableDevices.toList())
            }
        }
    }

    fun onPermissionsGranted() {
        _uiState.update { it.copy(permissionsGranted = true, micEnabled = true, webcamEnabled = true) }
    }

    fun toggleMic() {
        if (!_uiState.value.permissionsGranted) return
        _uiState.update { it.copy(micEnabled = !it.micEnabled) }
    }

    fun toggleWebcam(context: android.content.Context) {
        if (!_uiState.value.permissionsGranted) return
        
        val newWebcamState = !_uiState.value.webcamEnabled
        _uiState.update { it.copy(webcamEnabled = newWebcamState) }

        if (newWebcamState) {
            // Create and start video track
            videoTrack = VideoSDK.createCameraVideoTrack(
                "h720p_w960p",
                "front",
                CustomStreamTrack.VideoMode.TEXT,
                true,
                context,
                VideoSDK.getSelectedVideoDevice()
            )
            _uiState.update { it.copy(videoTrack = videoTrack?.track as? VideoTrack) }
        } else {
            // Stop and dispose video track
            videoTrack?.track?.dispose()
            videoTrack = null
            _uiState.update { it.copy(videoTrack = null) }
        }
    }

    fun switchCamera(context: android.content.Context) {
        val selectedDevice = VideoSDK.getSelectedVideoDevice()
        val currentFacingMode = selectedDevice.facingMode
        
        val videoDevices = VideoSDK.getVideoDevices()
        val targetFacingMode = if (currentFacingMode.name == "front") {
            live.videosdk.rtc.android.mediaDevice.FacingMode.back
        } else {
            live.videosdk.rtc.android.mediaDevice.FacingMode.front
        }

        val newDevice = videoDevices.firstOrNull { it.facingMode == targetFacingMode }
        newDevice?.let {
            VideoSDK.setSelectedVideoDevice(it)
            updateCameraView(context, it)
        }
    }

    private fun updateCameraView(context: android.content.Context, device: VideoDeviceInfo? = null) {
        if (_uiState.value.webcamEnabled) {
            videoTrack?.track?.dispose()
            videoTrack = VideoSDK.createCameraVideoTrack(
                "h720p_w960p",
                "front",
                CustomStreamTrack.VideoMode.TEXT,
                true,
                context,
                device
            )
            _uiState.update { it.copy(videoTrack = videoTrack?.track as? VideoTrack) }
        }
    }

    fun getAudioDevices(): List<AudioDeviceInfo> {
        return VideoSDK.getAudioDevices().toList()
    }

    fun selectAudioDevice(device: AudioDeviceInfo) {
        VideoSDK.setSelectedAudioDevice(device)
    }

    private fun updateAudioDevices(selected: AudioDeviceInfo, available: List<AudioDeviceInfo>) {
        _uiState.update { 
            it.copy(
                selectedAudioDevice = selected,
                availableAudioDevices = available
            )
        }
    }

    fun showAudioDeviceSheet(show: Boolean) {
        _uiState.update { it.copy(showAudioDeviceSheet = show) }
    }

    override fun onCleared() {
        super.onCleared()
        videoTrack?.track?.dispose()
        videoTrack = null
    }
}

/**
 * UI State for CreateOrJoin screen
 */
data class CreateOrJoinUiState(
    val permissionsGranted: Boolean = false,
    val micEnabled: Boolean = false,
    val webcamEnabled: Boolean = false,
    val videoTrack: VideoTrack? = null,
    val selectedAudioDevice: AudioDeviceInfo? = null,
    val availableAudioDevices: List<AudioDeviceInfo> = emptyList(),
    val showAudioDeviceSheet: Boolean = false
)
