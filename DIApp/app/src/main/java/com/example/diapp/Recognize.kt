package com.example.diapp

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import java.util.*
import kotlin.collections.ArrayList

class Recognize(private val context: Context) {
    private val mainActivity = MainActivity.getInstance()
    private val preprocess = Preprocess()
    private val draw = Draw(preprocess, context)
    private var images = ArrayList<Bitmap>()
    private val imageSize = arrayListOf(128, 96, 0)
    private var straightLines = ArrayList<ArrayList<Int>>() // 직선
    private var scrawledLines = ArrayList<Line>()   // 이어흘려씀
    private var result = arrayListOf("None", "None", "None", "None")

    init {
        mainActivity?.lines = mainActivity?.allLines?.clone() as ArrayList<Line>
        preprocess.run()
        preprocess.setLineMaxMin()
        preprocess.setLineLocation()
        images = draw.getImages(imageSize[0], imageSize[0], 30)
    }

    private fun init() {
        mainActivity?.allLines = scrawledLines.clone() as ArrayList<Line>
        mainActivity?.lines = mainActivity?.allLines?.clone() as ArrayList<Line>
        mainActivity?.indices = (0 until scrawledLines.size).toList() as ArrayList<Int>
        preprocess.run()
        preprocess.setLineMaxMin()
        images = draw.getImages(imageSize[0], imageSize[0], 30)

        var tmpIndices = ArrayList<Int>()
        for (index in mainActivity?.indices) {
            tmpIndices.add(index)
            Log.d("tmpIndices", tmpIndices.toString())
            draw.saveImage(draw.combineImage(images, tmpIndices, imageSize[0], imageSize[0], imageSize[0]), "hi", index.toString())
        }
    }

    private fun updateLines() {
        mainActivity?.lines?.clear()
        for (i in mainActivity?.indices!!) {
            mainActivity?.lines.add(mainActivity?.allLines[i])
        }
        preprocess.run()
    }

    private fun sortLines(xy: Char): ArrayList<Int> {
        val sortedLines = ArrayList<ArrayList<Int>>()
        val sortedIndex = ArrayList<Int>()

        if (xy == 'x') {    // 각 획의 x 중간 값으로 정렬
            for (index in mainActivity?.indices!!) {
                val maxMin = preprocess.lineMaxMin[index]
                sortedLines.add(arrayListOf(index, ((maxMin[0] + maxMin[1]) / 2).toInt()))
            }

        } else {    // 각 획의 y 중간 값으로 정렬
            for (index in mainActivity?.indices!!) {
                val maxMin = preprocess.lineMaxMin[index]
                sortedLines.add(arrayListOf(index, ((maxMin[2] + maxMin[3]) / 2).toInt()))
            }
        }
        sortedLines.sortByDescending { it[1] }

        for ((index, _) in sortedLines) {
            sortedIndex.add(index)
        }

        return sortedIndex
    }

    private fun getStandardIndices(xy: Char): ArrayList<Int> { //인덱스 반환
        var standardIndices = ArrayList<Int>()
        val ratio = 0.8

        if (xy == 'x') {
            val target = preprocess.getWidth() * ratio
            for (index in mainActivity?.indices!!)
                if (preprocess.lineMaxMin[index][0] >= target)
                    standardIndices.add(index)
        } else {
            val target = preprocess.getHeight() * ratio
            for (index in mainActivity?.indices!!) {
                if (preprocess.lineMaxMin[index][3] >= target)
                    standardIndices.add(index)
            }
        }

        return standardIndices
    }

    private fun setResult(
        resultIndex: Int, indices: ArrayList<Int>, select: String, width: Int, height: Int
    ) {
        val image = draw.combineImage(images, indices, imageSize[0], width, height)
        draw.saveImage(image, "88", "$resultIndex")
        result[resultIndex] = Model(context).run(image, select)
    }

