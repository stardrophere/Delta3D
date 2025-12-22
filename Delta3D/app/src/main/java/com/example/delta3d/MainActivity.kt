package com.example.delta3d

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import com.example.delta3d.ui.AppNavigation
import com.example.delta3d.ui.session.SessionViewModel
import com.example.delta3d.ui.theme.Delta3DTheme

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {

    private val sessionVm: SessionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        Log.d(TAG, "========== App 启动 ==========")

        setContent {
            val isFirstLaunch by sessionVm.isFirstLaunch.collectAsState()

            Delta3DTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colorResource(id = R.color.splash_background))
                ) {
                    AppNavigation(
                        sessionVm = sessionVm,
                        isFirstLaunch = isFirstLaunch
                    )
                }
            }
        }
    }
}