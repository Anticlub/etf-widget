package com.anticlub.etfwidget.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path

// Dibuja una mini-gráfica de línea con los valores dados en un Bitmap.
// Glance no tiene Canvas propio, por eso se pinta aquí y luego se muestra con un Image.
fun sparklineBitmap(
    values: List<Double>,
    widthPx: Int,
    heightPx: Int,
    lineColor: Int,
): Bitmap {
    val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    if (values.size < 2) return bitmap

    val canvas = Canvas(bitmap)
    val min = values.min()
    val max = values.max()
    val range = (max - min).takeIf { it > 0.0 } ?: 1.0

    val pad = 6f
    val w = widthPx - pad * 2
    val h = heightPx - pad * 2

    val path = Path()
    values.forEachIndexed { i, v ->
        val x = pad + w * i / (values.size - 1)
        val y = pad + h * (1f - ((v - min) / range).toFloat())
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = lineColor
        strokeWidth = 4f
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    canvas.drawPath(path, paint)
    return bitmap
}
