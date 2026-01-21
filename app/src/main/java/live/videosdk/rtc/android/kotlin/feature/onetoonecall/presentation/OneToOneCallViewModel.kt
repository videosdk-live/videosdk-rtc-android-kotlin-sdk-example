package live.videosdk.rtc.android.kotlin.feature.onetoonecall.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import live.videosdk.rtc.android.Meeting
import live.videosdk.rtc.android.Participant
import live.videosdk.rtc.android.VideoSDK
import live.videosdk.rtc.android.lib.MeetingState
import live.videosdk.rtc.android.listeners.MeetingEventListener
import live.videosdk.rtc.android.listeners.PubSubMessageListener
import live.videosdk.rtc.android.model.PubSubPublishOptions
import live.videosdk.rtc.android.mediaDevice.AudioDeviceInfo
import org.json.JSONObject

/**
 * ViewModel for One-to-One Call screen
 * Manages meeting state, remote participant, screen sharing, chat, and hand raise events
 */
class OneToOneCallViewModel : ViewModel() {

    companion object {
        private const val TAG = "OneToOneCall"
    }

    private val _uiState = MutableStateFlow(OneToOneCallUiState())
    val uiState: StateFlow<OneToOneCallUiState> = _uiState.asStateFlow()

    private var meeting: Meeting? = null
    private val chatTopic = "CHAT"
    private val handRaiseTopic = "HAND_RAISE"

    fun initializeMeeting(meeting: Meeting, initialMicEnabled: Boolean, initialWebcamEnabled: Boolean) {
        this.meeting = meeting
        
        _uiState.update {
            it.copy(
                micEnabled = initialMicEnabled,
                webcamEnabled = initialWebcamEnabled
            )
        }

        setupMeetingListeners()
        subscribeToPubSubTopics()
        setupAudioDeviceListener()
    }

    private fun setupMeetingListeners() {
        meeting?.addEventListener(object : MeetingEventListener() {
            override fun onMeetingJoined() {
                android.util.Log.d(TAG, "📍 onMeetingJoined() - Meeting joined successfully")
                updateRemoteParticipant()
            }

            override fun onParticipantJoined(participant: Participant) {
                android.util.Log.d(TAG, "📍 onParticipantJoined() - Participant: ${participant.displayName} (ID: ${participant.id})")
                updateRemoteParticipant()
            }

            override fun onParticipantLeft(participant: Participant) {
                android.util.Log.d(TAG, "📍 onParticipantLeft() - Participant: ${participant.displayName} (ID: ${participant.id})")
                updateRemoteParticipant()
                // Clear remote hand raise if they left
                _uiState.update { it.copy(isRemoteHandRaised = false) }
            }

            override fun onPresenterChanged(participantId: String?) {
                android.util.Log.d(TAG, "📍 onPresenterChanged() - PresenterID: $participantId")
                _uiState.update { it.copy(presenterId = participantId) }
            }

            override fun onMeetingLeft() {
                android.util.Log.d(TAG, "📍 onMeetingLeft() - Meeting left, triggering navigation")
                _uiState.update { it.copy(shouldNavigateAway = true) }
            }

            override fun onSpeakerChanged(participantId: String?) {
                android.util.Log.d(TAG, "📍 onSpeakerChanged() - SpeakerID: $participantId")
            }

            override fun onRecordingStateChanged(recordingState: String) {
                android.util.Log.d(TAG, "📍 onRecordingStateChanged() - State: $recordingState")
            }

            override fun onLivestreamStateChanged(livestreamState: String) {
                android.util.Log.d(TAG, "📍 onLivestreamStateChanged() - State: $livestreamState")
            }

            override fun onHlsStateChanged(hlsState: org.json.JSONObject) {
                android.util.Log.d(TAG, "📍 onHlsStateChanged() - State: $hlsState")
            }

            override fun onTranscriptionStateChanged(transcriptionState: org.json.JSONObject) {
                android.util.Log.d(TAG, "📍 onTranscriptionStateChanged() - State: $transcriptionState")
            }

            override fun onMeetingStateChanged(state: MeetingState) {
                android.util.Log.d(TAG, "📍 onMeetingStateChanged() - State: $state")
            }

            override fun onParticipantModeChanged(data: org.json.JSONObject) {
                android.util.Log.d(TAG, "📍 onParticipantModeChanged() - Data: $data")
            }

            override fun onPinStateChanged(data: org.json.JSONObject) {
                android.util.Log.d(TAG, "📍 onPinStateChanged() - Data: $data")
            }

            override fun onError(error: org.json.JSONObject) {
                android.util.Log.e(TAG, "📍 onError() - Error: $error")
            }

            override fun onExternalCallStarted() {
                android.util.Log.d(TAG, "📍 onExternalCallStarted()")
            }
        })
    }

