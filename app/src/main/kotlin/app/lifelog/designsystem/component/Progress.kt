package app.lifelog.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import app.lifelog.designsystem.motion.Motion
import kotlinx.coroutines.delay

/**
 * A spinner that only appears if loading takes longer than [delayMillis], then fades in.
 * Most screens load from the local database in a few frames; showing a spinner for those
 * frames is a flash that makes the app feel slower than it is.
 */
@Composable
fun DelayedProgressIndicator(modifier: Modifier = Modifier, delayMillis: Long = 300) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMillis)
        visible = true
    }
    AnimatedVisibility(visible = visible, enter = fadeIn(Motion.enter(Motion.SHORT)), modifier = modifier) {
        CircularProgressIndicator()
    }
}
