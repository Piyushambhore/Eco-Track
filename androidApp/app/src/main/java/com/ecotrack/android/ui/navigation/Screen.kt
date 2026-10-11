package com.ecotrack.android.ui.navigation

sealed class Screen(val route: String, val title: String) {
    object Auth : Screen("auth", "Sign In & Onboard")
    object Dashboard : Screen("dashboard", "Home")
    object Nearby : Screen("nearby", "Explore")
    object Scanner : Screen("scanner", "Scan")
    object Analysis : Screen("analysis", "Analysis")
    object Marketplace : Screen("marketplace", "Market")
    object Impact : Screen("impact", "Activity")
    object CollectorPortal : Screen("collector_portal", "Collector Portal")
    object Admin : Screen("admin", "Governance")
    object Profile : Screen("profile", "Profile")
    object Settings : Screen("settings", "Settings & AWS")
}
