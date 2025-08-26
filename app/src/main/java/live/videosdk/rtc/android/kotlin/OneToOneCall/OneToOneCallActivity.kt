package live.videosdk.rtc.android.kotlin.OneToOneCall

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
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
import android.view.View.OnTouchListener
import android.view.ViewGroup.MarginLayoutParams
import android.view.animation.TranslateAnimation
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar

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
import live.videosdk.rtc.android.kotlin.R
import live.videosdk.rtc.android.lib.AppRTCAudioManager
import live.videosdk.rtc.android.lib.JsonUtils
import live.videosdk.rtc.android.lib.MeetingState
import live.videosdk.rtc.android.listeners.*
import live.videosdk.rtc.android.model.PubSubPublishOptions
import live.videosdk.rtc.android.permission.Permission
import live.videosdk.rtc.android.permission.PermissionHandler
import live.videosdk.rtc.android.permission.Permissions
import org.json.JSONObject
import org.webrtc.VideoTrack
import java.util.*
import kotlin.math.roundToInt


class OneToOneCallActivity : AppCompatActivity() {
    private var meeting: Meeting? = null
    private var localVideoView: VideoView? = null
    private var participantVideoView: VideoView? = null
    private var btnWebcam: FloatingActionButton? = null
    private var btnMic: ImageButton? = null
    private var btnAudioSelection: ImageButton? = null
    private var btnSwitchCameraMode: ImageButton? = null
    private var btnLeave: FloatingActionButton? = null
    private var btnChat: FloatingActionButton? = null
    private var btnMore: FloatingActionButton? = null
    private var localCard: CardView? = null
    private var participantCard: CardView? = null
    private var micLayout: LinearLayout? = null
    var participants: ArrayList<Participant>? = null
    private var ivParticipantMicStatus: ImageView? = null
//    private ImageView ivLocalParticipantMicStatus;
//    private GifImageView img_localActiveSpeaker, img_participantActiveSpeaker;

    private var ivLocalNetwork: ImageView? = null
    private var ivParticipantNetwork: ImageView? = null
    private var ivLocalScreenShareNetwork: ImageView? = null
    private var popupwindow_obj_local: PopupWindow? = null
    private var popupwindow_obj: PopupWindow? = null

    private var txtLocalParticipantName: TextView? = null
    private var txtParticipantName: TextView? = null
    private var tvName: TextView? = null
    private var tvLocalParticipantName: TextView? = null
    private var participantName: String? = null

    private var participantTrack: VideoTrack? = null

    private var micEnabled = true
    private var webcamEnabled = true
    private var recording = false
    private var localScreenShare = false
    private var fullScreen = false
    private var token: String? = null
    var clickCount = 0
    var startTime: Long = 0
    val MAX_DURATION = 500
    private var recordingStatusSnackbar: Snackbar? = null


    private val CAPTURE_PERMISSION_REQUEST_CODE = 1

    private val timer = Timer()
    private var screenshareEnabled = false
    private var localTrack: VideoTrack? = null
    private var screenshareTrack: VideoTrack? = null
    private var bottomSheetDialog: BottomSheetDialog? = null
    private var selectedAudioDeviceName: String? = null

    private var etmessage: EditText? = null

    private var messageAdapter: MessageAdapter? = null
    private var pubSubMessageListener: PubSubMessageListener? = null
    private var meetingSeconds = 0
    private var txtMeetingTime: TextView? = null
    private var screenShareParticipantNameSnackbar: Snackbar? = null

    private var runnable: Runnable? = null
    val handler = Handler()

    private var chatListener: PubSubMessageListener? = null

