package tv.mg4.app

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import tv.mg4.app.fragments.LoginFragment


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (null == savedInstanceState) {
            fragmentManager.beginTransaction()
                    .replace(R.id.main_content, LoginFragment())
                    .commit()
        }

    }
}
