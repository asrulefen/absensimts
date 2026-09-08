package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Attendance
import com.example.data.model.Teacher
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldTertiary
import com.example.ui.theme.StatusDiLuarRadius
import com.example.ui.theme.StatusHadir
import com.example.ui.theme.StatusIzin
import com.example.ui.theme.StatusSakit
import com.example.ui.theme.StatusTerlambat
import java.util.Locale

@Composable
fun RecapScreen(
    currentTeacher: Teacher,
    attendances: List<Attendance>,
    teachers: List<Teacher>,
    selectedMonth: String,
    onSelectMonth: (String) -> Unit,
    selectedTeacherFilter: String?,
    onSelectTeacherFilter: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var showExportDialog by remember { mutableStateOf(false) }

    // Role-based filtering:
    // If not admin, the teacher is strictly restricted to their OWN attendance records!
    val effectiveTeacherFilter = if (currentTeacher.isAdmin) {
        selectedTeacherFilter
    } else {
        currentTeacher.id
    }

    val filteredList = attendances.filter { item ->
        val matchMonth = if (selectedMonth == "ALL") true else item.date.startsWith(selectedMonth)
        val matchTeacher = if (effectiveTeacherFilter == null || effectiveTeacherFilter == "ALL") true else item.teacherId == effectiveTeacherFilter
        matchMonth && matchTeacher
    }

    // Statistics
    val totalCount = filteredList.size
    val tepatWaktuCount = filteredList.count { it.status == "HADIR" }
    val terlambatCount = filteredList.count { it.status == "TERLAMBAT" }
    val izinCount = filteredList.count { it.status == "IZIN" || it.status == "SAKIT" || it.status == "TUGAS_LUAR" }
    val disciplineRate = if (totalCount > 0) {
        ((tepatWaktuCount.toDouble() / totalCount.toDouble()) * 100).toInt()
    } else 100

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header & Export button
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (currentTeacher.isAdmin) "Rekapitulasi Madrasah" else "Rekapitulasi Saya",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = EmeraldPrimary
                    )
                    Text(
                        text = if (currentTeacher.isAdmin) "Hak Akses Admin: Seluruh Guru MTs" else "Laporan Presensi Individu: ${currentTeacher.name}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = { showExportDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    modifier = Modifier.testTag("export_recap_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Print,
                        contentDescription = "Cetak",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cetak", fontSize = 12.sp)
                }
            }
        }

        // Role badge notice
        item {
            if (!currentTeacher.isAdmin) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFFBF4)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Mode Guru: Menampilkan catatan kehadiran khusus NIP ${currentTeacher.id}. Rekap seluruh guru madrasah dikelola oleh Admin.",
                            fontSize = 11.sp,
                            color = Color(0xFF003825)
                        )
                    }
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = GoldTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Mode Admin Madrasah: Anda dapat memfilter dan mengecek rekap semua guru maupun per individu.",
                            fontSize = 11.sp,
                            color = Color(0xFF451A03),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Summary Stats Grid Cards
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ringkasan Statistik Kehadiran",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Surface(
                            color = EmeraldContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Disiplin: $disciplineRate%",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp,
                                color = EmeraldPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Total
                        StatBox(
                            title = "Total Absen",
                            count = "$totalCount",
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        // Tepat Waktu
                        StatBox(
                            title = "Tepat Waktu",
                            count = "$tepatWaktuCount",
                            color = StatusHadir,
                            modifier = Modifier.weight(1f)
                        )
                        // Terlambat
                        StatBox(
                            title = "Terlambat",
                            count = "$terlambatCount",
                            color = StatusTerlambat,
                            modifier = Modifier.weight(1f)
                        )
                        // Izin/Sakit
                        StatBox(
                            title = "Izin/Sakit",
                            count = "$izinCount",
                            color = StatusIzin,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Filter chips (Month)
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Filter Periode Bulan:",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val months = listOf(
                        "2026-09" to "September 2026",
                        "2026-08" to "Agustus 2026",
                        "2026-07" to "Juli 2026",
                        "ALL" to "Semua Riwayat"
                    )
                    items(months) { (key, label) ->
                        FilterChip(
                            selected = (selectedMonth == key),
                            onClick = { onSelectMonth(key) },
                            label = { Text(label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldContainer,
                                selectedLabelColor = EmeraldPrimary
                            )
                        )
                    }
                }
            }
        }

        // Filter chips (Teacher) - ONLY VISIBLE TO ADMIN!
        if (currentTeacher.isAdmin) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Filter Guru MTs (Khusus Admin):",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = (selectedTeacherFilter == "ALL" || selectedTeacherFilter == null),
                                onClick = { onSelectTeacherFilter("ALL") },
                                label = { Text("Semua Guru", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldContainer,
                                    selectedLabelColor = EmeraldPrimary
                                )
                            )
                        }
                        items(teachers) { teacher ->
                            FilterChip(
                                selected = (selectedTeacherFilter == teacher.id),
                                onClick = { onSelectTeacherFilter(teacher.id) },
                                label = { Text(teacher.name.split(" ").firstOrNull() ?: teacher.name, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldContainer,
                                    selectedLabelColor = EmeraldPrimary
                                )
                            )
                        }
                    }
                }
            }
        }

        // Attendance Record Cards List
        if (filteredList.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Kosong",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Belum Ada Catatan Presensi",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (currentTeacher.isAdmin) {
                                "Tidak ada data kehadiran untuk filter bulan dan guru yang dipilih."
                            } else {
                                "Anda belum memiliki riwayat presensi pada periode bulan ini."
                            },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredList) { record ->
                AttendanceItemCard(record = record)
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Printable/Exportable Recap Dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.School, contentDescription = null, tint = EmeraldPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Laporan Presensi Resmi MTs", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier.height(360.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("MTs AL-ASY'ARI PRUNGGAHAN KULON", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Semanding, Kabupaten Tuban, Jawa Timur", fontSize = 11.sp)
                                Text("Periode: $selectedMonth • Total: $totalCount Catatan", fontSize = 11.sp)
                                if (!currentTeacher.isAdmin) {
                                    Text("Nama Guru: ${currentTeacher.name} (NIP: ${currentTeacher.id})", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                } else {
                                    Text("Laporan Kolektif Seluruh Guru Madrasah", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Text("Tingkat Disiplin Tepat Waktu: $disciplineRate%", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = EmeraldPrimary)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    items(filteredList) { item ->
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(item.date, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text(
                                        item.status,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (item.status == "HADIR") StatusHadir else StatusTerlambat
                                    )
                                }
                                Text("${item.teacherName} (NIP: ${item.teacherId})", fontSize = 11.sp)
                                Text("Masuk: ${item.checkInTime} • Pulang: ${item.checkOutTime ?: '-'}", fontSize = 10.sp)
                                Text("Verifikasi GPS: ${item.distanceMeters}m dari titik madrasah", fontSize = 10.sp, color = EmeraldPrimary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showExportDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Tutup & Selesai")
                }
            }
        )
    }
}

@Composable
fun StatBox(
    title: String,
    count: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = color
            )
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun AttendanceItemCard(record: Attendance) {
    val statusColor = when (record.status) {
        "HADIR" -> StatusHadir
        "TERLAMBAT" -> StatusTerlambat
        "IZIN" -> StatusIzin
        "SAKIT" -> StatusSakit
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
            .testTag("attendance_item_${record.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top Row: Date, Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = record.date,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Surface(
                    color = statusColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = record.status,
                        color = statusColor,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = record.teacherName,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
            Text(
                text = "NIP. ${record.teacherId}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Timestamps: Masuk & Pulang
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    Text(text = "Presensi Masuk", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "${record.checkInTime} WIB", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Column {
                    Text(text = "Presensi Pulang", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = if (record.checkOutTime != null) "${record.checkOutTime} WIB" else "Belum Pulang",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (record.checkOutTime != null) MaterialTheme.colorScheme.onSurface else StatusTerlambat
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // GPS Geofence details badge
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "GPS",
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "GPS Terverifikasi: Jarak ${String.format(Locale.US, "%.1f", record.distanceMeters)}m (${String.format(Locale.US, "%.4f", record.latitude)}, ${String.format(Locale.US, "%.4f", record.longitude)})",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (record.subjectNote.isNotBlank() || record.remarks.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Catatan: ${record.subjectNote} • ${record.remarks}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
