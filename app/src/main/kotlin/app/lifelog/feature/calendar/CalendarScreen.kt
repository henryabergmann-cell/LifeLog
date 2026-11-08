package app.lifelog.feature.calendar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import app.lifelog.designsystem.motion.Motion
import app.lifelog.designsystem.motion.pressScale
import app.lifelog.domain.model.Occurrence
import app.lifelog.feature.premium.UpgradeBanner
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lifelog.R
import app.lifelog.designsystem.format.DateTimeFormats
import app.lifelog.designsystem.format.rememberDateTimeFormats
import app.lifelog.designsystem.icon.LifeLogIcons
import app.lifelog.domain.model.CalendarViewMode
import app.lifelog.ui.LocalSnackbarHostState
import app.lifelog.feature.calendar.agenda.AgendaList
import app.lifelog.feature.calendar.month.MonthPage
import app.lifelog.feature.calendar.week.TimeGrid
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun CalendarRoute(
    onOpenEvent: (Long) -> Unit,
    onCreateEvent: (date: LocalDate, minuteOfDay: Int?) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenPremium: () -> Unit,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        viewModel.refreshToday()
        onPauseOrDispose {}
    }
    CalendarScreen(
        state = state,
        onPrevious = viewModel::onPrevious,
        onNext = viewModel::onNext,
        onToday = viewModel::onToday,
        onViewModeSelected = viewModel::onViewModeSelected,
        onPeriodSettled = viewModel::onPeriodSettled,
        onDaySelected = viewModel::onDaySelected,
        onOpenEvent = onOpenEvent,
        onCreateEvent = onCreateEvent,
        onOpenSearch = onOpenSearch,
        onAgendaLoadEarlier = viewModel::onAgendaLoadEarlier,
        onAgendaLoadLater = viewModel::onAgendaLoadLater,
        onOpenPremium = onOpenPremium,
        onDismissUpgradeBanner = viewModel::onDismissUpgradeBanner,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    state: CalendarUiState,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onViewModeSelected: (CalendarViewMode) -> Unit,
    onPeriodSettled: (LocalDate) -> Unit,
    onDaySelected: (LocalDate) -> Unit,
    onOpenEvent: (Long) -> Unit,
    onCreateEvent: (date: LocalDate, minuteOfDay: Int?) -> Unit,
    onOpenSearch: () -> Unit,
    onAgendaLoadEarlier: () -> Unit,
    onAgendaLoadLater: () -> Unit,
    onOpenPremium: () -> Unit,
    onDismissUpgradeBanner: () -> Unit,
) {
    val formats = rememberDateTimeFormats()
    val ready = state as? CalendarUiState.Ready

    Scaffold(
        topBar = {
            TopAppBar(
                title = { CalendarTitle(ready, formats) },
                actions = {
                    if (ready != null && ready.viewMode != CalendarViewMode.AGENDA) {
                        IconButton(onClick = onPrevious) {
                            Icon(LifeLogIcons.ChevronLeft, contentDescription = stringResource(previousLabel(ready.viewMode)))
                        }
                        IconButton(onClick = onNext) {
                            Icon(LifeLogIcons.ChevronRight, contentDescription = stringResource(nextLabel(ready.viewMode)))
                        }
                    }
                    IconButton(onClick = onToday) {
                        Icon(LifeLogIcons.Today, contentDescription = stringResource(R.string.calendar_today))
                    }
                    IconButton(onClick = onOpenSearch) {
                        Icon(LifeLogIcons.Search, contentDescription = stringResource(R.string.calendar_search))
                    }
                    if (ready != null) ViewModeMenu(ready.viewMode, onViewModeSelected)
                },
            )
        },
        floatingActionButton = {
            if (ready != null) {
                val interaction = remember { MutableInteractionSource() }
                FloatingActionButton(
                    onClick = { onCreateEvent(ready.selectedDate, null) },
                    interactionSource = interaction,
                    modifier = Modifier.pressScale(interaction, pressedScale = 0.94f),
                ) {
                    Icon(LifeLogIcons.Add, contentDescription = stringResource(R.string.calendar_new_event))
                }
            }
        },
        snackbarHost = { SnackbarHost(LocalSnackbarHostState.current) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Dismissing folds the banner away rather than letting the calendar jump up.
            AnimatedVisibility(
                visible = ready?.showUpgradeBanner == true,
                enter = fadeIn(Motion.enter(Motion.MEDIUM)) + expandVertically(Motion.move()),
                exit = fadeOut(Motion.exit(Motion.SHORT)) + shrinkVertically(Motion.move()),
            ) {
                UpgradeBanner(onLearnMore = onOpenPremium, onDismiss = onDismissUpgradeBanner)
            }
            CalendarContent(ready, formats, onOpenEvent, onCreateEvent, onPeriodSettled, onDaySelected, onAgendaLoadEarlier, onAgendaLoadLater)
        }
    }
}

