package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import java.util.Locale
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import android.util.Log
import android.widget.Toast

class CalculatorViewModel(
    private val repository: CalculatorRepository,
    application: Application
) : AndroidViewModel(application) {

    private fun showToast(message: String) {
        viewModelScope.launch(Dispatchers.Main) {
            Toast.makeText(getApplication(), message, Toast.LENGTH_SHORT).show()
        }
    }

    fun retryPendingUserProfiles() {
        viewModelScope.launch {
            try {
                val pendingProfiles = repository.getUserProfilesBySyncStatus("pending")
                if (pendingProfiles.isNotEmpty()) {
                    Log.d("ViewModel", "Found ${pendingProfiles.size} pending user profiles to sync with Firestore.")
                    val firestore = FirebaseService.getFirestore()
                    if (firestore != null) {
                        for (profile in pendingProfiles) {
                            val userProfileMap = mapOf(
                                "uid" to profile.uid,
                                "email" to profile.email,
                                "role" to profile.role,
                                "trialStartDate" to profile.trialStartDate,
                                "trialEndDate" to profile.trialEndDate,
                                "subscriptionStatus" to profile.subscriptionStatus,
                                "accountType" to profile.accountType,
                                "createdAt" to profile.createdAt,
                                "isActive" to profile.isActive
                            )
                            firestore.collection("users")
                                .document(profile.uid)
                                .set(userProfileMap)
                                .addOnSuccessListener {
                                    Log.i("FIRESTORE_PROFILE_SAVE_SUCCESS", "Retried and saved pending profile for ${profile.uid} successfully on app launch.")
                                    viewModelScope.launch {
                                        try {
                                            val updatedProfile = profile.copy(syncStatus = "synced")
                                            repository.insertUserProfile(updatedProfile)
                                            Log.d("ROOM_PROFILE_CACHE_SUCCESS", "Updated local profile syncStatus to synced for ${profile.uid}.")
                                        } catch (e: Exception) {
                                            Log.e("ROOM_PROFILE_CACHE_FAILED", "Failed to update local profile syncStatus for ${profile.uid}.", e)
                                        }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Log.e("FIRESTORE_PROFILE_SAVE_FAILED", "Retry failed to save pending profile for ${profile.uid} on Firestore: ${e.message}")
                                }
                        }
                    } else {
                        Log.w("ViewModel", "Firestore is not available yet to retry sync.")
                    }
                }
            } catch (e: Exception) {
                Log.e("ViewModel", "Error while retrying pending user profiles", e)
            }
        }
    }

    fun syncLoginStatus(uid: String, email: String, onComplete: () -> Unit) {
        val firestore = FirebaseService.getFirestore()
        if (firestore == null) {
            Log.e("FIREBASE_LOGIN_STATUS_FAILED", "Firestore is null or not initialized")
            viewModelScope.launch {
                try {
                    val localLog = UserActivityLogEntity(
                        uid = uid,
                        email = email,
                        action = "LOGIN",
                        timestamp = System.currentTimeMillis(),
                        syncStatus = "pending_login"
                    )
                    repository.insertUserActivityLog(localLog)
                    Log.d("ROOM_PROFILE_CACHE_SUCCESS", "UserActivityLogEntity cached locally as pending_login due to null firestore.")
                } catch (e: Exception) {
                    Log.e("ROOM_PROFILE_CACHE_FAILED", "Failed to cache UserActivityLogEntity in Room: ${e.message}")
                }
                onComplete()
            }
            return
        }

        val userUpdates = mapOf(
            "uid" to uid,
            "email" to email,
            "isOnline" to true,
            "loginStatus" to "logged_in",
            "lastLoginAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )

        val activityLog = mapOf(
            "uid" to uid,
            "email" to email,
            "action" to "LOGIN",
            "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )

        firestore.collection("users")
            .document(uid)
            .set(userUpdates, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                Log.i("FIREBASE_LOGIN_STATUS_SUCCESS", "Successfully updated login status for UID: $uid")
                
                firestore.collection("userActivityLogs")
                    .add(activityLog)
                    .addOnSuccessListener {
                        Log.i("FIREBASE_LOGIN_ACTIVITY_SUCCESS", "Successfully added login activity log for UID: $uid")
                        
                        // Show toast only after Firestore success
                        showToast("Firebase Login: Status saved in database.")
                        
                        viewModelScope.launch {
                            try {
                                val localLog = UserActivityLogEntity(
                                    uid = uid,
                                    email = email,
                                    action = "LOGIN",
                                    timestamp = System.currentTimeMillis(),
                                    syncStatus = "synced"
                                )
                                repository.insertUserActivityLog(localLog)
                                Log.d("ROOM_PROFILE_CACHE_SUCCESS", "UserActivityLogEntity cached locally as synced.")
                            } catch (e: Exception) {
                                Log.e("ROOM_PROFILE_CACHE_FAILED", "Failed to cache UserActivityLogEntity in Room: ${e.message}")
                            }
                            onComplete()
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("FIREBASE_LOGIN_ACTIVITY_FAILED", "Failed to add login activity log: ${e.message}")
                        
                        viewModelScope.launch {
                            try {
                                val localLog = UserActivityLogEntity(
                                    uid = uid,
                                    email = email,
                                    action = "LOGIN",
                                    timestamp = System.currentTimeMillis(),
                                    syncStatus = "pending_activity"
                                )
                                repository.insertUserActivityLog(localLog)
                                Log.d("ROOM_PROFILE_CACHE_SUCCESS", "UserActivityLogEntity cached locally as pending_activity.")
                            } catch (e2: Exception) {
                                Log.e("ROOM_PROFILE_CACHE_FAILED", "Failed to cache UserActivityLogEntity in Room: ${e2.message}")
                            }
                            onComplete()
                        }
                    }
            }
            .addOnFailureListener { e ->
                Log.e("FIREBASE_LOGIN_STATUS_FAILED", "Failed to update login status: ${e.message}")
                
                viewModelScope.launch {
                    try {
                        val localLog = UserActivityLogEntity(
                            uid = uid,
                            email = email,
                            action = "LOGIN",
                            timestamp = System.currentTimeMillis(),
                            syncStatus = "pending_login"
                        )
                        repository.insertUserActivityLog(localLog)
                        Log.d("ROOM_PROFILE_CACHE_SUCCESS", "UserActivityLogEntity cached locally as pending_login.")
                    } catch (e2: Exception) {
                        Log.e("ROOM_PROFILE_CACHE_FAILED", "Failed to cache UserActivityLogEntity in Room: ${e2.message}")
                    }
                    onComplete()
                }
            }
    }

    fun syncLogoutStatus(uid: String, email: String, onComplete: () -> Unit) {
        val firestore = FirebaseService.getFirestore()
        
        val performSignOut = {
            try {
                FirebaseService.getAuth()?.signOut()
            } catch (authEx: Exception) {
                Log.e("ViewModel", "Auth signOut error", authEx)
            }
            onComplete()
        }

        if (firestore == null) {
            Log.e("FIREBASE_LOGOUT_STATUS_FAILED", "Firestore is null or not initialized")
            viewModelScope.launch {
                try {
                    val localLog = UserActivityLogEntity(
                        uid = uid,
                        email = email,
                        action = "LOGOUT",
                        timestamp = System.currentTimeMillis(),
                        syncStatus = "pending_logout"
                    )
                    repository.insertUserActivityLog(localLog)
                    Log.d("ROOM_PROFILE_CACHE_SUCCESS", "UserActivityLogEntity cached locally as pending_logout.")
                } catch (e: Exception) {
                    Log.e("ROOM_PROFILE_CACHE_FAILED", "Failed to cache UserActivityLogEntity in Room: ${e.message}")
                }
                performSignOut()
            }
            return
        }

        val userUpdates = mapOf(
            "isOnline" to false,
            "loginStatus" to "logged_out",
            "lastLogoutAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )

        val activityLog = mapOf(
            "uid" to uid,
            "email" to email,
            "action" to "LOGOUT",
            "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )

        firestore.collection("users")
            .document(uid)
            .set(userUpdates, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                Log.i("FIREBASE_LOGOUT_STATUS_SUCCESS", "Successfully updated logout status on Firestore for UID: $uid")
                
                firestore.collection("userActivityLogs")
                    .add(activityLog)
                    .addOnSuccessListener {
                        Log.i("FIREBASE_LOGOUT_ACTIVITY_SUCCESS", "Successfully added logout activity log for UID: $uid")
                        
                        viewModelScope.launch {
                            try {
                                val localLog = UserActivityLogEntity(
                                    uid = uid,
                                    email = email,
                                    action = "LOGOUT",
                                    timestamp = System.currentTimeMillis(),
                                    syncStatus = "synced"
                                )
                                repository.insertUserActivityLog(localLog)
                                Log.d("ROOM_PROFILE_CACHE_SUCCESS", "UserActivityLogEntity logout cached locally as synced.")
                            } catch (e: Exception) {
                                Log.e("ROOM_PROFILE_CACHE_FAILED", "Failed to cache UserActivityLogEntity in Room: ${e.message}")
                            }
                            performSignOut()
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("FIREBASE_LOGOUT_ACTIVITY_FAILED", "Failed to add logout activity log: ${e.message}")
                        
                        viewModelScope.launch {
                            try {
                                val localLog = UserActivityLogEntity(
                                    uid = uid,
                                    email = email,
                                    action = "LOGOUT",
                                    timestamp = System.currentTimeMillis(),
                                    syncStatus = "pending_activity"
                                )
                                repository.insertUserActivityLog(localLog)
                                Log.d("ROOM_PROFILE_CACHE_SUCCESS", "UserActivityLogEntity cached locally as pending_activity.")
                            } catch (e2: Exception) {
                                Log.e("ROOM_PROFILE_CACHE_FAILED", "Failed to cache UserActivityLogEntity in Room: ${e2.message}")
                            }
                            performSignOut()
                        }
                    }
            }
            .addOnFailureListener { e ->
                Log.e("FIREBASE_LOGOUT_STATUS_FAILED", "Failed to update logout status to Firestore: ${e.message}")
                
                viewModelScope.launch {
                    try {
                        val localLog = UserActivityLogEntity(
                            uid = uid,
                            email = email,
                            action = "LOGOUT",
                            timestamp = System.currentTimeMillis(),
                            syncStatus = "pending_logout"
                        )
                        repository.insertUserActivityLog(localLog)
                        Log.d("ROOM_PROFILE_CACHE_SUCCESS", "UserActivityLogEntity cached locally as pending_logout.")
                    } catch (e2: Exception) {
                        Log.e("ROOM_PROFILE_CACHE_FAILED", "Failed to cache UserActivityLogEntity in Room: ${e2.message}")
                    }
                    performSignOut()
                }
            }
    }

    fun retryPendingUserActivityLogs() {
        viewModelScope.launch {
            try {
                val pendingLogs = repository.getPendingUserActivityLogs()
                if (pendingLogs.isNotEmpty()) {
                    Log.d("ViewModel", "Found ${pendingLogs.size} pending activity logs to sync.")
                    val firestore = FirebaseService.getFirestore()
                    if (firestore != null) {
                        for (log in pendingLogs) {
                            when (log.syncStatus) {
                                "pending_login" -> {
                                    val userUpdates = mapOf(
                                        "uid" to log.uid,
                                        "email" to log.email,
                                        "isOnline" to true,
                                        "loginStatus" to "logged_in",
                                        "lastLoginAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                                        "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                                    )
                                    val activityLog = mapOf(
                                        "uid" to log.uid,
                                        "email" to log.email,
                                        "action" to "LOGIN",
                                        "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                                    )
                                    firestore.collection("users")
                                        .document(log.uid)
                                        .set(userUpdates, com.google.firebase.firestore.SetOptions.merge())
                                        .addOnSuccessListener {
                                            Log.i("FIREBASE_LOGIN_STATUS_SUCCESS", "Retried and saved login status for UID: ${log.uid} on launch.")
                                            firestore.collection("userActivityLogs")
                                                .add(activityLog)
                                                .addOnSuccessListener {
                                                    Log.i("FIREBASE_LOGIN_ACTIVITY_SUCCESS", "Retried and saved login activity log for UID: ${log.uid} on launch.")
                                                    viewModelScope.launch {
                                                        try {
                                                            val updatedLog = log.copy(syncStatus = "synced")
                                                            repository.insertUserActivityLog(updatedLog)
                                                            Log.d("ROOM_PROFILE_CACHE_SUCCESS", "Marked Room local activity log #${log.id} as synced on retry.")
                                                        } catch (e: Exception) {
                                                            Log.e("ROOM_PROFILE_CACHE_FAILED", "Failed to update Room local activity log sync status.", e)
                                                        }
                                                    }
                                                }
                                                .addOnFailureListener { e ->
                                                    Log.e("FIREBASE_LOGIN_ACTIVITY_FAILED", "Launch retry failed for activity log: ${e.message}")
                                                    viewModelScope.launch {
                                                        try {
                                                            val updatedLog = log.copy(syncStatus = "pending_activity")
                                                            repository.insertUserActivityLog(updatedLog)
                                                        } catch (ex: Exception) {
                                                            Log.e("ROOM_PROFILE_CACHE_FAILED", "Failed to cache on retry demotion", ex)
                                                        }
                                                    }
                                                }
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("FIREBASE_LOGIN_STATUS_FAILED", "Launch retry failed for login status: ${e.message}")
                                        }
                                }
                                "pending_logout" -> {
                                    val userUpdates = mapOf(
                                        "isOnline" to false,
                                        "loginStatus" to "logged_out",
                                        "lastLogoutAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                                        "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                                    )
                                    val activityLog = mapOf(
                                        "uid" to log.uid,
                                        "email" to log.email,
                                        "action" to "LOGOUT",
                                        "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                                    )
                                    firestore.collection("users")
                                        .document(log.uid)
                                        .set(userUpdates, com.google.firebase.firestore.SetOptions.merge())
                                        .addOnSuccessListener {
                                            Log.i("FIREBASE_LOGOUT_STATUS_SUCCESS", "Retried and saved logout status for UID: ${log.uid} on launch.")
                                            firestore.collection("userActivityLogs")
                                                .add(activityLog)
                                                .addOnSuccessListener {
                                                    Log.i("FIREBASE_LOGOUT_ACTIVITY_SUCCESS", "Retried and saved logout activity log for UID: ${log.uid} on launch.")
                                                    viewModelScope.launch {
                                                        try {
                                                            val updatedLog = log.copy(syncStatus = "synced")
                                                            repository.insertUserActivityLog(updatedLog)
                                                            Log.d("ROOM_PROFILE_CACHE_SUCCESS", "Marked Room local activity log #${log.id} as synced on retry.")
                                                        } catch (e: Exception) {
                                                            Log.e("ROOM_PROFILE_CACHE_FAILED", "Failed to update Room local activity log sync status.", e)
                                                        }
                                                    }
                                                }
                                                .addOnFailureListener { e ->
                                                    Log.e("FIREBASE_LOGOUT_ACTIVITY_FAILED", "Launch retry failed for activity log: ${e.message}")
                                                    viewModelScope.launch {
                                                        try {
                                                            val updatedLog = log.copy(syncStatus = "pending_activity")
                                                            repository.insertUserActivityLog(updatedLog)
                                                        } catch (ex: Exception) {
                                                            Log.e("ROOM_PROFILE_CACHE_FAILED", "Failed to cache on retry demotion", ex)
                                                        }
                                                    }
                                                }
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("FIREBASE_LOGOUT_STATUS_FAILED", "Launch retry failed for logout status: ${e.message}")
                                        }
                                }
                                "pending_activity" -> {
                                    val activityLog = mapOf(
                                        "uid" to log.uid,
                                        "email" to log.email,
                                        "action" to log.action,
                                        "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                                    )
                                    firestore.collection("userActivityLogs")
                                        .add(activityLog)
                                        .addOnSuccessListener {
                                            Log.i("FIREBASE_LOGIN_ACTIVITY_SUCCESS", "Retried and completed pending activity log successfully for Action: ${log.action}")
                                            viewModelScope.launch {
                                                try {
                                                    val updatedLog = log.copy(syncStatus = "synced")
                                                    repository.insertUserActivityLog(updatedLog)
                                                    Log.d("ROOM_PROFILE_CACHE_SUCCESS", "Marked Room local activity log #${log.id} as synced.")
                                                } catch (e: Exception) {
                                                    Log.e("ROOM_PROFILE_CACHE_FAILED", "Failed to update Room local activity log status.", e)
                                                }
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("FIREBASE_LOGIN_ACTIVITY_FAILED", "Launch retry failed again for individual pending activity log: ${e.message}")
                                        }
                                }
                            }
                        }
                    } else {
                        Log.w("ViewModel", "Firestore is not available yet to retry activity logs sync.")
                    }
                }
            } catch (e: Exception) {
                Log.e("ViewModel", "Error while retrying pending user activity logs on launch", e)
            }
        }
    }

    // Initialize database on first launch
    init {
        viewModelScope.launch {
            repository.checkAndPrepopulate()
            loadFirebaseConfigFromSharedPrefs()
            loadFlowPrefs()
            loadAdminPassword()
            loadFavoritePrefs()
            loadUserSession()
            loadAdminSession()
            retryPendingUserProfiles()
            retryPendingUserActivityLogs()
        }
    }

    // List of all slides from database
    val allSlides: StateFlow<List<SlideWithVariables>> = repository.allSlides
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    sealed class FormulaOperationStatus {
        object Idle : FormulaOperationStatus()
        object Success : FormulaOperationStatus()
        data class Error(val message: String) : FormulaOperationStatus()
    }

    private val _formulaSaveStatus = MutableStateFlow<FormulaOperationStatus>(FormulaOperationStatus.Idle)
    val formulaSaveStatus: StateFlow<FormulaOperationStatus> = _formulaSaveStatus.asStateFlow()

    fun resetFormulaSaveStatus() {
        _formulaSaveStatus.value = FormulaOperationStatus.Idle
    }

    // Current active slide (1-indexed based ID, mapped to list indices)
    private val _activeSlideId = MutableStateFlow(1)
    val activeSlideId: StateFlow<Int> = _activeSlideId.asStateFlow()

    // Real-time search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Selected category filter
    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // Real-time favorites list
    private val _favoriteSlideIds = MutableStateFlow<Set<Int>>(emptySet())
    val favoriteSlideIds: StateFlow<Set<Int>> = _favoriteSlideIds.asStateFlow()

    fun toggleFavorite(slideId: Int) {
        val current = _favoriteSlideIds.value
        val updated = if (current.contains(slideId)) current - slideId else current + slideId
        _favoriteSlideIds.value = updated
        saveFavoritePrefs(updated)
    }

    private fun saveFavoritePrefs(favorites: Set<Int>) {
        val sp = getApplication<Application>().getSharedPreferences("favorite_prefs", Context.MODE_PRIVATE)
        sp.edit().putStringSet("fav_ids", favorites.map { it.toString() }.toSet()).apply()
    }

    fun loadFavoritePrefs() {
        val sp = getApplication<Application>().getSharedPreferences("favorite_prefs", Context.MODE_PRIVATE)
        val set = sp.getStringSet("fav_ids", emptySet()) ?: emptySet()
        _favoriteSlideIds.value = set.mapNotNull { it.toIntOrNull() }.toSet()
    }

    // Local Calculation History logging
    data class LocalCalculationLog(
        val slideId: Int,
        val timestamp: String,
        val inputs: String,
        val result: String
    )

    private val _localHistory = MutableStateFlow<List<LocalCalculationLog>>(emptyList())
    val localHistory: StateFlow<List<LocalCalculationLog>> = _localHistory.asStateFlow()

    fun addToLocalHistory(slideId: Int, inputs: String, result: String) {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        val now = sdf.format(java.util.Date())
        val log = LocalCalculationLog(slideId, now, inputs, result)
        _localHistory.value = listOf(log) + _localHistory.value
    }

    fun deleteLocalHistoryLog(log: LocalCalculationLog) {
        _localHistory.value = _localHistory.value.filter { it != log }
    }

    fun clearUserHistory() {
        _localHistory.value = emptyList()
    }

    fun clearAllHistory() {
        _localHistory.value = emptyList()
    }

    // Filtered slides list by category and search keyword
    val filteredSlides: StateFlow<List<SlideWithVariables>> = combine(
        allSlides,
        _searchQuery,
        _selectedCategory,
        _favoriteSlideIds
    ) { slides, query, category, favorites ->
        var resultList = slides

        // Category Filter
        if (category == "Favorites") {
            resultList = resultList.filter {
                favorites.contains(it.slide.id)
            }
        } else if (category != "All") {
            resultList = resultList.filter {
                it.slide.category.equals(category, ignoreCase = true)
            }
        }

        // Text Search Filter
        if (query.isNotBlank()) {
            val lowerQuery = query.lowercase(Locale.getDefault())
            resultList = resultList.filter {
                it.slide.id.toString().contains(lowerQuery) ||
                "page ${it.slide.id}".contains(lowerQuery) ||
                it.slide.title.lowercase(Locale.getDefault()).contains(lowerQuery) ||
                it.slide.description.lowercase(Locale.getDefault()).contains(lowerQuery) ||
                it.slide.formula.lowercase(Locale.getDefault()).contains(lowerQuery) ||
                it.slide.category.lowercase(Locale.getDefault()).contains(lowerQuery)
            }
        }

        resultList
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Active slide detail stream
    val activeSlide: StateFlow<SlideWithVariables?> = combine(
        allSlides,
        _activeSlideId
    ) { slides, activeId ->
        slides.find { it.slide.id == activeId }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // Admin mode status
    private val _isAdminMode = MutableStateFlow(false)
    val isAdminMode: StateFlow<Boolean> = _isAdminMode.asStateFlow()

    // Session State Flows
    private val _isUserLoggedIn = MutableStateFlow(false)
    val isUserLoggedIn: StateFlow<Boolean> = _isUserLoggedIn.asStateFlow()

    private val _isAdminLoggedIn = MutableStateFlow(false)
    val isAdminLoggedIn: StateFlow<Boolean> = _isAdminLoggedIn.asStateFlow()

    private val _loggedInUserId = MutableStateFlow<String?>(null)
    val loggedInUserId: StateFlow<String?> = _loggedInUserId.asStateFlow()

    private val _loggedInName = MutableStateFlow<String?>(null)
    val loggedInName: StateFlow<String?> = _loggedInName.asStateFlow()

    private val _loggedInEmail = MutableStateFlow<String?>(null)
    val loggedInEmail: StateFlow<String?> = _loggedInEmail.asStateFlow()

    private val _isAuthenticating = MutableStateFlow(false)
    val isAuthenticating: StateFlow<Boolean> = _isAuthenticating.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    // Securely stored Admin Password (Defaults to 4735)
    private val _adminPassword = MutableStateFlow("4735")
    val adminPassword: StateFlow<String> = _adminPassword.asStateFlow()

    // Light / Dark screen visual override
    private val _isDarkMode = MutableStateFlow(true) // Start in crisp modern dark mode
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _followSystemTheme = MutableStateFlow(true) // Default to follow system theme automatically
    val followSystemTheme: StateFlow<Boolean> = _followSystemTheme.asStateFlow()

    // --- NEW: Decimal Format Precision Settings Option (Defaults to 2) ---
    private val _decimalPlaces = MutableStateFlow(2)
    val decimalPlaces: StateFlow<Int> = _decimalPlaces.asStateFlow()

    // --- Universal Flow Unit Conversion System ---
    private val _defaultFlowUnit = MutableStateFlow("LPH")
    val defaultFlowUnit: StateFlow<String> = _defaultFlowUnit.asStateFlow()

    fun updateDefaultFlowUnit(unit: String) {
        _defaultFlowUnit.value = unit
        saveFlowPrefs()
    }

    // --- User Defined Custom Units System ---
    private val _userDefinedUnits = MutableStateFlow<List<String>>(emptyList())
    val userDefinedUnits: StateFlow<List<String>> = _userDefinedUnits.asStateFlow()

    val allAvailableUnits: StateFlow<List<String>> = combine(
        _userDefinedUnits,
        allSlides
    ) { customUnits, slides ->
        val standardVolumetric = listOf("LPH", "LPM", "m³/hr", "GPH")
        val unitsInDb = slides.flatMap { item ->
            listOf(item.slide.resultUnit) + item.variables.map { it.unit }
        }.filter { it.isNotBlank() && it != "_" }
        (standardVolumetric + customUnits + unitsInDb).distinct().sorted()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = listOf("LPH", "LPM", "m³/hr", "GPH", "kg", "g/t", "mL", "kg/H", "MT/H", "%", "ratio", "g/L")
    )

    // --- Additional Settings as Requested ---
    private val _autoCalculation = MutableStateFlow(true)
    val autoCalculation: StateFlow<Boolean> = _autoCalculation.asStateFlow()

    private val _fontScale = MutableStateFlow(1.0f)
    val fontScale: StateFlow<Float> = _fontScale.asStateFlow()

    private val _openaiApiKey = MutableStateFlow("")
    val openaiApiKey: StateFlow<String> = _openaiApiKey.asStateFlow()

    private val _enableAiCalculations = MutableStateFlow(false)
    val enableAiCalculations: StateFlow<Boolean> = _enableAiCalculations.asStateFlow()

    private val _aiSuggestions = MutableStateFlow(true)
    val aiSuggestions: StateFlow<Boolean> = _aiSuggestions.asStateFlow()

    fun setAutoCalculation(enabled: Boolean) {
        _autoCalculation.value = enabled
        saveFlowPrefs()
    }

    fun setFontScale(scale: Float) {
        _fontScale.value = scale
        saveFlowPrefs()
    }

    fun setOpenaiApiKey(key: String) {
        _openaiApiKey.value = key
        saveFlowPrefs()
    }

    fun setEnableAiCalculations(enabled: Boolean) {
        _enableAiCalculations.value = enabled
        saveFlowPrefs()
    }

    fun setAiSuggestions(enabled: Boolean) {
        _aiSuggestions.value = enabled
        saveFlowPrefs()
    }

    fun addUserDefinedUnit(unit: String) {
        val trimmed = unit.trim()
        if (trimmed.isNotBlank() && trimmed !in _userDefinedUnits.value) {
            val newList = _userDefinedUnits.value + trimmed
            _userDefinedUnits.value = newList
            val sp = getApplication<Application>().getSharedPreferences("flow_prefs", Context.MODE_PRIVATE)
            sp.edit().putString("user_defined_units", newList.joinToString(",")).apply()
        }
    }

    fun removeUserDefinedUnit(unit: String) {
        val newList = _userDefinedUnits.value - unit
        _userDefinedUnits.value = newList
        val sp = getApplication<Application>().getSharedPreferences("flow_prefs", Context.MODE_PRIVATE)
        sp.edit().putString("user_defined_units", newList.joinToString(",")).apply()
    }

    fun editUnit(oldUnit: String, newUnit: String) {
        val trimmedOld = oldUnit.trim()
        val trimmedNew = newUnit.trim()
        if (trimmedOld.isBlank() || trimmedNew.isBlank() || trimmedOld == trimmedNew) return
        
        viewModelScope.launch {
            createAutoBackup()
            
            // 1) Update in userDefinedUnits list if present
            if (trimmedOld in _userDefinedUnits.value) {
                val updatedList = _userDefinedUnits.value.map { if (it == trimmedOld) trimmedNew else it }.distinct()
                _userDefinedUnits.value = updatedList
                val sp = getApplication<Application>().getSharedPreferences("flow_prefs", Context.MODE_PRIVATE)
                sp.edit().putString("user_defined_units", updatedList.joinToString(",")).apply()
            }
            
            // 2) Update all slides and variable records in SQLite
            val list = allSlides.value
            list.forEach { item ->
                var slideDirty = false
                var updatedSlide = item.slide
                if (item.slide.resultUnit == trimmedOld) {
                    updatedSlide = item.slide.copy(resultUnit = trimmedNew)
                    slideDirty = true
                }
                
                val updatedVars = item.variables.map { variable ->
                    if (variable.unit == trimmedOld) {
                        variable.copy(unit = trimmedNew)
                    } else {
                        variable
                    }
                }
                
                if (slideDirty) {
                    repository.updateSlide(updatedSlide)
                }
                updatedVars.forEach { v ->
                    if (v.unit == trimmedNew) {
                        repository.updateVariable(v)
                    }
                }
            }
            logAction("EDIT_UNIT", "Renamed unit '$trimmedOld' to '$trimmedNew' dynamically.")
        }
    }

    fun deleteUnit(unit: String) {
        val trimmed = unit.trim()
        if (trimmed.isBlank()) return
        
        viewModelScope.launch {
            createAutoBackup()
            
            // 1) Remove from userDefinedUnits if present
            if (trimmed in _userDefinedUnits.value) {
                val newList = _userDefinedUnits.value - trimmed
                _userDefinedUnits.value = newList
                val sp = getApplication<Application>().getSharedPreferences("flow_prefs", Context.MODE_PRIVATE)
                sp.edit().putString("user_defined_units", newList.joinToString(",")).apply()
            }
            
            // 2) Clear from slides & variables
            val list = allSlides.value
            list.forEach { item ->
                var slideDirty = false
                var updatedSlide = item.slide
                if (item.slide.resultUnit == trimmed) {
                    updatedSlide = item.slide.copy(resultUnit = "unit")
                    slideDirty = true
                }
                
                val updatedVars = item.variables.map { variable ->
                    if (variable.unit == trimmed) {
                        variable.copy(unit = "unit") // fallback
                    } else {
                        variable
                    }
                }
                
                if (slideDirty) {
                    repository.updateSlide(updatedSlide)
                }
                updatedVars.forEach { v ->
                    if (v.unit == "unit") {
                        repository.updateVariable(v)
                    }
                }
            }
            logAction("DELETE_UNIT", "Deleted unit '$trimmed' from Registry & Templates.")
        }
    }

    fun loadFlowPrefs() {
        val sp = getApplication<Application>().getSharedPreferences("flow_prefs", Context.MODE_PRIVATE)
        _defaultFlowUnit.value = sp.getString("def_flow_unit", "LPH") ?: "LPH"
        val savedDec = sp.getInt("decimal_places", -1)
        if (savedDec != -1) {
            _decimalPlaces.value = savedDec
        }
        val customUnitsString = sp.getString("user_defined_units", "") ?: ""
        if (customUnitsString.isNotBlank()) {
            _userDefinedUnits.value = customUnitsString.split(",").filter { it.isNotBlank() }
        }
        _autoCalculation.value = sp.getBoolean("auto_calculation", true)
        _fontScale.value = sp.getFloat("font_scale", 1.0f)
        _openaiApiKey.value = sp.getString("openai_api_key", "") ?: ""
        _enableAiCalculations.value = sp.getBoolean("enable_ai_calculations", false)
        _aiSuggestions.value = sp.getBoolean("ai_suggestions", true)
        _followSystemTheme.value = sp.getBoolean("follow_system_theme", true)
        _isDarkMode.value = sp.getBoolean("is_dark_mode", true)
    }

    fun saveFlowPrefs() {
        val sp = getApplication<Application>().getSharedPreferences("flow_prefs", Context.MODE_PRIVATE)
        sp.edit()
            .putString("def_flow_unit", _defaultFlowUnit.value)
            .putInt("decimal_places", _decimalPlaces.value)
            .putBoolean("auto_calculation", _autoCalculation.value)
            .putFloat("font_scale", _fontScale.value)
            .putString("openai_api_key", _openaiApiKey.value)
            .putBoolean("enable_ai_calculations", _enableAiCalculations.value)
            .putBoolean("ai_suggestions", _aiSuggestions.value)
            .putBoolean("follow_system_theme", _followSystemTheme.value)
            .putBoolean("is_dark_mode", _isDarkMode.value)
            .apply()
    }

    fun loadAdminPassword() {
        val sp = getApplication<Application>().getSharedPreferences("admin_prefs", Context.MODE_PRIVATE)
        val encrypted = sp.getString("admin_pwd_sec", null)
        if (encrypted != null) {
            val decrypted = CryptoHelper.decrypt(encrypted)
            _adminPassword.value = if (decrypted.isNotEmpty() && decrypted != encrypted) decrypted else "4735"
        } else {
            _adminPassword.value = "4735"
        }
    }

    fun changeAdminPassword(newPassword: String) {
        _adminPassword.value = newPassword
        val sp = getApplication<Application>().getSharedPreferences("admin_prefs", Context.MODE_PRIVATE)
        val encrypted = CryptoHelper.encrypt(newPassword)
        sp.edit().putString("admin_pwd_sec", encrypted).apply()
        logAction("CHANGE_ADMIN_PASSWORD", "Successfully updated the secured admin access credentials.")
    }

    // --- NEW: User Login, Session Roles, and Activity Logs ---
    private val _userMobile = MutableStateFlow("")
    val userMobile: StateFlow<String> = _userMobile.asStateFlow()

    private val _userRole = MutableStateFlow("") // "OPERATOR_VIEW" or "SUPERVISOR_ADMIN"
    val userRole: StateFlow<String> = _userRole.asStateFlow()

    private val _generatedOtp = MutableStateFlow("")
    val generatedOtp: StateFlow<String> = _generatedOtp.asStateFlow()

    // --- CLOUD READY WEB SYNCHRONIZATION ENGINE INTEGRATION ---
    private val networkService by lazy { NetworkService.getInstance(application) }

    private val _cloudConfig = MutableStateFlow(CloudDatabaseConfig())
    val cloudConfig: StateFlow<CloudDatabaseConfig> = _cloudConfig.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncReport = MutableStateFlow<SyncReport?>(null)
    val lastSyncReport: StateFlow<SyncReport?> = _lastSyncReport.asStateFlow()

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    fun updateCloudConfig(config: CloudDatabaseConfig) {
        networkService.updateConfig(config)
        _cloudConfig.value = config
    }

    fun refreshNetworkStatus() {
        _isOnline.value = networkService.isNetworkAvailable()
    }

    fun syncNow(onFinished: (SyncReport) -> Unit = {}) {
        viewModelScope.launch {
            _isSyncing.value = true
            refreshNetworkStatus()

            try {
                // Read all local slides with variables from database
                val localSlidesList = allSlides.value

                // 1. Upload formulas from client local SQLite db to website via NetworkService API representation
                val uploadReport = networkService.uploadFormulas(localSlidesList)

                var downloadMessage = ""
                // 2. Download any formulas added remotely on web dashboard to locally cache
                val remoteFormulas = networkService.downloadFormulas()
                if (remoteFormulas.isNotEmpty()) {
                    for (nf in remoteFormulas) {
                        val slide = Slide(
                            id = nf.id,
                            title = nf.title,
                            category = nf.category,
                            formula = nf.formula,
                            resultUnit = nf.resultUnit,
                            description = nf.description
                        )
                        val vars = nf.variables.map { nv ->
                            Variable(
                                id = 0,
                                slideId = nf.id,
                                symbol = nv.symbol,
                                name = nv.name,
                                value = nv.value,
                                unit = nv.unit
                            )
                        }
                        // Update Room database offline cache directly
                        repository.updateSlideAndVariables(slide, vars)
                    }
                    downloadMessage = " (Pull-synced ${remoteFormulas.size} formulas from dashboard, including optimized C-Starch coefficients)."
                }

                val finalReport = SyncReport(
                    success = uploadReport.success,
                    uploadedCount = uploadReport.uploadedCount,
                    downloadedCount = remoteFormulas.size,
                    message = uploadReport.message + downloadMessage
                )

                _lastSyncReport.value = finalReport
                onFinished(finalReport)
            } catch (e: Exception) {
                val errorReport = SyncReport(
                    success = false,
                    uploadedCount = 0,
                    downloadedCount = 0,
                    message = "Synchronization error: ${e.localizedMessage ?: "Connection Timeout"}"
                )
                _lastSyncReport.value = errorReport
                onFinished(errorReport)
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun loginAsRole(role: String) {
        if (role == "Admin") {
            _isAdminMode.value = true
        } else {
            _isAdminMode.value = false
        }
    }

    val currentUserRole: StateFlow<String> = _isAdminMode.map { if (it) "Admin" else "Operator" }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "Operator"
        )

    fun logout() {
        _isAdminMode.value = false
    }

    fun logAction(action: String, details: String) {
        android.util.Log.d("ChemDoseLog", "[$action] $details")
    }


    // --- NEW: Unlimited Slide Adding ---
    fun addNewCustomSlide(
        title: String,
        category: String,
        formula: String,
        resultUnit: String,
        description: String,
        variables: List<Variable>
    ) {
        viewModelScope.launch {
            createAutoBackup()
            val slides = allSlides.value
            val nextId = (slides.maxOfOrNull { it.slide.id } ?: 0) + 1
            
            val newSlide = Slide(
                id = nextId,
                title = title,
                formula = formula,
                resultUnit = resultUnit,
                description = description,
                category = category
            )
            val newVars = variables.map { it.copy(id = 0L, slideId = nextId) }
            
            repository.updateSlideAndVariables(newSlide, newVars)
            saveNewFormulaToFirestoreDirect(newSlide, newVars)
            _activeSlideId.value = nextId // Swaps selection directly to the newly added slide
            logAction("ADD_SLIDE", "Added brand new slide #$nextId: $title ($formula)")
        }
    }

    fun deleteSlide(slideId: Int) {
        viewModelScope.launch {
            createAutoBackup()
            repository.deleteSlide(slideId)
            deleteFormulaFromFirestoreDirect(slideId)
            logAction("DELETE_SLIDE", "Deleted slide/formula #$slideId from the database.")
            // If the deleted slide was active, switch to another available slide
            if (_activeSlideId.value == slideId) {
                val remaining = allSlides.value.filter { it.slide.id != slideId }
                if (remaining.isNotEmpty()) {
                    _activeSlideId.value = remaining.first().slide.id
                } else {
                    _activeSlideId.value = 1 // default fallback
                }
            }
        }
    }

    fun duplicateSlide(slideId: Int) {
        viewModelScope.launch {
            createAutoBackup()
            val list = allSlides.value
            val original = list.find { it.slide.id == slideId } ?: return@launch
            
            val nextId = (list.maxOfOrNull { it.slide.id } ?: 0) + 1
            val duplicatedSlide = original.slide.copy(
                id = nextId,
                title = "${original.slide.title} (Copy)",
                category = original.slide.category
            )
            val duplicatedVars = original.variables.map { 
                it.copy(id = 0L, slideId = nextId) 
            }
            
            repository.updateSlideAndVariables(duplicatedSlide, duplicatedVars)
            saveNewFormulaToFirestoreDirect(duplicatedSlide, duplicatedVars)
            _activeSlideId.value = nextId // set duplicated slide active!
            logAction("DUPLICATE_SLIDE", "Duplicated slide #$slideId as new slide #$nextId.")
        }
    }

    // --- NEW: Slide Reordering via Swapping ---
    fun swapSlides(id1: Int, id2: Int) {
        viewModelScope.launch {
            createAutoBackup()
            val list = allSlides.value
            val item1 = list.find { it.slide.id == id1 } ?: return@launch
            val item2 = list.find { it.slide.id == id2 } ?: return@launch

            val slide1 = item1.slide
            val slide2 = item2.slide

            // Swap slide parameters (retaining respective slide IDs)
            val newSlide1 = slide2.copy(id = id1)
            val newSlide2 = slide1.copy(id = id2)

            // Swap variable relations (attaching item1's variables to ID2, and item2's to ID1)
            val varsFor1 = item2.variables.map { it.copy(id = 0L, slideId = id1) }
            val varsFor2 = item1.variables.map { it.copy(id = 0L, slideId = id2) }

            repository.updateSlideAndVariables(newSlide1, varsFor1)
            repository.updateSlideAndVariables(newSlide2, varsFor2)
            saveNewFormulaToFirestoreDirect(newSlide1, varsFor1)
            saveNewFormulaToFirestoreDirect(newSlide2, varsFor2)

            logAction("REORDER_SLIDE", "Swapped positions of slide $id1 (${slide1.title}) and slide $id2 (${slide2.title})")
            
            // Adjust selection pointer if swapped active slide
            if (_activeSlideId.value == id1) {
                _activeSlideId.value = id2
            } else if (_activeSlideId.value == id2) {
                _activeSlideId.value = id1
            }
        }
    }

    /**
     * Slide Navigation (Adjusted bounds dynamically to support unlimited slides)
     */
    fun selectSlide(id: Int) {
        val exists = allSlides.value.any { it.slide.id == id }
        if (exists) {
            _activeSlideId.value = id
        }
    }

    fun nextSlide() {
        val slides = allSlides.value
        if (slides.isNotEmpty()) {
            val currentIndex = slides.indexOfFirst { it.slide.id == _activeSlideId.value }
            val nextIndex = if (currentIndex != -1 && currentIndex < slides.lastIndex) {
                currentIndex + 1
            } else {
                0
            }
            _activeSlideId.value = slides[nextIndex].slide.id
        }
    }

    fun previousSlide() {
        val slides = allSlides.value
        if (slides.isNotEmpty()) {
            val currentIndex = slides.indexOfFirst { it.slide.id == _activeSlideId.value }
            val prevIndex = if (currentIndex > 0) {
                currentIndex - 1
            } else {
                slides.lastIndex
            }
            _activeSlideId.value = slides[prevIndex].slide.id
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun toggleAdminMode() {
        _isAdminMode.value = !_isAdminMode.value
    }

    fun setAdminMode(enabled: Boolean) {
        _isAdminMode.value = enabled
    }

    fun toggleDarkMode() {
        _followSystemTheme.value = false
        _isDarkMode.value = !_isDarkMode.value
        saveFlowPrefs()
    }

    fun toggleFollowSystemTheme() {
        _followSystemTheme.value = !_followSystemTheme.value
        saveFlowPrefs()
    }

    fun setFollowSystemTheme(follow: Boolean) {
        _followSystemTheme.value = follow
        saveFlowPrefs()
    }

    fun setDecimalPlaces(places: Int) {
        if (places in 0..8) {
            _decimalPlaces.value = places
            logAction("DECIMAL_SETTINGS_CHANGE", "Changed outcome precision setting to: $places decimal places.")
            saveFlowPrefs()
        }
    }

    private fun logFormulaUpdateError(id: Int?, name: String?, expression: String?, errorMsg: String, exception: Throwable?) {
        android.util.Log.e("ChemDoseApp", "Formula ID: $id")
        android.util.Log.e("ChemDoseApp", "Formula Name: $name")
        android.util.Log.e("ChemDoseApp", "Formula Expression: $expression")
        android.util.Log.e("ChemDoseApp", "Error Message: $errorMsg")
        if (exception != null) {
            android.util.Log.e("ChemDoseApp", "Stack Trace: ${android.util.Log.getStackTraceString(exception)}")
        } else {
            android.util.Log.e("ChemDoseApp", "Stack Trace: No Exception thrown (Validation Failure)")
        }
    }

    /**
     * Slide and Variable Editing (CRUD) - Writes directly to reactive Room DB (Autosave!)
     */
    fun updateSlideDetails(slideId: Int, title: String, formula: String, resultUnit: String, description: String, category: String) {
        viewModelScope.launch {
            _formulaSaveStatus.value = FormulaOperationStatus.Idle
            
            // 1. Formula object & Null checks
            val currentSlideWithVars = allSlides.value.find { it.slide.id == slideId }
            if (currentSlideWithVars == null) {
                val errMsg = "Formula object is null or not found."
                logFormulaUpdateError(slideId, title, formula, errMsg, null)
                _formulaSaveStatus.value = FormulaOperationStatus.Error(errMsg)
                return@launch
            }
            val slideObj = currentSlideWithVars.slide
            if (slideObj == null) {
                val errMsg = "Formula object slide component is null."
                logFormulaUpdateError(slideId, title, formula, errMsg, null)
                _formulaSaveStatus.value = FormulaOperationStatus.Error(errMsg)
                return@launch
            }

            // 2. Formula ID must be valid
            if (slideId <= 0) {
                val errMsg = "Formula ID must be valid."
                logFormulaUpdateError(slideId, title, formula, errMsg, null)
                _formulaSaveStatus.value = FormulaOperationStatus.Error(errMsg)
                return@launch
            }

            // 3. Formula name cannot be empty
            if (title.isNullOrBlank()) {
                val errMsg = "Formula name cannot be empty."
                logFormulaUpdateError(slideId, title, formula, errMsg, null)
                _formulaSaveStatus.value = FormulaOperationStatus.Error(errMsg)
                return@launch
            }

            // 4. Formula expression cannot be empty
            if (formula.isNullOrBlank()) {
                val errMsg = "Formula expression cannot be empty."
                logFormulaUpdateError(slideId, title, formula, errMsg, null)
                _formulaSaveStatus.value = FormulaOperationStatus.Error(errMsg)
                return@launch
            }

            // 5. Parameter list cannot be null
            val parameters = currentSlideWithVars.variables
            if (parameters == null) {
                val errMsg = "Parameter list cannot be null."
                logFormulaUpdateError(slideId, title, formula, errMsg, null)
                _formulaSaveStatus.value = FormulaOperationStatus.Error(errMsg)
                return@launch
            }

            // 6. Units cannot be null
            if (resultUnit == null) {
                val errMsg = "Units cannot be null (Result unit is null)."
                logFormulaUpdateError(slideId, title, formula, errMsg, null)
                _formulaSaveStatus.value = FormulaOperationStatus.Error(errMsg)
                return@launch
            }
            for (param in parameters) {
                if (param == null) {
                    val errMsg = "Parameter object is null."
                    logFormulaUpdateError(slideId, title, formula, errMsg, null)
                    _formulaSaveStatus.value = FormulaOperationStatus.Error(errMsg)
                    return@launch
                }
                if (param.symbol.isNullOrBlank()) {
                    val errMsg = "Parameter symbol cannot be empty."
                    logFormulaUpdateError(slideId, title, formula, errMsg, null)
                    _formulaSaveStatus.value = FormulaOperationStatus.Error(errMsg)
                    return@launch
                }
                if (param.unit == null) {
                    val errMsg = "Units cannot be null (Parameter unit is null)."
                    logFormulaUpdateError(slideId, title, formula, errMsg, null)
                    _formulaSaveStatus.value = FormulaOperationStatus.Error(errMsg)
                    return@launch
                }
            }

            // 7. Duplicate formula name check
            val isDuplicateTitle = allSlides.value.any { 
                it.slide.id != slideId && it.slide.title.equals(title, ignoreCase = true) 
            }
            if (isDuplicateTitle) {
                val errMsg = "Duplicate formula name: '$title' already exists."
                logFormulaUpdateError(slideId, title, formula, errMsg, null)
                _formulaSaveStatus.value = FormulaOperationStatus.Error(errMsg)
                return@launch
            }

            // 8. Parse / Syntax validation before saving
            val parsedTokens = try {
                if (formula.trim().startsWith("[")) {
                    FormulaSerializer.deserialize(formula) ?: FormulaEvaluator.tokenize(formula)
                } else {
                    FormulaEvaluator.tokenize(formula)
                }
            } catch (e: Exception) {
                val errMsg = "Invalid formula syntax tokenizer failure: ${e.message}"
                logFormulaUpdateError(slideId, title, formula, errMsg, e)
                _formulaSaveStatus.value = FormulaOperationStatus.Error(errMsg)
                return@launch
            }

            val allowedVariablesMap = parameters.associate { it.symbol to it.value }
            val syntaxError = FormulaEvaluator.validateTokens(parsedTokens, allowedVariablesMap)
            if (syntaxError != null) {
                val errMsg = "Formula syntax validation error: $syntaxError"
                logFormulaUpdateError(slideId, title, formula, errMsg, null)
                _formulaSaveStatus.value = FormulaOperationStatus.Error(errMsg)
                return@launch
            }

            // 9. Safe database write
            try {
                createAutoBackup()
                val updatedSlide = Slide(
                    id = slideId,
                    title = title,
                    formula = formula,
                    resultUnit = resultUnit,
                    description = description,
                    category = category
                )
                repository.updateSlide(updatedSlide)
                saveNewFormulaToFirestoreDirect(updatedSlide, parameters)
                logAction("EDIT_FORMULA", "Modified formula details of slide #$slideId: $title ($formula)")
                _formulaSaveStatus.value = FormulaOperationStatus.Success
            } catch (e: Exception) {
                val errMsg = "Safe database transaction write failed: ${e.message}"
                logFormulaUpdateError(slideId, title, formula, errMsg, e)
                _formulaSaveStatus.value = FormulaOperationStatus.Error(errMsg)
            }
        }
    }

    fun updateStarchKitchenAsaFlowInput(value: Double) {
        viewModelScope.launch {
            val list = allSlides.value
            val slide2 = list.find { it.slide.id == 2 } ?: return@launch
            val variableF = slide2.variables.find { it.symbol == "F" } ?: return@launch
            val updatedVar = variableF.copy(value = value)
            repository.updateVariable(updatedVar)
            logAction("LINKAGE_UPDATE", "Automatically updated C-Starch's ASA Flow input (F) to $value from ASA Calculator output")
            triggerAutoSync()
        }
    }

    fun updateVariableValue(variable: Variable, value: Double) {
        viewModelScope.launch {
            val updatedVar = variable.copy(value = value)
            repository.updateVariable(updatedVar)
            triggerAutoSync()
        }
    }

    fun addVariableToSlide(
        slideId: Int,
        symbol: String,
        name: String,
        value: Double,
        unit: String,
        isRequired: Boolean = true,
        isHidden: Boolean = false,
        sortOrder: Int = 0
    ) {
        viewModelScope.launch {
            createAutoBackup()
            val newVar = Variable(
                slideId = slideId,
                symbol = symbol,
                name = name,
                value = value,
                unit = unit,
                isRequired = isRequired,
                isHidden = isHidden,
                sortOrder = sortOrder
            )
            repository.insertVariable(newVar)
            logAction("EDIT_FORMULA", "Added variable $symbol ($name) to slide #$slideId")
            kotlinx.coroutines.delay(200)
            saveFormulaToFirestoreDirect(slideId)
        }
    }

    fun updateVariableSpecs(
        variable: Variable,
        symbol: String,
        name: String,
        value: Double,
        unit: String,
        isRequired: Boolean = variable.isRequired,
        isHidden: Boolean = variable.isHidden,
        sortOrder: Int = variable.sortOrder
    ) {
        viewModelScope.launch {
            createAutoBackup()
            val updatedVar = variable.copy(
                symbol = symbol,
                name = name,
                value = value,
                unit = unit,
                isRequired = isRequired,
                isHidden = isHidden,
                sortOrder = sortOrder
            )
            repository.updateVariable(updatedVar)
            logAction("EDIT_FORMULA", "Updated variable specs of $symbol on slide #${variable.slideId}")
            kotlinx.coroutines.delay(200)
            saveFormulaToFirestoreDirect(variable.slideId)
        }
    }

    fun updateVariablesOrder(variables: List<Variable>) {
        viewModelScope.launch {
            createAutoBackup()
            variables.forEachIndexed { index, variable ->
                val updatedVar = variable.copy(sortOrder = index)
                repository.updateVariable(updatedVar)
            }
            logAction("EDIT_FORMULA", "Reordered parameters for slide #${variables.firstOrNull()?.slideId}")
            val firstSlideId = variables.firstOrNull()?.slideId
            if (firstSlideId != null) {
                kotlinx.coroutines.delay(200)
                saveFormulaToFirestoreDirect(firstSlideId)
            }
        }
    }

    fun deleteVariableFromSlide(variable: Variable) {
        viewModelScope.launch {
            createAutoBackup()
            repository.deleteVariable(variable)
            logAction("EDIT_FORMULA", "Deleted variable ${variable.symbol} from slide #${variable.slideId}")
            kotlinx.coroutines.delay(200)
            saveFormulaToFirestoreDirect(variable.slideId)
        }
    }

    /**
     * Backup & Restore Database
     */
    fun getBackupDataJson(): String {
        logAction("BACKUP", "Initiated formula database copy stream backup.")
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        val dateStr = sdf.format(java.util.Date())
        
        val slides = allSlides.value.map { it.slide }
        val variables = allSlides.value.flatMap { it.variables }
        val historyList = _localHistory.value.map {
            BackupCalculationLog(
                slideId = it.slideId,
                timestamp = it.timestamp,
                inputs = it.inputs,
                result = it.result
            )
        }
        
        return BackupHelper.createBackupJson(
            slides = slides,
            variables = variables,
            activeSlideId = _activeSlideId.value,
            decimalPlaces = _decimalPlaces.value,
            isDarkMode = _isDarkMode.value,
            adminPassword = _adminPassword.value,
            favoriteSlideIds = _favoriteSlideIds.value.toList(),
            calculationHistory = historyList,
            backupDate = dateStr,
            version = "1.0"
        )
    }

    /**
     * Generates a local cloud sync JSON blueprint containing ALL formulas and variables in the exact API payload format.
     */
    fun getCloudSyncBlueprintJson(): String {
        logAction("EXPORT_BLUEPRINT", "Generated cloud synchronization payload schema JSON.")
        return BackupHelper.createCloudBlueprintJson(allSlides.value)
    }

    /**
     * Decodes and validates a cloud sync JSON blueprint, and updates local Room storage safely if successful.
     */
    fun importCloudSyncBlueprintJson(json: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val formulas = BackupHelper.restoreFromCloudBlueprintJson(json)
            if (formulas != null) {
                try {
                    // Turn NetworkFormulas into local Slides & Variables
                    for (nf in formulas) {
                        val slide = Slide(
                            id = nf.id,
                            title = nf.title,
                            category = nf.category,
                            formula = nf.formula,
                            resultUnit = nf.resultUnit,
                            description = nf.description
                        )
                        val vars = nf.variables.map { nv ->
                            Variable(
                                id = 0,
                                slideId = nf.id,
                                symbol = nv.symbol,
                                name = nv.name,
                                value = nv.value,
                                unit = nv.unit
                            )
                        }
                        repository.updateSlideAndVariables(slide, vars)
                    }
                    val msg = "Validated successfully! Simulated pulling ${formulas.size} formulas into local Room database persistence layer."
                    logAction("IMPORT_BLUEPRINT_SUCCESS", msg)
                    onResult(true, msg)
                } catch (e: Exception) {
                    val errMsg = "Database write failure: ${e.localizedMessage ?: "Unknown SQLite write error"}"
                    logAction("IMPORT_BLUEPRINT_FAILED", errMsg)
                    onResult(false, errMsg)
                }
            } else {
                val errMsg = "Invalid cloud JSON format. Must match standard List<NetworkFormula> schema."
                logAction("IMPORT_BLUEPRINT_FAILED", errMsg)
                onResult(false, errMsg)
            }
        }
    }

    data class LocalBackupInfo(
        val name: String,
        val filePath: String,
        val size: String,
        val date: String,
        val version: String,
        val isValid: Boolean
    )
    
    private val _localBackups = MutableStateFlow<List<LocalBackupInfo>>(emptyList())
    val localBackups: StateFlow<List<LocalBackupInfo>> = _localBackups.asStateFlow()

    fun refreshLocalBackups() {
        val list = mutableListOf<LocalBackupInfo>()
        try {
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            val chemicalDashboardDir = java.io.File(downloadsDir, "ChemDoseFormula")
            if (chemicalDashboardDir.exists() && chemicalDashboardDir.isDirectory) {
                val files = chemicalDashboardDir.listFiles { _, name -> name.endsWith(".json") }
                files?.forEach { file ->
                    val sizeStr = getFolderSizeLabel(file.length())
                    val lastModified = file.lastModified()
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                    val formattedDate = sdf.format(java.util.Date(lastModified))
                    
                    var version = "Unknown"
                    var isValid = false
                    var backupDate = formattedDate
                    try {
                        val content = file.readText()
                        val parsed = BackupHelper.restoreFromBackupJson(content)
                        if (parsed != null) {
                            isValid = true
                            version = parsed.version ?: "1.0"
                            backupDate = parsed.backupDate ?: formattedDate
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    
                    list.add(
                        LocalBackupInfo(
                            name = file.name,
                            filePath = file.absolutePath,
                            size = sizeStr,
                            date = backupDate,
                            version = version,
                            isValid = isValid
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _localBackups.value = list.sortedByDescending { it.date }
    }
    
    private fun getFolderSizeLabel(bytes: Long): String {
        return if (bytes < 1024) {
            "$bytes B"
        } else if (bytes < 1024 * 1024) {
            String.format(java.util.Locale.US, "%.1f KB", bytes / 1024.0)
        } else {
            String.format(java.util.Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
        }
    }

    fun createAutoBackup() {
        try {
            val json = getBackupDataJson()
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            val chemicalDashboardDir = java.io.File(downloadsDir, "ChemDoseFormula")
            if (!chemicalDashboardDir.exists()) {
                chemicalDashboardDir.mkdirs()
            }
            val sdf = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
            val dateStr = sdf.format(java.util.Date())
            val file = java.io.File(chemicalDashboardDir, "auto_backup_$dateStr.json")
            file.writeText(json)
            android.util.Log.d("ChemDoseBackup", "Created automatic backup before modification: ${file.absolutePath}")
            refreshLocalBackups()
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("ChemDoseBackup", "Failed to create automatic backup", e)
        }
    }

    fun exportBackup(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val json = getBackupDataJson()
                val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                val chemicalDashboardDir = java.io.File(downloadsDir, "ChemDoseFormula")
                if (!chemicalDashboardDir.exists()) {
                    chemicalDashboardDir.mkdirs()
                }
                val sdf = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
                val dateStr = sdf.format(java.util.Date())
                val file = java.io.File(chemicalDashboardDir, "ChemDoseFormula_Backup_$dateStr.json")
                file.writeText(json)
                
                refreshLocalBackups()
                onResult(true, "Backup saved to: Downloads/ChemDoseFormula/${file.name}")
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false, e.localizedMessage ?: "Failed to write backup file")
            }
        }
    }

    /**
     * Imports dynamic state and updates schema safely
     */
    fun importBackup(json: String, onResult: (Boolean) -> Unit) {
        val parsed = BackupHelper.restoreFromBackupJson(json)
        if (parsed != null) {
            viewModelScope.launch {
                try {
                    repository.resetDatabase(parsed.slides, parsed.variables)
                    
                    // Restore calculator states if present
                    parsed.activeSlideId?.let { id ->
                        _activeSlideId.value = id
                    }
                    parsed.decimalPlaces?.let { places ->
                        _decimalPlaces.value = places
                        saveFlowPrefs()
                    }
                    parsed.isDarkMode?.let { darkMode ->
                        _isDarkMode.value = darkMode
                    }
                    parsed.adminPassword?.let { pwd ->
                        changeAdminPassword(pwd)
                    }
                    parsed.favoriteSlideIds?.let { favs ->
                        val updated = favs.toSet()
                        _favoriteSlideIds.value = updated
                        saveFavoritePrefs(updated)
                    }
                    parsed.calculationHistory?.let { history ->
                        _localHistory.value = history.map {
                            LocalCalculationLog(
                                slideId = it.slideId,
                                timestamp = it.timestamp,
                                inputs = it.inputs,
                                result = it.result
                            )
                        }
                    }

                    refreshLocalBackups()
                    onResult(true)
                    logAction("RESTORE", "Successfully restored database backup (${parsed.slides.size} slides, ${parsed.variables.size} variables).")
                } catch (e: Exception) {
                    e.printStackTrace()
                    onResult(false)
                    logAction("RESTORE_FAILED", "Failed restore query execution.")
                }
            }
        } else {
            onResult(false)
            logAction("RESTORE_FAILED", "Failed backup deserialization schema parse stage.")
        }
    }

    /**
     * Resets active slide back to basic original templates or fully blank custom slot
     */
    fun resetActiveSlide() {
        val activeId = _activeSlideId.value
        viewModelScope.launch {
            createAutoBackup()
            if (activeId <= 10) {
                // Re-add default slide configuration
                // Just toggle pre-populate check after wipe is done
                // But specifically for this slide ID, let's delete custom vars and reinstate defaults:
                val defaultSlideAndVars = getDefaultSlideSetupForId(activeId)
                if (defaultSlideAndVars != null) {
                    repository.updateSlideAndVariables(defaultSlideAndVars.first, defaultSlideAndVars.second)
                    logAction("RESET_FORMULA", "Reset slide #$activeId back to factory standard presets.")
                }
            } else {
                // Completely blank custom slot
                val blankSlide = Slide(
                    id = activeId,
                    title = "Formula Slide $activeId",
                    formula = "A + B",
                    resultUnit = "_",
                    description = "Custom calculator slot for manual paper mill calculations.",
                    category = "Custom"
                )
                val blankVars = listOf(
                    Variable(slideId = activeId, symbol = "A", name = "Input 1", value = 0.0, unit = "%"),
                    Variable(slideId = activeId, symbol = "B", name = "Input 2", value = 0.0, unit = "kg")
                )
                repository.updateSlideAndVariables(blankSlide, blankVars)
                logAction("RESET_FORMULA", "Cleared custom slide #$activeId to generic blank structures.")
            }
        }
    }

    fun triggerGlobalWipeReset() {
        viewModelScope.launch {
            createAutoBackup()
            repository.resetToFactoryDefaults()
            logAction("GLOBAL_WIPE_RESET", "Triggered extreme clean database wipe and full preset re-population.")
        }
    }

    private fun getDefaultSlideSetupForId(id: Int): Pair<Slide, List<Variable>>? {
        // Return matching preset config
        val title: String
        val formula: String
        val category: String
        val desc: String
        val unit: String
        val vars: List<Variable>

        when (id) {
            1 -> {
                title = "ASA Calculator"
                category = "Chemical Dosage"
                desc = "Calculates ASA sizing chemical flow rate in kg/H based on paper machine production rate."
                formula = "P * D"
                unit = "kg/H"
                vars = listOf(
                    Variable(slideId = 1, symbol = "P", name = "Production", value = 20.0, unit = "MT/H"),
                    Variable(slideId = 1, symbol = "D", name = "ASA Dosage", value = 1.2, unit = "kg/T")
                )
            }
            2 -> {
                title = "C-Starch Calculator"
                category = "Chemical Dosage"
                desc = "Calculates cationic starch solid rate (kg/H) and total starch solution flow rate (kg/H) based on ASA flow rate.\n---outputs---\nC-Starch Solution Flow:(F * R) / (C * 0.01):kg/H"
                formula = "F * R"
                unit = "kg/H"
                vars = listOf(
                    Variable(slideId = 2, symbol = "F", name = "ASA Flow", value = 24.0, unit = "kg/H"),
                    Variable(slideId = 2, symbol = "R", name = "ASA : C-Starch Ratio", value = 2.5, unit = "ratio"),
                    Variable(slideId = 2, symbol = "C", name = "C-Starch Concentration", value = 3.0, unit = "%")
                )
            }
            3 -> {
                title = "AKD Calculator"
                category = "Chemical Dosage"
                desc = "Calculates Alkylketene Dimer sizing chemical flow rate based on machine production.\n---outputs---\nAKD Wet Product Flow:(P * D) / (C * 0.01):kg/H"
                formula = "P * D"
                unit = "kg/H"
                vars = listOf(
                    Variable(slideId = 3, symbol = "P", name = "Production", value = 20.0, unit = "MT/H"),
                    Variable(slideId = 3, symbol = "D", name = "AKD Dosage", value = 1.5, unit = "kg/T"),
                    Variable(slideId = 3, symbol = "C", name = "AKD Concentration", value = 15.0, unit = "%")
                )
            }
            4 -> {
                title = "PAC Calculator"
                category = "Chemical Dosage"
                desc = "Calculates PAC active mass flow and commercial liquid solution flow rate.\n---outputs---\nPAC Solution Flow:(P * D) / (C * 0.01):kg/H"
                formula = "P * D"
                unit = "kg/H"
                vars = listOf(
                    Variable(slideId = 4, symbol = "P", name = "Production", value = 20.0, unit = "MT/H"),
                    Variable(slideId = 4, symbol = "D", name = "PAC Dosage", value = 5.0, unit = "kg/T"),
                    Variable(slideId = 4, symbol = "C", name = "PAC Concentration", value = 10.0, unit = "%")
                )
            }
            5 -> {
                title = "Alum Calculator"
                category = "Chemical Dosage"
                desc = "Calculates Alum dry flow and commercial liquid solution flow rate.\n---outputs---\nAlum Solution Flow:(P * D) / (C * 0.01):kg/H"
                formula = "P * D"
                unit = "kg/H"
                vars = listOf(
                    Variable(slideId = 5, symbol = "P", name = "Production", value = 20.0, unit = "MT/H"),
                    Variable(slideId = 5, symbol = "D", name = "Alum Dosage", value = 8.0, unit = "kg/T"),
                    Variable(slideId = 5, symbol = "C", name = "Alum Concentration", value = 8.0, unit = "%")
                )
            }
            6 -> {
                title = "Bentonite Calculator"
                category = "Chemical Dosage"
                desc = "Calculates bentonite microparticle active dry flow rate and wet slurry flow rate.\n---outputs---\nBentonite Slurry Flow:(P * D) / (C * 0.01):kg/H"
                formula = "P * D"
                unit = "kg/H"
                vars = listOf(
                    Variable(slideId = 6, symbol = "P", name = "Production", value = 20.0, unit = "MT/H"),
                    Variable(slideId = 6, symbol = "D", name = "Bentonite Dosage", value = 2.0, unit = "kg/T"),
                    Variable(slideId = 6, symbol = "C", name = "Bentonite Concentration", value = 5.0, unit = "%")
                )
            }
            7 -> {
                title = "Defoamer Calculator"
                category = "Chemical Dosage"
                desc = "Calculates defoamer chemical flow rate in kg/H based on production rate."
                formula = "P * D"
                unit = "kg/H"
                vars = listOf(
                    Variable(slideId = 7, symbol = "P", name = "Production", value = 20.0, unit = "MT/H"),
                    Variable(slideId = 7, symbol = "D", name = "Defoamer Dosage", value = 0.3, unit = "kg/T")
                )
            }
            8 -> {
                title = "Retention Aid Calculator"
                category = "Chemical Dosage"
                desc = "Calculates retention aid active polymer flow rate.\n---outputs---\nPolymer Preparation Flow:((P * D) / 1000) / (C * 0.01):kg/H"
                formula = "(P * D) / 1000"
                unit = "kg/H"
                vars = listOf(
                    Variable(slideId = 8, symbol = "P", name = "Production", value = 20.0, unit = "MT/H"),
                    Variable(slideId = 8, symbol = "D", name = "Retention Aid Dosage", value = 150.0, unit = "g/T"),
                    Variable(slideId = 8, symbol = "C", name = "Polymer Concentration", value = 0.1, unit = "%")
                )
            }
            9 -> {
                title = "Wet Strength Resin"
                category = "Chemical Dosage"
                desc = "Calculates Wet Strength Resin active dry solids rate and product wet solution flowrate.\n---outputs---\nWSR Solution Flow:(P * D) / (C * 0.01):kg/H"
                formula = "P * D"
                unit = "kg/H"
                vars = listOf(
                    Variable(slideId = 9, symbol = "P", name = "Production", value = 20.0, unit = "MT/H"),
                    Variable(slideId = 9, symbol = "D", name = "WSR Dosage", value = 10.0, unit = "kg/T"),
                    Variable(slideId = 9, symbol = "C", name = "WSR Concentration", value = 12.5, unit = "%")
                )
            }
            10 -> {
                title = "Dry Strength Resin"
                category = "Chemical Dosage"
                desc = "Calculates Dry Strength Resin active dry solids and solution feed flow rate.\n---outputs---\nDSR Solution Flow:(P * D) / (C * 0.01):kg/H"
                formula = "P * D"
                unit = "kg/H"
                vars = listOf(
                    Variable(slideId = 10, symbol = "P", name = "Production", value = 20.0, unit = "MT/H"),
                    Variable(slideId = 10, symbol = "D", name = "DSR Dosage", value = 6.0, unit = "kg/T"),
                    Variable(slideId = 10, symbol = "C", name = "DSR Concentration", value = 15.0, unit = "%")
                )
            }
            else -> return null
        }

        return Pair(
            Slide(id, title, formula, unit, desc, category),
            vars
        )
    }

    // --- FIREBASE AND CLOUD STORAGE AUTH/SYNC METHODS ---
    private val _firebaseConfig = MutableStateFlow(FirebaseConfig())
    val firebaseConfig: StateFlow<FirebaseConfig> = _firebaseConfig.asStateFlow()

    private val _firebaseUserUid = MutableStateFlow<String?>(null)
    val firebaseUserUid: StateFlow<String?> = _firebaseUserUid.asStateFlow()

    private val _firebaseAuthStatus = MutableStateFlow("Disabled")
    val firebaseAuthStatus: StateFlow<String> = _firebaseAuthStatus.asStateFlow()

    private val _syncStatus = MutableStateFlow("Offline")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    private val _lastBackupTime = MutableStateFlow("Never")
    val lastBackupTime: StateFlow<String> = _lastBackupTime.asStateFlow()

    private val _lastBackupStatus = MutableStateFlow("Idle")
    val lastBackupStatus: StateFlow<String> = _lastBackupStatus.asStateFlow()

    fun updateFirebaseConfig(config: FirebaseConfig) {
        _firebaseConfig.value = config
        saveFirebaseConfigToSharedPrefs(config)
        
        if (config.isEnabled) {
            val initialized = FirebaseService.initialize(getApplication(), config)
            if (initialized) {
                _firebaseAuthStatus.value = "Firebase Ready"
                _syncStatus.value = "Connecting..."
                startRealtimeFormulaListener()
                if (_firebaseUserUid.value == null) {
                    signInFirebaseAnonymously { success ->
                        if (success) {
                            runFirestoreTest()
                        }
                    }
                } else {
                    runFirestoreTest()
                }
            } else {
                _firebaseAuthStatus.value = "Disabled/Uninitialized"
                _syncStatus.value = "Error: GOOGLE_SERVICES_JSON_ERROR"
                _firebaseUserUid.value = null
            }
        } else {
            _syncStatus.value = "Offline"
            _firebaseAuthStatus.value = "Disabled"
            _firebaseUserUid.value = null
        }
    }

    fun runFirestoreTest(onComplete: ((Boolean) -> Unit)? = null) {
        val uid = _firebaseUserUid.value
        val config = _firebaseConfig.value
        if (config.databaseUrl.isNotEmpty()) {
            try {
                val database = com.google.firebase.database.FirebaseDatabase.getInstance(config.databaseUrl)
                val testRef = database.getReference("users").child(uid ?: "anonymous_guest").child("data").child("connectivity_test")
                testRef.setValue(java.text.DateFormat.getDateTimeInstance().format(java.util.Date()))
                    .addOnSuccessListener {
                        _syncStatus.value = "Online"
                        retryPendingUserProfiles()
                        retryPendingUserActivityLogs()
                        onComplete?.invoke(true)
                    }
                    .addOnFailureListener { e ->
                        _syncStatus.value = "Error: ${e.localizedMessage}"
                        _firebaseAuthStatus.value = "Cloud sync unavailable. Using offline mode."
                        onComplete?.invoke(false)
                    }
            } catch (e: Exception) {
                _syncStatus.value = "Error: ${e.localizedMessage}"
                _firebaseAuthStatus.value = "Cloud sync unavailable. Using offline mode."
                onComplete?.invoke(false)
            }
        } else {
            FirebaseService.testFirestoreConnection(uid) { testSuccess, message ->
                if (testSuccess) {
                    _syncStatus.value = "Online"
                    startRealtimeFormulaListener()
                    // On connection success, retry pending Room queues
                    retryPendingUserProfiles()
                    retryPendingUserActivityLogs()
                    onComplete?.invoke(true)
                } else {
                    _syncStatus.value = "Error: $message"
                    _firebaseAuthStatus.value = "Cloud sync unavailable. Using offline mode."
                    onComplete?.invoke(false)
                }
            }
        }
    }

    fun retrySync() {
        val config = _firebaseConfig.value
        if (!config.isEnabled) {
            _syncStatus.value = "Offline"
            return
        }
        _syncStatus.value = "Connecting..."
        val initialized = FirebaseService.initialize(getApplication(), config)
        if (initialized) {
            if (_firebaseUserUid.value == null) {
                signInFirebaseAnonymously { success ->
                    if (success) {
                        runFirestoreTest()
                    }
                }
            } else {
                runFirestoreTest()
            }
        } else {
            _syncStatus.value = "Error: GOOGLE_SERVICES_JSON_ERROR"
            _firebaseAuthStatus.value = "Cloud sync unavailable. Using offline mode."
        }
    }

    fun autoConfigureFirebase() {
        val autoConfig = FirebaseConfig(
            isEnabled = true,
            autoSync = true
        )
        updateFirebaseConfig(autoConfig)
    }

    fun signInFirebaseAnonymously(onComplete: ((Boolean) -> Unit)? = null) {
        if (!FirebaseService.isInitialized) {
            _syncStatus.value = "Error: GOOGLE_SERVICES_JSON_ERROR"
            onComplete?.invoke(false)
            return
        }
        _firebaseAuthStatus.value = "Authenticating..."
        FirebaseService.signInAnonymously { success, uid ->
            if (success && uid != null) {
                _firebaseUserUid.value = uid
                _firebaseAuthStatus.value = "Authenticated (UID: $uid)"
                logAction("FIREBASE_AUTH_SUCCESS", "Anonymous session established. UID: $uid")
                startRealtimeFormulaListener()
                triggerAutoSync()
                onComplete?.invoke(true)
            } else {
                _firebaseUserUid.value = null
                _firebaseAuthStatus.value = "Auth Failed ($uid)"
                logAction("FIREBASE_AUTH_FAILED", "Authentication failed: $uid")
                val errReason = if (uid?.contains("API key") == true || uid?.contains("invalid") == true) {
                    "GOOGLE_SERVICES_JSON_ERROR"
                } else {
                    "NETWORK_ERROR"
                }
                _syncStatus.value = "Error: $errReason"
                onComplete?.invoke(false)
            }
        }
    }

    fun loginFirebaseWithEmail(email: String, password: String) {
        if (!FirebaseService.isInitialized) return
        _firebaseAuthStatus.value = "Authenticating..."
        FirebaseService.loginWithEmail(email, password) { success, uid ->
            if (success && uid != null) {
                syncLoginStatus(uid, email) {
                    _firebaseUserUid.value = uid
                    _firebaseAuthStatus.value = "Authenticated (Email: $email)"
                    logAction("FIREBASE_AUTH_SUCCESS", "Authenticated via Email. UID: $uid")
                    startRealtimeFormulaListener()
                    triggerAutoSync()
                }
            } else {
                _firebaseUserUid.value = null
                _firebaseAuthStatus.value = "Auth Failed: $uid"
                logAction("FIREBASE_AUTH_FAILED", "Email authentication failed: $uid")
            }
        }
    }

    fun signOutFirebase() {
        val currentUid = _firebaseUserUid.value ?: FirebaseService.getAuth()?.currentUser?.uid
        val currentEmail = _loggedInEmail.value ?: FirebaseService.getAuth()?.currentUser?.email ?: "guest_user"
        if (currentUid != null) {
            syncLogoutStatus(currentUid, currentEmail) {
                _firebaseUserUid.value = null
                _firebaseAuthStatus.value = "Logged Out"
                _lastBackupStatus.value = "Disconnected"
                logAction("FIREBASE_LOGOUT", "Terminated Firebase user session.")
            }
        } else {
            try {
                FirebaseService.getAuth()?.signOut()
            } catch (e: Exception) {
                Log.e("ViewModel", "signOut error", e)
            }
            _firebaseUserUid.value = null
            _firebaseAuthStatus.value = "Logged Out"
            _lastBackupStatus.value = "Disconnected"
            logAction("FIREBASE_LOGOUT", "Terminated Firebase user session.")
        }
    }

    private fun handleBackupSuccess(message: String) {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        val formattedDate = sdf.format(java.util.Date())
        _lastBackupTime.value = formattedDate
        _lastBackupStatus.value = "Success"
        
        val sp = getApplication<Application>().getSharedPreferences("firebase_prefs", Context.MODE_PRIVATE)
        sp.edit().putString("last_backup_time", formattedDate).apply()
    }

    private fun handleBackupFailure(message: String) {
        _lastBackupStatus.value = "Failed: $message"
    }

    fun backupToFirebase(onResult: (Boolean, String) -> Unit) {
        val uid = _firebaseUserUid.value
        _lastBackupStatus.value = "Backing up..."
        if (uid == null) {
            handleBackupFailure("No authenticated user")
            onResult(false, "No authenticated user. Authenticate anonymously or log in first.")
            return
        }

        val config = _firebaseConfig.value
        if (config.databaseUrl.isNotEmpty()) {
            FirebaseService.backupFormulasToRealtimeDatabase(
                userId = uid,
                databaseUrl = config.databaseUrl,
                slidesWithVars = allSlides.value,
                decimalPlaces = decimalPlaces.value
            ) { success, message ->
                logAction(if (success) "FIREBASE_BACKUP_SUCCESS" else "FIREBASE_BACKUP_FAILED", message)
                if (success) {
                    handleBackupSuccess(message)
                } else {
                    handleBackupFailure(message)
                }
                onResult(success, message)
            }
        } else {
            FirebaseService.backupFormulasToFirestore(
                userId = uid,
                slidesWithVars = allSlides.value,
                decimalPlaces = decimalPlaces.value
            ) { success, message ->
                logAction(if (success) "FIREBASE_BACKUP_SUCCESS" else "FIREBASE_BACKUP_FAILED", message)
                if (success) {
                    handleBackupSuccess(message)
                } else {
                    handleBackupFailure(message)
                }
                onResult(success, message)
            }
        }
    }

    fun restoreFromFirebase(onResult: (Boolean, String) -> Unit) {
        val uid = _firebaseUserUid.value
        if (uid == null) {
            onResult(false, "No authenticated user. Authenticate anonymously or log in first.")
            return
        }

        _firebaseAuthStatus.value = "Restoring data..."
        
        val config = _firebaseConfig.value
        val onRestoreComplete = { success: Boolean, networkFormulas: List<NetworkFormula>?, originalDecimalPlaces: Int?, message: String ->
            if (success && networkFormulas != null) {
                viewModelScope.launch {
                    try {
                        for (nf in networkFormulas) {
                            val slide = Slide(
                                id = nf.id,
                                title = nf.title,
                                category = nf.category,
                                formula = nf.formula,
                                resultUnit = nf.resultUnit,
                                description = nf.description
                            )
                            val vars = nf.variables.map { nv ->
                                Variable(
                                    id = 0,
                                    slideId = nf.id,
                                    symbol = nv.symbol,
                                    name = nv.name,
                                    value = nv.value,
                                    unit = nv.unit
                                )
                            }
                            repository.updateSlideAndVariables(slide, vars)
                        }

                        originalDecimalPlaces?.let { places ->
                            _decimalPlaces.value = places
                        }

                        _firebaseAuthStatus.value = "Authenticated (UID: $uid)"
                        logAction("FIREBASE_RESTORE_SUCCESS", "Restored ${networkFormulas.size} formulas and configuration parameters.")
                        onResult(true, "Successfully restored ${networkFormulas.size} formulas!")
                    } catch (e: java.lang.Exception) {
                        _firebaseAuthStatus.value = "Restore Failed"
                        onResult(false, "Failed to write database: ${e.localizedMessage}")
                    }
                }
            } else {
                _firebaseAuthStatus.value = "Authenticated (UID: $uid)"
                onResult(false, message)
            }
            Unit
        }

        if (config.databaseUrl.isNotEmpty()) {
            FirebaseService.restoreFormulasFromRealtimeDatabase(uid, config.databaseUrl, onRestoreComplete)
        } else {
            FirebaseService.restoreFormulasFromFirestore(uid, onRestoreComplete)
        }
    }

    fun triggerAutoSync() {
        val config = _firebaseConfig.value
        val uid = _firebaseUserUid.value
        if (config.isEnabled && config.autoSync && uid != null) {
            _lastBackupStatus.value = "Auto-syncing..."
            if (config.databaseUrl.isNotEmpty()) {
                FirebaseService.backupFormulasToRealtimeDatabase(uid, config.databaseUrl, allSlides.value, decimalPlaces.value) { success, msg ->
                    android.util.Log.d("AutoSync", "Offline-Sync Status: success=$success msg=$msg")
                    if (success) {
                        handleBackupSuccess("Auto-sync completed")
                    } else {
                        handleBackupFailure(msg)
                    }
                }
            } else {
                FirebaseService.backupFormulasToFirestore(uid, allSlides.value, decimalPlaces.value) { success, msg ->
                    android.util.Log.d("AutoSync", "Offline-Sync Status: success=$success msg=$msg")
                    if (success) {
                        handleBackupSuccess("Auto-sync completed")
                    } else {
                        handleBackupFailure(msg)
                    }
                }
            }
        }
    }

    private val _formulaCloudUpdateMessage = MutableStateFlow<String?>(null)
    val formulaCloudUpdateMessage: StateFlow<String?> = _formulaCloudUpdateMessage.asStateFlow()

    fun clearFormulaCloudUpdateMessage() {
        _formulaCloudUpdateMessage.value = null
    }

    private var formulaListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

    fun startRealtimeFormulaListener() {
        val firestore = FirebaseService.getFirestore()
        if (firestore == null) {
            Log.w("RealtimeFormula", "Firestore not initialized yet. Cannot start realtime listener.")
            return
        }

        // Unregister existing listener if any
        formulaListenerRegistration?.remove()

        Log.i("RealtimeFormula", "Starting real-time formula synchronization listener...")
        _syncStatus.value = "Syncing..."
        
        formulaListenerRegistration = firestore.collection("formulas")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("RealtimeFormula", "Firestore snapshot listener error", error)
                    _syncStatus.value = "Sync Error"
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    _syncStatus.value = "Empty Database"
                    return@addSnapshotListener
                }

                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val documents = snapshot.documents
                        if (documents.isEmpty()) {
                            Log.i("RealtimeFormula", "No formulas found in Firestore. If admin, we should prepopulate cloud.")
                            _syncStatus.value = "Cloud Empty"
                            if (_isAdminLoggedIn.value) {
                                Log.i("RealtimeFormula", "Admin detected and cloud is empty. Initializing cloud formulas with default/local templates...")
                                uploadAllFormulasToFirestoreQuietly()
                            }
                            return@launch
                        }

                        // Parse formulas from Firestore documents
                        val firestoreFormulaIds = mutableSetOf<Int>()
                        var changed = false

                        for (doc in documents) {
                            val formulaIdStr = doc.id
                            val formulaId = formulaIdStr.toIntOrNull() ?: continue
                            firestoreFormulaIds.add(formulaId)

                            val title = doc.getString("title") ?: ""
                            val category = doc.getString("category") ?: "Custom"
                            val formulaExpr = doc.getString("formulaExpression") ?: ""
                            val outputUnit = doc.getString("outputUnit") ?: ""
                            val description = doc.getString("description") ?: ""
                            
                            // Parse variables list
                            val variablesRaw = doc.get("variables") as? List<Map<String, Any>> ?: emptyList()
                            val parsedVars = variablesRaw.map { vMap ->
                                Variable(
                                    slideId = formulaId,
                                    symbol = vMap["symbol"] as? String ?: "",
                                    name = vMap["name"] as? String ?: "",
                                    value = (vMap["value"] as? Number)?.toDouble() ?: 0.0,
                                    unit = vMap["unit"] as? String ?: ""
                                )
                            }

                            // Compare with local db to see if there is any difference.
                            val localSlide = repository.getSlideById(formulaId).firstOrNull()
                            if (localSlide == null || 
                                localSlide.slide.title != title ||
                                localSlide.slide.category != category ||
                                localSlide.slide.formula != formulaExpr ||
                                localSlide.slide.resultUnit != outputUnit ||
                                localSlide.slide.description != description ||
                                localSlide.variables.size != parsedVars.size ||
                                localSlide.variables.any { lv -> 
                                    val matching = parsedVars.find { it.symbol == lv.symbol }
                                    matching == null || matching.name != lv.name || matching.unit != lv.unit
                                }
                            ) {
                                // There is a change! Let's update local DB.
                                val slideObj = Slide(
                                    id = formulaId,
                                    title = title,
                                    formula = formulaExpr,
                                    resultUnit = outputUnit,
                                    description = description,
                                    category = category
                                )
                                repository.updateSlideAndVariables(slideObj, parsedVars)
                                changed = true
                            }
                        }

                        // Delete any local formula that doesn't exist in Firestore
                        val localSlides = allSlides.value
                        for (local in localSlides) {
                            if (!firestoreFormulaIds.contains(local.slide.id)) {
                                repository.deleteSlide(local.slide.id)
                                changed = true
                            }
                        }

                        _syncStatus.value = "Synced"

                        if (changed) {
                            if (!_isAdminLoggedIn.value) {
                                _formulaCloudUpdateMessage.value = "Formula updated from cloud"
                                showToast("Formula updated from cloud")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("RealtimeFormula", "Error parsing / updating formulas from Firestore snapshot", e)
                        _syncStatus.value = "Error parsing"
                    }
                }
            }
    }

    private suspend fun uploadAllFormulasToFirestoreQuietly() {
        val firestore = FirebaseService.getFirestore() ?: return
        val localSlides = allSlides.value
        val batch = firestore.batch()
        for (swv in localSlides) {
            val docRef = firestore.collection("formulas").document(swv.slide.id.toString())
            val docData = mapOf(
                "formulaId" to swv.slide.id,
                "title" to swv.slide.title,
                "category" to swv.slide.category,
                "inputLabels" to swv.variables.map { it.name },
                "inputUnits" to swv.variables.map { it.unit },
                "outputLabel" to swv.slide.title,
                "outputUnit" to swv.slide.resultUnit,
                "formulaExpression" to swv.slide.formula,
                "decimalPrecision" to decimalPlaces.value,
                "visibilityStatus" to true,
                "sortOrder" to swv.slide.id,
                "createdBy" to "admin",
                "updatedBy" to "admin",
                "createdAt" to System.currentTimeMillis(),
                "updatedAt" to System.currentTimeMillis(),
                "variables" to swv.variables.map { v ->
                    mapOf(
                        "symbol" to v.symbol,
                        "name" to v.name,
                        "value" to v.value,
                        "unit" to v.unit
                    )
                }
            )
            batch.set(docRef, docData)
        }
        batch.commit().addOnSuccessListener {
            Log.i("RealtimeFormula", "Cloud database pre-populated successfully with ${localSlides.size} templates.")
        }.addOnFailureListener { e ->
            Log.e("RealtimeFormula", "Failed to pre-populate cloud database", e)
        }
    }

    fun saveNewFormulaToFirestoreDirect(slide: Slide, variables: List<Variable>) {
        if (!_isAdminLoggedIn.value) return
        val firestore = FirebaseService.getFirestore() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val docRef = firestore.collection("formulas").document(slide.id.toString())
            val docData = mapOf(
                "formulaId" to slide.id,
                "title" to slide.title,
                "category" to slide.category,
                "inputLabels" to variables.map { it.name },
                "inputUnits" to variables.map { it.unit },
                "outputLabel" to slide.title,
                "outputUnit" to slide.resultUnit,
                "formulaExpression" to slide.formula,
                "decimalPrecision" to decimalPlaces.value,
                "visibilityStatus" to true,
                "sortOrder" to slide.id,
                "createdBy" to "admin",
                "updatedBy" to "admin",
                "createdAt" to System.currentTimeMillis(),
                "updatedAt" to System.currentTimeMillis(),
                "variables" to variables.map { v ->
                    mapOf(
                        "symbol" to v.symbol,
                        "name" to v.name,
                        "value" to v.value,
                        "unit" to v.unit
                    )
                }
            )
            docRef.set(docData)
                .addOnSuccessListener {
                    Log.d("RealtimeFormula", "New Formula #${slide.id} successfully saved to Firestore.")
                    showToast("Saved to Firebase")
                    _formulaSaveStatus.value = FormulaOperationStatus.Success
                }
                .addOnFailureListener { e ->
                    Log.e("RealtimeFormula", "Failed to save new formula #${slide.id} to Firestore", e)
                    _formulaSaveStatus.value = FormulaOperationStatus.Error(e.localizedMessage ?: "Unknown Firestore Error")
                }
        }
    }

    fun saveFormulaToFirestoreDirect(slideId: Int) {
        if (!_isAdminLoggedIn.value) return
        val firestore = FirebaseService.getFirestore() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val swv = allSlides.value.find { it.slide.id == slideId } ?: return@launch
            val docRef = firestore.collection("formulas").document(slideId.toString())
            val docData = mapOf(
                "formulaId" to swv.slide.id,
                "title" to swv.slide.title,
                "category" to swv.slide.category,
                "inputLabels" to swv.variables.map { it.name },
                "inputUnits" to swv.variables.map { it.unit },
                "outputLabel" to swv.slide.title,
                "outputUnit" to swv.slide.resultUnit,
                "formulaExpression" to swv.slide.formula,
                "decimalPrecision" to decimalPlaces.value,
                "visibilityStatus" to true,
                "sortOrder" to swv.slide.id,
                "createdBy" to "admin",
                "updatedBy" to "admin",
                "createdAt" to System.currentTimeMillis(),
                "updatedAt" to System.currentTimeMillis(),
                "variables" to swv.variables.map { v ->
                    mapOf(
                        "symbol" to v.symbol,
                        "name" to v.name,
                        "value" to v.value,
                        "unit" to v.unit
                    )
                }
            )
            docRef.set(docData)
                .addOnSuccessListener {
                    Log.d("RealtimeFormula", "Formula #$slideId successfully saved to Firestore.")
                    showToast("Saved to Firebase")
                    _formulaSaveStatus.value = FormulaOperationStatus.Success
                }
                .addOnFailureListener { e ->
                    Log.e("RealtimeFormula", "Failed to save formula #$slideId to Firestore", e)
                    _formulaSaveStatus.value = FormulaOperationStatus.Error(e.localizedMessage ?: "Unknown Firestore Error")
                }
        }
    }

    fun deleteFormulaFromFirestoreDirect(slideId: Int) {
        if (!_isAdminLoggedIn.value) return
        val firestore = FirebaseService.getFirestore() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            firestore.collection("formulas").document(slideId.toString())
                .delete()
                .addOnSuccessListener {
                    Log.d("RealtimeFormula", "Formula #$slideId successfully deleted from Firestore.")
                    showToast("Saved to Firebase")
                }
                .addOnFailureListener { e ->
                    Log.e("RealtimeFormula", "Failed to delete formula #$slideId from Firestore", e)
                }
        }
    }

    fun resetFirestoreFormulasToDefaults() {
        if (!_isAdminLoggedIn.value) return
        val firestore = FirebaseService.getFirestore() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            firestore.collection("formulas").get()
                .addOnSuccessListener { snapshot ->
                    val batch = firestore.batch()
                    for (doc in snapshot.documents) {
                        batch.delete(doc.reference)
                    }
                    batch.commit().addOnSuccessListener {
                        viewModelScope.launch(Dispatchers.IO) {
                            uploadAllFormulasToFirestoreQuietly()
                        }
                        showToast("Saved to Firebase")
                    }
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        formulaListenerRegistration?.remove()
    }

    fun logCurrentCalculationToFirestore(slideTitle: String, formula: String, inputs: String, result: String) {
        val uid = _firebaseUserUid.value
        val config = _firebaseConfig.value
        if (uid != null) {
            if (config.databaseUrl.isNotEmpty()) {
                FirebaseService.saveCalculationHistoryToRealtimeDatabase(uid, config.databaseUrl, slideTitle, formula, inputs, result)
            } else {
                FirebaseService.saveCalculationHistory(uid, slideTitle, formula, inputs, result)
            }
        }
    }

    private fun saveFirebaseConfigToSharedPrefs(config: FirebaseConfig) {
        val sp = getApplication<Application>().getSharedPreferences("firebase_prefs", Context.MODE_PRIVATE)
        sp.edit()
            .putBoolean("enabled", config.isEnabled)
            .putBoolean("auto_sync", config.autoSync)
            .putString("api_key", config.apiKey)
            .putString("application_id", config.applicationId)
            .putString("project_id", config.projectId)
            .putString("database_url", config.databaseUrl)
            .apply()
    }

    private fun loadFirebaseConfigFromSharedPrefs() {
        val sp = getApplication<Application>().getSharedPreferences("firebase_prefs", Context.MODE_PRIVATE)
        _lastBackupTime.value = sp.getString("last_backup_time", "Never") ?: "Never"
        val config = FirebaseConfig(
            isEnabled = sp.getBoolean("enabled", true),
            autoSync = sp.getBoolean("auto_sync", true),
            apiKey = sp.getString("api_key", "") ?: "",
            applicationId = sp.getString("application_id", "") ?: "",
            projectId = sp.getString("project_id", "") ?: "",
            databaseUrl = sp.getString("database_url", "") ?: ""
        )
        _firebaseConfig.value = config
        if (config.isEnabled) {
            val initialized = FirebaseService.initialize(getApplication(), config)
            if (initialized) {
                _firebaseAuthStatus.value = "Firebase Connected"
                _syncStatus.value = "Connecting..."
                startRealtimeFormulaListener()
                // Only sign in anonymously if no actual user is logged in
                if (!_isUserLoggedIn.value && !_isAdminLoggedIn.value) {
                    signInFirebaseAnonymously { success ->
                        if (success) {
                            runFirestoreTest()
                        }
                    }
                } else {
                    runFirestoreTest()
                }
            } else {
                _syncStatus.value = "Error: GOOGLE_SERVICES_JSON_ERROR"
                _firebaseAuthStatus.value = "Cloud sync unavailable. Using offline mode."
            }
        } else {
            _syncStatus.value = "Offline"
        }
    }

    // --- SECURE AUTHENTICATION & SESSION MANAGEMENT SYSTEM ---

    private fun saveUserSession(userId: String, name: String, email: String) {
        val sp = getApplication<Application>().getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        sp.edit()
            .putBoolean("user_logged_in", true)
            .putString("user_id", userId)
            .putString("user_name", name)
            .putString("user_email", email)
            .apply()
        
        _isUserLoggedIn.value = true
        _loggedInUserId.value = userId
        _loggedInName.value = name
        _loggedInEmail.value = email
    }

    private fun loadUserSession() {
        val sp = getApplication<Application>().getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val isLoggedIn = sp.getBoolean("user_logged_in", false)
        if (isLoggedIn) {
            _isUserLoggedIn.value = true
            _loggedInUserId.value = sp.getString("user_id", "")
            _loggedInName.value = sp.getString("user_name", "")
            _loggedInEmail.value = sp.getString("user_email", "")
        }
    }

    fun saveAdminSession() {
        val sp = getApplication<Application>().getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        sp.edit()
            .putBoolean("admin_logged_in", true)
            .apply()

        _isAdminLoggedIn.value = true
        _isAdminMode.value = true
    }

    private fun loadAdminSession() {
        val sp = getApplication<Application>().getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val adminActive = sp.getBoolean("admin_logged_in", false)
        if (adminActive) {
            _isAdminLoggedIn.value = true
            _isAdminMode.value = true
        }
    }

    fun clearAuthSession() {
        val sp = getApplication<Application>().getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val currentUid = _loggedInUserId.value ?: sp.getString("user_id", null) ?: FirebaseService.getAuth()?.currentUser?.uid
        val currentEmail = _loggedInEmail.value ?: sp.getString("user_email", null) ?: FirebaseService.getAuth()?.currentUser?.email ?: "guest_user"

        sp.edit().clear().apply()

        _isUserLoggedIn.value = false
        _isAdminLoggedIn.value = false
        _loggedInUserId.value = null
        _loggedInName.value = null
        _loggedInEmail.value = null
        _isAdminMode.value = false
        _authError.value = null
        
        if (currentUid != null) {
            syncLogoutStatus(currentUid, currentEmail) {
                logAction("FIREBASE_LOGOUT", "Logged out and cleared session for UID: $currentUid")
            }
        } else {
            try {
                FirebaseService.getAuth()?.signOut()
            } catch (e: Exception) {
                logAction("FIREBASE_SIGNOUT_ERROR", "Error signing out: ${e.localizedMessage}")
            }
        }
    }

    fun loginAsAdminWithPassword(password: String, onFinished: (Boolean, String) -> Unit) {
        _isAuthenticating.value = true
        _authError.value = null
        
        if (password == "4735") {
            saveAdminSession()
            _isAuthenticating.value = false
            onFinished(true, "Successfully authorized as System Administrator")
        } else {
            _isAuthenticating.value = false
            val err = "Incorrect admin passcode. Access Denied."
            _authError.value = err
            onFinished(false, err)
        }
    }

    fun signUpUser(
        email: String,
        passwordRaw: String,
        passwordConfirm: String,
        onFinished: (Boolean, String) -> Unit
    ) {
        _isUserLoggedIn.value = false
        _isAuthenticating.value = true
        _authError.value = null

        // 1. Validate email format
        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _isAuthenticating.value = false
            val err = "Invalid email address format."
            _authError.value = err
            onFinished(false, err)
            return
        }

        // 2. Validate passwords match
        if (passwordRaw != passwordConfirm) {
            _isAuthenticating.value = false
            val err = "Passwords do not match."
            _authError.value = err
            onFinished(false, err)
            return
        }

        // 3. Password minimum 6 characters
        if (passwordRaw.length < 6) {
            _isAuthenticating.value = false
            val err = "Password must be at least 6 characters."
            _authError.value = err
            onFinished(false, err)
            return
        }

        val auth = FirebaseService.getAuth()
        if (auth != null) {
            auth.createUserWithEmailAndPassword(email, passwordRaw)
                .addOnSuccessListener { authResult ->
                    val uid = authResult.user?.uid
                    if (uid != null) {
                        // 1. Confirm Firebase Auth account created
                        Log.d("AUTH_SUCCESS", "Firebase Auth account created for email: $email, UID: $uid")
                        showToast("Firebase Auth: Account Created!")

                        val now = ZonedDateTime.now()
                        val trialStartStr = now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                        val trialEndStr = now.plusDays(30).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                        val createdAtStr = now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

                        val userProfileMap = mapOf(
                            "uid" to uid,
                            "email" to email,
                            "role" to "user",
                            "trialStartDate" to trialStartStr,
                            "trialEndDate" to trialEndStr,
                            "subscriptionStatus" to "trial",
                            "accountType" to "firebase_user",
                            "createdAt" to createdAtStr,
                            "isActive" to true
                        )

                        // 2. Try saving to Firestore
                        val firestore = FirebaseService.getFirestore()
                        if (firestore != null) {
                            firestore.collection("users")
                                .document(uid)
                                .set(userProfileMap)
                                .addOnSuccessListener {
                                    // Firestore Profile Save succeeded
                                    Log.i("FIRESTORE_PROFILE_SAVE_SUCCESS", "Firestore user profile users/$uid saved successfully.")
                                    showToast("Firestore: Profile saved! Setup is production-ready.")

                                    _isUserLoggedIn.value = true
                                    _isAuthenticating.value = false
                                    viewModelScope.launch {
                                        try {
                                            val profileObj = UserProfile(
                                                uid = uid,
                                                email = email,
                                                role = "user",
                                                trialStartDate = trialStartStr,
                                                trialEndDate = trialEndStr,
                                                subscriptionStatus = "trial",
                                                accountType = "firebase_user",
                                                createdAt = createdAtStr,
                                                isActive = true,
                                                syncStatus = "synced"
                                            )
                                            repository.insertUserProfile(profileObj)
                                            // 3. Room Profiles Cache Success
                                            Log.d("ROOM_PROFILE_CACHE_SUCCESS", "Local Room cache updated with synced status for UID: $uid")
                                            showToast("Room Local: Profile cached successfully.")
                                        } catch (dbEx: Exception) {
                                            // 3. Room Profiles Cache Failed
                                            Log.e("ROOM_PROFILE_CACHE_FAILED", "Failed to cache user profile in Room: ${dbEx.message}")
                                            showToast("Room Local: Profile cache failed.")
                                        }
                                        saveUserSession(uid, "Operator", email)
                                        onFinished(true, "Signup complete. Account is production-ready!")
                                    }
                                }
                                .addOnFailureListener { e ->
                                    // Firestore Profile Save failed
                                    Log.e("FIRESTORE_PROFILE_SAVE_FAILED", "Failed to write user profile to Firestore: ${e.message}")
                                    showToast("Firestore: Profile failed to save. Synchronizing in background...")

                                    _isUserLoggedIn.value = true
                                    _isAuthenticating.value = false
                                    viewModelScope.launch {
                                        try {
                                            val profileObj = UserProfile(
                                                uid = uid,
                                                email = email,
                                                role = "user",
                                                trialStartDate = trialStartStr,
                                                trialEndDate = trialEndStr,
                                                subscriptionStatus = "trial",
                                                accountType = "firebase_user",
                                                createdAt = createdAtStr,
                                                isActive = true,
                                                syncStatus = "pending"
                                            )
                                            repository.insertUserProfile(profileObj)
                                            // 3. Room Profiles Cache Success
                                            Log.d("ROOM_PROFILE_CACHE_SUCCESS", "Local Room cache saved with pending status for UID: $uid")
                                            showToast("Room Local: Profile cached as pending.")
                                        } catch (dbEx: Exception) {
                                            // 3. Room Profiles Cache Failed
                                            Log.e("ROOM_PROFILE_CACHE_FAILED", "Failed to cache user profile in Room: ${dbEx.message}")
                                            showToast("Room Local: Profile cache failed.")
                                        }
                                        saveUserSession(uid, "Operator", email)
                                        onFinished(true, "Signup completed using fallback offline database cache.")
                                    }
                                }
                        } else {
                            // Firestore unavailable/null
                            Log.e("FIRESTORE_PROFILE_SAVE_FAILED", "Failed to write user profile to Firestore: Firestore is uninitialized.")
                            showToast("Firestore: Profile failed to save. Synchronizing in background...")

                            _isUserLoggedIn.value = true
                            _isAuthenticating.value = false
                            viewModelScope.launch {
                                try {
                                    val profileObj = UserProfile(
                                        uid = uid,
                                        email = email,
                                        role = "user",
                                        trialStartDate = trialStartStr,
                                        trialEndDate = trialEndStr,
                                        subscriptionStatus = "trial",
                                        accountType = "firebase_user",
                                        createdAt = createdAtStr,
                                        isActive = true,
                                        syncStatus = "pending"
                                    )
                                    repository.insertUserProfile(profileObj)
                                    // 3. Room Profiles Cache Success
                                    Log.d("ROOM_PROFILE_CACHE_SUCCESS", "Local Room cache saved with pending status for UID: $uid")
                                    showToast("Room Local: Profile cached as pending.")
                                } catch (dbEx: Exception) {
                                    // 3. Room Profiles Cache Failed
                                    Log.e("ROOM_PROFILE_CACHE_FAILED", "Failed to cache user profile in Room: ${dbEx.message}")
                                    showToast("Room Local: Profile cache failed.")
                                }
                                saveUserSession(uid, "Operator", email)
                                onFinished(true, "Signup completed using fallback offline database cache.")
                            }
                        }
                    } else {
                        _isAuthenticating.value = false
                        val err = "Authentication succeeded but UID was null."
                        _authError.value = err
                        onFinished(false, err)
                    }
                }
                .addOnFailureListener { e ->
                    _isAuthenticating.value = false
                    val friendlyMsg = when {
                        e.localizedMessage?.contains("already in use", ignoreCase = true) == true ->
                            "This email address is already registered!"
                        else -> e.localizedMessage ?: "Registration failed"
                    }
                    _authError.value = friendlyMsg
                    onFinished(false, friendlyMsg)
                }
        } else {
            // When Firebase is NOT initialized / NOT available, fallback to offline sandbox database
            val sp = getApplication<Application>().getSharedPreferences("offline_sandbox_users", Context.MODE_PRIVATE)
            val existingEmails = sp.getStringSet("registered_emails", mutableSetOf()) ?: mutableSetOf()
            
            if (existingEmails.contains(email)) {
                _isAuthenticating.value = false
                val err = "This email address is already registered!"
                _authError.value = err
                onFinished(false, err)
            } else {
                val newEmails = existingEmails.toMutableSet().apply { add(email) }
                
                sp.edit()
                    .putStringSet("registered_emails", newEmails)
                    .putString("profile_${email}_password", passwordRaw)
                    .apply()
                    
                _isAuthenticating.value = false
                saveUserSession(email, "Operator", email)
                onFinished(true, "Successfully registered in Offline Sandbox Mode!")
            }
        }
    }

    fun signInWithCredentials(
        emailOrUserId: String,
        passwordRaw: String,
        onFinished: (Boolean, String) -> Unit
    ) {
        _isAuthenticating.value = true
        _authError.value = null
        
        viewModelScope.launch {
            val isFirebaseReady = FirebaseService.isInitialized
            if (isFirebaseReady) {
                if (emailOrUserId.contains("@")) {
                    FirebaseService.loginWithEmail(emailOrUserId, passwordRaw) { success, uid ->
                        if (success && uid != null) {
                            syncLoginStatus(uid, emailOrUserId) {
                                FirebaseService.fetchUserProfile(uid) { profSuccess, data, errorMsg ->
                                    if (profSuccess && data != null) {
                                        _isAuthenticating.value = false
                                        val userId = data["uid"] as? String ?: data["userId"] as? String ?: uid
                                        val name = data["name"] as? String ?: "Operator"
                                        val email = data["email"] as? String ?: emailOrUserId
                                        saveUserSession(userId, name, email)
                                        onFinished(true, "Logged in successfully!")
                                    } else {
                                        // Fallback to local Room database profile
                                        viewModelScope.launch {
                                            val localProfile = repository.getUserProfile(uid)
                                            _isAuthenticating.value = false
                                            if (localProfile != null) {
                                                saveUserSession(localProfile.uid, "Operator", localProfile.email)
                                                onFinished(true, "Logged in successfully!")
                                            } else {
                                                saveUserSession(uid, "Operator User", emailOrUserId)
                                                onFinished(true, "Welcome operator!")
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            _isAuthenticating.value = false
                            _authError.value = uid
                            onFinished(false, uid ?: "Incorrect credentials")
                        }
                    }
                } else {
                    FirebaseService.findEmailByUserId(emailOrUserId) { success, email ->
                        if (success && email != null) {
                            FirebaseService.loginWithEmail(email, passwordRaw) { loginSuccess, uid ->
                                if (loginSuccess && uid != null) {
                                    syncLoginStatus(uid, email) {
                                        FirebaseService.fetchUserProfile(uid) { profSuccess, data, errorMsg ->
                                            if (profSuccess && data != null) {
                                                _isAuthenticating.value = false
                                                val userId = data["uid"] as? String ?: data["userId"] as? String ?: uid
                                                val name = data["name"] as? String ?: "Operator"
                                                val resolvedEmail = data["email"] as? String ?: email
                                                saveUserSession(userId, name, resolvedEmail)
                                                onFinished(true, "Logged in successfully!")
                                            } else {
                                                // Fallback to local Room database profile
                                                viewModelScope.launch {
                                                    val localProfile = repository.getUserProfile(uid)
                                                    _isAuthenticating.value = false
                                                    if (localProfile != null) {
                                                        saveUserSession(localProfile.uid, "Operator", localProfile.email)
                                                        onFinished(true, "Logged in successfully!")
                                                    } else {
                                                        saveUserSession(uid, "Operator User", email)
                                                        onFinished(true, "Logged in successfully!")
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    _isAuthenticating.value = false
                                    _authError.value = uid
                                    onFinished(false, uid ?: "Incorrect credentials")
                                }
                            }
                        } else {
                            _isAuthenticating.value = false
                            val err = email ?: "User ID not registered"
                            _authError.value = err
                            onFinished(false, err)
                        }
                    }
                }
            } else {
                val sp = getApplication<Application>().getSharedPreferences("offline_sandbox_users", Context.MODE_PRIVATE)
                val resolvedEmail = if (emailOrUserId.contains("@")) {
                    emailOrUserId
                } else {
                    sp.getString("profile_id_${emailOrUserId}_email", "") ?: ""
                }
                
                if (resolvedEmail.isBlank()) {
                    _isAuthenticating.value = false
                    val err = "No account found with this email/User ID."
                    _authError.value = err
                    onFinished(false, err)
                } else {
                    val savedPass = sp.getString("profile_${resolvedEmail}_password", "")
                    if (savedPass == passwordRaw) {
                        val userId = sp.getString("profile_${resolvedEmail}_userId", "User ID") ?: "User ID"
                        val name = sp.getString("profile_${resolvedEmail}_name", "Operator User") ?: "Operator User"
                        
                        _isAuthenticating.value = false
                        saveUserSession(userId, name, resolvedEmail)
                        onFinished(true, "Logged in successfully as operator (Sandbox)!")
                    } else {
                        _isAuthenticating.value = false
                        val err = "Incorrect password. Please try again."
                        _authError.value = err
                        onFinished(false, err)
                    }
                }
            }
        }
    }

    fun lookupUserId(email: String, onFinished: (Boolean, String) -> Unit) {
        _isAuthenticating.value = true
        _authError.value = null
        
        viewModelScope.launch {
            if (FirebaseService.isInitialized) {
                FirebaseService.findUserIdByEmail(email) { success, result ->
                    _isAuthenticating.value = false
                    if (success && result != null) {
                        onFinished(true, "Your User ID is: $result")
                    } else {
                        _authError.value = result
                        onFinished(false, result ?: "Search failed")
                    }
                }
            } else {
                val sp = getApplication<Application>().getSharedPreferences("offline_sandbox_users", Context.MODE_PRIVATE)
                val userId = sp.getString("profile_${email}_userId", "") ?: ""
                _isAuthenticating.value = false
                if (userId.isNotEmpty()) {
                    onFinished(true, "Your User ID is: $userId")
                } else {
                    val err = "No registered User ID found for this email address."
                    _authError.value = err
                    onFinished(false, err)
                }
            }
        }
    }

    fun sendPasswordReset(email: String, onFinished: (Boolean, String) -> Unit) {
        _isAuthenticating.value = true
        _authError.value = null
        
        viewModelScope.launch {
            if (FirebaseService.isInitialized) {
                FirebaseService.sendPasswordReset(email) { success, result ->
                    _isAuthenticating.value = false
                    if (success) {
                        onFinished(true, "Password reset link sent to your email.")
                    } else {
                        _authError.value = result
                        onFinished(false, result ?: "Failed to send reset email")
                    }
                }
            } else {
                _isAuthenticating.value = false
                val sp = getApplication<Application>().getSharedPreferences("offline_sandbox_users", Context.MODE_PRIVATE)
                val userId = sp.getString("profile_${email}_userId", "") ?: ""
                if (userId.isNotEmpty()) {
                    onFinished(true, "Password reset link simulated! Sent to $email.")
                } else {
                    val err = "This email is not registered in sandbox."
                    _authError.value = err
                    onFinished(false, err)
                }
            }
        }
    }
}

class CalculatorViewModelFactory(
    private val repository: CalculatorRepository,
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CalculatorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CalculatorViewModel(repository, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
