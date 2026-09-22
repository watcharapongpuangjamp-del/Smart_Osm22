package com.example.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext

/**
 * Available theme modes for the Smart OSM application.
 */
enum class AppThemeMode(
    val title: String,
    val description: String,
    val icon: ImageVector
) {
    SYSTEM(
        title = "ตามระบบ (System Default)",
        description = "ปรับธีมสว่างหรือมืดตามการตั้งค่าของเครื่องโทรศัพท์",
        icon = Icons.Filled.BrightnessAuto
    ),
    LIGHT(
        title = "โหมดสว่าง (Light Mode)",
        description = "พื้นหลังสีขาวนวล อ่านสบายตา คมชัดในที่สว่าง",
        icon = Icons.Filled.LightMode
    ),
    DARK(
        title = "โหมดมืด (Dark Mode)",
        description = "พื้นหลังโทนมืดเขียวมรกต ถนอมสายตา และประหยัดแบตเตอรี่",
        icon = Icons.Filled.DarkMode
    )
}

/**
 * Controller class that manages theme selection and persists user preference in SharedPreferences.
 */
class ThemeController(
    private val preferences: SharedPreferences,
    initialMode: AppThemeMode
) {
    var themeMode by mutableStateOf(initialMode)
        private set

    fun setTheme(mode: AppThemeMode) {
        themeMode = mode
        preferences.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    fun toggleDarkMode(currentIsDark: Boolean) {
        if (currentIsDark) {
            setTheme(AppThemeMode.LIGHT)
        } else {
            setTheme(AppThemeMode.DARK)
        }
    }

    companion object {
        private const val PREFS_NAME = "smart_osm_theme_prefs"
        private const val KEY_THEME_MODE = "pref_app_theme_mode"

        fun create(context: Context): ThemeController {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val saved = prefs.getString(KEY_THEME_MODE, AppThemeMode.SYSTEM.name)
            val initial = try {
                AppThemeMode.valueOf(saved ?: AppThemeMode.SYSTEM.name)
            } catch (e: Exception) {
                AppThemeMode.SYSTEM
            }
            return ThemeController(prefs, initial)
        }
    }
}

/**
 * CompositionLocal to access ThemeController anywhere in the UI hierarchy.
 */
val LocalThemeController = staticCompositionLocalOf<ThemeController> {
    error("ThemeController not provided. Make sure to wrap with AppThemeProvider.")
}

/**
 * Theme Provider component that manages the app theme mode and applies light/dark color schemes.
 */
@Composable
fun AppThemeProvider(
    themeController: ThemeController = rememberThemeController(),
    content: @Composable () -> Unit
) {
    val systemInDark = isSystemInDarkTheme()
    val isDark = when (themeController.themeMode) {
        AppThemeMode.SYSTEM -> systemInDark
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    CompositionLocalProvider(LocalThemeController provides themeController) {
        MyApplicationTheme(darkTheme = isDark) {
            content()
        }
    }
}

@Composable
fun rememberThemeController(): ThemeController {
    val context = LocalContext.current.applicationContext
    return remember { ThemeController.create(context) }
}
