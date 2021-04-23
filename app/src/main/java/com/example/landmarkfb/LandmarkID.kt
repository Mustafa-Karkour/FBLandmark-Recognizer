package com.example.landmarkfb

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Typeface
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.Gravity
import android.view.View
import android.widget.Toast
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.gson.*
import kotlinx.android.synthetic.main.activity_landmark_id.*
import kotlinx.android.synthetic.main.activity_main.*
import java.io.ByteArrayOutputStream

class LandmarkID : AppCompatActivity() {
    lateinit var bitmapImg: Bitmap
    lateinit var functions: FirebaseFunctions
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_landmark_id)
    }

    fun clickToPredict(v: View) {

        //val image = FirebaseVisionImage.fromBitmap(bitmapImg)
        //val detector = FirebaseVision.getInstance().visionCloudLandmarkDetector

        // Initialize instance of Cloud Functions
        functions = FirebaseFunctions.getInstance()

        // Convert image to base64 encoded string
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmapImg.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val imageBytes: ByteArray = byteArrayOutputStream.toByteArray()
        val base64encoded = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

        // Create json request to cloud vision
        val request = JsonObject()
        // Add image to request
        val image = JsonObject()
        image.add("content", JsonPrimitive(base64encoded))
        request.add("image", image)
        //Add features to the request
        val feature = JsonObject()
        feature.add("maxResults", JsonPrimitive(5))
        feature.add("type", JsonPrimitive("LANDMARK_DETECTION"))
        val features = JsonArray()
        features.add(feature)
        request.add("features", features)

        annotateImage(request.toString())
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    // Task failed with an exception
                    Toast.makeText(this, "Detection failed", Toast.LENGTH_SHORT)
                } else {
                    // Task completed successfully
                    displayDetails(task)
                }
            }

//        detector.detectInImage(image)
//            .addOnSuccessListener {
//                // Task succeeded!
//
//                var sLandmarkName:String =""
//                var sEntityId:String =""
//                var sConfidence:String =""
//
//                var sLatitude:String =""
//                var sLongitude:String =""
//
//                for (landmark in it) {
//                    // Do something with landmark
//                    val landmarkName = landmark.landmark
//                    sLandmarkName = landmarkName.toString()
//
//                    val entityId = landmark.entityId
//                    sEntityId = entityId.toString()
//
//                    val confidence = Math.round(landmark.confidence*100)
//                    sConfidence = confidence.toString()
//
//                    val locations = landmark.locations
//
//                    var latitude:Double = 0.0
//                    var longitude:Double= 0.0
//
//                    for(loc in locations){
//                        latitude = loc.latitude
//                        sLatitude = latitude.toString()
//
//                        longitude = loc.longitude
//                        sLongitude=longitude.toString()
//                    }
//
//
//                }
//
//                tvResult.text = "LandmarkName: $sLandmarkName\nEntityID: $sEntityId"+
//                        "\nConfidence: $sConfidence\nLatitude: $sLatitude\nLongitude: $sLongitude"
//            }
//            .addOnFailureListener {
//                // Task failed with an exception
//                displayMsg("Task failed with an exception")
//
//
//            }
    }

    private fun annotateImage(requestJson: String): Task<JsonElement> {
        return functions
            .getHttpsCallable("annotateImage")
            .call(requestJson)
            .continueWith { task ->
                // This continuation runs on either success or failure, but if the task
                // has failed then result will throw an Exception which will be
                // propagated down.
                val result = task.result?.data
                JsonParser.parseString(Gson().toJson(result))
            }
    }

    private fun displayDetails(task: Task<JsonElement>) {
        tvResult.setTypeface(Typeface.DEFAULT)
        tvResult.gravity = Gravity.START
        var result = ""
        for (label in task.result!!.asJsonArray[0].asJsonObject["landmarkAnnotations"].asJsonArray) {
            val labelObj = label.asJsonObject
            val landmarkName = labelObj["description"]
            //val entityId = labelObj["mid"]
            //val score = labelObj["score"]
            //val bounds = labelObj["boundingPoly"]
            result = "Landmark Name: ${landmarkName.toString()}"

            // Multiple locations are possible, e.g., the location of the depicted
            // landmark and the location the picture was taken.
            result += "\n\nLocation:"
            for (loc in labelObj["locations"].asJsonArray) {
                val latitude = loc.asJsonObject["latLng"].asJsonObject["latitude"]
                val longitude = loc.asJsonObject["latLng"].asJsonObject["longitude"]
                result += "\nLatitude: $latitude\nLongitude: $longitude"
            }
            tvResult.text = result
        }
    }

    fun openGallery(v: View) {
        var intent: Intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"

        startActivityForResult(intent, 100)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        img.setImageURI(data?.data)
        var uri: Uri? = data?.data

        // convert the image to bitmap using its uri
        bitmapImg = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
    }

//    fun signOut(v: View) {
//        FirebaseAuth.getInstance().signOut()
//        val intent = Intent(applicationContext, Login::class.java)
//        startActivity(intent)
//        finish()
//    }
}