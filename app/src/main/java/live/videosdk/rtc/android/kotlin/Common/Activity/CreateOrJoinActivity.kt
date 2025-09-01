package live.videosdk.rtc.android.kotlin.Common.Activity

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
import live.videosdk.rtc.android.lib.PeerConnectionUtils
import live.videosdk.rtc.android.mediaDevice.AudioDeviceInfo
import live.videosdk.rtc.android.mediaDevice.FacingMode
import live.videosdk.rtc.android.mediaDevice.VideoDeviceInfo
import live.videosdk.rtc.android.permission.Permission
import live.videosdk.rtc.android.permission.PermissionHandler
import live.videosdk.rtc.android.permission.Permissions
import org.webrtc.Camera1Enumerator
import org.webrtc.PeerConnectionFactory
import org.webrtc.PeerConnectionFactory.InitializationOptions
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoCapturer
import org.webrtc.VideoSource
import org.webrtc.VideoTrack


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
    private var videoCapturer: VideoCapturer? = null
    private var initializationOptions: InitializationOptions? = null
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private lateinit var preCallListAdaptor: DeviceAdaptor
    private lateinit var recyclerView: RecyclerView


    private var videoSource: VideoSource? = null
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
        setAudioDeviceChangeListener()

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
            R.id.Audio -> {
                getAudioDevices()
            }
            else -> super.onOptionsItemSelected(item)
        }
        return super.onOptionsItemSelected(item)
    }

    private var previousAvailableDevices: MutableList<String> = mutableListOf()
    private fun setAudioDeviceChangeListener(){
        VideoSDK.setAudioDeviceChangeListener { selectedAudioDevice, availableAudioDevices ->
            Log.d(TAG, "setAudioDeviceChangeListener: " + selectedAudioDevice.label)

            val currentAvailableDevices = availableAudioDevices.map { it.label }.toMutableList()
            Log.d(TAG, "Current available : $currentAvailableDevices")

            val addedDevices = currentAvailableDevices.filter { it !in previousAvailableDevices }
            Log.d(TAG, "Added audio devices: $addedDevices")

            val removedDevices = previousAvailableDevices.filter { it !in currentAvailableDevices }
            Log.d(TAG, "Removed audio devices: $removedDevices")
            
            previousAvailableDevices = currentAvailableDevices

            if(addedDevices.isNotEmpty() && addedDevices != previousAvailableDevices) {
                Toast.makeText(this, "$addedDevices Connected", Toast.LENGTH_SHORT).show()
            }
            if(removedDevices.isNotEmpty()){
                Toast.makeText(this,"$removedDevices Removed", Toast.LENGTH_SHORT).show()
            }
            when (selectedAudioDevice.label) {
                "BLUETOOTH" -> optionsMenu.findItem(R.id.Audio).setIcon(R.drawable.baseline_bluetooth_connected_24)
                "WIRED_HEADSET" -> optionsMenu.findItem(R.id.Audio).setIcon(R.drawable.baseline_headphones_24)
                "SPEAKER_PHONE" -> optionsMenu.findItem(R.id.Audio).setIcon(R.drawable.baseline_volume_up_24)
                "EARPIECE" -> optionsMenu.findItem(R.id.Audio).setIcon(R.drawable.baseline_call_24)
            }
        }

    }


    private fun changeCamera() {
        // Code to change Camera
        val videoDevices : Set<VideoDeviceInfo>  = VideoSDK.getVideoDevices()

        val currentDevice =  VideoSDK.getSelectedVideoDevice();

        val currentFacingMode = currentDevice.facingMode;

        var facingMode = FacingMode.front
        var videoDevice : VideoDeviceInfo? = null;

        if(currentFacingMode.equals(FacingMode.front))
        {
            facingMode = FacingMode.back
        }else if(currentFacingMode.equals(FacingMode.back))
        {
            facingMode = FacingMode.front
        }

        for (devices in videoDevices)
        {
            if(devices.facingMode.equals(facingMode))
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

    private fun getAudioDevices() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet, null)
        bottomSheetDialog.setContentView(bottomSheetView)
        recyclerView = bottomSheetView.findViewById(R.id.rvItems)
        recyclerView.layoutManager = LinearLayoutManager(this)
        val audioDevice : Set<AudioDeviceInfo> = VideoSDK.getAudioDevices()
        val labels = mutableListOf<String>()

        for (device in audioDevice) {
            val label = device.label
            labels.add(label)
        }
        preCallListAdaptor = DeviceAdaptor(labels) { itemDto: String->
            for (device in audioDevice) {
                if (device.label == itemDto) {
                    Log.d(TAG, "changeAudio: selected called...")
                    VideoSDK.setSelectedAudioDevice(device)
                }
            }
            bottomSheetDialog.cancel()
            Toast.makeText(this, "Selected  $itemDto",Toast.LENGTH_SHORT).show()
        }
        recyclerView.adapter = preCallListAdaptor
        bottomSheetDialog.show()
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
        val permissionList: MutableList<String> = ArrayList()
        permissionList.add(Manifest.permission.INTERNET)
        permissionList.add(Manifest.permission.READ_PHONE_STATE)
        permissionList.add(Manifest.permission.POST_NOTIFICATIONS)
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


    private fun updateCameraView(videoDevice: VideoDeviceInfo?) {
        if (isWebcamEnabled) {
            cameraOffText?.visibility= View.GONE
            joinView!!.visibility = View.VISIBLE
            // create PeerConnectionFactory
            initializationOptions =
                InitializationOptions.builder(this).createInitializationOptions()
            PeerConnectionFactory.initialize(initializationOptions)
            peerConnectionFactory = PeerConnectionFactory.builder().createPeerConnectionFactory()

            val surfaceTextureHelper =
                SurfaceTextureHelper.create("CaptureThread", PeerConnectionUtils.getEglContext())

            videoTrack = VideoSDK.createCameraVideoTrack(
                "h720p_w960p",
                "front",
                CustomStreamTrack.VideoMode.TEXT,
                true,
                this,videoDevice
            )
            // display in localView
            joinView!!.addTrack(videoTrack!!.track as VideoTrack?)
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
        Log.d(TAG, "onDestroy crash")
        if(videoTrack?.track?.state()?.equals("LIVE") == true)
        {
            Log.d(TAG, "onDestroyIf")
            videoTrack?.track?.dispose()
            videoTrack = null
        }
        joinView!!.removeTrack()
        joinView!!.releaseSurfaceViewRenderer()
        closeCapturer()
        super.onDestroy()
    }

    override fun onPause() {
        videoTrack?.track?.dispose()
        videoTrack = null
        joinView!!.removeTrack()
        joinView!!.releaseSurfaceViewRenderer()
        closeCapturer()
        super.onPause()
    }

    override fun onRestart() {
        Log.d(TAG, "onRestart: ")
        updateCameraView(null)
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