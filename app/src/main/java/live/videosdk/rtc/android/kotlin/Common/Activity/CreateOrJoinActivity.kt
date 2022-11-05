package live.videosdk.rtc.android.kotlin.Common.Activity

import android.Manifest
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.nabinbhandari.android.permissions.PermissionHandler
import com.nabinbhandari.android.permissions.Permissions
import live.videosdk.rtc.android.kotlin.Common.Fragment.CreateMeetingFragment
import live.videosdk.rtc.android.kotlin.Common.Fragment.CreateOrJoinFragment
import live.videosdk.rtc.android.kotlin.Common.Fragment.JoinMeetingFragment
import live.videosdk.rtc.android.kotlin.R
import live.videosdk.rtc.android.lib.PeerConnectionUtils
import org.webrtc.*
import org.webrtc.PeerConnectionFactory.InitializationOptions


class CreateOrJoinActivity : AppCompatActivity() {
    var isMicEnabled = false
        private set
    var isWebcamEnabled = false
        private set
    private var btnMic: FloatingActionButton? = null
    private var btnWebcam: FloatingActionButton? = null
    private var svrJoin: SurfaceViewRenderer? = null
    private var toolbar: Toolbar? = null
    private var actionBar: ActionBar? = null
    private var videoTrack: VideoTrack? = null
    private var videoCapturer: VideoCapturer? = null
    private var initializationOptions: InitializationOptions? = null
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var videoSource: VideoSource? = null
    var permissionsGranted = false
    private val permissionHandler: PermissionHandler = object : PermissionHandler() {
        override fun onGranted() {
            permissionsGranted = true
            isMicEnabled = true
            btnMic!!.setImageResource(R.drawable.ic_mic_on)
            changeFloatingActionButtonLayout(btnMic, isMicEnabled)
            isWebcamEnabled = true
            btnWebcam!!.setImageResource(R.drawable.ic_video_camera)
            changeFloatingActionButtonLayout(btnWebcam, isWebcamEnabled)
            updateCameraView()
        }

        override fun onDenied(context: Context, deniedPermissions: ArrayList<String>) {
            super.onDenied(context, deniedPermissions)
            Toast.makeText(
                this@CreateOrJoinActivity,
                "Permission(s) not granted. Some feature may not work", Toast.LENGTH_SHORT
            ).show()
        }

        override fun onBlocked(context: Context, blockedList: ArrayList<String>): Boolean {
            Toast.makeText(
                this@CreateOrJoinActivity,
                "Permission(s) not granted. Some feature may not work", Toast.LENGTH_SHORT
            ).show()
            return super.onBlocked(context, blockedList)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_or_join)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        toolbar = findViewById(R.id.toolbar)
        toolbar!!.title = "VideoSDK RTC"
        setSupportActionBar(toolbar)
        actionBar = supportActionBar
        btnMic = findViewById(R.id.btnMic)
        btnWebcam = findViewById(R.id.btnWebcam)
        svrJoin = findViewById(R.id.svrJoiningView)
        checkPermissions()
        val fragContainer = findViewById<View>(R.id.fragContainer) as LinearLayout
        val ll = LinearLayout(this)
        ll.orientation = LinearLayout.HORIZONTAL
        supportFragmentManager.beginTransaction()
            .add(R.id.fragContainer, CreateOrJoinFragment(), "CreateOrJoinFragment").commit()
        fragContainer.addView(ll)
        btnMic!!.setOnClickListener { toggleMic() }
        btnWebcam!!.setOnClickListener { toggleWebcam() }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            supportFragmentManager.addOnBackStackChangedListener {
                if (supportFragmentManager.backStackEntryCount > 0) {
                    actionBar!!.setDisplayHomeAsUpEnabled(true)
                } else {
                    actionBar!!.setDisplayHomeAsUpEnabled(false)
                }
                toolbar!!.invalidate()
            }
            supportFragmentManager.popBackStack()
        }
        return super.onOptionsItemSelected(item)
    }

    fun createMeetingFragment() {
        setActionBar()
        val ft = supportFragmentManager.beginTransaction()
        ft.replace(R.id.fragContainer, CreateMeetingFragment(), "CreateMeetingFragment")
        ft.addToBackStack("CreateOrJoinFragment")
        ft.commit()
    }

    fun joinMeetingFragment() {
        setActionBar()
        val ft = supportFragmentManager.beginTransaction()
        ft.replace(R.id.fragContainer, JoinMeetingFragment(), "JoinMeetingFragment")
        ft.addToBackStack("CreateOrJoinFragment")
        ft.commit()
    }

    private fun setActionBar() {
        if (actionBar != null) {
            actionBar!!.setDisplayHomeAsUpEnabled(true)
        } else {
            throw NullPointerException("Something went wrong")
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

    private fun changeFloatingActionButtonLayout(btn: FloatingActionButton?, enabled: Boolean) {
        if (enabled) {
            btn!!.setColorFilter(Color.BLACK)
            btn.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.md_grey_300))
        } else {
            btn!!.setColorFilter(Color.WHITE)
            btn.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.md_red_500))
        }
    }

    private fun toggleMic() {
        if (!permissionsGranted) {
            checkPermissions()
            return
        }
        isMicEnabled = !isMicEnabled
        if (isMicEnabled) {
            btnMic!!.setImageResource(R.drawable.ic_mic_on)
        } else {
            btnMic!!.setImageResource(R.drawable.ic_mic_off)
        }
        changeFloatingActionButtonLayout(btnMic, isMicEnabled)
    }

    private fun toggleWebcam() {
        if (!permissionsGranted) {
            checkPermissions()
            return
        }
        isWebcamEnabled = !isWebcamEnabled
        if (isWebcamEnabled) {
            btnWebcam!!.setImageResource(R.drawable.ic_video_camera)
        } else {
            btnWebcam!!.setImageResource(R.drawable.ic_video_camera_off)
        }
        updateCameraView()
        changeFloatingActionButtonLayout(btnWebcam, isWebcamEnabled)
    }

    private fun updateCameraView() {
        if (isWebcamEnabled) {
            // create PeerConnectionFactory
            initializationOptions =
                InitializationOptions.builder(this).createInitializationOptions()
            PeerConnectionFactory.initialize(initializationOptions)
            peerConnectionFactory = PeerConnectionFactory.builder().createPeerConnectionFactory()
            svrJoin!!.init(PeerConnectionUtils.getEglContext(), null)
            svrJoin!!.setMirror(true)
            val surfaceTextureHelper =
                SurfaceTextureHelper.create("CaptureThread", PeerConnectionUtils.getEglContext())

            // create VideoCapturer
            videoCapturer = createCameraCapturer()
            videoSource = peerConnectionFactory!!.createVideoSource(videoCapturer!!.isScreencast)
            videoCapturer!!.initialize(
                surfaceTextureHelper,
                applicationContext,
                videoSource!!.capturerObserver
            )
            videoCapturer!!.startCapture(480, 640, 30)

            // create VideoTrack
            videoTrack = peerConnectionFactory!!.createVideoTrack("100", videoSource)

            // display in localView
            videoTrack!!.addSink(svrJoin)
        } else {
            if (videoTrack != null) videoTrack!!.removeSink(svrJoin)
            svrJoin!!.clearImage()
            svrJoin!!.release()
        }
    }

    private fun createCameraCapturer(): VideoCapturer? {
        val enumerator = Camera1Enumerator(false)
        val deviceNames = enumerator.deviceNames

        // First, try to find front facing camera
        for (deviceName in deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                val videoCapturer: VideoCapturer? = enumerator.createCapturer(deviceName, null)
                if (videoCapturer != null) {
                    return videoCapturer
                }
            }
        }

        // Front facing camera not found, try something else
        for (deviceName in deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                val videoCapturer: VideoCapturer? = enumerator.createCapturer(deviceName, null)
                if (videoCapturer != null) {
                    return videoCapturer
                }
            }
        }
        return null
    }

    override fun onDestroy() {
        if (videoTrack != null) videoTrack!!.removeSink(svrJoin)
        svrJoin!!.clearImage()
        svrJoin!!.release()
        closeCapturer()
        super.onDestroy()
    }

    override fun onPause() {
        if (videoTrack != null) videoTrack!!.removeSink(svrJoin)
        svrJoin!!.clearImage()
        svrJoin!!.release()
        closeCapturer()
        super.onPause()
    }

    override fun onRestart() {
        updateCameraView()
        super.onRestart()
    }

    private fun closeCapturer() {
        if (videoCapturer != null) {
            try {
                videoCapturer!!.stopCapture()
            } catch (e: InterruptedException) {
                throw RuntimeException(e)
            }
            videoCapturer!!.dispose()
            videoCapturer = null
        }
        if (videoSource != null) {
            videoSource!!.dispose()
            videoSource = null
        }
        if (peerConnectionFactory != null) {
            peerConnectionFactory!!.stopAecDump()
            peerConnectionFactory!!.dispose()
            peerConnectionFactory = null
        }
        PeerConnectionFactory.stopInternalTracingCapture()
        PeerConnectionFactory.shutdownInternalTracer()
    }
}