    @SuppressLint("ClickableViewAccessibility", "MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_one_to_one_call)
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
        localCard = findViewById(R.id.LocalCard)
        participantCard = findViewById(R.id.ParticipantCard)
        txtLocalParticipantName = findViewById(R.id.txtLocalParticipantName)
        txtParticipantName = findViewById(R.id.txtParticipantName)
        tvName = findViewById(R.id.tvName)
        tvLocalParticipantName = findViewById(R.id.tvLocalParticipantName)
        localVideoView = findViewById(R.id.svrLocal)
        localVideoView!!.setMirror(true)
        participantVideoView = findViewById(R.id.participantVideoView)
        btnMic = findViewById(R.id.btnMic)
        btnWebcam = findViewById(R.id.btnWebcam)
        btnAudioSelection = findViewById(R.id.btnAudioSelection)
        txtMeetingTime = findViewById(R.id.txtMeetingTime)
//        img_localActiveSpeaker = findViewById(R.id.img_localActiveSpeaker)
//        img_participantActiveSpeaker = findViewById(R.id.img_participantActiveSpeaker)

        ivLocalNetwork = findViewById(R.id.ivLocalNetwork)
        ivParticipantNetwork = findViewById(R.id.ivParticipantNetwork)
        ivLocalScreenShareNetwork = findViewById(R.id.ivLocalScreenShareNetwork)

//        ivLocalParticipantMicStatus = findViewById(R.id.ivLocalParticipantMicStatus)
        ivParticipantMicStatus = findViewById(R.id.ivParticipantMicStatus)

        token = intent.getStringExtra("token")
        val meetingId = intent.getStringExtra("meetingId")
        micEnabled = intent.getBooleanExtra("micEnabled", true)
        webcamEnabled = intent.getBooleanExtra("webcamEnabled", true)
        var localParticipantName = intent.getStringExtra("participantName")
        if (localParticipantName == null) {
            localParticipantName = "John Doe"
        }
        txtLocalParticipantName!!.text = localParticipantName.substring(0, 1)
        tvLocalParticipantName!!.text = "You"
        //
        val textMeetingId = findViewById<TextView>(R.id.txtMeetingId)
        textMeetingId.text = meetingId

        // pass the token generated from api server
        VideoSDK.config(token)

        val customTracks: MutableMap<String, CustomStreamTrack> = HashMap()

        val videoCustomTrack = VideoSDK.createCameraVideoTrack(
            "h720p_w960p",
            "front",
            CustomStreamTrack.VideoMode.TEXT,
            true,
            this,VideoSDK.getSelectedVideoDevice()
        )

        Log.d("TAG", "onCreate: " + VideoSDK.getSelectedVideoDevice().label)
        customTracks["video"] = videoCustomTrack

        val audioCustomTrack = VideoSDK.createAudioTrack("high_quality", this)
        customTracks["mic"] = audioCustomTrack

        // create a new meeting instance
        meeting = VideoSDK.initMeeting(
            this@OneToOneCallActivity, meetingId, localParticipantName,
            false, false, null, null, true,customTracks,null
        )
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

        findViewById<Button>(R.id.btnStopScreenShare).setOnClickListener({
            if (localScreenShare) {
                meeting!!.disableScreenShare()
            }
        })

        recordingStatusSnackbar = Snackbar.make(
            findViewById(R.id.mainLayout), "Recording will be started in few moments",
            Snackbar.LENGTH_INDEFINITE
        )
        HelperClass.setSnackBarStyle(recordingStatusSnackbar!!.view, 0)
        recordingStatusSnackbar!!.isGestureInsetBottomIgnored = true

        (findViewById<View>(R.id.participants_frameLayout) as FrameLayout).setOnTouchListener(object :
            OnTouchListener {
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
                                    toolbar.visibility = View.VISIBLE
                                    run {
                                        var i = 0
                                        while (i < toolbar.childCount) {
                                            toolbar.getChildAt(i).visibility = View.VISIBLE
                                            i++
                                        }
                                    }
                                    val params = Toolbar.LayoutParams(
                                        ViewGroup.LayoutParams.WRAP_CONTENT,
                                        ViewGroup.LayoutParams.WRAP_CONTENT
                                    )
                                    params.setMargins(30, 10, 0, 0)
                                    findViewById<View>(R.id.meetingLayout).layoutParams =
                                        params
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
                                    bottomAppBar.visibility = View.VISIBLE
                                    var i = 0
                                    while (i < bottomAppBar.childCount) {
                                        bottomAppBar.getChildAt(i).visibility = View.VISIBLE
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
                                    toolbar.visibility = View.GONE
                                    run {
                                        var i = 0
                                        while (i < toolbar.childCount) {
                                            toolbar.getChildAt(i).visibility = View.GONE
                                            i++
                                        }
                                    }
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
                                    bottomAppBar.visibility = View.GONE
                                    var i = 0
                                    while (i < bottomAppBar.childCount) {
                                        bottomAppBar.getChildAt(i).visibility = View.GONE
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
        })

        ivLocalNetwork!!.setOnClickListener {
            popupwindow_obj_local = HelperClass().callStatsPopupDisplay(
                meeting!!.localParticipant,
                ivLocalNetwork!!,
                this@OneToOneCallActivity,
                false
            )
            if (getAllParticipants().size == 0) {
                if (screenshareEnabled) popupwindow_obj_local!!.showAsDropDown(
                    ivLocalNetwork,
                    100,
                    -380
                ) else popupwindow_obj_local!!.showAsDropDown(ivLocalNetwork, -350, -85)
            } else {
                if (screenshareEnabled) {
                    val participantList = getAllParticipants()
                    val participant = participantList[0]
                    popupwindow_obj_local = HelperClass().callStatsPopupDisplay(
                        participant,
                        ivLocalNetwork!!,
                        this@OneToOneCallActivity,
                        false
                    )
                }
                popupwindow_obj_local!!.showAsDropDown(ivLocalNetwork, 100, -380)
            }
        }

        ivParticipantNetwork!!.setOnClickListener {
            val participantList = getAllParticipants()
            val participant = participantList[0]
            popupwindow_obj = HelperClass().callStatsPopupDisplay(
                participant,
                ivParticipantNetwork!!,
                this@OneToOneCallActivity,
                screenshareEnabled
            )
            popupwindow_obj!!.showAsDropDown(ivParticipantNetwork, -350, -85)
        }

        ivLocalScreenShareNetwork!!.setOnClickListener {
            popupwindow_obj = HelperClass().callStatsPopupDisplay(
                meeting!!.localParticipant,
                ivLocalScreenShareNetwork!!,
                this@OneToOneCallActivity,
                true
            )
            popupwindow_obj!!.showAsDropDown(ivLocalScreenShareNetwork, -350, -85)
        }
    }

    private fun toggleMicIcon(micEnabled: Boolean) {
        if (micEnabled) {
            btnMic!!.setImageResource(R.drawable.ic_mic_on)
            btnAudioSelection!!.setImageResource(R.drawable.ic_baseline_arrow_drop_down_24)
            micLayout!!.background =
                ContextCompat.getDrawable(this@OneToOneCallActivity, R.drawable.layout_selected)
        } else {
            btnMic!!.setImageResource(R.drawable.ic_mic_off_24)
            btnAudioSelection!!.setImageResource(R.drawable.ic_baseline_arrow_drop_down)
            micLayout!!.setBackgroundColor(Color.WHITE)
            micLayout!!.background =
                ContextCompat.getDrawable(this@OneToOneCallActivity, R.drawable.layout_nonselected)
        }
    }

    @SuppressLint("ResourceType")
    private fun toggleWebcamIcon(webcamEnabled: Boolean) {
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
        override fun onMeetingJoined() {
            if (meeting != null) {
                //hide progress when meetingJoined
                HelperClass.hideProgress(window.decorView.rootView)
                localCard!!.visibility = View.VISIBLE

                // if more than 2 participant join than leave the meeting
                if (meeting!!.participants.size <= 1) {
                    toggleMicIcon(micEnabled)
                    micEnabled = !micEnabled
                    webcamEnabled = !webcamEnabled
                    toggleMic()
                    toggleWebCam()

                    // Local participant listeners
                    setLocalListeners()
                    NetworkUtils(this@OneToOneCallActivity).fetchMeetingTime(
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


                    chatListener =
                        PubSubMessageListener { pubSubMessage ->
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
                                this@OneToOneCallActivity,
                                R.style.AlertDialogCustom
                            ).create()
                            alertDialog.setCanceledOnTouchOutside(false)
                            val inflater = this@OneToOneCallActivity.layoutInflater
                            val dialogView: View =
                                inflater.inflate(R.layout.alert_dialog_layout, null)
                            alertDialog.setView(dialogView)
                            val title = dialogView.findViewById<View>(R.id.title) as TextView
                            title.text = "Meeting Left"
                            val message = dialogView.findViewById<View>(R.id.message) as TextView
                            message.text = "Demo app limits meeting to 10 Minutes"
                            val positiveButton = dialogView.findViewById<Button>(R.id.positiveBtn)
                            positiveButton.text = "Ok"
                            positiveButton.setOnClickListener {
                                if (!isDestroyed) {
                                    unSubscribeTopics()
                                    meeting!!.leave()
                                }
                                alertDialog.dismiss()
                            }
                            val negativeButton = dialogView.findViewById<Button>(R.id.negativeBtn)
                            negativeButton.visibility = View.GONE
                            alertDialog.show()
                        }
                    }, 600000)

                } else {
                    val progressLayout: View = LayoutInflater.from(applicationContext)
                        .inflate(R.layout.progress_layout, findViewById(R.id.layout_progress))
                    if (!(this@OneToOneCallActivity as Activity).isFinishing) HelperClass.checkParticipantSize(
                        window.decorView.rootView, progressLayout
                    )
                    progressLayout.findViewById<View>(R.id.leaveBtn)
                        .setOnClickListener { meeting!!.leave() }
                }
            }
        }

        override fun onMeetingLeft() {
            if (!isDestroyed) {
                val intents = Intent(this@OneToOneCallActivity, CreateOrJoinActivity::class.java)
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

        override fun onParticipantJoined(participant: Participant) {
            if (meeting!!.participants.size < 2) {
                showParticipantCard()
                txtParticipantName!!.text = participant.displayName.substring(0, 1)
                participantName = participant.displayName
                tvName!!.text = participantName
                if (popupwindow_obj_local != null && popupwindow_obj_local!!.isShowing)
                    popupwindow_obj_local!!.dismiss()
                Toast.makeText(
                    this@OneToOneCallActivity, participant.displayName + " joined",
                    Toast.LENGTH_SHORT
                ).show()
            }
            participant.addEventListener(participantEventListener)
        }

        override fun onParticipantLeft(participant: Participant) {
            if (meeting!!.participants.size < 1) {
                hideParticipantCard()
                if (screenshareTrack != null) {
                    if (participantTrack != null) localVideoView!!.removeTrack()
                    localVideoView!!.visibility = View.GONE
                    showParticipantCard()
                    if (localTrack != null) {
                        localVideoView!!.addTrack(localTrack)
                        localVideoView!!.visibility = View.VISIBLE
                    }
                    participantVideoView!!.addTrack(screenshareTrack)
                    participantVideoView!!.visibility = View.VISIBLE
                }
                if (popupwindow_obj != null && popupwindow_obj!!.isShowing)
                    popupwindow_obj!!.dismiss()
                if (popupwindow_obj_local != null && popupwindow_obj_local!!.isShowing)
                    popupwindow_obj_local!!.dismiss()
                Toast.makeText(
                    this@OneToOneCallActivity, participant.displayName + " left",
                    Toast.LENGTH_SHORT
                ).show()
            }
            participant.removeAllListeners()
        }

        override fun onPresenterChanged(participantId: String?) {
            updatePresenter(participantId)
        }

        override fun onRecordingStarted() {
            recording = true
            recordingStatusSnackbar!!.dismiss()
            (findViewById<View>(R.id.recordingLottie)).visibility = View.VISIBLE
            Toast.makeText(
                this@OneToOneCallActivity, "Recording started",
                Toast.LENGTH_SHORT
            ).show()
        }

        override fun onRecordingStopped() {
            recording = false
            (findViewById<View>(R.id.recordingLottie)).visibility = View.GONE
            Toast.makeText(
                this@OneToOneCallActivity, "Recording stopped",
                Toast.LENGTH_SHORT
            ).show()
        }

        override fun onExternalCallStarted() {
            Toast.makeText(this@OneToOneCallActivity, "onExternalCallStarted", Toast.LENGTH_SHORT)
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

        override fun onSpeakerChanged(participantId: String) {
//            if (!HelperClass().isNullOrEmpty(participantId)) {
//                if ((participantId == meeting!!.localParticipant.id)) {
//                    img_localActiveSpeaker!!.visibility = View.VISIBLE
//                    img_participantActiveSpeaker!!.visibility = View.GONE
//                } else {
//                    img_participantActiveSpeaker!!.visibility = View.VISIBLE
//                    img_localActiveSpeaker!!.visibility = View.GONE
//                }
//            } else {
//                img_participantActiveSpeaker!!.visibility = View.GONE
//                img_localActiveSpeaker!!.visibility = View.GONE
//            }
        }

        override fun onMeetingStateChanged(state: MeetingState?) {
            if (state === MeetingState.DISCONNECTED) {
                val parentLayout = findViewById<View>(android.R.id.content)
                val builderTextLeft = SpannableStringBuilder()
                builderTextLeft.append("   Call disconnected. Reconnecting...")
                builderTextLeft.setSpan(
                    ImageSpan(
                        this@OneToOneCallActivity,
                        R.drawable.ic_call_disconnected
                    ), 0, 1, 0
                )
                val snackbar = Snackbar.make(parentLayout, builderTextLeft, Snackbar.LENGTH_LONG)
                HelperClass.setSnackBarStyle(
                    snackbar.view,
                    resources.getColor(R.color.md_red_400)
                )
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

    private fun showParticipantCard() {
        localCard!!.layoutParams =
            FrameLayout.LayoutParams(
                getWindowWidth() / 4,
                getWindowHeight() / 5,
                Gravity.RIGHT or Gravity.BOTTOM
            )
        val cardViewMarginParams = localCard!!.layoutParams as MarginLayoutParams
        cardViewMarginParams.setMargins(30, 0, 60, 40)
        localCard!!.requestLayout()
        txtLocalParticipantName!!.layoutParams = FrameLayout.LayoutParams(120, 120, Gravity.CENTER)
        txtLocalParticipantName!!.textSize = 24f
        txtLocalParticipantName!!.gravity = Gravity.CENTER
        tvLocalParticipantName!!.visibility = View.GONE
//        val layoutParams = FrameLayout.LayoutParams(50, 50, Gravity.RIGHT)
//        layoutParams.setMargins(0, 12, 12, 0)
//        img_localActiveSpeaker!!.layoutParams = layoutParams
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            txtLocalParticipantName!!.foregroundGravity = Gravity.CENTER
        }
        participantCard!!.visibility = View.VISIBLE
    }

    private fun hideParticipantCard() {
        localCard!!.layoutParams =
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        val cardViewMarginParams = localCard!!.layoutParams as MarginLayoutParams
        cardViewMarginParams.setMargins(30, 5, 30, 8)
        localCard!!.requestLayout()
        txtLocalParticipantName!!.layoutParams = FrameLayout.LayoutParams(220, 220, Gravity.CENTER)
        txtLocalParticipantName!!.textSize = 40f
        txtLocalParticipantName!!.gravity = Gravity.CENTER
        tvLocalParticipantName!!.visibility = View.VISIBLE
//        val layoutParams = FrameLayout.LayoutParams(75, 75, Gravity.RIGHT)
//        layoutParams.setMargins(0, 30, 30, 0)
//        img_localActiveSpeaker!!.layoutParams = layoutParams
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            txtLocalParticipantName!!.foregroundGravity = Gravity.CENTER
        }
        participantCard!!.visibility = View.GONE
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
                this@OneToOneCallActivity,
                "You didn't give permission to capture the screen.",
                Toast.LENGTH_SHORT
            ).show()
            localScreenShare = false
            return
        }
        meeting!!.enableScreenShare(data,true)
    }

    private fun updatePresenter(participantId: String?) {
        if (participantId == null) {
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
        screenshareTrack = shareStream.track as VideoTrack
        if (participantName != null) txtLocalParticipantName!!.text =
            participantName!!.substring(0, 1)
        tvName!!.text = "$participantName is presenting"
        ivParticipantMicStatus!!.visibility = View.GONE
        ivParticipantNetwork!!.visibility = View.VISIBLE
        onTrackChange()
        checkStream(participant, ivLocalNetwork!!)

        onTrackChange()
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
                    val track = stream.track as VideoTrack
                    screenshareTrack = null
                    participantVideoView!!.removeTrack()
                    participantVideoView!!.visibility = View.GONE
                    removeTrack(participantTrack, true)
                    txtLocalParticipantName!!.text =
                        meeting!!.localParticipant.displayName.substring(0, 1)
                    tvName!!.text = participantName
                    ivParticipantMicStatus!!.visibility = View.VISIBLE
                    checkStream(
                        participant,
                        ivParticipantNetwork!!
                    )
                    checkStream(meeting!!.localParticipant, ivLocalNetwork!!)

                    onTrackChange()
                    screenshareEnabled = false
                    localScreenShare = false
                }
            }
        })
    }

    private val permissionHandler: com.nabinbhandari.android.permissions.PermissionHandler = object : com.nabinbhandari.android.permissions.PermissionHandler() {
        override fun onGranted() {
        }

        override fun onDenied(context: Context, deniedPermissions: ArrayList<String>) {
            super.onDenied(context, deniedPermissions)
            Toast.makeText(
                this@OneToOneCallActivity,
                "Permission(s) not granted. Some feature may not work", Toast.LENGTH_SHORT
            ).show()
        }

        override fun onBlocked(context: Context, blockedList: ArrayList<String>): Boolean {
            Toast.makeText(
                this@OneToOneCallActivity,
                "Permission(s) not granted. Some feature may not work", Toast.LENGTH_SHORT
            ).show()
            return super.onBlocked(context, blockedList)
        }


    }


    private val permissionHandlerSDK: PermissionHandler = object :
        PermissionHandler() {
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
        val optionsSDK = Permissions.Options().setRationaleDialogTitle("Info").setSettingsDialogTitle("Warning")
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
        Toast.makeText(this@OneToOneCallActivity, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    private fun toggleMic() {
        if (micEnabled) {
            meeting!!.muteMic()
        } else {
            val audioCustomTrack = VideoSDK.createAudioTrack("high_quality", this)
            meeting!!.unmuteMic(audioCustomTrack)
        }
        micEnabled = !micEnabled
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
        webcamEnabled = !webcamEnabled
        toggleWebcamIcon(webcamEnabled)
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


    private fun showLeaveOrEndDialog() {
        val optionsArrayList: ArrayList<ListItem> = ArrayList<ListItem>()
        val leaveMeeting =
            AppCompatResources.getDrawable(this@OneToOneCallActivity, R.drawable.ic_leave)?.let {
                ListItem(
                    "Leave",
                    "Only you will leave the call",
                    it
                )
            }
        val endMeeting =
            AppCompatResources.getDrawable(this@OneToOneCallActivity, R.drawable.ic_end_meeting)
                ?.let {
                    ListItem(
                        "End",
                        "End call for all the participants",
                        it
                    )
                }
        optionsArrayList.add(leaveMeeting!!)
        optionsArrayList.add(endMeeting!!)
        val arrayAdapter: ArrayAdapter<*> = LeaveOptionListAdapter(
            this@OneToOneCallActivity,
            R.layout.leave_options_list_layout,
            optionsArrayList
        )
        val materialAlertDialogBuilder =
            MaterialAlertDialogBuilder(this@OneToOneCallActivity, R.style.AlertDialogCustom)
                .setAdapter(
                    arrayAdapter
                ) { _: DialogInterface?, which: Int ->
                    when (which) {
                        0 -> {
                            unSubscribeTopics()
                            meeting!!.leave()
                        }
                        1 -> {
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
        listView.addFooterView(View(this@OneToOneCallActivity))
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
        var audioDeviceListItem: ListItem? = null
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
            this@OneToOneCallActivity,
            R.layout.audio_device_list_layout,
            audioDeviceList
        )
        val materialAlertDialogBuilder =
            MaterialAlertDialogBuilder(this@OneToOneCallActivity, R.style.AlertDialogCustom)
                .setAdapter(arrayAdapter) { _: DialogInterface?, which: Int ->
                    var audioDevice: AppRTCAudioManager.AudioDevice? = null
                    when (audioDeviceList[which]!!.itemName) {
                        "Bluetooth" -> audioDevice = AppRTCAudioManager.AudioDevice.BLUETOOTH
                        "Wired headset" -> audioDevice =
                            AppRTCAudioManager.AudioDevice.WIRED_HEADSET
                        "Speaker phone" -> audioDevice =
                            AppRTCAudioManager.AudioDevice.SPEAKER_PHONE
                        "Earpiece" -> audioDevice = AppRTCAudioManager.AudioDevice.EARPIECE
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
        listView.addFooterView(View(this@OneToOneCallActivity))
        listView.dividerHeight = 2
        val wmlp = alertDialog.window!!.attributes
        wmlp.gravity = Gravity.BOTTOM or Gravity.LEFT
        val layoutParams = WindowManager.LayoutParams()
        layoutParams.copyFrom(alertDialog.window!!.attributes)
        layoutParams.width = Math.round(getWindowWidth() * 0.6).toInt()
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
        alertDialog.window!!.attributes = layoutParams
        alertDialog.show()
    }

    private fun showMoreOptionsDialog() {
        val participantSize = meeting!!.participants.size + 1
        val moreOptionsArrayList: ArrayList<ListItem> = ArrayList<ListItem>()
        val start_screen_share = ListItem(
            "Share screen",
            AppCompatResources.getDrawable(this@OneToOneCallActivity, R.drawable.ic_screen_share)!!
        )
        val stop_screen_share = ListItem(
            "Stop screen share",
            AppCompatResources.getDrawable(this@OneToOneCallActivity, R.drawable.ic_screen_share)!!
        )
        val start_recording = ListItem(
            "Start recording",
            AppCompatResources.getDrawable(this@OneToOneCallActivity, R.drawable.ic_recording)!!
        )
        val stop_recording = ListItem(
            "Stop recording",
            AppCompatResources.getDrawable(this@OneToOneCallActivity, R.drawable.ic_recording)!!
        )
        val participant_list = ListItem(
            "Participants ($participantSize)",
            AppCompatResources.getDrawable(this@OneToOneCallActivity, R.drawable.ic_people)!!
        )
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
            this@OneToOneCallActivity,
            R.layout.more_options_list_layout,
            moreOptionsArrayList
        )
        val materialAlertDialogBuilder =
            MaterialAlertDialogBuilder(this@OneToOneCallActivity, R.style.AlertDialogCustom)
                .setAdapter(
                    arrayAdapter
                ) { _: DialogInterface?, which: Int ->
                    when (which) {
                        0 -> {
                            toggleScreenSharing()
                        }
                        1 -> {
                            toggleRecording()
                        }
                        2 -> {
                            openParticipantList()
                        }
                    }
                }
        val alertDialog = materialAlertDialogBuilder.create()
        val listView = alertDialog.listView
        listView.divider =
            ColorDrawable(ContextCompat.getColor(this, R.color.md_grey_200)) // set color
        listView.setFooterDividersEnabled(false)
        listView.addFooterView(View(this@OneToOneCallActivity))
        listView.dividerHeight = 2
        val wmlp = alertDialog.window!!.attributes
        wmlp.gravity = Gravity.BOTTOM or Gravity.RIGHT
        val layoutParams = WindowManager.LayoutParams()
        layoutParams.copyFrom(alertDialog.window!!.attributes)
        layoutParams.width = (getWindowWidth() * 0.8).roundToInt()
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
        alertDialog.window!!.attributes = layoutParams
        alertDialog.show()
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

    @Deprecated("This method has been deprecated in favor of using the\n      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      The OnBackPressedDispatcher controls how back button events are dispatched\n      to one or more {@link OnBackPressedCallback} objects.")
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
        if (participantVideoView != null) {
            participantVideoView!!.visibility = View.GONE
            participantVideoView!!.releaseSurfaceViewRenderer()
        }
        if (localVideoView != null) {
            localVideoView!!.visibility = View.GONE
            localVideoView!!.releaseSurfaceViewRenderer()
        }
        timer.cancel()
        super.onDestroy()
    }

    private fun unSubscribeTopics() {
        if (meeting != null) {
            meeting!!.pubSub.unsubscribe("CHAT", chatListener)
        }
    }

    private fun onTrackChange() {
        if (screenshareTrack != null) {
            if (meeting!!.participants.size == 0) {
                showParticipantCard()
                if (localTrack != null) {
                    localVideoView!!.addTrack(localTrack)
                    localVideoView!!.visibility = View.VISIBLE
                }
            } else {
                if (localTrack != null) {
                    localVideoView!!.removeTrack()
                    localVideoView!!.visibility = View.GONE
                }
                if (participantTrack != null) {
                    participantVideoView!!.removeTrack()
                    localVideoView!!.addTrack(participantTrack)
                    if (participantName != null) txtLocalParticipantName!!.text =
                        participantName!!.substring(0, 1)
                    localVideoView!!.visibility = View.VISIBLE
                }
            }
            if (localScreenShare) {
                participantCard!!.visibility = View.GONE
                findViewById<View>(R.id.localScreenShareView).visibility = View.VISIBLE
            } else {
                participantVideoView!!.addTrack(screenshareTrack)
                participantVideoView!!.visibility = View.VISIBLE
            }
        } else {
            if (participantTrack != null) {
                participantVideoView!!.visibility = View.VISIBLE
                participantVideoView!!.addTrack(participantTrack)
//                (img_participantActiveSpeaker as View).bringToFront()
            }
            if (localTrack != null) {
                localVideoView!!.visibility = View.VISIBLE
                localVideoView!!.setZOrderMediaOverlay(true)
                localVideoView!!.addTrack(localTrack)
//                (img_localActiveSpeaker as View?)!!.bringToFront()
                (localCard as View?)!!.bringToFront()
            }
        }
    }

    private fun removeTrack(track: VideoTrack?, isLocal: Boolean) {
        if (screenshareTrack == null) {
            participantCard!!.visibility = View.VISIBLE
            findViewById<View>(R.id.localScreenShareView).visibility = View.GONE
            if (isLocal) {
                localVideoView!!.removeTrack()
                localVideoView!!.visibility = View.GONE
            } else {
                participantVideoView!!.removeTrack()
                participantVideoView!!.visibility = View.GONE
            }
        } else {
            if (!isLocal) {
                localVideoView!!.removeTrack()
                localVideoView!!.visibility = View.GONE
                onTrackChange()
            } else {
                if (meeting!!.participants.size == 0) {
                    localVideoView!!.removeTrack()
                    localVideoView!!.visibility = View.GONE
                } else {
                    participantVideoView!!.removeTrack()
                    participantVideoView!!.visibility = View.GONE
                    onTrackChange()
                }
            }
        }
    }

    private fun checkStream(participant: Participant, imageView: ImageView) {
        if (participant.streams.isNotEmpty()) {
            for (stream: Stream in participant.streams.values) {
                if (stream.kind.equals("video", ignoreCase = true) || stream.kind.equals(
                        "audio",
                        ignoreCase = true
                    )
                ) {
                    imageView.visibility = View.VISIBLE
                    break
                } else {
                    imageView.visibility = View.GONE
                }
            }
        } else imageView.visibility = View.GONE
    }

    private val participantEventListener: ParticipantEventListener =
        object : ParticipantEventListener() {
            override fun onStreamEnabled(stream: Stream) {
                if (stream.kind.equals("video", ignoreCase = true)) {
                    if (meeting!!.participants.size < 2) {
                        val track = stream.track as VideoTrack
                        participantTrack = track
                        onTrackChange()
                        setQuality("high")
                        val participantList = getAllParticipants()
                        val participant = participantList[0]
                        if (screenshareEnabled) {
                            checkStream(participant, ivLocalNetwork!!)
                        } else checkStream(participant, ivParticipantNetwork!!)
                    }
                }
                if (stream.kind.equals("audio", ignoreCase = true)) {
                    if (meeting!!.participants.size >= 2) {
                        stream.pause()
                    } else {
                        ivParticipantMicStatus!!.setImageResource(R.drawable.ic_audio_on)
                        val participantList = getAllParticipants()
                        val participant = participantList[0]
                        if (screenshareEnabled) {
                            checkStream(participant, ivLocalNetwork!!)
                        } else checkStream(participant, ivParticipantNetwork!!)
                    }
                }
            }

            override fun onStreamDisabled(stream: Stream) {
                if (stream.kind.equals("video", ignoreCase = true)) {
                    if (meeting!!.participants.size < 2) {
                        val track: VideoTrack = stream.track as VideoTrack
                        participantTrack = null
                        removeTrack(track, false)
                        val participantList = getAllParticipants()
                        val participant = participantList[0]
                        if (screenshareEnabled) {
                            checkStream(participant, ivLocalNetwork!!)
                        } else checkStream(participant, ivParticipantNetwork!!)
                    }
                }
                if (stream.kind.equals("audio", ignoreCase = true)) {
                    if (meeting!!.participants.size >= 2) {
                        stream.pause()
                    } else {
                        ivParticipantMicStatus!!.setImageResource(R.drawable.ic_audio_off)
                        val participantList = getAllParticipants()
                        val participant = participantList[0]
                        if (screenshareEnabled) {
                            checkStream(participant, ivLocalNetwork!!)
                        } else checkStream(participant, ivParticipantNetwork!!)
                    }
                }
            }
        }

    private fun setQuality(quality: String) {
        val participants: Iterator<Participant> = meeting!!.participants.values.iterator()
        for (i in 0 until meeting!!.participants.size) {
            val participant = participants.next()
            participant.quality = quality
        }
    }

    private fun setLocalListeners() {
        meeting!!.localParticipant.addEventListener(object : ParticipantEventListener() {
            override fun onStreamEnabled(stream: Stream) {
                if (stream.kind.equals("video", ignoreCase = true)) {
                    val track = stream.track as VideoTrack
                    localTrack = track
                    onTrackChange()
                    if (screenshareEnabled) {
                        if (getAllParticipants().size == 0) {
                            checkStream(
                                meeting!!.localParticipant,
                                ivLocalNetwork!!
                            )
                        }
                    } else checkStream(
                        meeting!!.localParticipant,
                        ivLocalNetwork!!
                    )

                } else if (stream.kind.equals("audio", ignoreCase = true)) {
//                    ivLocalParticipantMicStatus!!.setImageResource(R.drawable.ic_audio_on);
                    toggleMicIcon(true)

//                    ivLocalParticipantMicStatus.setImageResource(R.drawable.ic_audio_on);
                    if (screenshareEnabled) {
                        if (getAllParticipants().size == 0) {
                            checkStream(
                                meeting!!.localParticipant,
                                ivLocalNetwork!!
                            )
                        }
                    } else checkStream(
                        meeting!!.localParticipant,
                        ivLocalNetwork!!
                    )

                } else if (stream.kind.equals("share", ignoreCase = true)) {
                    // display share video
                    val videoTrack = stream.track as VideoTrack
                    screenshareTrack = videoTrack
                    if (participantName != null) txtLocalParticipantName!!.text =
                        participantName!!.substring(0, 1)
                    tvName!!.visibility = View.GONE
                    screenShareParticipantNameSnackbar = Snackbar.make(
                        findViewById(R.id.mainLayout), "You started presenting",
                        Snackbar.LENGTH_SHORT
                    )
                    HelperClass.setSnackBarStyle(screenShareParticipantNameSnackbar!!.view, 0)
                    screenShareParticipantNameSnackbar!!.isGestureInsetBottomIgnored = true
                    screenShareParticipantNameSnackbar!!.view.setOnClickListener { screenShareParticipantNameSnackbar!!.dismiss() }
                    screenShareParticipantNameSnackbar!!.show()

                    ivParticipantMicStatus!!.visibility = View.GONE
                    if (screenshareEnabled) {
                        if (getAllParticipants().size == 0) {
                            checkStream(
                                meeting!!.localParticipant,
                                ivLocalNetwork!!
                            )
                        } else {
                            val participantList = getAllParticipants()
                            val participant = participantList[0]
                            checkStream(participant, ivLocalNetwork!!)
                        }
                    } else {
                        if (getAllParticipants().size == 0) checkStream(
                            meeting!!.localParticipant,
                            ivLocalNetwork!!
                        ) else {
                            val participantList = getAllParticipants()
                            val participant = participantList[0]
                            checkStream(participant, ivLocalNetwork!!)
                        }
                    }


                    onTrackChange()
                    //
                    localScreenShare = true
                    screenshareEnabled = true
                }
            }

            override fun onStreamDisabled(stream: Stream) {
                if (stream.kind.equals("video", ignoreCase = true)) {
                    val track: VideoTrack = stream.track as VideoTrack
                    localTrack = null
                    removeTrack(track, true)
                    toggleWebcamIcon(false)
                    if (screenshareEnabled) {
                        if (getAllParticipants().size == 0) {
                            checkStream(
                                meeting!!.localParticipant,
                                ivLocalNetwork!!
                            )
                        }
                    } else checkStream(
                        meeting!!.localParticipant,
                        ivLocalNetwork!!
                    )

                } else if (stream.kind.equals("audio", ignoreCase = true)) {
//                    ivLocalParticipantMicStatus!!.setImageResource(R.drawable.ic_audio_off);
                    toggleMicIcon(false)
                    if (screenshareEnabled) {
                        if (getAllParticipants().size == 0) {
                            checkStream(
                                meeting!!.localParticipant,
                                ivLocalNetwork!!
                            )
                        }
                    } else checkStream(
                        meeting!!.localParticipant,
                        ivLocalNetwork!!
                    )

                } else if (stream.kind.equals("share", ignoreCase = true)) {
                    screenshareTrack = null
                    participantVideoView!!.removeTrack()
                    participantVideoView!!.visibility = View.GONE
                    if (meeting!!.participants.isEmpty()) hideParticipantCard()
                    removeTrack(participantTrack, true)
                    txtLocalParticipantName!!.text =
                        meeting!!.localParticipant.displayName.substring(0, 1)
                    tvName!!.visibility = View.VISIBLE
                    ivParticipantMicStatus!!.visibility = View.VISIBLE
                    checkStream(
                        meeting!!.localParticipant,
                        ivLocalNetwork!!
                    )
                    if (getAllParticipants().size > 0) {
                        val participantList = getAllParticipants()
                        val participant = participantList[0]
                        checkStream(participant, ivParticipantNetwork!!)
                    }
                    onTrackChange()
                    //
                    localScreenShare = false
                    screenshareEnabled = false
                }
            }
        })
    }

    private fun openParticipantList() {
        val participantsListView: RecyclerView
        val close: ImageView
        bottomSheetDialog = BottomSheetDialog(this)
        val v3: View = LayoutInflater.from(applicationContext)
            .inflate(R.layout.layout_participants_list_view, findViewById(R.id.layout_participants))
        bottomSheetDialog!!.setContentView(v3)
        participantsListView = v3.findViewById(R.id.rvParticipantsLinearView)
        (v3.findViewById<View>(R.id.participant_heading) as TextView).typeface =
            RobotoFont().getTypeFace(
                this@OneToOneCallActivity
            )
        close = v3.findViewById(R.id.ic_close)
        participantsListView.minimumHeight = getWindowHeight()
        bottomSheetDialog!!.show()
        close.setOnClickListener { bottomSheetDialog!!.dismiss() }
        meeting!!.addEventListener(meetingEventListener)
        participants = getAllParticipants()
        participantsListView.layoutManager = LinearLayoutManager(applicationContext)
        participantsListView.adapter =
            ParticipantListAdapter(participants, meeting!!, this@OneToOneCallActivity)
        participantsListView.setHasFixedSize(true)
    }

    private fun getWindowHeight(): Int {
        // Calculate window height for fullscreen use
        val displayMetrics = DisplayMetrics()
        (this@OneToOneCallActivity).windowManager.defaultDisplay.getMetrics(displayMetrics)
        return displayMetrics.heightPixels
    }

    private fun getWindowWidth(): Int {
        // Calculate window height for fullscreen use
        val displayMetrics = DisplayMetrics()
        (this@OneToOneCallActivity).windowManager.defaultDisplay.getMetrics(displayMetrics)
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
        val mBottomSheetCallback: BottomSheetBehavior.BottomSheetCallback =
            object : BottomSheetBehavior.BottomSheetCallback() {
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
        etmessage!!.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
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
                    this@OneToOneCallActivity, "Please Enter Message",
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
        val alertDialog = MaterialAlertDialogBuilder(
            this@OneToOneCallActivity,
            R.style.AlertDialogCustom
        ).create()
        alertDialog.setCanceledOnTouchOutside(false)
        val inflater = this.layoutInflater
        val dialogView: View = inflater.inflate(R.layout.alert_dialog_layout, null)
        alertDialog.setView(dialogView)
        val title = dialogView.findViewById<View>(R.id.title) as TextView
        title.visibility = View.GONE
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
        val alertDialog = MaterialAlertDialogBuilder(
            this@OneToOneCallActivity,
            R.style.AlertDialogCustom
        ).create()
        alertDialog.setCanceledOnTouchOutside(false)
        val inflater = this.layoutInflater
        val dialogView: View = inflater.inflate(R.layout.alert_dialog_layout, null)
        alertDialog.setView(dialogView)
        val title = dialogView.findViewById<View>(R.id.title) as TextView
        title.visibility = View.GONE
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