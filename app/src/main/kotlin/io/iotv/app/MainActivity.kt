package io.iotv.app

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.onClick

class MainActivity : AppCompatActivity() {
    companion object {
        val LOGIN_INTENT = 1
        val CAMERA_INTENT = 2
        val VIDEOS_INTENT = 3
    }

    var mLoginButton: Button? = null
    var mCameraButton: Button? = null
    var mMyVideosButton: Button? = null
    var mLoginIntent: Intent? = null
    var mCameraIntent: Intent? = null
    var mMyVideosIntent: Intent? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mLoginIntent = Intent(this, LoginActivity::class.java)
        mCameraIntent = Intent(this, CameraActivity::class.java)
        mMyVideosIntent = Intent(this, MyVideosActivity::class.java)

        setContentView(R.layout.activity_main)

        mLoginButton = main_login_link
        mCameraButton = main_camera_link
        mMyVideosButton = main_videos_link
        mLoginButton?.onClick { startActivityForResult(mLoginIntent, LOGIN_INTENT) }
        mCameraButton?.onClick { startActivityForResult(mCameraIntent, CAMERA_INTENT) }
        mMyVideosButton?.onClick { startActivityForResult(mMyVideosIntent, VIDEOS_INTENT) }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LOGIN_INTENT) {
            mLoginButton?.text = data?.extras?.get("user_id") as CharSequence? ?: ""
        }
    }
}