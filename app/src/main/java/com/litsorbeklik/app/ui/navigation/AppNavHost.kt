package com.litsorbeklik.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.litsorbeklik.app.data.model.AiEngineType
import com.litsorbeklik.app.data.model.BuildEngineType
import com.litsorbeklik.app.ui.screens.login.LoginScreen
import com.litsorbeklik.app.ui.screens.projects.ProjectListScreen
import com.litsorbeklik.app.ui.screens.register.RegisterScreen
import com.litsorbeklik.app.ui.screens.settings.EngineSettingsScreen
import com.litsorbeklik.app.ui.screens.spec.SpecChatScreen

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.REGISTER) {

        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegistered = { navController.navigate(Routes.LOGIN) },
                onGoToLogin = { navController.navigate(Routes.LOGIN) },
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(onLoggedIn = {
                navController.navigate(Routes.PROJECTS) {
                    popUpTo(Routes.REGISTER) { inclusive = true }
                }
            })
        }

        composable(Routes.PROJECTS) {
            ProjectListScreen(
                onOpenProject = { project -> navController.navigate(Routes.spec(project.id)) },
                onNewProject = { navController.navigate(Routes.spec("new")) },
            )
        }

        composable(Routes.SPEC) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: "new"
            SpecChatScreen(
                onSendMessage = { /* TODO: forward to active AiEngine.chatSpecStep */ },
                onUploadExistingSpec = { /* TODO: file picker -> parse -> AppSpec */ },
                onConfirmSpec = { navController.navigate(Routes.settings(projectId)) },
            )
        }

        composable(Routes.SETTINGS) {
            EngineSettingsScreen(
                onSave = { _: AiEngineType, _: BuildEngineType ->
                    navController.popBackStack(Routes.PROJECTS, inclusive = false)
                },
            )
        }
    }
}
