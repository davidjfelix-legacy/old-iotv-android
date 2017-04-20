package io.iotv.app

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.android.synthetic.main.activity_login.*
import org.jetbrains.anko.longToast
import org.jetbrains.anko.onClick
import org.jetbrains.anko.toast


class LoginActivity : AppCompatActivity(), GoogleApiClient.OnConnectionFailedListener, FacebookCallback<LoginResult> {
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
        val TAG: String = "LoginActivity"
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        mCallbackManager = CallbackManager.Factory.create()


        login_email_button.onClick { signInWithEmailAndPassword() }
        login_google_button.onClick { signInWithGoogle() }
        login_facebook_button.onClick { signInWithFacebook() }


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
        toast("Google Play Services error.")
    }

    override fun onStart() {
        super.onStart()
        mAuth.addAuthStateListener(mAuthListener)
    }

    override fun onStop() {
        super.onStop()
        mAuth.removeAuthStateListener(mAuthListener)
    }

    private fun finishWithUser() {
        toast("Authentication succeeded.")
        val intent = Intent()
        val bundle = Bundle()
        bundle.putString("user_id", mAuth.currentUser?.uid)
        intent.putExtras(bundle)
        setResult(RESULT_OK, intent)
        finish()
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount?) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + account?.id)
        val credential = GoogleAuthProvider.getCredential(account?.idToken, null)
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener {
                    Log.d(TAG, "firebaseAuthWithGoogle:onComplete:" + it.isSuccessful)

                    if (it.isSuccessful) {
                        finishWithUser()
                    } else {
                        if (it.exception != null && it.exception!!::class == FirebaseAuthUserCollisionException::class) {
                            Log.w(TAG, "firebaseAuthWithGoogle", it.exception)
                            longToast("Associated email already in use. Try a different login method?")
                        } else {
                            Log.w(TAG, "firebaseAuthWithGoogle", it.exception)
                            toast("Authentication failed.")
                        }
                    }
                }
    }

    private fun firebaseAuthWithFacebook(token: AccessToken) {
        Log.d(TAG, "firebaseAuthWithFacebook:" + token)

        val credential = FacebookAuthProvider.getCredential(token.token)
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener {
                    Log.d(TAG, "firebaseAuthWithFacebook:onComplete:" + it.isSuccessful)

                    if (it.isSuccessful) {
                        finishWithUser()
                    } else {
                        if (it.exception != null && it.exception!!::class == FirebaseAuthUserCollisionException::class) {
                            Log.w(TAG, "firebaseAuthWithFacebook", it.exception)
                            longToast("Associated email already in use. Try a different login method?")
                        } else {
                            Log.w(TAG, "firebaseAuthWithFacebook", it.exception)
                            toast("Authentication failed.")
                        }
                    }
                }
    }

    private fun signInWithEmailAndPassword() {
        val email = login_email_text.text.toString()
        val password = login_password_text.text.toString()

        if (email.isNotBlank() && password.isNotBlank()) {
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener {
                        Log.d(TAG, "signInWithEmailAndPassword:onComplete:" + it.isSuccessful)

                        if (it.isSuccessful) {
                            finishWithUser()
                        } else {
                            if (it.exception != null && it.exception!!::class == FirebaseAuthUserCollisionException::class) {
                                Log.w(TAG, "signInWithEmailAndPassword", it.exception)
                                longToast("Associated email already in use. Try a different login method?")
                            } else {
                                Log.w(TAG, "signInWithEmailAndPassword", it.exception)
                                toast("Authentication failed.")
                            }
                        }

                    }
        } else {
            toast("Please enter both an email and password.")
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient)
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    private fun signInWithFacebook() {
        val loginManager = LoginManager.getInstance()
        loginManager.logInWithReadPermissions(this, listOf("email"))
        loginManager.registerCallback(mCallbackManager, this)
    }
}
