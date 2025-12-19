package com.example.delta3d.ui.screens.upload

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns // 🟢 新增
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.delta3d.api.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat // 🟢 新增


class UploadViewModel : ViewModel() {

    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState = _uploadState.asStateFlow()

    private val defaultTags = listOf("Human", "Animal", "Building", "Car", "Nature", "Sci-Fi")
    private val _suggestedTags = MutableStateFlow<List<String>>(defaultTags)
    val suggestedTags = _suggestedTags.asStateFlow()

    //文件大小和预估时间
    private val _fileSizeStr = MutableStateFlow("Calculated...")
    val fileSizeStr = _fileSizeStr.asStateFlow()

    private val _estimatedTimeStr = MutableStateFlow("Calculating...")
    val estimatedTimeStr = _estimatedTimeStr.asStateFlow()

    private var calculatedSecondsInt: Int? = null

    // 计算文件大小和预估时间的逻辑
    fun calculateFileInfo(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                var sizeBytes: Long = 0
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst() && sizeIndex >= 0) {
                        sizeBytes = cursor.getLong(sizeIndex)
                    }
                }

                // 格式化文件大小
                val sizeMb = sizeBytes / (1024.0 * 1024.0)
                val df = DecimalFormat("#.##")
                _fileSizeStr.value = "${df.format(sizeMb)} MB"

                // 估算公式
                val baseTime = 30
                val variableTime = (sizeMb / 4.0) * 15.0
                val totalSeconds = (baseTime + variableTime).toInt()

                calculatedSecondsInt = totalSeconds

                // 格式化时间显示
                val minutes = totalSeconds / 60
                val seconds = totalSeconds % 60
                _estimatedTimeStr.value = if (minutes > 0) {
                    "$minutes min $seconds sec"
                } else {
                    "$seconds sec"
                }

            } catch (e: Exception) {
                _fileSizeStr.value = "Unknown"
                _estimatedTimeStr.value = "Unknown"
                calculatedSecondsInt = null
            }
        }
    }

    fun fetchUserTags(token: String) {
        viewModelScope.launch {
            try {
                val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
                val assets = RetrofitClient.api.getAssets(authHeader)
                val userUsedTags = assets
                    .flatMap { it.tags }
                    .filter { it.isNotBlank() }
                    .toSet()
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
                val file = uriToFile(context, videoUri) ?: throw Exception("File processing failed")
                val requestFile = file.asRequestBody("video/mp4".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("file", "video.mp4", requestFile)
                val titleBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
                val descBody = description.toRequestBody("text/plain".toMediaTypeOrNull())
                val remarkBody = remark.toRequestBody("text/plain".toMediaTypeOrNull())
                val tagsBody =
                    tags.joinToString(",").toRequestBody("text/plain".toMediaTypeOrNull())
                val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
                val timeBody = calculatedSecondsInt?.toString()
                    ?.toRequestBody("text/plain".toMediaTypeOrNull())

                RetrofitClient.api.uploadAsset(
                    token = authHeader,
                    file = body,
                    title = titleBody,
                    description = descBody,
                    remark = remarkBody,
                    tags = tagsBody,
                    estimatedTime = timeBody
                )

                _uploadState.value = UploadState.Success
                onSuccess()
                file.delete()
            } catch (e: Exception) {
                e.printStackTrace()
                _uploadState.value = UploadState.Error(e.message ?: "Upload failed")
            }
        }
    }

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