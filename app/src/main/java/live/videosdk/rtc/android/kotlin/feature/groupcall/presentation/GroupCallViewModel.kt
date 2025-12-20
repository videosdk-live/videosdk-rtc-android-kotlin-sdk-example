package live.videosdk.rtc.android.kotlin.feature.groupcall.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import live.videosdk.rtc.android.Meeting
import live.videosdk.rtc.android.Participant
import live.videosdk.rtc.android.VideoSDK
import live.videosdk.rtc.android.listeners.MeetingEventListener
import live.videosdk.rtc.android.listeners.PubSubMessageListener
import live.videosdk.rtc.android.mediaDevice.AudioDeviceInfo
import org.json.JSONObject

/**
 * ViewModel for Group Call screen
 * Manages meeting state, participants, screen sharing, chat, and hand raise events
 */
class GroupCallViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(GroupCallUiState())
    val uiState: StateFlow<GroupCallUiState> = _uiState.asStateFlow()

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
                updateParticipants()
            }

            override fun onParticipantJoined(participant: Participant) {
                updateParticipants()
            }

            override fun onParticipantLeft(participant: Participant) {
                updateParticipants()
                // Remove from raised hands if they left
                _uiState.update { state ->
                    state.copy(
                        raisedHands = state.raisedHands.filter { it.key != participant.id }
                    )
                }
            }

            override fun onPresenterChanged(participantId: String?) {
                _uiState.update { it.copy(presenterId = participantId) }
            }

            override fun onMeetingLeft() {
                // Meeting ended
            }
        })
    }

    private fun subscribeToPubSubTopics() {
        // Subscribe to chat messages  
        meeting?.pubSub?.subscribe(chatTopic, object : PubSubMessageListener {
            override fun onMessageReceived(pubSubMessage: live.videosdk.rtc.android.lib.PubSubMessage) {
                try {
                    val json = JSONObject(pubSubMessage.message)
                    val chatMessage = ChatMessage(
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
                    
                    _uiState.update { state ->
                        val updatedHands = state.raisedHands.toMutableMap()
                        if (isRaised) {
                            updatedHands[pubSubMessage.senderId] = pubSubMessage.senderName
                        } else {
                            updatedHands.remove(pubSubMessage.senderId)
                        }
                        state.copy(raisedHands = updatedHands)
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

    private fun updateParticipants() {
        meeting?.let { meeting ->
            _uiState.update {
                it.copy(participants = meeting.participants.values.toList())
            }
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

    fun toggleScreenShare(mediaProjectionIntent: android.content.Intent? = null) {
        val isSharing = _uiState.value.isScreenSharing
        if (isSharing) {
            meeting?.disableScreenShare()
            _uiState.update { it.copy(isScreenSharing = false) }
        } else {
            // Screen sharing requires MediaProjection Intent from Activity
            // The Activity will request permission and call this with the Intent
            if (mediaProjectionIntent != null) {
                meeting?.enableScreenShare(mediaProjectionIntent, true)
                _uiState.update { it.copy(isScreenSharing = true) }
            } else {
                // Signal that we need permission - Activity will handle the request
                _uiState.update { it.copy(needsScreenSharePermission = true) }
            }
        }
    }
    
    fun screenSharePermissionHandled() {
        _uiState.update { it.copy(needsScreenSharePermission = false) }
    }

    fun toggleHandRaise() {
        val isRaised = _uiState.value.isHandRaised
        val newState = !isRaised
        
        val localParticipant = meeting?.localParticipant
        
        // Publish hand raise state with sender info embedded in JSON
        val message = JSONObject().apply {
            put("raised", newState)
            put("senderId", localParticipant?.id ?: "")
            put("senderName", localParticipant?.displayName ?: "Unknown")
        }.toString()
        
        meeting?.pubSub?.publish(handRaiseTopic, message, null)
        
        _uiState.update { it.copy(isHandRaised = newState) }
    }

    fun sendChatMessage(message: String) {
        if (message.isBlank()) return
        
        val localParticipant = meeting?.localParticipant
        
        val chatJson = JSONObject().apply {
            put("message", message)
            put("senderId", localParticipant?.id ?: "")
            put("senderName", localParticipant?.displayName ?: "Unknown")
        }.toString()
        
        meeting?.pubSub?.publish(chatTopic, chatJson, null)
    }

    fun showChatSheet(show: Boolean) {
        _uiState.update { it.copy(showChatSheet = show) }
    }

    fun showAudioDeviceSheet(show: Boolean) {
        _uiState.update { it.copy(showAudioDeviceSheet = show) }
    }

    fun selectAudioDevice(device: AudioDeviceInfo) {
        VideoSDK.setSelectedAudioDevice(device)
    }

    fun getAudioDevices(): List<AudioDeviceInfo> {
        return VideoSDK.getAudioDevices().toList()
    }

    override fun onCleared() {
        super.onCleared()
        // Unsubscribe from topics
        meeting?.pubSub?.unsubscribe(chatTopic, null)
        meeting?.pubSub?.unsubscribe(handRaiseTopic, null)
    }
}

/**
 * UI State for Group Call screen
 */
data class GroupCallUiState(
    val participants: List<Participant> = emptyList(),
    val micEnabled: Boolean = true,
    val webcamEnabled: Boolean = true,
    val isScreenSharing: Boolean = false,
    val isHandRaised: Boolean = false,
    val presenterId: String? = null,
    val chatMessages: List<ChatMessage> = emptyList(),
    val raisedHands: Map<String, String> = emptyMap(), // participantId -> name
    val showChatSheet: Boolean = false,
    val showAudioDeviceSheet: Boolean = false,
    val selectedAudioDevice: AudioDeviceInfo? = null,
    val availableAudioDevices: List<AudioDeviceInfo> = emptyList(),
    val needsScreenSharePermission: Boolean = false
)

/**
 * Chat message model
 */
data class ChatMessage(
    val id: String,
    val senderId: String,
    val senderName: String,
    val message: String,
    val timestamp: Long
)
