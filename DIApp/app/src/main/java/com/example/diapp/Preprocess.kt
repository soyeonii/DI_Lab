package com.example.diapp

import android.util.Log
import kotlin.math.*

class Preprocess {
    private val mainActivity = MainActivity.getInstance()
    var linesMaxMin = ArrayList<Float>()    // 전체에서 max, min (원본)
    var lineMaxMin = ArrayList<ArrayList<Float>>()  // 각 라인별 max, min
    var lineLocation = ArrayList<ArrayList<Boolean>>()

    private fun setLinesMaxMin() { //전체에서 max, min
        linesMaxMin.clear()

        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE

        for (line in mainActivity?.lines!!) {
            for (point in line.points) {
                maxX = max(maxX, point.x)
                minX = min(minX, point.x)
                maxY = max(maxY, point.y)
                minY = min(minY, point.y)
            }
        }

        linesMaxMin.add(maxX)
        linesMaxMin.add(minX)
        linesMaxMin.add(maxY)
        linesMaxMin.add(minY)
    }

    fun setLineMaxMin() {//각 라인별 max, min
        lineMaxMin.clear()

        for (line in mainActivity?.lines!!) {
            var maxX = Float.MIN_VALUE
            var maxY = Float.MIN_VALUE
            var minX = Float.MAX_VALUE
            var minY = Float.MAX_VALUE

            for (point in line.points) {
                maxX = max(maxX, point.x)
                minX = min(minX, point.x)
                maxY = max(maxY, point.y)
                minY = min(minY, point.y)
            }

            lineMaxMin.add(arrayListOf(maxX, minX, maxY, minY))
        }
    }

    private fun shiftLines() { //0,0으로 시작점을 옮김
        for (line in mainActivity?.lines!!) {
            for (point in line.points) {
                point.x -= linesMaxMin[1]
                point.y -= linesMaxMin[3]
            }
        }
    }

    fun setLineLocation() {
        lineLocation.clear()

        val width = getWidth() / 3
        val height = getHeight() / 3

        for (line in mainActivity?.allLines!!) {
            var tmp = arrayListOf(false, false, false, false, false, false, false, false, false)

            for (point in line.points) {
                if (width >= point.x) {
                    if (height >= point.y)
                        tmp[0] = true
                    else if (height * 2 >= point.y)
                        tmp[3] = true
                    else
                        tmp[6] = true
                } else if (width * 2 >= point.x) {
                    if (height >= point.y)
                        tmp[1] = true
                    else if (height * 2 >= point.y)
                        tmp[4] = true
                    else
                        tmp[7] = true
                } else {
                    if (height >= point.y)
                        tmp[2] = true
                    else if (height * 2 >= point.y)
                        tmp[5] = true
                    else
                        tmp[8] = true
                }
            }
            lineLocation.add(tmp)
        }
    }

    fun getWidth(): Float {
        return linesMaxMin[0] - linesMaxMin[1]
    }

    fun getHeight(): Float {
        return linesMaxMin[2] - linesMaxMin[3]
    }

    private fun print() {
        Log.d("linesMinX", linesMaxMin[1].toString())
        Log.d("linesMinY", linesMaxMin[3].toString())
        for ((idx, line) in mainActivity?.lines?.withIndex()!!) {
            var s = ""
            for (point in line.points)
                s += "(" + point.x + ", " + point.y + ")"
            Log.d("line", idx.toString() + s)
            Log.d("line$idx", lineMaxMin[idx][0].toString())
            Log.d("line$idx", lineMaxMin[idx][1].toString())
            Log.d("line$idx", lineMaxMin[idx][2].toString())
            Log.d("line$idx", lineMaxMin[idx][3].toString())
        }
    }

    fun run() {
        setLinesMaxMin()
        shiftLines()
    }
}