package com.invest.easymoney.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.invest.easymoney.ui.alerts.AlertsScreen
import com.invest.easymoney.ui.detail.StockDetailScreen
import com.invest.easymoney.ui.home.HomeScreen
import com.invest.easymoney.ui.watchlist.WatchlistScreen

sealed class Screen(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Home : Screen("home", "Home", Icons.Filled.Home, Icons.Outlined.Home)
    data object Watchlist : Screen("watchlist", "Watchlist", Icons.Filled.Star, Icons.Outlined.StarBorder)
    data object Alerts : Screen("alerts", "Alerts", Icons.Filled.Notifications, Icons.Outlined.Notifications)
}

val bottomNavItems = listOf(Screen.Home, Screen.Watchlist, Screen.Alerts)

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { AppBottomBar(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onStockClick = { symbol ->
                        navController.navigate("detail/$symbol")
                    }
                )
            }
            composable(Screen.Watchlist.route) {
                WatchlistScreen(
                    onStockClick = { symbol ->
                        navController.navigate("detail/$symbol")
                    }
                )
            }
            composable(Screen.Alerts.route) {
                AlertsScreen()
            }
            composable(
                route = "detail/{symbol}",
                arguments = listOf(navArgument("symbol") { type = NavType.StringType })
            ) {
                StockDetailScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

@Composable
private fun AppBottomBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Hide bottom bar on detail screen
    val showBottomBar = bottomNavItems.any { screen ->
        currentDestination?.hierarchy?.any { it.route == screen.route } == true
    }
    if (!showBottomBar) return

    NavigationBar {
        bottomNavItems.forEach { screen ->
            val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
            NavigationBarItem(
                icon = {
                    Icon(
                        if (selected) screen.selectedIcon else screen.unselectedIcon,
                        contentDescription = screen.label
                    )
                },
                label = { Text(screen.label) },
                selected = selected,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}