    private fun getTmpResult(
        standardIndices: ArrayList<Int>, candidateIndices: ArrayList<Int>,
        select1: String, select2: String, target: String,
        width1: Int, height1: Int, width2: Int, height2: Int
    ): ArrayList<String> {
        val tmpResult = arrayListOf("None", "None")
        for (i in candidateIndices.indices) {
            if (i > 0) {
                standardIndices.add(candidateIndices[i - 1])
            }
            Log.d("i", i.toString())
            Log.d("standardIndex", standardIndices.toString())
            Log.d("candidateIndices", (candidateIndices - standardIndices.toSet()).toString())
            val image1 = draw.combineImage(images, standardIndices, imageSize[0], width1, height1)
            val image2 = draw.combineImage(
                images, candidateIndices - standardIndices.toSet(), imageSize[0], width2, height2
            )
            draw.saveImage(image1, i.toString(), "0")
            draw.saveImage(image2, i.toString(), "1")
            val result1 = Model(context).run(image1, select1)
            val result2 = Model(context).run(image2, select2)
            Log.d("first", result1)
            Log.d("second", result2)

            if (result1 != "None" && result2 != "None") { //None은 쓰레기 값
                when (target) {
                    result2 -> {    // case와 종성 분류 시 (case1)
                        tmpResult[0] = result1
                        tmpResult[1] = result2
                        mainActivity?.indices!! -= standardIndices.toSet()
                        if (straightLines.isNotEmpty()) {
                            var tmpStraightLines = ArrayList<ArrayList<Int>>()
                            for (straightLine in straightLines) {
                                if (!standardIndices.contains(straightLine[0]))
                                    tmpStraightLines.add(straightLine)
                            }
                            straightLines = tmpStraightLines.clone() as ArrayList<ArrayList<Int>>
                        }
                        break
                    }
                    "consonantVowel" -> {   // 자음과 모음 분류 시
                        tmpResult[0] = result1
                        tmpResult[1] = result2
                        break
                    }
                }
            }
        }

        return tmpResult
    }

    private fun setStraightLines() { //방향성
        for (i in mainActivity?.indices!!) {
            var diff = ArrayList<ArrayList<Float>>()
            var count = arrayListOf(0, 0, 0, 0, 0)

            Log.d("index", i.toString())

            for (j in 1 until mainActivity?.allLines?.get(i)?.points!!.size) {
                diff.add(
                    arrayListOf(
                        mainActivity?.allLines[i].points[j].x - mainActivity?.allLines[i].points[j - 1].x,
                        mainActivity?.allLines[i].points[j].y - mainActivity?.allLines[i].points[j - 1].y
                    )
                )
            }

            for (d in diff) { // 4 1 3 2
                count[getDistance(d)]++
            }

            Log.d("count[1]", count[1].toString())
            Log.d("count[2]", count[2].toString())
            Log.d("count[3]", count[3].toString())
            Log.d("count[4]", count[4].toString())
            Log.d("diff.size", diff.size.toString())

            if (count[1] / diff.size.toFloat() >= 0.7) { //float으로 소수점
                Log.d("직선 방향 1, 인덱스 : ", i.toString())
                straightLines.add(arrayListOf(i, 1))
            } else if (count[2] / diff.size.toFloat() >= 0.7) {
                Log.d("직선 방향 2, 인덱스 : ", i.toString())
                straightLines.add(arrayListOf(i, 1))
            } else if (count[3] / diff.size.toFloat() >= 0.7) {
                Log.d("직선 방향 3, 인덱스 : ", i.toString())
                straightLines.add(arrayListOf(i, 2))
            } else if (count[4] / diff.size.toFloat() >= 0.7) {
                Log.d("직선 방향 4, 인덱스 : ", i.toString())
                straightLines.add(arrayListOf(i, 2))
            }
        }
    }

