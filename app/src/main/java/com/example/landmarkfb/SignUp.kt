package com.example.landmarkfb

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_email_log_in.email
import kotlinx.android.synthetic.main.activity_email_log_in.password
import kotlinx.android.synthetic.main.activity_email_sign_up.*

class SignUp : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_email_sign_up)

        // Initialize Firebase Auth
        auth = Firebase.auth

        // On clicking the sign up button, call the function to create an account
        btnSignUp.setOnClickListener {
            if (checkDataEntered()) {
                val userEmail = email.text.toString()
                val userPassword = password.text.toString()
                createAccount(userEmail, userPassword)
            } else {
                progressSignUp.visibility = View.GONE
                btnSignUp.visibility = View.VISIBLE
            }
        }

        // Go to the Login page
        txtLogin.setOnClickListener {
            val intent = Intent(applicationContext, Login::class.java)
            startActivity(intent)
            finish()
        }
    }

    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null)
        val currentUser = auth.currentUser
        if (currentUser != null) {
            reload()
        }
    }

    private fun createAccount(email: String, password: String) {
        // [START create_user_with_email]
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success
                    Log.d(TAG, "createUserWithEmail:success")

                    // Go to main menu
                    reload()
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)
                    Toast.makeText(this@SignUp, "Authentication failed",
                        Toast.LENGTH_SHORT).show()

                    // Remove progress bar and display sign up button
                    progressSignUp!!.visibility = View.GONE
                    btnSignUp!!.visibility = View.VISIBLE
                }
            }
        // [END create_user_with_email]
    }

    private fun reload() {
        // Go to main menu
        val intent = Intent(applicationContext, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun isEmail(text: EditText?): Boolean {
        // Check if email field is empty or has an invalid format
        val email: CharSequence = text!!.text.toString()
        return email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun checkDataEntered(): Boolean {
        // Check if form is filled correctly and returns a boolean

        // Display a progress bar while checking the fields
        progressSignUp!!.visibility = View.VISIBLE
        btnSignUp!!.visibility = View.INVISIBLE

        // Check if email has a valid format
        if (!isEmail(email)) {
            email!!.error = "Enter valid email!"
            return false
        }
        // Check if password field is empty
        if (password.text.toString().isEmpty()) {
            password!!.error = "Enter a password!"
            return false
        }
        // Check if password meets the length requirement of 6 characters
        if (password!!.text.toString().length < 6) {
            password!!.error = "Password must have at least 6 characters"
            return false
        }
        // Check if confirm password field is empty
        if (confirmPassword.text.toString().isEmpty()) {
            confirmPassword!!.error = "Confirm password"
            return false
        }
        // Check if both the password fields match
        if (!password!!.text.toString().equals(confirmPassword!!.text.toString())) {
            confirmPassword!!.error = "Passwords do not match"
            return false
        }
        return true
    }

    companion object {
        private const val TAG = "EmailPassword"
    }
}