package io.iotv.app

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import io.iotv.app.fragments.CameraRecordFragment


class CameraActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        if (null == savedInstanceState) {
            fragmentManager.beginTransaction()
                    .replace(R.id.container, CameraRecordFragment())
                    .commit()
        }

    }
}