    private fun setScrawledLines() {
        for (i in mainActivity?.indices!!) {
            var stack = Stack<Int>()
            var diff = ArrayList<ArrayList<Float>>()
            var allPoints = arrayListOf(mainActivity?.allLines[i].points[1])
            var tmpPoints = ArrayList<Point>()

            Log.d("index", i.toString())

            for (j in 2 until mainActivity?.allLines[i].points.size) {
                val line = mainActivity?.allLines[i]
                if (line.points[j - 1].check) {
                    diff.add(
                        arrayListOf(
                            line.points[j].x - line.points[j - 1].x,
                            line.points[j].y - line.points[j - 1].y
                        )
                    )
                    allPoints.add(Point(line.points[j].x, line.points[j].y, true))
                }
            }

            for ((i, d) in diff.withIndex()) {
                var distance = getDistance(d)
                if (stack.isEmpty()) {
                    stack.push(distance)
                } else if (stack.peek() != distance) {  // 방향이 바뀌었다면
                    if (tmpPoints.size < 4) {   // 너무 짧게 끊기는 거 방지
                        stack.push(distance)
                    } else {
                        stack.clear()
                        scrawledLines.add(Line(tmpPoints.clone() as ArrayList<Point>))
                        tmpPoints.clear()
                        tmpPoints.add(allPoints[i - 1]) // 선 띄어지는거 방지 전에 점 추가
                    }
                }
                tmpPoints.add(allPoints[i])
            }
            scrawledLines.add(Line(tmpPoints.clone() as ArrayList<Point>))
        }
    }

    private fun getDistance(d: ArrayList<Float>): Int {
        return if (d[1] == 0f) {
            if (d[0] >= 0) 1 else 2
        } else if (d[0] == 0f) {
            if (d[1] >= 0) 3 else 4
        } else {
            val slope = d[1] / d[0]
            if (d[0] > 0) {
                if (slope >= 1) 3 else if (-1 <= slope && slope < 1) 1 else 4
            } else {
                if (slope >= 1) 4 else if (-1 <= slope && slope < 1) 2 else 3
            }
        }
    }

    private fun getSRLength(index: Int): ArrayList<Float> {   // 세로
        val maxX = preprocess.lineMaxMin[index][0]
        val minX = preprocess.lineMaxMin[index][1]
        val maxY = preprocess.lineMaxMin[index][2]
        val minY = preprocess.lineMaxMin[index][3]
        val avgX = (maxX + minX) / 2
        val dx = preprocess.getWidth()
        val dy = preprocess.getHeight()

        return arrayListOf(avgX + dx, avgX - dx, maxY + dy, minY - dy)
    }

    private fun getSRWidth(index: Int, case: Int): ArrayList<Float> {   // 가로
        val maxX = preprocess.lineMaxMin[index][0]
        val minX = preprocess.lineMaxMin[index][1]
        val maxY = preprocess.lineMaxMin[index][2]
        val minY = preprocess.lineMaxMin[index][3]
        var dx: Float
        var dy: Float

        when (case) {
            1 -> {
                dx = (preprocess.getWidth()) / 6
                dy = (preprocess.getHeight()) / 15
            }
            2 -> {
                dx = 0f
                dy = (preprocess.getHeight()) / 6
            }
            else -> {
                dx = 0f
                dy = 0f
            }
        }

        return arrayListOf(maxX + dx, minX - dx, maxY + dy, minY - dy)
    }

