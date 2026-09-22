package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.BiometricAuthHelper
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BiometricAuthHelperTest {

    @Test
    fun testBiometricPreferencesDefaultAndToggle() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Default should be true for user convenience
        assertTrue(BiometricAuthHelper.isBiometricEnabled(context))

        // Set to false
        BiometricAuthHelper.setBiometricEnabled(context, false)
        assertFalse(BiometricAuthHelper.isBiometricEnabled(context))

        // Set back to true
        BiometricAuthHelper.setBiometricEnabled(context, true)
        assertTrue(BiometricAuthHelper.isBiometricEnabled(context))
    }
}
