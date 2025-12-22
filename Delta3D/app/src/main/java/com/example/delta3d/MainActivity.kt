package com.example.delta3d

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.example.delta3d.ui.AppNavigation
import com.example.delta3d.ui.deltatree.DeltaTreeScreen
import com.example.delta3d.ui.screens.splash.Delta3DLogoSplash
import com.example.delta3d.ui.session.SessionViewModel
import com.example.delta3d.ui.theme.Delta3DTheme

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {

    private val sessionVm: SessionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val isFirstLaunch = prefs.getBoolean("show_opening_sequence", true)

        Log.d(TAG, "========== App 启动 ==========")
        Log.d(TAG, "isFirstLaunch = $isFirstLaunch")

        setContent {
            var currentScreen by remember {
                mutableStateOf(
                    if (isFirstLaunch) "delta_tree" else "main"
                )
            }

            Log.d(TAG, "setContent called, currentScreen = $currentScreen")

            Delta3DTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0F2027))
                ) {
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(150)) togetherWith
                                    fadeOut(animationSpec = tween(150))
                        },
                        label = "screen_transition"
                    ) { screen ->
                        when (screen) {
                            "delta_tree" -> {
                                Log.d(TAG, "Rendering DeltaTreeScreen")
                                DeltaTreeScreen(
                                    onDismiss = {
                                        Log.d(TAG, "DeltaTree completed, showing Logo")
                                        currentScreen = "logo"
                                    }
                                )
                            }

                            "logo" -> {
                                Log.d(TAG, "Rendering Delta3DLogoSplash")
                                Delta3DLogoSplash(
                                    onAnimationComplete = {
                                        Log.d(TAG, "Logo completed, entering main app")
                                        currentScreen = "main"
                                        prefs.edit().putBoolean("show_opening_sequence", false).apply()
                                    }
                                )
                            }

                            "main" -> {
                                Log.d(TAG, "Rendering AppNavigation")
                                AppNavigation(sessionVm)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Delta3DTheme {
        Greeting("Android")
    }
}