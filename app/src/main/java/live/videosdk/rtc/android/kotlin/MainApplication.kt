package live.videosdk.rtc.android.kotlin

import android.app.Application
import live.videosdk.rtc.android.Meeting
import live.videosdk.rtc.android.VideoSDK
import com.androidnetworking.AndroidNetworking

class MainApplication : Application() {
    var meeting: Meeting? = null
    override fun onCreate() {
        super.onCreate()
        VideoSDK.initialize(applicationContext)
        AndroidNetworking.initialize(applicationContext)
    }
}