package org.cycb.canvas.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.cycb.canvas.data.api.RetrofitClient
import org.cycb.canvas.data.model.LoginRequest
import org.cycb.canvas.data.model.User
import org.cycb.canvas.data.socket.SocketManager
import org.cycb.canvas.data.storage.StoredAccount
import org.cycb.canvas.data.storage.TokenManager
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel(context: Context) : ViewModel() {
    private val tokenManager = TokenManager(context)

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val accounts: StateFlow<List<StoredAccount>> = tokenManager.accounts
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        checkSavedAuth()
    }

    private fun checkSavedAuth() {
        viewModelScope.launch {
            try {
                val token = tokenManager.token.first()
                android.util.Log.d("AuthViewModel", "Checking saved auth, token: ${token?.take(20)}...")
                if (!token.isNullOrEmpty()) {
                    RetrofitClient.setToken(token)
                    try {
                        android.util.Log.d("AuthViewModel", "Fetching current user...")
                        val user = RetrofitClient.apiService.getCurrentUser()
                        android.util.Log.d("AuthViewModel", "User fetched: ${user.id}, ${user.username}")
                        _user.value = user
                        RetrofitClient.setCurrentUserId(user.getUserId())

                        SocketManager.getInstance().connect(token)
                        registerFCMToken()
                    } catch (e: Exception) {
                        android.util.Log.e("AuthViewModel", "Failed to fetch user: ${e.message}", e)
                        // If token is invalid, but we have multiple accounts, maybe we shouldn't clear all?
                        // For now, if active token fails, we clear it from that specific account
                        val activeUserId = tokenManager.activeUserId.first()
                        if (activeUserId != null) {
                            tokenManager.removeAccount(activeUserId)
                        }
                        RetrofitClient.setToken(null)
                    }
                }
                _isLoading.value = false
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "checkSavedAuth error: ${e.message}", e)
                _isLoading.value = false
            }
        }
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val response = RetrofitClient.apiService.login(
                    LoginRequest(username, password)
                )

                android.util.Log.d("AuthViewModel", "Login successful: ${response.user.getUserId()}, ${response.user.username}")
                
                val account = StoredAccount(
                    userId = response.user.getUserId(),
                    token = response.token,
                    username = response.user.username,
                    displayName = response.user.displayName,
                    profilePicture = response.user.profilePicture
                )
                tokenManager.saveAccount(account)
                
                RetrofitClient.setToken(response.token)
                RetrofitClient.setCurrentUserId(response.user.getUserId())
                _user.value = response.user
                
                SocketManager.getInstance().connect(response.token)
                registerFCMToken()
                _uiState.value = AuthUiState.Success
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "Login failed: ${e.message}", e)
                _uiState.value = AuthUiState.Error(
                    e.message ?: "Login failed. Please try again."
                )
            }
        }
    }

    fun switchAccount(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            tokenManager.switchAccount(userId)
            val token = tokenManager.token.first()
            if (token != null) {
                RetrofitClient.setToken(token)
                try {
                    val user = RetrofitClient.apiService.getCurrentUser()
                    _user.value = user
                    RetrofitClient.setCurrentUserId(user.getUserId())
                    SocketManager.getInstance().disconnect()
                    SocketManager.getInstance().connect(token)
                    registerFCMToken()
                } catch (e: Exception) {
                    android.util.Log.e("AuthViewModel", "Failed to switch to user $userId: ${e.message}")
                }
            }
            _isLoading.value = false
        }
    }

    fun removeAccount(userId: String) {
        viewModelScope.launch {
            tokenManager.removeAccount(userId)
            if (tokenManager.activeUserId.first() == null) {
                _user.value = null
                RetrofitClient.setToken(null)
                RetrofitClient.setCurrentUserId(null)
                SocketManager.getInstance().disconnect()
            } else {
                // If we removed the active account, switchAccount logic in tokenManager 
                // already updated the active one, we just need to refresh UI
                val newToken = tokenManager.token.first()
                RetrofitClient.setToken(newToken)
                if (newToken != null) {
                    try {
                        val user = RetrofitClient.apiService.getCurrentUser()
                        _user.value = user
                        RetrofitClient.setCurrentUserId(user.getUserId())
                        SocketManager.getInstance().disconnect()
                        SocketManager.getInstance().connect(newToken)
                    } catch (e: Exception) {
                        _user.value = null
                    }
                }
            }
        }
    }

    fun register(username: String, password: String, displayName: String, email: String? = null) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val response = RetrofitClient.apiService.register(
                    org.cycb.canvas.data.model.RegisterRequest(
                        username = username,
                        password = password,
                        displayName = displayName,
                        email = email
                    )
                )

                val account = StoredAccount(
                    userId = response.user.getUserId(),
                    token = response.token,
                    username = response.user.username,
                    displayName = response.user.displayName,
                    profilePicture = response.user.profilePicture
                )
                tokenManager.saveAccount(account)
                
                RetrofitClient.setToken(response.token)
                RetrofitClient.setCurrentUserId(response.user.getUserId())
                _user.value = response.user

                SocketManager.getInstance().connect(response.token)
                registerFCMToken()
                _uiState.value = AuthUiState.Success
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "Registration failed", e)
                val errorMessage = when {
                    e is retrofit2.HttpException -> {
                        try {
                            val errorBody = e.response()?.errorBody()?.string()
                            val jsonError = org.json.JSONObject(errorBody ?: "{}")
                            jsonError.optString("error", "Registration failed")
                        } catch (parseError: Exception) {
                            "Registration failed: ${e.code()}"
                        }
                    }
                    else -> e.message ?: "Registration failed. Please try again."
                }
                _uiState.value = AuthUiState.Error(errorMessage)
            }
        }
    }

    private fun registerFCMToken() {
        viewModelScope.launch {
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                RetrofitClient.apiService.registerFCMToken(mapOf("fcmToken" to token))
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "Error registering FCM token", e)
            }
        }
    }

    fun clearError() {
        _uiState.value = AuthUiState.Idle
    }

    fun refreshUser() {
        viewModelScope.launch {
            try {
                val user = RetrofitClient.apiService.getCurrentUser()
                _user.value = user
                
                // Update stored account info
                val currentToken = tokenManager.token.first()
                if (currentToken != null) {
                    tokenManager.saveAccount(StoredAccount(
                        userId = user.getUserId(),
                        token = currentToken,
                        username = user.username,
                        displayName = user.displayName,
                        profilePicture = user.profilePicture
                    ))
                }
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "Failed to refresh user: ${e.message}", e)
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            val activeId = tokenManager.activeUserId.first()
            if (activeId != null) {
                removeAccount(activeId)
            }
        }
    }
    
    fun logoutAll() {
        viewModelScope.launch {
            tokenManager.clearToken()
            RetrofitClient.setToken(null)
            RetrofitClient.setCurrentUserId(null)
            SocketManager.getInstance().disconnect()
            _user.value = null
        }
    }
}

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    object Success : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}
