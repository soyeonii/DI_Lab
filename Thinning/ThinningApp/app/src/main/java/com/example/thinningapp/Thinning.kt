package com.example.thinningapp

import android.graphics.Bitmap
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc

class Thinning {
    private val prep = Preprocessing()
    private var originalMat: Mat
    private var thresholdMat: Mat

    init {
        OpenCVLoader.initDebug()
    }

    constructor(bitmap: Bitmap) {
        this.originalMat = prep.deleteBackground(rgb2gray(bitmap), 128, 5)
        this.thresholdMat = Mat()
        Imgproc.threshold(originalMat,
            thresholdMat,
            -1.0,
            1.0,
            Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU)
    }

    private fun rgb2gray(bitmap: Bitmap): Mat {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2GRAY)
        return mat
    }

    private fun neighbours(x: Int, y: Int, mat: Mat): List<Double> {
        val dx = listOf(-1, -1, 0, 1, 1, 1, 0, -1)
        val dy = listOf(0, 1, 1, 1, 0, -1, -1, -1)
        var img = mutableListOf<Double>()
        for (i in 0..7) {
            img.add(mat[x + dx[i], y + dy[i]][0])
        }
        return img
    }

    private fun transitions(neighbours: List<Double>): Int {
        val n = neighbours + neighbours[0]
        return n.dropLast(1).zip(n.drop(1)).count { (n1, n2) -> n1 == 0.0 && n2 == 1.0 }
    }

    fun zhangSuen(): Mat {
        var thinnedMat = thresholdMat.clone()
        val rows = thinnedMat.rows()
        val cols = thinnedMat.cols()
        var changing1 = mutableListOf(listOf(0, 0))
        var changing2 = mutableListOf(listOf(0, 0))
        while (changing1.isNotEmpty() || changing2.isNotEmpty()) {
            // Step 1
            changing1.clear()
            for (x in 1 until rows - 1) {
                for (y in 1 until cols - 1) {
                    val n = neighbours(x, y, thinnedMat)
                    if (thinnedMat[x, y][0] == 1.0 &&
                        n.sum() in 2.0..6.0 &&
                        transitions(n) == 1 &&
                        n[0] * n[2] * n[4] == 0.0 &&
                        n[2] * n[4] * n[6] == 0.0) {
                        changing1.add(listOf(x, y))
                    }
                }
            }
            for ((x, y) in changing1) {
                thinnedMat.put(x, y, 0.0)
            }
            // Step 2
            changing2.clear()
            for (x in 1 until rows - 1) {
                for (y in 1 until cols - 1) {
                    val n = neighbours(x, y, thinnedMat)
                    if (thinnedMat[x, y][0] == 1.0 &&
                        n.sum() in 2.0..6.0 &&
                        transitions(n) == 1 &&
                        n[0] * n[2] * n[6] == 0.0 &&
                        n[0] * n[4] * n[6] == 0.0) {
                        changing2.add(listOf(x, y))
                    }
                }
            }
            for ((x, y) in changing2) {
                thinnedMat.put(x, y, 0.0)
            }
        }
        return thinnedMat
    }
}