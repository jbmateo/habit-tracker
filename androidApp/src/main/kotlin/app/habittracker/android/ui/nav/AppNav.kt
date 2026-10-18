package app.habittracker.android.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.Lifecycle
import app.habittracker.android.ui.calendar.CalendarScreen
import app.habittracker.android.ui.categories.CategoryListScreen
import app.habittracker.android.ui.detail.HabitDetailScreen
import app.habittracker.android.ui.form.HabitFormSheet
import app.habittracker.android.ui.habits.HabitListScreen
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.habittracker.android.ui.dashboard.DashboardScreen

private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    Today("today", "Today", Icons.Rounded.Today),
    Calendar("calendar", "Calendar", Icons.Rounded.CalendarMonth),
    Habits("habits", "Habits", Icons.Rounded.Checklist),
}

private object Routes {
    const val HABIT = "habit/{id}"
    const val CATEGORIES = "categories"
    fun habit(id: String) = "habit/$id"
}

/**
 * Form sheet state: create (`habitId == null`) or edit. [openId] is unique per open so each opening gets its own
 * form ViewModel.
 */
private data class FormSheet(val habitId: String?, val openId: Int)

/** Survives rotation and process death. Ids are never empty, so "" encodes create. */
private val FormSheetSaver: Saver<FormSheet?, Any> = Saver(
    save = { sheet -> sheet?.let { arrayListOf<Any>(it.habitId.orEmpty(), it.openId) } },
    restore = { saved ->
        @Suppress("UNCHECKED_CAST")
        val list = saved as List<Any>
        FormSheet((list[0] as String).ifEmpty { null }, list[1] as Int)
    },
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNav() {
    val navController = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var formSheet by rememberSaveable(stateSaver = FormSheetSaver) { mutableStateOf<FormSheet?>(null) }
    var formOpens by rememberSaveable { mutableIntStateOf(0) }

    val backStack by navController.currentBackStackEntryAsState()
    val destination = backStack?.destination
    val showBar = Tab.entries.any { tab -> destination?.hierarchy?.any { it.route == tab.route } == true }

    val openForm: (String?) -> Unit = { formSheet = FormSheet(it, ++formOpens) }
    val openHabit: (String) -> Unit = { navController.navigate(Routes.habit(it)) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (showBar) {
                NavigationBar {
                    Tab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = destination?.hierarchy?.any { it.route == tab.route } == true,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(navController, startDestination = Tab.Today.route) {
            composable(Tab.Today.route) {
                DashboardScreen(
                    snackbar = snackbar,
                    onAddHabit = { openForm(null) },
                    onOpenHabit = openHabit,
                    contentPadding = padding,
                )
            }
            composable(Tab.Calendar.route) { CalendarScreen(snackbar, padding) }
            composable(Tab.Habits.route) {
                HabitListScreen(
                    onOpenHabit = openHabit,
                    onAddHabit = { openForm(null) },
                    onOpenCategories = { navController.navigate(Routes.CATEGORIES) },
                    contentPadding = padding,
                )
            }
            composable(Routes.HABIT) { entry ->
                HabitDetailScreen(
                    habitId = requireNotNull(entry.arguments?.getString("id")),
                    snackbar = snackbar,
                    // Pop only while this entry is still the visible destination, so a second close signal
                    // arriving mid-transition cannot pop an unrelated screen.
                    onClose = {
                        if (entry.lifecycle.currentState == Lifecycle.State.RESUMED) navController.popBackStack()
                    },
                    onEdit = openForm,
                    contentPadding = padding,
                )
            }
            composable(Routes.CATEGORIES) {
                CategoryListScreen(snackbar, onBack = { navController.popBackStack() }, contentPadding = padding)
            }
        }
    }

    formSheet?.let { sheet ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val hideSheet: () -> Unit = {
            scope.launch { sheetState.hide() }.invokeOnCompletion { formSheet = null }
        }
        ModalBottomSheet(onDismissRequest = { formSheet = null }, sheetState = sheetState) {
            HabitFormSheet(habitId = sheet.habitId, sheetKey = sheet.openId, onDismiss = hideSheet)
        }
    }
}
