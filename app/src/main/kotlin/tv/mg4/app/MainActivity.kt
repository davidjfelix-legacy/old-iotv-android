package tv.mg4.app

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.TextView

class MainActivity : AppCompatActivity(), View.OnClickListener {
    override fun onClick(v: View?) {
        startActivityForResult(mLoginIntent, LOGIN_INTENT)
    }

    companion object {
        val LOGIN_INTENT = 1
    }


    var mLoginText: TextView? = null
    var mLoginIntent: Intent? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mLoginIntent = Intent(this, LoginActivity::class.java)
        setContentView(R.layout.activity_main)
        mLoginText = findViewById(R.id.main_login_link) as TextView
        mLoginText?.setOnClickListener(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LOGIN_INTENT) {
            mLoginText?.text = data?.extras?.get("user_id") as CharSequence? ?: ""
        }
    }
}