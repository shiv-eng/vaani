package com.murmur.transcriber

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.murmur.transcriber.ui.components.MurmurBottomBar
import com.murmur.transcriber.ui.components.MurmurTab
import com.murmur.transcriber.ui.detail.DetailScreen
import com.murmur.transcriber.ui.home.HomeScreen
import com.murmur.transcriber.ui.importing.ImportScreen
import com.murmur.transcriber.ui.library.LibraryScreen
import com.murmur.transcriber.ui.record.RecordScreen
import com.murmur.transcriber.ui.storage.StorageScreen
import com.murmur.transcriber.ui.theme.MurmurTheme
import androidx.compose.foundation.background

private object Routes {
    const val HOME = "home"
    const val LIBRARY = "library"
    const val SEARCH = "search"
    const val STORAGE = "storage"
    const val RECORD = "record"
    const val IMPORT = "import"
    const val DETAIL = "detail/{recordingId}"
    fun detail(recordingId: String) = "detail/$recordingId"
}

class MainActivity : ComponentActivity() {

    private val pendingImportUri = mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingImportUri.value = extractImportUri(intent)

        setContent {
            MurmurTheme {
                MurmurApp(
                    pendingImportUri = pendingImportUri.value,
                    onImportConsumed = { pendingImportUri.value = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractImportUri(intent)?.let { pendingImportUri.value = it }
    }

    private fun extractImportUri(intent: Intent?): Uri? = when (intent?.action) {
        Intent.ACTION_SEND -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }
        Intent.ACTION_VIEW -> intent.data
        else -> null
    }
}

@Composable
private fun MurmurApp(pendingImportUri: Uri?, onImportConsumed: () -> Unit) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    var manualImportUri by remember { mutableStateOf<Uri?>(null) }
    val activeImportUri = pendingImportUri ?: manualImportUri

    LaunchedEffect(pendingImportUri) {
        if (pendingImportUri != null) {
            navController.navigate(Routes.IMPORT)
        }
    }

    val pickDocument = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            manualImportUri = uri
            navController.navigate(Routes.IMPORT)
        }
    }

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
                    onOpenRecording = { id -> navController.navigate(Routes.detail(id)) },
                    onOpenLibrary = { navController.navigateToTab(Routes.LIBRARY) },
                )
            }
            composable(Routes.LIBRARY) {
                LibraryScreen(
                    onImportClick = { pickDocument.launch(arrayOf("audio/*")) },
                    onSearchClick = { navController.navigateToTab(Routes.SEARCH) },
                    onOpenRecording = { id -> navController.navigate(Routes.detail(id)) },
                )
            }
            composable(Routes.SEARCH) { PlaceholderScreen("Search") }
            composable(Routes.STORAGE) { StorageScreen() }
            composable(Routes.RECORD) {
                RecordScreen(
                    onBack = { navController.popBackStack() },
                    onFinished = { id ->
                        navController.navigate(Routes.detail(id)) {
                            popUpTo(Routes.RECORD) { inclusive = true }
                        }
                    },
                )
            }
            composable(
                Routes.DETAIL,
                arguments = listOf(navArgument("recordingId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val recordingId = backStackEntry.arguments?.getString("recordingId").orEmpty()
                DetailScreen(recordingId = recordingId, onBack = { navController.popBackStack() })
            }
            composable(Routes.IMPORT) {
                val uri = activeImportUri
                if (uri == null) {
                    PlaceholderScreen("Import")
                } else {
                    ImportScreen(
                        uri = uri,
                        onBack = {
                            manualImportUri = null
                            onImportConsumed()
                            navController.popBackStack()
                        },
                        onFinished = {
                            manualImportUri = null
                            onImportConsumed()
                            navController.popBackStack(Routes.HOME, inclusive = false)
                        },
                    )
                }
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
    val colors = MurmurTheme.colors
    Box(
        modifier = Modifier.fillMaxSize().background(colors.bg),
        contentAlignment = Alignment.Center,
    ) {
        Text("$name — coming soon", color = colors.ink3)
    }
}
