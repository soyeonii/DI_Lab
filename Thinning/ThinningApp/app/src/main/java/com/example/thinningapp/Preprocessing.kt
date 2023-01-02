package com.example.thinningapp

import org.opencv.android.OpenCVLoader
import org.opencv.core.Mat
import org.opencv.core.Rect
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

class Preprocessing {
    init {
        OpenCVLoader.initDebug()
    }

    fun deleteBackground(mat: Mat, size: Int, padding: Int): Mat {
        var minX = 0
        var maxX = 0
        var minY = 0
        var maxY = 0

        loop@ for (i in 0 until mat.rows()) {
            for (j in 0 until mat.cols()) {
                if (mat[i, j][0] < 1) {
                    minY = i
                    break@loop
                }
            }
        }

        loop@ for (i in mat.rows()-1 downTo 0) {
            for (j in 0 until mat.cols()) {
                if (mat[i, j][0] < 1) {
                    maxY = i
                    break@loop
                }
            }
        }

        loop@ for (i in 0 until mat.cols()) {
            for (j in 0 until mat.rows()) {
                if (mat[j, i][0] < 1) {
                    minX = i
                    break@loop
                }
            }
        }

        loop@ for (i in mat.cols()-1 downTo 0) {
            for (j in 0 until mat.rows()) {
                if (mat[j, i][0] < 1) {
                    maxX = i
                    break@loop
                }
            }
        }

        val roi = Rect(minX-padding, minY-padding, maxX-minX+padding*2, maxY-minY+padding*2)
        val matRoi = Mat(mat, roi)
        Imgproc.resize(matRoi, matRoi, Size(size.toDouble(), size.toDouble()))

        return matRoi
    }
}