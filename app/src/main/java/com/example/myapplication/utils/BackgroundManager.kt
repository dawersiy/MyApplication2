package com.example.myapplication.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object BackgroundManager {
    private const val BACKGROUND_FILE_NAME = "background_image.jpg"

    fun saveBackground(context: Context, bitmap: Bitmap) {
        try {
            val file = File(context.filesDir, BACKGROUND_FILE_NAME)
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            outputStream.flush()
            outputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadBackground(context: Context): Bitmap? {
        try {
            val file = File(context.filesDir, BACKGROUND_FILE_NAME)
            if (file.exists()) {
                return BitmapFactory.decodeFile(file.absolutePath)
            } else {
                // 使用默认背景图片
                return try {
                    BitmapFactory.decodeResource(context.resources, com.example.myapplication.R.drawable.background_image)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun clearBackground(context: Context) {
        try {
            val file = File(context.filesDir, BACKGROUND_FILE_NAME)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
