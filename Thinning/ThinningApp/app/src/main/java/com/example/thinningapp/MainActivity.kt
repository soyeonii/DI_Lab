package com.example.thinningapp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
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
                    thinnedMat.convertTo(thinnedMat, -1, 255.0)
                    Core.bitwise_not(thinnedMat, thinnedMat)
                    Utils.matToBitmap(thinnedMat, bitmap)
                    findViewById<ImageView>(R.id.imageView).setImageBitmap(bitmap)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}