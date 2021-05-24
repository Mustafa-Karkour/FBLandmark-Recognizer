package com.example.landmarkfb

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

    }

    fun toLandmarkID(view: View) {
        val intent = Intent(applicationContext, LandmarkID::class.java)
        startActivity(intent)
    }

    fun toJournal(view: View) {
        val intent = Intent(applicationContext, Journal::class.java)
        startActivity(intent)
        //finish()
    }

    fun toNearby(view: View) {
        val intent = Intent(applicationContext, NearbyPlaces::class.java)
        startActivity(intent)
    }

    fun signOut(v: View) {
        FirebaseAuth.getInstance().signOut()
        //val intent = Intent(applicationContext, Email_Login::class.java)
        val intent = Intent(applicationContext, Login::class.java)
        startActivity(intent)
        finish()
    }
}