package com.wandr.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wandr.wear.ui.WearApp
import com.wandr.wear.ui.WearViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: WearViewModel = viewModel()
            WearApp(viewModel)
        }
    }
}
