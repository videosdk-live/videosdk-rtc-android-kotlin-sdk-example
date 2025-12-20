package live.videosdk.rtc.android.kotlin.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = ColorPrimary,
    onPrimary = White,
    primaryContainer = ColorPrimaryVariant,
    onPrimaryContainer = White,
    
    secondary = GreyMd300,
    onSecondary = Black,
    secondaryContainer = GreyMd700,
    onSecondaryContainer = White,
    
    tertiary = BlueGreyMd200,
    onTertiary = Black,
    
    error = RedMd400,
    onError = White,
    errorContainer = RedMd500,
    onErrorContainer = White,
    
    background = ColorBackground,
    onBackground = White,
    
    surface = ColorPrimaryVariant,
    onSurface = White,
    surfaceVariant = GreyMd1000,
    onSurfaceVariant = TextColor,
    
    outline = GreyMd200,
    outlineVariant = GreyMd100A
)

private val LightColorScheme = lightColorScheme(
    primary = ColorPrimary,
    onPrimary = White,
    primaryContainer = ColorPrimaryVariant,
    onPrimaryContainer = White,
    
    secondary = GreyMd700,
    onSecondary = White,
    secondaryContainer = GreyMd300,
    onSecondaryContainer = Black,
    
    tertiary = BlueGreyMd200,
    onTertiary = Black,
    
    error = RedMd500,
    onError = White,
    errorContainer = RedMd400,
    onErrorContainer = Black,
    
    background = White,
    onBackground = Black,
    
    surface = GreyMd300,
    onSurface = Black,
    surfaceVariant = GreyMd200,
    onSurfaceVariant = GreyMd2000,
    
    outline = GreyMd700,
    outlineVariant = GreyMd100A
)

/**
 * VideoSDK Material3 Theme
 * 
 * @param darkTheme Whether to use dark theme (default: true for video call apps)
 * @param content The composable content
 */
@Composable
fun VideoSDKTheme(
    darkTheme: Boolean = true, // Default to dark theme for video apps
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = VideoSDKTypography,
        content = content
    )
}
