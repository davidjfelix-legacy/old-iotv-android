package tv.mg4.app

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Toast
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.facebook.login.widget.LoginButton
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider


class MainActivity : AppCompatActivity(), GoogleApiClient.OnConnectionFailedListener, FacebookCallback<LoginResult>, View.OnClickListener {
    override fun onError(error: FacebookException?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onSuccess(result: LoginResult?) {
        result?.let {
            firebaseAuthWithFacebook(result.accessToken)
        }
    }

    override fun onCancel() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    companion object {
        val TAG: String = "MainActivity"
        val RC_SIGN_IN: Int = 9001
    }


    var mCallbackManager: CallbackManager? = null
    var mGoogleApiClient: GoogleApiClient? = null
    var mAuth: FirebaseAuth = FirebaseAuth.getInstance()
    var mAuthListener: FirebaseAuth.AuthStateListener = FirebaseAuth.AuthStateListener {
        val user = it.currentUser
        if (null != user) {
            Log.d(TAG, "onAuthStateChanged:signed_in:" + user.uid)
        } else {
            Log.d(TAG, "onAuthStateChanged:signed_out")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            if (result.isSuccess) {
                firebaseAuthWithGoogle(result.signInAccount)
            }
        } else {
            mCallbackManager?.onActivityResult(requestCode, resultCode, data)
        }

    }

    override fun onClick(v: View?) {
        if (v?.id == R.id.login_google_button) {
            signInWithGoogle()
        } else {
            Toast.makeText(this, "Something else clicked.",
                    Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mCallbackManager = CallbackManager.Factory.create()

        findViewById(R.id.login_google_button).setOnClickListener(this)
        val mFacebookButton = findViewById(R.id.login_facebook_button) as LoginButton

        mFacebookButton.setReadPermissions("email")
        LoginManager.getInstance().registerCallback(mCallbackManager, this)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()

        mGoogleApiClient = GoogleApiClient.Builder(this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .enableAutoManage(this, this)
                .build()
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        Log.d(TAG, "onConnectionFailed:" + connectionResult)
        Toast.makeText(this, "Google Play Services error.", Toast.LENGTH_SHORT).show()
    }

    override fun onStart() {
        super.onStart()
        mAuth.addAuthStateListener(mAuthListener)
    }

    override fun onStop() {
        super.onStop()
        mAuth.removeAuthStateListener(mAuthListener)
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount?) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + account?.id)
        val credential = GoogleAuthProvider.getCredential(account?.idToken, null)
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener {
                    Log.d(TAG, "signInWithCredential:onComplete:" + it.isSuccessful)

                    if (it.isSuccessful) {
                        Toast.makeText(this, "Authentication succeeded.",
                                Toast.LENGTH_SHORT).show()
                    } else {
                        Log.w(TAG, "signInWithCredential", it.exception)
                        Toast.makeText(this, "Authentication failed.",
                                Toast.LENGTH_SHORT).show()
                    }
                }
    }

    private fun firebaseAuthWithFacebook(token: AccessToken) {
        Log.d(TAG, "firebaseAuthWithFacebook:" + token)

        val credential = FacebookAuthProvider.getCredential(token.token)
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener {
                    Log.d(TAG, "signInWithCredential:onComplete:" + it.isSuccessful)

                    if (it.isSuccessful) {
                        Toast.makeText(this, "Authentication succeeded.",
                                Toast.LENGTH_SHORT).show()
                    } else {
                        Log.w(TAG, "signInWithCredential", it.exception)
                        Toast.makeText(this, "Authentication failed.",
                                Toast.LENGTH_SHORT).show()
                    }

                }
    }


    private fun signInWithGoogle() {
        val signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient)
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }
}
