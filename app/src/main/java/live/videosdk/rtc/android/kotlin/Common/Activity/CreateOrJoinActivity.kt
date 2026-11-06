package live.videosdk.rtc.android.kotlin.Common.Activity

import live.videosdk.rtc.android.kotlin.Common.sdkScope

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.crashlytics.internal.Logger.TAG
import live.videosdk.rtc.android.CustomStreamTrack
import live.videosdk.rtc.android.VideoSDK
import live.videosdk.rtc.android.VideoView
import live.videosdk.rtc.android.kotlin.Common.Fragment.CreateMeetingFragment
import live.videosdk.rtc.android.kotlin.Common.Fragment.CreateOrJoinFragment
import live.videosdk.rtc.android.kotlin.Common.Fragment.JoinMeetingFragment
import live.videosdk.rtc.android.kotlin.R
import live.videosdk.rtc.android.mediaDevice.FacingMode
import live.videosdk.rtc.android.mediaDevice.CameraDeviceInfo
import live.videosdk.rtc.android.permission.Permission
import live.videosdk.rtc.android.permission.PermissionHandler
import live.videosdk.rtc.android.permission.Permissions
import videosdk.org.webrtc.VideoTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import live.videosdk.rtc.android.CustomCameraTrackConfig


class CreateOrJoinActivity : AppCompatActivity() {
    var isMicEnabled = false
        private set
    var isWebcamEnabled = false
        private set
    private var btnMic: FloatingActionButton? = null
    private var btnWebcam: FloatingActionButton? = null
    private var joinView: VideoView? = null
    private var cameraOffText: TextView? = null
    private var toolbar: Toolbar? = null
    private var actionBar: ActionBar? = null
    private var videoTrack: CustomStreamTrack? = null
    private val uiScope = CoroutineScope(Dispatchers.Main)
    private lateinit var preCallListAdaptor: DeviceAdaptor
    private lateinit var recyclerView: RecyclerView