@Composable
private fun ColumnScope.CalendarContent(
    ready: CalendarUiState.Ready?,
    formats: DateTimeFormats,
    onOpenEvent: (Long) -> Unit,
    onCreateEvent: (date: LocalDate, minuteOfDay: Int?) -> Unit,
    onPeriodSettled: (LocalDate) -> Unit,
    onDaySelected: (LocalDate) -> Unit,
    onAgendaLoadEarlier: () -> Unit,
    onAgendaLoadLater: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .weight(1f),
    ) {
        if (ready == null) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
            return@Box
        }
        val openOccurrence: (Occurrence) -> Unit = { onOpenEvent(it.event.id) }
        // Switching views (an occasional action) settles in from a hair smaller instead of
        // swapping in one frame. Swiping between periods is the pager's own motion.
        AnimatedContent(
            targetState = ready.viewMode,
            transitionSpec = {
                (fadeIn(Motion.enter(Motion.MEDIUM)) + scaleIn(Motion.enter(Motion.MEDIUM), initialScale = 0.97f))
                    .togetherWith(fadeOut(Motion.exit(Motion.SHORT / 2)))
            },
            label = "calendarViewMode",
            modifier = Modifier.fillMaxSize(),
        ) { viewMode ->
            when (viewMode) {
                CalendarViewMode.AGENDA -> AgendaList(
                    state = ready,
                    formats = formats,
                    onEventClick = openOccurrence,
                    onLoadEarlier = onAgendaLoadEarlier,
                    onLoadLater = onAgendaLoadLater,
                )
                else -> CalendarPager(viewMode, ready, onPeriodSettled) { periodStart ->
                    when (viewMode) {
                        CalendarViewMode.MONTH -> MonthPage(
                            month = YearMonth.from(periodStart),
                            firstDayOfWeek = ready.firstDayOfWeek,
                            today = ready.today,
                            occurrences = ready.occurrences,
                            categories = ready.categories,
                            formats = formats,
                            onDayClick = onDaySelected,
                        )
                        CalendarViewMode.WEEK -> TimeGrid(
                            days = CalendarPeriods.weekDays(periodStart),
                            today = ready.today,
                            occurrences = ready.occurrences,
                            categories = ready.categories,
                            formats = formats,
                            onEventClick = openOccurrence,
                            onSlotClick = { date, minute -> onCreateEvent(date, minute) },
                            onDayHeaderClick = onDaySelected,
                        )
                        else -> TimeGrid(
                            days = listOf(periodStart),
                            today = ready.today,
                            occurrences = ready.occurrences,
                            categories = ready.categories,
                            formats = formats,
                            onEventClick = openOccurrence,
                            onSlotClick = { date, minute -> onCreateEvent(date, minute) },
                            onDayHeaderClick = null,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Swipeable periods. The pager and the selected date in the ViewModel follow each other.
 * [mode] is passed separately from [state] so a view that is fading out keeps its own layout.
 */
@Composable
private fun CalendarPager(
    mode: CalendarViewMode,
    state: CalendarUiState.Ready,
    onPeriodSettled: (LocalDate) -> Unit,
    page: @Composable (periodStart: LocalDate) -> Unit,
) {
    val firstDay = state.firstDayOfWeek
    key(mode, firstDay) {
        val pageCount = remember { CalendarPeriods.pageCount(mode, firstDay) }
        val targetPage = CalendarPeriods.pageFor(mode, state.selectedDate, firstDay)
        val pagerState = rememberPagerState(initialPage = targetPage) { pageCount }
        val currentOnSettled by rememberUpdatedState(onPeriodSettled)

        LaunchedEffect(targetPage) {
            if (pagerState.settledPage != targetPage) pagerState.animateScrollToPage(targetPage)
        }
        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.settledPage }.collect {
                currentOnSettled(CalendarPeriods.dateForPage(mode, it, firstDay))
            }
        }
        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 1,
            key = { it },
            modifier = Modifier.fillMaxSize(),
        ) { index ->
            page(CalendarPeriods.dateForPage(mode, index, firstDay))
        }
    }
}

@Composable
private fun ViewModeMenu(current: CalendarViewMode, onSelected: (CalendarViewMode) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val currentLabel = stringResource(viewModeLabel(current))
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = viewModeIcon(current),
                contentDescription = stringResource(R.string.calendar_change_view, currentLabel),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            CalendarViewMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(stringResource(viewModeLabel(mode))) },
                    leadingIcon = { Icon(viewModeIcon(mode), contentDescription = null) },
                    trailingIcon = if (mode == current) {
                        { Icon(LifeLogIcons.Check, contentDescription = stringResource(R.string.state_selected)) }
                    } else {
                        null
                    },
                    onClick = {
                        expanded = false
                        onSelected(mode)
                    },
                )
            }
        }
    }
}

