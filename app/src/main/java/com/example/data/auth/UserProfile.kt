package com.example.data.auth

import com.google.firebase.auth.FirebaseUser
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Data model representing the authenticated user's profile in Smart OSM.
 *
 * Adheres strictly to the system Identity model:
 * User Identity = Firebase Authentication user.uid
 * (Separate from personUuid and householdUuid).
 *
 * Contains authenticated user details retrieved from Firebase Auth,
 * such as display name, email, avatar URL, provider information,
 * and account timestamps.
 */
data class UserProfile(
    val uid: String,
    val displayName: String? = null,
    val email: String? = null,
    val photoUrl: String? = null,
    val isEmailVerified: Boolean = false,
    val phoneNumber: String? = null,
    val isAnonymous: Boolean = false,
    val providerId: String = "firebase",
    val providerIds: List<String> = emptyList(),
    val creationTimestamp: Long? = null,
    val lastSignInTimestamp: Long? = null,
    val villageNo: String = "8",
    val villageName: String = "หมู่ 8 บ้านกร่างประดู่วัง",
    val subdistrict: String = "ต.ป่าขะ",
    val district: String = "อ.บ้านนา",
    val province: String = "จ.นครนายก",
    val roleTitle: String = "อสม. ประจำหมู่บ้าน"
) {
    /**
     * True if this profile represents an authenticated Firebase user with a valid UID.
     */
    val isAuthenticated: Boolean
        get() = uid.isNotBlank()

    /**
     * User-facing display name with a sensible fallback.
     */
    val safeDisplayName: String
        get() = displayName?.takeIf { it.isNotBlank() }
            ?: email?.substringBefore("@")?.takeIf { it.isNotBlank() }
            ?: if (isAnonymous) "ผู้ใช้ชั่วคราว (Guest)" else "ผู้ใช้งาน Smart OSM"

    /**
     * Formatted provider label in Thai for UI display.
     */
    val providerLabelThai: String
        get() = when {
            isAnonymous -> "ผู้ใช้ชั่วคราว (Guest / Anonymous)"
            providerIds.any { it.contains("google", ignoreCase = true) } || providerId.contains("google", ignoreCase = true) ->
                "บัญชี Google (Google Account)"
            providerIds.any { it.contains("password", ignoreCase = true) } || providerId.contains("password", ignoreCase = true) ->
                "อีเมลและรหัสผ่าน (Email & Password)"
            else -> providerId
        }

    /**
     * Formatted creation date string (e.g. 13 ก.ย. 2026 15:30 น.).
     */
    val formattedCreationDate: String?
        get() = creationTimestamp?.takeIf { it > 0 }?.let { formatDate(it) }

    /**
     * Formatted last sign-in date string.
     */
    val formattedLastSignInDate: String?
        get() = lastSignInTimestamp?.takeIf { it > 0 }?.let { formatDate(it) }

    companion object {
        private val thaiDateFormat by lazy {
            SimpleDateFormat("d MMM yyyy, HH:mm น.", Locale.forLanguageTag("th-TH"))
        }

        private fun formatDate(epochMillis: Long): String {
            return try {
                thaiDateFormat.format(Date(epochMillis))
            } catch (e: Exception) {
                Date(epochMillis).toString()
            }
        }

        /**
         * Safely creates a [UserProfile] instance from a [FirebaseUser].
         * Safely handles any null fields or unavailable metadata.
         */
        fun fromFirebaseUser(
            user: FirebaseUser,
            villageNo: String = "8",
            villageName: String = "หมู่ 8 บ้านกร่างประดู่วัง",
            subdistrict: String = "ต.ป่าขะ",
            district: String = "อ.บ้านนา",
            province: String = "จ.นครนายก",
            phoneNumberOverride: String? = null,
            roleTitle: String = "อสม. ประจำหมู่บ้าน"
        ): UserProfile {
            val providerList = try {
                user.providerData.map { it.providerId }
            } catch (e: Exception) {
                emptyList()
            }

            return UserProfile(
                uid = user.uid,
                displayName = user.displayName?.takeIf { it.isNotBlank() },
                email = user.email?.takeIf { it.isNotBlank() },
                photoUrl = try { user.photoUrl?.toString() } catch (e: Exception) { null },
                isEmailVerified = try { user.isEmailVerified } catch (e: Exception) { false },
                phoneNumber = phoneNumberOverride?.takeIf { it.isNotBlank() }
                    ?: try { user.phoneNumber?.takeIf { it.isNotBlank() } } catch (e: Exception) { null },
                isAnonymous = try { user.isAnonymous } catch (e: Exception) { false },
                providerId = try { user.providerId } catch (e: Exception) { "firebase" },
                providerIds = providerList,
                creationTimestamp = try { user.metadata?.creationTimestamp } catch (e: Exception) { null },
                lastSignInTimestamp = try { user.metadata?.lastSignInTimestamp } catch (e: Exception) { null },
                villageNo = villageNo,
                villageName = villageName,
                subdistrict = subdistrict,
                district = district,
                province = province,
                roleTitle = roleTitle
            )
        }
    }
}
