package live.videosdk.rtc.android.kotlin.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import live.videosdk.rtc.android.kotlin.feature.createjoin.presentation.CreateOrJoinScreen
import live.videosdk.rtc.android.kotlin.feature.groupcall.presentation.GroupCallScreen
import live.videosdk.rtc.android.kotlin.feature.onetoonecall.presentation.OneToOneCallScreen

/**
 * Main navigation graph for the app
 */
@Composable
fun VideoSDKNavGraph(
    navController: NavHostController,
    token: String
) {
    NavHost(
        navController = navController,
        startDestination = Screen.CreateOrJoin.route
    ) {
        // CreateOrJoin Screen
        composable(Screen.CreateOrJoin.route) {
            CreateOrJoinScreen(
                onNavigateToGroupCall = { meetingId, micEnabled, webcamEnabled, participantName ->
                    navController.navigate(
                        Screen.GroupCall.createRoute(meetingId, micEnabled, webcamEnabled, participantName)
                    )
                },
                onNavigateToOneToOneCall = { meetingId, micEnabled, webcamEnabled, participantName ->
                    navController.navigate(
                        Screen.OneToOneCall.createRoute(meetingId, micEnabled, webcamEnabled, participantName)
                    )
                }
            )
        }

        // Group Call Screen
        composable(
            route = Screen.GroupCall.route,
            arguments = listOf(
                navArgument("meetingId") { type = NavType.StringType },
                navArgument("micEnabled") { type = NavType.BoolType },
                navArgument("webcamEnabled") { type = NavType.BoolType },
                navArgument("participantName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            GroupCallScreen(
                token = token,
                meetingId = backStackEntry.arguments?.getString("meetingId") ?: "",
                initialMicEnabled = backStackEntry.arguments?.getBoolean("micEnabled") ?: true,
                initialWebcamEnabled = backStackEntry.arguments?.getBoolean("webcamEnabled") ?: true,
                participantName = backStackEntry.arguments?.getString("participantName") ?: "Guest",
                onLeaveCall = {
                    navController.popBackStack(Screen.CreateOrJoin.route, inclusive = false)
                }
            )
        }

        // One-to-One Call Screen
        composable(
            route = Screen.OneToOneCall.route,
            arguments = listOf(
                navArgument("meetingId") { type = NavType.StringType },
                navArgument("micEnabled") { type = NavType.BoolType },
                navArgument("webcamEnabled") { type = NavType.BoolType },
                navArgument("participantName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            OneToOneCallScreen(
                token = token,
                meetingId = backStackEntry.arguments?.getString("meetingId") ?: "",
                initialMicEnabled = backStackEntry.arguments?.getBoolean("micEnabled") ?: true,
                initialWebcamEnabled = backStackEntry.arguments?.getBoolean("webcamEnabled") ?: true,
                participantName = backStackEntry.arguments?.getString("participantName") ?: "Guest",
                onLeaveCall = {
                    navController.popBackStack(Screen.CreateOrJoin.route, inclusive = false)
                }
            )
        }
    }
}
