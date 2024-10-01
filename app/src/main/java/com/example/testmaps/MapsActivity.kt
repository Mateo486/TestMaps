package com.example.testmaps

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Spinner

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.example.testmaps.databinding.ActivityMapsBinding
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener
import com.google.android.gms.maps.UiSettings
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.StrokeStyle
import com.google.android.gms.maps.model.StyleSpan

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val dropdownButton = findViewById<ImageButton>(R.id.showCheckboxButton)
        val background = findViewById<View>(R.id.greyed_background_remove)

        dropdownButton.setOnClickListener {
            background.visibility = View.VISIBLE
        }

        val exitmenubutton = findViewById<ImageButton>(R.id.exitMenuButton)

        exitmenubutton.setOnClickListener {
            background.visibility = View.GONE
        }




    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true

        val gainesville = LatLng(29.6520, -82.3520)
        mMap.addMarker(MarkerOptions().position(gainesville).title("Marker in Gainesville"))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(gainesville))



        val gainesvillePoly = PolylineOptions()
            .add(LatLng(29.5520,-82.4520))
            .add (LatLng(29.7520,-82.4520))
            .add (LatLng(29.7520,-82.2520))
            .add(LatLng(29.5520,-82.2520))
            .add(LatLng(29.5520,-82.4520))
        mMap.addPolyline(gainesvillePoly)

        mMap.addPolyline(
            PolylineOptions()
                .add(LatLng(29.5520,-82.4520), LatLng(29.7520,-82.4520))
                .addSpan(StyleSpan(Color.RED))
        )

        mMap.addPolyline(
            PolylineOptions()
                .add (LatLng(29.7520,-82.4520),LatLng(29.7520,-82.2520))
                .addSpan(
                    StyleSpan(
                        StrokeStyle.gradientBuilder(
                            Color.RED,
                            Color.YELLOW
                        ).build()
                    )
                )
        )
        val markers: MutableMap<Marker,LatLng> = mutableMapOf()
        var recentMarker: Marker? = null
        var markersVisible = true

        fun addMarker(latLng: LatLng): Marker? {
            recentMarker?.isVisible = false
            val latitude = latLng.latitude
            val longitude = latLng.longitude
            val markerOptions = MarkerOptions()
                .position(latLng)
                .title("Latitude: $latitude, Longitude: $longitude" ?: "Marker")

            val marker = googleMap.addMarker(markerOptions)

            if(marker != null){
                markers[marker] = latLng
                recentMarker = marker
            }
            marker?.showInfoWindow()

            return marker
        }

        fun removeMarker(marker: Marker){
            marker.remove()
            markers.remove(marker)
        }

        fun clearMarkers(){
            for (key in markers.keys) {
                key.remove()
            }
            markers.clear()
        }


        mMap.setOnMapClickListener { latLng ->
            addMarker(latLng)
        }

        mMap.setOnMarkerClickListener{ marker ->
            removeMarker(marker)
            true
        }

        fun toggleMarkers() {
            if (markersVisible) {
                for (marker in markers.keys) {
                    marker.isVisible = false
                }
                markersVisible = false
            } else {
                for (marker in markers.keys){
                    marker.isVisible = true
                }
                markersVisible = true
            }
        }

        val showAllMarkers: Button = findViewById(R.id.showAllMarkers)
        showAllMarkers.setOnClickListener{
            toggleMarkers()
            showAllMarkers.text = if (markersVisible) "Hide Markers" else "Show Markers"
        }

    }
}