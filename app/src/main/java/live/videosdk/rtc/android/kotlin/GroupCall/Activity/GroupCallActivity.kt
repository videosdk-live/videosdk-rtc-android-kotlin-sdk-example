package live.videosdk.rtc.android.kotlin.GroupCall.Activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.*
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.text.style.ImageSpan
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.view.View.*
import android.view.animation.TranslateAnimation
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.nabinbhandari.android.permissions.PermissionHandler
import com.nabinbhandari.android.permissions.Permissions
import live.videosdk.rtc.android.*
import live.videosdk.rtc.android.VideoView
import live.videosdk.rtc.android.kotlin.Common.Activity.CreateOrJoinActivity
import live.videosdk.rtc.android.kotlin.Common.Adapter.*
import live.videosdk.rtc.android.kotlin.Common.Listener.ResponseListener
import live.videosdk.rtc.android.kotlin.Common.Modal.ListItem
import live.videosdk.rtc.android.kotlin.Common.RobotoFont
import live.videosdk.rtc.android.kotlin.Common.Service.ForegroundService
import live.videosdk.rtc.android.kotlin.Common.Utils.HelperClass
import live.videosdk.rtc.android.kotlin.Common.Utils.NetworkUtils
import live.videosdk.rtc.android.kotlin.GroupCall.Adapter.ParticipantViewAdapter
import live.videosdk.rtc.android.kotlin.GroupCall.Utils.ParticipantState
import live.videosdk.rtc.android.kotlin.R
import live.videosdk.rtc.android.lib.AppRTCAudioManager.AudioDevice
import live.videosdk.rtc.android.lib.JsonUtils
import live.videosdk.rtc.android.lib.MeetingState
import live.videosdk.rtc.android.listeners.*
import live.videosdk.rtc.android.model.PubSubPublishOptions
import live.videosdk.rtc.android.permission.Permission
import org.json.JSONObject
import org.webrtc.RendererCommon
import org.webrtc.VideoTrack
import java.util.*
import kotlin.math.roundToInt


class GroupCallActivity : AppCompatActivity() {
    private var meeting: Meeting? = null
    private var btnWebcam: FloatingActionButton? = null
    private var btnMic: ImageButton? = null
    private var btnAudioSelection: ImageButton? = null
    private var btnSwitchCameraMode: ImageButton? = null
    private var btnLeave: FloatingActionButton? = null
    private var btnChat: FloatingActionButton? = null
    private var btnMore: FloatingActionButton? = null

    private var micLayout: LinearLayout? = null
    private var participants: ArrayList<Participant>? = null
    private var shareView: VideoView? = null
    private var shareLayout: FrameLayout? = null

    private var micEnabled = true
    private var webcamEnabled = true
    private var recording = false
    private var localScreenShare = false
    private var token: String? = null
    private var recordingStatusSnackbar: Snackbar? = null


    private val CAPTURE_PERMISSION_REQUEST_CODE = 1

    private var screenshareEnabled = false
    private var bottomSheetDialog: BottomSheetDialog? = null
    private var selectedAudioDeviceName: String? = null

    private var etmessage: EditText? = null
    private var messageAdapter: MessageAdapter? = null
    private var pubSubMessageListener: PubSubMessageListener? = null
    private var viewPager2: ViewPager2? = null
    private var viewAdapter: ParticipantViewAdapter? = null
    private var meetingSeconds = 0
    private var txtMeetingTime: TextView? = null
    private var btnStopScreenShare: Button? = null

