package app.lifelog.ui

import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.rememberNavigationSuiteScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.lifelog.R
import app.lifelog.navigation.LifeLogNavHost
import app.lifelog.navigation.TopLevelDestination
import app.lifelog.navigation.isTopLevel

/** One snackbar host for the whole app, so messages survive navigation between screens. */
val LocalSnackbarHostState = staticCompositionLocalOf { SnackbarHostState() }

/**
 * App shell: a bottom navigation bar on phones and a navigation rail on wider windows
 * (NavigationSuiteScaffold picks the layout from the window size class). The bar slides away
 * on screens below the top level (detail, editor, search).
 */
@Composable
fun LifeLogApp(
    navController: NavHostController = rememberNavController(),
    shellViewModel: ShellViewModel = hiltViewModel(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val isTopLevel = currentDestination?.isTopLevel() ?: true
    val snackbarHostState = remember { SnackbarHostState() }
    // The bar slides away (and back) with the screen transition instead of vanishing in one frame.
    val suiteState = rememberNavigationSuiteScaffoldState()
    LaunchedEffect(isTopLevel) {
        if (isTopLevel) suiteState.show() else suiteState.hide()
    }

    UndoDeleteSnackbar(shellViewModel, snackbarHostState)

    CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
        NavigationSuiteScaffold(
            layoutType = NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfo()),
            state = suiteState,
            navigationSuiteItems = {
                TopLevelDestination.entries.forEach { destination ->
                    val selected = currentDestination?.hierarchy?.any { it.hasRoute(destination.routeClass) } == true
                    item(
                        selected = selected,
                        onClick = { navController.navigateToTopLevel(destination) },
                        // The visible label already names the destination for TalkBack.
                        icon = { Icon(painterResource(destination.icon), contentDescription = null) },
                        label = { Text(stringResource(destination.label)) },
                    )
                }
            },
        ) {
            LifeLogNavHost(navController = navController)
        }
    }
}

@Composable
private fun UndoDeleteSnackbar(viewModel: ShellViewModel, hostState: SnackbarHostState) {
    val deleted by viewModel.lastDeleted.collectAsStateWithLifecycle()
    val resources = LocalContext.current.resources
    LaunchedEffect(deleted) {
        val pending = deleted ?: return@LaunchedEffect
        val result = hostState.showSnackbar(
            message = resources.getString(R.string.event_deleted, pending.title),
            actionLabel = resources.getString(R.string.action_undo),
            duration = SnackbarDuration.Long,
        )
        if (result == SnackbarResult.ActionPerformed) viewModel.onUndo(pending) else viewModel.onUndoDismissed(pending)
    }
}

private fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
