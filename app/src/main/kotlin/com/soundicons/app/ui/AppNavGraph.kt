package com.soundicons.app.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.soundicons.app.ui.editor.EditorScreen
import com.soundicons.app.ui.home.HomeScreen
import com.soundicons.app.ui.settings.SettingsScreen

object Routes {
    const val HOME     = "home"
    const val SETTINGS = "settings"
    const val EDITOR   = "editor?iconId={iconId}"
    fun editorRoute(iconId: Long? = null) = "editor?iconId=${iconId ?: 0}"
}

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToEditor   = { navController.navigate(Routes.editorRoute(it)) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(
            route     = Routes.EDITOR,
            arguments = listOf(navArgument("iconId") { type = NavType.LongType; defaultValue = 0L })
        ) { back ->
            val iconId = back.arguments?.getLong("iconId")?.takeIf { it > 0 }
            EditorScreen(iconId = iconId, onNavigateBack = { navController.popBackStack() })
        }
    }
}
