package com.example.landmarkfb

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_email_log_in.*

class Login : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_email_log_in)

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

    private fun isEmail(text: EditText?): Boolean {
        // Check if email field is empty or has an invalid format
        val email: CharSequence = text!!.text.toString()
        return email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun checkDataEntered(): Boolean {
        // Show progress bar while processing the input
        progress.visibility = View.VISIBLE
        btnLogin.visibility = View.INVISIBLE

        // Check if email field is empty or has an invalid format
        if (!isEmail(email)) {
            email!!.error = "Enter a valid email!"
            return false
        }
        // Check if password field is empty
        if (password.text.toString().isEmpty()) {
            password!!.error = "Enter a password!"
            return false
        }
        return true
    }

    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null)
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
                    Toast.makeText(this@Login, "Login successful", Toast.LENGTH_SHORT).show()
                    reload()
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    Toast.makeText(this@Login, "Authentication failed.",
                        Toast.LENGTH_SHORT).show()

                    // Remove progress bar and show login button
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