package com.example.thinningapp

import android.graphics.*
import kotlin.math.*

class Draw {
    fun getImages(lines: ArrayList<Line>): ArrayList<Bitmap> {
        var images = ArrayList<Bitmap>()

        for (line in lines) {
            val bitmap = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.BLACK)
            val paint = Paint()
            paint.strokeWidth = 40F
            paint.isAntiAlias = true

            for (i in 1 until line.points.size) {
                canvas.drawLine(line.points[i - 1].x,
                    line.points[i - 1].y,
                    line.points[i].x,
                    line.points[i].y,
                    paint)
            }

            images.add(Bitmap.createScaledBitmap(bitmap, 128, 128, false))
        }

        return images
    }

    fun combineImage(
        images: ArrayList<Bitmap>): Bitmap {
        var bitmap = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
        var maxX = Int.MIN_VALUE
        var minX = Int.MAX_VALUE
        var maxY = Int.MIN_VALUE
        var minY = Int.MAX_VALUE

        for (i in images.indices) {
            for (x in 0 until 128) {
                for (y in 0 until 128) {
                    val pixel = images[i].getPixel(x, y)
                    val r = pixel shr 16 and 0xFF
                    val a = Color.alpha(pixel)
                    if (r == 0) {
                        maxX = max(maxX, x)
                        minX = min(minX, x)
                        maxY = max(maxY, y)
                        minY = min(minY, y)
                        bitmap.setPixel(x, y, Color.argb(a, 0, 0, 0))
                    } else {
                        if (i == 0)
                            bitmap.setPixel(x, y, Color.argb(a, 255, 255, 255))
                    }
                }
            }
        }

        val x = max(0, min(127, minX - 2))
        val y = max(0, min(127, minY - 2))

        return Bitmap.createScaledBitmap(
            Bitmap.createBitmap(
                bitmap,
                x,
                y,
                min(128 - x, max(1, maxX - minX + 5)),
                min(128 - y, max(1, maxY - minY + 5))
            ), 128, 128, false
        )
    }
}