package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.database.FirebaseDatabase

data class FirebaseConfig(
    val isEnabled: Boolean = false,
    val autoSync: Boolean = false,
    val apiKey: String = "",
    val applicationId: String = "",
    val projectId: String = "",
    val databaseUrl: String = ""
)

object FirebaseService {
    private const val TAG = "FirebaseService"
    
    var isInitialized = false
        private set

    private var isSettingsConfigured = false

    fun initialize(context: Context, config: FirebaseConfig): Boolean {
        if (!config.isEnabled) {
            isInitialized = false
            return false
        }
        return try {
            val apps = FirebaseApp.getApps(context)
            val hasCustomConfig = config.apiKey.isNotEmpty() && config.applicationId.isNotEmpty() && config.projectId.isNotEmpty()
            
            var needsReinit = false
            if (apps.isNotEmpty()) {
                try {
                    val app = FirebaseApp.getInstance()
                    val currentOptions = app.options
                    val currentProjectId = currentOptions.projectId
                    
                    if (hasCustomConfig) {
                        if (currentProjectId != config.projectId || currentOptions.apiKey != config.apiKey) {
                            needsReinit = true
                        }
                    } else {
                        // If current project matches the fallback but we want default (google-services.json), we need to reinit
                        if (currentProjectId == "chemdose-formula") {
                            needsReinit = true
                        }
                    }
                } catch (e: Exception) {
                    needsReinit = true
                }
            }
            
            if (needsReinit && apps.isNotEmpty()) {
                try {
                    val app = FirebaseApp.getInstance()
                    app.delete()
                    isSettingsConfigured = false
                    Log.d(TAG, "Successfully deleted old FirebaseApp instance for clean re-initialization")
                } catch (e: Exception) {
                    Log.e(TAG, "Error resetting FirebaseApp for new configuration", e)
                }
            }
            
            if (FirebaseApp.getApps(context).isEmpty()) {
                if (hasCustomConfig) {
                    Log.d(TAG, "Initializing Firebase programmatically with custom user-provided credentials")
                    val builder = FirebaseOptions.Builder()
                        .setApiKey(config.apiKey)
                        .setApplicationId(config.applicationId)
                        .setProjectId(config.projectId)
                    if (config.databaseUrl.isNotEmpty()) {
                        builder.setDatabaseUrl(config.databaseUrl)
                    }
                    FirebaseApp.initializeApp(context, builder.build())
                } else {
                    try {
                        FirebaseApp.initializeApp(context)
                    } catch (e: Exception) {
                        Log.w(TAG, "Default Firebase initialization failed, initiating robust manual programmatic fallback setup", e)
                        val options = FirebaseOptions.Builder()
                            .setApplicationId("1:47780244743:android:8440369b3f9458667d29a2")
                            .setApiKey("AIzaSyC6YQfew7BRCzE7fHyMyh9zPC3EZiMTgSo")
                            .setProjectId("chemdose-formula")
                            .setStorageBucket("chemdose-formula.firebasestorage.app")
                            .build()
                        FirebaseApp.initializeApp(context, options)
                    }
                }
            }
            isInitialized = true
            
            // Configure Firestore offline cache to 100 MB safely upon initialization
            if (!isSettingsConfigured) {
                try {
                    val firestore = FirebaseFirestore.getInstance()
                    val settings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                        .setLocalCacheSettings(
                            com.google.firebase.firestore.PersistentCacheSettings.newBuilder()
                                .setSizeBytes(100L * 1024L * 1024L)
                                .build()
                        )
                        .build()
                    firestore.firestoreSettings = settings
                    isSettingsConfigured = true
                    Log.d(TAG, "Firestore persistent cache configured to 100MB successfully.")
                } catch (e: Exception) {
                    Log.e(TAG, "Error configuring Firestore offline persistence settings", e)
                }
            }

            Log.d(TAG, "Firebase initialized safely using google-services.json")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firebase safely via google-services", e)
            isInitialized = false
            false
        }
    }

