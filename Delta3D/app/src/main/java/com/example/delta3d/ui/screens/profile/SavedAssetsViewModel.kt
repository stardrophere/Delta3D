package com.example.delta3d.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.delta3d.api.AssetCard
import com.example.delta3d.api.RetrofitClient
import com.example.delta3d.ui.session.SessionViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SavedAssetsViewModel : ViewModel() {
    private val _assets = MutableStateFlow<List<AssetCard>>(emptyList())
    val assets: StateFlow<List<AssetCard>> = _assets.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadCollectedAssets(token: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val auth = if (token.startsWith("Bearer ")) token else "Bearer $token"

                val list = RetrofitClient.api.getCollectedAssets(auth)
                _assets.value = list
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleCollect(assetId: Int, token: String) {
        viewModelScope.launch {
            try {
                val auth = if (token.startsWith("Bearer ")) token else "Bearer $token"
                val response = RetrofitClient.api.toggleCollect(auth, assetId)


                if (!response.is_active) {
                    _assets.value = _assets.value.filter { it.id != assetId }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}