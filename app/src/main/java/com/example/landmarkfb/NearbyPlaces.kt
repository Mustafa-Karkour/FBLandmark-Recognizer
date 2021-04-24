package com.example.landmarkfb

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.view.View
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener
import com.google.android.gms.maps.GoogleMap.OnPoiClickListener
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener
import com.google.android.gms.maps.GoogleMap.OnMapClickListener
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PointOfInterest
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class NearbyPlaces : AppCompatActivity(), OnMapReadyCallback,
    OnMyLocationButtonClickListener,
    OnPoiClickListener,
    OnMarkerClickListener,
    OnMapClickListener
{
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    var currentLocation: Location? = null
    private lateinit var mMap: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        //Set up location request and get current location
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 2)
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        getLocation()

        //Map options
        val options = GoogleMapOptions()
        options.zoomControlsEnabled(true)

        //Create map
        val mapFragment = SupportMapFragment.newInstance(options)
        supportFragmentManager
            .beginTransaction()
            .add(R.id.mapContainer, mapFragment)
            .commit()
        mapFragment.getMapAsync(this)

    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        getLocation()

        mMap = googleMap
        mMap.isMyLocationEnabled = true
        mMap.setOnMyLocationButtonClickListener(this)
        mMap.setOnPoiClickListener(this)
        mMap.setOnMarkerClickListener(this)
        mMap.setOnMapClickListener(this)

        mMap.moveCamera(CameraUpdateFactory.zoomTo(12f))
        val location = LatLng(currentLocation!!.latitude, currentLocation!!.longitude)
        mMap.moveCamera(CameraUpdateFactory.newLatLng(location))

    }

    override fun onMyLocationButtonClick(): Boolean {
        getLocation()
        val latlng = LatLng(currentLocation!!.latitude, currentLocation!!.longitude)
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latlng, 17f))
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        return true
    }

    override fun onPoiClick(poi: PointOfInterest) {

        mMap.clear()
        mMap.uiSettings.isMapToolbarEnabled = false
        val marker = mMap.addMarker(MarkerOptions().position(poi.latLng).title(poi.name))
        marker.showInfoWindow()
        mMap.animateCamera(CameraUpdateFactory.newLatLng(marker.position))

    }

    override fun onMarkerClick(p0: Marker?): Boolean {
        mMap.uiSettings.isMapToolbarEnabled = true
        return false
    }

    override fun onMapClick(p0: LatLng?) {
        mMap.clear()
    }

    fun getLocation()
    {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 2)
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location : Location? ->
            currentLocation = location

        }
    }

    fun searchLocations(type:String) : ArrayList<Place>
    {
        getLocation()
        val result = ArrayList<Place>()

        val client = OkHttpClient()
        val url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=" + currentLocation!!.latitude +", " + currentLocation!!.longitude + "&radius=50000&type=" + type + "&key=AIzaSyAhUhs2q209k-NFFlS7yMF5I3Wk6WdIHmY"
        val request = Request.Builder().url(url).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful)
                        throw IOException("Unexpected code $response")

                    val myResponse = response?.body?.string()

                    val jsonObject = JSONObject(myResponse)
                    val jsonArray = jsonObject.optJSONArray("results")
                    for (i in 0 until jsonArray.length()) {
                        val jsonObject = jsonArray.getJSONObject(i)
                        val location = jsonObject.getJSONObject("geometry").getJSONObject("location")
                        val lat = location.optString("lat").toDouble()
                        val lng = location.optString("lng").toDouble()
                        val name = jsonObject.optString("name")
                        result.add(Place(name, LatLng(lat, lng)))
                    }

                    runOnUiThread {
                        mMap.clear()
                        for (place in result)
                        {
                            mMap.addMarker(MarkerOptions().title(place.name).position(place.latlng))
                        }
                    }
                }
            }
        })

        return result
    }

    fun onAttractionClick(v:View)
    {
        getLocation()
        val result = searchLocations("tourist_attraction")
        addMarker(result)
    }
    fun onRestaurantClick(v:View)
    {
        getLocation()
        val result = searchLocations("restaurant")
        addMarker(result)
    }

    fun onHospitalClick(v:View)
    {
        getLocation()
        val result = searchLocations("hospital")
        addMarker(result)
    }

    fun onAtmClick(v:View)
    {
        getLocation()
        val result = searchLocations("atm")
        addMarker(result)
    }

    fun addMarker(places: MutableList<Place>)
    {
        mMap.clear()
        for (place in places)
        {
            mMap.addMarker(MarkerOptions().title(place.name).position(place.latlng))
        }
    }

}

data class Place(val name:String = "", val latlng:LatLng = LatLng(0.0, 0.0))


