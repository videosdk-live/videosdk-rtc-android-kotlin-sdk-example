package live.videosdk.rtc.android.kotlin.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import live.videosdk.rtc.android.kotlin.core.ui.theme.GreyMd1000
import live.videosdk.rtc.android.kotlin.core.ui.theme.White

/**
 * Reusable control button for call controls (mic, camera, etc.)
 * 
 * @param icon Icon to display
 * @param onClick Click handler
 * @param backgroundColor Background color
 * @param iconColor Icon tint color
 * @param size Button size
 * @param contentDescription Accessibility description
 */
@Composable
fun ControlButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = GreyMd1000,
    iconColor: Color = White,
    size: Dp = 50.dp,
    contentDescription: String? = null
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = backgroundColor,
        modifier = modifier.size(size)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = iconColor
            )
        }
    }
}
