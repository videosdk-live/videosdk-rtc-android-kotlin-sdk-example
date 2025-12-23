package live.videosdk.rtc.android.kotlin.Common

import android.app.Application
import live.videosdk.rtc.android.VideoSDK
import com.androidnetworking.AndroidNetworking

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        VideoSDK.setEnableActivityLifecycle(false)
        VideoSDK.initialize(applicationContext)
        AndroidNetworking.initialize(applicationContext)
    }
}