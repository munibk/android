package com.financetracker.app.presentation.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.financetracker.app.MainActivity
import com.financetracker.app.presentation.ui.categories.CategoriesScreen
import com.financetracker.app.presentation.ui.charts.ChartsScreen
import com.financetracker.app.presentation.ui.creditcards.CreditCardsScreen
import com.financetracker.app.presentation.ui.dashboard.DashboardScreen
import com.financetracker.app.presentation.ui.settings.SettingsScreen
import com.financetracker.app.presentation.ui.transactions.TransactionsScreen

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun AppNavigation() {
    val activity = LocalContext.current as MainActivity
    val windowSizeClass = calculateWindowSizeClass(activity)
    val useRail = windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Medium

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    fun navigate(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val navContent: @Composable () -> Unit = {
        NavHost(
            navController    = navController,
            startDestination = BottomNavItem.Dashboard.route
        ) {
            composable(BottomNavItem.Dashboard.route) {
                DashboardScreen(onNavigateToSettings = { navController.navigate("settings") })
            }
            composable(BottomNavItem.Transactions.route) { TransactionsScreen() }
            composable(BottomNavItem.Charts.route) { ChartsScreen() }
            composable(BottomNavItem.Categories.route) { CategoriesScreen() }
            composable(BottomNavItem.CreditCards.route) { CreditCardsScreen() }
            composable("settings") { SettingsScreen(onBack = { navController.popBackStack() }) }
        }
    }

    if (useRail) {
        // ── Tablet / large screen: NavigationRail on the left ──────────────
        Row(modifier = Modifier.fillMaxSize()) {
            NavigationRail {
                Spacer(Modifier.weight(1f))
                BottomNavItem.all.forEach { item ->
                    NavigationRailItem(
                        icon     = { Icon(item.icon, contentDescription = item.label) },
                        label    = { Text(item.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                        onClick  = { navigate(item.route) }
                    )
                }
                Spacer(Modifier.weight(1f))
            }
            Box(modifier = Modifier.fillMaxSize()) {
                navContent()
            }
        }
    } else {
        // ── Phone: NavigationBar at the bottom ─────────────────────────────
        Scaffold(
            bottomBar = {
                NavigationBar {
                    BottomNavItem.all.forEach { item ->
                        NavigationBarItem(
                            icon     = { Icon(item.icon, contentDescription = item.label) },
                            label    = { Text(item.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                            onClick  = { navigate(item.route) }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                navContent()
            }
        }
    }
}
