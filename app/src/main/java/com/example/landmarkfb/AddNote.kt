package com.example.landmarkfb

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.landmarkfb.model.NoteModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.android.synthetic.main.activity_add_note.*
import kotlinx.android.synthetic.main.content_add_note.*
import java.text.DateFormat
import java.time.LocalDate
import java.util.*

class AddNote : AppCompatActivity() {

    lateinit var mAuth: FirebaseAuth

    lateinit var db: FirebaseDatabase
    lateinit var myDBRef:DatabaseReference
    lateinit var fStorage: FirebaseStorage
    lateinit var fStorageRef:StorageReference
    var imgURL:Uri? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_note)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        //Coming from LandmarkID class
        var imgUri = intent.getParcelableExtra<Uri>("Image uri")
        var landmarkName:String? = intent.getStringExtra("landmark name")
        if(imgUri != null) {

            imgURL = imgUri
            addNoteImg.setImageURI(imgURL)
        }
        if(landmarkName != null){
            etAddNoteTitle.setText(landmarkName)
        }
        //-------------------------------------

        pBLoadNote.visibility = View.INVISIBLE
        btnSaveNote.visibility = View.VISIBLE

        mAuth = FirebaseAuth.getInstance()




        db = FirebaseDatabase.getInstance()

        myDBRef = db.getReference("Notes")

        fStorage = FirebaseStorage.getInstance()
        fStorageRef = fStorage.getReference("Images")








    }

    fun AddNoteClicked(v: View){

        var title = etAddNoteTitle.text.toString()
        var content = etAddNoteContent.text.toString()




        if(title.isEmpty()){
            Toast.makeText(applicationContext,"Please insert a title",Toast.LENGTH_LONG).show()
        }
        else if(content.isEmpty()){

            etAddNoteContent.setError("Please insert a description")

        }else if(imgURL == null){
            Toast.makeText(applicationContext,"Please select an image",Toast.LENGTH_LONG).show()

        }
        else{




            uploadToFB(title,content,imgURL!!) //upload note details to both storage and db


        }



    }

    private fun getCurrDate():String{

        var calender:Calendar = Calendar.getInstance()

        var currDate:String = DateFormat.getDateInstance(DateFormat.MEDIUM).format(calender.time)

        return currDate


    }

    private fun uploadToFB(title:String,content:String,imgURL: Uri) {

        var currUser = mAuth.currentUser
        var currDate = getCurrDate()





        var fileRef = fStorageRef.child(currUser.email.toString()).child(System.currentTimeMillis().toString())



            fileRef.putFile(imgURL).addOnSuccessListener { taskSnapshot  ->

            fileRef.downloadUrl.addOnSuccessListener { uri ->






                var userEmail = currUser.email
                userEmail = userEmail.replace(".","-") //dot is invalid char in firebaseDB

                var noteID = myDBRef.push().key.toString() //random Key

                var noteMode = NoteModel(
                    title,
                    content,
                    uri.toString(),
                    currDate,
                    noteID
                )

                myDBRef.child(userEmail).child(noteID).setValue(noteMode).addOnSuccessListener { taskSnapshot ->
                    //Toast.makeText(applicationContext,"Uploaded Successfully",Toast.LENGTH_LONG).show()
                    onBackPressed() //go to the previous page

//                    val toJournal = Intent(this,Journal::class.java)
//                    startActivity(toJournal)
//                    finish()
                }.addOnFailureListener{error->

                    Toast.makeText(applicationContext,error.message,Toast.LENGTH_LONG).show()

                }




            }.addOnFailureListener{error ->

                Toast.makeText(applicationContext,error.message,Toast.LENGTH_LONG).show()

            }

        }.addOnProgressListener { progress ->
            pBLoadNote.visibility = View.VISIBLE
            btnSaveNote.visibility = View.INVISIBLE
        }
            .addOnFailureListener{ error ->

            Toast.makeText(applicationContext,error.message,Toast.LENGTH_LONG).show()
                pBLoadNote.visibility = View.INVISIBLE
                btnSaveNote.visibility = View.VISIBLE

        }

    }



    fun addNoteImgClicked(v: View) {
        var intent: Intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"

        startActivityForResult(intent, 100)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode ==100 && resultCode == Activity.RESULT_OK && data != null){

            imgURL = data.data!!
            addNoteImg.setImageURI(imgURL)
        }

    }



    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        //the back button
        if(item.itemId == android.R.id.home){
            onBackPressed() //go to the previous page
        }

        return super.onOptionsItemSelected(item)
    }
}