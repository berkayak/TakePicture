package net.berkayak.takepicture.presenter

import android.hardware.camera2.CameraCaptureSession
import com.google.android.material.snackbar.Snackbar

interface IMainActivityContract {
    interface View{
        fun initItems()
        fun snackMaker(): Snackbar
    }

    interface Presenter{
        fun onCreate()
        fun onResume()
        fun onPause()
        fun onCapture()
        fun onChangeCamera()
        fun onPermissionResult(requestCode: Int,permissions: Array<out String>,grantResults: IntArray)
        fun checkPermission(permissions: String, reqCode: Int): Boolean
    }
}