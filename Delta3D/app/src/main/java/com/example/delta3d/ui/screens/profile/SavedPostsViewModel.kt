package com.example.delta3d.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.delta3d.api.PostCard
import com.example.delta3d.api.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SavedPostsViewModel : ViewModel() {
    private val _posts = MutableStateFlow<List<PostCard>>(emptyList())
    val posts = _posts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun loadCollectedPosts(token: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"

                val result = RetrofitClient.api.getCollectedPosts(authHeader)
                _posts.value = result
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // 处理点赞
    fun toggleLike(postId: Int, token: String) {
        viewModelScope.launch {
            // 1. 乐观更新 UI
            _posts.value = _posts.value.map {
                if (it.postId == postId) {
                    it.copy(
                        isLiked = !it.isLiked,
                        likeCount = if (it.isLiked) it.likeCount - 1 else it.likeCount + 1
                    )
                } else it
            }
            // 发送请求
            try {
                val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
                RetrofitClient.api.likePost(authHeader, postId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // 处理收藏
    fun toggleCollect(postId: Int, token: String) {
        viewModelScope.launch {
            // 乐观更新

            _posts.value = _posts.value.filter { it.postId != postId }

            // 发送请求
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
            // 乐观更新
            _posts.value = _posts.value.map {
                if (it.ownerId == ownerId) it.copy(isFollowing = !it.isFollowing) else it
            }
            try {
                val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
                RetrofitClient.api.followUser(authHeader, ownerId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}