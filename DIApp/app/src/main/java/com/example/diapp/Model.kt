package com.example.diapp

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.util.Pair
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.Tensor
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class Model(private var context: Context) {
    private lateinit var interpreter: Interpreter
    private var modelOutputClasses = 0
    private lateinit var bitmap: Bitmap

    private fun getModel(m: String): String? {
        val model = mapOf(
            "case" to "caseModel.tflite",
            "case1None" to "case1NoneModel.tflite",
            "case2None" to "case2NoneModel.tflite",
            "case3None" to "case3NoneModel.tflite",
            "consonant1" to "consonant1Model.tflite",
            "consonant2" to "consonant2Model.tflite",
            "vowel14" to "vowel14Model.tflite",
            "vowel25" to "vowel25Model.tflite",
        )
        return model[m]
    }

    private fun getLabel(m: String): ArrayList<String>? {
        val case = arrayListOf("case1", "case2", "case3", "case4", "case5", "case6")
        val case1None = arrayListOf("case1", "None")
        val case2None = arrayListOf("case2", "None")
        val case3None = arrayListOf("case3", "None")
        val consonant1 = arrayListOf(
            "ㄱ", "ㄴ", "ㄷ", "ㄹ", "ㅁ", "ㅂ", "ㅅ", "ㅇ", "ㅈ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ", "None"
        )
        val consonant2 = arrayListOf(
            "ㄱ", "ㄴ", "ㄷ", "ㄹ", "ㅁ", "ㅂ", "ㅅ", "ㅇ", "ㅈ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ", "None"
        )
        val vowel14 = arrayListOf("ㅏ", "ㅑ", "ㅓ", "ㅕ", "ㅐ", "ㅔ", "ㅣ", "None")
        val vowel25 = arrayListOf("ㅗ", "ㅛ", "ㅜ", "ㅠ", "ㅡ", "None")
        val label = mapOf(
            "case" to case,
            "case1None" to case1None,
            "case2None" to case2None,
            "case3None" to case3None,
            "consonant1" to consonant1,
            "consonant2" to consonant2,
            "vowel14" to vowel14,
            "vowel25" to vowel25
        )
        return label[m]
    }

    private fun loadModelFile(modelName: String): ByteBuffer {
        val am = context.assets
        val afd = am.openFd(modelName)
        val fis = FileInputStream(afd.fileDescriptor)
        val fc = fis.channel
        val startOffset = afd.startOffset
        val declaredLength = afd.declaredLength
        return fc.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun init(modelName: String) {
        val model = loadModelFile(modelName)
        model.order(ByteOrder.nativeOrder())
        interpreter = Interpreter(model)
        initModelShape()
    }

    private fun initModelShape() {
        val outputTensor: Tensor = interpreter.getOutputTensor(0)
        val outputShape: IntArray = outputTensor.shape()
        modelOutputClasses = outputShape[1]
    }

    private fun convertBitmapToGrayByteBuffer(): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(bitmap.byteCount)
        byteBuffer.order(ByteOrder.nativeOrder())
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(
            pixels, 0, bitmap.width, 0, 0, bitmap.width,
            bitmap.height
        )
        for (pixel in pixels) {
            val r = pixel shr 16 and 0xFF
            val g = pixel shr 8 and 0xFF
            val b = pixel shr 0xFF
            val avgPixelValue = (r + g + b) / 3.0f
            val normalizedPixelValue = avgPixelValue / 255.0f
            byteBuffer.putFloat(normalizedPixelValue)
        }
        return byteBuffer
    }

    private fun classify(): Pair<Int, Float> {
        val buffer = convertBitmapToGrayByteBuffer()
        val result = Array(1) {
            FloatArray(
                modelOutputClasses
            )
        }
        interpreter.run(buffer, result)
        return argmax(result[0])
    }

    private fun argmax(array: FloatArray): Pair<Int, Float> {
        var argmax = 0
        var max = array[0]
        for (i in 1 until array.size) {
            val f = array[i]
            if (f > max) {
                argmax = i
                max = f
            }
        }
        return Pair(argmax, max)
    }

    fun run(bitmap: Bitmap, select: String): String { //비트맵을 받아서 처리
        this.bitmap = bitmap
        val model = getModel(select)

        try {
            if (model != null) {
                init(model)
            }
        } catch (ioe: IOException) {
            Log.d("DigitClassifier", "Model is None")
        }

        return getLabel(select)!![classify().first]
    }
}