package net.berkayak.takepicture.presenter

import android.hardware.camera2.CameraCaptureSession

interface IMainActivityContract {
    interface View{
        fun initItems()
        fun showSnackForPermission(permission: String)
    }

    interface Presenter{
        fun onCreate()
        fun onResume()
        fun onPause()
        fun onCapture()
        fun onPermissionResult(requestCode: Int,permissions: Array<out String>,grantResults: IntArray)
        fun checkPermission(permissions: String): Boolean
    }
}