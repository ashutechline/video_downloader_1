package com.quickvideodownloader.app.presentation.ui.components
 
import androidx.compose.runtime.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.material3.*
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.ui.res.stringResource
import com.quickvideodownloader.app.R
import com.quickvideodownloader.app.presentation.navigation.Screen
 
data class NavItem(
    val title: String,
    val icon: ImageVector,
    val route: String
)
 
@Composable
fun BottomNavBar(navController: NavController) {
    val items = listOf(
        NavItem(stringResource(R.string.home), Icons.Default.GridView, Screen.HomeScreen.route),
        NavItem(stringResource(R.string.downloads), Icons.Default.Download, Screen.DownloadManagerScreen.route),
        NavItem(stringResource(R.string.collection), Icons.Default.Collections, Screen.CollectionScreen.route),
        NavItem(stringResource(R.string.settings), Icons.Default.Settings, Screen.SettingsScreen.route)
    )
 
 
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
 
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 0.dp,
        modifier = Modifier
            .background(Color.White)
            .drawBehind {
                drawLine(
                    color = Color(0xFFEBEBEB),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            }
    ) {
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title) },
                selected = currentRoute == item.route,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(Screen.HomeScreen.route) {
                                inclusive = false
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = Color.Transparent,
                    unselectedIconColor = Color(0xFFAAAAAA),
                    unselectedTextColor = Color(0xFFAAAAAA)
                )
            )
        }
    }
}
