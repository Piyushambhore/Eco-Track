package com.ecotrack.android.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Auth : Screen("auth", "Sign In")
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object Scanner : Screen("scanner", "AI Scanner", Icons.Default.CameraAlt)
    object Reports : Screen("reports", "Reports", Icons.Default.ListAlt)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)

    companion object {
        val bottomNavItems = listOf(Dashboard, Scanner, Reports, Profile)
    }
}
