package tv.mg4.app

import android.os.Bundle
import android.support.v7.app.AppCompatActivity


class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (null == savedInstanceState) {
            fragmentManager.beginTransaction()
                    .replace(R.id.container, CameraRecordFragment())
                    .commit()
        }

    }
}
