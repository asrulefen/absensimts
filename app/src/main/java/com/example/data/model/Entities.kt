package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class UserRole {
    GURU,
    ADMIN
}

@Entity(tableName = "teachers")
data class Teacher(
    @PrimaryKey val id: String, // NIP, NUPTK, or Username, e.g. "admin", "198503152010011012"
    val name: String,
    val subject: String,
    val role: String = "GURU", // "GURU" or "ADMIN"
    val pin: String = "123456",
    val email: String = "",
    val photoUrl: String = "",
    val registeredDeviceId: String = "", // Bound device ID (1 akun = 1 device)
    val deviceModel: String = ""
) {
    val isAdmin: Boolean
        get() = role.equals("ADMIN", ignoreCase = true)
}

@Entity(tableName = "attendances")
data class Attendance(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val teacherId: String,
    val teacherName: String,
    val date: String, // "YYYY-MM-DD"
    val checkInTime: String, // "HH:mm:ss"
    val checkOutTime: String? = null,
    val status: String, // "HADIR", "TERLAMBAT", "IZIN", "SAKIT", "TUGAS_LUAR", "AUTO_CHECKOUT"
    val latitude: Double,
    val longitude: Double,
    val distanceMeters: Float,
    val isWithinGeofence: Boolean,
    val subjectNote: String = "",
    val remarks: String = "",
    val photoUri: String = ""
)

@Entity(tableName = "school_geofence")
data class SchoolGeofence(
    @PrimaryKey val id: Int = 1,
    val schoolName: String = "MTs Al-Asy'ari Prunggahan Kulon",
    val schoolAddress: String = "Prunggahan Kulon, Kec. Semanding, Kab. Tuban, Jawa Timur",
    val websiteUrl: String = "https://mtsalasyari.my.id/login",
    val latitude: Double = -6.914100, // Tuban Semanding / Prunggahan Kulon coordinates
    val longitude: Double = 112.039200,
    val radiusMeters: Float = 100.0f, // Geofencing threshold (100 meter)
    val workStartHour: Int = 7,
    val workStartMinute: Int = 0,
    val lateThresholdHour: Int = 7,
    val lateThresholdMinute: Int = 15,
    val workEndHour: Int = 14,
    val workEndMinute: Int = 0,
    val autoCheckoutHour: Int = 18,
    val autoCheckoutMinute: Int = 0
)

@Entity(tableName = "login_logs")
data class LoginLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val teacherId: String,
    val teacherName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val formattedTime: String,
    val deviceInfo: String,
    val ipOrSessionId: String,
    val status: String = "BERHASIL", // "BERHASIL", "DITOLAK"
    val warningMessage: String = "Peringatan keamanan: Login baru terdeteksi pada sesi ini."
)
