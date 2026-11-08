package app.lifelog.designsystem.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The app's motion tokens. Every animation uses these instead of its own values, so motion
 * feels like one system.
 *
 * Rules (from Emil Kowalski's design-engineering guidance):
 * - Things entering or leaving use [EaseOut]; things moving on screen use [EaseInOut].
 *   Never ease-in: it delays the moment the user is watching.
 * - UI motion stays under 300 ms, and exits are faster than entrances.
 * - Only transform (scale, translation) and alpha are animated.
 * - Nothing animates from scale 0; entrances start at 0.95 or more, combined with a fade.
 * - Things seen many times a day (period swipes, tab switches) get the fastest, subtlest
 *   motion. Rare moments (the Premium screen) may take a little more.
 *
 * The system "Remove animations" setting (animator duration scale 0) is honoured by Compose
 * itself: all of these finish instantly.
 */
object Motion {
    /** Strong ease-out for anything entering, leaving or responding to a press. */
    val EaseOut: Easing = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)

    /** Strong ease-in-out for elements moving or morphing on screen. */
    val EaseInOut: Easing = CubicBezierEasing(0.77f, 0f, 0.175f, 1f)

    /** Drawer-style curve for screens sliding in. */
    val EaseDrawer: Easing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)

    /** Press feedback. */
    const val PRESS = 120

    /** Small changes: a selection, a status text, a tab switch. */
    const val SHORT = 180

    /** Medium changes: a banner collapsing, chart bars moving, a view switch. */
    const val MEDIUM = 240

    /** Screens entering. Leaving uses [SHORT]. */
    const val SCREEN = 280

    /** Delay between items entering one after another. */
    const val STAGGER = 40

    /** Scale of a pressed button, card or chip. */
    const val PRESSED_SCALE = 0.97f

    fun <T> enter(durationMillis: Int = SHORT, delayMillis: Int = 0): FiniteAnimationSpec<T> =
        tween(durationMillis, delayMillis, EaseOut)

    fun <T> exit(durationMillis: Int = SHORT): FiniteAnimationSpec<T> = tween(durationMillis, easing = EaseOut)

    fun <T> move(durationMillis: Int = MEDIUM): FiniteAnimationSpec<T> = tween(durationMillis, easing = EaseInOut)
}

/**
 * Fades the element in and lifts it [offset] into place when it first appears, [index] steps
 * after the first element of its group (a 40 ms stagger, capped so long lists don't keep the
 * user waiting). Only for screens people open rarely, never for lists seen many times a day.
 * Never blocks input: the element is fully interactive while it settles.
 */
fun Modifier.enterOnce(index: Int, offset: Dp = 8.dp): Modifier = composed {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, Motion.enter(Motion.MEDIUM, delayMillis = index.coerceAtMost(MAX_STAGGER_STEPS) * Motion.STAGGER))
    }
    val offsetPx = with(LocalDensity.current) { offset.toPx() }
    graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * offsetPx
    }
}

private const val MAX_STAGGER_STEPS = 6

/**
 * Shrinks the element slightly while it is pressed, so a tap is answered the instant the
 * finger lands rather than on release. Pass the same [interactionSource] to the element's
 * `clickable`/`selectable`/button.
 */
fun Modifier.pressScale(
    interactionSource: InteractionSource,
    pressedScale: Float = Motion.PRESSED_SCALE,
): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = Motion.enter(Motion.PRESS),
        label = "pressScale",
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
