package live.videosdk.rtc.android.kotlin.feature.createjoin.presentation

import android.Manifest
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import live.videosdk.rtc.android.kotlin.core.ui.theme.VideoSDKTheme
import live.videosdk.rtc.android.kotlin.core.ui.theme.ColorPrimary
import live.videosdk.rtc.android.kotlin.core.ui.theme.White
import live.videosdk.rtc.android.kotlin.feature.createjoin.presentation.components.CameraPreviewCard
import live.videosdk.rtc.android.kotlin.feature.createjoin.presentation.components.CreateOrJoinOptions

/**
 * Main CreateOrJoin screen
 */
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CreateOrJoinScreen(
    onNavigateToGroupCall: (String, Boolean, Boolean, String) -> Unit,
    onNavigateToOneToOneCall: (String, Boolean, Boolean, String) -> Unit,
    onNavigateToHlsViewer: () -> Unit,
    viewModel: CreateOrJoinViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    
    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var showAudioDeviceSheet by remember { mutableStateOf(false) }

    // Permission handling
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    )

    LaunchedEffect(permissionsState.allPermissionsGranted) {
        if (permissionsState.allPermissionsGranted) {
            viewModel.onPermissionsGranted()
            viewModel.toggleWebcam(context)
        }
    }

    VideoSDKTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("VideoSDK RTC") },
                    actions = {
                        IconButton(onClick = { viewModel.switchCamera(context) }) {
                            Icon(Icons.Rounded.Cameraswitch, "Switch Camera")
                        }
                        IconButton(onClick = { showAudioDeviceSheet = true }) {
                            val icon = when (uiState.selectedAudioDevice?.label) {
                                "BLUETOOTH" -> Icons.Default.Bluetooth
                                "WIRED_HEADSET" -> Icons.Default.Headphones
                                "SPEAKER_PHONE" -> Icons.Default.VolumeUp
                                else -> Icons.Default.Call
                            }
                            Icon(icon, "Audio Device")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                // Camera Preview
                CameraPreviewCard(
                    videoTrack = uiState.videoTrack,
                    micEnabled = uiState.micEnabled,
                    webcamEnabled = uiState.webcamEnabled,
                    onToggleMic = { viewModel.toggleMic() },
                    onToggleWebcam = { viewModel.toggleWebcam(context) }
                )

                Spacer(modifier = Modifier.weight(1f))

                // HLS Viewer Demo Button
                Button(
                    onClick = onNavigateToHlsViewer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ColorPrimary,
                        contentColor = White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("HLS Viewer Demo", color = White)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Create or Join buttons
                CreateOrJoinOptions(
                    onCreateMeeting = { showCreateDialog = true },
                    onJoinMeeting = { showJoinDialog = true }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Create Meeting Dialog
        if (showCreateDialog) {
            CreateMeetingDialog(
                onDismiss = { showCreateDialog = false },
                onCreate = { meetingId, participantName, isGroupCall ->
                    showCreateDialog = false
                    if (isGroupCall) {
                        onNavigateToGroupCall(
                            meetingId,
                            uiState.micEnabled,
                            uiState.webcamEnabled,
                            participantName
                        )
                    } else {
                        onNavigateToOneToOneCall(
                            meetingId,
                            uiState.micEnabled,
                            uiState.webcamEnabled,
                            participantName
                        )
                    }
                }
            )
        }

        // Join Meeting Dialog
        if (showJoinDialog) {
            JoinMeetingDialog(
                onDismiss = { showJoinDialog = false },
                onJoin = { meetingId, participantName, isGroupCall ->
                    showJoinDialog = false
                    if (isGroupCall) {
                        onNavigateToGroupCall(
                            meetingId,
                            uiState.micEnabled,
                            uiState.webcamEnabled,
                            participantName
                        )
                    } else {
                        onNavigateToOneToOneCall(
                            meetingId,
                            uiState.micEnabled,
                            uiState.webcamEnabled,
                            participantName
                        )
                    }
                }
            )
        }

        // Audio Device Bottom Sheet
        if (showAudioDeviceSheet) {
            AudioDeviceBottomSheet(
                devices = viewModel.getAudioDevices(),
                selectedDevice = uiState.selectedAudioDevice,
                onDeviceSelected = { device ->
                    viewModel.selectAudioDevice(device)
                    showAudioDeviceSheet = false
                    Toast.makeText(context, "Selected ${device.label}", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { showAudioDeviceSheet = false }
            )
        }

        // Request permissions if not granted
        if (!permissionsState.allPermissionsGranted) {
            LaunchedEffect(Unit) {
                permissionsState.launchMultiplePermissionRequest()
            }
        }
    }
}

@Composable
fun CreateMeetingDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, Boolean) -> Unit
) {
    var meetingId by remember { mutableStateOf("remq-6gbe-dnvy") }
    var participantName by remember { mutableStateOf("DC") }
    var isGroupCall by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Meeting") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = meetingId,
                    onValueChange = { meetingId = it },
                    label = { Text("Meeting ID") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = participantName,
                    onValueChange = { participantName = it },
                    label = { Text("Your Name") },
                    singleLine = true
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isGroupCall,
                        onCheckedChange = { isGroupCall = it }
                    )
                    Text("Group Call")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (meetingId.isNotBlank() && participantName.isNotBlank()) {
                        onCreate(meetingId, participantName, isGroupCall)
                    }
                }
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun JoinMeetingDialog(
    onDismiss: () -> Unit,
    onJoin: (String, String, Boolean) -> Unit
) {
    var meetingId by remember { mutableStateOf("") }
    var participantName by remember { mutableStateOf("") }
    var isGroupCall by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Join Meeting") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = meetingId,
                    onValueChange = { meetingId = it },
                    label = { Text("Meeting ID") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = participantName,
                    onValueChange = { participantName = it },
                    label = { Text("Your Name") },
                    singleLine = true
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isGroupCall,
                        onCheckedChange = { isGroupCall = it }
                    )
                    Text("Group Call")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (meetingId.isNotBlank() && participantName.isNotBlank()) {
                        onJoin(meetingId, participantName, isGroupCall)
                    }
                }
            ) {
                Text("Join")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
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
                    modifier = Modifier.fillMaxWidth(),
                    colors = ListItemDefaults.colors(
                        containerColor = if (device == selectedDevice) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ),
                    tonalElevation = if (device == selectedDevice) 4.dp else 0.dp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
