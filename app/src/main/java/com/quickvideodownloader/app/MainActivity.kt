package com.quickvideodownloader.app

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.quickvideodownloader.app.core.utils.LocaleManager
import com.quickvideodownloader.app.data.local.LanguagePreferences
import com.quickvideodownloader.app.presentation.lock.LockScreen
import com.quickvideodownloader.app.presentation.lock.LockViewModel
import com.quickvideodownloader.app.presentation.navigation.Screen
import com.quickvideodownloader.app.presentation.ui.components.BottomNavBar
import com.quickvideodownloader.app.presentation.ui.screens.*
import com.quickvideodownloader.app.presentation.ui.theme.VideoDownloaderTheme
import com.quickvideodownloader.app.presentation.viewmodel.StatusViewModel
import com.quickvideodownloader.app.presentation.viewmodel.UpdateViewModel
import com.quickvideodownloader.app.presentation.ui.components.UpdateDialog
import androidx.compose.runtime.collectAsState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var languagePreferences: LanguagePreferences

    override fun attachBaseContext(newBase: Context) {
        // We can't use the injected languagePreferences here because attachBaseContext 
        // is called before Hilt injection occurs. We create a temporary instance.
        val prefs = LanguagePreferences(newBase)
        val languageCode = runBlocking {
            prefs.getLanguage.first() ?: "en"
        }
        val context = LocaleManager.setLocale(newBase, languageCode)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VideoDownloaderTheme {
                val navController = rememberNavController()
                val updateViewModel: UpdateViewModel = hiltViewModel()
                val updateState by updateViewModel.uiState.collectAsState()

                if (updateState.showDialog) {
                    UpdateDialog(
                        isForceUpdate = updateState.isForceUpdate,
                        packageName = updateState.packageName,
                        onDismiss = { updateViewModel.onDismissDialog() }
                    )
                }
                
                // Define routes where bottom bar should be visible
                val bottomNavRoutes = listOf(
                    Screen.HomeScreen.route,
                    Screen.DownloadManagerScreen.route,
                    Screen.CollectionScreen.route,
                    Screen.SettingsScreen.route
                )

                // Get current route to determine if we should show bottom bar
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                Scaffold(
                    bottomBar = {
                        if (currentRoute in bottomNavRoutes) {
                            BottomNavBar(navController)
                        }
                    }
                ) { paddingValues ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.SplashScreen.route,
                        modifier = Modifier.padding(paddingValues)
                    ) {
                        composable(route = Screen.SplashScreen.route) {
                            SplashScreen(
                                onTimeout = {
                                    val savedLanguage = runBlocking {
                                        languagePreferences.getLanguage.first()
                                    }
                                    if (savedLanguage == null) {
                                        navController.navigate(Screen.LanguageSelectionScreen.route) {
                                            popUpTo(Screen.SplashScreen.route) { inclusive = true }
                                        }
                                    } else {
                                        navController.navigate(Screen.HomeScreen.route) {
                                            popUpTo(Screen.SplashScreen.route) { inclusive = true }
                                        }
                                    }
                                }
                            )
                        }

                        composable(route = Screen.LanguageSelectionScreen.route) {
                            LanguageSelectionScreen(
                                onContinue = {
                                    navController.navigate(Screen.HomeScreen.route) {
                                        popUpTo(Screen.LanguageSelectionScreen.route) { inclusive = true }
                                    }
                                    recreate()
                                }
                            )
                        }
                        
                        composable(route = Screen.HomeScreen.route) {
                            HomeScreen(
                                onFromLinkClick = {
                                    navController.navigate(Screen.DownloadLinkScreen.route)
                                },
                                onDirectChatClick = {
                                    navController.navigate(Screen.DirectChatScreen.route)
                                },
                                onDeviceVideosClick = {
                                    navController.navigate(Screen.AlbumListScreen.createRoute("Device Videos"))
                                },
                                onStatusSaverClick = {
                                    navController.navigate(Screen.StatusSaverScreen.route)
                                },
                                onLockedVideosClick = {
                                    navController.navigate(Screen.LockScreen.route)
                                },
                                onHiddenVideosClick = {
                                    navController.navigate(Screen.HiddenVideosScreen.route)
                                },
                                onDownloadsClick = {
                                    if (currentRoute != Screen.DownloadManagerScreen.route) {
                                        navController.navigate(Screen.DownloadManagerScreen.route) {
                                            popUpTo(Screen.HomeScreen.route) {
                                                inclusive = false
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            )
                        }
                        
                        composable(route = Screen.HiddenVideosScreen.route) {
                            HiddenVideosScreen(
                                onNavigateBack = { navController.popBackStack() },
                                onVideoClick = { path ->
                                    val encoded = java.net.URLEncoder.encode(path, "UTF-8")
                                    navController.navigate(Screen.VideoPlayerScreen.createRoute(encoded))
                                }
                            )
                        }

                        composable(route = Screen.DeviceVideosScreen.route) {
                            DeviceVideosScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                onVideoClick = { uriStr ->
                                    val encoded = java.net.URLEncoder.encode(uriStr, "UTF-8")
                                    navController.navigate(Screen.VideoPlayerScreen.createRoute(encoded))
                                }
                            )
                        }

                        composable(
                            route = Screen.VideoPlayerScreen.route,
                            arguments = listOf(navArgument("uri") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val uriStr = backStackEntry.arguments?.getString("uri") ?: ""
                            VideoPlayerScreen(
                                uriStr = uriStr,
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable(route = Screen.StatusSaverScreen.route) {
                            StatusSaverScreen(
                                onBackClick = { navController.popBackStack() },
                                onPreviewClick = { statusId ->
                                    navController.navigate(Screen.StatusPreviewScreen.createRoute(statusId))
                                }
                            )
                        }

                        composable(route = Screen.DownloadLinkScreen.route) {
                            DownloadLinkScreen(
                                onBackClick = { navController.popBackStack() },
                                onPreviewClick = { uriStr ->
                                    val encoded = java.net.URLEncoder.encode(uriStr, "UTF-8")
                                    navController.navigate(Screen.VideoPlayerScreen.createRoute(encoded))
                                }
                            )
                        }

                        composable(route = Screen.DownloadManagerScreen.route) {
                            DownloadManagerScreen(
                                onPlayClick = { item ->
                                    val encoded = java.net.URLEncoder.encode(item.filePath, "UTF-8")
                                    navController.navigate(Screen.VideoPlayerScreen.createRoute(encoded))
                                }
                            )
                        }

                        composable(route = Screen.CollectionScreen.route) {
                            CollectionScreen(
                                onLockedTabClick = {
                                    navController.navigate(Screen.LockScreen.route)
                                },
                                onMediaClick = { item ->
                                    if (item.path.isEmpty() && item.albumId.isNotEmpty()) {
                                        navController.navigate(Screen.CollectionAlbumVideosScreen.createRoute(item.albumId))
                                    } else {
                                        val encoded = java.net.URLEncoder.encode(item.path, "UTF-8")
                                        navController.navigate(Screen.VideoPlayerScreen.createRoute(encoded))
                                    }
                                },
                                onImportClick = {
                                    navController.navigate(Screen.AlbumListScreen.createRoute("Select Album"))
                                }
                            )
                        }

                        composable(route = Screen.SettingsScreen.route) {
                            SettingsScreen(
                                onLanguageClick = {
                                    navController.navigate(Screen.LanguageSelectionScreen.route)
                                },
                                onLockScreenClick = {
                                    navController.navigate(Screen.LockScreen.route)
                                }
                            )
                        }
                        
                        composable(
                            route = Screen.StatusPreviewScreen.route,
                            arguments = listOf(navArgument("statusId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val statusId = backStackEntry.arguments?.getString("statusId") ?: ""
                            val viewModel: StatusViewModel = hiltViewModel() 
                            StatusPreviewScreen(
                                statusId = statusId,
                                onBackClick = { navController.popBackStack() },
                                viewModel = viewModel
                            )
                        }

                        composable(route = Screen.LockScreen.route) {
                            val viewModel: LockViewModel = hiltViewModel()
                            LockScreen(
                                viewModel = viewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onUnlockSuccess = {
                                    navController.navigate(Screen.LockedVideosScreen.route) {
                                        popUpTo(Screen.LockScreen.route) { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable(route = Screen.LockedVideosScreen.route) {
                            LockedVideosScreen(
                                onNavigateBack = { navController.popBackStack() },
                                onVideoClick = { path ->
                                    val encoded = java.net.URLEncoder.encode(path, "UTF-8")
                                    navController.navigate(Screen.VideoPlayerScreen.createRoute(encoded))
                                }
                            )
                        }

                        composable(route = Screen.DirectChatScreen.route) {
                            DirectChatScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        composable(
                            route = Screen.AlbumListScreen.route,
                            arguments = listOf(navArgument("title") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val title = backStackEntry.arguments?.getString("title") ?: "Albums"
                            AlbumListScreen(
                                title = title,
                                onAlbumClick = { album ->
                                    navController.navigate(Screen.AlbumVideosScreen.createRoute(album.id, album.name))
                                },
                                onBackClick = { navController.popBackStack() }
                            )
                        }

                        composable(
                            route = Screen.AlbumVideosScreen.route,
                            arguments = listOf(
                                navArgument("albumId") { type = NavType.StringType },
                                navArgument("albumName") { type = NavType.StringType }
                            )
                        ) { backStackEntry ->
                            val albumId = backStackEntry.arguments?.getString("albumId") ?: ""
                            val albumName = backStackEntry.arguments?.getString("albumName") ?: "Videos"
                            AlbumVideosScreen(
                                albumId = albumId,
                                albumName = albumName,
                                onVideoClick = { video ->
                                    val encoded = java.net.URLEncoder.encode(video.path, "UTF-8")
                                    navController.navigate(Screen.VideoPlayerScreen.createRoute(encoded))
                                },
                                onBackClick = { navController.popBackStack() }
                            )
                        }
                        composable(
                            route = Screen.CollectionAlbumVideosScreen.route,
                            arguments = listOf(navArgument("albumId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val albumId = backStackEntry.arguments?.getString("albumId") ?: ""
                            // For collection albums, we can fetch the name from the ViewModel or just show "Album"
                            AlbumVideosScreen(
                                albumId = albumId,
                                albumName = "Album",
                                onVideoClick = { video ->
                                    val encoded = java.net.URLEncoder.encode(video.path, "UTF-8")
                                    navController.navigate(Screen.VideoPlayerScreen.createRoute(encoded))
                                },
                                onBackClick = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
