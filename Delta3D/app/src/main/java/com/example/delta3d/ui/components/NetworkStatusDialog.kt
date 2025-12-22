package com.example.delta3d.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.delta3d.config.NetworkMode
import com.example.delta3d.ui.screens.profile.GlassCard

@Composable
fun NetworkStatusDialog(
    mode: NetworkMode,
    onDismiss: () -> Unit
) {
    val isLan = mode == NetworkMode.LAN
    val title = if (isLan) "LAN Connected" else "Public Network Connected"
    val icon = if (isLan) Icons.Rounded.Wifi else Icons.Rounded.Public
    val color = if (isLan) Color(0xFF64FFDA) else Color(0xFFFFD740)
    val limitText = if (isLan) "All features available" else "Some features available"

    Dialog(onDismissRequest = onDismiss) {
        GlassCard {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(color.copy(0.1f), CircleShape)
                        .border(1.dp, color.copy(0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(32.dp))
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    limitText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(0.7f)
                )

                if (!isLan) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "LAN unavailable. Switched to public server.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(0.5f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(Modifier.height(12.dp))

                    Text(
                        "Warning: Model preview and creation may fail due to network.",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFFF9E80),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = color),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("OK", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}