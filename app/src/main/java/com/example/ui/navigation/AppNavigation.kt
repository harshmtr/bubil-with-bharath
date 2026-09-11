package com.example.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.InsightsScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ResultScreen
import com.example.ui.screens.ScannerScreen
import com.example.ui.theme.GoogleBlue
import com.example.ui.viewmodel.HistoryViewModel
import com.example.ui.viewmodel.ScannerViewModel

sealed class Screen(val route: String, val title: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    data object Home : Screen("home", "Home", Icons.Filled.Home, Icons.Outlined.Home)
    data object Scanner : Screen("scanner", "Scan", Icons.Filled.CenterFocusStrong, Icons.Outlined.CenterFocusStrong)
    data object History : Screen("history", "History", Icons.Filled.History, Icons.Outlined.History)
    data object Insights : Screen("insights", "Insights", Icons.Filled.Insights, Icons.Outlined.Insights)
    data object Profile : Screen("profile", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
    data object Result : Screen("result", "Result", Icons.Filled.CenterFocusStrong, Icons.Outlined.CenterFocusStrong)
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Scanner,
    Screen.History,
    Screen.Insights,
    Screen.Profile
)

@Composable
fun MainAppScaffold(
    navController: NavHostController = rememberNavController(),
    scannerViewModel: ScannerViewModel = viewModel(),
    historyViewModel: HistoryViewModel = viewModel()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Hide bottom bar on Scanner screen and Result screen for immersion
    val showBottomBar = currentRoute in bottomNavItems.map { it.route } && currentRoute != Screen.Scanner.route

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    bottomNavItems.forEach { screen ->
                        val isSelected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = screen.title
                                )
                            },
                            label = { Text(screen.title) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = GoogleBlue.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.testTag("nav_item_${screen.route}")
                        )
                    }
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    scannerViewModel = scannerViewModel,
                    historyViewModel = historyViewModel,
                    onNavigateToScanner = { navController.navigate(Screen.Scanner.route) },
                    onNavigateToResult = { navController.navigate(Screen.Result.route) },
                    onNavigateToHistory = { navController.navigate(Screen.History.route) }
                )
            }

            composable(Screen.Scanner.route) {
                ScannerScreen(
                    viewModel = scannerViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToResult = { navController.navigate(Screen.Result.route) }
                )
            }

            composable(Screen.Result.route) {
                ResultScreen(
                    viewModel = scannerViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.History.route) {
                HistoryScreen(
                    historyViewModel = historyViewModel,
                    scannerViewModel = scannerViewModel,
                    onNavigateToResult = { navController.navigate(Screen.Result.route) }
                )
            }

            composable(Screen.Insights.route) {
                InsightsScreen(
                    historyViewModel = historyViewModel
                )
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    scannerViewModel = scannerViewModel,
                    historyViewModel = historyViewModel
                )
            }
        }
    }
}
