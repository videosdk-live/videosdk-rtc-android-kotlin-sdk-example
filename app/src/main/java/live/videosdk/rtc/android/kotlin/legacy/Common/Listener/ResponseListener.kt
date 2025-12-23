package live.videosdk.rtc.android.kotlin.legacy.Common.Listener

interface ResponseListener<T> {
    fun onResponse(response: T?)
}