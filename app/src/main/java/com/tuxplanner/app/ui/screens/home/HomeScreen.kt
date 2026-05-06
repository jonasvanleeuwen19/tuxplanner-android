package com.tuxplanner.app.ui.screens.home

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tuxplanner.app.ui.navigation.HomeTab
import com.tuxplanner.app.ui.screens.dashboard.DashboardScreen
import com.tuxplanner.app.ui.screens.events.EventsScreen
import com.tuxplanner.app.ui.screens.settings.SettingsScreen
import com.tuxplanner.app.ui.screens.todos.TodosScreen

private data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun HomeScreen(onLogout: () -> Unit, onNavigateToCalendarLists: () -> Unit) {
    val nestedNavController = rememberNavController()

    val navItems = listOf(
        BottomNavItem(HomeTab.Dashboard, "Dashboard", Icons.Default.Home),
        BottomNavItem(HomeTab.Events, "Events", Icons.Default.CalendarMonth),
        BottomNavItem(HomeTab.Todos, "Todos", Icons.Default.CheckCircle),
        BottomNavItem(HomeTab.Settings, "Settings", Icons.Default.Settings)
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by nestedNavController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                navItems.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                        onClick = {
                            nestedNavController.navigate(item.route) {
                                popUpTo(nestedNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = nestedNavController,
            startDestination = HomeTab.Dashboard,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(HomeTab.Dashboard) { DashboardScreen() }
            composable(HomeTab.Events) {
                EventsScreen(onNavigateToCalendarLists = onNavigateToCalendarLists)
            }
            composable(HomeTab.Todos) { TodosScreen() }
            composable(HomeTab.Settings) { SettingsScreen(onLogout = onLogout) }
        }
    }
}
