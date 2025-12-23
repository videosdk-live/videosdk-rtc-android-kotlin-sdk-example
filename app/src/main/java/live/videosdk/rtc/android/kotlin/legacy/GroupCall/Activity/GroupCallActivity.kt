package live.videosdk.rtc.android.kotlin.legacy.GroupCall.Activity

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
import live.videosdk.rtc.android.kotlin.feature.groupcall.presentation.GroupCallScreen

class GroupCallActivity : ComponentActivity() {

    private var token: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Extract Intent Data
        token = intent.getStringExtra("token") ?: ""
        val meetingId = intent.getStringExtra("meetingId") ?: ""
        val micEnabled = intent.getBooleanExtra("micEnabled", true)
        val webcamEnabled = intent.getBooleanExtra("webcamEnabled", true)
        val localParticipantName = intent.getStringExtra("participantName") ?: "John Doe"

        setContent {
            GroupCallScreen(
                token = token!!,
                meetingId = "remq-6gbe-dnvy",
                initialMicEnabled = micEnabled,
                initialWebcamEnabled = webcamEnabled,
                participantName = localParticipantName,
                onLeaveCall = {
                    leaveMeeting()
                }
            )
        }

        startService(Intent(applicationContext, ForegroundService::class.java).apply {
            Intent.setAction = ForegroundService.ACTION_START
        })
    }

    private fun leaveMeeting() {
        startService(Intent(applicationContext, ForegroundService::class.java).apply {
            Intent.setAction = ForegroundService.ACTION_STOP
        })
        val intents = Intent(this, CreateOrJoinActivity::class.java)
        intents.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK
                    or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
        )
        startActivity(intents)
        finish()
    }

    fun getTouchListener(): View.OnTouchListener {
        return View.OnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                v.performClick()
            }
            true
        }
    }
}