    var clickCount = 0
    var startTime: Long = 0
    val MAX_DURATION = 500
    var fullScreen = false
    var onTouchListener: OnTouchListener? = null
    private var screenShareParticipantNameSnackbar: Snackbar? = null
    private var runnable: Runnable? = null
    val handler = Handler()
    private var chatListener: PubSubMessageListener? = null
    private var raiseHandListener: PubSubMessageListener? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_call)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        //
        val toolbar = findViewById<Toolbar>(R.id.material_toolbar)
        toolbar.title = ""
        setSupportActionBar(toolbar)

        //
        btnLeave = findViewById(R.id.btnLeave)
        btnChat = findViewById(R.id.btnChat)
        btnMore = findViewById(R.id.btnMore)
        btnSwitchCameraMode = findViewById(R.id.btnSwitchCameraMode)
        micLayout = findViewById(R.id.micLayout)
        btnMic = findViewById(R.id.btnMic)
        btnWebcam = findViewById(R.id.btnWebcam)
        btnAudioSelection = findViewById(R.id.btnAudioSelection)
        txtMeetingTime = findViewById(R.id.txtMeetingTime)
        btnStopScreenShare = findViewById(R.id.btnStopScreenShare)
        viewPager2 = findViewById(R.id.view_pager_video_grid)
        shareLayout = findViewById(R.id.shareLayout)
        shareView = findViewById(R.id.shareView)
        token = intent.getStringExtra("token")
        val meetingId = intent.getStringExtra("meetingId")
        micEnabled = intent.getBooleanExtra("micEnabled", true)
        webcamEnabled = intent.getBooleanExtra("webcamEnabled", true)
        var localParticipantName = intent.getStringExtra("participantName")
        if (localParticipantName == null) {
            localParticipantName = "John Doe"
        }

        // pass the token generated from api server
        VideoSDK.config(token)

        val customTracks: MutableMap<String, CustomStreamTrack> = HashMap()

        val videoCustomTrack = VideoSDK.createCameraVideoTrack(
            "h720p_w960p",
            "front",
            CustomStreamTrack.VideoMode.TEXT,
            true,
            this,null,VideoSDK.getSelectedVideoDevice()
        )
        customTracks["video"] = videoCustomTrack

        val audioCustomTrack = VideoSDK.createAudioTrack("high_quality", this)
        customTracks["mic"] = audioCustomTrack

        // create a new meeting instance
        meeting = VideoSDK.initMeeting(
            this@GroupCallActivity, meetingId, localParticipantName,
            micEnabled, webcamEnabled, null, null, true,customTracks,null
        )

        //
        val textMeetingId = findViewById<TextView>(R.id.txtMeetingId)
        textMeetingId.text = meetingId
        meeting!!.addEventListener(meetingEventListener)

        //show Progress
        HelperClass.showProgress(window.decorView.rootView)

        //
        checkPermissions()

        // Actions
        setActionListeners()
        setAudioDeviceListeners()
        (findViewById<View>(R.id.btnCopyContent) as ImageButton).setOnClickListener {
            copyTextToClipboard(
                meetingId
            )
        }
        btnAudioSelection!!.setOnClickListener { showAudioInputDialog() }
        btnStopScreenShare!!.setOnClickListener {
            if (localScreenShare) {
                meeting!!.disableScreenShare()
            }
        }
        recordingStatusSnackbar = Snackbar.make(
            findViewById(R.id.mainLayout), "Recording will be started in few moments",
            Snackbar.LENGTH_INDEFINITE
        )
        HelperClass.setSnackBarStyle(recordingStatusSnackbar!!.view, 0)
        recordingStatusSnackbar!!.isGestureInsetBottomIgnored = true
        viewAdapter = ParticipantViewAdapter(this@GroupCallActivity, meeting!!)
        onTouchListener = object : OnTouchListener {
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action and MotionEvent.ACTION_MASK) {
                    MotionEvent.ACTION_UP -> {
                        clickCount++
                        if (clickCount == 1) {
                            startTime = System.currentTimeMillis()
                        } else if (clickCount == 2) {
                            val duration = System.currentTimeMillis() - startTime
                            if (duration <= MAX_DURATION) {
                                if (fullScreen) {
                                    toolbar.visibility = VISIBLE
                                    run {
                                        var i = 0
                                        while (i < toolbar.childCount) {
                                            toolbar.getChildAt(i).visibility = VISIBLE
                                            i++
                                        }
                                    }
                                    val params = Toolbar.LayoutParams(
                                        ViewGroup.LayoutParams.WRAP_CONTENT,
                                        ViewGroup.LayoutParams.WRAP_CONTENT
                                    )
                                    params.setMargins(22, 10, 0, 0)
                                    findViewById<View>(R.id.meetingLayout).layoutParams =
                                        params
                                    shareLayout!!.layoutParams = LinearLayout.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        HelperClass().dpToPx(420, this@GroupCallActivity)
                                    )
                                    (findViewById<View>(R.id.localScreenShareView) as LinearLayout).layoutParams =
                                        LinearLayout.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            HelperClass().dpToPx(420, this@GroupCallActivity)
                                        )
                                    val toolbarAnimation = TranslateAnimation(
                                        0F,
                                        0F,
                                        0F,
                                        10F
                                    )
                                    toolbarAnimation.duration = 500
                                    toolbarAnimation.fillAfter = true
                                    toolbar.startAnimation(toolbarAnimation)
                                    val bottomAppBar = findViewById<BottomAppBar>(R.id.bottomAppbar)
                                    bottomAppBar.visibility = VISIBLE
                                    var i = 0
                                    while (i < bottomAppBar.childCount) {
                                        bottomAppBar.getChildAt(i).visibility = VISIBLE
                                        i++
                                    }
                                    val animate = TranslateAnimation(
                                        0F,
                                        0F,
                                        findViewById<View>(R.id.bottomAppbar).height
                                            .toFloat(),
                                        0F
                                    )
                                    animate.duration = 300
                                    animate.fillAfter = true
                                    findViewById<View>(R.id.bottomAppbar).startAnimation(animate)
                                } else {
                                    toolbar.visibility = GONE
                                    run {
                                        var i = 0
                                        while (i < toolbar.childCount) {
                                            toolbar.getChildAt(i).visibility = GONE
                                            i++
                                        }
                                    }
                                    shareLayout!!.layoutParams = LinearLayout.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        HelperClass().dpToPx(500, this@GroupCallActivity)
                                    )
                                    (findViewById<View>(R.id.localScreenShareView) as LinearLayout).layoutParams =
                                        LinearLayout.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            HelperClass().dpToPx(500, this@GroupCallActivity)
                                        )
                                    val toolbarAnimation = TranslateAnimation(
                                        0F,
                                        0F,
                                        0F,
                                        10F
                                    )
                                    toolbarAnimation.duration = 500
                                    toolbarAnimation.fillAfter = true
                                    toolbar.startAnimation(toolbarAnimation)
                                    val bottomAppBar = findViewById<BottomAppBar>(R.id.bottomAppbar)
                                    bottomAppBar.visibility = GONE
                                    var i = 0
                                    while (i < bottomAppBar.childCount) {
                                        bottomAppBar.getChildAt(i).visibility = GONE
                                        i++
                                    }
                                    val animate = TranslateAnimation(
                                        0F,
                                        0F,
                                        0F,
                                        findViewById<View>(R.id.bottomAppbar).height
                                            .toFloat()
                                    )
                                    animate.duration = 400
                                    animate.fillAfter = true
                                    findViewById<View>(R.id.bottomAppbar).startAnimation(animate)
                                }
                                fullScreen = !fullScreen
                                clickCount = 0
                            } else {
                                clickCount = 1
                                startTime = System.currentTimeMillis()
                            }

                        }
                    }
                }
                return true
            }
        }
        findViewById<View>(R.id.participants_Layout).setOnTouchListener(onTouchListener)

        findViewById<View>(R.id.ivParticipantScreenShareNetwork).setOnClickListener {
            val participantList = getAllParticipants()
            val participant = participantList[0]
            val popupwindow_obj: PopupWindow? = HelperClass().callStatsPopupDisplay(
                participant,
                findViewById(R.id.ivParticipantScreenShareNetwork),
                this@GroupCallActivity,
                true
            )
            popupwindow_obj!!.showAsDropDown(
                findViewById(R.id.ivParticipantScreenShareNetwork),
                -350,
                -85
            )
        }

        findViewById<View>(R.id.ivLocalScreenShareNetwork).setOnClickListener {
            val popupwindow_obj: PopupWindow? = HelperClass().callStatsPopupDisplay(
                meeting!!.getLocalParticipant(),
                findViewById(R.id.ivLocalScreenShareNetwork),
                this@GroupCallActivity,
                true
            )
            popupwindow_obj!!.showAsDropDown(
                findViewById(R.id.ivLocalScreenShareNetwork),
                -350,
                -85
            )
        }
    }

    fun getTouchListener(): OnTouchListener? {
        return onTouchListener
    }

    private fun toggleMicIcon() {
        if (micEnabled) {
            btnMic!!.setImageResource(R.drawable.ic_mic_on)
            btnAudioSelection!!.setImageResource(R.drawable.ic_baseline_arrow_drop_down_24)
            micLayout!!.background =
                ContextCompat.getDrawable(this@GroupCallActivity, R.drawable.layout_selected)
        } else {
            btnMic!!.setImageResource(R.drawable.ic_mic_off_24)
            btnAudioSelection!!.setImageResource(R.drawable.ic_baseline_arrow_drop_down)
            micLayout!!.setBackgroundColor(Color.WHITE)
            micLayout!!.background =
                ContextCompat.getDrawable(this@GroupCallActivity, R.drawable.layout_nonselected)
        }
    }

    @SuppressLint("ResourceType")
    private fun toggleWebcamIcon() {
        if (webcamEnabled) {
            btnWebcam!!.setImageResource(R.drawable.ic_video_camera)
            btnWebcam!!.setColorFilter(Color.WHITE)
            var buttonDrawable = btnWebcam!!.background
            buttonDrawable = DrawableCompat.wrap(buttonDrawable!!)
            //the color is a direct color int and not a color resource
            DrawableCompat.setTint(buttonDrawable, Color.TRANSPARENT)
            btnWebcam!!.background = buttonDrawable
        } else {
            btnWebcam!!.setImageResource(R.drawable.ic_video_camera_off)
            btnWebcam!!.setColorFilter(Color.BLACK)
            var buttonDrawable = btnWebcam!!.background
            buttonDrawable = DrawableCompat.wrap(buttonDrawable!!)
            //the color is a direct color int and not a color resource
            DrawableCompat.setTint(buttonDrawable, Color.WHITE)
            btnWebcam!!.background = buttonDrawable
        }
    }


    private val meetingEventListener: MeetingEventListener = object : MeetingEventListener() {
        @SuppressLint("NewApi")
        override fun onMeetingJoined() {
            if (meeting != null) {
                //hide progress when meetingJoined
                HelperClass.hideProgress(window.decorView.rootView)
                toggleMicIcon()
                toggleWebcamIcon()
                setLocalListeners()
                NetworkUtils(this@GroupCallActivity).fetchMeetingTime(
                    meeting!!.meetingId,
                    token,
                    object : ResponseListener<Int> {
                        override fun onResponse(meetingTime: Int?) {
                            meetingSeconds = meetingTime!!
                            showMeetingTime()
                        }
                    })

                startService(Intent(applicationContext, ForegroundService::class.java).apply {
                    action = ForegroundService.ACTION_START
                })

                viewPager2!!.offscreenPageLimit = 1
                viewPager2!!.adapter = viewAdapter
                raiseHandListener =
                    PubSubMessageListener { pubSubMessage ->
                        val parentLayout = findViewById<View>(android.R.id.content)
                        var snackbar: Snackbar
                        if ((pubSubMessage.senderId == meeting!!.localParticipant.id)) {
                            snackbar = Snackbar.make(
                                parentLayout,
                                "You raised hand",
                                Snackbar.LENGTH_SHORT
                            )
                        } else {
                            snackbar = Snackbar.make(
                                parentLayout,
                                pubSubMessage.senderName + " raised hand  ",
                                Snackbar.LENGTH_LONG
                            )
                        }

                        val snackbarLayout = snackbar.view
                        val snackbarTextId = com.google.android.material.R.id.snackbar_text
                        val textView = snackbarLayout.findViewById<View>(snackbarTextId) as TextView

                        val drawable = resources.getDrawable(R.drawable.ic_raise_hand)
                        drawable.setBounds(0, 0, 50, 65)
                        textView.setCompoundDrawablesRelative(drawable, null, null, null)
                        textView.compoundDrawablePadding = 15
                        HelperClass.setSnackBarStyle(snackbar.view, 0)
                        snackbar.isGestureInsetBottomIgnored = true
                        snackbar.view.setOnClickListener { snackbar.dismiss() }
                        snackbar.show()
                    }

                // notify user for raise hand
                meeting!!.pubSub.subscribe("RAISE_HAND", raiseHandListener)
                chatListener = PubSubMessageListener { pubSubMessage ->
                    if (pubSubMessage.senderId != meeting!!.localParticipant.id) {
                        val parentLayout = findViewById<View>(android.R.id.content)
                        val snackbar = Snackbar.make(
                            parentLayout, (pubSubMessage.senderName + " says: " +
                                    pubSubMessage.message), Snackbar.LENGTH_SHORT
                        )
                            .setDuration(2000)
                        val snackbarView = snackbar.view
                        HelperClass.setSnackBarStyle(snackbarView, 0)
                        snackbar.view.setOnClickListener { snackbar.dismiss() }
                        snackbar.show()
                    }
                }
                // notify user of any new messages
                meeting!!.pubSub.subscribe("CHAT", chatListener)

                //terminate meeting in 10 minutes
                Handler().postDelayed({
                    if (!isDestroyed) {
                        val alertDialog = MaterialAlertDialogBuilder(
                            this@GroupCallActivity,
                            R.style.AlertDialogCustom
                        ).create()
                        alertDialog.setCanceledOnTouchOutside(false)
                        val inflater = this@GroupCallActivity.layoutInflater
                        val dialogView = inflater.inflate(R.layout.alert_dialog_layout, null)
                        alertDialog.setView(dialogView)
                        val title = dialogView.findViewById<View>(R.id.title) as TextView
                        title.text = "Meeting Left"
                        val message = dialogView.findViewById<View>(R.id.message) as TextView
                        message.text = "Demo app limits meeting to 10 Minutes"
                        val positiveButton = dialogView.findViewById<Button>(R.id.positiveBtn)
                        positiveButton.text = "Ok"
                        positiveButton.setOnClickListener {
                            if (!isDestroyed) {
                                ParticipantState.destroy()
                                unSubscribeTopics()
                                meeting!!.leave()
                            }
                            alertDialog.dismiss()
                        }
                        val negativeButton = dialogView.findViewById<Button>(R.id.negativeBtn)
                        negativeButton.visibility = GONE
                        alertDialog.show()
                    }
                }, 600000)
            }
        }

        override fun onMeetingLeft() {
            handler.removeCallbacks(runnable!!)
            if (!isDestroyed) {
                val intents = Intent(this@GroupCallActivity, CreateOrJoinActivity::class.java)
                intents.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                            or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK
                )

                startService(Intent(applicationContext, ForegroundService::class.java).apply {
                    action = ForegroundService.ACTION_STOP
                })

                startActivity(intents)
                finish()
            }
        }

        override fun onPresenterChanged(participantId: String?) {
            updatePresenter(participantId)
        }

        override fun onRecordingStarted() {
            recording = true
            recordingStatusSnackbar!!.dismiss()
            (findViewById<View>(R.id.recordingLottie)).visibility = VISIBLE
            Toast.makeText(
                this@GroupCallActivity, "Recording started",
                Toast.LENGTH_SHORT
            ).show()
        }

        override fun onRecordingStopped() {
            recording = false
            (findViewById<View>(R.id.recordingLottie)).visibility = GONE
            Toast.makeText(
                this@GroupCallActivity, "Recording stopped",
                Toast.LENGTH_SHORT
            ).show()
        }

        override fun onExternalCallStarted() {
            Toast.makeText(this@GroupCallActivity, "onExternalCallStarted", Toast.LENGTH_SHORT)
                .show()
        }

        override fun onError(error: JSONObject) {
            try {
                val errorCodes = VideoSDK.getErrorCodes()
                val code = error.getInt("code")
                if (code == errorCodes.getInt("PREV_RECORDING_PROCESSING")) {
                    recordingStatusSnackbar!!.dismiss()
                }
                val snackbar = Snackbar.make(
                    findViewById(R.id.mainLayout), error.getString("message"),
                    Snackbar.LENGTH_LONG
                )
                HelperClass.setSnackBarStyle(snackbar.view, 0)
                snackbar.view.setOnClickListener { snackbar.dismiss() }
                snackbar.show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun onSpeakerChanged(participantId: String?) {}
        override fun onMeetingStateChanged(state: MeetingState?) {
            if (state === MeetingState.DISCONNECTED) {
                val parentLayout = findViewById<View>(android.R.id.content)
                val builderTextLeft = SpannableStringBuilder()
                builderTextLeft.append("   Call disconnected. Reconnecting...")
                builderTextLeft.setSpan(
                    ImageSpan(
                        this@GroupCallActivity,
                        R.drawable.ic_call_disconnected
                    ), 0, 1, 0
                )
                val snackbar = Snackbar.make(parentLayout, builderTextLeft, Snackbar.LENGTH_LONG)
                HelperClass.setSnackBarStyle(snackbar.view, resources.getColor(R.color.md_red_400))
                snackbar.view.setOnClickListener { snackbar.dismiss() }
                snackbar.show()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (handler.hasCallbacks((runnable)!!)) handler.removeCallbacks((runnable)!!)
                }
            }
        }

        override fun onMicRequested(participantId: String, listener: MicRequestListener) {
            showMicRequestDialog(listener)
        }

        override fun onWebcamRequested(participantId: String, listener: WebcamRequestListener) {
            showWebcamRequestDialog(listener)
        }
    }


    private fun setLocalListeners() {
        meeting!!.localParticipant.addEventListener(object : ParticipantEventListener() {
            override fun onStreamEnabled(stream: Stream) {
                if (stream.kind.equals("video", ignoreCase = true)) {
                    webcamEnabled = true
                    toggleWebcamIcon()
                } else if (stream.kind.equals("audio", ignoreCase = true)) {
                    micEnabled = true
                    toggleMicIcon()
                } else if (stream.kind.equals("share", ignoreCase = true)) {
                    findViewById<View>(R.id.localScreenShareView).visibility = VISIBLE
                    screenShareParticipantNameSnackbar = Snackbar.make(
                        findViewById(R.id.mainLayout), "You started presenting",
                        Snackbar.LENGTH_SHORT
                    )
                    HelperClass.setSnackBarStyle(screenShareParticipantNameSnackbar!!.view, 0)
                    screenShareParticipantNameSnackbar!!.isGestureInsetBottomIgnored = true
                    screenShareParticipantNameSnackbar!!.view.setOnClickListener { screenShareParticipantNameSnackbar!!.dismiss() }
                    screenShareParticipantNameSnackbar!!.show()
                    localScreenShare = true
                    screenshareEnabled = true
                }
            }

            override fun onStreamDisabled(stream: Stream) {
                if (stream.kind.equals("video", ignoreCase = true)) {
                    webcamEnabled = false
                    toggleWebcamIcon()
                } else if (stream.kind.equals("audio", ignoreCase = true)) {
                    micEnabled = false
                    toggleMicIcon()
                } else if (stream.kind.equals("share", ignoreCase = true)) {
                    findViewById<View>(R.id.localScreenShareView).visibility = GONE
                    localScreenShare = false
                    screenshareEnabled = false
                }
            }
        })
    }

    private fun askPermissionForScreenShare() {
        val mediaProjectionManager = application.getSystemService(
            MEDIA_PROJECTION_SERVICE
        ) as MediaProjectionManager
        startActivityForResult(
            mediaProjectionManager.createScreenCaptureIntent(), CAPTURE_PERMISSION_REQUEST_CODE
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != CAPTURE_PERMISSION_REQUEST_CODE) return
        if (resultCode != RESULT_OK) {
            Toast.makeText(
                this@GroupCallActivity,
                "You didn't give permission to capture the screen.",
                Toast.LENGTH_SHORT
            ).show()
            localScreenShare = false
            return
        }
        meeting!!.enableScreenShare(data,true)
    }

    private val permissionHandler: com.nabinbhandari.android.permissions.PermissionHandler = object : com.nabinbhandari.android.permissions.PermissionHandler() {
        override fun onGranted() {
        }

        override fun onDenied(context: Context, deniedPermissions: ArrayList<String>) {
            super.onDenied(context, deniedPermissions)
            Toast.makeText(
                this@GroupCallActivity,
                "Permission(s) not granted. Some feature may not work", Toast.LENGTH_SHORT
            ).show()
        }

        override fun onBlocked(context: Context, blockedList: ArrayList<String>): Boolean {
            Toast.makeText(
                this@GroupCallActivity,
                "Permission(s) not granted. Some feature may not work", Toast.LENGTH_SHORT
            ).show()
            return super.onBlocked(context, blockedList)
        }


    }


    private val permissionHandlerSDK: live.videosdk.rtc.android.permission.PermissionHandler = object :
        live.videosdk.rtc.android.permission.PermissionHandler() {
        override fun onGranted() {
            if (meeting != null) meeting!!.join()
        }

        override fun onBlocked(
            context: Context,
            blockedList: java.util.ArrayList<Permission>
        ): Boolean {
            for (blockedPermission in blockedList) {
                Log.d("VideoSDK Permission", "onBlocked: $blockedPermission")
            }
            return super.onBlocked(context, blockedList)
        }

        override fun onDenied(
            context: Context,
            deniedPermissions: java.util.ArrayList<Permission>
        ) {
            for (deniedPermission in deniedPermissions) {
                Log.d("VideoSDK Permission", "onDenied: $deniedPermission")
            }
            super.onDenied(context, deniedPermissions)
        }

        override fun onJustBlocked(
            context: Context,
            justBlockedList: java.util.ArrayList<Permission>,
            deniedPermissions: java.util.ArrayList<Permission>
        ) {
            for (justBlockedPermission in justBlockedList) {
                Log.d("VideoSDK Permission", "onJustBlocked: $justBlockedPermission")
            }
            super.onJustBlocked(context, justBlockedList, deniedPermissions)
        }
    }

    private fun checkPermissions() {
        val permissionList: MutableList<String> = ArrayList()
        permissionList.add(Manifest.permission.INTERNET)
        permissionList.add(Manifest.permission.READ_PHONE_STATE)
        val options =
            com.nabinbhandari.android.permissions.Permissions.Options().sendDontAskAgainToSettings(false)
        com.nabinbhandari.android.permissions.Permissions.check(this, permissionList.toTypedArray(), null, options, permissionHandler)
        val permissionListSDK: MutableList<Permission> = ArrayList()
        permissionListSDK.add(Permission.audio)
        permissionListSDK.add(Permission.video)
        permissionListSDK.add(Permission.bluetooth)
        val optionsSDK = live.videosdk.rtc.android.permission.Permissions.Options().setRationaleDialogTitle("Info").setSettingsDialogTitle("Warning")
        VideoSDK.checkPermissions(this,
            permissionListSDK,
            optionsSDK,
            permissionHandlerSDK
        )
    }

    private fun setAudioDeviceListeners() {
        meeting!!.setAudioDeviceChangeListener { selectedAudioDevice, availableAudioDevices ->
            selectedAudioDeviceName = selectedAudioDevice.toString()
        }
    }

    private fun copyTextToClipboard(text: String?) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Copied text", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this@GroupCallActivity, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    private fun toggleMic() {
        if (micEnabled) {
            meeting!!.muteMic()
        } else {
            val audioCustomTrack = VideoSDK.createAudioTrack("high_quality", this)
            meeting!!.unmuteMic(audioCustomTrack)
        }
    }

    private fun toggleWebCam() {
        if (webcamEnabled) {
            meeting!!.disableWebcam()
        } else {
            val videoCustomTrack = VideoSDK.createCameraVideoTrack(
                "h720p_w960p",
                "front",
                CustomStreamTrack.VideoMode.DETAIL,
                true,
                this,VideoSDK.getSelectedVideoDevice()
            )
            meeting!!.enableWebcam(videoCustomTrack)
        }
    }

    private fun setActionListeners() {
        // Toggle mic
        micLayout!!.setOnClickListener { toggleMic() }
        btnMic!!.setOnClickListener { toggleMic() }

        // Toggle webcam
        btnWebcam!!.setOnClickListener { toggleWebCam() }

        // Leave meeting
        btnLeave!!.setOnClickListener { showLeaveOrEndDialog() }
        btnMore!!.setOnClickListener { showMoreOptionsDialog() }
        btnSwitchCameraMode!!.setOnClickListener { meeting!!.changeWebcam() }

        // Chat
        btnChat!!.setOnClickListener {
            if (meeting != null) {
                openChat()
            }
        }
    }

    private fun toggleScreenSharing() {
        if (!screenshareEnabled) {
            if (!localScreenShare) {
                askPermissionForScreenShare()
            }
            localScreenShare = !localScreenShare
        } else {
            if (localScreenShare) {
                meeting!!.disableScreenShare()
            } else {
                Toast.makeText(this, "You can't share your screen", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updatePresenter(participantId: String?) {
        if (participantId == null) {
            shareView!!.visibility = GONE
            shareLayout!!.visibility = GONE
            screenshareEnabled = false
            return
        } else {
            screenshareEnabled = true
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
        (findViewById<View>(R.id.tvScreenShareParticipantName) as TextView).text =
            participant.displayName + " is presenting"
        findViewById<View>(R.id.tvScreenShareParticipantName).visibility = VISIBLE
        findViewById<View>(R.id.ivParticipantScreenShareNetwork).visibility = VISIBLE

        // display share video
        shareLayout!!.visibility = VISIBLE
        shareView!!.visibility = VISIBLE
        shareView!!.setZOrderMediaOverlay(true)
        shareView!!.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
        val videoTrack = shareStream.track as VideoTrack
        shareView!!.addTrack(videoTrack)
        screenShareParticipantNameSnackbar = Snackbar.make(
            findViewById(R.id.mainLayout), participant.displayName + " started presenting",
            Snackbar.LENGTH_SHORT
        )
        HelperClass.setSnackBarStyle(screenShareParticipantNameSnackbar!!.view, 0)
        screenShareParticipantNameSnackbar!!.isGestureInsetBottomIgnored = true
        screenShareParticipantNameSnackbar!!.view.setOnClickListener { screenShareParticipantNameSnackbar!!.dismiss() }
        screenShareParticipantNameSnackbar!!.show()


        // listen for share stop event
        participant.addEventListener(object : ParticipantEventListener() {
            override fun onStreamDisabled(stream: Stream) {
                if ((stream.kind == "share")) {
                    val track: VideoTrack = stream.track as VideoTrack
                    shareView!!.removeTrack()
                    shareView!!.visibility = GONE
                    shareLayout!!.visibility = GONE
                    findViewById<View>(R.id.tvScreenShareParticipantName).visibility =
                        GONE
                    findViewById<View>(R.id.ivParticipantScreenShareNetwork).visibility =
                        GONE

                    localScreenShare = false
                }
            }
        })
    }

    private fun showLeaveOrEndDialog() {
        val optionsArrayList: ArrayList<ListItem> = ArrayList<ListItem>()
        val leaveMeeting =
            AppCompatResources.getDrawable(this@GroupCallActivity, R.drawable.ic_leave)?.let {
                ListItem(
                    "Leave",
                    "Only you will leave the call",
                    it
                )
            }
        val endMeeting =
            AppCompatResources.getDrawable(this@GroupCallActivity, R.drawable.ic_end_meeting)?.let {
                ListItem(
                    "End",
                    "End call for all the participants",
                    it
                )
            }
        optionsArrayList.add(leaveMeeting!!)
        optionsArrayList.add(endMeeting!!)
        val arrayAdapter: ArrayAdapter<*> = LeaveOptionListAdapter(
            this@GroupCallActivity,
            R.layout.leave_options_list_layout,
            optionsArrayList
        )
        val materialAlertDialogBuilder =
            MaterialAlertDialogBuilder(this@GroupCallActivity, R.style.AlertDialogCustom)
                .setAdapter(
                    arrayAdapter
                ) { _: DialogInterface?, which: Int ->
                    when (which) {
                        0 -> {
                            viewPager2!!.adapter = null
                            ParticipantState.destroy()
                            unSubscribeTopics()
                            meeting!!.leave()
                        }
                        1 -> {
                            viewPager2!!.adapter = null
                            ParticipantState.destroy()
                            unSubscribeTopics()
                            meeting!!.end()
                        }
                    }
                }
        val alertDialog = materialAlertDialogBuilder.create()
        val listView = alertDialog.listView
        listView.divider =
            ColorDrawable(ContextCompat.getColor(this, R.color.md_grey_200)) // set color
        listView.setFooterDividersEnabled(false)
        listView.addFooterView(View(this@GroupCallActivity))
        listView.dividerHeight = 2
        val wmlp = alertDialog.window!!.attributes
        wmlp.gravity = Gravity.BOTTOM or Gravity.LEFT
        val layoutParams = WindowManager.LayoutParams()
        layoutParams.copyFrom(alertDialog.window!!.attributes)
        layoutParams.width = (getWindowWidth() * 0.8).roundToInt()
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
        alertDialog.window!!.attributes = layoutParams
        alertDialog.show()
    }


    private fun showAudioInputDialog() {
        val mics = meeting!!.mics
        var audioDeviceListItem: ListItem?
        val audioDeviceList: ArrayList<ListItem?> = ArrayList<ListItem?>()
        // Prepare list
        var item: String
        for (i in mics.indices) {
            item = mics.toTypedArray()[i].toString()
            var mic =
                item.substring(0, 1).uppercase(Locale.getDefault()) + item.substring(1).lowercase(
                    Locale.getDefault()
                )
            mic = mic.replace("_", " ")
            audioDeviceListItem = ListItem(mic, null, (item == selectedAudioDeviceName))
            audioDeviceList.add(audioDeviceListItem)
        }
        val arrayAdapter: ArrayAdapter<*> = AudioDeviceListAdapter(
            this@GroupCallActivity,
            R.layout.audio_device_list_layout,
            audioDeviceList
        )
        val materialAlertDialogBuilder =
            MaterialAlertDialogBuilder(this@GroupCallActivity, R.style.AlertDialogCustom)
                .setAdapter(arrayAdapter) { _: DialogInterface?, which: Int ->
                    var audioDevice: AudioDevice? = null
                    when (audioDeviceList[which]!!.itemName) {
                        "Bluetooth" -> audioDevice = AudioDevice.BLUETOOTH
                        "Wired headset" -> audioDevice = AudioDevice.WIRED_HEADSET
                        "Speaker phone" -> audioDevice = AudioDevice.SPEAKER_PHONE
                        "Earpiece" -> audioDevice = AudioDevice.EARPIECE
                    }
                    meeting!!.changeMic(
                        audioDevice,
                        VideoSDK.createAudioTrack("high_quality", this)
                    )
                }
        val alertDialog = materialAlertDialogBuilder.create()
        val listView = alertDialog.listView
        listView.divider =
            ColorDrawable(ContextCompat.getColor(this, R.color.md_grey_200)) // set color
        listView.setFooterDividersEnabled(false)
        listView.addFooterView(View(this@GroupCallActivity))
        listView.dividerHeight = 2
        val wmlp = alertDialog.window!!.attributes
        wmlp.gravity = Gravity.BOTTOM or Gravity.LEFT
        val layoutParams = WindowManager.LayoutParams()
        layoutParams.copyFrom(alertDialog.window!!.attributes)
        layoutParams.width = (getWindowWidth() * 0.6).roundToInt()
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
        alertDialog.window!!.attributes = layoutParams
        alertDialog.show()
    }

    private fun showMoreOptionsDialog() {
        val participantSize = meeting!!.participants.size + 1
        val moreOptionsArrayList: ArrayList<ListItem> = ArrayList<ListItem>()
        val raised_hand = ListItem(
            "Raise Hand",
            AppCompatResources.getDrawable(this@GroupCallActivity, R.drawable.raise_hand)!!
        )
        val start_screen_share = ListItem(
            "Share screen",
            AppCompatResources.getDrawable(this@GroupCallActivity, R.drawable.ic_screen_share)!!
        )
        val stop_screen_share = ListItem(
            "Stop screen share",
            AppCompatResources.getDrawable(this@GroupCallActivity, R.drawable.ic_screen_share)!!
        )
        val start_recording = ListItem(
            "Start recording",
            AppCompatResources.getDrawable(this@GroupCallActivity, R.drawable.ic_recording)!!
        )
        val stop_recording = ListItem(
            "Stop recording",
            AppCompatResources.getDrawable(this@GroupCallActivity, R.drawable.ic_recording)!!
        )
        val participant_list = ListItem(
            "Participants ($participantSize)",
            AppCompatResources.getDrawable(this@GroupCallActivity, R.drawable.ic_people)!!
        )
        moreOptionsArrayList.add(raised_hand)
        if (localScreenShare) {
            moreOptionsArrayList.add(stop_screen_share)
        } else {
            moreOptionsArrayList.add(start_screen_share)
        }
        if (recording) {
            moreOptionsArrayList.add(stop_recording)
        } else {
            moreOptionsArrayList.add(start_recording)
        }
        moreOptionsArrayList.add(participant_list)
        val arrayAdapter: ArrayAdapter<*> = MoreOptionsListAdapter(
            this@GroupCallActivity,
            R.layout.more_options_list_layout,
            moreOptionsArrayList
        )
        val materialAlertDialogBuilder =
            MaterialAlertDialogBuilder(this@GroupCallActivity, R.style.AlertDialogCustom)
                .setAdapter(
                    arrayAdapter
                ) { _: DialogInterface?, which: Int ->
                    when (which) {
                        0 -> {
                            raisedHand()
                        }
                        1 -> {
                            toggleScreenSharing()
                        }
                        2 -> {
                            toggleRecording()
                        }
                        3 -> {
                            openParticipantList()
                        }
                    }
                }
        val alertDialog = materialAlertDialogBuilder.create()
        val listView = alertDialog.listView
        listView.divider =
            ColorDrawable(ContextCompat.getColor(this, R.color.md_grey_200)) // set color
        listView.setFooterDividersEnabled(false)
        listView.addFooterView(View(this@GroupCallActivity))
        listView.dividerHeight = 2
        val wmlp = alertDialog.window!!.attributes
        wmlp.gravity = Gravity.BOTTOM or Gravity.RIGHT
        val layoutParams = WindowManager.LayoutParams()
        layoutParams.copyFrom(alertDialog.window!!.attributes)
        layoutParams.width = (getWindowWidth() * 0.8).roundToInt().toInt()
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
        alertDialog.window!!.attributes = layoutParams
        alertDialog.show()
    }

    private fun raisedHand() {
        meeting!!.pubSub.publish("RAISE_HAND", "Raise Hand by Me", PubSubPublishOptions())
    }

    private fun toggleRecording() {
        if (!recording) {
            recordingStatusSnackbar!!.show()
            val config = JSONObject()
            val layout = JSONObject()
            JsonUtils.jsonPut(layout, "type", "SPOTLIGHT")
            JsonUtils.jsonPut(layout, "priority", "PIN")
            JsonUtils.jsonPut(layout, "gridSize", 12)
            JsonUtils.jsonPut(config, "layout", layout)
            JsonUtils.jsonPut(config, "orientation", "portrait")
            JsonUtils.jsonPut(config, "theme", "DARK")
            meeting!!.startRecording(null,null,config,null)
        } else {
            meeting!!.stopRecording()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        showLeaveOrEndDialog()
    }

    override fun onDestroy() {
        if (meeting != null) {
            meeting!!.removeAllListeners()
            meeting!!.localParticipant.removeAllListeners()
            meeting!!.leave()
            meeting = null
        }
        if (shareView != null) {
            shareView!!.visibility = GONE
            shareLayout!!.visibility = GONE
            shareView!!.releaseSurfaceViewRenderer()
        }
        super.onDestroy()
    }

    fun unSubscribeTopics() {
        if (meeting != null) {
            meeting!!.pubSub.unsubscribe("CHAT", chatListener)
            meeting!!.pubSub.unsubscribe("RAISE_HAND", raiseHandListener)
        }
    }

    private fun openParticipantList() {
        val participantsListView: RecyclerView
        val close: ImageView
        bottomSheetDialog = BottomSheetDialog(this)
        val v3 = LayoutInflater.from(applicationContext)
            .inflate(R.layout.layout_participants_list_view, findViewById(R.id.layout_participants))
        bottomSheetDialog!!.setContentView(v3)
        participantsListView = v3.findViewById(R.id.rvParticipantsLinearView)
        (v3.findViewById<View>(R.id.participant_heading) as TextView).typeface =
            RobotoFont().getTypeFace(
                this@GroupCallActivity
            )
        close = v3.findViewById(R.id.ic_close)
        participantsListView.minimumHeight = getWindowHeight()
        bottomSheetDialog!!.show()
        close.setOnClickListener { bottomSheetDialog!!.dismiss() }
        meeting!!.addEventListener(meetingEventListener)
        participants = getAllParticipants()
        participantsListView.layoutManager = LinearLayoutManager(applicationContext)
        participantsListView.adapter =
            ParticipantListAdapter(participants, meeting!!, this@GroupCallActivity)
        participantsListView.setHasFixedSize(true)
    }

    private fun getWindowHeight(): Int {
        // Calculate window height for fullscreen use
        val displayMetrics = DisplayMetrics()
        (this@GroupCallActivity).windowManager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics.heightPixels
    }

    private fun getWindowWidth(): Int {
        // Calculate window height for fullscreen use
        val displayMetrics = DisplayMetrics()
        (this@GroupCallActivity).windowManager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics.widthPixels
    }

    private fun getAllParticipants(): ArrayList<Participant> {
        val participantList: ArrayList<Participant> = ArrayList<Participant>()
        val participants: Iterator<Participant> = meeting!!.participants.values.iterator()
        for (i in 0 until meeting!!.participants.size) {
            val participant = participants.next()
            participantList.add(participant)
        }
        return participantList
    }


    @SuppressLint("ClickableViewAccessibility")
    fun openChat() {
        val messageRcv: RecyclerView
        val close: ImageView
        bottomSheetDialog = BottomSheetDialog(this)
        val v3 = LayoutInflater.from(applicationContext)
            .inflate(R.layout.activity_chat, findViewById(R.id.layout_chat))
        bottomSheetDialog!!.setContentView(v3)
        messageRcv = v3.findViewById(R.id.messageRcv)
        messageRcv.layoutManager = LinearLayoutManager(applicationContext)
        val lp = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            getWindowHeight() / 2
        )
        messageRcv.layoutParams = lp
        val mBottomSheetCallback: BottomSheetCallback = object : BottomSheetCallback() {
            override fun onStateChanged(
                bottomSheet: View,
                @BottomSheetBehavior.State newState: Int
            ) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    val lp = RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.MATCH_PARENT,
                        getWindowHeight() / 2
                    )
                    messageRcv.layoutParams = lp
                } else if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    val lp = RelativeLayout.LayoutParams(
                        RelativeLayout.LayoutParams.MATCH_PARENT,
                        RelativeLayout.LayoutParams.MATCH_PARENT
                    )
                    messageRcv.layoutParams = lp
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        }
        bottomSheetDialog!!.behavior.addBottomSheetCallback(mBottomSheetCallback)
        etmessage = v3.findViewById(R.id.etMessage)
        etmessage!!.setOnTouchListener { view, event ->
            if (view.id == R.id.etMessage) {
                view.parent.requestDisallowInterceptTouchEvent(true)
                when (event.action and MotionEvent.ACTION_MASK) {
                    MotionEvent.ACTION_UP -> view.parent.requestDisallowInterceptTouchEvent(false)
                }
            }
            false
        }
        val btnSend = v3.findViewById<ImageButton>(R.id.btnSend)
        btnSend.isEnabled = false
        etmessage!!.onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                etmessage!!.hint = ""
            }
        }
        etmessage!!.isVerticalScrollBarEnabled = true
        etmessage!!.isScrollbarFadingEnabled = false
        etmessage!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                if (etmessage!!.text.toString().trim { it <= ' ' }.isNotEmpty()) {
                    btnSend.isEnabled = true
                    btnSend.isSelected = true
                } else {
                    btnSend.isEnabled = false
                    btnSend.isSelected = false
                }
            }

            override fun afterTextChanged(editable: Editable) {}
        })

        //
        pubSubMessageListener = PubSubMessageListener { message ->
            messageAdapter!!.addItem(message)
            messageRcv.scrollToPosition(messageAdapter!!.itemCount - 1)
        }

        // Subscribe for 'CHAT' topic
        val pubSubMessageList = meeting!!.pubSub.subscribe("CHAT", pubSubMessageListener)

        //
        messageAdapter =
            MessageAdapter(this, pubSubMessageList, meeting!!)
        messageRcv.adapter = messageAdapter
        messageRcv.addOnLayoutChangeListener { _: View?, _: Int, _: Int, _: Int, _: Int, _: Int, _: Int, _: Int, _: Int ->
            messageRcv.scrollToPosition(
                messageAdapter!!.itemCount - 1
            )
        }
        v3.findViewById<View>(R.id.btnSend).setOnClickListener {
            val message: String = etmessage!!.text.toString()
            if (message != "") {
                val publishOptions = PubSubPublishOptions()
                publishOptions.isPersist = true
                meeting!!.pubSub.publish("CHAT", message, publishOptions)
                etmessage!!.setText("")
            } else {
                Toast.makeText(
                    this@GroupCallActivity, "Please Enter Message",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        close = v3.findViewById(R.id.ic_close)
        bottomSheetDialog!!.show()
        close.setOnClickListener { bottomSheetDialog!!.dismiss() }
        bottomSheetDialog!!.setOnDismissListener {
            meeting!!.pubSub.unsubscribe(
                "CHAT",
                pubSubMessageListener
            )
        }
    }

    fun showMeetingTime() {
        runnable = object : Runnable {
            override fun run() {
                val hours = meetingSeconds / 3600
                val minutes = (meetingSeconds % 3600) / 60
                val secs = meetingSeconds % 60

                // Format the seconds into minutes,seconds.
                val time = String.format(
                    Locale.getDefault(),
                    "%02d:%02d:%02d", hours,
                    minutes, secs
                )
                txtMeetingTime!!.text = time
                meetingSeconds++
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(runnable!!)
    }

    private fun showMicRequestDialog(listener: MicRequestListener) {
        val alertDialog =
            MaterialAlertDialogBuilder(this@GroupCallActivity, R.style.AlertDialogCustom).create()
        alertDialog.setCanceledOnTouchOutside(false)
        val inflater = this.layoutInflater
        val dialogView = inflater.inflate(R.layout.alert_dialog_layout, null)
        alertDialog.setView(dialogView)
        val title = dialogView.findViewById<View>(R.id.title) as TextView
        title.visibility = GONE
        val message = dialogView.findViewById<View>(R.id.message) as TextView
        message.text = "Host is asking you to unmute your mic, do you want to allow ?"
        val positiveButton = dialogView.findViewById<Button>(R.id.positiveBtn)
        positiveButton.text = "Yes"
        positiveButton.setOnClickListener {
            listener.accept()
            alertDialog.dismiss()
        }
        val negativeButton = dialogView.findViewById<Button>(R.id.negativeBtn)
        negativeButton.text = "No"
        negativeButton.setOnClickListener {
            listener.reject()
            alertDialog.dismiss()
        }
        alertDialog.show()
    }


    private fun showWebcamRequestDialog(listener: WebcamRequestListener) {
        val alertDialog =
            MaterialAlertDialogBuilder(this@GroupCallActivity, R.style.AlertDialogCustom).create()
        alertDialog.setCanceledOnTouchOutside(false)
        val inflater = this.layoutInflater
        val dialogView = inflater.inflate(R.layout.alert_dialog_layout, null)
        alertDialog.setView(dialogView)
        val title = dialogView.findViewById<View>(R.id.title) as TextView
        title.visibility = GONE
        val message = dialogView.findViewById<View>(R.id.message) as TextView
        message.text = "Host is asking you to enable your webcam, do you want to allow ?"
        val positiveButton = dialogView.findViewById<Button>(R.id.positiveBtn)
        positiveButton.text = "Yes"
        positiveButton.setOnClickListener {
            listener.accept()
            alertDialog.dismiss()
        }
        val negativeButton = dialogView.findViewById<Button>(R.id.negativeBtn)
        negativeButton.text = "No"
        negativeButton.setOnClickListener {
            listener.reject()
            alertDialog.dismiss()
        }
        alertDialog.show()
    }


}