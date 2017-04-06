package io.iotv.app

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.onClick
import io.iotv.app.R

class MainActivity : AppCompatActivity() {
    companion object {
        val LOGIN_INTENT = 1
        val CAMERA_INTENT = 2
    }

    var mLoginButton: Button? = null
    var mCameraButton: Button? = null
    var mLoginIntent: Intent? = null
    var mCameraIntent: Intent? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mLoginIntent = Intent(this, LoginActivity::class.java)
        mCameraIntent = Intent(this, CameraActivity::class.java)

        setContentView(R.layout.activity_main)

        mLoginButton = main_login_link
        mCameraButton = main_camera_link
        mLoginButton?.onClick { startActivityForResult(mLoginIntent, LOGIN_INTENT) }
        mCameraButton?.onClick { startActivityForResult(mCameraIntent, CAMERA_INTENT) }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LOGIN_INTENT) {
            mLoginButton?.text = data?.extras?.get("user_id") as CharSequence? ?: ""
        }
    }
}