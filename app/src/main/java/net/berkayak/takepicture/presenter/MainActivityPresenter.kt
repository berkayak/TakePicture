package net.berkayak.takepicture.presenter

import android.annotation.SuppressLint
import android.content.Context
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
        if (mTextureView.isAvailable)
            openCamera()
        else
            mViewPusher.initItems()
    }

    override fun onPause() {
        stopBackgroundThread()
    }

    override fun onCapture() {
        takePicture()
    }

    override fun checkPermission() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onPermissionResult(requestCode: Int,permissions: Array<out String>,grantResults: IntArray) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    @SuppressLint("MissingPermission")
    fun openCamera() {
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