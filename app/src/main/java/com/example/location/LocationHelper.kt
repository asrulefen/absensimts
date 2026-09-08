package com.example.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class LocationState(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val accuracy: Float = 0.0f,
    val isGpsEnabled: Boolean = true,
    val isLocating: Boolean = false,
    val hasAccurateFix: Boolean = false,
    val lastUpdateTime: Long = 0L,
    val errorMessage: String? = null
)

class LocationHelper(private val context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val _locationState = MutableStateFlow(LocationState())
    val locationState: StateFlow<LocationState> = _locationState.asStateFlow()

    private var locationCallback: LocationCallback? = null

    private fun checkGpsEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        return locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
                locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates() {
        val isGpsOn = checkGpsEnabled()
        if (!isGpsOn) {
            _locationState.update {
                it.copy(
                    isGpsEnabled = false,
                    errorMessage = "GPS / Layanan Lokasi belum diaktifkan pada perangkat."
                )
            }
            return
        }

        _locationState.update { it.copy(isLocating = true, errorMessage = null, isGpsEnabled = true) }

        // Fetch last known location first for immediate feedback
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc: Location? ->
                loc?.let { updateWithLocation(it) }
            }
        } catch (_: Exception) {}

        // Request high accuracy continuous updates
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
            .setMinUpdateIntervalMillis(1500L)
            .setMinUpdateDistanceMeters(1.0f)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    updateWithLocation(location)
                }
            }
        }

        try {
            locationCallback?.let { callback ->
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    callback,
                    Looper.getMainLooper()
                )
            }
        } catch (e: Exception) {
            _locationState.update {
                it.copy(isLocating = false, errorMessage = "Gagal memulai GPS: ${e.localizedMessage}")
            }
        }
    }

    private fun updateWithLocation(location: Location) {
        _locationState.update {
            it.copy(
                latitude = location.latitude,
                longitude = location.longitude,
                accuracy = location.accuracy,
                hasAccurateFix = location.accuracy <= 30.0f,
                isLocating = false,
                isGpsEnabled = true,
                lastUpdateTime = System.currentTimeMillis(),
                errorMessage = null
            )
        }
    }

    fun stopLocationUpdates() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        locationCallback = null
        _locationState.update { it.copy(isLocating = false) }
    }

    /**
     * For manual simulation / testing adjustment if in emulator or developer mode
     */
    fun setManualCoordinates(lat: Double, lng: Double, accuracy: Float = 5.0f) {
        _locationState.update {
            it.copy(
                latitude = lat,
                longitude = lng,
                accuracy = accuracy,
                hasAccurateFix = true,
                isLocating = false,
                isGpsEnabled = true,
                lastUpdateTime = System.currentTimeMillis(),
                errorMessage = null
            )
        }
    }
}
