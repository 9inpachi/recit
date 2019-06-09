package com.recit

import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Camera
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.FrameLayout
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_camera.*
import kotlinx.android.synthetic.main.activity_camera.view.*
import java.io.IOException
import android.R.attr.bitmap
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import java.lang.IllegalStateException


class CameraActivity : Activity() {

    private var mCamera: Camera? = null
    private var mPreview: CameraPreview? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        mCamera = getCameraInstance()
        mPreview = mCamera?.let {
            CameraPreview(this, it, assets, cameraResult)
        }
        if(!baseContext.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            cameraResult.text = "Unable to use camera"
        }

        mPreview.also {
            val preview: FrameLayout = cameraPreview
            preview.addView(it)
        }

    }

    override fun onPause() {
        super.onPause()
        mCamera?.release() // release the camera immediately on pause event
    }

    fun getCameraInstance(): Camera? {
        return try {
            Camera.open() // attempt to get a Camera instance
        } catch (e: Exception) {
            // Camera is not available (in use or does not exist)
            null // returns null if camera is unavailable
        }
    }

    class CameraPreview(context: Context, private val mCamera: Camera, assets: AssetManager, cameraResult: TextView) : SurfaceView(context), SurfaceHolder.Callback, Camera.PreviewCallback
    {

        var isProcessingF = false
        val classifier = ImageClassifier(assets)
        var imageFormat: Int? = null

        override fun onPreviewFrame(data: ByteArray, camera: Camera?) {
            if(imageFormat == ImageFormat.NV21)
                if(isProcessingF){
                    Log.i("Skipping Frame", "Skipping frame because of processing")
                    return
                }else{
                    Log.i("STEP 1", data.toString())
                    isProcessingF = true
                    try{
                        Log.i("STEP 2", data.inputStream().toString())
                        Log.i("STEP 3", BitmapFactory.decodeStream(data.inputStream()).toString())
//                        val bitMap = BitmapFactory.decodeStream(data.inputStream())
//                        val resultArr = classifier.recognizeImage(bitMap)
//                        Log.i("IMAGE DATA", data.toString())
                        //cameraResult.text = resultArr.joinToString("\n")
                    }catch (e: IllegalStateException){
                        Log.i("Exception", e.toString())
                    }
                    isProcessingF = false
                }
        }

        private val mHolder: SurfaceHolder = holder.apply {
            // Install a SurfaceHolder.Callback so we get notified when the
            // underlying surface is created and destroyed.
            addCallback(this@CameraPreview)
            // deprecated setting, but required on Android versions prior to 3.0
            setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
        }

        override fun surfaceCreated(holder: SurfaceHolder) {
            // The Surface has been created, now tell the camera where to draw the preview.
            mCamera.apply {
                try {
                    setPreviewDisplay(holder)
                    imageFormat = this.parameters.previewFormat
                    startPreview()
                } catch (e: IOException) {
                    Log.d(TAG, "Error setting camera preview: ${e.message}")
                }
            }
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
            // empty. Take care of releasing the Camera preview in your activity.
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
            // If your preview can change or rotate, take care of those events here.
            // Make sure to stop the preview before resizing or reformatting it.
            if (mHolder.surface == null) {
                // preview surface does not exist
                return
            }

            // stop preview before making changes
            try {
                mCamera.stopPreview()
                mCamera.setPreviewCallback(this);
            } catch (e: Exception) {
                // ignore: tried to stop a non-existent preview
            }

            // set preview size and make any resize, rotate or
            // reformatting changes here

            // start preview with new settings
            mCamera.apply {
                try {
                    setPreviewDisplay(mHolder)
                    startPreview()
                } catch (e: Exception) {
                    Log.d(TAG, "Error starting camera preview: ${e.message}")
                }
            }
        }
    }

}