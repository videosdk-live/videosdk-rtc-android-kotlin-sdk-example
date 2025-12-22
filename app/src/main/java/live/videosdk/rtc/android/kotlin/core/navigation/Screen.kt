package live.videosdk.rtc.android.kotlin.core.navigation

/**
 * Navigation routes for Compose Navigation
 */
sealed class Screen(val route: String) {
    object CreateOrJoin : Screen("create_or_join")
    object GroupCall : Screen("group_call/{meetingId}/{micEnabled}/{webcamEnabled}/{participantName}") {
        fun createRoute(
            meetingId: String,
            micEnabled: Boolean,
            webcamEnabled: Boolean,
            participantName: String
        ) = "group_call/$meetingId/$micEnabled/$webcamEnabled/$participantName"
    }
    object OneToOneCall : Screen("one_to_one_call/{meetingId}/{micEnabled}/{webcamEnabled}/{participantName}") {
        fun createRoute(
            meetingId: String,
            micEnabled: Boolean,
            webcamEnabled: Boolean,
            participantName: String
        ) = "one_to_one_call/$meetingId/$micEnabled/$webcamEnabled/$participantName"
    }
    object HlsViewer : Screen("hls_viewer")
}
