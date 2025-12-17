package com.example.delta3d.ui.session

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.delta3d.data.TokenStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SessionViewModel(app: Application) : AndroidViewModel(app) {

    private val tokenStore = TokenStore(app.applicationContext)

    private val _token = MutableStateFlow<String?>(null)
    val token: StateFlow<String?> = _token

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded

    init {
        // 冷启动：从 DataStore 读一次进内存
        viewModelScope.launch {
            _token.value = tokenStore.accessTokenFlow.first()
            _loaded.value = true
        }
    }

    fun login(token: String) {
        // 1) 内存立刻可用（多页面共享）
        _token.value = token
        // 2) 持久化（下次打开也能恢复）
        viewModelScope.launch { tokenStore.saveAccessToken(token) }
    }

    fun logout() {
        _token.value = null
        viewModelScope.launch { tokenStore.clear() }
    }
}
