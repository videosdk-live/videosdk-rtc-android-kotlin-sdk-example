package live.videosdk.rtc.android.kotlin.feature.createjoin.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Options for creating or joining a meeting
 */
@Composable
fun CreateOrJoinOptions(
    onCreateMeeting: () -> Unit,
    onJoinMeeting: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onCreateMeeting,
            modifier = Modifier.weight(1f)
        ) {
            Text("Create Meeting")
        }

        Button(
            onClick = onJoinMeeting,
            modifier = Modifier.weight(1f)
        ) {
            Text("Join Meeting")  
        }
    }
}
