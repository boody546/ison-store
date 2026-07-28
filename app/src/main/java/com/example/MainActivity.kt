package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp
import com.example.ui.screens.AppDetailsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.StoreViewModel

class MainActivity : ComponentActivity() {
    private val storeViewModel: StoreViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                StoreApp(viewModel = storeViewModel)
            }
        }
    }
}

@Composable
fun StoreApp(viewModel: StoreViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val user by viewModel.reactiveUser.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (currentScreen != "SPLASH") {
                // Standard Material Design 3 Bottom Navigation Bar
                NavigationBar(
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    NavigationBarItem(
                        selected = currentScreen == "HOME" || currentScreen == "DETAILS",
                        onClick = { viewModel.navigateTo("HOME") },
                        icon = { Icon(imageVector = Icons.Default.Home, contentDescription = "الرئيسية") },
                        label = { Text("الرئيسية") },
                        modifier = Modifier.testTag("nav_home_item")
                    )

                    // Only show Studio tab if developer or admin is logged in
                    val u = user
                    if (u != null && (u.role == "DEVELOPER" || u.role == "ADMIN")) {
                        NavigationBarItem(
                            selected = currentScreen == "DEV_DASHBOARD",
                            onClick = { viewModel.navigateTo("DEV_DASHBOARD") },
                            icon = { Icon(imageVector = Icons.Default.Build, contentDescription = "لوحة التحكم") },
                            label = { Text("الاستوديو") },
                            modifier = Modifier.testTag("nav_dashboard_item")
                        )
                    }

                    NavigationBarItem(
                        selected = currentScreen == "PROFILE" || currentScreen == "LOGIN" || currentScreen == "REGISTER",
                        onClick = { viewModel.navigateTo("PROFILE") },
                        icon = { Icon(imageVector = Icons.Default.Person, contentDescription = "الملف الشخصي") },
                        label = { Text("حسابي") },
                        modifier = Modifier.testTag("nav_profile_item")
                    )
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (currentScreen == "SPLASH") PaddingValues(0.dp) else innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            when (currentScreen) {
                "SPLASH" -> SplashScreen()
                "HOME" -> HomeScreen(viewModel = viewModel)
                "DETAILS" -> AppDetailsScreen(viewModel = viewModel)
                "DEV_DASHBOARD" -> DashboardScreen(viewModel = viewModel)
                "PROFILE", "LOGIN", "REGISTER" -> ProfileScreen(viewModel = viewModel)
                else -> HomeScreen(viewModel = viewModel)
            }
        }
    }
}