    private fun case1() {
        Log.d("START", "----------------case1----------------")

        if (straightLines.isNotEmpty()) {
            Log.d("TYPE", "흘려쓰지 않음")
            var standardX = preprocess.getWidth() / 4
            var tmpIndices = ArrayList<Int>()
            for (index in mainActivity?.indices!!) {
                if (preprocess.lineMaxMin[index][1] < standardX && standardX < preprocess.lineMaxMin[index][0])
                    tmpIndices.add(index)
            }

            var minY = Float.MAX_VALUE
            var baseMaxX = 0f
            for (index in tmpIndices) {
                if (preprocess.lineMaxMin[index][3] < minY) {
                    minY = preprocess.lineMaxMin[index][3]
                    baseMaxX = preprocess.lineMaxMin[index][0] + preprocess.getWidth() / 8
                }
            }

            var minX = Float.MAX_VALUE
            var baseIndex = 0
            for (straightLine in straightLines) {
                if (straightLine[1] == 2) {
                    val avgX =
                        (preprocess.lineMaxMin[straightLine[0]][0] + preprocess.lineMaxMin[straightLine[0]][1]) / 2
                    if (baseMaxX < avgX && avgX < minX) {
                        minX = avgX
                        baseIndex = straightLine[0]
                    }
                }
            }

            var SRLength = getSRLength(baseIndex)
            var vowelIndices = ArrayList<Int>()
            vowelIndices.add(baseIndex)
            val baseX = preprocess.lineMaxMin[baseIndex][0]
            for (straightLine in straightLines) {
                val maxX = preprocess.lineMaxMin[straightLine[0]][0]
                if (maxX > baseX)
                    vowelIndices.add(straightLine[0])
                else {
                    if (straightLine[1] == 1) {
                        minX = preprocess.lineMaxMin[straightLine[0]][1]
                        if (minX > standardX && SRLength[1] <= maxX) {
                            vowelIndices.add(straightLine[0])
                        }
                    }
                }
            }

            val consonantIndices =
                (mainActivity?.indices?.minus(vowelIndices.toSet())) as ArrayList<Int>
            Log.d("자음", consonantIndices.toString())
            Log.d("모음", vowelIndices.toString())

            setResult(0, consonantIndices, "consonant1", imageSize[1], imageSize[1])
            setResult(1, vowelIndices, "vowel14", imageSize[1], imageSize[0])

            if (result[0] != "None" && result[1] != "None") {
                Log.d("결과", result.toString())
                return
            }
        }

        Log.d("TYPE", "흘려씀")
        // 초성 후보 인덱스
        val standardIndices = getStandardIndices('x')
        Log.d("standardIndices", standardIndices.toString())

        // 초성 후보에 더할 인덱스
        val candidateIndices = (sortLines('x') - standardIndices.toSet()) as ArrayList<Int>
        Log.d("candidateIndices", candidateIndices.toString())

        val tmpResult = getTmpResult(
            standardIndices,
            candidateIndices,
            "vowel14",
            "consonant1",
            "consonantVowel",
            imageSize[1],
            imageSize[0],
            imageSize[1],
            imageSize[1]
        )

        result[0] = tmpResult[1]
        result[1] = tmpResult[0]
    }

    private fun case2() { // 하단부터
        Log.d("START", "----------------case2----------------")

        if (straightLines.isNotEmpty()) {
            Log.d("TYPE", "흘려쓰지 않음")
            var maxY = Float.MIN_VALUE
            var baseIndex = 0
            for (straightLine in straightLines) {
                if (straightLine[1] == 1) {
                    if (preprocess.lineLocation[straightLine[0]][3] || preprocess.lineLocation[straightLine[0]][6]) {
                        if (preprocess.lineMaxMin[straightLine[0]][3] > maxY) {
                            maxY = preprocess.lineMaxMin[straightLine[0]][3]
                            baseIndex = straightLine[0]
                        }
                    }
                }
            }

            val SRWidth = getSRWidth(baseIndex, 2)
            val standardY = preprocess.getHeight() / 10
            var consonantIndices = ArrayList<Int>()
            consonantIndices.add(baseIndex)
            for (straightLine in straightLines) {
                if (straightLine[1] == 2) {
                    val maxY = preprocess.lineMaxMin[straightLine[0]][2]
                    val minY = preprocess.lineMaxMin[straightLine[0]][3]
                    if (minY > standardY) {
                        if (SRWidth[3] <= minY && minY <= SRWidth[2] || SRWidth[3] <= maxY && maxY <= SRWidth[2])
                            consonantIndices.add(straightLine[0])
                    }
                }
            }

            val vowelIndices =
                (mainActivity?.indices!! - consonantIndices.toSet()) as ArrayList<Int>
            Log.d("자음", vowelIndices.toString())
            Log.d("모음", consonantIndices.toString())

            setResult(0, vowelIndices, "consonant1", imageSize[1], imageSize[1])
            setResult(1, consonantIndices, "vowel25", imageSize[0], imageSize[1])

            if (result[0] != "None" && result[1] != "None") {
                Log.d("결과", result.toString())
                return
            }
        }

        Log.d("TYPE", "흘려씀")
        // 초성 후보 인덱스
        val standardIndices = getStandardIndices('y')
        Log.d("standardIndices", standardIndices.toString())

        // 초성 후보에 더할 인덱스
        val candidateIndices = (sortLines('y') - standardIndices.toSet()) as ArrayList<Int>
        Log.d("candidateIndices", candidateIndices.toString())

        val tmpResult = getTmpResult(
            standardIndices,
            candidateIndices,
            "vowel25",
            "consonant1",
            "consonantVowel",
            imageSize[0],
            imageSize[1],
            imageSize[1],
            imageSize[1]
        )

        result[0] = tmpResult[1]
        result[1] = tmpResult[0]
    }

