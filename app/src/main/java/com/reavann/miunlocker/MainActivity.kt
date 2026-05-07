package com.reavann.miunlocker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.reavann.miunlocker.ui.MainRoute
import com.reavann.miunlocker.ui.MainViewModel
import com.reavann.miunlocker.ui.theme.MiUnlockerTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels {
        MainViewModel.factory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MiUnlockerTheme {
                MainRoute(viewModel = viewModel)
            }
        }
    }
}
