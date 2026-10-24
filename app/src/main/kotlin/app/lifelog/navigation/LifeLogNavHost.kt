package app.lifelog.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import app.lifelog.feature.categories.CategoriesRoute
import app.lifelog.feature.event.detail.EventDetailRoute
import app.lifelog.feature.event.editor.EventEditorRoute
import app.lifelog.feature.insights.InsightsScreen
import app.lifelog.feature.journal.JournalScreen
import app.lifelog.feature.premium.PremiumScreen
import app.lifelog.feature.search.SearchRoute
import app.lifelog.feature.settings.SettingsScreen
import app.lifelog.feature.calendar.CalendarRoute as CalendarScreenRoute
import app.lifelog.navigation.CategoriesRoute as CategoriesDestination
import app.lifelog.navigation.EventDetailRoute as EventDetailDestination
import app.lifelog.navigation.EventEditorRoute as EventEditorDestination
import app.lifelog.navigation.SearchRoute as SearchDestination

@Composable
fun LifeLogNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val openPremium: () -> Unit = { navController.navigate(PremiumRoute) { launchSingleTop = true } }

    NavHost(
        navController = navController,
        startDestination = CalendarRoute,
        modifier = modifier,
    ) {
        composable<CalendarRoute> {
            CalendarScreenRoute(
                onOpenEvent = { navController.navigate(EventDetailDestination(it)) },
                onCreateEvent = { date, minute ->
                    navController.navigate(
                        EventEditorDestination(dateEpochDay = date.toEpochDay(), startMinute = minute ?: -1),
                    )
                },
                onOpenSearch = { navController.navigate(SearchDestination) },
                onOpenPremium = openPremium,
            )
        }
        composable<JournalRoute> { JournalScreen(onOpenPremium = openPremium) }
        composable<InsightsRoute> { InsightsScreen(onOpenPremium = openPremium) }
        composable<SettingsRoute> {
            SettingsScreen(
                onOpenCategories = { navController.navigate(CategoriesDestination) },
                onOpenPremium = openPremium,
            )
        }
        composable<EventDetailDestination> {
            EventDetailRoute(
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(EventEditorDestination(eventId = it)) },
                onDuplicate = { navController.navigate(EventEditorDestination(duplicateOfId = it)) },
            )
        }
        composable<EventEditorDestination> {
            EventEditorRoute(
                onClose = { navController.popBackStack() },
                onManageCategories = { navController.navigate(CategoriesDestination) },
            )
        }
        composable<SearchDestination> {
            SearchRoute(
                onBack = { navController.popBackStack() },
                onOpenEvent = { navController.navigate(EventDetailDestination(it)) },
            )
        }
        composable<CategoriesDestination> {
            CategoriesRoute(onBack = { navController.popBackStack() }, onOpenPremium = openPremium)
        }
        composable<PremiumRoute> {
            PremiumScreen(onBack = { navController.popBackStack() })
        }
    }
}
