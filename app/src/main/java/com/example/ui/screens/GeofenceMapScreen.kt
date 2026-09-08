package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SchoolGeofence
import com.example.location.LocationState
import com.example.ui.GeofenceStatus
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.StatusDiLuarRadius
import com.example.ui.theme.StatusHadir
import java.util.Locale

@Composable
fun GeofenceMapScreen(
    schoolGeofence: SchoolGeofence,
    locationState: LocationState,
    geofenceStatus: GeofenceStatus,
    onCalibrateToCurrentLocation: () -> Unit,
    onUpdateRadius: (Float) -> Unit,
    onRestoreDefaultLocation: () -> Unit,
    onRefreshGps: () -> Unit,
    modifier: Modifier = Modifier
) {
    var radiusSliderValue by remember(schoolGeofence.radiusMeters) {
        mutableFloatStateOf(schoolGeofence.radiusMeters)
    }

    val isWithin = geofenceStatus.isWithinRadius
    val distanceStr = if (geofenceStatus.distanceMeters >= 1000) {
        "${String.format(Locale.US, "%.2f", geofenceStatus.distanceMeters / 1000f)} km"
    } else {
        "${geofenceStatus.distanceMeters.toInt()} meter"
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Column {
                Text(
                    text = "Pengaturan Geofencing GPS",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = EmeraldPrimary
                )
                Text(
                    text = "Konfigurasi Titik Lokasi Madrasah & Radius Presensi Akurat",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Radar Distance Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isWithin) Color(0xFFEFFBF4) else Color(0xFFFEF2F2)
                ),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, if (isWithin) StatusHadir else StatusDiLuarRadius, RoundedCornerShape(18.dp))
                    .testTag("geofence_radar_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isWithin) StatusHadir else StatusDiLuarRadius),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isWithin) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isWithin) "DI DALAM RADIUS MADRASAH" else "DI LUAR RADIUS MADRASAH",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    color = if (isWithin) StatusHadir else StatusDiLuarRadius
                                )
                                Text(
                                    text = if (isWithin) "Absensi Diperbolehkan" else "Absensi Dikunci Otomatis",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = onRefreshGps,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Refresh", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Distance progress illustration
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Jarak Guru ke Pusat Madrasah:", fontSize = 12.sp)
                            Text(distanceStr, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = if (isWithin) StatusHadir else StatusDiLuarRadius)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Batas Toleransi Geofencing:", fontSize = 12.sp)
                            Text("${schoolGeofence.radiusMeters.toInt()} meter", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Current GPS Coordinates info
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.GpsFixed, contentDescription = null, tint = EmeraldPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Sinyal GPS Ponsel Anda", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Latitude: ${String.format(Locale.US, "%.6f", locationState.latitude)}",
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Longitude: ${String.format(Locale.US, "%.6f", locationState.longitude)}",
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Akurasi Sinyal: ±${String.format(Locale.US, "%.1f", locationState.accuracy)} meter",
                        fontSize = 12.sp,
                        color = if (locationState.accuracy <= 25f) StatusHadir else StatusDiLuarRadius
                    )
                }
            }
        }

        // Madrasah Target Coordinates Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.School, contentDescription = null, tint = EmeraldPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Titik Koordinat Madrasah (Geofence)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = schoolGeofence.schoolName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text(text = schoolGeofence.schoolAddress, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Target Lat: ${String.format(Locale.US, "%.6f", schoolGeofence.latitude)}",
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Target Lng: ${String.format(Locale.US, "%.6f", schoolGeofence.longitude)}",
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Radius Slider
                    Text(
                        text = "Atur Radius Geofence: ${radiusSliderValue.toInt()} meter",
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    )
                    Slider(
                        value = radiusSliderValue,
                        onValueChange = { radiusSliderValue = it },
                        onValueChangeFinished = {
                            onUpdateRadius(radiusSliderValue)
                        },
                        valueRange = 30f..300f,
                        steps = 26,
                        colors = SliderDefaults.colors(
                            thumbColor = EmeraldPrimary,
                            activeTrackColor = EmeraldPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Calibration Action Buttons
                    Button(
                        onClick = onCalibrateToCurrentLocation,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("apply_current_gps_button")
                    ) {
                        Icon(imageVector = Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Jadikan GPS Saya Sebagai Titik Madrasah", fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = onRestoreDefaultLocation,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("restore_default_geofence_button")
                    ) {
                        Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reset ke Titik Asli MTs Al-Asy'ari", fontSize = 12.sp)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
