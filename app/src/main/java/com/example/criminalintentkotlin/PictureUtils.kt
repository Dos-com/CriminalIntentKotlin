package com.example.criminalintentkotlin

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.os.Build
import android.view.WindowInsets
import android.view.WindowManager


fun getScaledBitmap(path: String, destWidth: Int, destHeight: Int) : Bitmap{
    var options = BitmapFactory.Options()
    options.inJustDecodeBounds = true
    BitmapFactory.decodeFile(path, options)

    val srcWidth = options.outWidth.toFloat()
    val srcHeight = options.outHeight.toFloat()

    var inSampleSize = 1
    if (srcHeight > destHeight || srcWidth > destWidth){
        val heightScale = srcHeight / destHeight
        val widthScale = srcWidth / destWidth

        val sampleScale = if (heightScale > widthScale){
            heightScale
        }
        else{
            widthScale
        }
        inSampleSize = Math.round(sampleScale)
    }

    options = BitmapFactory.Options()
    options.inSampleSize = inSampleSize

    return BitmapFactory.decodeFile(path , options)
}

fun getScaledBitmap(path: String, activity: Activity) : Bitmap{
    val width: Int
    val height: Int
    val size = Point()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val wm = activity.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val windowMetrics = wm.currentWindowMetrics
        val windowInsets: WindowInsets = windowMetrics.windowInsets

        val insets = windowInsets.getInsetsIgnoringVisibility(
            WindowInsets.Type.navigationBars() or WindowInsets.Type.displayCutout())
        val insetsWidth = insets.right + insets.left
        val insetsHeight = insets.top + insets.bottom

        val b = windowMetrics.bounds
        width = b.width() - insetsWidth
        height = b.height() - insetsHeight
    } else {
        val display = activity.windowManager.defaultDisplay // deprecated in API 30
        display?.getSize(size) // deprecated in API 30
        width = size.x
        height = size.y
    }

    return getScaledBitmap(path, width, height)
}