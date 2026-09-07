package com.quietstack.vaani

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.quietstack.vaani.ui.about.AboutScreen
import com.quietstack.vaani.ui.components.VaaniBottomBar
import com.quietstack.vaani.ui.components.VaaniTab
import com.quietstack.vaani.ui.components.WordmarkLockup
import com.quietstack.vaani.ui.components.mrise
import com.quietstack.vaani.shared.data.AppSettings
import kotlinx.coroutines.delay
import com.quietstack.vaani.ui.detail.DetailScreen
import com.quietstack.vaani.ui.edit.EditScreen
import com.quietstack.vaani.ui.home.HomeScreen
import com.quietstack.vaani.ui.importing.ImportScreen
import com.quietstack.vaani.ui.library.LibraryScreen
import com.quietstack.vaani.ui.search.SearchScreen
import com.quietstack.vaani.ui.onboarding.OnboardingScreen
import com.quietstack.vaani.ui.record.RecordScreen
import com.quietstack.vaani.ui.theme.VaaniTheme
import org.koin.android.ext.android.inject

private object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val LIBRARY = "library"
    const val SEARCH = "search"
    const val ABOUT = "about"
    const val RECORD = "record"
    const val IMPORT = "import"
    const val DETAIL = "detail/{recordingId}"
    fun detail(recordingId: String) = "detail/$recordingId"
    const val EDIT = "edit/{recordingId}"
    fun edit(recordingId: String) = "edit/$recordingId"
}

class MainActivity : ComponentActivity() {

    private val pendingImportUri = mutableStateOf<Uri?>(null)
    private val appSettings: AppSettings by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        // The system splash (blank — see Theme.Vaani.Splash in themes.xml)
        // stays on screen until our own Compose splash below has actually
        // composed and is ready to draw, so there's one continuous hold on
        // the background color and then one splash — logo and name — rather
        // than a blank/icon frame visibly replaced by a different-looking one.
        val splashScreen = installSplashScreen()
        var composeSplashReady = false
        splashScreen.setKeepOnScreenCondition { !composeSplashReady }
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        pendingImportUri.value = extractImportUri(intent)
        val startDestination = if (appSettings.hasCompletedOnboarding) Routes.HOME else Routes.ONBOARDING

        setContent {
            VaaniTheme {
                // Purely a brand moment, not gating on real startup work — Koin
                // and the model preload (VaaniApplication) already run before
                // this Activity is even created, so there's no async result to
                // wait on here. A fixed short delay is honest about that: it's
                // not pretending to be a progress indicator.
                var showSplash by remember { mutableStateOf(true) }
                LaunchedEffect(Unit) {
                    delay(2900)
                    showSplash = false
                }
                Crossfade(targetState = showSplash, animationSpec = tween(300), label = "splash") { splash ->
                    if (splash) {
                        SideEffect { composeSplashReady = true }
                        SplashScreen()
                    } else {
                        VaaniApp(
                            startDestination = startDestination,
                            pendingImportUri = pendingImportUri.value,
                            onImportConsumed = { pendingImportUri.value = null },
                        )
                    }
                }
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
private fun VaaniApp(startDestination: String, pendingImportUri: Uri?, onImportConsumed: () -> Unit) {
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

    // POST_NOTIFICATIONS is declared in the manifest but Android 13+ also requires
    // this runtime request — without it the transcription-progress notification
    // (TranscriptionForegroundService) builds and posts successfully but the OS
    // silently drops it, which read as "no progress shown" (real-device finding).
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val currentTab = when (currentRoute) {
        Routes.LIBRARY -> VaaniTab.Library
        Routes.SEARCH -> VaaniTab.Search
        Routes.ABOUT -> VaaniTab.About
        else -> VaaniTab.Home
    }
    val showBottomBar = currentRoute in setOf(Routes.HOME, Routes.LIBRARY, Routes.SEARCH, Routes.ABOUT)

    Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            enterTransition = { fadeIn(tween(220)) },
            exitTransition = { fadeOut(tween(160)) },
            popEnterTransition = { fadeIn(tween(220)) },
            popExitTransition = { fadeOut(tween(160)) },
        ) {
            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    onComplete = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    },
                )
            }
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
                    onEditRecording = { id -> navController.navigate(Routes.edit(id)) },
                )
            }
            composable(Routes.SEARCH) {
                SearchScreen(onOpenRecording = { id -> navController.navigate(Routes.detail(id)) })
            }
            composable(Routes.ABOUT) {
                AboutScreen()
            }
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
                DetailScreen(
                    recordingId = recordingId,
                    onBack = { navController.popBackStack() },
                    onEditTranscript = { navController.navigate(Routes.edit(recordingId)) },
                )
            }
            composable(
                Routes.EDIT,
                arguments = listOf(navArgument("recordingId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val recordingId = backStackEntry.arguments?.getString("recordingId").orEmpty()
                EditScreen(
                    recordingId = recordingId,
                    onCancel = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                )
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
            VaaniBottomBar(
                current = currentTab,
                onTabSelected = { tab ->
                    val route = when (tab) {
                        VaaniTab.Home -> Routes.HOME
                        VaaniTab.Library -> Routes.LIBRARY
                        VaaniTab.Search -> Routes.SEARCH
                        VaaniTab.About -> Routes.ABOUT
                    }
                    navController.navigateToTab(route)
                },
                onRecordClick = { navController.navigate(Routes.RECORD) },
                onImportClick = { pickDocument.launch(arrayOf("audio/*")) },
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding(),
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
private fun SplashScreen() {
    val colors = VaaniTheme.colors
    Box(
        modifier = Modifier.fillMaxSize().background(colors.bg),
        contentAlignment = Alignment.Center,
    ) {
        // Same soft radial glow as Onboarding's welcome step (design consistency),
        // behind the mark rather than around it so it doesn't compete for focus.
        Box(
            modifier = Modifier
                .size(360.dp)
                .background(
                    Brush.radialGradient(listOf(colors.priDim.copy(alpha = 0.4f), Color.Transparent)),
                    CircleShape,
                ),
        )
        Box(modifier = Modifier.mrise(durationMillis = 650)) {
            WordmarkLockup(barWidth = 8.dp, barMaxHeight = 48.dp, fontSize = 34.sp, letterSpacing = 5.sp)
        }
    }
}

@Composable
private fun PlaceholderScreen(name: String) {
    val colors = VaaniTheme.colors
    Box(
        modifier = Modifier.fillMaxSize().background(colors.bg),
        contentAlignment = Alignment.Center,
    ) {
        Text("$name — coming soon", color = colors.ink3)
    }
}
