package halogen.samples.chromecast

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import halogen.chromecast.HandoffState
import kotlinx.coroutines.delay

/**
 * One-shot "Now playing on [device]" toast that pulses once on every transition to
 * [HandoffState.Acknowledged] and auto-dismisses after [autoDismissMs] milliseconds.
 */
@Composable
fun HandoffToast(
    state: HandoffState,
    modifier: Modifier = Modifier,
    autoDismissMs: Long = 1_800L,
) {
    var visible by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var pulseTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(state) {
        when (state) {
            is HandoffState.Acknowledged -> {
                message = "Now playing on ${state.deviceName}"
                visible = true
                pulseTrigger++
                delay(autoDismissMs)
                visible = false
            }
            is HandoffState.Sending -> {
                message = "Casting theme…"
                visible = true
            }
            is HandoffState.Failed -> {
                message = "Couldn't cast theme"
                visible = true
                delay(autoDismissMs)
                visible = false
            }
            HandoffState.Disconnected -> {
                visible = false
            }
            else -> {}
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (pulseTrigger % 2 == 1) 1.06f else 1.0f,
        animationSpec = tween(durationMillis = 325, easing = FastOutSlowInEasing),
        label = "pulse",
    )

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
        modifier = modifier.padding(top = 48.dp),
    ) {
        Text(
            text = message.orEmpty(),
            color = Color.White,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .scale(scale)
                .widthIn(min = 200.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(999.dp))
                .padding(horizontal = 20.dp, vertical = 12.dp),
        )
        Spacer(Modifier.height(8.dp))
    }
}
