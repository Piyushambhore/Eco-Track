package com.ecotrack.android.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.compose.*
import com.ecotrack.android.ui.screens.*
import com.ecotrack.android.ui.theme.*

@Composable
fun EcoTrackNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Auth.route

    // Screens where floating bottom navigation is displayed
    val showBottomBar = currentRoute in listOf(
        Screen.Dashboard.route,
        Screen.Nearby.route,
        Screen.Marketplace.route,
        Screen.Impact.route,
        Screen.Settings.route
    )

    var activeScanCategory by remember { mutableStateOf("Plastic") }

    // Proactively connect to Ktor backend on launch
    LaunchedEffect(Unit) {
        com.ecotrack.android.data.api.EcoTrackApiClient.ensureAuthenticated()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Screen.Auth.route,
            modifier = Modifier.fillMaxSize(),
            enterTransition = { fadeIn(animationSpec = tween(280)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(280)) },
            exitTransition = { fadeOut(animationSpec = tween(280)) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(280)) },
            popEnterTransition = { fadeIn(animationSpec = tween(280)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(280)) },
            popExitTransition = { fadeOut(animationSpec = tween(280)) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(280)) }
        ) {
            // Authentication & Onboarding Screen with Map
            composable(Screen.Auth.route) {
                AuthScreen(
                    onCitizenAuthSuccess = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Auth.route) { inclusive = true }
                        }
                    },
                    onCollectorAuthSuccess = {
                        navController.navigate(Screen.CollectorPortal.route) {
                            popUpTo(Screen.Auth.route) { inclusive = true }
                        }
                    }
                )
            }

            // Screen 1: Home Dashboard
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onNavigateToScanner = { navController.navigate(Screen.Scanner.route) },
                    onNavigateToNearby = { navController.navigate(Screen.Nearby.route) },
                    onNavigateToAnalysis = { navController.navigate(Screen.Analysis.route) },
                    onNavigateToImpact = { navController.navigate(Screen.Impact.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToAuth = {
                        navController.navigate(Screen.Auth.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            // Screen 2: AI Waste Scanner
            composable(Screen.Scanner.route) {
                ScannerScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToAnalysis = { category ->
                        activeScanCategory = category
                        navController.navigate(Screen.Analysis.route)
                    }
                )
            }

            // Screen 3: Waste Analysis & Disposal
            composable(Screen.Analysis.route) {
                AnalysisScreen(
                    categoryName = activeScanCategory,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToNearby = { navController.navigate(Screen.Nearby.route) }
                )
            }

            // Screen 4: Nearby Services & Collection
            composable(Screen.Nearby.route) {
                NearbyScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Screen 5: Scrap Marketplace
            composable(Screen.Marketplace.route) {
                MarketplaceScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Screen 6: Impact & Rewards
            composable(Screen.Impact.route) {
                ImpactScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Screen 7: Recycler & Scrap Dealer Portal
            composable(Screen.CollectorPortal.route) {
                CollectorPortalScreen(
                    onLogout = {
                        navController.navigate(Screen.Auth.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            // Settings & AWS Cloud Architecture
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Citizen Profile Screen
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onLogout = {
                        navController.navigate(Screen.Auth.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateToAdmin = { navController.navigate(Screen.Admin.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }

            // Municipal Governance Admin
            composable(Screen.Admin.route) {
                AdminScreen(
                    onNavigateToDashboard = { navController.popBackStack() }
                )
            }
        }

        // Floating White Pill Bottom Navigation Bar (as specified in ui.md)
        if (showBottomBar) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 20.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                // Floating White Pill Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .shadow(elevation = 12.dp, shape = RoundedCornerShape(999.dp), spotColor = Color(0x22171A16))
                        .clip(RoundedCornerShape(999.dp))
                        .background(EcoCard)
                        .border(1.dp, EcoBorder, RoundedCornerShape(999.dp))
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Home
                        val isHome = currentRoute == Screen.Dashboard.route
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (isHome) EcoBrand else Color.Transparent)
                                .clickable {
                                    if (!isHome) {
                                        navController.navigate(Screen.Dashboard.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "Home",
                                tint = if (isHome) EcoBrandDark else EcoMuted,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // 2. Nearby / Explore
                        val isNearby = currentRoute == Screen.Nearby.route
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (isNearby) EcoBrand else Color.Transparent)
                                .clickable {
                                    if (!isNearby) {
                                        navController.navigate(Screen.Nearby.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Explore,
                                contentDescription = "Explore",
                                tint = if (isNearby) EcoBrandDark else EcoMuted,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Center Spacer for Raised Scan Button
                        Spacer(modifier = Modifier.size(54.dp))

                        // 4. Marketplace
                        val isMarket = currentRoute == Screen.Marketplace.route
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (isMarket) EcoBrand else Color.Transparent)
                                .clickable {
                                    if (!isMarket) {
                                        navController.navigate(Screen.Marketplace.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = "Marketplace",
                                tint = if (isMarket) EcoBrandDark else EcoMuted,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // 5. Settings & AWS Cloud Architecture
                        val isSettings = currentRoute == Screen.Settings.route
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (isSettings) EcoBrand else Color.Transparent)
                                .clickable {
                                    if (!isSettings) {
                                        navController.navigate(Screen.Settings.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings & AWS",
                                tint = if (isSettings) EcoBrandDark else EcoMuted,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // 3. Central Raised Scan Button (54dp Lime Circle, raised 14dp, white ring border)
                Box(
                    modifier = Modifier
                        .offset(y = (-14).dp)
                        .size(54.dp)
                        .shadow(elevation = 10.dp, shape = CircleShape, spotColor = EcoBrandDark.copy(alpha = 0.3f))
                        .clip(CircleShape)
                        .background(EcoBrand)
                        .border(3.dp, Color.White, CircleShape)
                        .clickable { navController.navigate(Screen.Scanner.route) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Scan",
                        tint = EcoBrandDark,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}
