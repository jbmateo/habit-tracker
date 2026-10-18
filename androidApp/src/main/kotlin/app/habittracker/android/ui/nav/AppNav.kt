package app.habittracker.android.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
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

/** Form sheet state: hidden, create (`habitId == null`) or edit. */
private data class FormSheet(val habitId: String?)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNav() {
    val navController = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    var formSheet by remember { mutableStateOf<FormSheet?>(null) }

    val backStack by navController.currentBackStackEntryAsState()
    val destination = backStack?.destination
    val showBar = Tab.entries.any { tab -> destination?.hierarchy?.any { it.route == tab.route } == true }

    val openForm: (String?) -> Unit = { formSheet = FormSheet(it) }
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
            composable(Tab.Calendar.route) { Placeholder("Calendar", padding) }
            composable(Tab.Habits.route) { Placeholder("Habits", padding) }
            composable(Routes.HABIT) { entry ->
                Placeholder("Habit ${entry.arguments?.getString("id")}", padding)
            }
            composable(Routes.CATEGORIES) { Placeholder("Categories", padding) }
        }
    }

    formSheet?.let { sheet ->
        ModalBottomSheet(
            onDismissRequest = { formSheet = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            FormSheetContent(sheet.habitId)
        }
    }
}

/** Placeholder until Task 18 builds the habit form. */
@Composable
private fun FormSheetContent(habitId: String?) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(if (habitId == null) "New habit" else "Edit habit $habitId")
    }
}

@Composable
private fun Placeholder(title: String, padding: PaddingValues) {
    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text(title) }
}
