package live.videosdk.rtc.android.kotlin.Common.Listener

interface ResponseListener {
    fun onResponse(meetingId: String?)
    fun onMeetingTimeChanged(meetingTime: Int)
}