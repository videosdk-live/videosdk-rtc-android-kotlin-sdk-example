package live.videosdk.rtc.android.kotlin.feature.groupcall.presentation

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.lifecycle.viewmodel.compose.viewModel
import live.videosdk.rtc.android.CustomStreamTrack
import live.videosdk.rtc.android.Meeting
import live.videosdk.rtc.android.VideoSDK
import live.videosdk.rtc.android.kotlin.core.ui.components.ControlButton
import live.videosdk.rtc.android.kotlin.core.ui.components.ParticipantTile
import live.videosdk.rtc.android.kotlin.core.ui.theme.RedMd400
import live.videosdk.rtc.android.kotlin.core.ui.theme.VideoSDKTheme
import live.videosdk.rtc.android.kotlin.core.ui.theme.White
import live.videosdk.rtc.android.kotlin.feature.groupcall.presentation.components.ChatBottomSheet
import live.videosdk.rtc.android.kotlin.feature.groupcall.presentation.components.HandRaiseIndicator
import live.videosdk.rtc.android.listeners.ParticipantEventListener
import org.webrtc.VideoTrack

/**
 * Enhanced Group Call Screen with Screen Sharing, Chat, Hand Raise, and Audio Device Selection
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupCallScreen(
    token: String,
    meetingId: String,
    initialMicEnabled: Boolean,
    initialWebcamEnabled: Boolean,
    participantName: String,
    onLeaveCall: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GroupCallViewModel = viewModel()
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
    
    // Screen share permission handling
    val screenShareLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK && result.data != null) {
            viewModel.toggleScreenShare(result.data)
        }
        viewModel.screenSharePermissionHandled()
    }
    
    // Monitor for screen share permission requests
    LaunchedEffect(uiState.needsScreenSharePermission) {
        if (uiState.needsScreenSharePermission) {
            val mediaProjectionManager = context.getSystemService(android.content.Context.MEDIA_PROJECTION_SERVICE) 
                as android.media.projection.MediaProjectionManager
            screenShareLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
        }
    }

    VideoSDKTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(meetingId, fontWeight = FontWeight.Bold)
                            IconButton(onClick = {
                                val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
                                val clip = android.content.ClipData.newPlainText("Meeting ID", meetingId)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Default.ContentCopy, "Copy", tint = White)
                            }
                        }
                    },
                    actions = {
                        // Hand raise indicator
                        if (uiState.raisedHands.isNotEmpty()) {
                            HandRaiseIndicator(
                                raisedHands = uiState.raisedHands,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                        
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
                    // Main content area
                    if (uiState.presenterId != null) {
                        // Screen share view
                        ScreenShareView(
                            presenterId = uiState.presenterId,
                            participants = uiState.participants,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        // Participant grid
                        ParticipantGrid(
                            participants = uiState.participants,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Bottom controls
                    BottomControls(
                        micEnabled = uiState.micEnabled,
                        webcamEnabled = uiState.webcamEnabled,
                        isScreenSharing = uiState.isScreenSharing,
                        isHandRaised = uiState.isHandRaised,
                        unreadMessages = uiState.chatMessages.size,
                        onToggleMic = { viewModel.toggleMic() },
                        onToggleWebcam = { viewModel.toggleWebcam() },
                        onToggleScreenShare = { viewModel.toggleScreenShare() },
                        onToggleHandRaise = { viewModel.toggleHandRaise() },
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

@Composable
fun ParticipantGrid(
    participants: List<live.videosdk.rtc.android.Participant>,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.padding(8.dp),
        contentPadding = PaddingValues(4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(participants) { participant ->
            var videoTrack by remember { mutableStateOf<VideoTrack?>(null) }
            
            LaunchedEffect(participant) {
                participant.streams.values.find { it.kind == "video" }?.let {
                    videoTrack = it.track as VideoTrack
                }
            }

            DisposableEffect(participant) {
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
                participant.addEventListener(listener)
                onDispose { participant.removeEventListener(listener) }
            }

            ParticipantTile(
                participant = participant,
                videoTrack = videoTrack,
                isLocal = false
            )
        }
    }
}

@Composable
fun ScreenShareView(
    presenterId: String?,
    participants: List<live.videosdk.rtc.android.Participant>,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth()) {
        // Show screen share video
        // TODO: Implement screen share video view
        Text(
            text = "${participants.find { it.id == presenterId }?.displayName ?: "Someone"} is presenting",
            color = White,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomControls(
    micEnabled: Boolean,
    webcamEnabled: Boolean,
    isScreenSharing: Boolean,
    isHandRaised: Boolean,
    unreadMessages: Int,
    onToggleMic: () -> Unit,
    onToggleWebcam: () -> Unit,
    onToggleScreenShare: () -> Unit,
    onToggleHandRaise: () -> Unit,
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

        // Screen Share
        ControlButton(
            icon = if (isScreenSharing) Icons.Rounded.StopScreenShare else Icons.Rounded.ScreenShare,
            backgroundColor = if (isScreenSharing) Color.Green else Color.DarkGray,
            iconColor = White,
            onClick = onToggleScreenShare
        )

        // Hand Raise
        ControlButton(
            icon = Icons.Default.PanTool,
            backgroundColor = if (isHandRaised) Color.Yellow else Color.DarkGray,
            iconColor = if (isHandRaised) Color.Black else White,
            onClick = onToggleHandRaise
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