    private fun case3() { // 하단부터
        Log.d("START", "----------------case3----------------")

        Log.d("TYPE", "흘려씀")
        // 초성 후보 인덱스
        val standardIndices = getStandardIndices('x')
        Log.d("standardIndices", standardIndices.toString())

        // 초성 후보에 더할 인덱스
        val candidateIndices = (sortLines('x') - standardIndices.toSet()) as ArrayList<Int>
        Log.d("candidateIndices", candidateIndices.toString())

        result[2] = getTmpResult(
            standardIndices,
            candidateIndices,
            "vowel14",
            "case2None",
            "case2",
            imageSize[1],
            imageSize[0],
            imageSize[0],
            imageSize[0]
        )[0]

        updateLines()
        case2()
    }

    private fun case4() {
        Log.d("START", "----------------case4----------------")

        if (straightLines.isNotEmpty()) {
            Log.d("TYPE", "흘려쓰지 않음")
            var maxX = Float.MIN_VALUE
            var baseIndex = ArrayList<Int>()
            for (straightLine in straightLines) {
                if (straightLine[1] == 2) {
                    val standardY = preprocess.getHeight() / 8 * 7
                    if (preprocess.lineMaxMin[straightLine[0]][2] < standardY && maxX < preprocess.lineMaxMin[straightLine[0]][1]) {
                        maxX = preprocess.lineMaxMin[straightLine[0]][1]
                        baseIndex = straightLine
                    }
                }
            }

            val standardY = preprocess.getHeight() / 8 * 7
            var vowelIndices = ArrayList<Int>()
            vowelIndices.add(baseIndex[0])
            do {
                var SRLengthOrWidth =
                    if (baseIndex[1] == 2) getSRLength(baseIndex[0])
                    else getSRWidth(baseIndex[0], 1)

                var tmpIndex = -1
                for ((i, straightLine) in straightLines.withIndex()) {
                    if (!vowelIndices.contains(straightLine[0])) {
                        if (preprocess.lineMaxMin[straightLine[0]][2] < standardY) {
                            if (baseIndex[1] == 1) {    // base 가로 직선
                                if (SRLengthOrWidth[2] < preprocess.lineMaxMin[straightLine[0]][2] && straightLine[1] == 2 && preprocess.lineMaxMin[straightLine[0]][0] >= SRLengthOrWidth[1]) {
                                    vowelIndices.add(straightLine[0])
                                    tmpIndex = i
                                }
                            } else {    // base 세로 직선
                                if ((straightLine[1] == 1 && preprocess.lineMaxMin[straightLine[0]][2] < preprocess.lineMaxMin[baseIndex[0]][2] / 15 * 14) || straightLine[1] == 2) {
                                    if ((SRLengthOrWidth[1] <= preprocess.lineMaxMin[straightLine[0]][1] && preprocess.lineMaxMin[straightLine[0]][1] <= SRLengthOrWidth[0]) || (SRLengthOrWidth[1] <= preprocess.lineMaxMin[straightLine[0]][0] && preprocess.lineMaxMin[straightLine[0]][0] <= SRLengthOrWidth[0])) {
                                        vowelIndices.add(straightLine[0])
                                        tmpIndex = i
                                    }
                                }
                            }
                        }
                    }
                }
                if (tmpIndex != -1)
                    baseIndex = straightLines[tmpIndex]
            } while (tmpIndex != -1)

            val consonant1Indices = ArrayList<Int>()
            val consonant2Indices = ArrayList<Int>()
            for (index in mainActivity?.indices?.minus(vowelIndices.toSet())!!)
                if (index < vowelIndices[0])
                    consonant1Indices.add(index)
                else
                    consonant2Indices.add(index)
            Log.d("초성", consonant1Indices.toString())
            Log.d("중성", vowelIndices.toString())
            Log.d("종성", consonant2Indices.toString())

            setResult(0, consonant1Indices, "consonant1", imageSize[1], imageSize[1])
            setResult(1, vowelIndices, "vowel14", imageSize[1], imageSize[0])
            setResult(2, consonant2Indices, "consonant1", imageSize[1], imageSize[1])

            if (result[0] != "None" && result[1] != "None" && result[2] != "None") {
                Log.d("결과", result.toString())
                return
            }
        }

        Log.d("TYPE", "흘려씀")
        // 초성 후보 인덱스
        val standardIndices = getStandardIndices('y')
        Log.d("standardIndices", standardIndices.toString())

        // 초성 후보에 더할 인덱스
        val candidateIndices = (sortLines('y') - standardIndices.toSet()) as ArrayList<Int>
        Log.d("candidateIndices", candidateIndices.toString())

        result[2] = getTmpResult(
            standardIndices,
            candidateIndices,
            "consonant1",
            "case1None",
            "case1",
            imageSize[1],
            imageSize[1],
            imageSize[0],
            imageSize[0]
        )[0]

        if (result[2] != "None") {
            updateLines()
            case1()
        }
    }

