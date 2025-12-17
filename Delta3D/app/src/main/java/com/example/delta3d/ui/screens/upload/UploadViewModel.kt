package com.example.delta3d.ui.screens.upload

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.delta3d.api.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import android.util.Log

class UploadViewModel : ViewModel() {

    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState = _uploadState.asStateFlow()

    // 🟢 修改 1: 将标签列表改为 StateFlow，以便 UI 动态更新
    // 我们可以保留一些基础标签作为初始值，防止用户没有任何数据时标签栏为空
    private val defaultTags = listOf("Human", "Animal", "Building", "Car", "Nature", "Sci-Fi")

    private val _suggestedTags = MutableStateFlow<List<String>>(defaultTags)
    val suggestedTags = _suggestedTags.asStateFlow()


    // 🟢 修改 2: 新增获取用户历史标签的方法
    fun fetchUserTags(token: String) {

        viewModelScope.launch {
            try {
                val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"




                val assets = RetrofitClient.api.getAssets(authHeader)



                val userUsedTags = assets
                    .flatMap { it.tags }
                    .filter { it.isNotBlank() }
                    .toSet()

                // 🟢 Log 4: 打印处理后的标签
//                Log.d("UploadDebug", "User tags extracted: $userUsedTags")

                val combinedTags = (userUsedTags + defaultTags).distinct().sorted()
                _suggestedTags.value = combinedTags

            } catch (e: Exception) {

                e.printStackTrace()
            }
        }
    }

    fun uploadFile(
        context: Context,
        videoUri: Uri,
        token: String,
        title: String,
        description: String,
        remark: String,
        tags: List<String>,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _uploadState.value = UploadState.Loading
            try {
                // 1. 将 Uri 转换为临时文件
                val file = uriToFile(context, videoUri) ?: throw Exception("File processing failed")

                // 2. 准备 RequestBody
                val requestFile = file.asRequestBody("video/mp4".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("file", "video.mp4", requestFile)

                val titleBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
                val descBody = description.toRequestBody("text/plain".toMediaTypeOrNull())
                val remarkBody = remark.toRequestBody("text/plain".toMediaTypeOrNull())
                // 将标签列表转为 "tag1,tag2" 格式
                val tagsBody =
                    tags.joinToString(",").toRequestBody("text/plain".toMediaTypeOrNull())

                val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"

                // 3. 调用 API
                RetrofitClient.api.uploadAsset(
                    token = authHeader,
                    file = body,
                    title = titleBody,
                    description = descBody,
                    remark = remarkBody,
                    tags = tagsBody
                )

                _uploadState.value = UploadState.Success
                onSuccess()
                file.delete() // 清理临时文件

            } catch (e: Exception) {
                e.printStackTrace()
                _uploadState.value = UploadState.Error(e.message ?: "Upload failed")
            }
        }
    }

    // --- 核心工具：Uri -> File ---
    private fun uriToFile(context: Context, uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val tempFile = File.createTempFile("upload_cache", ".mp4", context.cacheDir)
            val outputStream = FileOutputStream(tempFile)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

sealed class UploadState {
    data object Idle : UploadState()
    data object Loading : UploadState()
    data object Success : UploadState()
    data class Error(val message: String) : UploadState()
}