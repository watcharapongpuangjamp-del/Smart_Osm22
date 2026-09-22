package com.example

import com.example.data.auth.AuthManager
import com.example.viewmodel.AuthUiState
import com.example.viewmodel.AuthViewModel
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

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
    fun `initial state is Idle`() {
        val fakeAuthManager = object : AuthManager({ null }) {}
        val viewModel = AuthViewModel(fakeAuthManager)

        assertEquals(AuthUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `signInWithEmail validation fails on blank input`() = runTest(testDispatcher) {
        val fakeAuthManager = object : AuthManager({ null }) {}
        val viewModel = AuthViewModel(fakeAuthManager)

        viewModel.signInWithEmail("", "")
        assertTrue(viewModel.uiState.value is AuthUiState.Error)
        val error = viewModel.uiState.value as AuthUiState.Error
        assertEquals("กรุณากรอกอีเมลและรหัสผ่านให้ครบถ้วน", error.message)
    }

    @Test
    fun `signUpWithEmail validation fails on short password`() = runTest(testDispatcher) {
        val fakeAuthManager = object : AuthManager({ null }) {}
        val viewModel = AuthViewModel(fakeAuthManager)

        viewModel.signUpWithEmail("test@example.com", "12345")
        assertTrue(viewModel.uiState.value is AuthUiState.Error)
        val error = viewModel.uiState.value as AuthUiState.Error
        assertEquals("รหัสผ่านต้องมีความยาวอย่างน้อย 6 ตัวอักษร", error.message)
    }

    @Test
    fun `signUpWithEmail validation fails on blank input`() = runTest(testDispatcher) {
        val fakeAuthManager = object : AuthManager({ null }) {}
        val viewModel = AuthViewModel(fakeAuthManager)

        viewModel.signUpWithEmail("", "")
        assertTrue(viewModel.uiState.value is AuthUiState.Error)
        val error = viewModel.uiState.value as AuthUiState.Error
        assertEquals("กรุณากรอกอีเมลและรหัสผ่านให้ครบถ้วน", error.message)
    }

    @Test
    fun `signInAnonymously sets error state when manager returns failure`() = runTest(testDispatcher) {
        val fakeAuthManager = object : AuthManager({ null }) {
            override suspend fun signInAnonymously(): Result<FirebaseUser> {
                return Result.failure(IllegalStateException("Network offline"))
            }
        }
        val viewModel = AuthViewModel(fakeAuthManager)

        viewModel.signInAnonymously()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is AuthUiState.Error)
        val error = viewModel.uiState.value as AuthUiState.Error
        assertEquals("Network offline", error.message)
    }

    @Test
    fun `signInWithEmail sets error state when manager returns failure`() = runTest(testDispatcher) {
        val fakeAuthManager = object : AuthManager({ null }) {
            override suspend fun signInWithEmail(email: String, pass: String): Result<FirebaseUser> {
                return Result.failure(IllegalStateException("User not found"))
            }
        }
        val viewModel = AuthViewModel(fakeAuthManager)

        viewModel.signInWithEmail("osm@health.go.th", "Secret1234")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is AuthUiState.Error)
        val error = viewModel.uiState.value as AuthUiState.Error
        assertEquals("User not found", error.message)
    }

    @Test
    fun `signUpWithEmail sets error state when manager returns failure`() = runTest(testDispatcher) {
        val fakeAuthManager = object : AuthManager({ null }) {
            override suspend fun signUpWithEmail(email: String, pass: String): Result<FirebaseUser> {
                return Result.failure(IllegalStateException("Email already in use"))
            }
        }
        val viewModel = AuthViewModel(fakeAuthManager)

        viewModel.signUpWithEmail("osm@health.go.th", "Secret1234")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is AuthUiState.Error)
        val error = viewModel.uiState.value as AuthUiState.Error
        assertEquals("Email already in use", error.message)
    }

    @Test
    fun `resetState resets state back to Idle`() = runTest(testDispatcher) {
        val fakeAuthManager = object : AuthManager({ null }) {}
        val viewModel = AuthViewModel(fakeAuthManager)

        viewModel.signInWithEmail("", "")
        assertTrue(viewModel.uiState.value is AuthUiState.Error)

        viewModel.resetState()
        assertEquals(AuthUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `signOut resets ui state to Idle`() = runTest(testDispatcher) {
        val fakeAuthManager = object : AuthManager({ null }) {
            override fun signOut(context: android.content.Context?) {}
        }
        val viewModel = AuthViewModel(fakeAuthManager)

        viewModel.signOut()
        assertEquals(AuthUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `signInWithGoogleTest sets success state`() = runTest(testDispatcher) {
        val fakeAuthManager = object : AuthManager({ null }) {}
        val viewModel = AuthViewModel(fakeAuthManager)
        val context = org.robolectric.RuntimeEnvironment.getApplication()

        viewModel.signInWithGoogleTest(context, "testuser@gmail.com")
        assertTrue(viewModel.uiState.value is AuthUiState.Success)
        val success = viewModel.uiState.value as AuthUiState.Success
        assertEquals("เข้าสู่ระบบบัญชี Google ทดสอบสำเร็จ", success.message)
    }
}
