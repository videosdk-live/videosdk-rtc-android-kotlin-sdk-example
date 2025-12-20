package live.videosdk.rtc.android.kotlin.feature.groupcall.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import live.videosdk.rtc.android.kotlin.core.ui.theme.White

/**
 * Hand raise indicator showing raised hands
 */
@Composable
fun HandRaiseIndicator(
    raisedHands: Map<String, String>,
    modifier: Modifier = Modifier
) {
    if (raisedHands.isNotEmpty()) {
        Row(
            modifier = modifier
                .background(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                    CircleShape
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.PanTool,
                contentDescription = "Raised hands",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(16.dp)
            )
            
            Text(
                text = if (raisedHands.size == 1) {
                    raisedHands.values.first()
                } else {
                    "${raisedHands.values.first()} +${raisedHands.size - 1}"
                },
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
