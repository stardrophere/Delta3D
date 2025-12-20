package com.example.delta3d.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.delta3d.api.AssetDetail
import com.example.delta3d.api.AssetUpdateRequest
import com.example.delta3d.api.ReportRequest
import com.example.delta3d.api.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class AssetDetailViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState = _uiState.asStateFlow()

    //下载流通知
    private val _downloadEvent = MutableSharedFlow<DownloadEvent>()
    val downloadEvent = _downloadEvent.asSharedFlow()

    fun downloadAsset(token: String, assetId: Int, formatUiLabel: String) {
        viewModelScope.launch {
            try {
                // 文件类型映射
                val fileType = when {
                    formatUiLabel.contains("OBJ") -> "obj"
                    formatUiLabel.contains("GLB") -> "glb"
                    formatUiLabel.contains("PLY") -> "ply"
                    else -> "msgpack" // Source Data
                }

                val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"

                // 调用 API
                val response = RetrofitClient.api.downloadAsset(authHeader, assetId, fileType)

                _downloadEvent.emit(DownloadEvent.Success(response.url, response.filename))

            } catch (e: Exception) {
                e.printStackTrace()
                _downloadEvent.emit(DownloadEvent.Error("Download Request Failed: ${e.message}"))
            }
        }
    }

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


    fun refreshDetail(token: String, assetId: Int) {
        viewModelScope.launch {
            try {
                val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
                val detail = RetrofitClient.api.getAssetDetail(authHeader, assetId)

                _uiState.value = DetailUiState.Success(detail)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    //状态更新
    fun updateAssetInfo(
        token: String,
        assetId: Int,
        title: String,
        description: String,
        remark: String,
        tags: List<String>
    ) {
        viewModelScope.launch {
            try {
                val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
                val request = AssetUpdateRequest(title, description, remark, tags)

                // 调用 API 更新
                val updatedDetail = RetrofitClient.api.updateAsset(authHeader, assetId, request)

                _uiState.value = DetailUiState.Success(updatedDetail)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // 问题举报
    fun reportIssue(
        token: String,
        assetId: Int,
        category: String,
        content: String,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
                val request = ReportRequest(category, content)
                RetrofitClient.api.reportAsset(authHeader, assetId, request)
                onComplete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

sealed class DetailUiState {
    object Loading : DetailUiState()
    data class Success(val data: AssetDetail) : DetailUiState()
    data class Error(val msg: String) : DetailUiState()
}


sealed class DownloadEvent {
    data class Success(val url: String, val filename: String) : DownloadEvent()
    data class Error(val msg: String) : DownloadEvent()
}
