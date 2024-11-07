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
    private val layers = hashMapOf<Int, GeoJsonLayer>()

    private val preferences = "MyPrefs"
    private val keycountieschecked = "countiesChecked"
    private val keycountrieschecked = "countriesChecked"
    private val keystateschecked = "statesChecked"
    private val keytornadoeschecked = "tornadoesChecked"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        initUiComponents()
    }

    private fun initUiComponents() {
        val dropdownButton = findViewById<ImageButton>(R.id.showCheckboxButton)
        val background = findViewById<View>(R.id.greyed_background_remove)

        dropdownButton.setOnClickListener {
            background.visibility = View.VISIBLE
        }

        val exitmenubutton = findViewById<ImageButton>(R.id.exitMenuButton)
        exitmenubutton.setOnClickListener {
            background.visibility = View.GONE
        }

        val countiesCheckbox = findViewById<CheckBox>(R.id.countiesCheckbox)
        val countriesCheckBox = findViewById<CheckBox>(R.id.countriesCheckbox)
        val statesCheckbox = findViewById<CheckBox>(R.id.statesCheckbox)
        val tornadoesCheckbox = findViewById<CheckBox>(R.id.tornadoesCheckbox)

        val sharedPreferences = getSharedPreferences(preferences, MODE_PRIVATE)
        countiesCheckbox.isChecked = sharedPreferences.getBoolean(keycountieschecked, false)
        countriesCheckBox.isChecked = sharedPreferences.getBoolean(keycountrieschecked, false)
        statesCheckbox.isChecked = sharedPreferences.getBoolean(keystateschecked, false)
        tornadoesCheckbox.isChecked = sharedPreferences.getBoolean(keytornadoeschecked, false)

        setupCheckBoxListeners()
    }

    private fun setupCheckBoxListeners() {
        val countiesCheckbox = findViewById<CheckBox>(R.id.countiesCheckbox)
        val countriesCheckBox = findViewById<CheckBox>(R.id.countriesCheckbox)
        val statesCheckbox = findViewById<CheckBox>(R.id.statesCheckbox)
        val tornadoesCheckbox = findViewById<CheckBox>(R.id.tornadoesCheckbox)

        countiesCheckbox.setOnCheckedChangeListener { _, isChecked ->
            toggleLayer(R.raw.us_counties, isChecked)
        }

        countriesCheckBox.setOnCheckedChangeListener { _, isChecked ->
            toggleLayer(R.raw.countries, isChecked)
        }

        statesCheckbox.setOnCheckedChangeListener { _, isChecked ->
            toggleLayer(R.raw.us_states, isChecked)
        }

        tornadoesCheckbox.setOnCheckedChangeListener { _, isChecked ->
            toggleLayer(loadGeoJsonFromUrl(
                "https://services9.arcgis.com/RHVPKKiFTONKtxq3/arcgis/rest/services/NOAA_storm_reports_v1/FeatureServer/1/query?where=1%3D1&outFields=*&f=Geojson",
                tornadoesCheckbox
            ), isChecked)
        }
    }

    private fun toggleLayer(layerId: Int, addLayer: Boolean) {
        if (addLayer) {
            layers[layerId]?.addLayerToMap()
        } else {
            layers[layerId]?.removeLayerFromMap()
        }
        saveCheckboxStates()
    }

    override fun onPause() {
        super.onPause()
        saveCheckboxStates()
    }

    private fun saveCheckboxStates() {
        val prefs = getSharedPreferences(preferences, MODE_PRIVATE)
        val editor = prefs.edit()
        val countiesCheckbox = findViewById<CheckBox>(R.id.countiesCheckbox)
        val countriesCheckBox = findViewById<CheckBox>(R.id.countriesCheckbox)
        val statesCheckbox = findViewById<CheckBox>(R.id.statesCheckbox)
        val tornadoesCheckbox = findViewById<CheckBox>(R.id.tornadoesCheckbox)

        editor.putBoolean(keycountieschecked, countiesCheckbox.isChecked)
        editor.putBoolean(keycountrieschecked, countriesCheckBox.isChecked)
        editor.putBoolean(keystateschecked, statesCheckbox.isChecked)
        editor.putBoolean(keytornadoeschecked, tornadoesCheckbox.isChecked)
        editor.apply()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true

        if (findViewById<CheckBox>(R.id.countiesCheckbox).isChecked) {
            loadGeoJsonFromResource(R.raw.us_counties)
        }
        if (findViewById<CheckBox>(R.id.countriesCheckbox).isChecked) {
            loadGeoJsonFromResource(R.raw.countries)
        }
        if (findViewById<CheckBox>(R.id.statesCheckbox).isChecked) {
            loadGeoJsonFromResource(R.raw.us_states)
        }
        if (findViewById<CheckBox>(R.id.tornadoesCheckbox).isChecked) {
            loadGeoJsonFromUrl(
                "https://services9.arcgis.com/RHVPKKiFTONKtxq3/arcgis/rest/services/NOAA_storm_reports_v1/FeatureServer/1/query?where=1%3D1&outFields=*&f=Geojson",
                findViewById<CheckBox>(R.id.tornadoesCheckbox)
            )
        }

        setupMapInteractions()
    }

    private fun setupMapInteractions() {
        val markers: MutableMap<Marker, LatLng> = mutableMapOf()
        var recentMarker: Marker? = null

        mMap.setOnMapClickListener { latLng ->
            recentMarker?.isVisible = false
            recentMarker = addMarker(latLng, markers)
        }

        mMap.setOnMarkerClickListener { marker ->
            displayMetadata(marker)
            true
        }

        val showAllMarkers: Button = findViewById(R.id.showAllMarkers)
        var markersVisible = true
        showAllMarkers.setOnClickListener {
            markersVisible = toggleMarkersVisibility(markers, markersVisible)
            showAllMarkers.text = if (markersVisible) "Hide Markers" else "Show Markers"
        }
    }

    private fun addMarker(latLng: LatLng, markers: MutableMap<Marker, LatLng>): Marker? {
        val marker = mMap.addMarker(MarkerOptions().position(latLng).title("Latitude: ${latLng.latitude}, Longitude: ${latLng.longitude}"))
        if (marker != null) {
            markers[marker] = latLng
        }
        return marker
    }

    private fun toggleMarkersVisibility(markers: Map<Marker, LatLng>, markersVisible: Boolean): Boolean {
        for (marker in markers.keys) {
            marker.isVisible = !markersVisible
        }
        return !markersVisible
    }

    private fun displayMetadata(marker: Marker) {
        val text = BottomSheetDialog(this)
        val sheetView = layoutInflater.inflate(R.layout.metadata_display, null)
        sheetView.findViewById<TextView>(R.id.markerTitle).text = marker.title
        sheetView.findViewById<TextView>(R.id.markerDetails).text = "hello just wanted to show you this is working"
        text.setContentView(sheetView)
        text.show()
    }

    private fun loadGeoJsonFromResource(resourceId: Int) {
        try {
            val layer = GeoJsonLayer(mMap, resourceId, baseContext)
            layers[resourceId] = layer
            if (isCheckboxCheckedForResource(resourceId)) {
                layer.addLayerToMap()
            }
            setupLayerClickListener(layer)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun isCheckboxCheckedForResource(resourceId: Int): Boolean {
        return when (resourceId) {
            R.raw.us_counties -> findViewById<CheckBox>(R.id.countiesCheckbox).isChecked
            R.raw.countries -> findViewById<CheckBox>(R.id.countriesCheckbox).isChecked
            R.raw.us_states -> findViewById<CheckBox>(R.id.statesCheckbox).isChecked
            else -> false
        }
    }

    private fun setupLayerClickListener(layer: GeoJsonLayer) {
        layer.setOnFeatureClickListener { feature ->
            Toast.makeText(this, "${feature.id}: ${feature.properties.first()}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadGeoJsonFromUrl(url: String, checkbox: CheckBox): Int {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()
        val jsonKey = System.currentTimeMillis().toInt()
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onResponse(call: Call, response: Response) {
                val jsonData = response.body?.string()
                if (jsonData != null) {
                    val layer = GeoJsonLayer(mMap, JSONObject(jsonData))
                    layers[jsonKey] = layer
                    runOnUiThread {
                        if (checkbox.isChecked) {
                            layer.addLayerToMap()
                        }
                        setupLayerClickListener(layer)
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }
        })
        return jsonKey
    }
}