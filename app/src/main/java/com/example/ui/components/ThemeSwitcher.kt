package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.CardShadowTint
import com.example.ui.theme.LocalThemeController

@Composable
fun ThemeSelectionDialog(
    onDismiss: () -> Unit
) {
    val themeController = LocalThemeController.current
    val currentMode = themeController.themeMode

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "เลือกธีมการแสดงผล",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                AppThemeMode.values().forEach { mode ->
                    val isSelected = currentMode == mode
                    val containerColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        },
                        label = "themeDialogContainer"
                    )
                    val borderColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        },
                        label = "themeDialogBorder"
                    )

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                themeController.setTheme(mode)
                                onDismiss()
                            },
                        shape = RoundedCornerShape(14.dp),
                        color = containerColor,
                        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surface
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = mode.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = mode.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = mode.description,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 14.sp
                                )
                            }

                            if (isSelected) {
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "ปิด",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(22.dp)
    )
}

/**
 * Top bar action icon button that allows one-tap opening of the theme selection dialog.
 */
@Composable
fun ThemeQuickToggleButton(
    modifier: Modifier = Modifier,
    iconTint: Color = Color.White
) {
    var showDialog by remember { mutableStateOf(false) }
    val themeController = LocalThemeController.current
    val systemInDark = isSystemInDarkTheme()

    val currentIcon = when (themeController.themeMode) {
        AppThemeMode.SYSTEM -> if (systemInDark) Icons.Filled.DarkMode else Icons.Filled.LightMode
        AppThemeMode.LIGHT -> Icons.Filled.LightMode
        AppThemeMode.DARK -> Icons.Filled.DarkMode
    }

    IconButton(
        onClick = { showDialog = true },
        modifier = modifier
    ) {
        Icon(
            imageVector = currentIcon,
            contentDescription = "สลับธีม (${themeController.themeMode.title})",
            tint = iconTint
        )
    }

    if (showDialog) {
        ThemeSelectionDialog(onDismiss = { showDialog = false })
    }
}

/**
 * Bento-style card widget for the Settings / Developer Info screen to switch themes.
 */
@Composable
fun ThemeSettingsCard(
    modifier: Modifier = Modifier
) {
    val themeController = LocalThemeController.current
    val currentMode = themeController.themeMode

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(18.dp), spotColor = CardShadowTint),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(4.dp, 16.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.primary)
                )
                Text(
                    text = "การแสดงผลและการมองเห็น",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppThemeMode.values().forEach { mode ->
                    val isSelected = currentMode == mode
                    val itemBgColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        },
                        label = "themeItemBg"
                    )
                    val itemBorderColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        },
                        label = "themeItemBorder"
                    )

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { themeController.setTheme(mode) },
                        shape = RoundedCornerShape(14.dp),
                        color = itemBgColor,
                        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, itemBorderColor)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surface
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = mode.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = when (mode) {
                                    AppThemeMode.SYSTEM -> "ตามระบบ"
                                    AppThemeMode.LIGHT -> "สว่าง"
                                    AppThemeMode.DARK -> "มืด"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Text(
                text = "โหมดปัจจุบัน: ${currentMode.title} - ${currentMode.description}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }
    }
}
