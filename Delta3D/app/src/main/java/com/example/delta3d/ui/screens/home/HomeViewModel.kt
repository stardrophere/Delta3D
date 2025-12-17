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
    // 1. 原始完整数据源 (不对外暴露，仅用于过滤源)
    private var _allAssets = listOf<AssetCard>()

    // 2. 最终展示给 UI 的列表 (经过了状态筛选和搜索过滤)
    private val _displayAssets = MutableStateFlow<List<AssetCard>>(emptyList())
    val displayAssets = _displayAssets.asStateFlow()

    // 3. 搜索框文字状态 (用于 UI 即时回显)
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // 4. 处理中任务数量
    private val _processingCount = MutableStateFlow(0)
    val processingCount = _processingCount.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    // 🟢 防抖任务句柄
    private var searchJob: Job? = null

    /**
     * 用户输入时调用此方法 (包含防抖逻辑)
     */
    fun onSearchInput(query: String) {
        // 1. 立即更新 UI 文字，保证输入框不卡顿
        _searchQuery.value = query

        // 2. 取消上一次未执行的搜索任务
        searchJob?.cancel()

        // 3. 启动新任务
        searchJob = viewModelScope.launch {
            // ⏳ 防抖核心：如果用户 500ms 内连续输入，之前的任务会被 cancel
            delay(500)
            // 时间到了，在后台线程执行过滤
            refreshDisplayList()
        }
    }

    /**
     * 🟢 收藏切换 (乐观更新)
     */
    fun toggleCollect(assetId: Int, token: String) {
        viewModelScope.launch {
            val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"

            // 1. 备份旧数据用于回滚
            val backupAssets = _allAssets

            // 2. 立即更新本地 UI 状态 (乐观更新)
            updateLocalAssetStatus(assetId)

            try {
                // 3. 调用后端接口
                val response = RetrofitClient.api.toggleCollect(authHeader, assetId)
                // 4. 根据后端结果二次校验状态（确保同步）
                syncAssetStatus(assetId, response.is_active)
            } catch (e: Exception) {
                e.printStackTrace()
                // 5. 失败则回滚
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
                _processingCount.value = list.count { it.status == "pending" || it.status == "processing" }

                // 刷新显示列表（应用当前的搜索词）
                refreshDisplayList()

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}