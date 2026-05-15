package beta.manager.ui

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import beta.manager.navigation.NavRoutes
import beta.manager.ui.screen.*
import beta.manager.ui.theme.BetaManagerTheme
import beta.manager.ui.viewmodel.GameProfilesViewModel
import beta.manager.ui.viewmodel.HomeViewModel
import beta.manager.ui.viewmodel.PluginViewModel
import beta.manager.ui.viewmodel.SettingsViewModel
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BetaManagerTheme {
                BetaManagerNav()
            }
        }
    }
}

@Composable
fun BetaManagerNav() {
    val navController = rememberNavController()
    val context = androidx.compose.ui.platform.LocalContext.current
    val homeViewModel: HomeViewModel = viewModel()
    val pluginViewModel: PluginViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()
    val gameProfilesViewModel: GameProfilesViewModel = viewModel()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { uri ->
            try {
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@rememberLauncherForActivityResult
                val tempFile = File(context.cacheDir, "plugin_${System.currentTimeMillis()}.zip")
                FileOutputStream(tempFile).use { output -> inputStream.copyTo(output) }
                inputStream.close()
                pluginViewModel.installZip(tempFile.absolutePath)
            } catch (_: Exception) {}
        }
    }

    NavHost(
        navController = navController,
        startDestination = NavRoutes.Home.route
    ) {
        composable(NavRoutes.Home.route) {
            HomeScreen(
                viewModel = homeViewModel,
                onNavigateToPlugins = { navController.navigate(NavRoutes.Plugins.route) },
                onNavigateToGameProfiles = { navController.navigate(NavRoutes.GameProfiles.route) },
                onNavigateToSettings = { navController.navigate(NavRoutes.Settings.route) },
                onNavigateToShell = { navController.navigate(NavRoutes.ShellExecutor.route) }
            )
        }

        composable(NavRoutes.Plugins.route) {
            PluginScreen(
                viewModel = pluginViewModel,
                onNavigateBack = { navController.popBackStack() },
                onInstallPlugin = { filePickerLauncher.launch("application/zip") },
                onOpenWebUI = { pluginId ->
                    navController.navigate(NavRoutes.WebUI.createRoute(pluginId))
                }
            )
        }

        composable(NavRoutes.GameProfiles.route) {
            GameProfilesScreen(
                viewModel = gameProfilesViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.Settings.route) {
            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.ShellExecutor.route) {
            ShellExecutorScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = NavRoutes.WebUI.route,
            arguments = listOf(navArgument("pluginId") { type = NavType.StringType })
        ) { backStackEntry ->
            val pluginId = backStackEntry.arguments?.getString("pluginId") ?: ""
            WebUIScreen(
                pluginId = pluginId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}


