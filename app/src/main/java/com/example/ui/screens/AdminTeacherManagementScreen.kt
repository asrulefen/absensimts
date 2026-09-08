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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Teacher
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldTertiary
import com.example.ui.theme.StatusHadir

@Composable
fun AdminTeacherManagementScreen(
    teachers: List<Teacher>,
    onAddTeacher: (Teacher) -> Unit,
    onUpdateTeacher: (Teacher) -> Unit,
    onDeleteTeacher: (String) -> Unit,
    onResetDevice: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingTeacher by remember { mutableStateOf<Teacher?>(null) }
    var teacherToDelete by remember { mutableStateOf<Teacher?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Kelola Akun Guru",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = EmeraldPrimary
                        )
                        Text(
                            text = "Admin Panel • Pembuatan Akun & Device Binding",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        color = EmeraldContainer,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "${teachers.size} Akun Terdaftar",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Explainer Card for Admin
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFFBF4)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF8CF8C6), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Kebijakan 1 Guru = 1 Akun Terdaftar",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = EmeraldPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Admin membuatkan akun satu per satu untuk setiap guru MTs. Guru hanya bisa melihat rekapitulasi individu masing-masing di ponselnya, sedangkan admin memiliki hak akses melihat dan mengunduh seluruh rekap kehadiran madrasah.",
                            fontSize = 11.sp,
                            color = Color(0xFF003825)
                        )
                    }
                }
            }

            items(teachers) { teacher ->
                TeacherCardItem(
                    teacher = teacher,
                    onEdit = { editingTeacher = teacher },
                    onDelete = { teacherToDelete = teacher },
                    onResetDevice = { onResetDevice(teacher.id) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(80.dp)) // Space for FAB
            }
        }

        // FAB to add new teacher
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = EmeraldPrimary,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_teacher_fab")
        ) {
            Icon(imageVector = Icons.Default.PersonAdd, contentDescription = "Tambah Guru")
        }
    }

    // Add Teacher Dialog
    if (showAddDialog) {
        TeacherFormDialog(
            title = "Buat Akun Guru Baru",
            initialTeacher = null,
            onDismiss = { showAddDialog = false },
            onSave = { newTeacher ->
                onAddTeacher(newTeacher)
                showAddDialog = false
            }
        )
    }

    // Edit Teacher Dialog
    editingTeacher?.let { teacher ->
        TeacherFormDialog(
            title = "Edit Akun Guru",
            initialTeacher = teacher,
            onDismiss = { editingTeacher = null },
            onSave = { updated ->
                onUpdateTeacher(updated)
                editingTeacher = null
            }
        )
    }

    // Delete confirmation dialog
    teacherToDelete?.let { teacher ->
        AlertDialog(
            onDismissRequest = { teacherToDelete = null },
            title = { Text("Konfirmasi Hapus Akun") },
            text = {
                Text("Apakah Anda yakin ingin menghapus akun ${teacher.name} (NIP: ${teacher.id})? Data yang sudah dihapus tidak dapat dikembalikan.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteTeacher(teacher.id)
                        teacherToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { teacherToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun TeacherCardItem(
    teacher: Teacher,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onResetDevice: () -> Unit
) {
    val isItemAdmin = teacher.isAdmin

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            .testTag("admin_teacher_card_${teacher.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (isItemAdmin) GoldTertiary else EmeraldPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = teacher.name.firstOrNull()?.toString() ?: "G",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = teacher.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "NIP/Username: ${teacher.id}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    color = if (isItemAdmin) Color(0xFFFEF3C7) else EmeraldContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (isItemAdmin) "ADMIN" else "GURU",
                        color = if (isItemAdmin) GoldTertiary else EmeraldPrimary,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "Mapel/Tugas: ${teacher.subject}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Password Akun: ${teacher.pin}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (teacher.email.isNotBlank()) {
                        Text(
                            text = "Email: ${teacher.email}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Device Binding Status
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PhoneAndroid,
                        contentDescription = "Device",
                        tint = if (teacher.registeredDeviceId != null) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (teacher.registeredDeviceId != null) "Terikat ke 1 Perangkat" else "Belum Terikat",
                        fontSize = 11.sp,
                        color = if (teacher.registeredDeviceId != null) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (teacher.registeredDeviceId != null) {
                        OutlinedButton(
                            onClick = onResetDevice,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Reset HP", fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Akun",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (!isItemAdmin) {
                        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Hapus Akun",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TeacherFormDialog(
    title: String,
    initialTeacher: Teacher?,
    onDismiss: () -> Unit,
    onSave: (Teacher) -> Unit
) {
    val focusManager = LocalFocusManager.current
    var id by remember { mutableStateOf(initialTeacher?.id ?: "") }
    var name by remember { mutableStateOf(initialTeacher?.name ?: "") }
    var subject by remember { mutableStateOf(initialTeacher?.subject ?: "") }
    var role by remember { mutableStateOf(initialTeacher?.role ?: "GURU") }
    var pin by remember { mutableStateOf(initialTeacher?.pin ?: "123456") }
    var email by remember { mutableStateOf(initialTeacher?.email ?: "") }
    var errorMessage by remember { mutableStateOf("") }

    val isEditing = initialTeacher != null

    AlertDialog(
        onDismissRequest = {
            focusManager.clearFocus(force = true)
            onDismiss()
        },
        title = {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    OutlinedTextField(
                        value = id,
                        onValueChange = { id = it },
                        label = { Text("NIP / Username Guru") },
                        enabled = !isEditing, // NIP immutable on edit
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nama Lengkap & Gelar") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("Mata Pelajaran / Tugas Tambahan") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { pin = it },
                        label = { Text("Password Akun (Default: 123456)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Sekolah (Opsional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Text("Peran Akun:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = role == "GURU",
                                onClick = { role = "GURU" }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Guru MTs", fontSize = 13.sp)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = role == "ADMIN",
                                onClick = { role = "ADMIN" }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Admin Madrasah", fontSize = 13.sp)
                        }
                    }
                }

                if (errorMessage.isNotBlank()) {
                    item {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    focusManager.clearFocus(force = true)
                    if (id.isBlank() || name.isBlank() || subject.isBlank() || pin.isBlank()) {
                        errorMessage = "Mohon lengkapi NIP, nama, mata pelajaran, dan password."
                        return@Button
                    }
                    val teacher = Teacher(
                        id = id.trim(),
                        name = name.trim(),
                        subject = subject.trim(),
                        role = role,
                        pin = pin.trim(),
                        email = email.trim()
                    )
                    onSave(teacher)
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("Simpan Akun")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                focusManager.clearFocus(force = true)
                onDismiss()
            }) {
                Text("Batal")
            }
        }
    )
}
