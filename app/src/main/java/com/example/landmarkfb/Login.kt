package com.example.landmarkfb

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.text.TextUtils
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_log_in.*

class Login : AppCompatActivity() {
    //    var email: EditText? = null
//    var password: EditText? = null
//    var btnLogin: MaterialButton? = null
//    var txtSignUp: TextView? = null
//    var progressBar: ProgressBar? = null
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_in)

        // Initialize Firebase Auth
        auth = Firebase.auth

        btnLogin.setOnClickListener {
            if (checkDataEntered()) {
                val userEmail = email.getText().toString()
                val userPassword = password.getText().toString()
                signIn(userEmail, userPassword)
            } else {
                progress.setVisibility(View.GONE)
                btnLogin.setVisibility(View.VISIBLE)
            }
        }
        txtSignUp.setOnClickListener {
            val intent = Intent(applicationContext, SignUp::class.java)
            startActivity(intent)
            finish()
        }
    }

    fun isEmail(text: EditText?): Boolean {
        val email: CharSequence = text!!.text.toString()
        return !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun isEmpty(text: EditText?): Boolean {
        val str: CharSequence = text!!.text.toString()
        return TextUtils.isEmpty(str)
    }

    fun checkDataEntered(): Boolean {
        progress.visibility = View.VISIBLE
        btnLogin.visibility = View.INVISIBLE
        if (!isEmail(email)) {
            Toast.makeText(this, "Enter an e-mail address to register", Toast.LENGTH_SHORT).show()
            email!!.error = "Enter valid email!"
            return false
        }
        if (isEmpty(password)) {
            Toast.makeText(this, "Enter a password to register", Toast.LENGTH_SHORT).show()
            password!!.error = "Enter a password!"
            return false
        }
        return true
    }

    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        if (currentUser != null) {
            reload()
        }
    }

    private fun reload() {
        // Go to main menu
        val intent = Intent(applicationContext, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun signIn(email: String, password: String) {
        // [START sign_in_with_email]
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithEmail:success")
                    //val user = auth.currentUser
                    Toast.makeText(this@Login, "Login successful", Toast.LENGTH_SHORT).show()
                    reload()
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    Toast.makeText(this@Login, "Authentication failed.",
                        Toast.LENGTH_SHORT).show()
                    progress.visibility = View.GONE
                    btnLogin.visibility = View.VISIBLE
                }
            }
        // [END sign_in_with_email]
    }

    companion object {
        private const val TAG = "EmailPassword"
    }
}