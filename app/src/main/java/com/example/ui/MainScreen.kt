package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

@Composable
fun MainScreen(viewModel: FitnessViewModel) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF050505),
                contentColor = Color.White.copy(alpha = 0.5f),
                tonalElevation = 0.dp
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                NavigationBarItem(
                    icon = { Icon(if (currentDestination?.hierarchy?.any { it.route == "weight" } == true) Icons.Filled.MonitorWeight else Icons.Outlined.MonitorWeight, contentDescription = "Weight") },
                    label = { Text("Weight", style = if (currentDestination?.hierarchy?.any { it.route == "weight" } == true) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelSmall) },
                    selected = currentDestination?.hierarchy?.any { it.route == "weight" } == true,
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color.Transparent,
                        selectedIconColor = Color(0xFF0A84FF),
                        selectedTextColor = Color(0xFF0A84FF),
                        unselectedIconColor = Color(0xFF8E8E93),
                        unselectedTextColor = Color(0xFF8E8E93)
                    ),
                    onClick = {
                        navController.navigate("weight") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(if (currentDestination?.hierarchy?.any { it.route == "workout" } == true) Icons.AutoMirrored.Filled.List else Icons.AutoMirrored.Outlined.List, contentDescription = "Workouts") },
                    label = { Text("Workouts", style = if (currentDestination?.hierarchy?.any { it.route == "workout" } == true) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelSmall) },
                    selected = currentDestination?.hierarchy?.any { it.route == "workout" } == true,
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color.Transparent,
                        selectedIconColor = Color(0xFF0A84FF),
                        selectedTextColor = Color(0xFF0A84FF),
                        unselectedIconColor = Color(0xFF8E8E93),
                        unselectedTextColor = Color(0xFF8E8E93)
                    ),
                    onClick = {
                        navController.navigate("workout") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(if (currentDestination?.hierarchy?.any { it.route == "dashboard" } == true) Icons.Filled.Home else Icons.Outlined.Home, contentDescription = "Home") },
                    label = { Text("Home", style = if (currentDestination?.hierarchy?.any { it.route == "dashboard" } == true) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelSmall) },
                    selected = currentDestination?.hierarchy?.any { it.route == "dashboard" } == true,
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color.Transparent,
                        selectedIconColor = Color(0xFF0A84FF),
                        selectedTextColor = Color(0xFF0A84FF),
                        unselectedIconColor = Color(0xFF8E8E93),
                        unselectedTextColor = Color(0xFF8E8E93)
                    ),
                    onClick = {
                        navController.navigate("dashboard") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(if (currentDestination?.hierarchy?.any { it.route == "meals" } == true) Icons.Filled.Restaurant else Icons.Outlined.Restaurant, contentDescription = "Nutrition") },
                    label = { Text("Nutrition", style = if (currentDestination?.hierarchy?.any { it.route == "meals" } == true) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelSmall) },
                    selected = currentDestination?.hierarchy?.any { it.route == "meals" } == true,
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color.Transparent,
                        selectedIconColor = Color(0xFF0A84FF),
                        selectedTextColor = Color(0xFF0A84FF),
                        unselectedIconColor = Color(0xFF8E8E93),
                        unselectedTextColor = Color(0xFF8E8E93)
                    ),
                    onClick = {
                        navController.navigate("meals") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(if (currentDestination?.hierarchy?.any { it.route == "photos" } == true) Icons.Filled.CameraAlt else Icons.Outlined.CameraAlt, contentDescription = "Progress") },
                    label = { Text("Progress", style = if (currentDestination?.hierarchy?.any { it.route == "photos" } == true) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelSmall) },
                    selected = currentDestination?.hierarchy?.any { it.route == "photos" } == true,
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color.Transparent,
                        selectedIconColor = Color(0xFF0A84FF),
                        selectedTextColor = Color(0xFF0A84FF),
                        unselectedIconColor = Color(0xFF8E8E93),
                        unselectedTextColor = Color(0xFF8E8E93)
                    ),
                    onClick = {
                        navController.navigate("photos") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("weight") {
                WeightScreen(viewModel = viewModel)
            }
            composable("workout") {
                WorkoutScreen(viewModel = viewModel)
            }
            composable("meals") {
                MealsScreen(viewModel = viewModel)
            }
            composable("dashboard") {
                DashboardScreen(viewModel = viewModel, navController = navController)
            }
            composable("photos") {
                ProgressPhotosScreen(viewModel = viewModel)
            }
        }
    }
}
