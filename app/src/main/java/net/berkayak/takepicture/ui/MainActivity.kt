package net.berkayak.takepicture.ui

import android.graphics.SurfaceTexture
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.TextureView
import android.view.View
import android.widget.Button
import com.google.android.material.snackbar.Snackbar
import net.berkayak.takepicture.R
import net.berkayak.takepicture.presenter.IMainActivityContract
import net.berkayak.takepicture.presenter.MainActivityPresenter

class MainActivity : AppCompatActivity(), IMainActivityContract.View {

    lateinit var mTextureView: TextureView
    lateinit var mCaptureBtn: Button
    lateinit var mPresenter: MainActivityPresenter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        this.mPresenter = MainActivityPresenter(this, this)
        this.mPresenter.onCreate()
    }

    override fun onResume() {
        super.onResume()
        mPresenter.onResume()
    }

    override fun onPause() {
        super.onPause()
        mPresenter.onPause()
    }

    override fun onRequestPermissionsResult(requestCode: Int,permissions: Array<out String>,grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        mPresenter.onPermissionResult(requestCode, permissions, grantResults)
    }

    override fun initItems() {
        mTextureView = findViewById(R.id.myTexture)
        mCaptureBtn = findViewById(R.id.captureBtn)

        mPresenter.mTextureView = this.mTextureView
        mTextureView.surfaceTextureListener = textureListener
        mCaptureBtn.setOnClickListener(captureListener)
    }

    override fun snackMaker(): Snackbar {
        return Snackbar.make(findViewById(R.id.mainLayout), "", Snackbar.LENGTH_LONG)
    }

    //we must listen the our texture which preview camera view
    private var textureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
            return false
        }

        override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
            mPresenter.openCamera()
        }
    }

    //we must listen capture button
    private var captureListener = View.OnClickListener {
        mPresenter.onCapture()
    }

}
