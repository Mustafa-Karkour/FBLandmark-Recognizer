package com.example.landmarkfb

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_log_in.*
import kotlinx.android.synthetic.main.activity_log_in.email
import kotlinx.android.synthetic.main.activity_log_in.password
import kotlinx.android.synthetic.main.activity_sign_up.*
import java.util.*

class SignUp : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        // Initialize Firebase Auth
        auth = Firebase.auth

//        val db: FirebaseFirestore = FirebaseFirestore.getInstance()
//        val users: CollectionReference = db.collection("users")
//        val user: MutableMap<String, Any> = HashMap()

        btnSignUp.setOnClickListener(View.OnClickListener {
            if (checkDataEntered()) {
                val userEmail = email.getText().toString()
                val userPassword = password.getText().toString()
                //createAccount(userEmail, userPassword, users, user)
                createAccount(userEmail, userPassword)
            } else {
                progressSignUp.setVisibility(View.GONE)
                btnSignUp.setVisibility(View.VISIBLE)
            }
        })
        txtLogin.setOnClickListener(View.OnClickListener {
            val intent = Intent(applicationContext, Login::class.java)
            startActivity(intent)
            finish()
        })
    }

    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth!!.currentUser
        if (currentUser != null) {
            reload()
        }
    }

    private fun createAccount(
        email: String,
        password: String
//        users: CollectionReference,
//        user: MutableMap<*, *>
    ) {
        // [START create_user_with_email]
        auth!!.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "createUserWithEmail:success")
                    val fbUser = auth.currentUser
//                    updateDB(users, user)
                    updateUI(fbUser)
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)
                    Toast.makeText(this@SignUp, "Authentication failed",
                        Toast.LENGTH_SHORT).show()
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

//    private fun updateDB(users: CollectionReference, user: MutableMap<*, *>) {
//        val fn = firstName!!.text.toString()
//        val ln = lastName!!.text.toString()
//        val userEmail = email!!.text.toString()
//        user["First Name"] = fn
//        user["Last Name"] = ln
//        users.document(userEmail).set(user)
//    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            // Set Display Name to the entered first name
            val setDisplayName = UserProfileChangeRequest.Builder().setDisplayName(
                firstName!!.text.toString()
            ).build()
            user.updateProfile(setDisplayName)

            //Re-authentication to update Display Name
            auth.signInWithEmailAndPassword(email!!.text.toString(), password!!.text.toString())
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Toast.makeText(this@SignUp, "Successful Authentication", Toast.LENGTH_SHORT)
                            .show()
                        Log.d(TAG, "signInWithEmail:success")

                        //Go to main menu
                        reload()
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "signInWithEmail:failure", task.exception)
                        Toast.makeText(this@SignUp, "Authentication failed.",
                            Toast.LENGTH_SHORT).show()
                    }
                }
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
        // Check if form is filled correctly
        progressSignUp!!.visibility = View.VISIBLE
        btnSignUp!!.visibility = View.INVISIBLE
        if (isEmpty(firstName)) {
            Toast.makeText(this, "You must enter first name to register!", Toast.LENGTH_SHORT)
                .show()
            firstName!!.error = "First name is required!"
            return false
        }
        if (isEmpty(lastName)) {
            Toast.makeText(this, "Enter a last name to register", Toast.LENGTH_SHORT).show()
            lastName!!.error = "Last name is required!"
            return false
        }
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
        if (password!!.text.toString().length < 6) {
            Toast.makeText(this, "Password must have at least 6 characters", Toast.LENGTH_SHORT)
                .show()
            password!!.error = "Password must have at least 6 characters"
            return false
        }
        return true
    }

    companion object {
        private const val TAG = "EmailPassword"
    }
}