    private fun subscribeToPubSubTopics() {
        // Subscribe to chat messages  
        meeting?.pubSub?.subscribe(chatTopic, object : PubSubMessageListener {
            override fun onMessageReceived(pubSubMessage: live.videosdk.rtc.android.lib.PubSubMessage) {
                try {
                    val json = JSONObject(pubSubMessage.message)
                    val chatMessage = live.videosdk.rtc.android.kotlin.feature.groupcall.presentation.ChatMessage(
                        id = pubSubMessage.id,
                        senderId = pubSubMessage.senderId,
                        senderName = pubSubMessage.senderName,
                        message = json.optString("message", ""),
                        timestamp = pubSubMessage.timestamp
                    )
                    _uiState.update { state ->
                        state.copy(chatMessages = state.chatMessages + chatMessage)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            override fun onOldMessagesReceived(messages: List<live.videosdk.rtc.android.lib.PubSubMessage>) {
                // Handle old messages
                messages.forEach { onMessageReceived(it) }
            }
        })

        // Subscribe to hand raise events
        meeting?.pubSub?.subscribe(handRaiseTopic, object : PubSubMessageListener {
            override fun onMessageReceived(pubSubMessage: live.videosdk.rtc.android.lib.PubSubMessage) {
                try {
                    val json = JSONObject(pubSubMessage.message)
                    val isRaised = json.optBoolean("raised", false)
                    
                    // In one-to-one, we only care if remote participant raised hand
                    val isRemote = pubSubMessage.senderId != meeting?.localParticipant?.id
                    if (isRemote) {
                        _uiState.update { it.copy(isRemoteHandRaised = isRaised) }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            override fun onOldMessagesReceived(messages: List<live.videosdk.rtc.android.lib.PubSubMessage>) {
                // Handle old messages
                messages.forEach { onMessageReceived(it) }
            }
        })
    }

    private fun setupAudioDeviceListener() {
        VideoSDK.setAudioDeviceChangeListener { selectedDevice, availableDevices ->
            _uiState.update {
                it.copy(
                    selectedAudioDevice = selectedDevice,
                    availableAudioDevices = availableDevices.toList()
                )
            }
        }
    }

    private fun updateRemoteParticipant() {
        meeting?.let { meeting ->
            Log.e("OneToOneCall", "Participant: -> Size -> ${meeting.participants.size}")
            val remote = meeting.participants.values.firstOrNull { it.id != meeting.localParticipant.id }
            _uiState.update { it.copy(remoteParticipant = remote) }
        }
    }

    fun toggleMic() {
        val newState = !_uiState.value.micEnabled
        if (newState) {
            meeting?.unmuteMic()
        } else {
            meeting?.muteMic()
        }
        _uiState.update { it.copy(micEnabled = newState) }
    }

    fun toggleWebcam() {
        val newState = !_uiState.value.webcamEnabled
        if (newState) {
            meeting?.enableWebcam()
        } else {
            meeting?.disableWebcam()
        }
        _uiState.update { it.copy(webcamEnabled = newState) }
    }

    fun switchCamera() {
        meeting?.changeWebcam()
    }

    fun toggleScreenShare() {
        // Screen sharing requires special permission handling with MediaProjection
        // TODO: Implement with proper Intent from MediaProjectionManager
        val isSharing = _uiState.value.isScreenSharing
        _uiState.update { it.copy(isScreenSharing = !isSharing) }
    }

    fun toggleHandRaise() {
        val isRaised = _uiState.value.isHandRaised
        val newState = !isRaised
        
        val localParticipant = meeting?.localParticipant
        val localId = localParticipant?.id ?: ""
        val localName = localParticipant?.displayName ?: "Unknown"
        
        // Publish hand raise state with sender info embedded in JSON
        val message = JSONObject().apply {
            put("raised", newState)
            put("senderId", localId)
            put("senderName", localName)
        }.toString()
        
        val options = PubSubPublishOptions()
        options.isPersist = false
        meeting?.pubSub?.publish(handRaiseTopic, message, options)
        
        // Update local state immediately for both isHandRaised and raisedHands map
        _uiState.update { state ->
            val updatedHands = state.raisedHands.toMutableMap()
            if (newState) {
                updatedHands[localId] = localName
            } else {
                updatedHands.remove(localId)
            }
            state.copy(
                isHandRaised = newState,
                raisedHands = updatedHands
            )
        }
    }

    fun sendChatMessage(message: String) {
        if (message.isBlank()) return
        
       val localParticipant = meeting?.localParticipant
        
        val chatJson = JSONObject().apply {
            put("message", message)
            put("senderId", localParticipant?.id ?: "")
            put("senderName", localParticipant?.displayName ?: "Unknown")
        }.toString()
        
        val options = PubSubPublishOptions()
        options.isPersist = true
        meeting?.pubSub?.publish(chatTopic, chatJson, options)
    }

    fun showChatSheet(show: Boolean) {
        _uiState.update { it.copy(showChatSheet = show) }
    }

    fun showAudioDeviceSheet(show: Boolean) {
        _uiState.update { it.copy(showAudioDeviceSheet = show) }
    }

    fun leaveMeeting() {
        Log.d(TAG, "📍 leaveMeeting() called")
        // Clear remote participant first to stop video rendering before tracks are disposed
        _uiState.update { it.copy(remoteParticipant = null, isLeaving = true) }
        meeting?.leave()
        meeting?.removeAllListeners()

    }

    fun selectAudioDevice(device: AudioDeviceInfo) {
        VideoSDK.setSelectedAudioDevice(device)
    }

    override fun onCleared() {
        super.onCleared()
        // Unsubscribe from topics
        meeting?.removeAllListeners()
        meeting?.pubSub?.unsubscribe(chatTopic, null)
        meeting?.pubSub?.unsubscribe(handRaiseTopic, null)
    }
}

/**
 * UI State for One-to-One Call screen
 */
data class OneToOneCallUiState(
    val remoteParticipant: Participant? = null,
    val micEnabled: Boolean = true,
    val webcamEnabled: Boolean = true,
    val isScreenSharing: Boolean = false,
    val isHandRaised: Boolean = false,
    val isRemoteHandRaised: Boolean = false,
    val presenterId: String? = null,
    val chatMessages: List<live.videosdk.rtc.android.kotlin.feature.groupcall.presentation.ChatMessage> = emptyList(),
    val raisedHands: Map<String, String> = emptyMap(), // participantId -> name
    val showChatSheet: Boolean = false,
    val showAudioDeviceSheet: Boolean = false,
    val selectedAudioDevice: AudioDeviceInfo? = null,
    val availableAudioDevices: List<AudioDeviceInfo> = emptyList(),
    val shouldNavigateAway: Boolean = false,
    val isLeaving: Boolean = false
)
