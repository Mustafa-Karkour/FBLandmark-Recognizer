package com.example.landmarkfb.adapter

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.landmarkfb.Journal
import com.example.landmarkfb.NoteDetails
import com.example.landmarkfb.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.note_view_item.view.*
import kotlin.random.Random

class NoteAdapter(private val titles:ArrayList<String>,
                  private val contents:ArrayList<String>,
                  private val imgURLS:ArrayList<String>,
                  private val imgDates:ArrayList<String>,
                  private val noteIDs:ArrayList<String>):RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {

        //"objectifying" note_view_item  layout
        var itemView = LayoutInflater.from(parent.context).inflate(R.layout.note_view_item,parent,false)

        return NoteViewHolder(itemView)
    }


    override fun onBindViewHolder(currHolder: NoteViewHolder, currPosition: Int) {

        var currTitle = titles.get(currPosition)
        var currContent = contents.get(currPosition)
        var currImgURL = imgURLS.get(currPosition)
        var currDate = imgDates.get(currPosition)
        var currNoteID = noteIDs.get(currPosition)






        currHolder.deleteIcon.setOnClickListener { view ->


            val builder = AlertDialog.Builder(view.context)
            builder.setTitle("Delete Note")
            builder.setMessage("Are you sure you want to delete this note?")
            builder.setIcon(android.R.drawable.ic_menu_delete)

            builder.setPositiveButton("DELETE"){ _,_ ->


                //deleteNote from db and Storage
                 deleteNoteInfo(view,currNoteID,currImgURL)

                 //deleteNote from UI
                titles.remove(currTitle)
                contents.remove(currContent)
                imgURLS.remove(currImgURL)
                imgDates.remove(currDate)
                noteIDs.remove(currNoteID)
                notifyDataSetChanged()



            }

            builder.setNeutralButton("CANCEL"){_ , _ ->

            }

            val alertDialog: AlertDialog = builder.create()
            alertDialog.setCancelable(false)
            alertDialog.show()




        }







        //to ensure that each note's title or description looks neat
        //too long title or description
        if(currTitle.length>13) //14 or more characters is too long for a title
            currHolder.tvNoteTitle.text = currTitle.substring(0,14)+"..."
        else{

            //to be continue in the note page itself
            currHolder.tvNoteTitle.text = currTitle

        }
        if(currContent.length>19)
            currHolder.tvNoteContent.text = currContent.substring(0,20)+"..."
        else{
            //to be continue in the note page itself
            currHolder.tvNoteContent.text = currContent
        }


        var ranColor = getRandColor()
        currHolder.cardNote.setCardBackgroundColor(currHolder.cnView.resources.getColor(ranColor,null))








        // a note is clicked from the recyclerview
        currHolder.cnView.setOnClickListener { view ->

            val toNoteDetails = Intent(view.context, NoteDetails::class.java)
            toNoteDetails.putExtra("title",currTitle)
            toNoteDetails.putExtra("content",currContent)
            toNoteDetails.putExtra("color",ranColor)
            toNoteDetails.putExtra("url",currImgURL)
            toNoteDetails.putExtra("date",currDate)
            view.context.startActivity(toNoteDetails)

        }

    }




    //delete note from the Database and Storage section
    private fun deleteNoteInfo(v:View,currNoteID: String, fImgURL:String) {

        var db = FirebaseDatabase.getInstance()
        var fStorage = FirebaseStorage.getInstance()




        var mAuth = FirebaseAuth.getInstance()
        var currUser = mAuth.currentUser

        var userEmail:String = currUser.email

        userEmail = userEmail.replace(".","-")



        var myStoreRef = fStorage.getReferenceFromUrl(fImgURL).delete()
            .addOnSuccessListener { success ->

               // Toast.makeText(v.context,"Image deleted from the Storage",Toast.LENGTH_LONG).show()

                //Then delete the note info from firebase DB
                var myRef = db.getReference("Notes").child(userEmail).child(currNoteID).removeValue()
                    .addOnSuccessListener { success ->
                        //Toast.makeText(v.context,"Note info deleted from DB",Toast.LENGTH_LONG).show()



                    }.addOnFailureListener{error ->
                        Toast.makeText(v.context,"DB Failed: "+error.message,Toast.LENGTH_LONG).show()
                    }



            }.addOnFailureListener{ error ->

                Toast.makeText(v.context,"Storage Failed: "+error.message,Toast.LENGTH_LONG).show()

            }











    }






    private fun getRandColor(): Int {

        var colorCodes = ArrayList<Int>()

        colorCodes.add(R.color.yellow)
        colorCodes.add(R.color.lightGreen)
        colorCodes.add(R.color.pink)
        colorCodes.add(R.color.lightPurple)
        colorCodes.add(R.color.skyblue)
        colorCodes.add(R.color.gray)
        colorCodes.add(R.color.red)
        colorCodes.add(R.color.blue)
        colorCodes.add(R.color.greenlight)

        var ran = Random
        val num = ran.nextInt(colorCodes.size)

        return colorCodes.get(num)


    }


    override fun getItemCount() = titles.size

    //handling the widgets inside the itemView 'layout'
    class NoteViewHolder(itemView: View):RecyclerView.ViewHolder(itemView){

        var tvNoteContent:TextView = itemView.note_content
        var tvNoteTitle:TextView = itemView.note_title
        var cardNote = itemView.noteCard

        var deleteIcon:ImageView = itemView.deleteIcon




        var cnView = itemView







    }

}