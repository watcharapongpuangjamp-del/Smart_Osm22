package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable

/**
 * Helper to generate custom Map Markers for Village Baseline GIS locations.
 */
fun createVillageMarkerDrawable(
    context: Context,
    villageName: String,
    houseCount: Int,
    isSelected: Boolean
): Drawable {
    val density = context.resources.displayMetrics.density
    val width = (80 * density).toInt()
    val height = (46 * density).toInt()
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Primary Colors
    val badgeBgColor = if (isSelected) android.graphics.Color.rgb(16, 185, 129) else android.graphics.Color.rgb(30, 58, 138)
    val strokeColor = if (isSelected) android.graphics.Color.YELLOW else android.graphics.Color.WHITE
    val textColor = android.graphics.Color.WHITE

    // Shadow
    val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.argb(60, 0, 0, 0)
    }
    val rectShadow = RectF(4 * density, 4 * density, width - 4 * density, 34 * density)
    canvas.drawRoundRect(rectShadow, 8 * density, 8 * density, shadowPaint)

    // Main Rounded Badge
    val rect = RectF(2 * density, 2 * density, width - 2 * density, 32 * density)
    paint.color = badgeBgColor
    paint.style = Paint.Style.FILL
    canvas.drawRoundRect(rect, 8 * density, 8 * density, paint)

    // Border Stroke
    paint.color = strokeColor
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 2 * density
    canvas.drawRoundRect(rect, 8 * density, 8 * density, paint)

    // Pointer Pin Bottom Triangle
    val path = Path().apply {
        moveTo((width / 2 - 6 * density), 32 * density)
        lineTo((width / 2 + 6 * density), 32 * density)
        lineTo((width / 2).toFloat(), 42 * density)
        close()
    }
    paint.style = Paint.Style.FILL
    paint.color = badgeBgColor
    canvas.drawPath(path, paint)

    // Text Label: Village Name
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor
        textSize = 10 * density
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }

    val displayName = if (villageName.length > 8) villageName.take(7) + ".." else villageName
    canvas.drawText(displayName, width / 2f, 16 * density, textPaint)

    // Subtext: Target house count
    val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(226, 232, 240)
        textSize = 8.5f * density
        textAlign = Paint.Align.CENTER
    }
    val houseLabel = if (houseCount > 0) "🎯 $houseCount หลัง" else "ศูนย์กลางหมู่บ้าน"
    canvas.drawText(houseLabel, width / 2f, 27 * density, subTextPaint)

    return BitmapDrawable(context.resources, bitmap)
}
