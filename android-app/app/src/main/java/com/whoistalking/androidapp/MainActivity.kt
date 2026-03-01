package com.whoistalking.androidapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.core.view.WindowCompat
import com.whoistalking.core.ui.theme.WhoIsTalkingTheme
import com.whoistalking.feature.session.ui.SessionRoute
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            WhoIsTalkingTheme {
                Surface {
                    SessionRoute()
                }
            }
        }
    }
}
