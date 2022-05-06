package live.videosdk.rtc.android.kotlin

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.nabinbhandari.android.permissions.PermissionHandler
import com.nabinbhandari.android.permissions.Permissions
import live.videosdk.rtc.android.lib.PeerConnectionUtils
import org.webrtc.*
import java.lang.NullPointerException
import java.lang.RuntimeException
import java.util.ArrayList

class JoinActivity : AppCompatActivity() {

    private var micEnabled = false
    private var webcamEnabled = false
    private var btnMic: FloatingActionButton? = null
    private var btnWebcam: FloatingActionButton? = null
    private var svrJoin: SurfaceViewRenderer? = null
    private var etName: EditText? = null

    var videoTrack: VideoTrack? = null
    var videoCapturer: VideoCapturer? = null
    var initializationOptions: PeerConnectionFactory.InitializationOptions? = null
    var peerConnectionFactory: PeerConnectionFactory? = null
    var videoSource: VideoSource? = null
    var permissionsGranted = false


    private val permissionHandler: PermissionHandler = object : PermissionHandler() {
        override fun onGranted() {
            permissionsGranted = true
            micEnabled = true
            btnMic!!.setImageResource(R.drawable.ic_baseline_mic_24)
            changeFloatingActionButtonLayout(btnMic, micEnabled)
            webcamEnabled = true
            btnWebcam!!.setImageResource(R.drawable.ic_baseline_videocam_24)
            changeFloatingActionButtonLayout(btnWebcam, webcamEnabled)
            updateCameraView()
        }

        override fun onDenied(context: Context, deniedPermissions: ArrayList<String>) {
            super.onDenied(context, deniedPermissions)
            Toast.makeText(
                this@JoinActivity,
                "Permission(s) not granted. Some feature may not work", Toast.LENGTH_SHORT
            ).show()
        }

        override fun onBlocked(context: Context, blockedList: ArrayList<String>): Boolean {
            Toast.makeText(
                this@JoinActivity,
                "Permission(s) not granted. Some feature may not work", Toast.LENGTH_SHORT
            ).show()
            return super.onBlocked(context, blockedList)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_join)

        val btnJoin = findViewById<Button>(R.id.btnJoin)
        btnMic = findViewById(R.id.btnMic)
        btnWebcam = findViewById(R.id.btnWebcam)
        svrJoin = findViewById(R.id.svrJoiningView)
        etName = findViewById(R.id.etName)

        checkPermissions()


        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.title = "Join Meeting"
        setSupportActionBar(toolbar)
        val ab = supportActionBar
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true)
        } else {
            throw NullPointerException("Something went wrong")
        }

        btnMic!!.setOnClickListener(View.OnClickListener { v: View? -> toggleMic() })

        btnWebcam!!.setOnClickListener(View.OnClickListener { v: View? -> toggleWebcam() })

        val token = intent.getStringExtra("token")
        val meetingId = intent.getStringExtra("meetingId")
        btnJoin.setOnClickListener { v: View? ->
            if ("" == etName!!.getText().toString()) {
                Toast.makeText(this@JoinActivity, "Please Enter Name", Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(this@JoinActivity, MainActivity::class.java)
                intent.putExtra("token", token)
                intent.putExtra("meetingId", meetingId)
                intent.putExtra("micEnabled", micEnabled)
                intent.putExtra("webcamEnabled", webcamEnabled)
                intent.putExtra("paticipantName", etName!!.getText().toString().trim { it <= ' ' })
                startActivity(intent)
                finish()
            }
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
        micEnabled = !micEnabled
        if (micEnabled) {
            btnMic!!.setImageResource(R.drawable.ic_baseline_mic_24)
        } else {
            btnMic!!.setImageResource(R.drawable.ic_baseline_mic_off_24)
        }
        changeFloatingActionButtonLayout(btnMic, micEnabled)
    }

    private fun toggleWebcam() {
        if (!permissionsGranted) {
            checkPermissions()
            return
        }
        webcamEnabled = !webcamEnabled
        if (webcamEnabled) {
            btnWebcam!!.setImageResource(R.drawable.ic_baseline_videocam_24)
        } else {
            btnWebcam!!.setImageResource(R.drawable.ic_baseline_videocam_off_24)
        }
        updateCameraView()
        changeFloatingActionButtonLayout(btnWebcam, webcamEnabled)
    }

    private fun updateCameraView() {
        if (webcamEnabled) {
            // create PeerConnectionFactory
            initializationOptions =
                PeerConnectionFactory.InitializationOptions.builder(this).createInitializationOptions()
            PeerConnectionFactory.initialize(initializationOptions)
            peerConnectionFactory = PeerConnectionFactory.builder().createPeerConnectionFactory()
            svrJoin!!.init(PeerConnectionUtils.getEglContext(), null)
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
        videoTrack!!.removeSink(svrJoin)
        svrJoin!!.clearImage()
        svrJoin!!.release()
        closeCapturer()
        super.onDestroy()
    }

    private fun closeCapturer() {
        val TAG = "PeerConnectionUtils"
        if (videoCapturer != null) {
            try {
                videoCapturer!!.stopCapture()
            } catch (e: InterruptedException) {
                throw RuntimeException(e)
            }
            videoCapturer!!.dispose()
            videoCapturer = null
        }
        Log.d(TAG, "Stopped capture.")
        if (videoSource != null) {
            videoSource!!.dispose()
            videoSource = null
        }
        if (peerConnectionFactory != null) {
            peerConnectionFactory!!.stopAecDump()
            peerConnectionFactory!!.dispose()
            peerConnectionFactory = null
        }
        Log.d(TAG, "Closed video source.")
        PeerConnectionFactory.stopInternalTracingCapture()
        PeerConnectionFactory.shutdownInternalTracer()
        Log.d(TAG, "Closed peer connection.")
    }
}