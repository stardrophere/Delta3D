package com.example.delta3d

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.delta3d.ui.theme.Delta3DTheme
import com.example.delta3d.ui.AppNavigation
import androidx.activity.viewModels
import com.example.delta3d.ui.session.SessionViewModel

class MainActivity : ComponentActivity() {

    private val sessionVm: SessionViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Delta3DTheme {
                AppNavigation(sessionVm)
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