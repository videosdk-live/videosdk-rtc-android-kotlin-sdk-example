package live.videosdk.rtc.android.kotlin.GroupCall.Activity

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import live.videosdk.rtc.android.CustomStreamTrack
import live.videosdk.rtc.android.Meeting
import live.videosdk.rtc.android.Participant
import live.videosdk.rtc.android.Stream
import live.videosdk.rtc.android.VideoSDK
import live.videosdk.rtc.android.VideoView
import live.videosdk.rtc.android.kotlin.Common.Activity.CreateOrJoinActivity
import live.videosdk.rtc.android.kotlin.Common.Service.ForegroundService
import live.videosdk.rtc.android.listeners.MeetingEventListener
import live.videosdk.rtc.android.listeners.ParticipantEventListener
import org.webrtc.VideoTrack
import java.util.*
import androidx.compose.runtime.key

class GroupCallActivity : ComponentActivity() {

    private var meeting: Meeting? = null
    private var token: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Extract Intent Data
        token = intent.getStringExtra("token")
        val meetingId = intent.getStringExtra("meetingId") ?: ""
        val micEnabled = intent.getBooleanExtra("micEnabled", true)
        val webcamEnabled = intent.getBooleanExtra("webcamEnabled", true)
        val localParticipantName = intent.getStringExtra("participantName") ?: "John Doe"

        // Initialize VideoSDK
        VideoSDK.config(token)

        // Create Custom Tracks
        val customTracks: MutableMap<String, CustomStreamTrack> = HashMap()
        val videoCustomTrack = VideoSDK.createCameraVideoTrack(
            "h720p_w960p", "front", CustomStreamTrack.VideoMode.TEXT, true, this, VideoSDK.getSelectedVideoDevice()
        )
        customTracks["video"] = videoCustomTrack
        val audioCustomTrack = VideoSDK.createAudioTrack("high_quality", this)
        customTracks["mic"] = audioCustomTrack

        // Initialize Meeting
        meeting = VideoSDK.initMeeting(
            this, "remq-6gbe-dnvy", localParticipantName,
            micEnabled, webcamEnabled, null, null, true, customTracks, null
        )

        setContent {
            MaterialTheme {
                GroupCallScreen(
                    meeting = meeting!!,
                    meetingId = meetingId,
                    initialMicEnabled = micEnabled,
                    initialWebcamEnabled = webcamEnabled,
                    onLeaveCall = {
                        leaveMeeting()
                    }
                )
            }
        }

        // Join the meeting
        meeting!!.join()

        startService(Intent(applicationContext, ForegroundService::class.java).apply {
            action = ForegroundService.ACTION_START
        })
    }

    private fun leaveMeeting() {
        meeting?.leave()
        startService(Intent(applicationContext, ForegroundService::class.java).apply {
            action = ForegroundService.ACTION_STOP
        })
        val intents = Intent(this, CreateOrJoinActivity::class.java)
        intents.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK
                    or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
        )
        startActivity(intents)
        finish()
    }

    override fun onDestroy() {
        meeting?.leave()
        super.onDestroy()
    }

    fun getTouchListener(): View.OnTouchListener {
        return View.OnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                // Toggle your meeting controls (toolbar/bottom bar) visibility here
                // Example:
                // if (controlsLayout.visibility == View.VISIBLE) {
                //     hideControls()
                // } else {
                //     showControls()
                // }

                // If you don't have logic yet, just return false
                v.performClick()
            }
            true
        }
    }
}

