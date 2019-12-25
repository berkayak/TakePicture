package net.berkayak.takepicture.presenter

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivityPresenter: IMainActivityContract.Presenter {
    private var mViewPusher: IMainActivityContract.View
    private var mContext: Context
    lateinit var mTextureView: TextureView

    lateinit var mImageDimension: Size
    lateinit var mCameraDevice: CameraDevice
    lateinit var mImageReader: ImageReader
    lateinit var mBackGroundHandler: Handler
    lateinit var mHandlerThread: HandlerThread

    companion object {
        const val CAMERA_INDEX = 0
        const val REQ_CODE_CAMERA_PERM = 401
    }

    constructor(viewPusher: IMainActivityContract.View, context: Context){
        this.mViewPusher = viewPusher
        this.mContext = context
    }

    override fun onCreate() {
        mViewPusher.initItems()
    }

    override fun onResume() {
        startBackgroundThread()
    }

    override fun onPause() {
        stopBackgroundThread()
    }

    override fun onCapture() {
        takePicture()
    }

    override fun checkPermission(permissions: String): Boolean {
        if(ContextCompat.checkSelfPermission(mContext, permissions) == PackageManager.PERMISSION_GRANTED){
            return true
        } else {
            ActivityCompat.requestPermissions((mContext as Activity), arrayOf(Manifest.permission.CAMERA), REQ_CODE_CAMERA_PERM)
            return false
        }
    }

    override fun onPermissionResult(requestCode: Int,permissions: Array<out String>,grantResults: IntArray) {
        when(requestCode){
            REQ_CODE_CAMERA_PERM -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    openCamera()
                else { //denny button pressed
                    if (ActivityCompat.shouldShowRequestPermissionRationale((mContext as Activity), Manifest.permission.CAMERA)){
                        mViewPusher.showSnackForPermission(Manifest.permission.CAMERA)
                    } else {
                        //never ask cheked
                        //mViewPusher.showSnackForPermission(Manifest.permission.CAMERA)
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun openCamera() {
        if(!checkPermission(Manifest.permission.CAMERA))
            return
        var cameraManager = mContext.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        var cameraID = cameraManager.cameraIdList[CAMERA_INDEX]
        var cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraID)
        var configMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) as StreamConfigurationMap
        mImageDimension = configMap.getOutputSizes(SurfaceTexture::class.java)[0]
        cameraManager.openCamera(cameraID, cameraDeviceStateCallback, null)
    }

    fun startCameraPreview() {
        var texture = mTextureView.surfaceTexture
        texture.setDefaultBufferSize(mImageDimension.width, mImageDimension.height)
        var surface = Surface(texture)
        mCameraDevice.createCaptureSession(listOf(surface), previewCaptureSessionStateCallback, null)
    }

    fun updatePreview(session: CameraCaptureSession) {
        if (!::mCameraDevice.isInitialized) return
        var texture = mTextureView.surfaceTexture
        texture.setDefaultBufferSize(mImageDimension.width, mImageDimension.height)
        var surface = Surface(texture)
        var previewCaptureReq = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        previewCaptureReq.addTarget(surface)
        previewCaptureReq.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
        session.setRepeatingRequest(previewCaptureReq.build(), null, mBackGroundHandler)

    }

    private fun takePicture() {
        if (!::mImageReader.isInitialized)
            mImageReader = ImageReader.newInstance(mImageDimension.width, mImageDimension.height, ImageFormat.JPEG, 1)
        var surfaces = mutableListOf<Surface>()
        surfaces.add(mImageReader.surface)
        surfaces.add(Surface(mTextureView.surfaceTexture))

        var takePictureCaptureRequest = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
        takePictureCaptureRequest.addTarget(mImageReader.surface)
        takePictureCaptureRequest.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
        takePictureCaptureRequest.set(CaptureRequest.JPEG_ORIENTATION, 90)

        mImageReader.setOnImageAvailableListener(imageAvailableListener, mBackGroundHandler)

        var captureLisener = object : CameraCaptureSession.CaptureCallback() {
            override fun onCaptureCompleted(session: CameraCaptureSession,request: CaptureRequest,result: TotalCaptureResult) {
                startCameraPreview()
            }
        }

        mCameraDevice.createCaptureSession(surfaces, object: CameraCaptureSession.StateCallback(){
            override fun onConfigureFailed(session: CameraCaptureSession) {
                Log.e("REGS", "onConfigureFailed")
            }
            override fun onConfigured(session: CameraCaptureSession) {
                session.capture(takePictureCaptureRequest.build(), captureLisener, mBackGroundHandler)
            }
        }, null)
    }

    private fun startBackgroundThread() {
        mHandlerThread = HandlerThread("Camera Background")
        mHandlerThread.start()
        mBackGroundHandler = Handler(mHandlerThread.looper)
    }

    private fun stopBackgroundThread() {
        mHandlerThread.quitSafely()
        mHandlerThread.join()
    }

    //use this in openCamera
    private var cameraDeviceStateCallback = object: CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            mCameraDevice = camera
            startCameraPreview()
        }

        override fun onDisconnected(camera: CameraDevice) {
            mCameraDevice.close()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            mCameraDevice.close()
        }
    }

    //this will be use when creating capture request for preview
    private var previewCaptureSessionStateCallback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigureFailed(session: CameraCaptureSession) {
            Log.e("REGS", "onConfigureFailed")
        }

        override fun onConfigured(session: CameraCaptureSession) {
            if (!::mCameraDevice.isInitialized){
                return
            }
            updatePreview(session)
        }
    }

    //this will use for takePicture
    private var imageAvailableListener = ImageReader.OnImageAvailableListener { reader ->
        var image : Image
        try{
            image = reader!!.acquireLatestImage()
            Log.i("REGS", "${image.timestamp}")
        } catch (e: Exception){
            Log.e("REGS", e.message)
        }
    }

}