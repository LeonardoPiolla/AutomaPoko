package com.automapoko.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.automapoko.app.service.WorkManagerScheduler
import com.automapoko.app.ui.navigation.NavGraph
import com.automapoko.app.ui.navigation.Screen
import com.automapoko.app.ui.theme.AutomaPokoTheme
import com.automapoko.app.util.PermissionChecker
import com.automapoko.app.util.PreferencesKeys
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var workManagerScheduler: WorkManagerScheduler
    @Inject lateinit var permissionChecker: PermissionChecker

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Agenda o worker de saúde (KEEP — não recria se já existe)
        workManagerScheduler.scheduleHealthCheck()

        // Verifica se o onboarding já foi concluído
        val onboardingCompleted = runBlocking {
            permissionChecker.isOnboardingCompleted()
        }

        // Verifica e solicita permissões críticas revogadas
        lifecycleScope.launch {
            permissionChecker.checkAndRequestCriticalPermissions(this@MainActivity)
        }

        setContent {
            AutomaPokoTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    val startDestination = if (onboardingCompleted) {
                        Screen.Home.route
                    } else {
                        Screen.Onboarding.route
                    }

                    NavGraph(
                        navController = navController,
                        startDestination = startDestination
                    )
                }
            }
        }
    }
}
