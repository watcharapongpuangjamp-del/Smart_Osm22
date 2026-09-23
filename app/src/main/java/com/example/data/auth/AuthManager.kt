package com.example.data.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Authentication manager for Smart OSM.
 *
 * Implements the system User Identity model:
 * User Identity = Firebase Authentication user.uid
 *
 * Supports Google Sign-In via Android Credential Manager,
 * Email/Password authentication, and Anonymous guest authentication
 * while preserving Room local-first functionality.
 */
open class AuthManager(
    private val authProvider: () -> FirebaseAuth? = {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.w("AuthManager", "FirebaseAuth not available: ${e.message}")
            null
        }
    }
) {
    companion object {
        private const val TAG = "AuthManager"
    }

    private val firebaseAuth: FirebaseAuth?
        get() = authProvider()

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    open val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private var _localProfile: UserProfile? = null

    val currentUid: String?
        get() = _currentUser.value?.uid ?: _localProfile?.uid ?: _userProfile.value?.uid

    private var cachedVillageNo: String = "8"
    private var cachedVillageName: String = "หมู่ 8 บ้านกร่างประดู่วัง"
    private var cachedSubdistrict: String = "ต.ป่าขะ"
    private var cachedDistrict: String = "อ.บ้านนา"
    private var cachedProvince: String = "จ.นครนายก"
    private var cachedPhoneNumber: String? = null
    private var cachedRoleTitle: String = "อสม. ประจำหมู่บ้าน"
    private var cachedFullName: String? = null
    private var cachedPhotoUrl: String? = null
    private var cachedVhvCardId: String? = null
    private var cachedCitizenId: String? = null
    private var cachedHealthCenter: String? = null
    private var cachedVhvCardPhotoUrl: String? = null

    fun ensureFirebase(context: Context): FirebaseAuth? {
        try {
            val hasApps = try {
                FirebaseApp.getApps(context).isNotEmpty()
            } catch (e: Exception) {
                false
            }
            if (!hasApps) {
                try {
                    FirebaseApp.initializeApp(context)
                } catch (e: Exception) {
                    try {
                        val options = com.google.firebase.FirebaseOptions.Builder()
                            .setApplicationId(context.packageName)
                            .setApiKey("AIzaSySmartOsmAndroidKeySurvey2026")
                            .setProjectId("smart-osm-community")
                            .build()
                        FirebaseApp.initializeApp(context, options)
                    } catch (e2: Exception) {
                        Log.w(TAG, "Fallback FirebaseApp init failed: ${e2.message}")
                    }
                }
            }
            return try {
                FirebaseAuth.getInstance()
            } catch (e: Exception) {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "ensureFirebase failed: ${e.message}")
            return null
        }
    }

    open fun loadSurveyorProfile(context: Context) {
        try {
            ensureFirebase(context)
            val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
            cachedVillageNo = prefs.getString("surveyor_village_no", "8") ?: "8"
            cachedVillageName = prefs.getString("surveyor_village_name", "หมู่ 8 บ้านกร่างประดู่วัง") ?: "หมู่ 8 บ้านกร่างประดู่วัง"
            cachedSubdistrict = prefs.getString("surveyor_subdistrict", "ต.ป่าขะ") ?: "ต.ป่าขะ"
            cachedDistrict = prefs.getString("surveyor_district", "อ.บ้านนา") ?: "อ.บ้านนา"
            cachedProvince = prefs.getString("surveyor_province", "จ.นครนายก") ?: "จ.นครนายก"
            cachedPhoneNumber = prefs.getString("surveyor_phone", null)
            cachedRoleTitle = prefs.getString("surveyor_role", "อสม. ประจำหมู่บ้าน") ?: "อสม. ประจำหมู่บ้าน"
            cachedVhvCardId = prefs.getString("surveyor_vhv_card_id", null)
            cachedCitizenId = prefs.getString("surveyor_citizen_id", null)
            cachedHealthCenter = prefs.getString("surveyor_health_center", "รพ.สต.ป่าขะ")
            cachedVhvCardPhotoUrl = prefs.getString("surveyor_vhv_card_photo", null)

            val localUid = prefs.getString("local_user_uid", null) ?: "vhv_local_user_1"
            val displayName = prefs.getString("local_user_name", null)
            val photoUrl = prefs.getString("local_user_photo", null)

            val profile = UserProfile(
                uid = _currentUser.value?.uid ?: localUid,
                displayName = displayName ?: _currentUser.value?.displayName,
                email = prefs.getString("local_user_email", _currentUser.value?.email),
                photoUrl = photoUrl ?: _currentUser.value?.photoUrl?.toString(),
                isEmailVerified = true,
                phoneNumber = cachedPhoneNumber ?: _currentUser.value?.phoneNumber,
                isAnonymous = prefs.getBoolean("local_user_anonymous", false),
                providerId = prefs.getString("local_user_provider", "local") ?: "local",
                providerIds = listOf(prefs.getString("local_user_provider", "local") ?: "local"),
                creationTimestamp = prefs.getLong("local_user_timestamp", System.currentTimeMillis()),
                lastSignInTimestamp = System.currentTimeMillis(),
                villageNo = cachedVillageNo,
                villageName = cachedVillageName,
                subdistrict = cachedSubdistrict,
                district = cachedDistrict,
                province = cachedProvince,
                roleTitle = cachedRoleTitle,
                vhvCardId = cachedVhvCardId,
                citizenId = cachedCitizenId,
                healthCenter = cachedHealthCenter,
                vhvCardPhotoUrl = cachedVhvCardPhotoUrl
            )
            _localProfile = profile
            _userProfile.value = profile
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load surveyor profile: ${e.message}")
        }
    }

    fun setLocalProfile(
        context: Context,
        uid: String,
        email: String?,
        displayName: String?,
        photoUrl: String?,
        provider: String
    ) {
        val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("local_user_uid", uid)
            .putString("local_user_email", email)
            .putString("local_user_name", displayName)
            .putString("local_user_photo", photoUrl)
            .putString("local_user_provider", provider)
            .putLong("local_user_timestamp", System.currentTimeMillis())
            .apply()

        val profile = UserProfile(
            uid = uid,
            displayName = displayName,
            email = email,
            photoUrl = photoUrl,
            isEmailVerified = true,
            phoneNumber = cachedPhoneNumber,
            isAnonymous = (provider == "anonymous"),
            providerId = provider,
            providerIds = listOf(provider),
            creationTimestamp = System.currentTimeMillis(),
            lastSignInTimestamp = System.currentTimeMillis(),
            villageNo = cachedVillageNo,
            villageName = cachedVillageName,
            subdistrict = cachedSubdistrict,
            district = cachedDistrict,
            province = cachedProvince,
            roleTitle = cachedRoleTitle,
            vhvCardId = cachedVhvCardId,
            citizenId = cachedCitizenId,
            healthCenter = cachedHealthCenter,
            vhvCardPhotoUrl = cachedVhvCardPhotoUrl
        )
        _localProfile = profile
        _userProfile.value = profile
    }

    open fun saveSurveyorProfile(
        context: Context,
        fullName: String? = null,
        villageNo: String,
        villageName: String,
        subdistrict: String = "ต.ป่าขะ",
        district: String = "อ.บ้านนา",
        province: String = "จ.นครนายก",
        phone: String? = null,
        role: String? = "อสม. ประจำหมู่บ้าน",
        vhvCardId: String? = null,
        citizenId: String? = null,
        healthCenter: String? = null,
        photoUrl: String? = null,
        vhvCardPhotoUrl: String? = null
    ) {
        try {
            val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
            cachedVillageNo = villageNo.trim().ifBlank { "8" }
            cachedVillageName = villageName.trim().ifBlank { "หมู่ 8 บ้านกร่างประดู่วัง" }
            cachedSubdistrict = subdistrict.trim().ifBlank { "ต.ป่าขะ" }
            cachedDistrict = district.trim().ifBlank { "อ.บ้านนา" }
            cachedProvince = province.trim().ifBlank { "จ.นครนายก" }
            cachedPhoneNumber = phone?.trim()?.takeIf { it.isNotBlank() }
            cachedRoleTitle = role?.trim()?.takeIf { it.isNotBlank() } ?: "อสม. ประจำหมู่บ้าน"
            if (!vhvCardId.isNullOrBlank()) cachedVhvCardId = vhvCardId.trim()
            if (!citizenId.isNullOrBlank()) cachedCitizenId = citizenId.trim()
            if (!healthCenter.isNullOrBlank()) cachedHealthCenter = healthCenter.trim()
            if (!vhvCardPhotoUrl.isNullOrBlank()) cachedVhvCardPhotoUrl = vhvCardPhotoUrl.trim()

            val editor = prefs.edit()
                .putString("surveyor_village_no", cachedVillageNo)
                .putString("surveyor_village_name", cachedVillageName)
                .putString("surveyor_subdistrict", cachedSubdistrict)
                .putString("surveyor_district", cachedDistrict)
                .putString("surveyor_province", cachedProvince)
                .putString("surveyor_phone", cachedPhoneNumber)
                .putString("surveyor_role", cachedRoleTitle)
                .putString("surveyor_vhv_card_id", cachedVhvCardId)
                .putString("surveyor_citizen_id", cachedCitizenId)
                .putString("surveyor_health_center", cachedHealthCenter)
                .putString("surveyor_vhv_card_photo", cachedVhvCardPhotoUrl)
                .putBoolean("surveyor_setup_completed", true)

            fullName?.trim()?.takeIf { it.isNotBlank() }?.let { name ->
                editor.putString("local_user_name", name)
            }
            photoUrl?.trim()?.takeIf { it.isNotBlank() }?.let { pic ->
                editor.putString("local_user_photo", pic)
            }
            editor.apply()

            val currentProfile = _userProfile.value
            val newUid = currentProfile?.uid ?: prefs.getString("local_user_uid", "vhv_local_user_1") ?: "vhv_local_user_1"
            val newName = fullName?.trim()?.takeIf { it.isNotBlank() } ?: currentProfile?.displayName ?: prefs.getString("local_user_name", "ผู้ลงทะเบียน อสม.")
            val newPhoto = photoUrl?.trim()?.takeIf { it.isNotBlank() } ?: currentProfile?.photoUrl ?: prefs.getString("local_user_photo", null)

            val updatedProfile = UserProfile(
                uid = newUid,
                displayName = newName,
                email = currentProfile?.email ?: prefs.getString("local_user_email", null),
                photoUrl = newPhoto,
                isEmailVerified = true,
                phoneNumber = cachedPhoneNumber,
                isAnonymous = currentProfile?.isAnonymous ?: false,
                providerId = currentProfile?.providerId ?: "local",
                providerIds = currentProfile?.providerIds ?: listOf("local"),
                creationTimestamp = currentProfile?.creationTimestamp ?: System.currentTimeMillis(),
                lastSignInTimestamp = System.currentTimeMillis(),
                villageNo = cachedVillageNo,
                villageName = cachedVillageName,
                subdistrict = cachedSubdistrict,
                district = cachedDistrict,
                province = cachedProvince,
                roleTitle = cachedRoleTitle,
                vhvCardId = cachedVhvCardId,
                citizenId = cachedCitizenId,
                healthCenter = cachedHealthCenter,
                vhvCardPhotoUrl = cachedVhvCardPhotoUrl
            )
            _localProfile = updatedProfile
            _userProfile.value = updatedProfile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save surveyor profile", e)
        }
    }

    private fun updateUser(user: FirebaseUser?) {
        _currentUser.value = user
        if (user != null) {
            _userProfile.value = UserProfile.fromFirebaseUser(
                user = user,
                villageNo = cachedVillageNo,
                villageName = cachedVillageName,
                subdistrict = cachedSubdistrict,
                district = cachedDistrict,
                province = cachedProvince,
                phoneNumberOverride = cachedPhoneNumber,
                roleTitle = cachedRoleTitle,
                vhvCardId = cachedVhvCardId,
                citizenId = cachedCitizenId,
                healthCenter = cachedHealthCenter,
                vhvCardPhotoUrl = cachedVhvCardPhotoUrl
            )
        } else if (_localProfile != null) {
            _userProfile.value = _localProfile
        } else {
            _userProfile.value = null
        }
    }

    init {
        try {
            val auth = firebaseAuth
            if (auth != null) {
                updateUser(auth.currentUser)
                auth.addAuthStateListener { updatedAuth ->
                    updateUser(updatedAuth.currentUser)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to initialize AuthStateListener: ${e.message}")
        }
    }

    open fun isAuthAvailable(): Boolean {
        return firebaseAuth != null || _localProfile != null || _userProfile.value?.isAuthenticated == true
    }

    /**
     * Signs in immediately with a test Google Account profile.
     * Adheres strictly to User Identity architecture: User Identity = "google:<id>"
     * linked with Area Identity (villageId) and local persistence.
     */
    open fun signInWithGoogleTest(
        context: Context,
        email: String = "gigatvthai@gmail.com",
        displayName: String = "ผู้สำรวจ อสม. (Google Test)"
    ): Result<UserProfile> {
        val uid = "google:${Math.abs(email.hashCode())}"
        setLocalProfile(
            context = context,
            uid = uid,
            email = email,
            displayName = displayName,
            photoUrl = null,
            provider = "google.com"
        )
        val profile = _userProfile.value ?: UserProfile(
            uid = uid,
            displayName = displayName,
            email = email,
            providerId = "google.com",
            providerIds = listOf("google.com"),
            villageNo = cachedVillageNo,
            villageName = cachedVillageName,
            subdistrict = cachedSubdistrict,
            district = cachedDistrict,
            province = cachedProvince,
            roleTitle = cachedRoleTitle
        )
        Log.i(TAG, "Signed in via Google Test account. UID: $uid")
        return Result.success(profile)
    }

    /**
     * Triggers Google Sign-In via Android Credential Manager and exchanges
     * the Google ID Token with Firebase Authentication (if configured)
     * or provisions a valid User Identity locally.
     */
    open suspend fun signInWithGoogle(
        context: Context,
        customWebClientId: String? = null
    ): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            // 1. Resolve Web Client ID from params, saved preferences, or generated strings.xml
            val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
            if (!customWebClientId.isNullOrBlank()) {
                prefs.edit().putString("web_client_id", customWebClientId.trim()).apply()
            }
            val savedClientId = prefs.getString("web_client_id", null)

            val webClientIdResId = context.resources.getIdentifier(
                "default_web_client_id", "string", context.packageName
            )
            val defaultWebClientId = if (webClientIdResId != 0) {
                try { context.getString(webClientIdResId) } catch (e: Exception) { "" }
            } else {
                ""
            }

            val clientId = customWebClientId?.trim()?.takeIf { it.isNotBlank() }
                ?: savedClientId?.trim()?.takeIf { it.isNotBlank() }
                ?: defaultWebClientId.trim().takeIf { it.isNotBlank() }

            if (clientId.isNullOrBlank()) {
                return@withContext Result.failure(
                    IllegalStateException(
                        "MISSING_WEB_CLIENT_ID: ยังไม่ได้กำหนดค่า Web Client ID สำหรับ Google Sign-In"
                    )
                )
            }

            // 2. Trigger Android Credential Manager
            val credentialManager = CredentialManager.create(context)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(clientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val response = credentialManager.getCredential(
                request = request,
                context = context
            )

            val credential = response.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                
                // 3. Ensure Firebase Auth is ready if available
                val auth = ensureFirebase(context) ?: firebaseAuth
                var firebaseUser: FirebaseUser? = null
                if (auth != null) {
                    try {
                        val authCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                        val authResult = auth.signInWithCredential(authCredential).await()
                        firebaseUser = authResult.user
                    } catch (e: Exception) {
                        Log.w(TAG, "Firebase credential exchange skipped or offline: ${e.message}")
                    }
                }

                val userEmail = googleIdTokenCredential.id
                val displayName = googleIdTokenCredential.displayName ?: userEmail.substringBefore("@")
                val photoUrl = googleIdTokenCredential.profilePictureUri?.toString()
                val uid = firebaseUser?.uid ?: "google:${Math.abs(userEmail.hashCode())}"

                if (firebaseUser != null) {
                    updateUser(firebaseUser)
                } else {
                    setLocalProfile(
                        context = context,
                        uid = uid,
                        email = userEmail,
                        displayName = displayName,
                        photoUrl = photoUrl,
                        provider = "google.com"
                    )
                }

                val profile = _userProfile.value ?: UserProfile(
                    uid = uid,
                    displayName = displayName,
                    email = userEmail,
                    photoUrl = photoUrl,
                    providerId = "google.com",
                    providerIds = listOf("google.com"),
                    villageNo = cachedVillageNo,
                    villageName = cachedVillageName,
                    subdistrict = cachedSubdistrict,
                    district = cachedDistrict,
                    province = cachedProvince,
                    roleTitle = cachedRoleTitle
                )

                Log.i(TAG, "Google Sign-In successful. User UID: ${profile.uid}")
                Result.success(profile)
            } else {
                Result.failure(IllegalStateException("ประเภทข้อมูล Credential ไม่ถูกต้อง: ${credential::class.java.name}"))
            }
        } catch (e: GetCredentialCancellationException) {
            Log.i(TAG, "User cancelled Google Sign-In prompt")
            Result.failure(e)
        } catch (e: GetCredentialException) {
            if (e.message?.contains("No credentials available", ignoreCase = true) == true || e.javaClass.simpleName == "NoCredentialException") {
                Log.w(TAG, "CredentialManager: No credentials available on device (${e.message})")
            } else {
                Log.e(TAG, "CredentialManager failed: ${e.message}", e)
            }
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Authentication failed", e)
            Result.failure(e)
        }
    }

    /**
     * Signs in an existing user with Email and Password using Firebase Auth.
     */
    open suspend fun signInWithEmail(email: String, pass: String): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        try {
            val auth = firebaseAuth ?: return@withContext Result.failure(
                IllegalStateException("Firebase Auth ยังไม่ได้ตั้งค่าในระบบนี้")
            )
            val result = auth.signInWithEmailAndPassword(email.trim(), pass).await()
            val user = result.user ?: throw IllegalStateException("User is null after sign in")
            updateUser(user)
            Log.i(TAG, "Email Sign-In successful. User UID: ${user.uid}")
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Sign-in with email failed", e)
            Result.failure(e)
        }
    }

    /**
     * Registers a new user with Email and Password using Firebase Auth.
     */
    open suspend fun signUpWithEmail(email: String, pass: String): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        try {
            val auth = firebaseAuth ?: return@withContext Result.failure(
                IllegalStateException("Firebase Auth ยังไม่ได้ตั้งค่าในระบบนี้")
            )
            val result = auth.createUserWithEmailAndPassword(email.trim(), pass).await()
            val user = result.user ?: throw IllegalStateException("User is null after registration")
            updateUser(user)
            Log.i(TAG, "Email Registration successful. User UID: ${user.uid}")
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Registration with email failed", e)
            Result.failure(e)
        }
    }

    /**
     * Signs in anonymously to obtain a valid Firebase Authentication user.uid
     * without requiring an external OAuth provider setup.
     */
    open suspend fun signInAnonymously(): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        try {
            val auth = firebaseAuth ?: return@withContext Result.failure(
                IllegalStateException("Firebase Auth ยังไม่ได้ตั้งค่าในระบบนี้")
            )
            val result = auth.signInAnonymously().await()
            val user = result.user ?: throw IllegalStateException("User is null after anonymous auth")
            updateUser(user)
            Log.i(TAG, "Anonymous Sign-In successful. User UID: ${user.uid}")
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Anonymous sign-in failed", e)
            Result.failure(e)
        }
    }

    /**
     * Signs out the current user from Firebase Authentication or local test session.
     */
    open fun signOut(context: Context? = null) {
        try {
            firebaseAuth?.signOut()
            updateUser(null)
            _localProfile = null
            _userProfile.value = null
            if (context != null) {
                try {
                    context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                        .edit()
                        .remove("local_user_uid")
                        .remove("local_user_email")
                        .remove("local_user_name")
                        .remove("local_user_photo")
                        .remove("local_user_provider")
                        .apply()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to clear local user preferences on sign out: ${e.message}")
                }
            }
            Log.i(TAG, "User signed out successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error signing out", e)
        }
    }
}
