package com.example.thinningapp

import android.util.Log
import org.opencv.android.OpenCVLoader
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.util.*
import kotlin.collections.ArrayList

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

        loop@ for (i in mat.rows() - 1 downTo 0) {
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

        loop@ for (i in mat.cols() - 1 downTo 0) {
            for (j in 0 until mat.rows()) {
                if (mat[j, i][0] < 1) {
                    maxX = i
                    break@loop
                }
            }
        }

        val roi = Rect(minX - padding,
            minY - padding,
            maxX - minX + padding * 2,
            maxY - minY + padding * 2)
        val matRoi = Mat(mat, roi)
        Imgproc.resize(matRoi, matRoi, Size(size.toDouble(), size.toDouble()))

        return matRoi
    }

    fun simplify(mat: Mat): ArrayList<Point> {
        val size = 2  // frame 크기
        val space = 3  // point 최소 간격
        var points = ArrayList<Point>()
        for (i in 0 until 128 step size) {
            for (j in 0 until 128 step size) {
                val subArray = mat.submat(Range(i, i + size - 1), Range(j, j + size - 1))
                val mask = Mat.zeros(subArray.size(), CvType.CV_8UC1)
                Core.inRange(subArray, Scalar(0.0), Scalar(0.0), mask)
                if (Core.countNonZero(mask) > 0) {
                    val x = i + mask.get(0, 0)[0].toInt()
                    val y = j + mask.get(1, 0)[0].toInt()
                    val spacingMask = Mat.zeros(mat.submat(Range(x - space, x + space - 1),
                        Range(y - space, y + space - 1)).size(), CvType.CV_8UC1)
                    Core.inRange(mat.submat(Range(x - space, x + space - 1),
                        Range(y - space, y + space - 1)), Scalar(2.0), Scalar(2.0), spacingMask)
                    if (Core.countNonZero(spacingMask) == 0) {
                        mat.put(x, y, 2.0)
                        points.add(Point(x.toFloat(), y.toFloat(), true))
                    }
                }
            }
        }
        Log.d("point size", points.size.toString())
        return points
    }

    fun divide(points: ArrayList<Point>): ArrayList<Line> {
        val size = 8
        val lines = ArrayList<Line>()
        val stack = ArrayList<Point>()
        val check = Array(128) { BooleanArray(128) }
        var quadrant = 0
        fun bfs(start: Point) {
            check[start.x.toInt()][start.y.toInt()] = true
            stack.add(start)
            val queue = LinkedList<Point>()
            queue.add(start)
            while (queue.isNotEmpty()) {
                val point = queue.poll()
                val surroundPoints = ArrayList<Point>()
                for (j in (point.y - size).toInt() until (point.y + size).toInt()) {
                    for (i in (point.x - size).toInt() until (point.x + size).toInt()) {
                        if (Point(i.toFloat(), j.toFloat(), true) in points && !check[i][j]) {
                            if (quadrant == 0 || quadrant == getQuadrant(Pair(i - stack.last().x,
                                    j - stack.last().y))
                            ) {
                                if (quadrant == 0) {
                                    quadrant =
                                        getQuadrant(Pair(i - stack.last().x, j - stack.last().y))
                                }
                                surroundPoints.add(Point(i.toFloat(), j.toFloat(), true))
                            }
                        }
                    }
                }
                if (surroundPoints.isNotEmpty()) {
                    var index = 0
                    if (quadrant == 2 || quadrant == 4) {
                        surroundPoints.sortBy { getSlope(stack.last(), it) }
                        if (getSlope(stack.last(), surroundPoints.last()) == 0.0) {
                            index = -1
                        }
                    } else {
                        surroundPoints.sortByDescending { getSlope(stack.last(), it) }
                    }
                    check[surroundPoints[index].x.toInt()][surroundPoints[index].y.toInt()] = true
                    stack.add(surroundPoints[index])
                    queue.add(surroundPoints[index])
                }
            }
        }
        for (i in points.indices) {
            val point = points[i]
            if (!check[point.x.toInt()][point.y.toInt()]) {
                bfs(point)
                lines.add(Line(stack.clone() as ArrayList<Point>))
                Log.d("line add", lines.toString())
                stack.clear()
                quadrant = 0
            }
        }
        return lines
    }

    private fun getSlope(p1: Point, p2: Point): Double {
        return if (p1.y != p2.y) {
            kotlin.math.abs((p1.x - p2.x).toDouble() / (p1.y - p2.y).toDouble())
        } else {
            0.0
        }
    }

    private fun getQuadrant(diff: Pair<Float, Float>): Int {
        val x = diff.first
        val y = diff.second
        return if (x == 0f) {
            if (y >= 0) 3 else 1
        } else if (y == 0f) {
            if (x >= 0) 4 else 2
        } else {
            when {
                x > 0 && y > 0 -> 1
                x > 0 && y < 0 -> 4
                x < 0 && y > 0 -> 2
                else -> 3
            }
        }
    }
}