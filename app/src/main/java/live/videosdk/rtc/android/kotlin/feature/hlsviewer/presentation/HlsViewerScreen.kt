package live.videosdk.rtc.android.kotlin.feature.hlsviewer.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import live.videosdk.rtc.android.kotlin.core.ui.theme.*
import live.videosdk.rtc.android.kotlin.feature.hlsviewer.presentation.components.ComprehensiveHlsStatsPanel
import live.videosdk.rtc.android.kotlin.feature.hlsviewer.presentation.components.QualitySelectionDialog
import live.videosdk.rtc.android.kotlin.feature.hlsviewer.presentation.components.StatsComparisonPanel

/**
 * HLS Viewer Screen with meeting integration
 * Supports Host mode (start/stop HLS) and Viewer mode (watch with stats)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HlsViewerScreen(
    token: String,
    onNavigateBack: () -> Unit,
    viewModel: HlsStatsViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val customStats by viewModel.customUiState.collectAsState()
    val hmsStats by viewModel.hmsUiState.collectAsState()
    
    var meetingId by remember { mutableStateOf("remq-6gbe-dnvy") }
    var participantName by remember { mutableStateOf("JetRTC") }
    var selectedMode by remember { mutableStateOf(0) } // 0 = Host, 1 = Viewer
    var showQualityDialog by remember { mutableStateOf(false) }
    
    // Create ExoPlayer instance
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().also { player ->
            viewModel.initializePlayer(player)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }
    
    // Quality selection dialog
    if (showQualityDialog) {
        QualitySelectionDialog(
            player = exoPlayer,
            onDismiss = { showQualityDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "HLS Streaming",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Quality selector button (only show when playing)
                    if (uiState.isPlaying || uiState.playbackUrl.isNotEmpty()) {
                        IconButton(onClick = { showQualityDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Quality Settings",
                                tint = White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ColorPrimary,
                    titleContentColor = White,
                    navigationIconContentColor = White
                )
            )
        },
        containerColor = Black
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            if (!uiState.isMeetingJoined) {
                // Meeting Join UI
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = GreyMd700)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Join Meeting",
                            color = White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Meeting ID Input
                        OutlinedTextField(
                            value = meetingId,
                            onValueChange = { meetingId = it },
                            label = { Text("Meeting ID") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = White,
                                unfocusedTextColor = White, 
                                focusedBorderColor = ColorAccent,
                                unfocusedBorderColor = White.copy(alpha = 0.5f),
                                focusedLabelColor = ColorAccent,
                                unfocusedLabelColor = White.copy(alpha = 0.7f),
                                cursorColor = ColorAccent
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Participant Name Input
                        OutlinedTextField(
                            value = participantName,
                            onValueChange = { participantName = it },
                            label = { Text("Your Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = White,
                                unfocusedTextColor = White,
                                focusedBorderColor = ColorAccent,
                                unfocusedBorderColor = White.copy(alpha = 0.5f),
                                focusedLabelColor = ColorAccent,
                                unfocusedLabelColor = White.copy(alpha = 0.7f),
                                cursorColor = ColorAccent
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Mode Selection
                        Text(
                            text = "Select Mode",
                            color = White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Host Mode
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .selectable(
                                        selected = selectedMode == 0,
                                        onClick = {
                                            selectedMode = 0
                                            viewModel.setMode(true)
                                        }
                                    ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedMode == 0) ColorAccent else GreyMd1000
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.Videocam,
                                        contentDescription = null,
                                        tint = White
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Host", color = White, fontWeight = FontWeight.Bold)
                                    Text("Start HLS", color = White.copy(0.7f), fontSize = 11.sp)
                                }
                            }

                            // Viewer Mode
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .selectable(
                                        selected = selectedMode == 1,
                                        onClick = {
                                            selectedMode = 1
                                            viewModel.setMode(false)
                                        }
                                    ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedMode == 1) ColorAccent else GreyMd1000
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = White
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Viewer", color = White, fontWeight = FontWeight.Bold)
                                    Text("Watch HLS", color = White.copy(0.7f), fontSize = 11.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Join Button
                        Button(
                            onClick = {
                                viewModel.joinMeeting(context, token, meetingId, participantName)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ColorPrimary
                            ),
                            enabled = meetingId.isNotBlank() && participantName.isNotBlank()
                        ) {
                            Icon(Icons.Default.Login, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Join Meeting")
                        }
                    }
                }
            } else {
                // Meeting Joined - Show Controls
                
                // HLS Status Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = GreyMd700)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = ColorAccent
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Meeting: ${uiState.meetingId}",
                                    color = White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Mode: ${if (uiState.isHost) "Host" else "Viewer"}",
                                    color = White.copy(0.8f),
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "HLS: ${uiState.hlsState}",
                                    color = when (uiState.hlsState) {
                                        "HLS_PLAYABLE" -> ColorGreen
                                        "HLS_STARTING", "HLS_STOPPING" -> ColorYellow
                                        else -> White.copy(0.6f)
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (uiState.isHost) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Start HLS Button
                                Button(
                                    onClick = { viewModel.startHls() },
                                    modifier = Modifier.weight(1f),
                                    enabled = uiState.hlsState == "IDLE" || uiState.hlsState == "HLS_STOPPED",
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ColorGreen
                                    )
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Start HLS")
                                }

                                // Stop HLS Button
                                Button(
                                    onClick = { viewModel.stopHls() },
                                    modifier = Modifier.weight(1f),
                                    enabled = uiState.hlsState == "HLS_PLAYABLE",
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = RedMd400
                                    )
                                ) {
                                    Icon(Icons.Default.Stop, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Stop HLS")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Leave Meeting Button
                        TextButton(
                            onClick = { viewModel.leaveMeeting() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = null, tint = RedMd400)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Leave Meeting", color = RedMd400)
                        }
                    }
                }

                // Video Player (when HLS is playable)
                if (uiState.hlsState == "HLS_PLAYABLE") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .height(240.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = GreyMd700)
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                PlayerView(ctx).apply {
                                    player = exoPlayer
                                    useController = true
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Stats Panel - Side-by-side comparison
                    StatsComparisonPanel(
                        customStats = customStats,
                        hmsStats = hmsStats
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// Color definitions for status indicators
private val ColorGreen = androidx.compose.ui.graphics.Color(0xFF4CAF50)
private val ColorYellow = androidx.compose.ui.graphics.Color(0xFFFFC107)
