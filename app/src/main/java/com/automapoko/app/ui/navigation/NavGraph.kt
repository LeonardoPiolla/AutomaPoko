package com.automapoko.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.automapoko.app.ui.screens.create.CreateAutomationScreen
import com.automapoko.app.ui.screens.detail.AutomationDetailScreen
import com.automapoko.app.ui.screens.home.HomeScreen
import com.automapoko.app.ui.screens.onboarding.OnboardingScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Onboarding
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        // Tela principal
        composable(Screen.Home.route) {
            HomeScreen(
                onCreateAutomation = {
                    navController.navigate(Screen.CreateAutomation.route)
                },
                onAutomationClick = { automationId ->
                    navController.navigate(
                        Screen.AutomationDetail().createRoute(automationId)
                    )
                }
            )
        }

        // Criar automação
        composable(Screen.CreateAutomation.route) {
            CreateAutomationScreen(
                onNavigateBack = { navController.popBackStack() },
                onSaved = {
                    navController.popBackStack()
                }
            )
        }

        // Editar automação
        composable(
            route = Screen.EditAutomation().route,
            arguments = listOf(navArgument("automationId") { type = NavType.StringType })
        ) { backStackEntry ->
            val automationId = backStackEntry.arguments?.getString("automationId") ?: return@composable
            CreateAutomationScreen(
                editingAutomationId = automationId,
                onNavigateBack = { navController.popBackStack() },
                onSaved = {
                    navController.popBackStack()
                    navController.popBackStack() // Volta do detalhe também
                }
            )
        }

        // Detalhe
        composable(
            route = Screen.AutomationDetail().route,
            arguments = listOf(navArgument("automationId") { type = NavType.StringType })
        ) { backStackEntry ->
            val automationId = backStackEntry.arguments?.getString("automationId") ?: return@composable
            AutomationDetailScreen(
                automationId = automationId,
                onNavigateBack = { navController.popBackStack() },
                onEdit = { id ->
                    navController.navigate(Screen.EditAutomation().createRoute(id))
                }
            )
        }
    }
}
