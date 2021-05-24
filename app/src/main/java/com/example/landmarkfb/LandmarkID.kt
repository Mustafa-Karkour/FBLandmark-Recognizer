package com.example.landmarkfb

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.method.ScrollingMovementMethod
import android.util.Base64
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.tasks.Task
import com.google.firebase.functions.FirebaseFunctions
import com.google.gson.*
import kotlinx.android.synthetic.main.activity_landmark_id.*
import org.json.JSONObject
import java.io.ByteArrayOutputStream


class LandmarkID : AppCompatActivity() {
    lateinit var bitmapImg: Bitmap
    lateinit var functions: FirebaseFunctions
    lateinit var queue: RequestQueue
    var imgUri: Uri? = null
    var latitude: Double? = null
    var longitude: Double? = null
    val TAG = "LandmarkID"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_landmark_id)
        queue = Volley.newRequestQueue(this)
    }

    fun openGallery(v: View) {
        var intent: Intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"

        startActivityForResult(intent, 100)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode ==100 && resultCode == Activity.RESULT_OK && data != null) {
            imgLocate.setImageURI(data?.data)
            // Change constraints of Gallery button
            val params = btnGallery.layoutParams as ConstraintLayout.LayoutParams
            params.topToBottom = imgLocate.id
            btnGallery.requestLayout()

            // Remove on-screen text and replace with buttons
            txtSelect.visibility = View.GONE
            imgLocate.visibility = View.VISIBLE
            btnLocate.visibility = View.VISIBLE

            // Save Uri of image and convert it to a bitmap
            imgUri = data?.data
            bitmapImg = MediaStore.Images.Media.getBitmap(this.contentResolver, imgUri)
        }
    }

    fun detectLandmark(v: View) {
        progressLocate.visibility = View.VISIBLE

        // Remove buttons after clicking
        btnGallery.visibility = View.GONE
        btnLocate.visibility = View.GONE

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
        feature.add("maxResults", JsonPrimitive(1))
        feature.add("type", JsonPrimitive("LANDMARK_DETECTION"))
        val features = JsonArray()
        features.add(feature)
        request.add("features", features)

        annotateImage(request.toString())
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.d(TAG, "failed")
                    // Task failed with an exception
                    Toast.makeText(this, "Detection failed", Toast.LENGTH_SHORT).show()
                    btnGallery.visibility = View.VISIBLE
                    btnLocate.visibility = View.VISIBLE
                    progressLocate.visibility = View.GONE
                } else {
                    Log.d(TAG, "success")
                    // Task completed successfully
                    tvResult.movementMethod = ScrollingMovementMethod()
                    parseResult(task)
                }
            }
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

    private fun parseResult(task: Task<JsonElement>) {
        Log.d(TAG, task.result!!.toString())
        val resultArray = task.result!!.asJsonArray[0].asJsonObject["landmarkAnnotations"].asJsonArray
        if (resultArray.size() == 0) {
            val toast = Toast.makeText(this, "Detection failed\nPlease try again or use a different image", Toast.LENGTH_SHORT)
            val v = toast.view.findViewById<TextView>(android.R.id.message)
            v.gravity = Gravity.CENTER
            toast.show()

            btnGallery.visibility = View.VISIBLE
            btnLocate.visibility = View.VISIBLE
            progressLocate.visibility = View.GONE
        } else {
            for (label in resultArray) {
                val labelObj = label.asJsonObject
                val landmarkName = labelObj["description"]
                val latLng = labelObj["locations"].asJsonArray[0].asJsonObject["latLng"]
                latitude = latLng.asJsonObject["latitude"].toString().toDoubleOrNull()
                longitude = latLng.asJsonObject["longitude"].toString().toDoubleOrNull()
                findPageID(landmarkName.toString())
            }
        }
    }

    private fun findPageID(landmarkName: String) {
        // Initialize Request Queue
        var url = "https://en.wikipedia.org/w/api.php?format=json&action=query&list=search"
        val resultLimit = 1
        url += "&srlimit=$resultLimit&srsearch=$landmarkName"

        // Request a string response from the provided URL.
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                val jsonObject: JSONObject = response.getJSONObject("query")
                val searchArray = jsonObject.getJSONArray("search")
                for (i in 0 until searchArray.length()) {
                    val landmarkTitle = searchArray.getJSONObject(i).getString("title")
                    val pageID = searchArray.getJSONObject(i).getInt("pageid")
                    displayExtract(pageID)
                }
            },
            { error ->
                Toast.makeText(this, "Could not find page", Toast.LENGTH_SHORT).show()
                Log.d(TAG, error.message)
            }
        )
        // Add a tag to the request
        jsonObjectRequest.setTag(TAG)
        // Add the request to the RequestQueue.
        queue.add(jsonObjectRequest)
    }

    private fun displayExtract(pageID: Int) {
        var url = "https://en.wikipedia.org/w/api.php?format=json&action=query&prop=extracts&exintro&explaintext&redirects=1"
        url += "&pageids=$pageID"

        // Request a string response from the provided URL.
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                val jsonObject: JSONObject =
                    response.getJSONObject("query").getJSONObject("pages").getJSONObject(
                        pageID.toString()
                    )
                var title = jsonObject.getString("title")
                val extract = jsonObject.getString("extract")
                progressLocate.visibility = View.GONE
                tvTitle.text = title
                tvResult.text = extract
                tvResult.visibility = View.VISIBLE
                btnShowInJournal.visibility = View.VISIBLE
                btnShowOnMap.visibility = View.VISIBLE
            },
            { error ->
                Log.d(TAG, error.message)
            }
        )

        // Add a tag to the request
        jsonObjectRequest.setTag(TAG)
        // Add the request to the RequestQueue.
        queue.add(jsonObjectRequest)
    }

    protected override fun onStop() {
        super.onStop()
        queue.cancelAll(TAG)
    }

    fun showInJournal(v: View) {
        intent = Intent(this, AddNote::class.java)
        intent.putExtra("Image uri", imgUri)
        intent.putExtra("landmark name", tvTitle.text)
        // To get it back, use Uri imgUri = intent.getParcelableExtra("Image uri")
        // You can also send it as a string, then convert it back
        startActivity(intent)
    }

    fun showOnMap(v: View) {

        intent = Intent(this, NearbyPlaces::class.java)
        intent.putExtra("landmark name", tvTitle.text)
        intent.putExtra("latitude", latitude)
        intent.putExtra("longitude", longitude)

        startActivity(intent)

    }
}