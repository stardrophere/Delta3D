package com.example.delta3d.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.delta3d.api.AssetCard
import com.example.delta3d.api.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DownloadHistoryViewModel : ViewModel() {
    private val _assets = MutableStateFlow<List<AssetCard>>(emptyList())
    val assets: StateFlow<List<AssetCard>> = _assets.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadDownloadHistory(token: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val auth = if (token.startsWith("Bearer ")) token else "Bearer $token"
                val list = RetrofitClient.api.getMyDownloads(auth)
                _assets.value = list
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}