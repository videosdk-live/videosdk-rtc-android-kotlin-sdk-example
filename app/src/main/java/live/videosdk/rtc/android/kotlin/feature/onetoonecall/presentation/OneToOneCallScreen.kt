package live.videosdk.rtc.android.kotlin.feature.onetoonecall.presentation

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import live.videosdk.rtc.android.CustomStreamTrack
import live.videosdk.rtc.android.VideoSDK
import live.videosdk.rtc.android.kotlin.core.ui.components.ControlButton
import live.videosdk.rtc.android.kotlin.core.ui.components.ParticipantTile
import live.videosdk.rtc.android.kotlin.core.ui.theme.RedMd400
import live.videosdk.rtc.android.kotlin.core.ui.theme.VideoSDKTheme
import live.videosdk.rtc.android.kotlin.core.ui.theme.White
import live.videosdk.rtc.android.kotlin.feature.groupcall.presentation.components.ChatBottomSheet
import live.videosdk.rtc.android.listeners.ParticipantEventListener
import org.webrtc.VideoTrack

/**
 * Enhanced One-to-One Call Screen with Chat, Hand Raise, and Audio Device Selection
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OneToOneCallScreen(
    token: String,
    meetingId: String,
    initialMicEnabled: Boolean,
    initialWebcamEnabled: Boolean,
    participantName: String,
    onLeaveCall: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OneToOneCallViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // Initialize meeting
    LaunchedEffect(Unit) {
        VideoSDK.config(token)
        
        val customTracks: MutableMap<String, CustomStreamTrack> = HashMap()
        val videoCustomTrack = VideoSDK.createCameraVideoTrack(
            "h720p_w960p", "front", CustomStreamTrack.VideoMode.TEXT, true, context, VideoSDK.getSelectedVideoDevice()
        )
        customTracks["video"] = videoCustomTrack
        val audioCustomTrack = VideoSDK.createAudioTrack("high_quality", context)
        customTracks["mic"] = audioCustomTrack

        val meeting = VideoSDK.initMeeting(
            context, meetingId, participantName,
            initialMicEnabled, initialWebcamEnabled, null, null, true, customTracks, null
        )
        
        viewModel.initializeMeeting(meeting, initialMicEnabled, initialWebcamEnabled)
        meeting.join()
    }

    VideoSDKTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(uiState.remoteParticipant?.displayName ?: "Waiting for participant...", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(meetingId, fontSize = 12.sp, color = White.copy(alpha = 0.7f))
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.switchCamera() }) {
                            Icon(Icons.Rounded.Cameraswitch, "Switch Camera", tint = White)
                        }
                        
                        IconButton(onClick = { viewModel.showAudioDeviceSheet(true) }) {
                            val icon = when (uiState.selectedAudioDevice?.label) {
                                "BLUETOOTH" -> Icons.Default.Bluetooth
                                "WIRED_HEADSET" -> Icons.Default.Headphones
                                "SPEAKER_PHONE" -> Icons.Default.VolumeUp
                                else -> Icons.Default.Call
                            }
                            Icon(icon, "Audio Device", tint = White)
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color(0xFF212121))
            ) {
                Column(Modifier.fillMaxSize()) {
                    // Main video area
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        // Remote participant video (fullscreen)
                        uiState.remoteParticipant?.let { remote ->
                            var videoTrack by remember { mutableStateOf<VideoTrack?>(null) }
                            
                            LaunchedEffect(remote) {
                                remote.streams.values.find { it.kind == "video" }?.let {
                                    videoTrack = it.track as VideoTrack
                                }
                            }

                            DisposableEffect(remote) {
                                val listener = object : ParticipantEventListener() {
                                    override fun onStreamEnabled(stream: live.videosdk.rtc.android.Stream) {
                                        if (stream.kind == "video") {
                                            videoTrack = stream.track as VideoTrack
                                        }
                                    }

                                    override fun onStreamDisabled(stream: live.videosdk.rtc.android.Stream) {
                                        if (stream.kind == "video") {
                                            videoTrack = null
                                        }
                                    }
                                }
                                remote.addEventListener(listener)
                                onDispose { remote.removeEventListener(listener) }
                            }

                            ParticipantTile(
                                participant = remote,
                                videoTrack = videoTrack,
                                isLocal = false,
                                modifier = Modifier.fillMaxSize()
                            )
                        } ?: run {
                            // Waiting state
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = White)
                                    Spacer(Modifier.height(16.dp))
                                    Text("Waiting for participant to join...", color = White)
                                }
                            }
                        }

                        // Local video (small overlay in corner)
                        // TODO: Add local video thumbnail
                    }

                    // Bottom controls
                    OneToOneBottomControls(
                        micEnabled = uiState.micEnabled,
                        webcamEnabled = uiState.webcamEnabled,
                        unreadMessages = uiState.chatMessages.size,
                        onToggleMic = { viewModel.toggleMic() },
                        onToggleWebcam = { viewModel.toggleWebcam() },
                        onOpenChat = { viewModel.showChatSheet(true) },
                        onLeaveCall = onLeaveCall
                    )
                }
            }
        }

        // Chat bottom sheet
        if (uiState.showChatSheet) {
            ChatBottomSheet(
                messages = uiState.chatMessages,
                onSendMessage = { viewModel.sendChatMessage(it) },
                onDismiss = { viewModel.showChatSheet(false) }
            )
        }

        // Audio device bottom sheet
        if (uiState.showAudioDeviceSheet) {
            AudioDeviceBottomSheet(
                devices = uiState.availableAudioDevices,
                selectedDevice = uiState.selectedAudioDevice,
                onDeviceSelected = { device ->
                    viewModel.selectAudioDevice(device)
                    viewModel.showAudioDeviceSheet(false)
                    Toast.makeText(context, "Selected ${device.label}", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { viewModel.showAudioDeviceSheet(false) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OneToOneBottomControls(
    micEnabled: Boolean,
    webcamEnabled: Boolean,
    unreadMessages: Int,
    onToggleMic: () -> Unit,
    onToggleWebcam: () -> Unit,
    onOpenChat: () -> Unit,
    onLeaveCall: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Chat
        BadgedBox(
            badge = {
                if (unreadMessages > 0) {
                    Badge { Text("$unreadMessages") }
                }
            }
        ) {
            ControlButton(
                icon = Icons.Rounded.Chat,
                onClick = onOpenChat
            )
        }

        // Mic
        ControlButton(
            icon = if (micEnabled) Icons.Rounded.Mic else Icons.Rounded.MicOff,
            backgroundColor = if (micEnabled) White else RedMd400,
            iconColor = if (micEnabled) Color.Black else White,
            onClick = onToggleMic
        )

        // Leave
        ControlButton(
            icon = Icons.Rounded.CallEnd,
            backgroundColor = RedMd400,
            iconColor = White,
            size = 70.dp,
            onClick = onLeaveCall
        )

        // Webcam
        ControlButton(
            icon = if (webcamEnabled) Icons.Rounded.Videocam else Icons.Rounded.VideocamOff,
            backgroundColor = if (webcamEnabled) White else RedMd400,
            iconColor = if (webcamEnabled) Color.Black else White,
            onClick = onToggleWebcam
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioDeviceBottomSheet(
    devices: List<live.videosdk.rtc.android.mediaDevice.AudioDeviceInfo>,
    selectedDevice: live.videosdk.rtc.android.mediaDevice.AudioDeviceInfo?,
    onDeviceSelected: (live.videosdk.rtc.android.mediaDevice.AudioDeviceInfo) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Select Audio Device",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            devices.forEach { device ->
                ListItem(
                    headlineContent = { Text(device.label) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDeviceSelected(device) },
                    colors = ListItemDefaults.colors(
                        containerColor = if (device == selectedDevice) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
