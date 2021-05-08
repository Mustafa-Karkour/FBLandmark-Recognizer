package com.example.landmarkfb

import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide

import kotlinx.android.synthetic.main.activity_note_details.*
import kotlinx.android.synthetic.main.content_note_details.*

class NoteDetails : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_details)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        //from the noteAdapter
        var dataIntent = intent

        var currTitle = dataIntent.getStringExtra("title")
        var currContent = dataIntent.getStringExtra("content")
        var currDate = dataIntent.getStringExtra("date")
        var currColor:Int = dataIntent.getIntExtra("color",0)
        //-----------------------

        var imgUrl:String = dataIntent.getStringExtra("url")
        var URL:Uri = Uri.parse(imgUrl)
        var img:ImageView = findViewById(R.id.addNoteImg)
        Glide.with(this).load(URL).into(img) //to read the img URL from the Storage section


        tvNoteDetailsTitle.text = currTitle
        etAddNoteContent.text = currContent
        tvImgDate.text = currDate





        NoteDetailsContainer.setBackgroundColor(resources.getColor(currColor,null))


    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        //the back button
        if(item.itemId == android.R.id.home){
            onBackPressed() //go to the previous page
        }

        return super.onOptionsItemSelected(item)
    }

}