package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Attendance
import com.example.data.model.Teacher
import com.example.ui.GeofenceStatus
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldTertiary
import com.example.ui.theme.StatusDiLuarRadius
import com.example.ui.theme.StatusHadir
import com.example.ui.theme.StatusTerlambat
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AttendanceScreen(
    teacher: Teacher,
    todayAttendance: Attendance?,
    geofenceStatus: GeofenceStatus,
    sessionRemainingSeconds: Int,
    firebaseSyncStatus: String = "🟢 Firebase Realtime Online",
    onCheckIn: (subjectNote: String) -> Unit,
    onCheckOut: () -> Unit,
    onSubmitPermit: (statusType: String, reason: String) -> Unit,
    onRefreshLocation: () -> Unit,
    onCalibrateLocation: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    var currentTimeString by remember { mutableStateOf("") }
    var currentDateString by remember { mutableStateOf("") }
    var subjectNoteInput by remember { mutableStateOf(teacher.subject) }
    var showPermitDialog by remember { mutableStateOf(false) }

    // Live clock ticker
    LaunchedEffect(Unit) {
        while (true) {
            val now = Date()
            currentTimeString = SimpleDateFormat("HH:mm:ss", Locale("id", "ID")).format(now) + " WIB"
            currentDateString = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID")).format(now)
            delay(1000L)
        }
    }

    // Pulse animation for GPS radar status
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Teacher Profile & Session Header
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(EmeraldPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = teacher.name.firstOrNull()?.toString() ?: "G",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = teacher.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "NIP. ${teacher.id}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${teacher.subject} • ${teacher.role}",
                            fontSize = 11.sp,
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    IconButton(
                        onClick = onLogout,
                        modifier = Modifier.testTag("logout_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // Live Clock & Auto-Logout Security Bar
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = currentDateString,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = currentTimeString,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = EmeraldPrimary
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        // Persistent Account Status badge
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = EmeraldContainer,
                            modifier = Modifier.testTag("saved_account_badge")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Akun Tersimpan",
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Akun Tersimpan",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Realtime Cloud Sync Badge
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.border(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(20.dp)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(Color(0xFF2E7D32), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = firebaseSyncStatus,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Geofencing GPS Radar & Location Verification Card
        item {
            val isWithin = geofenceStatus.isWithinRadius
            val distanceStr = if (geofenceStatus.distanceMeters >= 1000) {
                "${String.format(Locale.US, "%.2f", geofenceStatus.distanceMeters / 1000f)} km"
            } else {
                "${geofenceStatus.distanceMeters.toInt()} meter"
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isWithin) Color(0xFFEFFBF4) else Color(0xFFFEF2F2)
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        2.dp,
                        if (isWithin) StatusHadir else StatusDiLuarRadius,
                        RoundedCornerShape(20.dp)
                    )
                    .testTag("geofence_status_card")
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
                                    .background(if (isWithin) StatusHadir else StatusDiLuarRadius)
                                    .scale(if (!isWithin) 1.0f else pulseScale),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isWithin) Icons.Default.CheckCircle else Icons.Default.Error,
                                    contentDescription = "Geofence Status",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isWithin) "DALAM RADIUS GEOFENCE" else "DI LUAR RADIUS MADRASAH",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    color = if (isWithin) StatusHadir else StatusDiLuarRadius
                                )
                                Text(
                                    text = if (isWithin) "Lokasi Sah • Presensi Terbuka" else "Absensi Terkunci Oleh Geofencing",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = onRefreshLocation,
                            modifier = Modifier.testTag("refresh_gps_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh GPS",
                                tint = EmeraldPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Madrasah and Device GPS Coordinates
                    Surface(
                        color = Color.White.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = "Madrasah",
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = geofenceStatus.school.schoolName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            Text(
                                text = "Titik Pusat: ${String.format(Locale.US, "%.5f", geofenceStatus.school.latitude)}, ${String.format(Locale.US, "%.5f", geofenceStatus.school.longitude)} (Radius Maks: ${geofenceStatus.school.radiusMeters.toInt()}m)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.GpsFixed,
                                    contentDescription = "GPS Terkini",
                                    tint = if (isWithin) StatusHadir else StatusDiLuarRadius,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Koordinat GPS Guru: ${String.format(Locale.US, "%.5f", geofenceStatus.currentLat)}, ${String.format(Locale.US, "%.5f", geofenceStatus.currentLng)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = "Jarak Ke Titik Madrasah: $distanceStr • Akurasi GPS: ±${String.format(Locale.US, "%.1f", geofenceStatus.accuracy)}m",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isWithin) StatusHadir else StatusDiLuarRadius
                            )
                        }
                    }

                    // Test calibration button
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = onCalibrateLocation,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("calibrate_gps_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Kalibrasi",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Kalibrasi GPS Saya Sebagai Lokasi Madrasah",
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // Attendance Action Area
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Form Presensi Harian",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (todayAttendance == null) {
                        // Not yet checked in
                        OutlinedTextField(
                            value = subjectNoteInput,
                            onValueChange = { subjectNoteInput = it },
                            label = { Text("Mata Pelajaran & Catatan Mengajar") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("subject_note_input"),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Check-in Button
                        val canCheckIn = geofenceStatus.isWithinRadius
                        Button(
                            onClick = {
                                focusManager.clearFocus(force = true)
                                onCheckIn(subjectNoteInput)
                            },
                            enabled = canCheckIn,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("check_in_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldPrimary,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Icon(
                                imageVector = if (canCheckIn) Icons.Default.LockOpen else Icons.Default.Lock,
                                contentDescription = "Absen Masuk",
                                tint = if (canCheckIn) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (canCheckIn) "PRESENSI MASUK SEKARANG" else "ABSENSI TERKUNCI (DI LUAR RADIUS)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (canCheckIn) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (!canCheckIn) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "⚠️ Anda harus berada di area madrasah untuk melakukan presensi. Atau klik tombol 'Kalibrasi GPS' di atas untuk uji coba lokasi.",
                                fontSize = 11.sp,
                                color = StatusDiLuarRadius,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Special Permit Option (Izin / Sakit / Tugas)
                        OutlinedButton(
                            onClick = { showPermitDialog = true },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("permit_button")
                        ) {
                            Text("Ajukan Izin / Sakit / Tugas Kedinasan", fontSize = 13.sp)
                        }

                    } else if (todayAttendance.checkOutTime == null && todayAttendance.status != "IZIN" && todayAttendance.status != "SAKIT") {
                        // Checked in, waiting for Check-out
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = EmeraldContainer.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Presensi Masuk:",
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "${todayAttendance.checkInTime} WIB",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        color = EmeraldPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "Status Kehadiran:", fontSize = 12.sp)
                                    Text(
                                        text = todayAttendance.status,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (todayAttendance.status == "HADIR") StatusHadir else StatusTerlambat
                                    )
                                }
                                Text(
                                    text = "Catatan: ${todayAttendance.subjectNote}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        val canCheckOut = geofenceStatus.isWithinRadius
                        Button(
                            onClick = onCheckOut,
                            enabled = canCheckOut,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("check_out_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFC2410C),
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Absen Pulang",
                                tint = if (canCheckOut) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (canCheckOut) "PRESENSI PULANG" else "ABSEN PULANG TERKUNCI (DI LUAR RADIUS)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (canCheckOut) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                    } else {
                        // Completed today
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = EmeraldContainer.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selesai",
                                    tint = StatusHadir,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Presensi Hari Ini Telah Lengkap",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Masuk: ${todayAttendance.checkInTime} • Pulang: ${todayAttendance.checkOutTime ?: '-'}",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Jazakumullah Khairan atas dedikasi mengajar di Madrasah!",
                                    fontSize = 12.sp,
                                    color = EmeraldPrimary,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Permit Submission Dialog (Izin/Sakit/Tugas)
    if (showPermitDialog) {
        var permitType by remember { mutableStateOf("IZIN") }
        var permitReason by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showPermitDialog = false },
            title = {
                Text(text = "Pengajuan Keterangan Absensi", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Pilih Jenis Permohonan:", fontSize = 13.sp)
                    listOf(
                        "IZIN" to "Izin Keperluan Mendesak",
                        "SAKIT" to "Sakit (Surat Dokter)",
                        "TUGAS_LUAR" to "Tugas Kedinasan / Kemenag"
                    ).forEach { (type, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { permitType = type }
                        ) {
                            RadioButton(
                                selected = (permitType == type),
                                onClick = { permitType = type }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = label, fontSize = 13.sp)
                        }
                    }

                    OutlinedTextField(
                        value = permitReason,
                        onValueChange = { permitReason = it },
                        label = { Text("Keterangan / Nomor Surat") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        focusManager.clearFocus(force = true)
                        if (permitReason.isNotBlank()) {
                            onSubmitPermit(permitType, permitReason)
                            showPermitDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Kirimkan")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    focusManager.clearFocus(force = true)
                    showPermitDialog = false
                }) {
                    Text("Batal")
                }
            }
        )
    }
}
