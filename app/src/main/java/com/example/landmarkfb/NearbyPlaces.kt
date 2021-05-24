package com.example.landmarkfb

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.GoogleMap.*
import com.google.android.gms.maps.model.*
import kotlinx.android.synthetic.main.activity_map.*
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class NearbyPlaces : AppCompatActivity(), OnMapReadyCallback,
    OnPoiClickListener,
    OnMarkerClickListener,
    OnMapClickListener
{
    //Data class for storing API response results
    data class Place(val name:String = "", val latlng:LatLng = LatLng(0.0, 0.0))

    var locationRequest:LocationRequest? = null
    val locationRequestCode = 2
    lateinit var locationCallback: LocationCallback
    lateinit var fusedLocationClient: FusedLocationProviderClient
    lateinit var currentLocation : LatLng

    lateinit var mMap: GoogleMap
    lateinit var landmarkLocation: LatLng
    var landmarkName:String? = null
    var cameFromLandmark = false

    //Reference to all markers
    val markers = ArrayList<Marker>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        //Check if google play services are installed before openning the map
        val googleServicesAvailable = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)
        if (googleServicesAvailable != ConnectionResult.SUCCESS)
        {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Google Play Services Required")
            builder.setMessage("Google Play Services are required to use the map")

            builder.setPositiveButton("Ok") { dialog, _ ->
                dialog.dismiss()
                finish()
            }

            builder.setOnDismissListener { finish() }

            builder.create().show()
        }

        else
        {
            //Request location access permissions
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), locationRequestCode)
            }
            else {
                //Create and initialize map
                startMapCreation()
            }
        }

    }

    //Called as soon as the map is open and ready for use
    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {

        //Display the map on current location
        mMap = googleMap
        mMap.isMyLocationEnabled = true
        //Disable my location button if came from landmark page
        mMap.uiSettings.isMyLocationButtonEnabled = !cameFromLandmark

        //Initialize listeners to listen to map clicks
        mMap.setOnPoiClickListener(this)
        mMap.setOnMarkerClickListener(this)
        mMap.setOnMapClickListener(this)

        //Initialize camera on current location

        mMap.moveCamera(CameraUpdateFactory.zoomTo(16f))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLocation))

        //Move to landmark (if coming from landmark ID)
        if (cameFromLandmark) {
            mMap.addMarker(MarkerOptions().title(landmarkName).position(currentLocation).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)))
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 16f))
        }
    }

    //Used for Google Places API request
    fun searchLocations(type:String = "", markerColor: Float)
    {
        //Get location and prepare the API request

        val result = ArrayList<Place>()
        val client = OkHttpClient()
        val url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=" + currentLocation.latitude +", " + currentLocation.longitude + "&rankby=distance&type=" + type + "&key=AIzaSyAhUhs2q209k-NFFlS7yMF5I3Wk6WdIHmY"

        //Request JSON from Places API
        val request = Request.Builder().url(url).build()
        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response)
            {
                response.use {

                    //If no response, throw an exception
                    if (!response.isSuccessful)
                        throw IOException("Unexpected code $response")

                    //If response successful, store the response as a string
                    val myResponse = response?.body?.string()

                    //Convert response to JSON object
                    val jsonObject = JSONObject(myResponse)

                    //Parse the response for name and location
                    val jsonArray = jsonObject.optJSONArray("results")

                    //Store all places in result array list
                    for (i in 0 until jsonArray.length()) {
                        val jsonObject = jsonArray.getJSONObject(i)
                        val location = jsonObject.getJSONObject("geometry").getJSONObject("location")
                        val lat = location.optString("lat").toDouble()
                        val lng = location.optString("lng").toDouble()
                        val name = jsonObject.optString("name")
                            result.add(Place(name, LatLng(lat, lng)))
                    }

                    //Go to UI thread and to be able to interact with the Views and map
                    runOnUiThread {

                        //Add markers for the received results
                        for (place in result)
                        {
                            markers.add(mMap.addMarker(MarkerOptions().title(place.name).position(place.latlng).icon(BitmapDescriptorFactory.defaultMarker(markerColor))))
                        }
                        //Allow buttons to be clickable again and reset background color
                        setClickable(true)
                        parent_buttons.background = null
                    }
                }
            }
        })

    }


    fun onButtonClick(v:View)
    {
        //Initialize variables
        var type = ""
        var markerColor = 0f

        //Move camera to current location and prevent button clicks until operation is finished
        mMap.animateCamera(CameraUpdateFactory.newLatLng(currentLocation))
        clearMarkers()
        setClickable(false)

        //Get the appropriate type and marker color for the clicked button
        when(v.id)
        {
            bt_landmark.id -> { type =  "tourist_attraction" ; markerColor = BitmapDescriptorFactory.HUE_AZURE }
            bt_restaurant.id -> { type = "restaurant" ; markerColor = BitmapDescriptorFactory.HUE_GREEN }
            bt_hospital.id -> { type = "hospital" ; markerColor = BitmapDescriptorFactory.HUE_ROSE }
            bt_atm.id -> { type = "atm" ; markerColor = BitmapDescriptorFactory.HUE_YELLOW }
        }

        //Initiate API request and change background color to match clicked button
        searchLocations(type, markerColor)
        parent_buttons.background = v.background

    }


    fun setClickable(clickable: Boolean)
    {
        //Store reference to all buttons
        val buttons = ArrayList<ImageView>()

        buttons.add(bt_landmark)
        buttons.add(bt_restaurant)
        buttons.add(bt_hospital)
        buttons.add(bt_atm)

        //Adjust the clickability and visibility of all buttons
        for (btn in buttons)
        {
            btn.isClickable = clickable

            if (clickable)
            {
                btn.alpha = 1f
            }
            else
            {
                btn.alpha = 0.3f
            }
        }
    }

    fun clearMarkers()
    {
        for (m in markers)
        {
            m.remove()
        }

        markers.clear()
    }

    @SuppressLint("MissingPermission")
    fun startMapCreation()
    {

        //Check if coming from landmarkID page and retrieve the data from the intent
        val lat: Double = intent.getDoubleExtra("latitude", 0.0)
        val long: Double = intent.getDoubleExtra("longitude", 0.0)
        landmarkName = intent.getStringExtra("landmark name")

        //If came from Landmark page name set current location to landmark location
        if (landmarkName != null) {
            cameFromLandmark = true
            landmarkLocation = LatLng(lat, long)
            currentLocation = landmarkLocation
            createMap()
        }

        else
        {
            //Set up location request to keep updating current location
            initializeLocationUpdates()

            //Get current location
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                currentLocation = LatLng(location!!.latitude, location!!.longitude)
                createMap()
            }
        }

    }

    fun createMap()
    {
        //Map options
        val options = GoogleMapOptions()
        options.zoomControlsEnabled(true)

        //Create map after obtaining current location
        val mapFragment = SupportMapFragment.newInstance(options)
        supportFragmentManager
            .beginTransaction()
            .add(R.id.mapContainer, mapFragment)
            .commit()
        mapFragment.getMapAsync(this)
    }

    /*************************
    Location updates functions
    *************************/

    fun initializeLocationUpdates()
    {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createLocationRequest()
        createLocationCallback()
        startLocationUpdates()
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(locationRequest,
            locationCallback,
            Looper.getMainLooper())
    }

    fun createLocationRequest() {
        locationRequest = LocationRequest.create()?.apply {
            interval = 10000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }

    fun createLocationCallback()
    {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                for (location in locationResult.locations){
                    currentLocation = LatLng(location.latitude, location.longitude)
                }
            }
        }
    }



    /****************************************
    Listener functions that handle map clicks
    ****************************************/

    override fun onPoiClick(poi: PointOfInterest) {

        clearMarkers()
        mMap.uiSettings.isMapToolbarEnabled = false

        //Add marker and center it on the camera
        val marker = mMap.addMarker(MarkerOptions().position(poi.latLng).title(poi.name))
        marker.showInfoWindow()
        markers.add(marker)
        mMap.animateCamera(CameraUpdateFactory.newLatLng(marker.position))

    }

    override fun onMarkerClick(p0: Marker?): Boolean {

        //Show buttons that open google maps on marker click
        mMap.uiSettings.isMapToolbarEnabled = true
        return false
    }

    override fun onMapClick(p0: LatLng?) {
        clearMarkers()
    }

    /**************************
    Handle location permission
    **************************/
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationRequestCode && grantResults.contains(PackageManager.PERMISSION_GRANTED))
        {
            startMapCreation()
        }
        else
        {
            finish()
        }
    }
}