    private fun case5() {
        Log.d("START", "----------------case5----------------")

        if (straightLines.isNotEmpty()) {
            Log.d("TYPE", "흘려쓰지 않음")
            var maxY = Float.MIN_VALUE
            val standardY = preprocess.getHeight() / 10 * 3
            for (index in mainActivity?.indices!!) {
                if (preprocess.lineLocation[index][0] || preprocess.lineLocation[index][1]) {
                    if (preprocess.lineMaxMin[index][3] < standardY && preprocess.lineMaxMin[index][2] > maxY)
                        maxY = preprocess.lineMaxMin[index][2]
                }
            }

            maxY += preprocess.getHeight() / 18
            var minY = Float.MAX_VALUE
            var baseIndex = 0
            for (straightLine in straightLines) {
                val avgY =
                    (preprocess.lineMaxMin[straightLine[0]][2] + preprocess.lineMaxMin[straightLine[0]][3]) / 2
                Log.d("maxY", maxY.toString())
                Log.d("avgY", avgY.toString())
                Log.d("minY", minY.toString())
                if (maxY < avgY && avgY < minY) {
                    minY = avgY
                    baseIndex = straightLine[0]
                }
            }

            Log.d("baseIndex", baseIndex.toString())

            val SRWidth = getSRWidth(baseIndex, 2)
            var vowelIndices = ArrayList<Int>()
            vowelIndices.add(baseIndex)
            for (straightLine in straightLines) {
                if (!vowelIndices.contains(straightLine[0])) {
                    if (preprocess.lineMaxMin[straightLine[0]][3] > standardY && straightLine[1] == 2) {
                        if ((SRWidth[3] <= preprocess.lineMaxMin[straightLine[0]][3] && preprocess.lineMaxMin[straightLine[0]][3] <= SRWidth[2]) || (SRWidth[3] <= preprocess.lineMaxMin[straightLine[0]][2] && preprocess.lineMaxMin[straightLine[0]][2] <= SRWidth[2]))
                            vowelIndices.add(straightLine[0])
                    }
                }
            }

            val consonant1Indices = ArrayList<Int>()
            val consonant2Indices = ArrayList<Int>()
            for (index in mainActivity?.indices.minus(vowelIndices.toSet())!!)
                if (preprocess.lineMaxMin[index][2] < preprocess.lineMaxMin[baseIndex][2])
                    consonant1Indices.add(index)
                else
                    consonant2Indices.add(index)
            Log.d("초성", consonant1Indices.toString())
            Log.d("중성", vowelIndices.toString())
            Log.d("종성", consonant2Indices.toString())

            setResult(0, consonant1Indices, "consonant1", imageSize[1], imageSize[1])
            setResult(1, vowelIndices, "vowel25", imageSize[0], imageSize[1])
            setResult(2, consonant2Indices, "consonant1", imageSize[1], imageSize[1])

            if (result[0] != "None" && result[1] != "None" && result[2] != "None") {
                Log.d("결과", result.toString())
                return
            }
        }

        Log.d("TYPE", "흘려씀")
        // 초성 후보 인덱스
        val standardIndices = getStandardIndices('y')
        Log.d("standardIndices", standardIndices.toString())

        // 초성 후보에 더할 인덱스
        val candidateIndices = (sortLines('y') - standardIndices.toSet()) as ArrayList<Int>
        Log.d("candidateIndices", candidateIndices.toString())

        result[2] = getTmpResult(
            standardIndices,
            candidateIndices,
            "consonant1",
            "case2None",
            "case2",
            imageSize[1],
            imageSize[1],
            imageSize[0],
            imageSize[0]
        )[0]

        updateLines()
        case2()
    }

