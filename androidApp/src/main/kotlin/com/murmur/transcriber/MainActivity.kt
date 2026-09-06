package com.murmur.transcriber

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.murmur.transcriber.ui.components.MurmurBottomBar
import com.murmur.transcriber.ui.components.MurmurTab
import com.murmur.transcriber.ui.home.HomeScreen
import com.murmur.transcriber.ui.library.LibraryScreen
import com.murmur.transcriber.ui.record.RecordScreen
import com.murmur.transcriber.ui.theme.MurmurTheme

private object Routes {
    const val HOME = "home"
    const val LIBRARY = "library"
    const val SEARCH = "search"
    const val STORAGE = "storage"
    const val RECORD = "record"
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MurmurTheme {
                MurmurApp()
            }
        }
    }
}

@Composable
private fun MurmurApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val currentTab = when (currentRoute) {
        Routes.LIBRARY -> MurmurTab.Library
        Routes.SEARCH -> MurmurTab.Search
        Routes.STORAGE -> MurmurTab.Storage
        else -> MurmurTab.Home
    }
    val showBottomBar = currentRoute in setOf(Routes.HOME, Routes.LIBRARY, Routes.SEARCH, Routes.STORAGE)

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = Routes.HOME) {
            composable(Routes.HOME) {
                HomeScreen(
                    onOpenRecording = { /* TODO: detail screen lands with playback (Milestone 4) */ },
                    onOpenLibrary = { navController.navigateToTab(Routes.LIBRARY) },
                )
            }
            composable(Routes.LIBRARY) { LibraryScreen() }
            composable(Routes.SEARCH) { PlaceholderScreen("Search") }
            composable(Routes.STORAGE) { PlaceholderScreen("Storage") }
            composable(Routes.RECORD) {
                RecordScreen(
                    onBack = { navController.popBackStack() },
                    onFinished = {
                        // TODO: land on the player+transcript screen (Milestone 4) instead
                        // of just going back, once that screen exists.
                        navController.popBackStack()
                    },
                )
            }
        }

        if (showBottomBar) {
            MurmurBottomBar(
                current = currentTab,
                onTabSelected = { tab ->
                    val route = when (tab) {
                        MurmurTab.Home -> Routes.HOME
                        MurmurTab.Library -> Routes.LIBRARY
                        MurmurTab.Search -> Routes.SEARCH
                        MurmurTab.Storage -> Routes.STORAGE
                    }
                    navController.navigateToTab(route)
                },
                onRecordClick = { navController.navigate(Routes.RECORD) },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun PlaceholderScreen(name: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("$name — coming soon")
    }
}
