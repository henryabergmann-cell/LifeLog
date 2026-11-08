package app.lifelog.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import app.lifelog.designsystem.motion.Motion

private typealias Scope = AnimatedContentTransitionScope<NavBackStackEntry>

/**
 * Screen transitions. Each kind of screen enters and leaves along the same path, so where a
 * screen went tells you where it will come back from:
 * - Switching tabs (done many times a day) is a quick cross-fade with no movement.
 * - Detail screens slide in a short way from the end edge and slide back out the same way.
 * - The appointment editor rises from the bottom like a sheet and sinks back down. The
 *   screen below dims instead of moving.
 * Leaving is always faster than arriving. Back gestures scrub these transitions, so they
 * can be grabbed and reversed midway.
 */
internal object NavTransitions {
    val enter: Scope.() -> EnterTransition = {
        when {
            isTabSwitch() -> fadeIn(Motion.enter(Motion.SHORT))
            targetState.isEditor() -> slideInVertically(tween(Motion.SCREEN, easing = Motion.EaseDrawer)) { it / 12 } +
                fadeIn(Motion.enter(Motion.SHORT))
            else -> slideInHorizontally(tween(Motion.SCREEN, easing = Motion.EaseDrawer)) { it / 10 } +
                fadeIn(Motion.enter(Motion.SHORT))
        }
    }

    val exit: Scope.() -> ExitTransition = {
        when {
            isTabSwitch() -> fadeOut(Motion.exit(Motion.SHORT / 2))
            targetState.isEditor() -> fadeOut(Motion.exit(Motion.SCREEN), targetAlpha = DIMMED)
            else -> slideOutHorizontally(Motion.exit(Motion.SCREEN)) { -it / 20 } + fadeOut(Motion.exit(Motion.SHORT))
        }
    }

    val popEnter: Scope.() -> EnterTransition = {
        when {
            initialState.isEditor() -> fadeIn(Motion.enter(Motion.SHORT), initialAlpha = DIMMED)
            else -> slideInHorizontally(Motion.enter(Motion.SCREEN)) { -it / 20 } + fadeIn(Motion.enter(Motion.SHORT))
        }
    }

    val popExit: Scope.() -> ExitTransition = {
        when {
            initialState.isEditor() -> slideOutVertically(Motion.exit(Motion.SHORT)) { it / 12 } + fadeOut(Motion.exit(Motion.SHORT))
            else -> slideOutHorizontally(Motion.exit(Motion.SHORT)) { it / 10 } + fadeOut(Motion.exit(Motion.SHORT))
        }
    }

    private const val DIMMED = 0.6f
}

internal fun NavDestination.isTopLevel(): Boolean =
    TopLevelDestination.entries.any { destination -> hierarchy.any { it.hasRoute(destination.routeClass) } }

private fun Scope.isTabSwitch(): Boolean = initialState.destination.isTopLevel() && targetState.destination.isTopLevel()

private fun NavBackStackEntry.isEditor(): Boolean = destination.hasRoute(EventEditorRoute::class)
