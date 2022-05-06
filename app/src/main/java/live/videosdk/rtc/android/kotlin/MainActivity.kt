package live.videosdk.rtc.android.kotlin

import android.Manifest
import android.annotation.TargetApi
import android.content.ClipData
import android.content.ClipboardManager
import android.content.DialogInterface
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.media.projection.MediaProjectionManager
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.nabinbhandari.android.permissions.PermissionHandler
import com.nabinbhandari.android.permissions.Permissions
import live.videosdk.rtc.android.Meeting
import live.videosdk.rtc.android.Participant
import live.videosdk.rtc.android.Stream
import live.videosdk.rtc.android.VideoSDK
import live.videosdk.rtc.android.lib.AppRTCAudioManager
import live.videosdk.rtc.android.lib.AppRTCAudioManager.AudioManagerEvents
import live.videosdk.rtc.android.lib.PeerConnectionUtils
import live.videosdk.rtc.android.lib.PubSubMessage
import live.videosdk.rtc.android.listeners.*
import live.videosdk.rtc.android.model.LivestreamOutput
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack
import java.util.*

class MainActivity() : AppCompatActivity() {


    private var svrShare: SurfaceViewRenderer? = null
    private var btnMic: FloatingActionButton? = null
    private var btnWebcam: FloatingActionButton? = null
    private var micEnabled = true
    private var webcamEnabled = true
    private var recording = false
    private var livestreaming = false
    private var localScreenShare = false
    private var isNetworkAvailable = true
    private var btnScreenShare: FloatingActionButton? = null
    private val timer = Timer()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //
        btnScreenShare = findViewById(R.id.btnScreenShare)
        svrShare = findViewById(R.id.svrShare)
        svrShare!!.init(PeerConnectionUtils.getEglContext(), null)
        btnMic = findViewById(R.id.btnMic)
        btnWebcam = findViewById(R.id.btnWebcam)
        val token = intent.getStringExtra("token")
        val meetingId = intent.getStringExtra("meetingId")
        micEnabled = intent.getBooleanExtra("micEnabled", true)
        webcamEnabled = intent.getBooleanExtra("webcamEnabled", true)
        var participantName = intent.getStringExtra("paticipantName")
        if (participantName == null) {
            participantName = "John Doe"
        }

        //
        toggleMicIcon()
        toggleWebcamIcon()

        //
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.title = meetingId

        // pass the token generated from api server
        VideoSDK.config(token)

        // create a new meeting instance
        meeting = VideoSDK.initMeeting(
            this@MainActivity, meetingId, participantName,
            micEnabled, webcamEnabled
        )

        //
        (this.application as MainApplication).meeting = meeting
        meeting!!.addEventListener(meetingEventListener)

        //
        val rvParticipants = findViewById<RecyclerView>(R.id.rvParticipants)
        rvParticipants.layoutManager = GridLayoutManager(this, 2)
        rvParticipants.adapter = ParticipantAdapter(meeting!!)

        // Local participant listeners
        setLocalListeners()

        //
        checkPermissions()

        // Actions
        setActionListeners()

        setAudioDeviceListeners()

