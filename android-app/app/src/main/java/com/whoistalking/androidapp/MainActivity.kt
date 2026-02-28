package com.whoistalking.androidapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.whoistalking.androidapp.ui.AppRoot
import com.whoistalking.androidapp.ui.theme.WhoIsTalkingTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WhoIsTalkingTheme {
                AppRoot(viewModel = viewModel)
            }
        }
    }
}
