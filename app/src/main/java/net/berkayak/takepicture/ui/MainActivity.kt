package net.berkayak.takepicture.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import net.berkayak.takepicture.R
import net.berkayak.takepicture.presenter.IMainActivityContract

class MainActivity : AppCompatActivity(), IMainActivityContract.View {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}
