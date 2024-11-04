package com.example.testmaps

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.testmaps.databinding.ActivityMapsBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.data.geojson.GeoJsonLayer
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import android.util.SparseArray
import android.widget.CheckBox
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialog
import okhttp3.Call
import org.json.JSONObject
import java.io.IOException

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private var layers: HashMap<Int,GeoJsonLayer> = HashMap()
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

        val tornadoKey = loadGeoJsonFromUrl("https://services9.arcgis.com/RHVPKKiFTONKtxq3/arcgis/rest/services/NOAA_storm_reports_v1/FeatureServer/1/query?where=1%3D1&outFields=*&f=Geojson")
        val countiesCheckbox = findViewById<CheckBox>(R.id.countiesCheckbox)
        val countriesCheckBox = findViewById<CheckBox>(R.id.countriesCheckbox)
        val statesCheckbox = findViewById<CheckBox>(R.id.statesCheckbox)
        val tornadoesCheckbox = findViewById<CheckBox>(R.id.tornadoesCheckbox)
        val statesChecked = false;
        val countriesChecked = false;
        val countiesChecked = false;
        val tornadoesChecked = false;



        countiesCheckbox.setOnCheckedChangeListener{ buttonView, isChecked ->
            !countiesChecked
            if (isChecked){
                layers[R.raw.us_counties]?.addLayerToMap()
            }else{
                layers[R.raw.us_counties]?.removeLayerFromMap()
            }
        }

        countriesCheckBox.setOnCheckedChangeListener{ buttonView, isChecked ->
            !countriesChecked
            if (isChecked){
                layers[R.raw.countries]?.addLayerToMap()
            }else{
                layers[R.raw.countries]?.removeLayerFromMap()
            }
        }

        statesCheckbox.setOnCheckedChangeListener{ buttonView, isChecked ->
            !statesChecked
            if (isChecked){
                layers[R.raw.us_states]?.addLayerToMap()
            }else{
                layers[R.raw.us_states]?.removeLayerFromMap()
            }
        }

        tornadoesCheckbox.setOnCheckedChangeListener{ buttonView, isChecked ->
            !tornadoesChecked
            if (isChecked){
                layers[tornadoKey]?.addLayerToMap()
            }else{
                layers[tornadoKey]?.removeLayerFromMap()
            }
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

        val markers: MutableMap<Marker,LatLng> = mutableMapOf()
        var recentMarker: Marker? = null
        var markersVisible = true

        val statesData = loadGeoJsonFromResource(R.raw.us_states)
        val countriesData = loadGeoJsonFromResource(R.raw.countries)
        val countiesData = loadGeoJsonFromResource(R.raw.us_counties)



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
            //marker?.showInfoWindow()

            return marker
        }

        fun removeMarker(marker: Marker){
            marker.remove()
            markers.remove(marker)
            true
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


        mMap.setOnMarkerClickListener { marker ->
            displayMetadata(marker)
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

    private fun displayMetadata(marker : Marker) {
        val text = BottomSheetDialog(this)
        val sheetView = layoutInflater.inflate(R.layout.metadata_display, null)

        val markerTitle = sheetView.findViewById<TextView>(R.id.markerTitle)
        val markerDetails = sheetView.findViewById<TextView>(R.id.markerDetails)
        markerTitle.text = marker.title
        markerDetails.text = "hello just wanted to show you this is working"
        text.setContentView(sheetView)
        text.show()
    }


    private fun loadGeoJsonFromResource(resourceId: Int){
        try {
            val layer = GeoJsonLayer(mMap,resourceId,baseContext)
            layers.put(resourceId,layer)
            layer.setOnFeatureClickListener { feature ->
                val fid = feature.id
                val name = feature.properties.first().toString().substring(5)
                if(fid != null){
                    Toast.makeText(this,fid +": "+ name, Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception){
            e.printStackTrace()
        }

    }

    private fun loadGeoJsonFromUrl(url: String): Int {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .build()

        val jsonKey = System.currentTimeMillis().toInt()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val jsonData = response.body?.string()
                if (jsonData != null) {
                    //parsing for attributes for each indiv tornado
                    Log.d("testing",jsonData)
                    val layer = GeoJsonLayer(mMap,JSONObject(jsonData))
                    layers[jsonKey] = layer
                }
            }

            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                e.printStackTrace()
            }
        })
        return jsonKey
    }


}