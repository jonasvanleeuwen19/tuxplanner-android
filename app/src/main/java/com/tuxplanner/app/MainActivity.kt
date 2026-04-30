package com.tuxplanner.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tuxplanner.app.ui.navigation.AppNavHost
import com.tuxplanner.app.ui.theme.TuxPlannerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TuxPlannerTheme {
                AppNavHost()
            }
        }
    }
}
