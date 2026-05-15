package beta.manager.navigation

sealed class NavRoutes(val route: String, val title: String) {
    data object Home : NavRoutes("home", "Beta Manager")
    data object Plugins : NavRoutes("plugins", "Plugins")
    data object GameProfiles : NavRoutes("game_profiles", "Game Profiles")
    data object ShellExecutor : NavRoutes("shell_executor", "Shell")
    data object Settings : NavRoutes("settings", "Settings")
    data object Logs : NavRoutes("logs", "Logs")
    data object Superuser : NavRoutes("superuser", "Superuser")
    data object WebUI : NavRoutes("plugins/{pluginId}/webui", "WebUI") {
        fun createRoute(pluginId: String) = "plugins/$pluginId/webui"
    }
}
