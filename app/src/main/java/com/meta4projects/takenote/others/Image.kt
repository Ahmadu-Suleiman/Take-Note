package com.meta4projects.takenote.others

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import kotlin.math.roundToInt

object Image {
    fun getScaledBitmap(path: String?, activity: Activity): Bitmap? {
        val size = Point()
        activity.windowManager.defaultDisplay.getSize(size)
        return getScaledBitmap(path, size.x, size.y)
    }

    private fun getScaledBitmap(path: String?, destWidth: Int, destHeight: Int): Bitmap? {
        var options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(path, options)
        val srcWidth = options.outWidth.toFloat()
        val srcHeight = options.outHeight.toFloat()
        var inSampleSize = 1
        if (srcHeight > destHeight || srcWidth > destWidth) {
            val heightScale = srcHeight / destHeight
            val widthScale = srcWidth / destWidth
            inSampleSize = heightScale.coerceAtLeast(widthScale).roundToInt()
        }
        options = BitmapFactory.Options()
        options.inSampleSize = inSampleSize
        return BitmapFactory.decodeFile(path, options)
    }
}