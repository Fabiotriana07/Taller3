package com.example.taller3

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlin.collections.remove

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var distanceText: TextView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var userLatLng: LatLng? = null
    private var currentLatLng: LatLng? = null

    private var firebaseListener: ListenerRegistration? = null

    private val LOCATION_PERMISSION_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        distanceText = findViewById(R.id.distanceText)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Obtener el ID del usuario desde el Intent
        val userId = intent.getStringExtra("userId") ?: return

        // Escuchar cambios en Firebase usando el ID recibido
        listenToFirebaseUpdates(userId)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        checkLocationPermission()
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            onLocationPermissionGranted()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onLocationPermissionGranted()
            } else {
                onLocationPermissionDenied()
            }
        }
    }

    private fun onLocationPermissionGranted() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                mMap.isMyLocationEnabled = true
                startLocationUpdates()
            } catch (e: SecurityException) {
                e.printStackTrace()
                distanceText.text = "Error al habilitar la ubicación"
            }
        } else {
            distanceText.text = "Permiso de ubicación no concedido"
        }
    }

    private fun onLocationPermissionDenied() {
        distanceText.text = "Permiso de ubicación denegado"
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 5000L
        ).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                val location = locationResult.lastLocation
                if (location != null) {
                    val latitude = location.latitude
                    val longitude = location.longitude

                    // Validar que las coordenadas no sean (0.0, 0.0)
                    if (latitude != 0.0 && longitude != 0.0) {
                        currentLatLng = LatLng(latitude, longitude)
                        updateMap()
                    } else {
                        distanceText.text = "Coordenadas inválidas del dispositivo"
                    }
                }
            }
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, mainLooper)
        }
    }

    private fun listenToFirebaseUpdates(userId: String) {
        val db = Firebase.firestore

        // Eliminar cualquier listener previo
        firebaseListener?.remove()

        // Registrar un nuevo listener
        firebaseListener = db.collection("users").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("MapsActivity", "Error al escuchar cambios en Firebase", error)
                    distanceText.text = "Error al obtener datos de Firebase"
                    return@addSnapshotListener
                }
                Log.d("MapsActivity", "Escuchando cambios en Firebase para el usuario: $userId")

                if (snapshot != null && snapshot.exists()) {
                    val latitude = snapshot.getDouble("latitude")
                    val longitude = snapshot.getDouble("longitude")

                    if (latitude != null && longitude != null) {
                        userLatLng = LatLng(latitude, longitude)
                        updateMap()
                        Log.d("MapsActivity", "Coordenadas actualizadas desde Firebase: Lat=$latitude, Lng=$longitude")
                    } else {
                        Log.w("MapsActivity", "Coordenadas no disponibles en Firebase")
                        distanceText.text = "Coordenadas no disponibles en Firebase"
                    }
                } else {
                    Log.w("MapsActivity", "El documento no existe o está vacío")
                    distanceText.text = "Datos no disponibles"
                }
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        firebaseListener?.remove()
    }

    private fun updateMap() {
        if (currentLatLng != null && userLatLng != null) {
            mMap.clear()

            // Agregar marcador para la ubicación actual
            mMap.addMarker(MarkerOptions().position(currentLatLng!!).title("Mi ubicación"))

            // Agregar marcador para la ubicación del usuario remoto
            mMap.addMarker(MarkerOptions().position(userLatLng!!).title("Usuario remoto"))

            // Dibujar línea entre las ubicaciones
            mMap.addPolyline(
                PolylineOptions()
                    .add(currentLatLng, userLatLng)
                    .width(5f)
                    .color(ContextCompat.getColor(this, android.R.color.holo_blue_dark))
            )

            // Calcular y mostrar la distancia
            val distance = calculateDistance(currentLatLng!!, userLatLng!!)
            distanceText.text = "Distancia: ${"%.2f".format(distance)} km"

            // Mover la cámara para mostrar ambas ubicaciones
            val bounds = LatLngBounds.Builder()
                .include(currentLatLng!!)
                .include(userLatLng!!)
                .build()
            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
        }
    }

    private fun calculateDistance(start: LatLng, end: LatLng): Float {
        val results = FloatArray(1)
        Location.distanceBetween(start.latitude, start.longitude, end.latitude, end.longitude, results)
        return results[0] / 1000 // Convertir a kilómetros
    }

}