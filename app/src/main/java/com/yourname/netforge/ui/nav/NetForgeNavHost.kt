package com.yourname.netforge.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.yourname.netforge.data.prefs.SecurePrefs
import com.yourname.netforge.data.repo.ConfigRepository
import com.yourname.netforge.ui.configs.ConfigListScreen
import com.yourname.netforge.ui.configs.ConfigListViewModel
import com.yourname.netforge.ui.detail.ConfigDetailScreen
import com.yourname.netforge.ui.detail.ConfigDetailViewModel
import com.yourname.netforge.ui.editor.PayloadEditorScreen
import com.yourname.netforge.ui.editor.PayloadEditorViewModel
import com.yourname.netforge.ui.export.ExportScreen
import com.yourname.netforge.ui.export.ExportViewModel
import com.yourname.netforge.ui.home.HomeScreen
import com.yourname.netforge.ui.home.HomeViewModel
import com.yourname.netforge.ui.logs.LogsScreen
import com.yourname.netforge.ui.logs.LogsViewModel
import com.yourname.netforge.ui.settings.SettingsScreen
import com.yourname.netforge.ui.settings.SettingsViewModel
import com.yourname.netforge.ui.theme.DarkBackground
import com.yourname.netforge.ui.theme.DarkCardBorder
import com.yourname.netforge.ui.theme.VioletPrimary

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Configs : Screen("configs", "Configs", Icons.Default.ListAlt)
    object Logs : Screen("logs", "Logs", Icons.Default.Terminal)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)

    object Detail : Screen("detail/{id}", "Details") {
        fun createRoute(id: Long) = "detail/$id"
    }

    object Editor : Screen("editor?id={id}", "Editor") {
        fun createRoute(id: Long? = null) = if (id != null) "editor?id=$id" else "editor"
    }

    object Export : Screen("export/{id}", "Export") {
        fun createRoute(id: Long) = "export/$id"
    }
}

@Composable
fun NetForgeNavHost(
    navController: NavHostController,
    configRepository: ConfigRepository,
    securePrefs: SecurePrefs,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        Screen.Home,
        Screen.Configs,
        Screen.Logs,
        Screen.Settings
    )

    val showBottomBar = bottomNavItems.any { it.route == currentRoute }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = DarkBackground,
                    tonalElevation = 8.dp
                ) {
                    bottomNavItems.forEach { screen ->
                        val selected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
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
                                    imageVector = screen.icon!!,
                                    contentDescription = screen.title
                                )
                            },
                            label = { Text(screen.title) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = VioletPrimary,
                                selectedTextColor = VioletPrimary,
                                indicatorColor = VioletPrimary.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.testTag("nav_item_${screen.route}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                val vm = remember { HomeViewModel(configRepository) }
                HomeScreen(
                    viewModel = vm,
                    onNavigateToConfigs = { navController.navigate(Screen.Configs.route) },
                    onNavigateToDetail = { id -> navController.navigate(Screen.Detail.createRoute(id)) },
                    onNavigateToEditor = { id -> navController.navigate(Screen.Editor.createRoute(id)) }
                )
            }

            composable(Screen.Configs.route) {
                val vm = remember { ConfigListViewModel(configRepository) }
                ConfigListScreen(
                    viewModel = vm,
                    onNavigateToDetail = { id -> navController.navigate(Screen.Detail.createRoute(id)) },
                    onNavigateToEditor = { id -> navController.navigate(Screen.Editor.createRoute(id)) }
                )
            }

            composable(Screen.Logs.route) {
                val vm = remember { LogsViewModel() }
                LogsScreen(viewModel = vm)
            }

            composable(Screen.Settings.route) {
                val vm = remember { SettingsViewModel(securePrefs) }
                SettingsScreen(viewModel = vm)
            }

            composable(
                route = Screen.Detail.route,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getLong("id") ?: 0L
                val vm = remember(id) { ConfigDetailViewModel(configRepository, id) }
                ConfigDetailScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEdit = { editId -> navController.navigate(Screen.Editor.createRoute(editId)) },
                    onNavigateToExport = { exportId -> navController.navigate(Screen.Export.createRoute(exportId)) }
                )
            }

            composable(
                route = Screen.Editor.route,
                arguments = listOf(navArgument("id") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) { backStackEntry ->
                val idString = backStackEntry.arguments?.getString("id")
                val id = idString?.toLongOrNull()
                val vm = remember(id) { PayloadEditorViewModel(configRepository, id) }
                PayloadEditorScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.Export.route,
                arguments = listOf(navArgument("id") { type = NavType.LongType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getLong("id") ?: 0L
                val vm = remember(id) { ExportViewModel(configRepository, id) }
                ExportScreen(
                    viewModel = vm,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
