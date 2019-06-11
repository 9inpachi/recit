package com.recit

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.res.AssetManager
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.hardware.Camera
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_camera.*
import kotlinx.android.synthetic.main.activity_camera.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.lang.IllegalStateException

//class CameraPreview(context: Context, private val mCamera: Camera) : SurfaceView(context), SurfaceHolder.Callback
//{
//
////    override fun onPreviewFrame(data: ByteArray, camera: Camera?) {
////
////    }
//
//    private val mHolder: SurfaceHolder = holder.apply {
//        // Install a SurfaceHolder.Callback so we get notified when the
//        // underlying surface is created and destroyed.
//        addCallback(this@CameraPreview)
//        // deprecated setting, but required on Android versions prior to 3.0
//        setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
//    }
//
//    override fun surfaceCreated(holder: SurfaceHolder) {
//        // The Surface has been created, now tell the camera where to draw the preview.
//        mCamera.apply {
//            try {
//                setPreviewDisplay(holder)
//                startPreview()
//            } catch (e: IOException) {
//                Log.d(ContentValues.TAG, "Error setting camera preview: ${e.message}")
//            }
//        }
//    }
//
//    override fun surfaceDestroyed(holder: SurfaceHolder) {
//        // empty. Take care of releasing the Camera preview in your activity.
//    }
//
//    override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
//        // If your preview can change or rotate, take care of those events here.
//        // Make sure to stop the preview before resizing or reformatting it.
//        if (mHolder.surface == null) {
//            // preview surface does not exist
//            return
//        }
//
//        // stop preview before making changes
//        try {
//            mCamera.stopPreview()
//        } catch (e: Exception) {
//            // ignore: tried to stop a non-existent preview
//        }
//
//        // set preview size and make any resize, rotate or
//        // reformatting changes here
//
//        // start preview with new settings
//        mCamera.apply {
//            try {
//                setPreviewDisplay(mHolder)
//                startPreview()
//            } catch (e: Exception) {
//                Log.d(ContentValues.TAG, "Error starting camera preview: ${e.message}")
//            }
//        }
//    }
//}
