package com.example.landmarkfb

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionLatLng
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    lateinit var bitmapImg: Bitmap



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun clickToPredict(v:View) {

        val image = FirebaseVisionImage.fromBitmap(bitmapImg)
        val detector = FirebaseVision.getInstance().visionCloudLandmarkDetector

        detector.detectInImage(image)
            .addOnSuccessListener {
                // Task succeeded!

                var sLandmarkName:String =""
                var sEntityId:String =""
                var sConfidence:String =""

                var sLatitude:String =""
                var sLongitude:String =""

                for (landmark in it) {
                    // Do something with landmark
                    val landmarkName = landmark.landmark
                    sLandmarkName = landmarkName.toString()

                    val entityId = landmark.entityId
                    sEntityId = entityId.toString()

                    val confidence = Math.round(landmark.confidence*100)
                    sConfidence = confidence.toString()

                    val locations = landmark.locations

                    var latitude:Double = 0.0
                    var longitude:Double= 0.0

                    for(loc in locations){
                        latitude = loc.latitude
                        sLatitude = latitude.toString()

                        longitude = loc.longitude
                        sLongitude=longitude.toString()
                    }


                }

                tvResult.text = "LandmarkName: $sLandmarkName\nEntityID: $sEntityId"+
                        "\nConfidence: $sConfidence\nLatitude: $sLatitude\nLongitude: $sLongitude"
            }
            .addOnFailureListener {
                // Task failed with an exception
                displayMsg("Task failed with an exception")


            }
    }

    fun openGallery(v: View){
        var intent:Intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"

        startActivityForResult(intent,100)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        img.setImageURI(data?.data)

        var uri: Uri?= data?.data

        // convert the image to bitmap using it uri
        bitmapImg = MediaStore.Images.Media.getBitmap(this.contentResolver,uri)

    }

    fun displayMsg(msg:String){
        Toast.makeText(this,msg,Toast.LENGTH_LONG).show()
    }

}