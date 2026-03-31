package com.financetracker.app.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.financetracker.app.presentation.ui.categories.CategoriesScreen
import com.financetracker.app.presentation.ui.charts.ChartsScreen
import com.financetracker.app.presentation.ui.creditcards.CreditCardsScreen
import com.financetracker.app.presentation.ui.dashboard.DashboardScreen
import com.financetracker.app.presentation.ui.settings.SettingsScreen
import com.financetracker.app.presentation.ui.transactions.TransactionsScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                BottomNavItem.all.forEach { item ->
                    NavigationBarItem(
                        icon     = { Icon(item.icon, contentDescription = item.label) },
                        label    = { Text(item.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                        onClick  = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
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
            navController    = navController,
            startDestination = BottomNavItem.Dashboard.route,
            modifier         = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Dashboard.route) {
                DashboardScreen(onNavigateToSettings = { navController.navigate("settings") })
            }
            composable(BottomNavItem.Transactions.route) {
                TransactionsScreen()
            }
            composable(BottomNavItem.Charts.route) {
                ChartsScreen()
            }
            composable(BottomNavItem.Categories.route) {
                CategoriesScreen()
            }
            composable(BottomNavItem.CreditCards.route) {
                CreditCardsScreen()
            }
            composable("settings") {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
