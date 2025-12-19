package com.example.delta3d.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.delta3d.api.PostDetail
import com.example.delta3d.api.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.delta3d.api.CommentCreateRequest
import com.example.delta3d.api.SendMessageRequest
import com.example.delta3d.utils.ShareLinkUtils
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow


sealed class PostUiState {
    object Loading : PostUiState()
    data class Success(val data: PostDetail) : PostUiState()
    data class Error(val msg: String) : PostUiState()
}

class PostDetailViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<PostUiState>(PostUiState.Loading)
    val uiState = _uiState.asStateFlow()

    //下载处理
    private val _downloadEvent = MutableSharedFlow<DownloadEvent>()
    val downloadEvent = _downloadEvent.asSharedFlow()

    fun loadPostDetail(token: String, postId: Int) {
        viewModelScope.launch {
            _uiState.value = PostUiState.Loading
            try {
                val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
                val detail = RetrofitClient.api.getPostDetail(authHeader, postId)
                _uiState.value = PostUiState.Success(detail)
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = PostUiState.Error(e.message ?: "Failed to load post")
            }
        }
    }

    // 点赞
    fun toggleLike(token: String) {
        val currentState = _uiState.value as? PostUiState.Success ?: return
        val currentData = currentState.data

        // 立即更新 UI
        val newLiked = !currentData.isLiked
        val newCount = if (newLiked) currentData.likeCount + 1 else currentData.likeCount - 1

        _uiState.value = PostUiState.Success(
            currentData.copy(isLiked = newLiked, likeCount = newCount)
        )

        //发送请求
        viewModelScope.launch {
            try {
                val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
                // 使用 camelCase 字段
                RetrofitClient.api.likePost(authHeader, currentData.postId)
            } catch (e: Exception) {
                e.printStackTrace()

            }
        }
    }

    // 收藏
    fun toggleCollect(token: String) {
        val currentState = _uiState.value as? PostUiState.Success ?: return
        val currentData = currentState.data

        // isCollected, collectCount
        val newCollected = !currentData.isCollected
        val newCount =
            if (newCollected) currentData.collectCount + 1 else currentData.collectCount - 1

        _uiState.value = PostUiState.Success(
            currentData.copy(isCollected = newCollected, collectCount = newCount)
        )

        viewModelScope.launch {
            try {
                val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
                RetrofitClient.api.collectPost(authHeader, currentData.postId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // 关注作者
    fun toggleFollow(token: String) {
        val currentState = _uiState.value as? PostUiState.Success ?: return
        val currentData = currentState.data

        // isFollowing
        val newFollowing = !currentData.isFollowing

        _uiState.value = PostUiState.Success(
            currentData.copy(isFollowing = newFollowing)
        )

        viewModelScope.launch {
            try {
                val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
                // ownerId
                RetrofitClient.api.followUser(authHeader, currentData.ownerId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    //评论
    fun sendComment(token: String, content: String) {
        if (content.isBlank()) return

        val currentState = _uiState.value as? PostUiState.Success ?: return
        val currentData = currentState.data

        viewModelScope.launch {
            try {
                val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"

                // 发送请求
                val newComment = RetrofitClient.api.createComment(
                    token = authHeader,
                    postId = currentData.postId,
                    request = CommentCreateRequest(content = content)
                )

                // 成功后，更新本地 UI 数据
                val updatedComments = currentData.comments + newComment
                val updatedCount = currentData.commentCount + 1

                _uiState.value = PostUiState.Success(
                    currentData.copy(
                        comments = updatedComments,
                        commentCount = updatedCount,

                        )
                )

            } catch (e: Exception) {
                e.printStackTrace()

            }
        }
    }

    fun downloadAsset(token: String, assetId: Int, formatUiLabel: String) {
        viewModelScope.launch {
            try {
                // 格式转换
                val fileType = when {
                    formatUiLabel.contains("OBJ") -> "obj"
                    formatUiLabel.contains("GLB") -> "glb"
                    formatUiLabel.contains("PLY") -> "ply"
                    else -> "msgpack"
                }

                val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"


                val response = RetrofitClient.api.downloadAsset(authHeader, assetId, fileType)


                _downloadEvent.emit(DownloadEvent.Success(response.url, response.filename))

            } catch (e: Exception) {
                e.printStackTrace()
                _downloadEvent.emit(DownloadEvent.Error("Download Failed: ${e.message}"))
            }
        }
    }


    //分享帖子链接
    fun sharePostToUser(token: String, targetUserId: Int) {
        val currentState = _uiState.value as? PostUiState.Success ?: return
        val post = currentState.data

        // 生成富媒体链接
        val shareContent = ShareLinkUtils.generatePostLink(
            postId = post.postId,
            title = post.asset.title,
            coverUrl = post.asset.videoUrl
        )

        viewModelScope.launch {
            try {
                val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"

                //调用 HTTP 接口发送消息
                RetrofitClient.api.sendChatMessage(
                    token = authHeader,
                    request = SendMessageRequest(
                        receiverId = targetUserId,
                        content = shareContent
                    )
                )


            } catch (e: Exception) {
                e.printStackTrace()

            }
        }
    }
}