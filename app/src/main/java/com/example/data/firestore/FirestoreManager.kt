package com.example.data.firestore

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings

/**
 * Centralized manager for initializing and accessing Cloud Firestore.
 * Ensures consistent configuration (offline persistence, retry handling)
 * and safe fallback across the application and unit tests.
 */
object FirestoreManager {
    private const val TAG = "FirestoreManager"

    @Volatile
    private var firestoreInstance: FirebaseFirestore? = null

    @Volatile
    private var isConfigured: Boolean = false

    /**
     * Initializes Firestore with optimized settings (offline persistent disk cache).
     * Safe to call multiple times; will reuse existing instance unless reset.
     *
     * @param context Application context
     * @param customInstance Optional instance for testing or dependency injection
     * @return Initialized FirebaseFirestore, or null if Firebase is unconfigured
     */
    fun initialize(
        context: Context? = null,
        customInstance: FirebaseFirestore? = null
    ): FirebaseFirestore? {
        if (customInstance != null) {
            firestoreInstance = customInstance
            isConfigured = true
            return customInstance
        }

        if (firestoreInstance != null) {
            return firestoreInstance
        }

        synchronized(this) {
            if (firestoreInstance != null) {
                return firestoreInstance
            }

            try {
                // Check if FirebaseApp is available
                val hasApps = try {
                    if (context != null) {
                        FirebaseApp.getApps(context).isNotEmpty()
                    } else {
                        FirebaseApp.getApps(FirebaseApp.getInstance().applicationContext).isNotEmpty()
                    }
                } catch (e: Exception) {
                    false
                }

                if (!hasApps && context != null) {
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
                            Log.w(TAG, "Failed to auto-initialize FirebaseApp: ${e.message}, fallback: ${e2.message}")
                        }
                    }
                }

                val instance = FirebaseFirestore.getInstance()
                
                // Configure persistent disk cache for offline-first reliability
                try {
                    val settings = FirebaseFirestoreSettings.Builder()
                        .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                        .build()
                    instance.firestoreSettings = settings
                } catch (e: Exception) {
                    // Settings can only be set before any other Firestore operations
                    Log.d(TAG, "Firestore settings already initialized: ${e.message}")
                }

                firestoreInstance = instance
                isConfigured = true
                Log.i(TAG, "Cloud Firestore initialized successfully with offline persistent cache")
                return instance
            } catch (e: Exception) {
                Log.w(TAG, "Cloud Firestore could not be initialized: ${e.message}")
                isConfigured = false
                return null
            }
        }
    }

    /**
     * Returns the current Firestore instance, or attempts to retrieve it if FirebaseApp is ready.
     */
    fun getInstance(): FirebaseFirestore? {
        if (firestoreInstance != null) {
            return firestoreInstance
        }
        return try {
            val instance = FirebaseFirestore.getInstance()
            firestoreInstance = instance
            isConfigured = true
            instance
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Checks if Firestore is configured and ready to perform operations.
     */
    fun isAvailable(): Boolean {
        return getInstance() != null
    }

    /**
     * Allows test suites to reset or mock the Firestore instance.
     */
    @androidx.annotation.VisibleForTesting
    fun resetForTesting(mockInstance: FirebaseFirestore? = null) {
        firestoreInstance = mockInstance
        isConfigured = mockInstance != null
    }
}