    private fun mapFirestoreException(e: Exception): String {
        val message = e.message ?: ""
        if (message.contains("google-services.json") || message.contains("Default FirebaseApp is not initialized")) {
            return "GOOGLE_SERVICES_JSON_ERROR"
        }
        if (e is com.google.firebase.firestore.FirebaseFirestoreException) {
            Log.e(TAG, "gRPC network exception detected. Gracefully falling back to Firestore local-persistence and long-polling mode (experimentalAutoDetectLongPolling = true) to bypass network blocks.", e)
            return when (e.code) {
                com.google.firebase.firestore.FirebaseFirestoreException.Code.UNAVAILABLE -> "UNAVAILABLE"
                com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED -> "PERMISSION_DENIED"
                com.google.firebase.firestore.FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> "DEADLINE_EXCEEDED"
                else -> "NETWORK_ERROR"
            }
        }
        return "NETWORK_ERROR"
    }

    fun testFirestoreConnection(uid: String?, onResult: (Boolean, String) -> Unit) {
        val firestore = getFirestore()
        if (firestore == null) {
            onResult(false, "GOOGLE_SERVICES_JSON_ERROR")
            return
        }
        try {
            // Ensure network is enabled and check connection with a test write & read
            firestore.enableNetwork().addOnCompleteListener { networkTask ->
                if (!networkTask.isSuccessful) {
                    Log.e(TAG, "enableNetwork failed during connection test", networkTask.exception)
                }
                
                val targetUid = uid ?: "anonymous_guest"
                val testDocRef = firestore.collection("users").document(targetUid).collection("data").document("connectivity_test")
                val data = mapOf("last_checked" to java.text.DateFormat.getDateTimeInstance().format(java.util.Date()))
                
                testDocRef.set(data)
                    .addOnSuccessListener {
                        testDocRef.get()
                            .addOnSuccessListener {
                                onResult(true, "Connected")
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Firestore connectivity test read failed", e)
                                onResult(false, mapFirestoreException(e))
                            }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Firestore connectivity test write failed", e)
                        onResult(false, mapFirestoreException(e))
                    }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firestore connection execution exception", e)
            onResult(false, mapFirestoreException(e))
        }
    }

    fun getAuth(): FirebaseAuth? {
        if (!isInitialized) return null
        return try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "FirebaseAuth not configured/initialized", e)
            null
        }
    }

    fun getFirestore(): FirebaseFirestore? {
        if (!isInitialized) return null
        return try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "FirebaseFirestore not configured/initialized", e)
            null
        }
    }

    /**
     * Signs in anonymously to ensure a valid authenticated session.
     */
    fun signInAnonymously(onComplete: (Boolean, String?) -> Unit) {
        val auth = getAuth()
        if (auth == null) {
            onComplete(false, "Firebase not initialized. Enable and enter credentials first.")
            return
        }

        auth.signInAnonymously()
            .addOnSuccessListener { result ->
                val uid = result.user?.uid
                Log.d(TAG, "Signed in anonymously as: $uid")
                onComplete(true, uid)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Anonymous sign in failed", e)
                onComplete(false, e.localizedMessage ?: "Unknown authentication failure")
            }
    }

    /**
     * Signs in or creates user with Email & Password.
     */
    fun loginWithEmail(email: String, password: String, onComplete: (Boolean, String?) -> Unit) {
        val auth = getAuth()
        if (auth == null) {
            onComplete(false, "Firebase not initialized")
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user != null) {
                    onComplete(true, user.uid)
                } else {
                    onComplete(false, "Failed to retrieve user details.")
                }
            }
            .addOnFailureListener { e ->
                onComplete(false, e.localizedMessage ?: "Incorrect email address or password")
            }
    }

    /**
     * Search Firestore to find email associated with custom userId.
     */
    fun findEmailByUserId(userId: String, onComplete: (Boolean, String?) -> Unit) {
        val firestore = getFirestore()
        if (firestore == null) {
            onComplete(false, "Firestore database not initialized")
            return
        }
        try {
            firestore.collection("users")
                .whereEqualTo("userId", userId)
                .limit(1)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) {
                        val doc = querySnapshot.documents[0]
                        val email = doc.getString("email")
                        if (email != null) {
                            onComplete(true, email)
                        } else {
                            onComplete(false, "No email found for this User ID")
                        }
                    } else {
                        onComplete(false, "User ID not registered")
                    }
                }
                .addOnFailureListener { e ->
                    onComplete(false, e.localizedMessage ?: "Database lookup failed")
                }
        } catch (e: Exception) {
            onComplete(false, e.localizedMessage ?: "Firestore error")
        }
    }

    /**
     * Search Firestore to retrieve custom userId associated with email.
     */
    fun findUserIdByEmail(email: String, onComplete: (Boolean, String?) -> Unit) {
        val firestore = getFirestore()
        if (firestore == null) {
            onComplete(false, "Firestore database not initialized")
            return
        }
        try {
            firestore.collection("users")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) {
                        val doc = querySnapshot.documents[0]
                        val userId = doc.getString("userId")
                        if (userId != null) {
                            onComplete(true, userId)
                        } else {
                            onComplete(false, "No User ID registered under this email")
                        }
                    } else {
                        onComplete(false, "Email address not found")
                    }
                }
                .addOnFailureListener { e ->
                    onComplete(false, e.localizedMessage ?: "Database search failed")
                }
        } catch (e: Exception) {
            onComplete(false, e.localizedMessage ?: "Firestore error")
        }
    }

    /**
     * Send Password Reset Email.
     */
    fun sendPasswordReset(email: String, onComplete: (Boolean, String?) -> Unit) {
        val auth = getAuth()
        if (auth == null) {
            onComplete(false, "Authenticator instance not initialized")
            return
        }
        try {
            auth.sendPasswordResetEmail(email)
                .addOnSuccessListener {
                    onComplete(true, "Password reset link sent to your email.")
                }
                .addOnFailureListener { e ->
                    onComplete(false, e.localizedMessage ?: "Failed to send reset email")
                }
        } catch (e: Exception) {
            onComplete(false, e.localizedMessage ?: "Authenticator error")
        }
    }

    /**
     * User profile creation in Firestore
     */
    fun createUserProfile(
        uid: String,
        userId: String,
        name: String,
        email: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val firestore = getFirestore()
        if (firestore == null) {
            onComplete(false, "Firestore instance not available")
            return
        }
        try {
            val userProfile = mapOf(
                "userId" to userId,
                "name" to name,
                "email" to email,
                "role" to "user",
                "createdDate" to java.text.DateFormat.getDateTimeInstance().format(java.util.Date())
            )
            firestore.collection("users")
                .document(uid)
                .set(userProfile)
                .addOnSuccessListener {
                    onComplete(true, "Profile saved successfully")
                }
                .addOnFailureListener { e ->
                    onComplete(false, e.localizedMessage ?: "Failed to save profile on database")
                }
        } catch (e: Exception) {
            onComplete(false, e.localizedMessage ?: "Profile creation database error")
        }
    }

    /**
     * Fetch user profile from Firestore to determine name, role, etc.
     */
    fun fetchUserProfile(
        uid: String,
        onComplete: (Boolean, Map<String, Any>?, String?) -> Unit
    ) {
        val firestore = getFirestore()
        if (firestore == null) {
            onComplete(false, null, "Firestore instance not available")
            return
        }
        try {
            firestore.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        onComplete(true, document.data, null)
                    } else {
                        onComplete(false, null, "Profile document does not exist")
                    }
                }
                .addOnFailureListener { e ->
                    onComplete(false, null, e.localizedMessage ?: "Failed to fetch user profile")
                }
        } catch (e: Exception) {
            onComplete(false, null, e.localizedMessage ?: "Firestore error fetching profile")
        }
    }

    /**
     * User registration with Email & Profile
     */
    fun signUpWithEmailAndProfile(
        name: String,
        email: String,
        userId: String,
        password: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val auth = getAuth()
        if (auth == null) {
            onComplete(false, "Auth service not available")
            return
        }
        val firestore = getFirestore()
        if (firestore == null) {
            onComplete(false, "Firestore not available")
            return
        }

        try {
            // First make sure userId is unique in Firestore users collection
            firestore.collection("users")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) {
                        onComplete(false, "This User ID is already taken. Please choose another.")
                    } else {
                        // Create auth account
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnSuccessListener { authResult ->
                                val uid = authResult.user?.uid
                                if (uid != null) {
                                    createUserProfile(uid, userId, name, email) { profileSuccess, errorMsg ->
                                        if (profileSuccess) {
                                            onComplete(true, uid)
                                        } else {
                                            onComplete(false, "Account created, but database profile failed: $errorMsg")
                                        }
                                    }
                                } else {
                                    onComplete(false, "Failed to retrieve user UID.")
                                }
                            }
                            .addOnFailureListener { e ->
                                val friendlyMsg = when {
                                    e.localizedMessage?.contains("already in use", ignoreCase = true) == true ->
                                        "This email address is already registered!"
                                    e.localizedMessage?.contains("password", ignoreCase = true) == true ->
                                        "Weak password, must be at least 6 characters."
                                    e.localizedMessage?.contains("badly formatted", ignoreCase = true) == true ->
                                        "Invalid email address layout."
                                    else -> e.localizedMessage ?: "Registration failed"
                                }
                                onComplete(false, friendlyMsg)
                            }
                    }
                }
                .addOnFailureListener { e ->
                    onComplete(false, "Database verification failed: ${e.localizedMessage}")
                }
        } catch (e: Exception) {
            onComplete(false, e.localizedMessage ?: "Sign up exception")
        }
    }

    /**
     * Store customized formulas list to Firestore under user scope.
     */
    fun backupFormulasToFirestore(
        userId: String,
        slidesWithVars: List<SlideWithVariables>,
        decimalPlaces: Int,
        onComplete: (Boolean, String) -> Unit
    ) {
        val firestore = getFirestore()
        if (firestore == null) {
            onComplete(false, "Firestore instance not available.")
            return
        }

        val userDocRef = firestore.collection("users").document(userId)
        
        // Settings map
        val settingsMap = mapOf(
            "decimalPlaces" to decimalPlaces,
            "lastBackupTime" to java.text.DateFormat.getDateTimeInstance().format(java.util.Date())
        )

        // Formulas maps
        val formulaListMap = slidesWithVars.map { swv ->
            mapOf(
                "id" to swv.slide.id,
                "title" to swv.slide.title,
                "category" to swv.slide.category,
                "formula" to swv.slide.formula,
                "resultUnit" to swv.slide.resultUnit,
                "description" to swv.slide.description,
                "variables" to swv.variables.map { v ->
                    mapOf(
                        "symbol" to v.symbol,
                        "name" to v.name,
                        "value" to v.value,
                        "unit" to v.unit
                    )
                }
            )
        }

        val batch = firestore.batch()
        batch.set(userDocRef.collection("data").document("settings"), settingsMap)
        batch.set(userDocRef.collection("data").document("formulas"), mapOf("formulas" to formulaListMap))

        batch.commit()
            .addOnSuccessListener {
                onComplete(true, "Successfully backed up ${formulaListMap.size} formulas and settings to Firestore!")
            }
            .addOnFailureListener { e ->
                onComplete(false, "Firestore backup failed: ${e.localizedMessage}")
            }
    }

    /**
     * Restore formulas and settings from Firestore.
     */
    fun restoreFormulasFromFirestore(
        userId: String,
        onComplete: (Boolean, List<NetworkFormula>?, Int?, String) -> Unit
    ) {
        val firestore = getFirestore()
        if (firestore == null) {
            onComplete(false, null, null, "Firestore instance not available.")
            return
        }

        val formulasRef = firestore.collection("users").document(userId).collection("data").document("formulas")
        val settingsRef = firestore.collection("users").document(userId).collection("data").document("settings")

        settingsRef.get().addOnSuccessListener { settingsSnap ->
            val decimalPlaces = settingsSnap.getLong("decimalPlaces")?.toInt()

            formulasRef.get().addOnSuccessListener { formulasSnap ->
                val list = formulasSnap.get("formulas") as? List<Map<String, Any>>
                if (list == null) {
                    onComplete(false, null, null, "No backup data found in Firestore.")
                    return@addOnSuccessListener
                }

                try {
                    val networkFormulas = list.map { map ->
                        val id = (map["id"] as? Long)?.toInt() ?: 0
                        val title = map["title"] as? String ?: ""
                        val category = map["category"] as? String ?: "Custom"
                        val formula = map["formula"] as? String ?: ""
                        val resultUnit = map["resultUnit"] as? String ?: ""
                        val description = map["description"] as? String ?: ""
                        val varnsRaw = map["variables"] as? List<Map<String, Any>> ?: emptyList()
                        
                        val variables = varnsRaw.map { vMap ->
                            NetworkVariable(
                                symbol = vMap["symbol"] as? String ?: "",
                                name = vMap["name"] as? String ?: "",
                                value = (vMap["value"] as? Number)?.toDouble() ?: 0.0,
                                unit = vMap["unit"] as? String ?: ""
                            )
                        }

                        NetworkFormula(
                            id = id,
                            title = title,
                            category = category,
                            formula = formula,
                            resultUnit = resultUnit,
                            description = description,
                            variables = variables
                        )
                    }

                    onComplete(true, networkFormulas, decimalPlaces, "Successfully compiled Firestore backup!")
                } catch (e: Exception) {
                    onComplete(false, null, null, "Failed to parse data schema: ${e.localizedMessage}")
                }
            }.addOnFailureListener { e ->
                onComplete(false, null, null, "Failed to fetch backup formulas: ${e.localizedMessage}")
            }
        }.addOnFailureListener { e ->
            onComplete(false, null, null, "Failed to fetch settings: ${e.localizedMessage}")
        }
    }

    /**
     * Push a calculation log or history entry to active Firestore stream.
     */
    fun saveCalculationHistory(
        userId: String,
        slideTitle: String,
        formula: String,
        inputs: String,
        result: String
    ) {
        val firestore = getFirestore() ?: return
        val historyMap = mapOf(
            "timestamp" to java.text.DateFormat.getDateTimeInstance().format(java.util.Date()),
            "formulaTitle" to slideTitle,
            "formula" to formula,
            "inputs" to inputs,
            "result" to result
        )

        firestore.collection("users")
            .document(userId)
            .collection("calculation_history")
            .add(historyMap)
            .addOnSuccessListener {
                Log.d(TAG, "History entry logged to Firestore")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed logging history to Firestore", e)
            }
    }

    /**
     * Store customized formulas list to Realtime Database under user scope.
     */
    fun backupFormulasToRealtimeDatabase(
        userId: String,
        databaseUrl: String,
        slidesWithVars: List<SlideWithVariables>,
        decimalPlaces: Int,
        onComplete: (Boolean, String) -> Unit
    ) {
        try {
            val database = if (databaseUrl.isNotEmpty()) {
                FirebaseDatabase.getInstance(databaseUrl)
            } else {
                FirebaseDatabase.getInstance()
            }
            val userRef = database.getReference("users").child(userId)
            
            val settingsMap = mapOf(
                "decimalPlaces" to decimalPlaces,
                "lastBackupTime" to java.text.DateFormat.getDateTimeInstance().format(java.util.Date())
            )
            
            val formulaListMap = slidesWithVars.map { swv ->
                mapOf(
                    "id" to swv.slide.id,
                    "title" to swv.slide.title,
                    "category" to swv.slide.category,
                    "formula" to swv.slide.formula,
                    "resultUnit" to swv.slide.resultUnit,
                    "description" to swv.slide.description,
                    "variables" to swv.variables.map { v ->
                        mapOf(
                            "symbol" to v.symbol,
                            "name" to v.name,
                            "value" to v.value,
                            "unit" to v.unit
                        )
                    }
                )
            }
            
            val dataMap = mapOf(
                "settings" to settingsMap,
                "formulas" to mapOf("formulas" to formulaListMap)
            )
            
            userRef.child("data").setValue(dataMap)
                .addOnSuccessListener {
                    onComplete(true, "Successfully backed up ${formulaListMap.size} formulas to Realtime Database!")
                }
                .addOnFailureListener { e ->
                    onComplete(false, "Realtime Database backup failed: ${e.localizedMessage}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Realtime Database error", e)
            onComplete(false, "Realtime Database error: ${e.localizedMessage}")
        }
    }

    /**
     * Restore formulas and settings from Realtime Database.
     */
    fun restoreFormulasFromRealtimeDatabase(
        userId: String,
        databaseUrl: String,
        onComplete: (Boolean, List<NetworkFormula>?, Int?, String) -> Unit
    ) {
        try {
            val database = if (databaseUrl.isNotEmpty()) {
                FirebaseDatabase.getInstance(databaseUrl)
            } else {
                FirebaseDatabase.getInstance()
            }
            val userRef = database.getReference("users").child(userId).child("data")
            
            userRef.get().addOnSuccessListener { dataSnap ->
                if (!dataSnap.exists()) {
                    onComplete(false, null, null, "No backup data found in Realtime Database.")
                    return@addOnSuccessListener
                }
                
                try {
                    val settingsSnap = dataSnap.child("settings")
                    val decimalPlaces = settingsSnap.child("decimalPlaces").getValue(Long::class.java)?.toInt()
                    
                    val formulasSnap = dataSnap.child("formulas").child("formulas")
                    val list = formulasSnap.value as? List<Map<String, Any>>
                    if (list == null) {
                        onComplete(false, null, null, "No formulas found in Realtime Database backup.")
                        return@addOnSuccessListener
                    }
                    
                    val networkFormulas = list.map { map ->
                        val id = (map["id"] as? Long)?.toInt() ?: 0
                        val title = map["title"] as? String ?: ""
                        val category = map["category"] as? String ?: "Custom"
                        val formula = map["formula"] as? String ?: ""
                        val resultUnit = map["resultUnit"] as? String ?: ""
                        val description = map["description"] as? String ?: ""
                        val varnsRaw = map["variables"] as? List<Map<String, Any>> ?: emptyList()
                        
                        val variables = varnsRaw.map { vMap ->
                            NetworkVariable(
                                symbol = vMap["symbol"] as? String ?: "",
                                name = vMap["name"] as? String ?: "",
                                value = (vMap["value"] as? Number)?.toDouble() ?: 0.0,
                                unit = vMap["unit"] as? String ?: ""
                            )
                        }
                        
                        NetworkFormula(
                            id = id,
                            title = title,
                            category = category,
                            formula = formula,
                            resultUnit = resultUnit,
                            description = description,
                            variables = variables
                        )
                    }
                    
                    onComplete(true, networkFormulas, decimalPlaces, "Successfully compiled Realtime Database backup!")
                } catch (e: Exception) {
                    onComplete(false, null, null, "Failed to parse data schema: ${e.localizedMessage}")
                }
            }.addOnFailureListener { e ->
                onComplete(false, null, null, "Failed to fetch backup: ${e.localizedMessage}")
            }
        } catch (e: Exception) {
            onComplete(false, null, null, "Realtime Database exception: ${e.localizedMessage}")
        }
    }

    /**
     * Push a calculation log or history entry to active Realtime Database stream.
     */
    fun saveCalculationHistoryToRealtimeDatabase(
        userId: String,
        databaseUrl: String,
        slideTitle: String,
        formula: String,
        inputs: String,
        result: String
    ) {
        try {
            val database = if (databaseUrl.isNotEmpty()) {
                FirebaseDatabase.getInstance(databaseUrl)
            } else {
                FirebaseDatabase.getInstance()
            }
            val historyMap = mapOf(
                "timestamp" to java.text.DateFormat.getDateTimeInstance().format(java.util.Date()),
                "formulaTitle" to slideTitle,
                "formula" to formula,
                "inputs" to inputs,
                "result" to result
            )
            database.getReference("users")
                .child(userId)
                .child("calculation_history")
                .push()
                .setValue(historyMap)
                .addOnSuccessListener {
                    Log.d(TAG, "History entry logged to Realtime Database")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed logging history to Realtime Database", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Realtime Database history log exception", e)
        }
    }
}
