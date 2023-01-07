package com.example.thinningapp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import org.opencv.android.OpenCVLoader
import org.opencv.core.Core
import org.opencv.imgproc.Imgproc

class MainActivity : AppCompatActivity() {
    private val OPEN_GALLERY = 1

    init {
        OpenCVLoader.initDebug()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<Button>(R.id.button).setOnClickListener { openGallery() }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.setType("image/*")
        startActivityForResult(intent, OPEN_GALLERY)
    }

    @Override
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == OPEN_GALLERY) {
                var currentImageUrl: Uri? = data?.data
                try {
                    val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, currentImageUrl)
                    val thinning = Thinning(bitmap)
                    var thinnedMat = thinning.zhangSuen()
//                    Log.d("before", thinnedMat[0, 0][0].toString())
                    Core.bitwise_not(thinnedMat, thinnedMat)
                    Imgproc.threshold(thinnedMat, thinnedMat, 0.5, 1.0,Imgproc.THRESH_BINARY)
//                    Log.d("after", thinnedMat[0, 0][0].toString())
                    val prep = Preprocessing()
                    val lines = prep.divide(prep.simplify(thinnedMat))
                    Log.d("lines", lines.size.toString())
                    val draw = Draw()
//                    val image = draw.combineImage(draw.getImages(lines))
//                    thinnedMat.convertTo(thinnedMat, -1, 255.0)
//                    Utils.matToBitmap(thinnedMat, bitmap)
                    findViewById<ImageView>(R.id.imageView).setImageBitmap(draw.getImages(lines)[0])
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}