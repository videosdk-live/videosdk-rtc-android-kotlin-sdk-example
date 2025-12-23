package live.videosdk.rtc.android.kotlin.legacy.Common.Activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import live.videosdk.rtc.android.kotlin.BuildConfig
import live.videosdk.rtc.android.kotlin.core.navigation.VideoSDKNavGraph
import live.videosdk.rtc.android.kotlin.core.ui.theme.VideoSDKTheme

/**
 * Main entry activity - now using Compose Navigation
 * 
 * This replaces the old XML-based CreateOrJoinActivity with a fully Compose implementation.
 * Uses MVVM architecture with feature-based structure.
 */
class CreateOrJoinActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Get token from BuildConfig
        val token = BuildConfig.AUTH_TOKEN
        
        setContent {
            VideoSDKTheme {
                val navController = rememberNavController()
                
                VideoSDKNavGraph(
                    navController = navController,
                    token = token
                )
            }
        }
    }
}