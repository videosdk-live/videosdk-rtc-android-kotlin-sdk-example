package live.videosdk.rtc.android.kotlin.legacy.OneToOneCall

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.VideocamOff
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
import live.videosdk.rtc.android.listeners.MeetingEventListener
import live.videosdk.rtc.android.listeners.ParticipantEventListener
import org.webrtc.VideoTrack
import java.util.*
import androidx.compose.runtime.key
import androidx.compose.ui.unit.Dp

class OneToOneCallActivity : ComponentActivity() {

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
                OneToOneCallScreen(
                    meeting = meeting!!,
                    meetingId = meetingId,
                    initialMicEnabled = micEnabled,
                    initialWebcamEnabled = webcamEnabled,
                    onLeaveCall = {
                        meeting?.leave()
                        finish()
                    }
                )
            }
        }

        // Join the meeting automatically
        meeting!!.join()
    }

    override fun onDestroy() {
        meeting?.leave()
        super.onDestroy()
    }
}

@Composable
fun OneToOneCallScreen(
    meeting: Meeting,
    meetingId: String,
    initialMicEnabled: Boolean,
    initialWebcamEnabled: Boolean,
    onLeaveCall: () -> Unit
) {
    val context = LocalContext.current

    // State Variables
    var isMicEnabled by remember { mutableStateOf(initialMicEnabled) }
    var isWebcamEnabled by remember { mutableStateOf(initialWebcamEnabled) }
    var localTrack by remember { mutableStateOf<VideoTrack?>(null) }
    var participantTrack by remember { mutableStateOf<VideoTrack?>(null) }
    var participantName by remember { mutableStateOf("Waiting...") }
    var isLocalScreenShare by remember { mutableStateOf(false) }

    // Join Event Listener
    DisposableEffect(Unit) {
        val meetingEventListener = object : MeetingEventListener() {
            override fun onMeetingJoined() {
                // Setup Local Track
                val localParticipant = meeting.localParticipant
                localParticipant.addEventListener(object : ParticipantEventListener() {
                    override fun onStreamEnabled(stream: Stream) { // Adjust import based on SDK
                        if (stream?.kind == "video") {
                            localTrack = stream.track as VideoTrack
                        }
                    }
                })
            }

            override fun onParticipantJoined(participant: Participant) {
                participantName = participant.displayName
                participant.addEventListener(object : ParticipantEventListener() {
                    override fun onStreamEnabled(stream: Stream) {
                        if (stream?.kind == "video") {
                            participantTrack = stream.track as VideoTrack
                        }
                    }
                    override fun onStreamDisabled(stream: Stream) {
                        if (stream?.kind == "video") {
                            participantTrack = null
                        }
                    }
                })
            }

            override fun onParticipantLeft(participant: Participant) {
                participantTrack = null
                participantName = "Waiting..."
                Toast.makeText(context, "${participant.displayName} left", Toast.LENGTH_SHORT).show()
            }
        }

        meeting.addEventListener(meetingEventListener)
        onDispose {
            meeting.removeEventListener(meetingEventListener)
        }
    }

    // UI Layout
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF212121)) // Dark background
    ) {
        // 1. Remote Participant Video (Full Screen)
        if (participantTrack != null) {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        setMirror(false)
                        addTrack(participantTrack)
                    }
                },
                update = { view ->
                    // Update track if changed
                    view.addTrack(participantTrack)
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Placeholder when no participant
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = participantName.firstOrNull()?.toString() ?: "?",
                        fontSize = 50.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .background(Color.Gray, CircleShape)
                            .padding(30.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "$participantName", color = Color.White)
                }
            }
        }

        // 2. Top Bar (Meeting ID)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = meetingId, color = Color.White, fontWeight = FontWeight.Bold)
                IconButton(onClick = { /* Copy ID Logic */ }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.White)
                }
            }

            // Switch Camera Button
            IconButton(onClick = { meeting.changeWebcam() }) {
                Icon(Icons.Rounded.Cameraswitch, contentDescription = "Switch Camera", tint = Color.White)
            }
        }

        // 3. Local Participant Video (Floating Picture-in-Picture)
        if (isWebcamEnabled) {
            Card(
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .width(100.dp)
                    .height(150.dp)
                    .align(Alignment.TopEnd)
                    .padding(top = 70.dp, end = 16.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                key(localTrack){
                    AndroidView(
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                setMirror(true)
                                // We need to fetch local track from meeting.localParticipant
                                setZOrderMediaOverlay(true)
                                if (localTrack != null) {
                                    addTrack(localTrack)
                                }
                            }
                        },
                        update = { view ->
                            // Usually local track is handled internally by SDK or via event listener
                            if (localTrack != null) {
                                view.addTrack(localTrack)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // 4. Bottom Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 30.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Chat
            ControlIconButton(
                icon = Icons.Rounded.Chat,
                onClick = { /* Open Chat Modal */ }
            )

            // Mic Toggle
            ControlIconButton(
                icon = if (isMicEnabled) Icons.Rounded.Mic else Icons.Rounded.MicOff,
                backgroundColor = if (isMicEnabled) Color.White else Color.Red,
                iconColor = if (isMicEnabled) Color.Black else Color.White,
                onClick = {
                    if (isMicEnabled) meeting.muteMic() else meeting.unmuteMic()
                    isMicEnabled = !isMicEnabled
                }
            )

            // Leave Call
            ControlIconButton(
                icon = Icons.Rounded.CallEnd,
                backgroundColor = Color.Red,
                iconColor = Color.White,
                size = 70.dp,
                onClick = onLeaveCall
            )

            // Webcam Toggle
            ControlIconButton(
                icon = if (isWebcamEnabled) Icons.Rounded.Videocam else Icons.Rounded.VideocamOff,
                backgroundColor = if (isWebcamEnabled) Color.White else Color.Red,
                iconColor = if (isWebcamEnabled) Color.Black else Color.White,
                onClick = {
                    if (isWebcamEnabled) meeting.disableWebcam() else meeting.enableWebcam()
                    isWebcamEnabled = !isWebcamEnabled
                }
            )

            // More Options
            ControlIconButton(
                icon = Icons.Rounded.MoreVert,
                onClick = { /* Open Bottom Sheet */ }
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
    size: Dp = 50.dp
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
