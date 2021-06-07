package com.example.landmarkfb

/*
CSIS-401 Project
Spring 2021

TravelMate

Team members:
David Liang (S00049751) - Landmark Identification and Login
Mustafa Karkour (S00049859) - Journal
Ahmad Aldulaie (S00052749) - Map
*/

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
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
    }

    fun toNearby(view: View) {
        val intent = Intent(applicationContext, NearbyPlaces::class.java)
        startActivity(intent)
    }

    fun signOut(v: View) {
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(applicationContext, Login::class.java)
        startActivity(intent)
        finish()
    }
}