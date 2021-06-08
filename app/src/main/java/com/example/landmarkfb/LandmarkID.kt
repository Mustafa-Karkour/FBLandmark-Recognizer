package com.example.landmarkfb

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
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
    private val REQUEST_IMAGE_CAPTURE = 1
    private val PICK_IMAGE = 2
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

        // Start the RequestQueue
        queue = Volley.newRequestQueue(this)
    }

    fun openCamera(view: View) {
        // Implicit intent to open the camera and get a picture
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        } catch (e: ActivityNotFoundException) {
            // display error state to the user
            Log.d(TAG, e.message)
        }
    }

    fun openGallery(v: View) {
        // Implicit intent to show only the images in the phone
        var intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        try {
            startActivityForResult(intent, PICK_IMAGE)
        } catch (e: ActivityNotFoundException) {
            // display error state to the user
            Log.d(TAG, e.message)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Check if there is a result from the camera
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK && data != null) {
            // Save image as a bitmap and display it
            bitmapImg = data?.extras?.get("data") as Bitmap
            imgLocate.setImageBitmap(bitmapImg)

            // Remove on-screen text and replace with buttons
            txtSelect.visibility = View.GONE
            imgLocate.visibility = View.VISIBLE
            btnLocate.visibility = View.VISIBLE
        }

        // Check if there is a result from the gallery
        if(requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            // Display the image
            imgLocate.setImageURI(data?.data)

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
        // Show progress bar
        progressLocate.visibility = View.VISIBLE

        // Remove buttons after pressing
        btnOpenCamera.visibility = View.GONE
        btnGallery.visibility = View.GONE
        btnLocate.visibility = View.GONE

        // Initialize instance of Cloud Functions
        functions = FirebaseFunctions.getInstance()

        // Convert image from bitmap to base64 encoded string
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
        // Add features to the request
        val feature = JsonObject()
        // Limit number of results
        feature.add("maxResults", JsonPrimitive(1))
        feature.add("type", JsonPrimitive("LANDMARK_DETECTION"))
        val features = JsonArray()
        features.add(feature)
        request.add("features", features)

        // Call the firebase function with our json request with a listener for when it ends
        annotateImage(request.toString())
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.d(TAG, "failed")
                    // Image annotation failed
                    Toast.makeText(this, "Detection failed", Toast.LENGTH_SHORT).show()

                    // Show the buttons on screen again
                    btnOpenCamera.visibility = View.VISIBLE
                    btnGallery.visibility = View.VISIBLE
                    btnLocate.visibility = View.VISIBLE
                    progressLocate.visibility = View.GONE
                } else {
                    Log.d(TAG, "success")
                    // Task completed successfully
                    // Set the text view to scrollable
                    tvResult.movementMethod = ScrollingMovementMethod()
                    // Send the result to be analyzed
                    parseResult(task)
                }
            }
    }

    private fun annotateImage(requestJson: String): Task<JsonElement> {
        // Calls a firebase function with our request json and returns the result
        return functions
            .getHttpsCallable("annotateImage")
            .call(requestJson)
            .continueWith { task ->
                // Returns an object of type Task<JsonElement>
                // If the request is successful, convert the result to Json, then a parseable JsonElement
                val result = task.result?.data
                JsonParser.parseString(Gson().toJson(result))
            }
    }

    private fun parseResult(task: Task<JsonElement>) {
        Log.d(TAG, task.result!!.toString())
        // Get the required section of the response as a JsonArray
        val resultArray = task.result!!.asJsonArray[0].asJsonObject["landmarkAnnotations"].asJsonArray

        // If the array is empty, display an error message and reset the positions of the buttons
        if (resultArray.size() == 0) {
            val toast = Toast.makeText(this, "Detection failed\nPlease try again or use a different image", Toast.LENGTH_SHORT)
            // This code is to center the text in the toast message
            val v = toast.view.findViewById<TextView>(android.R.id.message)
            v.gravity = Gravity.CENTER
            toast.show()

            // Show buttons if detection failed
            btnOpenCamera.visibility = View.VISIBLE
            btnGallery.visibility = View.VISIBLE
            btnLocate.visibility = View.VISIBLE
            progressLocate.visibility = View.GONE
        } else {
            // Parse the result of the request
            for (label in resultArray) {
                val labelObj = label.asJsonObject
                // Get the name of the landmark
                val landmarkName = labelObj["description"]
                // Store the latitude and longitude of the landmark for the map
                val latLng = labelObj["locations"].asJsonArray[0].asJsonObject["latLng"]
                latitude = latLng.asJsonObject["latitude"].toString().toDoubleOrNull()
                longitude = latLng.asJsonObject["longitude"].toString().toDoubleOrNull()
                // Use the identified landmark name to get its Wikipedia entry
                findPageID(landmarkName.toString())
            }
        }
    }

    private fun findPageID(landmarkName: String) {
        // Build the request url to find the page ID using the landmark name
        var url = "https://en.wikipedia.org/w/api.php?format=json&action=query&list=search"
        val resultLimit = 1
        url += "&srlimit=$resultLimit&srsearch=$landmarkName"

        // Request a string response from the provided URL using Volley
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                // Parse the result for the page ID of the landmark page
                val jsonObject: JSONObject = response.getJSONObject("query")
                val searchArray = jsonObject.getJSONArray("search")
                for (i in 0 until searchArray.length()) {
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
        // Build the request url to find the Wikipedia entry using the page ID
        var url = "https://en.wikipedia.org/w/api.php?format=json&action=query&prop=extracts&exintro&explaintext&redirects=1"
        url += "&pageids=$pageID"

        // Request a string response from the provided URL using Volley
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                val jsonObject: JSONObject =
                    response.getJSONObject("query").getJSONObject("pages").getJSONObject(
                        pageID.toString()
                    )

                // Show the title and extract on the screen
                var title = jsonObject.getString("title")
                val extract = jsonObject.getString("extract")
                progressLocate.visibility = View.GONE
                tvTitle.text = title
                tvResult.text = extract
                tvResult.visibility = View.VISIBLE

                // Show the buttons to go to the journal and map
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
        // Cancel all requests in the RequestQueue
        queue.cancelAll(TAG)
    }

    fun showInJournal(v: View) {
        // Create an intent that goes to the journal with the image and landmark name
        intent = Intent(this, AddNote::class.java)
        intent.putExtra("Image uri", imgUri)
        intent.putExtra("landmark name", tvTitle.text)
        startActivity(intent)
    }

    fun showOnMap(v: View) {
        // Create an intent that goes to the map with the landmark name and location coordinates
        intent = Intent(this, NearbyPlaces::class.java)
        intent.putExtra("landmark name", tvTitle.text)
        intent.putExtra("latitude", latitude)
        intent.putExtra("longitude", longitude)
        startActivity(intent)
    }
}