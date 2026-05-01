package com.tuxplanner.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tuxplanner.app.TuxPlannerApp
import com.tuxplanner.app.data.repository.ApiResult
import com.tuxplanner.app.ui.screens.home.HomeScreen
import com.tuxplanner.app.ui.screens.login.LoginScreen
import com.tuxplanner.app.ui.screens.serverconfig.ServerConfigScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val container = (context.applicationContext as TuxPlannerApp).container

    var startDestination by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (!container.appPreferences.isServerConfigured()) {
            startDestination = Screen.ServerConfig
            return@LaunchedEffect
        }

        val loggedIn = container.appPreferences.isLoggedIn()
        startDestination = if (loggedIn) {
            // Verify session with a lightweight /me call;
            // if it fails the Login screen will handle the re-auth.
            when (container.authRepository.getMe()) {
                is ApiResult.Success -> Screen.Home
                is ApiResult.Error -> {
                    container.appPreferences.setLoggedIn(false)
                    container.apiClient.clearCookies()
                    Screen.Login
                }
            }
        } else {
            Screen.Login
        }
    }

    if (startDestination == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    NavHost(navController = navController, startDestination = startDestination!!) {
        composable(Screen.ServerConfig) {
            ServerConfigScreen(
                onConfigSaved = {
                    navController.navigate(Screen.Login) {
                        popUpTo(Screen.ServerConfig) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Login) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home) {
                        popUpTo(Screen.Login) { inclusive = true }
                    }
                },
                onBackToOnboarding = {
                    navController.navigate(Screen.ServerConfig) {
                        popUpTo(Screen.Login) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Home) {
            HomeScreen(
                onLogout = {
                    navController.navigate(Screen.Login) {
                        popUpTo(Screen.Home) { inclusive = true }
                    }
                }
            )
        }
    }
}

