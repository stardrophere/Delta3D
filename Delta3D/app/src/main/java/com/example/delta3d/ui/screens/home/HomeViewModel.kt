package com.example.delta3d.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.delta3d.api.AssetCard
import com.example.delta3d.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel : ViewModel() {
    private var _allAssets = listOf<AssetCard>()

    // 最终展示给 UI 的列表
    private val _displayAssets = MutableStateFlow<List<AssetCard>>(emptyList())
    val displayAssets = _displayAssets.asStateFlow()

    // 搜索框文字状态
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    //处理中任务数量
    private val _processingCount = MutableStateFlow(0)
    val processingCount = _processingCount.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    // 防抖任务句柄
    private var searchJob: Job? = null

    /**
     * 用户输入时调用此方法 (包含防抖逻辑)
     */
    fun onSearchInput(query: String) {
        // 立即更新 UI 文字
        _searchQuery.value = query

        //取消上一次未执行的搜索任务
        searchJob?.cancel()

        //启动新任务
        searchJob = viewModelScope.launch {
            // 防抖
            delay(600)
            refreshDisplayList()
        }
    }

    /**
     * 收藏切换
     */
    fun toggleCollect(assetId: Int, token: String) {
        viewModelScope.launch {
            val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"

            // 备份旧数据用于回滚
            val backupAssets = _allAssets

            // 立即更新本地 UI 状态
            updateLocalAssetStatus(assetId)

            try {
                val response = RetrofitClient.api.toggleCollect(authHeader, assetId)
                // 后端结果二次校验状态
                syncAssetStatus(assetId, response.is_active)
            } catch (e: Exception) {
                e.printStackTrace()
                //失败回滚
                _allAssets = backupAssets
                refreshDisplayList()
            }
        }
    }

    // 辅助：更新本地状态并刷新显示
    private fun updateLocalAssetStatus(id: Int) {
        _allAssets = _allAssets.map {
            if (it.id == id) it.copy(isCollected = !it.isCollected) else it
        }
        refreshDisplayList()
    }

    // 辅助：与服务器状态同步
    private fun syncAssetStatus(id: Int, isActive: Boolean) {
        _allAssets = _allAssets.map {
            if (it.id == id) it.copy(isCollected = isActive) else it
        }
        refreshDisplayList()
    }

    // 辅助：根据当前搜索词刷新 displayAssets
    private fun refreshDisplayList() {
        val query = _searchQuery.value.trim()
        val filtered = if (query.isEmpty()) {
            _allAssets.filter { it.status != "com" }
        } else {
            _allAssets.filter { asset ->
                asset.status == "completed" && (
                        asset.title.contains(query, true) ||
                                asset.tags.any { it.contains(query, true) }
                        )
            }
        }
        _displayAssets.value = filtered
    }

    // 获取数据
    fun loadAssets(token: String) {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
                val list = RetrofitClient.api.getAssets(authHeader)

                _allAssets = list
                _processingCount.value =
                    list.count { it.status == "pending" || it.status == "processing" }

                // 刷新显示列表
                refreshDisplayList()

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}