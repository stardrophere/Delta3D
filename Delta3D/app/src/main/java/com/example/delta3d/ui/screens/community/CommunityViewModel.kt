package com.example.delta3d.ui.screens.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.delta3d.api.PostCard
import com.example.delta3d.api.RetrofitClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.json.JSONObject

class CommunityViewModel : ViewModel() {
    private var _allPosts = listOf<PostCard>() // 原始数据
    private val _displayPosts = MutableStateFlow<List<PostCard>>(emptyList()) // UI展示数据
    val displayPosts = _displayPosts.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private var searchJob: Job? = null

    // 用于发送一次性 UI 事件
    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent = _uiEvent.asSharedFlow()
    private val _onlyShowFollowing = MutableStateFlow(false)
    val onlyShowFollowing = _onlyShowFollowing.asStateFlow()

    fun setFilterMode(onlyFollowing: Boolean) {
        _onlyShowFollowing.value = onlyFollowing
        refreshDisplayList()
    }

    // 加载帖子
    fun loadPosts(token: String) {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
                _allPosts = RetrofitClient.api.getCommunityPosts(authHeader)
                refreshDisplayList()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    // 搜索
    fun onSearchInput(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(800)
            refreshDisplayList()
        }
    }


    private fun refreshDisplayList() {
        val query = _searchQuery.value.trim()
        val showFollowing = _onlyShowFollowing.value

        // 过滤是否只看关注
        var filtered = if (showFollowing) {
            _allPosts.filter { it.isFollowing }
        } else {
            _allPosts
        }

        // 进行搜索词过滤
        if (query.isNotEmpty()) {
            filtered = filtered.filter {
                it.title.contains(query, true) ||
                        (it.description?.contains(query, true) == true) ||
                        it.ownerName.contains(query, true) ||
                        it.tags.any { tag -> tag.contains(query, true) }
            }
        }

        _displayPosts.value = filtered
    }

    // 点赞
    fun toggleLike(postId: Int, token: String) {
        viewModelScope.launch {
            updateLocalPost(postId) {
                it.copy(
                    isLiked = !it.isLiked,
                    likeCount = if (it.isLiked) it.likeCount - 1 else it.likeCount + 1
                )
            }
            try {
                val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
                RetrofitClient.api.likePost(authHeader, postId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // 收藏
    fun toggleCollect(postId: Int, token: String) {
        viewModelScope.launch {
            updateLocalPost(postId) {
                it.copy(
                    isCollected = !it.isCollected,
                    collectCount = if (it.isCollected) it.collectCount - 1 else it.collectCount + 1
                )
            }
            try {
                val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
                RetrofitClient.api.collectPost(authHeader, postId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // 关注用户
    fun toggleFollow(ownerId: Int, token: String) {
        viewModelScope.launch {
            // 记录原始状态
            val originalState = _allPosts.find { it.ownerId == ownerId }?.isFollowing ?: false

            // 乐观更新
            updateLocalFollowState(ownerId, !originalState)

            try {
                val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
                // 发起请求
                val response = RetrofitClient.api.followUser(authHeader, ownerId)

                // 二次确认
                if (response.is_active != !originalState) {
                    updateLocalFollowState(ownerId, response.is_active)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // 失败回滚：改回原来的状态
                updateLocalFollowState(ownerId, originalState)

                // 解析错误并发送给 UI
                val errorMsg = parseErrorMessage(e)
                _uiEvent.emit(errorMsg)
            }
        }
    }


    private fun parseErrorMessage(e: Exception): String {
        return when (e) {
            is HttpException -> {
                try {
                    val errorBody = e.response()?.errorBody()?.string()
                    // 尝试解析 JSON: {"detail": "不能关注自己"}
                    if (errorBody != null) {
                        val json = JSONObject(errorBody)
                        json.optString("detail", "操作失败")
                    } else {
                        "网络请求失败"
                    }
                } catch (e: Exception) {
                    "操作失败"
                }
            }

            else -> "网络连接异常"
        }
    }

    // 批量更新本地列表中某位作者的关注状态
    private fun updateLocalFollowState(ownerId: Int, isFollowing: Boolean) {
        _allPosts = _allPosts.map {
            if (it.ownerId == ownerId) it.copy(isFollowing = isFollowing) else it
        }
        refreshDisplayList() // 刷新 UI 流
    }

    private fun updateLocalPost(postId: Int, update: (PostCard) -> PostCard) {
        _allPosts = _allPosts.map { if (it.postId == postId) update(it) else it }
        refreshDisplayList()
    }

    fun showToast(message: String) {
        viewModelScope.launch {
            _uiEvent.emit(message)
        }
    }


}