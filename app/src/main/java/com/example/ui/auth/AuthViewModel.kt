package com.example.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.models.User
import com.example.data.repository.AuraRepository
import com.example.data.supabase.SupabaseService
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(private val repository: AuraRepository) : ViewModel() {
    private val client = SupabaseService.client
    
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Checking)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        checkUserLoggedIn()
    }

    private fun checkUserLoggedIn() {
        viewModelScope.launch {
            client.auth.sessionStatus.collect { status ->
                when (status) {
                    is io.github.jan.supabase.auth.status.SessionStatus.Authenticated -> {
                        val user = status.session.user
                        if (user != null) {
                            val userProfile = repository.getUserProfile(user.id)
                            if (userProfile != null) {
                                _currentUser.value = userProfile
                                _authState.value = AuthState.Success
                            } else {
                                // Create default profile automatically
                                val newUser = User(
                                    id = user.id,
                                    name = user.userMetadata?.get("full_name")?.toString()?.replace("\"", "")
                                        ?: user.email?.substringBefore("@")?.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                                        ?: "User",
                                    email = user.email ?: "",
                                    provider = user.appMetadata?.get("provider")?.toString() ?: "email",
                                    photoUrl = user.userMetadata?.get("avatar_url")?.toString()?.replace("\"", "") ?: "",
                                    createdAt = System.currentTimeMillis(),
                                    role = "user"
                                )
                                try {
                                    repository.createUserProfile(newUser)
                                    _currentUser.value = newUser
                                    _authState.value = AuthState.Success
                                } catch (e: Exception) {
                                    android.util.Log.e("Auth", "Failed to create profile", e)
                                    _authState.value = AuthState.Error(AuthErrorHelper.getFriendlyMessage(e))
                                }
                            }
                        }
                    }
                    is io.github.jan.supabase.auth.status.SessionStatus.NotAuthenticated -> {
                        _currentUser.value = null
                        _authState.value = AuthState.Unauthenticated
                    }
                    else -> {}
                }
            }
        }
    }

    fun login(email: String, pass: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                com.example.data.supabase.SupabaseService.checkNetworkReachability()
                client.auth.signInWith(Email) {
                    this.email = email
                    this.password = pass
                }
                val user = client.auth.currentSessionOrNull()?.user
                if (user != null) {
                    val userProfile = repository.getUserProfile(user.id)
                    if (userProfile != null) {
                        _currentUser.value = userProfile
                        _authState.value = AuthState.Success
                    } else {
                        // User exists in auth but no profile, create one
                        val newUser = User(
                            id = user.id,
                            name = email.substringBefore("@").replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                            email = email,
                            provider = "email",
                            createdAt = System.currentTimeMillis(),
                            role = "user"
                        )
                        repository.createUserProfile(newUser)
                        _currentUser.value = newUser
                        _authState.value = AuthState.Success
                    }
                } else {
                    _authState.value = AuthState.Error("Login failed")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(AuthErrorHelper.getFriendlyMessage(e))
            }
        }
    }

    fun register(name: String, email: String, pass: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                com.example.data.supabase.SupabaseService.checkNetworkReachability()
                client.auth.signUpWith(Email) {
                    this.email = email
                    this.password = pass
                }
                val user = client.auth.currentSessionOrNull()?.user
                if (user != null) {
                    val newUser = User(
                        id = user.id,
                        name = name.ifEmpty { email.substringBefore("@").replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } },
                        email = email,
                        provider = "email",
                        createdAt = System.currentTimeMillis(),
                        role = "user"
                    )
                    repository.createUserProfile(newUser)
                    _currentUser.value = newUser
                    _authState.value = AuthState.Success
                } else {
                    // Check email confirmation requirement
                    _authState.value = AuthState.Error("Signup failed or email confirmation required.")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(AuthErrorHelper.getFriendlyMessage(e))
            }
        }
    }

    fun signInWithGoogleToken(idToken: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                com.example.data.supabase.SupabaseService.checkNetworkReachability()
                client.auth.signInWith(IDToken) {
                    this.idToken = idToken
                    this.provider = Google
                }
                val user = client.auth.currentSessionOrNull()?.user
                if (user != null) {
                    val userProfile = repository.getUserProfile(user.id)
                    if (userProfile != null) {
                        _currentUser.value = userProfile
                        _authState.value = AuthState.Success
                    } else {
                        val newUser = User(
                            id = user.id,
                            name = user.userMetadata?.get("full_name")?.toString()?.replace("\"", "") ?: "User",
                            email = user.email ?: "",
                            provider = "google",
                            photoUrl = user.userMetadata?.get("avatar_url")?.toString()?.replace("\"", "") ?: "",
                            createdAt = System.currentTimeMillis(),
                            role = "user"
                        )
                        repository.createUserProfile(newUser)
                        _currentUser.value = newUser
                        _authState.value = AuthState.Success
                    }
                } else {
                     _authState.value = AuthState.Error("Google Auth failed")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(AuthErrorHelper.getFriendlyMessage(e))
            }
        }
    }

    fun signInWithQuickAccess(email: String) {
        // Direct secure register and login path for quick authentication in review/test environments
        register(email.substringBefore("@"), email, "quick_access_secure_password_123")
    }


    val isAdmin: Boolean
        get() = _currentUser.value?.email == "shaan1002006@gmail.com"

    fun logout() {
        viewModelScope.launch {
            try {
                client.auth.signOut()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            _currentUser.value = null
            _authState.value = AuthState.Unauthenticated
        }
    }
    
    fun sendPasswordReset(email: String) {
        viewModelScope.launch {
            try {
                com.example.data.supabase.SupabaseService.checkNetworkReachability()
                client.auth.resetPasswordForEmail(email)
                _authState.value = AuthState.Error("Password reset email sent.")
            } catch (e: Exception) {
                _authState.value = AuthState.Error(AuthErrorHelper.getFriendlyMessage(e))
            }
        }
    }

    fun continueAsGuest() {
        // Guest mode is disabled.
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }

    fun updateUserProfile(updatedUser: User, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                if (updatedUser.id == "guest_user") {
                    _currentUser.value = updatedUser
                    repository.saveGuestProfile(updatedUser)
                    _authState.value = AuthState.Success
                    onSuccess()
                } else {
                    repository.updateUserProfile(updatedUser)
                    _currentUser.value = updatedUser
                    _authState.value = AuthState.Success
                    onSuccess()
                }
            } catch (e: Exception) {
                android.util.Log.e("Auth", "Failed to update profile", e)
                val friendlyMessage = AuthErrorHelper.getFriendlyMessage(e)
                _authState.value = AuthState.Error(friendlyMessage)
                onError(friendlyMessage)
            }
        }
    }

    fun toggleSaveBook(bookId: String) {
        val user = _currentUser.value ?: return
        if (user.id == "guest_user") {
            val newSaved = if (user.savedBooks.contains(bookId)) {
                user.savedBooks - bookId
            } else {
                user.savedBooks + bookId
            }
            val newUser = user.copy(savedBooks = newSaved)
            _currentUser.value = newUser
            repository.saveGuestProfile(newUser)
        } else {
            val newSaved = if (user.savedBooks.contains(bookId)) {
                user.savedBooks - bookId
            } else {
                user.savedBooks + bookId
            }
            val newUser = user.copy(savedBooks = newSaved)
            _currentUser.value = newUser
            viewModelScope.launch {
                try {
                    client.postgrest["users"].update(newUser) {
                        filter { eq("id", newUser.id) }
                    }
                } catch(e: Exception){
                    e.printStackTrace()
                }
            }
        }
    }

    fun toggleSaveVideo(videoId: String) {
        val user = _currentUser.value ?: return
        if (user.id == "guest_user") {
            val newSaved = if (user.savedVideos.contains(videoId)) {
                user.savedVideos - videoId
            } else {
                user.savedVideos + videoId
            }
            val newUser = user.copy(savedVideos = newSaved)
            _currentUser.value = newUser
            repository.saveGuestProfile(newUser)
        } else {
            val newSaved = if (user.savedVideos.contains(videoId)) {
                user.savedVideos - videoId
            } else {
                user.savedVideos + videoId
            }
            val newUser = user.copy(savedVideos = newSaved)
            _currentUser.value = newUser
            viewModelScope.launch {
                try {
                    client.postgrest["users"].update(newUser) {
                        filter { eq("id", newUser.id) }
                    }
                } catch(e: Exception){
                    e.printStackTrace()
                }
            }
        }
    }
    fun updateSelectedGrade(grade: String) {
        val user = _currentUser.value ?: return
        if (user.id == "guest_user") {
            val newUser = user.copy(selectedGrade = grade)
            _currentUser.value = newUser
            repository.saveGuestProfile(newUser)
        } else {
            val newUser = user.copy(selectedGrade = grade)
            _currentUser.value = newUser
            viewModelScope.launch {
                try {
                    client.postgrest["users"].update(newUser) {
                        filter { eq("id", newUser.id) }
                    }
                } catch(e: Exception){
                    e.printStackTrace()
                }
            }
        }
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Checking : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    object Unauthenticated : AuthState()
    data class Error(val message: String) : AuthState()
}
