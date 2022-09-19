package com.example.diapp

import android.content.Context
import android.graphics.*
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import kotlin.math.*

class Draw(private val preprocess: Preprocess, private val context: Context) {
    private val mainActivity = MainActivity.getInstance()

    fun getImages(width: Int, height: Int, padding: Int): ArrayList<Bitmap> {
        var images = ArrayList<Bitmap>()

        for (line in mainActivity?.allLines!!) {
            val bitmap = Bitmap.createBitmap(
                preprocess.getWidth().toInt() + padding * 2,
                preprocess.getHeight().toInt() + padding * 2,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            val paint = Paint()
            paint.strokeWidth = 40F
            paint.isAntiAlias = true

            for (i in 1 until line.points.size) {
                canvas.drawLine(
                    line.points[i - 1].x + padding,
                    line.points[i - 1].y + padding,
                    line.points[i].x + padding,
                    line.points[i].y + padding,
                    paint
                )
            }

            images.add(Bitmap.createScaledBitmap(bitmap, width, height, false))
        }

        return images
    }

    fun combineImage(
        images: ArrayList<Bitmap>, indices: List<Int>, size: Int, width: Int, height: Int
    ): Bitmap {
        var bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        var maxX = Int.MIN_VALUE
        var minX = Int.MAX_VALUE
        var maxY = Int.MIN_VALUE
        var minY = Int.MAX_VALUE

        for ((i, index) in indices.withIndex()) {
            for (x in 0 until size) {
                for (y in 0 until size) {
                    val pixel = images[index].getPixel(x, y)
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

        val x = max(0, min(size - 1, minX - 2))
        val y = max(0, min(size - 1, minY - 2))

        return Bitmap.createScaledBitmap(
            Bitmap.createBitmap(
                bitmap,
                x,
                y,
                min(size - x, max(1, maxX - minX + 5)),
                min(size - y, max(1, maxY - minY + 5))
            ), width, height, false
        )
    }

    fun saveImage(image: Bitmap, folderName: String, fileName: String) {
        val fos = FileOutputStream(
            File(
                this.context.getDir(folderName, Context.MODE_PRIVATE),
                "$fileName.png"
            )
        )
        image.compress(Bitmap.CompressFormat.PNG, 100, fos)
        fos.close()
    }

//    private fun grayScale(original: Bitmap): Bitmap {
//        Log.i("gray", "in")
//        val width: Int = original.width
//        val height: Int = original.height
//        val bmpGrayScale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
//
//        for (x in 0 until width) {
//            for (y in 0 until height) {
//                val pixel = original.getPixel(x, y)
//
//                val a = Color.alpha(pixel)
//                val r = Color.red(pixel)
//                val g = Color.green(pixel)
//                val b = Color.blue(pixel)
//                var gray = (0.2989 * r + 0.5870 * g + 0.1140 * b).toInt()
//
//                gray = if (gray > 128)
//                    255
//                else
//                    0
//                bmpGrayScale.setPixel(x, y, Color.argb(a, gray, gray, gray))
//            }
//        }
//        return bmpGrayScale
//    }

    fun changeSize(bitmap: Bitmap, changeSize: Int): Bitmap {
        return Bitmap.createScaledBitmap(bitmap, changeSize, changeSize, false)
    }
}