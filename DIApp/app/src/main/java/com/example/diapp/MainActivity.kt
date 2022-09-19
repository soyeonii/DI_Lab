package com.example.diapp

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.net.Socket

class MainActivity : AppCompatActivity() {
    private var context: Context = this@MainActivity
    var points = ArrayList<Point>()//점 모음 (사용자 필기 시)
    var allLines = ArrayList<Line>()
    var lines = ArrayList<Line>()
    var indices = ArrayList<Int>()

    init {
        instance = this
    }

    companion object {
        private var instance: MainActivity? = null
        fun getInstance(): MainActivity? {
            return instance
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val myView = MyView(this)
        var showResult = findViewById<TextView>(R.id.showResult)
        findViewById<LinearLayout>(R.id.canvas).addView(myView)

        fun clear() {
            points.clear()
            allLines.clear()
            lines.clear()
            indices.clear()
            myView.invalidate()
        }

        findViewById<Button>(R.id.btn_start).setOnClickListener {
            clear()
        }

        findViewById<Button>(R.id.btn_save).setOnClickListener {
            for (folder in File("/data/data/com.example.diapp").listFiles()) {
                if (folder.toString().split('/')[4].startsWith("app")) {
                    for (file in folder.listFiles()) {
                        Log.d("file_directory", file.toString())
                        var bitmap = BitmapFactory.decodeFile(file.toString())
                        var baos = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
                        var data = Base64.encodeToString(baos.toByteArray(), 0)
                        SocketThread(data).start()
                        file.delete()
                    }
                    folder.delete()
                }
            }
            Controller(context).run()
            clear()
        }

        findViewById<Button>(R.id.btn_submit).setOnClickListener {
            if (allLines.isNotEmpty()) {
                for (folder in File("/data/data/com.example.diapp").listFiles()) {
                    if (folder.toString().split('/')[4].startsWith("app")) {
                        for (file in folder.listFiles()) {
                            file.delete()
                        }
                        folder.delete()
                    }
                }
                showResult.text = "결과: ${Controller(context).run()}"
                clear()
            }
        }
    }

    inner class SocketThread(private var data: String) : Thread() {
        override fun run() {
            try {
                var socket = Socket("172.30.1.19", 9999)

                var output = socket.getOutputStream()
                var dos = DataOutputStream(output)

                dos.writeBytes(data)

                socket.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    inner class MyView(context: Context?) : View(context) {
        private var tmpPoints = ArrayList<Point>()//점 모음 (line 나눌 시, 중간에 초기화)

        override fun onDraw(canvas: Canvas) {
            val paint = Paint()
            paint.strokeWidth = 15F
            paint.isAntiAlias = true
            for (i in 1 until points.count()) {
                if (points[i].check)
                    canvas.drawLine(
                        points[i - 1].x,
                        points[i - 1].y,
                        points[i].x,
                        points[i].y,
                        paint
                    )
            }
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(event: MotionEvent): Boolean {
            val x = event.x
            val y = event.y

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    points.add(Point(x, y, false))
                    points.add(Point(x, y, true))
                    tmpPoints.add(Point(x, y, false))
                    tmpPoints.add(Point(x, y, true))
                }
                MotionEvent.ACTION_MOVE -> {
                    points.add(Point(x, y, true))
                    tmpPoints.add(Point(x, y, true))
                }
                MotionEvent.ACTION_UP -> {
                    indices.add(indices.size)
                    Log.d("indices", indices.toString())
                    allLines.add(Line(tmpPoints.clone() as ArrayList<Point>))
                    tmpPoints.clear()
                }
            }

            invalidate()
            return true
        }
    }
}
