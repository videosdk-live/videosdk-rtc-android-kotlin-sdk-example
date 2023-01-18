package live.videosdk.android.onetoonedemo

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.core.graphics.drawable.DrawableCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import live.videosdk.rtc.android.*
import live.videosdk.rtc.android.VideoView
import live.videosdk.rtc.android.listeners.MeetingEventListener
import live.videosdk.rtc.android.listeners.ParticipantEventListener
import org.webrtc.VideoTrack

class MeetingActivity : AppCompatActivity() {
    private var meeting: Meeting? = null
    private var micEnabled = true
    private var webcamEnabled = true

    private var btnWebcam: FloatingActionButton? = null
    private var btnMic: FloatingActionButton? = null
    private var btnLeave: FloatingActionButton? = null
    private var btnSwitchCameraMode: ImageButton? = null

    private var localView: VideoView? = null
    private var participantView: VideoView? = null

    private var localCard: CardView? = null
    private var participantCard: CardView? = null
    private var localParticipantImg: ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_meeting)

        //
        btnLeave = findViewById(R.id.btnLeave)
        btnSwitchCameraMode = findViewById(R.id.btnSwitchCameraMode)
        btnMic = findViewById(R.id.btnMic)
        btnWebcam = findViewById(R.id.btnWebcam)

        localCard = findViewById(R.id.LocalCard)
        participantCard = findViewById(R.id.ParticipantCard)
        localView = findViewById(R.id.localView)
        participantView = findViewById(R.id.participantView)
        localParticipantImg = findViewById(R.id.localParticipant_img)

        //
        val toolbar = findViewById<Toolbar>(R.id.material_toolbar)
        toolbar.title = ""
        setSupportActionBar(toolbar)

        //
        val token = intent.getStringExtra("token")
        val meetingId = intent.getStringExtra("meetingId")

        // set participant name
        val localParticipantName = "Alex"

        // Initialize VideoSDK
        VideoSDK.initialize(applicationContext)

        // pass the token generated from api server
        VideoSDK.config(token)

        // create a new meeting instance
        meeting = VideoSDK.initMeeting(
            this@MeetingActivity, meetingId, localParticipantName,
            micEnabled, webcamEnabled, null, null
        )

        // join the meeting
        meeting?.join()

        //
        val textMeetingId = findViewById<TextView>(R.id.txtMeetingId)
        textMeetingId.text = meetingId

        // copy meetingId to clipboard
        (findViewById<View>(R.id.btnCopyContent) as ImageButton).setOnClickListener {
            if (meetingId != null) {
                copyTextToClipboard(meetingId)
            }
        }

        // actions
        setActionListeners()

        // setup local participant view
        setLocalListeners()

        // handle meeting events
        meeting!!.addEventListener(meetingEventListener)
    }

    private fun copyTextToClipboard(text: String) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Copied text", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this@MeetingActivity, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    private fun setActionListeners() {
        // Toggle mic
        btnMic!!.setOnClickListener { toggleMic() }

        // Toggle webcam
        btnWebcam!!.setOnClickListener { toggleWebCam() }

        // Leave meeting
        btnLeave!!.setOnClickListener {
            // this will make the local participant leave the meeting
            meeting!!.leave()
        }

        // Switch camera
        btnSwitchCameraMode!!.setOnClickListener {
            //a participant can change stream from front/rear camera during the meeting.
            meeting!!.changeWebcam()
        }

    }

    private fun toggleMic() {
        if (micEnabled) {
            // this will mute the local participant's mic
            meeting!!.muteMic()
        } else {
            // this will unmute the local participant's mic
            meeting!!.unmuteMic()
        }
        micEnabled = !micEnabled
        // change mic icon according to micEnable status
        toggleMicIcon()
    }

    @SuppressLint("ResourceType")
    private fun toggleMicIcon() {
        if (micEnabled) {
            btnMic!!.setImageResource(R.drawable.ic_mic_on)
            btnMic!!.setColorFilter(Color.WHITE)
            var buttonDrawable = btnMic!!.background
            buttonDrawable = DrawableCompat.wrap(buttonDrawable!!)
            if (buttonDrawable != null) DrawableCompat.setTint(buttonDrawable, Color.TRANSPARENT)
            btnMic!!.background = buttonDrawable
        } else {
            btnMic!!.setImageResource(R.drawable.ic_mic_off)
            btnMic!!.setColorFilter(Color.BLACK)
            var buttonDrawable = btnMic!!.background
            buttonDrawable = DrawableCompat.wrap(buttonDrawable!!)
            if (buttonDrawable != null) DrawableCompat.setTint(buttonDrawable, Color.WHITE)
            btnMic!!.background = buttonDrawable
        }
    }

    private fun toggleWebCam() {
        if (webcamEnabled) {
            // this will disable the local participant webcam
            meeting!!.disableWebcam()
        } else {
            // this will enable the local participant webcam
            meeting!!.enableWebcam()
        }
        webcamEnabled = !webcamEnabled
        // change webCam icon according to webcamEnabled status
        toggleWebcamIcon()
    }

    @SuppressLint("ResourceType")
    private fun toggleWebcamIcon() {
        if (webcamEnabled) {
            btnWebcam!!.setImageResource(R.drawable.ic_video_camera)
            btnWebcam!!.setColorFilter(Color.WHITE)
            var buttonDrawable = btnWebcam!!.background
            buttonDrawable = DrawableCompat.wrap(buttonDrawable!!)
            if (buttonDrawable != null) DrawableCompat.setTint(buttonDrawable, Color.TRANSPARENT)
            btnWebcam!!.background = buttonDrawable
        } else {
            btnWebcam!!.setImageResource(R.drawable.ic_video_camera_off)
            btnWebcam!!.setColorFilter(Color.BLACK)
            var buttonDrawable = btnWebcam!!.background
            buttonDrawable = DrawableCompat.wrap(buttonDrawable!!)
            if (buttonDrawable != null) DrawableCompat.setTint(buttonDrawable, Color.WHITE)
            btnWebcam!!.background = buttonDrawable
        }
    }

    private fun setLocalListeners() {
        meeting!!.localParticipant
            .addEventListener(object : ParticipantEventListener() {
                override fun onStreamEnabled(stream: Stream) {
                    if (stream.kind.equals("video", ignoreCase = true)) {
                        val track = stream.track as VideoTrack
                        localView!!.visibility = View.VISIBLE
                        localView!!.addTrack(track)
                        localView!!.setZOrderMediaOverlay(true)
                        (localCard as View?)!!.bringToFront()
                    }
                }

                override fun onStreamDisabled(stream: Stream) {
                    if (stream.kind.equals("video", ignoreCase = true)) {
                        localView!!.removeTrack()
                        localView!!.visibility = View.GONE
                    }
                }
            })
    }

    private val participantEventListener: ParticipantEventListener =
        object : ParticipantEventListener() {
            // trigger when participant enabled mic/webcam
            override fun onStreamEnabled(stream: Stream) {
                if (stream.kind.equals("video", ignoreCase = true)) {
                    localView!!.setZOrderMediaOverlay(true)
                    (localCard as View?)!!.bringToFront()
                    val track = stream.track as VideoTrack
                    participantView!!.visibility = View.VISIBLE
                    participantView!!.addTrack(track)
                }
            }

            // trigger when participant disabled mic/webcam
            override fun onStreamDisabled(stream: Stream) {
                if (stream.kind.equals("video", ignoreCase = true)) {
                    participantView!!.removeTrack()
                    participantView!!.visibility = View.GONE
                }
            }
        }

    private val meetingEventListener: MeetingEventListener = object : MeetingEventListener() {
        override fun onMeetingJoined() {
            // change mic,webCam icon after meeting successfully joined
            toggleMicIcon()
            toggleWebcamIcon()
        }

        override fun onMeetingLeft() {
            if (!isDestroyed) {
                val intent = Intent(this@MeetingActivity, JoinActivity::class.java)
                startActivity(intent)
                finish()
            }
        }

        override fun onParticipantJoined(participant: Participant) {
            // Display local participant as miniView when other participant joined
            changeLocalParticipantView(true)
            Toast.makeText(
                this@MeetingActivity, participant.displayName + " joined",
                Toast.LENGTH_SHORT
            ).show()
            participant.addEventListener(participantEventListener)
        }

        override fun onParticipantLeft(participant: Participant) {
            // Display local participant as largeView when other participant left
            changeLocalParticipantView(false)
            Toast.makeText(
                this@MeetingActivity, participant.displayName + " left",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun changeLocalParticipantView(isMiniView: Boolean) {
        if (isMiniView) {
            // show localCard as miniView
            localCard!!.layoutParams =
                FrameLayout.LayoutParams(300, 430, Gravity.RIGHT or Gravity.BOTTOM)
            val cardViewMarginParams = localCard!!.layoutParams as ViewGroup.MarginLayoutParams
            cardViewMarginParams.setMargins(30, 0, 60, 40)
            localCard!!.requestLayout()
            // set height-width of localParticipant_img
            localParticipantImg!!.layoutParams = FrameLayout.LayoutParams(150, 150, Gravity.CENTER)
            (localCard as View?)!!.bringToFront()
            participantCard!!.visibility = View.VISIBLE
        } else {
            // show localCard as largeView
            localCard!!.layoutParams =
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            val cardViewMarginParams = localCard!!.layoutParams as ViewGroup.MarginLayoutParams
            cardViewMarginParams.setMargins(30, 5, 30, 30)
            localCard!!.requestLayout()
            // set height-width of localParticipant_img
            localParticipantImg!!.layoutParams = FrameLayout.LayoutParams(400, 400, Gravity.CENTER)
            participantCard!!.visibility = View.GONE
        }
    }

    override fun onDestroy() {
        if (meeting != null) {
            meeting!!.removeAllListeners()
            meeting!!.localParticipant.removeAllListeners()
            meeting!!.leave()
            meeting = null
        }
        if (participantView != null) {
            participantView!!.visibility = View.GONE
            participantView!!.releaseSurfaceViewRenderer()
        }
        if (localView != null) {
            localView!!.visibility = View.GONE
            localView!!.releaseSurfaceViewRenderer()
        }
        super.onDestroy()
    }
}