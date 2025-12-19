package com.example.delta3d.ui.screens.profile

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.delta3d.api.ApiService
import com.example.delta3d.api.RetrofitClient
import com.example.delta3d.api.UserOut
import com.example.delta3d.api.UserUpdate
import com.example.delta3d.ui.session.SessionViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

class ProfileViewModel : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _userList = MutableStateFlow<List<UserOut>>(emptyList())
    val userList: StateFlow<List<UserOut>> = _userList.asStateFlow()

    // 辅助方法：将 Uri 转为 File
    private fun uriToFile(context: Context, uri: Uri): File? {
        val contentResolver = context.contentResolver
        val tempFile = File.createTempFile("upload", ".jpg", context.cacheDir)
        try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val outputStream = FileOutputStream(tempFile)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            return tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun uploadImage(
        context: Context,
        uri: Uri,
        isAvatar: Boolean, // True=头像, False=背景
        token: String,
        sessionVm: SessionViewModel // 用于上传成功后刷新全局用户信息
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val file = uriToFile(context, uri)
                if (file != null) {
                    val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                    val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

                    val auth = if (token.startsWith("Bearer ")) token else "Bearer $token"

                    // 调用接口
                    val updatedUser = if (isAvatar) {
                        RetrofitClient.api.updateAvatar(auth, body)
                    } else {
                        RetrofitClient.api.updateCover(auth, body)
                    }

                    // 刷新 Session 中的当前用户
                    sessionVm.refreshUser(updatedUser)
                }
            } catch (e: Exception) {
                Log.e("ProfileVM", "Upload failed: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateProfile(
        username: String,
        bio: String,
        gender: String,
        token: String,
        sessionVm: SessionViewModel
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val auth = if (token.startsWith("Bearer ")) token else "Bearer $token"
                val updateData = UserUpdate(username = username, bio = bio, gender = gender)
                val updatedUser = RetrofitClient.api.updateProfile(auth, updateData)
                sessionVm.refreshUser(updatedUser)
            } catch (e: Exception) {
                Log.e("ProfileVM", "Update profile failed: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // 获取列表 (粉丝 or 关注)
    fun fetchUserList(userId: Int, type: String, token: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val auth = if (token.startsWith("Bearer ")) token else "Bearer $token"
                val list = if (type == "followers") {
                    RetrofitClient.api.getUserFollowers(auth, userId)
                } else {
                    RetrofitClient.api.getUserFollowing(auth, userId)
                }
                _userList.value = list
            } catch (e: Exception) {
                Log.e("ProfileVM", "Fetch list failed: ${e.message}")
                _userList.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // 在列表页进行取关/关注操作
    fun toggleFollowInList(targetId: Int, token: String) {
        viewModelScope.launch {
            try {
                val auth = if (token.startsWith("Bearer ")) token else "Bearer $token"
                RetrofitClient.api.toggleFollowUser(auth, targetId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}