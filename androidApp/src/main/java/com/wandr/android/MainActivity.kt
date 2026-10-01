package com.wandr.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.wandr.android.ui.theme.WandrTheme
import com.wandr.android.ui.navigation.WandrApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Official SplashScreen API: swaps Theme.Wandr.Starting for Theme.Wandr after the first frame.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            WandrTheme {
                WandrApp()
            }
        }
    }
}