        (findViewById<View>(R.id.btnCopyContent) as ImageButton).setOnClickListener(
            View.OnClickListener { copyTextToClipboard(meetingId) })
        (findViewById<View>(R.id.btnAudioSelection) as ImageButton).setOnClickListener { showAudioInputDialog() }

        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                isNetworkAvailable = isNetworkAvailable()
                if (!isNetworkAvailable) {
                    runOnUiThread {
                        if (!isDestroyed) {
                            MaterialAlertDialogBuilder(this@MainActivity)
                                .setMessage("No Internet Connection")
                                .setCancelable(false)
                                .setPositiveButton(
                                    "Ok",
                                    DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                                        if (!isDestroyed) {
                                            meeting!!.leave()
                                        }
                                    })
                                .create().show()
                        }
                    }
                }
            }
        }, 0, 10000)
    }

    private fun isNetworkAvailable(): Boolean {
        val manager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = manager.activeNetworkInfo
        val isAvailable = networkInfo != null && networkInfo.isConnected
        if (!isAvailable) {
            Snackbar.make(
                findViewById(R.id.mainLayout), "No Internet Connection",
                Snackbar.LENGTH_LONG
            ).show()
        }
        return isAvailable
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    private fun toggleMicIcon() {
        if (micEnabled) {
            btnMic!!.setImageResource(R.drawable.ic_baseline_mic_24)
            btnMic!!.setColorFilter(Color.WHITE)
            btnMic!!.backgroundTintList =
                ColorStateList.valueOf(resources.getColor(R.color.colorPrimary))
        } else {
            btnMic!!.setImageResource(R.drawable.ic_baseline_mic_off_24)
            btnMic!!.setColorFilter(Color.BLACK)
            btnMic!!.backgroundTintList =
                ColorStateList.valueOf(resources.getColor(R.color.md_grey_300))
        }
    }

    private fun toggleWebcamIcon() {
        if (webcamEnabled) {
            btnWebcam!!.setImageResource(R.drawable.ic_baseline_videocam_24)
            btnWebcam!!.setColorFilter(Color.WHITE)
            btnWebcam!!.backgroundTintList =
                ColorStateList.valueOf(resources.getColor(R.color.colorPrimary))
        } else {
            btnWebcam!!.setImageResource(R.drawable.ic_baseline_videocam_off_24)
            btnWebcam!!.setColorFilter(Color.BLACK)
            btnWebcam!!.backgroundTintList =
                ColorStateList.valueOf(resources.getColor(R.color.md_grey_300))
        }
    }

    private val meetingEventListener: MeetingEventListener = object : MeetingEventListener() {
        override fun onMeetingJoined() {
            Log.d("#meeting", "onMeetingJoined()")

            // notify user of any new messages
            meeting!!.pubSub.subscribe("CHAT", object : PubSubMessageListener {
                override fun onMessageReceived(pubSubMessage: PubSubMessage) {
                    if (pubSubMessage.senderId != meeting!!.localParticipant.id) {
                        val parentLayout = findViewById<View>(android.R.id.content)
                        Snackbar.make(
                            parentLayout, (pubSubMessage.senderName + " says: " +
                                    pubSubMessage.message), Snackbar.LENGTH_SHORT
                        )
                            .setDuration(2000).show()
                    }
                }
            })

            //terminate meeting in 10 minutes
            Handler().postDelayed({
                if (!isDestroyed) MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle("Meeting Left")
                    .setMessage("Demo app limits meeting to 10 Minutes")
                    .setCancelable(false)
                    .setPositiveButton("Ok") { dialog: DialogInterface?, which: Int ->
                        if (!isDestroyed) meeting!!.leave()
                        Log.d("Auto Terminate", "run: Meeting Terminated")
                    }
                    .create().show()
            }, 600000)
        }

        override fun onMeetingLeft() {
            Log.d("#meeting", "onMeetingLeft()")
            meeting = null
            if (!isDestroyed) {
                val intents = Intent(this@MainActivity, CreateOrJoinActivity::class.java)
                intents.addFlags(
                    (Intent.FLAG_ACTIVITY_NEW_TASK
                            or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                )
                startActivity(intents)
                finish()
            }
        }

        override fun onParticipantJoined(participant: Participant) {
            Toast.makeText(
                this@MainActivity, participant.displayName + " joined",
                Toast.LENGTH_SHORT
            ).show()
        }

        override fun onParticipantLeft(participant: Participant) {
            Toast.makeText(
                this@MainActivity, participant.displayName + " left",
                Toast.LENGTH_SHORT
            ).show()
        }

        override fun onPresenterChanged(participantId: String?) {
            updatePresenter(participantId)
        }

        override fun onRecordingStarted() {
            recording = true
            (findViewById<View>(R.id.recordIcon)).visibility = View.VISIBLE
            Toast.makeText(
                this@MainActivity, "Recording started",
                Toast.LENGTH_SHORT
            ).show()
        }

        override fun onRecordingStopped() {
            recording = false
            (findViewById<View>(R.id.recordIcon)).visibility = View.GONE
            Toast.makeText(
                this@MainActivity, "Recording stopped",
                Toast.LENGTH_SHORT
            ).show()
        }

        override fun onLivestreamStarted() {
            livestreaming = true
            Toast.makeText(
                this@MainActivity, "Livestream started",
                Toast.LENGTH_SHORT
            ).show()
        }

        override fun onLivestreamStopped() {
            livestreaming = false
            Toast.makeText(
                this@MainActivity, "Livestream stopped",
                Toast.LENGTH_SHORT
            ).show()
        }

        override fun onMicRequested(participantId: String, listener: MicRequestListener) {
            showMicRequestDialog(listener)
        }

        override fun onWebcamRequested(participantId: String, listener: WebcamRequestListener) {
            showWebcamRequestDialog(listener)
        }

        override fun onExternalCallStarted() {
            Toast.makeText(this@MainActivity, "onExternalCallStarted", Toast.LENGTH_SHORT).show()
            Log.d("#meeting", "onExternalCallAnswered: User Answered a Call")
        }
    }

    @TargetApi(21)
    private fun askPermissionForScreenShare() {
        val mediaProjectionManager = application.getSystemService(
            MEDIA_PROJECTION_SERVICE
        ) as MediaProjectionManager
        startActivityForResult(
            mediaProjectionManager.createScreenCaptureIntent(), CAPTURE_PERMISSION_REQUEST_CODE
        )
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != CAPTURE_PERMISSION_REQUEST_CODE) return
        if (resultCode != RESULT_OK) {
            Toast.makeText(
                this@MainActivity,
                "You didn't give permission to capture the screen.",
                Toast.LENGTH_SHORT
            ).show()
            localScreenShare = false
            return
        }
        meeting!!.enableScreenShare(data)
        btnScreenShare!!.setImageResource(R.drawable.ic_outline_stop_screen_share_24)
    }

    private fun updatePresenter(participantId: String?) {
        if (participantId == null) {
            svrShare!!.clearImage()
            svrShare!!.visibility = View.GONE
            btnScreenShare!!.isEnabled = true
            return
        } else {
            btnScreenShare!!.isEnabled = (meeting!!.localParticipant.id == participantId)
        }

        // find participant
        val participant = meeting!!.participants[participantId] ?: return

        // find share stream in participant
        var shareStream: Stream? = null
        for (stream: Stream in participant.streams.values) {
            if ((stream.kind == "share")) {
                shareStream = stream
                break
            }
        }
        if (shareStream == null) return

        // display share video
        svrShare!!.visibility = View.VISIBLE
        svrShare!!.setZOrderMediaOverlay(true)
        svrShare!!.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
        val videoTrack = shareStream.track as VideoTrack
        videoTrack.addSink(svrShare)

        // listen for share stop event
        participant.addEventListener(object : ParticipantEventListener() {
            override fun onStreamDisabled(stream: Stream) {
                if ((stream.kind == "share")) {
                    val track: VideoTrack = stream.track as VideoTrack
                    track.removeSink(svrShare)
                    svrShare!!.clearImage()
                    svrShare!!.visibility = View.GONE
                    localScreenShare = false
                }
            }
        })
    }

    private val permissionHandler: PermissionHandler = object : PermissionHandler() {
        override fun onGranted() {
            if (meeting != null) meeting!!.join()
        }
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.INTERNET,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.READ_PHONE_STATE
        )
        val rationale = "Please provide permissions"
        val options =
            Permissions.Options().setRationaleDialogTitle("Info").setSettingsDialogTitle("Warning")
        Permissions.check(this, permissions, rationale, options, permissionHandler)
    }

    private fun setAudioDeviceListeners() {
        meeting!!.setAudioDeviceChangeListener(object : AudioManagerEvents {
            override fun onAudioDeviceChanged(
                selectedAudioDevice: AppRTCAudioManager.AudioDevice,
                availableAudioDevices: Set<AppRTCAudioManager.AudioDevice>
            ) {
                when (selectedAudioDevice) {
                    AppRTCAudioManager.AudioDevice.BLUETOOTH -> (findViewById<View>(R.id.btnAudioSelection) as ImageButton).setImageDrawable(
                        ContextCompat.getDrawable(
                            applicationContext, R.drawable.ic_baseline_bluetooth_audio_24
                        )
                    )
                    AppRTCAudioManager.AudioDevice.WIRED_HEADSET -> (findViewById<View>(R.id.btnAudioSelection) as ImageButton).setImageDrawable(
                        ContextCompat.getDrawable(
                            applicationContext, R.drawable.ic_baseline_headset_24
                        )
                    )
                    AppRTCAudioManager.AudioDevice.SPEAKER_PHONE -> (findViewById<View>(R.id.btnAudioSelection) as ImageButton).setImageDrawable(
                        ContextCompat.getDrawable(
                            applicationContext, R.drawable.ic_baseline_volume_up_24
                        )
                    )
                    AppRTCAudioManager.AudioDevice.EARPIECE -> (findViewById<View>(R.id.btnAudioSelection) as ImageButton).setImageDrawable(
                        ContextCompat.getDrawable(
                            applicationContext, R.drawable.ic_baseline_phone_in_talk_24
                        )
                    )
                }
            }
        })
    }

    private fun setLocalListeners() {
        meeting!!.localParticipant.addEventListener(object : ParticipantEventListener() {
            override fun onStreamEnabled(stream: Stream) {
                Log.d("TAG", "onStreamEnabled: " + stream.kind)
                if (stream.kind.equals("video", ignoreCase = true)) {
                    webcamEnabled = true
                    toggleWebcamIcon()
                } else if (stream.kind.equals("audio", ignoreCase = true)) {
                    micEnabled = true
                    toggleMicIcon()
                } else if (stream.kind.equals("share", ignoreCase = true)) {
                    // display share video
                    svrShare!!.visibility = View.VISIBLE
                    svrShare!!.setZOrderMediaOverlay(true)
                    svrShare!!.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
                    val videoTrack = stream.track as VideoTrack
                    videoTrack.addSink(svrShare)
                    //
                    localScreenShare = true
                }
            }

            override fun onStreamDisabled(stream: Stream) {
                when {
                    stream.kind.equals("video", ignoreCase = true) -> {
                        webcamEnabled = false
                        toggleWebcamIcon()
                    }
                    stream.kind.equals("audio", ignoreCase = true) -> {
                        micEnabled = false
                        toggleMicIcon()
                    }
                    stream.kind.equals("share", ignoreCase = true) -> {
                        val track: VideoTrack = stream.track as VideoTrack
                        track.removeSink(svrShare)
                        svrShare!!.clearImage()
                        svrShare!!.visibility = View.GONE
                        //
                        localScreenShare = false
                    }
                }
            }
        })
    }

    private fun copyTextToClipboard(text: String?) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Copied text", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this@MainActivity, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    private fun setActionListeners() {


        // Toggle mic
        btnMic!!.setOnClickListener { _: View? ->
            if (micEnabled) {
                meeting!!.muteMic()
            } else {
                meeting!!.unmuteMic()
            }
        }

        // Toggle webcam
        btnWebcam!!.setOnClickListener {
            if (webcamEnabled) {
                meeting!!.disableWebcam()
            } else {
                meeting!!.enableWebcam()
            }
        }

        // Leave meeting
        findViewById<View>(R.id.btnLeave).setOnClickListener { view: View? -> showLeaveOrEndDialog() }
        findViewById<View>(R.id.btnMore).setOnClickListener { v: View? -> showMoreOptionsDialog() }
        findViewById<View>(R.id.btnSwitchCameraMode).setOnClickListener { view: View? -> meeting!!.changeWebcam() }

        // Chat
        findViewById<View>(R.id.btnChat).setOnClickListener { view: View? ->
            val intent = Intent(this@MainActivity, ChatActivity::class.java)
            startActivity(intent)
        }

        //
        btnScreenShare!!.setOnClickListener { view: View? -> toggleScreenSharing() }
    }

    private fun toggleScreenSharing() {
        if (!localScreenShare) {
            askPermissionForScreenShare()
        } else {
            meeting!!.disableScreenShare()
            btnScreenShare!!.setImageResource(R.drawable.ic_outline_screen_share_24)
        }
        localScreenShare = !localScreenShare
    }

    private fun showLeaveOrEndDialog() {
        MaterialAlertDialogBuilder(this@MainActivity)
            .setTitle("Leave or End meeting")
            .setMessage("Leave from meeting or end the meeting for everyone ?")
            .setPositiveButton(
                "Leave"
            ) { dialog: DialogInterface?, which: Int -> meeting!!.leave() }
            .setNegativeButton("End") { dialog: DialogInterface?, which: Int -> meeting!!.end() }
            .show()
    }

    private fun showAudioInputDialog() {
        val mics = meeting!!.mics

        // Prepare list
        val items = arrayOfNulls<String>(mics.size)
        for (i in mics.indices) {
            items[i] = mics.toTypedArray()[i].toString()
        }
        MaterialAlertDialogBuilder(this@MainActivity)
            .setTitle(getString(R.string.audio_options))
            .setItems(items) { dialog: DialogInterface?, which: Int ->
                var audioDevice: AppRTCAudioManager.AudioDevice? = null
                when (items.get(which)) {
                    "BLUETOOTH" -> audioDevice = AppRTCAudioManager.AudioDevice.BLUETOOTH
                    "WIRED_HEADSET" -> audioDevice = AppRTCAudioManager.AudioDevice.WIRED_HEADSET
                    "SPEAKER_PHONE" -> audioDevice = AppRTCAudioManager.AudioDevice.SPEAKER_PHONE
                    "EARPIECE" -> audioDevice = AppRTCAudioManager.AudioDevice.EARPIECE
                }
                meeting!!.changeMic(audioDevice)
            }
            .show()
    }

    private fun showMoreOptionsDialog() {
        val items = arrayOf(
            if (recording) "Stop recording" else "Start recording",
            if (livestreaming) "Stop livestreaming" else "Start livestreaming"
        )
        MaterialAlertDialogBuilder(this@MainActivity)
            .setTitle(getString(R.string.more_options))
            .setItems(items) { dialog: DialogInterface?, which: Int ->
                when (which) {
                    0 -> {
                        toggleRecording()
                    }
                    1 -> {
                        toggleLivestreaming()
                    }
                }
            }
            .show()
    }

    private fun toggleRecording() {
        if (!recording) {
            meeting!!.startRecording(null)
        } else {
            meeting!!.stopRecording()
        }
    }

    private fun toggleLivestreaming() {
        if (!livestreaming) {
            if (YOUTUBE_RTMP_URL == null || YOUTUBE_RTMP_STREAM_KEY == null) {
                throw Error("RTMP url or stream key missing.")
            }
            val outputs: MutableList<LivestreamOutput> = ArrayList()
            outputs.add(LivestreamOutput(YOUTUBE_RTMP_URL, YOUTUBE_RTMP_STREAM_KEY))
            meeting!!.startLivestream(outputs)
        } else {
            meeting!!.stopLivestream()
        }
    }

    private fun showMicRequestDialog(listener: MicRequestListener) {
        MaterialAlertDialogBuilder(this@MainActivity)
            .setTitle("Mic requested")
            .setMessage("Host is asking you to unmute your mic, do you want to allow ?")
            .setPositiveButton("Yes") { dialog: DialogInterface?, which: Int -> listener.accept() }
            .setNegativeButton("No") { dialog: DialogInterface?, which: Int -> listener.reject() }
            .show()
    }

    private fun showWebcamRequestDialog(listener: WebcamRequestListener) {
        MaterialAlertDialogBuilder(this@MainActivity)
            .setTitle("Webcam requested")
            .setMessage("Host is asking you to enable your webcam, do you want to allow ?")
            .setPositiveButton("Yes") { dialog: DialogInterface?, which: Int -> listener.accept() }
            .setNegativeButton("No") { dialog: DialogInterface?, which: Int -> listener.reject() }
            .show()
    }

    override fun onBackPressed() {
        showLeaveOrEndDialog()
    }

    override fun onDestroy() {
        if (meeting != null) meeting!!.leave()
        if (svrShare != null) svrShare!!.release()
        (findViewById<View>(R.id.rvParticipants) as RecyclerView).adapter = null
        timer.cancel()
        super.onDestroy()
    }

    companion object {
        private var meeting: Meeting? = null
        private val YOUTUBE_RTMP_URL: String? = null
        private val YOUTUBE_RTMP_STREAM_KEY: String? = null
        private val CAPTURE_PERMISSION_REQUEST_CODE = 1
    }
}