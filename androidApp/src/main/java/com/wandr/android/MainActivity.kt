package com.wandr.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.wandr.android.push.PushExtras
import com.wandr.android.push.PushNavigation
import com.wandr.android.push.PushPayload
import com.wandr.android.ui.theme.WandrTheme
import com.wandr.android.ui.navigation.WandrApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Official SplashScreen API: swaps Theme.Wandr.Starting for Theme.Wandr after the first frame.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handlePushIntent(intent)
        setContent {
            WandrTheme {
                WandrApp()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handlePushIntent(intent)
    }

    /** Tapped push notification (own or FCM-displayed): the data keys arrive as intent extras. */
    private fun handlePushIntent(intent: Intent?) {
        val target = PushPayload.target(
            intent?.getStringExtra(PushExtras.ENTITY_TYPE),
            intent?.getStringExtra(PushExtras.ENTITY_ID)
        ) ?: return
        PushNavigation.publish(target)
        // Do not re-deliver the target on configuration changes.
        intent?.removeExtra(PushExtras.ENTITY_TYPE)
        intent?.removeExtra(PushExtras.ENTITY_ID)
    }
}
