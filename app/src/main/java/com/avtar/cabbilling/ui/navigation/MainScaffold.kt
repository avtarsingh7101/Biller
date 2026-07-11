package com.avtar.cabbilling.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.avtar.cabbilling.ui.screens.calendar.CalendarScreen
import com.avtar.cabbilling.ui.screens.dashboard.DashboardScreen
import com.avtar.cabbilling.ui.screens.detail.BillDetailScreen
import com.avtar.cabbilling.ui.screens.ledger.LedgerScreen
import com.avtar.cabbilling.ui.screens.logbook.LogbookScreen
import com.avtar.cabbilling.ui.screens.settings.FaqScreen
import com.avtar.cabbilling.ui.screens.settings.SettingsScreen
import kotlinx.coroutines.launch

@Composable
fun MainScaffold() {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val onMessage: (String) -> Unit = { message -> scope.launch { snackbarHostState.showSnackbar(message) } }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val tabRoutes = TopDestination.entries.map { it.route }.toSet()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (currentRoute in tabRoutes) {
                NavigationBar {
                    TopDestination.entries.forEach { dest ->
                        NavigationBarItem(
                            selected = currentRoute == dest.route,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                            label = { Text(dest.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopDestination.DASHBOARD.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn(animationSpec = tween(220)) },
            exitTransition = { fadeOut(animationSpec = tween(180)) },
            popEnterTransition = { fadeIn(animationSpec = tween(220)) },
            popExitTransition = { fadeOut(animationSpec = tween(180)) }
        ) {
            composable(TopDestination.DASHBOARD.route) {
                DashboardScreen(
                    onOpenBill = { navController.navigate(Routes.billDetail(it)) },
                    onMessage = onMessage
                )
            }
            composable(TopDestination.LEDGER.route) {
                LedgerScreen(
                    onOpenBill = { navController.navigate(Routes.billDetail(it)) },
                    onMessage = onMessage
                )
            }
            composable(TopDestination.CALENDAR.route) {
                CalendarScreen(onOpenBill = { navController.navigate(Routes.billDetail(it)) })
            }
            composable(TopDestination.LOGBOOK.route) {
                LogbookScreen(onMessage = onMessage)
            }
            composable(TopDestination.SETTINGS.route) {
                SettingsScreen(
                    onMessage = onMessage,
                    onOpenFaq = { navController.navigate(Routes.FAQ) }
                )
            }
            composable(
                route = Routes.BILL_DETAIL,
                arguments = listOf(navArgument(Routes.ARG_BILL_ID) { type = NavType.LongType }),
                enterTransition = {
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(300)) +
                        fadeIn(animationSpec = tween(300))
                },
                popExitTransition = {
                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(300)) +
                        fadeOut(animationSpec = tween(300))
                }
            ) { entry ->
                val billId = entry.arguments?.getLong(Routes.ARG_BILL_ID) ?: 0L
                BillDetailScreen(
                    billId = billId,
                    onBack = { navController.popBackStack() },
                    onMessage = onMessage
                )
            }
            composable(
                route = Routes.FAQ,
                enterTransition = {
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(300)) +
                        fadeIn(animationSpec = tween(300))
                },
                popExitTransition = {
                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(300)) +
                        fadeOut(animationSpec = tween(300))
                }
            ) {
                FaqScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
