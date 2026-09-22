package com.example

import com.example.data.auth.AuthManager
import com.example.data.auth.UserProfile
import com.example.viewmodel.AuthViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UserProfileTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `UserProfile holds correct values and flags`() {
        val profile = UserProfile(
            uid = "firebase-uid-12345",
            displayName = "สมศักดิ์ สุขใจ",
            email = "somsak@example.com",
            photoUrl = "https://example.com/avatar.jpg",
            isEmailVerified = true,
            phoneNumber = "0812345678",
            isAnonymous = false,
            providerId = "google.com",
            providerIds = listOf("google.com"),
            creationTimestamp = 1710000000000L,
            lastSignInTimestamp = 1710001000000L
        )

        assertEquals("firebase-uid-12345", profile.uid)
        assertEquals("สมศักดิ์ สุขใจ", profile.displayName)
        assertEquals("somsak@example.com", profile.email)
        assertEquals("https://example.com/avatar.jpg", profile.photoUrl)
        assertTrue(profile.isEmailVerified)
        assertEquals("0812345678", profile.phoneNumber)
        assertFalse(profile.isAnonymous)
        assertTrue(profile.isAuthenticated)
        assertEquals("บัญชี Google (Google Account)", profile.providerLabelThai)
        assertNotNull(profile.formattedCreationDate)
        assertNotNull(profile.formattedLastSignInDate)
    }

    @Test
    fun `safeDisplayName fallbacks correctly for all user types`() {
        // Case 1: Display name present
        val profile1 = UserProfile(uid = "uid1", displayName = "สมชาย สุขเกษม", email = "somchai@health.go.th")
        assertEquals("สมชาย สุขเกษม", profile1.safeDisplayName)

        // Case 2: Display name null, email present -> use username before @
        val profile2 = UserProfile(uid = "uid2", displayName = null, email = "doctor.lee@health.go.th")
        assertEquals("doctor.lee", profile2.safeDisplayName)

        // Case 3: Anonymous guest user -> localized guest title
        val profile3 = UserProfile(uid = "uid3", displayName = null, email = null, isAnonymous = true)
        assertEquals("ผู้ใช้ชั่วคราว (Guest)", profile3.safeDisplayName)

        // Case 4: Blank displayName and blank email -> default fallback
        val profile4 = UserProfile(uid = "uid4", displayName = "   ", email = "")
        assertEquals("ผู้ใช้งาน Smart OSM", profile4.safeDisplayName)
    }

    @Test
    fun `providerLabelThai formats providers correctly`() {
        // Google Provider
        val googleUser = UserProfile(uid = "uid1", providerId = "google.com", providerIds = listOf("google.com"))
        assertEquals("บัญชี Google (Google Account)", googleUser.providerLabelThai)

        // Email & Password
        val emailUser = UserProfile(uid = "uid2", providerId = "password", providerIds = listOf("password"))
        assertEquals("อีเมลและรหัสผ่าน (Email & Password)", emailUser.providerLabelThai)

        // Guest / Anonymous
        val guestUser = UserProfile(uid = "uid3", isAnonymous = true)
        assertEquals("ผู้ใช้ชั่วคราว (Guest / Anonymous)", guestUser.providerLabelThai)

        // Custom Provider
        val customUser = UserProfile(uid = "uid4", providerId = "oidc.thaigov")
        assertEquals("oidc.thaigov", customUser.providerLabelThai)
    }

    @Test
    fun `formatted dates return null when timestamps missing or zero`() {
        val profile = UserProfile(uid = "uid1", creationTimestamp = null, lastSignInTimestamp = 0L)
        assertNull(profile.formattedCreationDate)
        assertNull(profile.formattedLastSignInDate)
    }

    @Test
    fun `isAuthenticated returns true only when uid is not blank`() {
        val validProfile = UserProfile(uid = "uid-abc-123")
        assertTrue(validProfile.isAuthenticated)

        val emptyProfile = UserProfile(uid = "")
        assertFalse(emptyProfile.isAuthenticated)
    }

    @Test
    fun `UserProfile holds surveyor area and role attributes`() {
        val profile = UserProfile(
            uid = "firebase-uid-osm-1",
            displayName = "สมศรี สุขเกษม",
            villageNo = "8",
            villageName = "หมู่ 8 บ้านกร่างประดู่วัง",
            subdistrict = "ต.ป่าขะ",
            district = "อ.บ้านนา",
            province = "จ.นครนายก",
            roleTitle = "อสม. ประจำหมู่บ้าน"
        )

        assertEquals("8", profile.villageNo)
        assertEquals("หมู่ 8 บ้านกร่างประดู่วัง", profile.villageName)
        assertEquals("ต.ป่าขะ", profile.subdistrict)
        assertEquals("อ.บ้านนา", profile.district)
        assertEquals("จ.นครนายก", profile.province)
        assertEquals("อสม. ประจำหมู่บ้าน", profile.roleTitle)
    }

    @Test
    fun `AuthViewModel exposes userProfile StateFlow`() = runTest(testDispatcher) {
        val fakeAuthManager = object : AuthManager({ null }) {}
        val viewModel = AuthViewModel(fakeAuthManager)

        // Initially, userProfile is null when no user is signed in
        assertNull(viewModel.userProfile.value)

        // When signOut is called, state remains clean
        viewModel.signOut()
        assertNull(viewModel.userProfile.value)
    }
}