@Composable
fun GroupCallScreen(
    meeting: Meeting,
    meetingId: String,
    initialMicEnabled: Boolean,
    initialWebcamEnabled: Boolean,
    onLeaveCall: () -> Unit
) {
    val context = LocalContext.current

    // State
    var micEnabled by remember { mutableStateOf(initialMicEnabled) }
    var webcamEnabled by remember { mutableStateOf(initialWebcamEnabled) }
    // We use a list to store participants for the grid
    var participants by remember { mutableStateOf(meeting.participants.values.toList()) }
    var presenterId by remember { mutableStateOf<String?>(null) }
    
    // Listeners
    DisposableEffect(Unit) {
        val meetingEventListener = object : MeetingEventListener() {
            override fun onMeetingJoined() {
                participants = meeting.participants.values.toList()
            }

            override fun onParticipantJoined(participant: Participant) {
                participants = meeting.participants.values.toList()
            }

            override fun onParticipantLeft(participant: Participant) {
                participants = meeting.participants.values.toList()
            }

            override fun onPresenterChanged(participantId: String?) {
                presenterId = participantId
            }

            override fun onMeetingLeft() {
                // Handled by activity usually
            }
        }
        meeting.addEventListener(meetingEventListener)
        onDispose {
            meeting.removeEventListener(meetingEventListener)
        }
    }

    // UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF212121))
    ) {
        Column(Modifier.fillMaxSize()) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = meetingId, color = Color.White, fontWeight = FontWeight.Bold)
                    IconButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Meeting ID", meetingId)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.White)
                    }
                }

                IconButton(onClick = { meeting.changeWebcam() }) {
                    Icon(Icons.Rounded.Cameraswitch, contentDescription = "Switch Camera", tint = Color.White)
                }
            }

            // Main Content (Grid or Screen Share)
            if (presenterId != null) {
                // Screen Share View
                Box(modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()) {
                    AndroidView(
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                setMirror(false)
                            }
                        },
                        update = { view ->
                            val participant = meeting.participants[presenterId]
                            val shareStream = participant?.streams?.values?.find { it.kind == "share" }
                            if (shareStream != null) {
                                view.addTrack(shareStream.track as VideoTrack)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    // Show presenter name
                    val presenterName = meeting.participants[presenterId]?.displayName ?: "Unknown"
                    Text(
                        text = "$presenterName is presenting",
                        color = Color.White,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).background(Color.Black.copy(alpha=0.6f)).padding(8.dp)
                    )
                }
            } else {
                // Grid View
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .weight(1f)
                        .padding(8.dp),
                    contentPadding = PaddingValues(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Local Participant
                    item {
                        ParticipantTile(participant = meeting.localParticipant, isLocal = true)
                    }
                    // Remote Participants
                    items(participants) { participant ->
                        ParticipantTile(participant = participant, isLocal = false)
                    }
                }
            }

            // Bottom Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Chat
                ControlIconButton(
                    icon = Icons.Rounded.Chat,
                    onClick = { /* Open Chat */ }
                )

                // Mic
                ControlIconButton(
                    icon = if (micEnabled) Icons.Rounded.Mic else Icons.Rounded.MicOff,
                    backgroundColor = if (micEnabled) Color.White else Color.Red,
                    iconColor = if (micEnabled) Color.Black else Color.White,
                    onClick = {
                        if (micEnabled) meeting.muteMic() else meeting.unmuteMic()
                        micEnabled = !micEnabled
                    }
                )

                // Leave
                ControlIconButton(
                    icon = Icons.Rounded.CallEnd,
                    backgroundColor = Color.Red,
                    iconColor = Color.White,
                    size = 70.dp,
                    onClick = onLeaveCall
                )

                // Webcam
                ControlIconButton(
                    icon = if (webcamEnabled) Icons.Rounded.Videocam else Icons.Rounded.VideocamOff,
                    backgroundColor = if (webcamEnabled) Color.White else Color.Red,
                    iconColor = if (webcamEnabled) Color.Black else Color.White,
                    onClick = {
                        if (webcamEnabled) meeting.disableWebcam() else meeting.enableWebcam()
                        webcamEnabled = !webcamEnabled
                    }
                )

                // More
                ControlIconButton(
                    icon = Icons.Rounded.MoreVert,
                    onClick = { /* More Options */ }
                )
            }
        }
    }
}

@Composable
fun ParticipantTile(participant: Participant, isLocal: Boolean) {
    var videoTrack by remember { mutableStateOf<VideoTrack?>(null) }
    
    // Initial check
    LaunchedEffect(participant) {
        participant.streams.values.find { it.kind == "video" }?.let {
            videoTrack = it.track as VideoTrack
        }
    }

    DisposableEffect(participant) {
        val listener = object : ParticipantEventListener() {
            override fun onStreamEnabled(stream: Stream) {
                if (stream.kind == "video") {
                    videoTrack = stream.track as VideoTrack
                }
            }

            override fun onStreamDisabled(stream: Stream) {
                if (stream.kind == "video") {
                    videoTrack = null
                }
            }
        }
        participant.addEventListener(listener)
        onDispose { participant.removeEventListener(listener) }
    }

    Card(
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .height(200.dp)
            .fillMaxWidth()
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Gray)) {
            if (videoTrack != null) {
                key(videoTrack) {
                    AndroidView(
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                setMirror(isLocal)
                                addTrack(videoTrack)
                            }
                        },
                        update = { view ->
                             // Track already added in factory or view handles it
                            view.addTrack(videoTrack)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                Text(
                    text = participant.displayName.take(1).uppercase(),
                    fontSize = 40.sp,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Text(
                text = if (isLocal) "You" else participant.displayName,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(4.dp),
                color = Color.White,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun ControlIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    backgroundColor: Color = Color.DarkGray,
    iconColor: Color = Color.White,
    size: androidx.compose.ui.unit.Dp = 50.dp
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = backgroundColor,
        modifier = Modifier.size(size)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = null, tint = iconColor)
        }
    }
}
