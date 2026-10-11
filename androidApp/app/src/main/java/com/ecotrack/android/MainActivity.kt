package com.ecotrack.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ecotrack.android.ui.navigation.EcoTrackNavigation
import com.ecotrack.android.ui.theme.EcoTrackTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EcoTrackTheme(darkTheme = false) {
                EcoTrackNavigation()
            }
        }
    }
}
