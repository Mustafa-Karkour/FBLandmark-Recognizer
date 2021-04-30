package com.example.landmarkfb

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.landmarkfb.adapter.NoteAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_journal.*
import kotlin.collections.ArrayList
import kotlin.collections.Map

class Journal : AppCompatActivity() {

    lateinit var db:FirebaseDatabase
    lateinit var myRef:DatabaseReference
    lateinit var mAuth:FirebaseAuth

    var titles = ArrayList<String>()
    var contents = ArrayList<String>()
    var imgUrls = ArrayList<String>()
    var imgDates = ArrayList<String>()
    var noteIDs = ArrayList<String>()





    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_journal)

        mAuth = FirebaseAuth.getInstance()
        var currUser = mAuth.currentUser
        var currUserEmail = currUser.email
        currUserEmail = currUserEmail.replace(".","-")

        db = FirebaseDatabase.getInstance()
        myRef = db.getReference("Notes").child(currUserEmail)


        titles.clear()
        contents.clear()
        imgUrls.clear()
        imgDates.clear()
        noteIDs.clear()

        var noteAdapter = NoteAdapter(titles,contents,imgUrls,imgDates,noteIDs)
        rvNoteList.layoutManager = StaggeredGridLayoutManager(2,StaggeredGridLayoutManager.VERTICAL)
        rvNoteList.adapter = noteAdapter



        readFromDB(myRef,noteAdapter)







    }

    //read note details from DB
    private fun readFromDB(myRef: DatabaseReference,noteAdapter:NoteAdapter) {

        myRef.addValueEventListener(object:ValueEventListener{

            override fun onCancelled(error: DatabaseError) {

            }

            override fun onDataChange(snapshot: DataSnapshot) {

                for(dataSnapshot in snapshot.children){

                    //var noteID = dataSnapshot.key

                    var map = dataSnapshot.getValue() as Map<String, String>


                    //a condition to avoid showing duplicate data in the RV
                    if(!noteIDs.contains(map["noteID"].toString())){

                        titles.add(map["title"].toString())
                        contents.add(map["content"].toString())
                        imgUrls.add(map["imgURL"].toString())
                        imgDates.add(map["date"].toString())
                        noteIDs.add(map["noteID"].toString())
                    }

                }
                noteAdapter.notifyDataSetChanged()






            }
        })



    }






    fun addNoteClicked(v: View){

        var i = Intent(applicationContext,AddNote::class.java)
        v.context.startActivity(i)

    }

}