/**
 * The period title. When the period changes the new title slides in a few pixels from the
 * direction of travel, so a swipe reads as moving through time. Picking another day in the
 * same month keeps the same text and doesn't animate.
 */
@Composable
private fun CalendarTitle(ready: CalendarUiState.Ready?, formats: DateTimeFormats) {
    AnimatedContent(
        targetState = ready?.let { TitleKey(title(it, formats), it.selectedDate) },
        transitionSpec = {
            val direction = if ((targetState?.date ?: LocalDate.MIN) >= (initialState?.date ?: LocalDate.MIN)) 1 else -1
            (fadeIn(Motion.enter(Motion.SHORT)) + slideInHorizontally(Motion.enter(Motion.SHORT)) { direction * it / 8 })
                .togetherWith(
                    fadeOut(Motion.exit(Motion.SHORT / 2)) +
                        slideOutHorizontally(Motion.exit(Motion.SHORT / 2)) { -direction * it / 8 },
                )
                .using(SizeTransform(clip = false))
        },
        contentKey = { it?.text },
        label = "calendarTitle",
    ) { key ->
        Text(
            text = key?.text.orEmpty(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.semantics { heading() },
        )
    }
}

private data class TitleKey(val text: String, val date: LocalDate)

private fun title(state: CalendarUiState.Ready, formats: DateTimeFormats): String {
    val text = when (state.viewMode) {
        CalendarViewMode.DAY -> formats.shortWeekdayDayMonth.format(state.selectedDate)
        CalendarViewMode.WEEK -> formats.monthYear.format(
            CalendarPeriods.periodStart(CalendarViewMode.WEEK, state.selectedDate, state.firstDayOfWeek),
        )
        CalendarViewMode.MONTH, CalendarViewMode.AGENDA -> formats.monthYear.format(state.selectedDate)
    }
    return text.replaceFirstChar { it.titlecase(formats.locale) }
}

private fun viewModeLabel(mode: CalendarViewMode): Int = when (mode) {
    CalendarViewMode.MONTH -> R.string.calendar_view_month
    CalendarViewMode.WEEK -> R.string.calendar_view_week
    CalendarViewMode.DAY -> R.string.calendar_view_day
    CalendarViewMode.AGENDA -> R.string.calendar_view_agenda
}

private fun viewModeIcon(mode: CalendarViewMode): ImageVector = when (mode) {
    CalendarViewMode.MONTH -> LifeLogIcons.ViewMonth
    CalendarViewMode.WEEK -> LifeLogIcons.ViewWeek
    CalendarViewMode.DAY -> LifeLogIcons.ViewDay
    CalendarViewMode.AGENDA -> LifeLogIcons.ViewAgenda
}

private fun previousLabel(mode: CalendarViewMode): Int = when (mode) {
    CalendarViewMode.MONTH -> R.string.calendar_previous_month
    CalendarViewMode.WEEK -> R.string.calendar_previous_week
    else -> R.string.calendar_previous_day
}

private fun nextLabel(mode: CalendarViewMode): Int = when (mode) {
    CalendarViewMode.MONTH -> R.string.calendar_next_month
    CalendarViewMode.WEEK -> R.string.calendar_next_week
    else -> R.string.calendar_next_day
}