    var permissionsGranted = false
    lateinit var optionsMenu: Menu
    private val permissionHandler: com.nabinbhandari.android.permissions.PermissionHandler = object : com.nabinbhandari.android.permissions.PermissionHandler() {
        override fun onGranted() {
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


    private val permissionHandlerSDK: PermissionHandler = object :PermissionHandler() {
        override fun onGranted() {
            permissionsGranted = true
            isMicEnabled = true
            btnMic!!.setImageResource(R.drawable.ic_mic_on)
            changeFloatingActionButtonLayout(btnMic, isMicEnabled)
            isWebcamEnabled = true
            btnWebcam!!.setImageResource(R.drawable.ic_video_camera)
            changeFloatingActionButtonLayout(btnWebcam, isWebcamEnabled)
            updateCameraView(null)
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

    @SuppressLint("MissingInflatedId")
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
        joinView = findViewById(R.id.joiningView)
        cameraOffText = findViewById(R.id.cameraoff)
        checkPermissions()
        val fragContainer = findViewById<View>(R.id.fragContainer) as LinearLayout
        val ll = LinearLayout(this)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragContainer, CreateOrJoinFragment(), "CreateOrJoinFragment").commit()
        fragContainer.addView(ll)
        btnMic!!.setOnClickListener { toggleMic() }
        btnWebcam!!.setOnClickListener { toggleWebcam() }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.top_app_bar,menu)
        optionsMenu = menu
        optionsMenu.findItem(R.id.Audio).isVisible = false

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
        when (item.itemId) {
            R.id.Camera -> {
                changeCamera()
            }
            else -> super.onOptionsItemSelected(item)
        }
        return super.onOptionsItemSelected(item)
    }

    private fun changeCamera() {
        // Code to change Camera
        val videoDevices: List<CameraDeviceInfo> = VideoSDK.getCameras()
        val currentDevice = getSelectedOrDefaultVideoDevice()
        if (currentDevice == null) {
            Toast.makeText(this, "No camera device available", Toast.LENGTH_SHORT).show()
            return
        }

        val currentFacingMode = currentDevice.facingMode

        var facingMode = FacingMode.front
        var videoDevice: CameraDeviceInfo? = null

        if(currentFacingMode == FacingMode.front)
        {
            facingMode = FacingMode.back
        }else if(currentFacingMode == FacingMode.back)
        {
            facingMode = FacingMode.front
        }

        for (devices in videoDevices)
        {
            if(devices.facingMode == facingMode)
            {
                videoDevice=devices;
            }
        }

        if(facingMode != FacingMode.front){
            joinView!!.setMirror(false)
        }

        if (videoDevice != null) {
            VideoSDK.setSelectedVideoDevice(videoDevice)
            updateCameraView(videoDevice)
        }
        Toast.makeText(this,"Camera switched",Toast.LENGTH_SHORT).show()
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
        val permissionListSDK: MutableList<Permission> = ArrayList()
        permissionListSDK.add(Permission.audio)
        permissionListSDK.add(Permission.video)
        val optionsSDK = Permissions.Options().apply {
            rationaleDialogTitle = "Info"
            settingsDialogTitle = "Warning"
        }
            VideoSDK.checkPermissions(this,
                permissionListSDK,
                options = optionsSDK,
                permissionHandler = permissionHandlerSDK
            )
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
        updateCameraView(null)
        changeFloatingActionButtonLayout(btnWebcam, isWebcamEnabled)
    }


    private fun updateCameraView(videoDevice: CameraDeviceInfo?) {
        if (isWebcamEnabled) {
            cameraOffText?.visibility= View.GONE
            joinView!!.visibility = View.VISIBLE

            val selectedVideoDevice = videoDevice ?: getSelectedOrDefaultVideoDevice()
            if (selectedVideoDevice == null) {
                Log.d(TAG, "updateCameraView: no video device available")
                cameraOffText?.visibility = View.VISIBLE
                joinView!!.visibility = View.INVISIBLE
                return
            }

            videoTrack?.track?.dispose()
            joinView!!.removeTrack()
            sdkScope.launch {
                videoTrack = VideoSDK.createCameraVideoTrack(
                    encoderConfig = CustomCameraTrackConfig.H720P_W960P,
                    facingMode = selectedVideoDevice.facingMode,
                    multiStream = true,
                    cameraId = selectedVideoDevice.deviceId
                )
                joinView!!.addTrack(videoTrack!!.track as VideoTrack?)
            }
        } else {
            if(videoTrack?.track?.state()?.equals("LIVE") == true){
                videoTrack?.track?.dispose()
                videoTrack?.track?.setEnabled(false)
            }
            videoTrack = null
            joinView!!.removeTrack()
            joinView!!.releaseSurfaceViewRenderer()
            joinView!!.visibility = View.INVISIBLE;
            cameraOffText?.visibility = View.VISIBLE
        }
    }

    private fun getSelectedOrDefaultVideoDevice(): CameraDeviceInfo? {
        VideoSDK.getSelectedVideoDevice()?.let { return it }

        val videoDevices = VideoSDK.getCameras()
        val videoDevice = videoDevices.firstOrNull { it.facingMode == FacingMode.front }
            ?: videoDevices.firstOrNull()

        if (videoDevice != null) {
            VideoSDK.setSelectedVideoDevice(videoDevice)
            joinView?.setMirror(videoDevice.facingMode == FacingMode.front)
        }

        return videoDevice
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy crash")
        if(videoTrack?.track?.state()?.equals("LIVE") == true)
        {
            Log.d(TAG, "onDestroyIf")
            videoTrack?.track?.dispose()
            videoTrack = null
        }
        joinView!!.removeTrack()
        joinView!!.releaseSurfaceViewRenderer()
        super.onDestroy()
    }

    override fun onPause() {
        videoTrack?.track?.dispose()
        videoTrack = null
        joinView!!.removeTrack()
        joinView!!.releaseSurfaceViewRenderer()
        super.onPause()
    }

    override fun onRestart() {
        Log.d(TAG, "onRestart: ")
        updateCameraView(VideoSDK.getSelectedVideoDevice())
        super.onRestart()
    }

}
