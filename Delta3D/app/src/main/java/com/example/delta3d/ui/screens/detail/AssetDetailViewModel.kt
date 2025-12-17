package com.example.delta3d.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.delta3d.api.AssetDetail
import com.example.delta3d.api.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AssetDetailViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState = _uiState.asStateFlow()

    fun loadDetail(token: String, assetId: Int) {
        viewModelScope.launch {
            _uiState.value = DetailUiState.Loading
            try {
                val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
                val detail = RetrofitClient.api.getAssetDetail(authHeader, assetId)
                _uiState.value = DetailUiState.Success(detail)
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = DetailUiState.Error(e.message ?: "Unknown Error")
            }
        }
    }
}

sealed class DetailUiState {
    object Loading : DetailUiState()
    data class Success(val data: AssetDetail) : DetailUiState()
    data class Error(val msg: String) : DetailUiState()
}