package live.videosdk.rtc.android.kotlin.Common.Listener

interface ResponseListener<T> {
    fun onResponse(response: T?)
}