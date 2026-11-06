package live.videosdk.rtc.android.kotlin.Common

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import live.videosdk.rtc.android.lib.VideoSDKException

var sdkAppContext: Context? = null

val sdkScope = CoroutineScope(
    SupervisorJob() + Dispatchers.Main.immediate + CoroutineExceptionHandler { _, error ->
        val detail = if (error is VideoSDKException) "${error.error.code} ${error.error.message}" else error.toString()
        Log.e("VideoSDK", "SDK call failed: $detail", error)
        sdkAppContext?.let { Handler(Looper.getMainLooper()).post { Toast.makeText(it, detail, Toast.LENGTH_LONG).show() } }
    }
)
