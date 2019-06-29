package com.recit

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import kotlinx.android.synthetic.main.activity_camera.*
import android.graphics.*
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.media.Image
import android.media.ImageReader
import android.net.Uri
import android.os.Handler
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import kotlinx.coroutines.*
import android.view.*
import android.widget.Toast
import java.util.*


class CameraActivity : AppCompatActivity() {


    private var mCameraManager: CameraManager? = null
    private var mCameraDevice: CameraDevice? = null
    private var mPreviewSession: CameraCaptureSession? = null
    private val backCamera = "0"
    private val frontCamera = "1"
    private var cameraId = backCamera
    private var isProcessingF = false


    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            CameraPermissionHelper.requestCameraPermission(this)
            return
        }

        mCameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        cameraPreview.holder.addCallback(surfaceReadyCallback)

        cameraChange.setOnClickListener {
            if(cameraId == backCamera){
                cameraId = frontCamera
                mPreviewSession!!.close()
                mCameraDevice!!.close()
                mCameraManager!!.openCamera(cameraId, mStateCallback, Handler { true })
            }else if(cameraId == frontCamera){
                cameraId = backCamera
                mPreviewSession!!.close()
                mCameraDevice!!.close()
                mCameraManager!!.openCamera(cameraId, mStateCallback, Handler { true })
            }
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            Toast.makeText(this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
                .show()
            if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                CameraPermissionHelper.launchPermissionSettings(this)
            }
            finish()
        }

        recreate()
    }

    private val mStateCallback = object: CameraDevice.StateCallback() {
        override fun onDisconnected(p0: CameraDevice) {}
        override fun onError(p0: CameraDevice, p1: Int) {}
        override fun onOpened(cameraDevice: CameraDevice) {
            mCameraDevice = cameraDevice
            val cameraCharacteristics = mCameraManager!!.getCameraCharacteristics(mCameraDevice!!.id)
            cameraCharacteristics[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]?.let { streamConfigurationMap ->
                streamConfigurationMap.getOutputSizes(ImageFormat.JPEG)?.let {
                    // We are using our own image sizes
                    //val previewSize = yuvSizes.last()
                    val displayRotation = windowManager.defaultDisplay.rotation
                    val swappedDimensions = areDimensionsSwapped(displayRotation, cameraCharacteristics)
                    val rotatedPreviewWidth = if (swappedDimensions) Keys.IMAGE_HEIGHT else Keys.IMAGE_WIDTH
                    val rotatedPreviewHeight = if (swappedDimensions) Keys.IMAGE_WIDTH else Keys.IMAGE_HEIGHT

                    cameraPreview.holder.setFixedSize(rotatedPreviewWidth, rotatedPreviewHeight)


                    // Configure Image Reader
                    val imageReader =
                        ImageReader.newInstance(rotatedPreviewWidth, rotatedPreviewHeight, ImageFormat.JPEG, 2)

                    val recordingSurface = imageReader.surface
                    val previewSurface = cameraPreview.holder.surface
                    val previewRequestBuilder = mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                        .apply {
                            this.addTarget(previewSurface)
                            this.addTarget(recordingSurface)
                        }

                    val captureCallback = object : CameraCaptureSession.StateCallback() {
                        override fun onConfigureFailed(session: CameraCaptureSession) {}

                        override fun onConfigured(session: CameraCaptureSession) {
                            mPreviewSession = session
                            mPreviewSession!!.setRepeatingRequest(
                                previewRequestBuilder.build(),
                                object : CameraCaptureSession.CaptureCallback() {},
                                Handler { true }
                            )
                        }
                    }

                    val classifier = ImageClassifier(assets)

                    imageReader.setOnImageAvailableListener({
                        // do something
                        try {
                            if (it != null) {
                                val image = it.acquireLatestImage()
                                previewCallback(image, classifier)
                            }
                        } catch (e: Throwable) {
                            Log.e("EXCEPTION", "ON READING IMAGE $e")
                        }
                    }, null)

                    mCameraDevice!!.createCaptureSession(
                        Arrays.asList(previewSurface, recordingSurface),
                        captureCallback,
                        Handler { true })

                }

            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startCameraSession() {
        if (mCameraManager!!.cameraIdList.isEmpty()) {
            return
        }
        mCameraManager!!.openCamera(cameraId, mStateCallback, Handler { true })
    }

    private fun areDimensionsSwapped(displayRotation: Int, cameraCharacteristics: CameraCharacteristics): Boolean {
        var swappedDimensions = false
        when (displayRotation) {
            Surface.ROTATION_0, Surface.ROTATION_180 -> {
                if (cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) == 90 || cameraCharacteristics.get(
                        CameraCharacteristics.SENSOR_ORIENTATION
                    ) == 270
                ) {
                    swappedDimensions = true
                }
            }
            Surface.ROTATION_90, Surface.ROTATION_270 -> {
                if (cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) == 0 || cameraCharacteristics.get(
                        CameraCharacteristics.SENSOR_ORIENTATION
                    ) == 180
                ) {
                    swappedDimensions = true
                }
            }
            else -> {
                // invalid display rotation
            }
        }
        return swappedDimensions
    }

    // Callback when surface is ready
    private val surfaceReadyCallback = object : SurfaceHolder.Callback {
        override fun surfaceChanged(p0: SurfaceHolder?, p1: Int, p2: Int, p3: Int) {
        }

        override fun surfaceDestroyed(p0: SurfaceHolder?) {}

        override fun surfaceCreated(p0: SurfaceHolder?) {
            startCameraSession()
        }
    }

    // Callback for each preview frame
    fun previewCallback(image: Image, classifier: ImageClassifier) {

        if (isProcessingF) {
            image.close()
        } else {
            isProcessingF = true
            //Inside detection
            GlobalScope.launch {

                try {
                    //NEW CODE
                    val imageProcessor = ImagePreprocessor()
                    val bitmap = imageProcessor.preprocessImage(image)
                    val resultArr = classifier.recognizeImage(bitmap)

                    this@CameraActivity.runOnUiThread {
                        cameraResult1.text = resultArr[0].toString()
                        cameraResult2.text = resultArr[1].toString()
                        cameraResult3.text = resultArr[2].toString()
                    }

                } catch (e: Exception) {
                    Log.wtf("EXCEPTION", e)
                }
                isProcessingF = false
                image.close()
            }
        }
    }

    object CameraPermissionHelper {
        private const val CAMERA_PERMISSION_CODE = 0
        private const val CAMERA_PERMISSION = Manifest.permission.CAMERA

        /** Check to see we have the necessary permissions for this app.  */
        fun hasCameraPermission(activity: Activity): Boolean {
            return ContextCompat.checkSelfPermission(activity, CAMERA_PERMISSION) == PackageManager.PERMISSION_GRANTED
        }

        /** Check to see we have the necessary permissions for this app, and ask for them if we don't.  */
        fun requestCameraPermission(activity: Activity) {
            ActivityCompat.requestPermissions(
                activity, arrayOf(CAMERA_PERMISSION), CAMERA_PERMISSION_CODE
            )
        }

        /** Check to see if we need to show the rationale for this permission.  */
        fun shouldShowRequestPermissionRationale(activity: Activity): Boolean {
            return ActivityCompat.shouldShowRequestPermissionRationale(activity, CAMERA_PERMISSION)
        }

        /** Launch Application Setting to grant permission.  */
        fun launchPermissionSettings(activity: Activity) {
            val intent = Intent()
            intent.action = Settings.ACTION_ACCESSIBILITY_SETTINGS
            intent.data = Uri.fromParts("package", activity.packageName, null)
            activity.startActivity(intent)
        }
    }

}