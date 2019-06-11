package com.recit

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Camera
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.FrameLayout
import kotlinx.android.synthetic.main.activity_camera.*
import android.graphics.*
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.*
import java.io.IOException
import android.view.*


class CameraActivity : AppCompatActivity() {
    private var mCamera: Camera? = null
    private var mPreview: CameraPreview? = null

    var isProcessingF = false
    val CAMERA_BACK = 0
    val CAMERA_FRONT = 1
    var cameraId = CAMERA_BACK

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        checkPermission()

        mCamera = getCameraInstance(cameraId)
        setCameraPreview()
        if(!baseContext.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            cameraResult.text = "Unable to use camera"
        }

        cameraChange.setOnClickListener{
            if(cameraId == CAMERA_FRONT){
                cameraId = CAMERA_BACK
                mCamera?.release()
                mCamera = getCameraInstance(cameraId)
                setCameraPreview()
            } else if(cameraId == CAMERA_BACK){
                cameraId = CAMERA_FRONT
                mCamera?.release()
                mCamera = getCameraInstance(cameraId)
                setCameraPreview()
            }
        }

        val classifier = ImageClassifier(assets)

        mCamera?.setPreviewCallback{data, camera->
            onPreviewCallBack(data, camera, classifier)
        }
    }

    private fun checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE), 0)
        }
    }

    override fun onPause() {
        super.onPause()
        mCamera?.release() // release the camera immediately on pause event
    }

    override fun onResume() {
        super.onResume()
        mCamera = getCameraInstance(cameraId)
        setCameraPreview()
    }

    fun getCameraInstance(cameraId: Int): Camera? {
        if(mCamera != null){
            mCamera?.release()
        }
        return try {
            Camera.open(cameraId) // attempt to get a Camera instance
        } catch (e: Exception) {
            // Camera is not available (in use or does not exist)
            null // returns null if camera is unavailable
        }
    }

    fun setCameraPreview(){
        mPreview = mCamera?.let {
            CameraPreview(this, it)
        }
        mPreview.also {
            val preview: FrameLayout = cameraPreview
            preview.addView(it, 0)
        }
        val classifier = ImageClassifier(assets)
        mCamera?.setPreviewCallback{data, camera->
            onPreviewCallBack(data, camera, classifier)
        }
    }

    fun onPreviewCallBack(data: ByteArray, camera: Camera, classifier: ImageClassifier){
        Log.i("INSIDE", "INSIDE PREVIEW CALLBACK")
        if(!isProcessingF){
            isProcessingF = true

            GlobalScope.launch {
                val parameters = camera!!.parameters
                val width = parameters.previewSize.width
                val height = parameters.previewSize.height

                val yuv = YuvImage(data, parameters.previewFormat, width, height, null)

                val out = ByteArrayOutputStream()
                yuv.compressToJpeg(Rect(0, 0, Keys.INPUT_SIZE, Keys.INPUT_SIZE), 10, out)

                val bytes = out.toByteArray()
                try{
                    val bitMap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    val resultArr = classifier.recognizeImage(bitMap)
                    Log.i("RESULT", resultArr.joinToString ("\n"))
                    this@CameraActivity.runOnUiThread{
                        cameraResult.text = resultArr[0].toString()
                    }

                }catch (e: Exception){
                    Log.e("Exception", e.toString())
                }
                isProcessingF = false
            }
        }
    }

}

class CameraPreview(context: Context, private val mCamera: Camera) : SurfaceView(context), SurfaceHolder.Callback
{

    private val mHolder: SurfaceHolder = holder.apply {
        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        addCallback(this@CameraPreview)
        // deprecated setting, but required on Android versions prior to 3.0
        setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        // The Surface has been created, now tell the camera where to draw the preview.

    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // empty. Take care of releasing the Camera preview in your activity.
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.
        mCamera.apply {
            try {
                mCamera.setDisplayOrientation(90)
                setPreviewDisplay(holder)
                startPreview()
            } catch (e: IOException) {
                Log.d(ContentValues.TAG, "Error setting camera preview: ${e.message}")
            }
        }
        if (mHolder.surface == null) {
            // preview surface does not exist
            return
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview()
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
                Log.d(ContentValues.TAG, "Error starting camera preview: ${e.message}")
            }
        }
    }
}