package beta.manager.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import beta.manager.navigation.NavRoutes
import beta.manager.service.BetaService
import beta.manager.ui.screen.*
import beta.manager.ui.theme.*
import beta.manager.ui.viewmodel.GameProfilesViewModel
import beta.manager.ui.viewmodel.HomeViewModel
import beta.manager.ui.viewmodel.PluginViewModel
import beta.manager.ui.viewmodel.SettingsViewModel
import beta.manager.utils.ShizukuShell
import rikka.shizuku.Shizuku
import java.io.File
import java.io.FileOutputStream

data class BottomNavItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val route: String
)

class MainActivity : ComponentActivity() {
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    private val shizukuPermissionListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == ShizukuShell.REQUEST_PERMISSION_CODE &&
                grantResult == PackageManager.PERMISSION_GRANTED
            ) {
                startBetaService()
            }
        }

    private val shizukuBinderReceivedListener = Shizuku.OnBinderReceivedListener {
        requestShizukuPermissionIfNeeded()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Shizuku.addRequestPermissionResultListener(shizukuPermissionListener)
        Shizuku.addBinderReceivedListener(shizukuBinderReceivedListener)
        requestNotificationPermissionIfNeeded()
        startBetaService()
        requestShizukuPermissionIfNeeded()
        setContent {
            BetaManagerTheme {
                BetaManagerNav()
            }
        }
    }

    override fun onDestroy() {
        try {
            Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener)
            Shizuku.removeBinderReceivedListener(shizukuBinderReceivedListener)
        } catch (_: Exception) {
        }
        super.onDestroy()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun requestShizukuPermissionIfNeeded() {
        try {
            if (!Shizuku.pingBinder() || Shizuku.isPreV11()) return
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) return
            if (Shizuku.shouldShowRequestPermissionRationale()) return
            Shizuku.requestPermission(ShizukuShell.REQUEST_PERMISSION_CODE)
        } catch (_: Exception) {
        }
    }

    private fun startBetaService() {
        try {
            ContextCompat.startForegroundService(
                this,
                Intent(this, BetaService::class.java)
            )
        } catch (_: Exception) {
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
                val cacheDir = context.externalCacheDir ?: context.cacheDir
                val tempFile = File(cacheDir, "plugin_${System.currentTimeMillis()}.zip")
                FileOutputStream(tempFile).use { output -> inputStream.copyTo(output) }
                inputStream.close()
                pluginViewModel.installZip(tempFile.absolutePath)
            } catch (_: Exception) {}
        }
    }

    val bottomNavItems = listOf(
        BottomNavItem("Home", Icons.Filled.Dashboard, Icons.Outlined.Dashboard, NavRoutes.Home.route),
        BottomNavItem("Plugins", Icons.Filled.Extension, Icons.Outlined.Extension, NavRoutes.Plugins.route),
        BottomNavItem("Profiles", Icons.Filled.SportsEsports, Icons.Outlined.SportsEsports, NavRoutes.GameProfiles.route),
        BottomNavItem("Settings", Icons.Filled.Settings, Icons.Outlined.Settings, NavRoutes.Settings.route),
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = currentDestination?.route in bottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = DarkSurface,
                    contentColor = TextPrimary,
                    tonalElevation = 0.dp,
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentDestination?.route != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            },
                            label = {
                                Text(
                                    text = item.label,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = NeonCyan,
                                selectedTextColor = NeonCyan,
                                unselectedIconColor = TextTertiary,
                                unselectedTextColor = TextTertiary,
                                indicatorColor = NeonCyan.copy(alpha = 0.12f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavRoutes.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(NavRoutes.Home.route) {
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToShell = { navController.navigate(NavRoutes.ShellExecutor.route) },
                    onInstallPlugin = { filePickerLauncher.launch("application/zip") },
                    onNavigateToLogs = { navController.navigate(NavRoutes.Logs.route) },
                    onNavigateToSuperuser = { navController.navigate(NavRoutes.Superuser.route) }
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

            composable(NavRoutes.Logs.route) {
                LogScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(NavRoutes.Superuser.route) {
                SuperuserScreen(
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
}