    private fun case6() {
        Log.d("START", "----------------case6----------------")

        Log.d("TYPE", "흘려씀")
        // 초성 후보 인덱스
        val standardIndices = getStandardIndices('y')
        Log.d("standardIndices", standardIndices.toString())

        // 초성 후보에 더할 인덱스
        val candidateIndices = (sortLines('y') - standardIndices.toSet()) as ArrayList<Int>
        Log.d("candidateIndices", candidateIndices.toString())

        result[3] = getTmpResult(
            standardIndices,
            candidateIndices,
            "consonant1",
            "case3None",
            "case3",
            imageSize[1],
            imageSize[1],
            imageSize[0],
            imageSize[0]
        )[0]

        updateLines()
        case3()
    }

    private fun printResult() {
        for (i in 0 until result.size) {
            if (result[i] != "None")
                Log.d("$i result", result[i])
        }
    }

    fun getResult(case: String): ArrayList<String> {    // 수정 필요!!
        setStraightLines()
        if (straightLines.isEmpty()) {
            setScrawledLines()
            init()
        }
        when (case) {
            "case1" -> case1()
            "case2" -> case2()
            "case3" -> case3()
            "case4" -> case4()
            "case5" -> case5()
            "case6" -> case6()
        }
        printResult()
        return result
    }

    fun caseCheck(): String {
        val caseImage =
            draw.combineImage(
                images,
                (0 until images.size).toList(),
                imageSize[0],
                imageSize[0],
                imageSize[0]
            )
        draw.saveImage(caseImage, "case", "case")
        return Model(context).run(caseImage, "case")
    }

    fun consonantVowelData() {
        images = draw.getImages(imageSize[0], imageSize[0], imageSize[0])
        draw.saveImage(
            draw.combineImage(
                images,
                (0 until images.size).toList(),
                imageSize[0],
                imageSize[0],
                imageSize[0]
            ), "case", "case"
        )
    }
}