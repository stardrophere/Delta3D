package com.example.delta3d.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.delta3d.ui.screens.home.ProductListView
import com.example.delta3d.ui.session.SessionViewModel
import com.example.delta3d.ui.screens.auth.AnimatedGradientBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedAssetsScreen(
    sessionVm: SessionViewModel,
    onBack: () -> Unit,
    onNavigateToDetail: (Int) -> Unit,
    viewModel: SavedAssetsViewModel = viewModel()
) {
    val token by sessionVm.token.collectAsState()
    val assets by viewModel.assets.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(token) {
        token?.let { viewModel.loadCollectedAssets(it) }
    }


    Box(modifier = Modifier.fillMaxSize()) {

        AnimatedGradientBackground()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "Model Collections",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color(0xFF64FFDA)
                    )
                } else if (assets.isEmpty()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "No collections yet",
                            color = Color.White.copy(0.5f),
                            fontSize = 16.sp
                        )
                    }
                } else {
                    ProductListView(
                        dataList = assets,
                        bottomPadding = 0.dp,
                        onItemClick = { assetId ->
                            onNavigateToDetail(assetId)
                        },
                        onCollectClick = { assetId ->
                            token?.let { viewModel.toggleCollect(assetId, it) }
                        }
                    )
                }
            }
        }
    }
}