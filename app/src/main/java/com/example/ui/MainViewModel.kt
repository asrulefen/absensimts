package com.example.ui

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.AttendanceRepository
import com.example.data.firebase.FirebaseManager
import com.example.data.model.Attendance
import com.example.data.model.LoginLog
import com.example.data.model.SchoolGeofence
import com.example.data.model.Teacher
import com.example.location.LocationHelper
import com.example.location.LocationState
import com.example.security.SecurityNotificationHelper
import com.example.security.SessionManager
import com.example.security.SessionState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class AppTab {
    PRESENSI,
    REKAPITULASI,
    KELOLA_GURU, // Admin only tab
    PORTAL_WEB,  // Direct access to https://mtsalasyari.my.id/login
    LOG_KEAMANAN,
    GEOFENCE_PETA
}

sealed class UiEvent {
    data class ShowToast(val message: String) : UiEvent()
    data class LoginSuccessAlert(val teacherName: String, val time: String) : UiEvent()
    data class PromptAccountRegistration(val usernameOrNip: String, val passwordAttempt: String) : UiEvent()
}

data class GeofenceStatus(
    val school: SchoolGeofence = SchoolGeofence(),
    val currentLat: Double = 0.0,
    val currentLng: Double = 0.0,
    val accuracy: Float = 0.0f,
    val distanceMeters: Float = 0.0f,
    val isWithinRadius: Boolean = false,
    val isGpsActive: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val firebaseManager = FirebaseManager(application, viewModelScope)
    private val repository = AttendanceRepository(
        database.teacherDao(),
        database.attendanceDao(),
        database.schoolGeofenceDao(),
        database.loginLogDao(),
        firebaseManager = firebaseManager
    )

    val isFirebaseOnline: StateFlow<Boolean> = firebaseManager.isOnline
    val firebaseSyncStatus: StateFlow<String> = firebaseManager.syncStatus

    private val locationHelper = LocationHelper(application)
    private val notificationHelper = SecurityNotificationHelper(application)

    private val prefs = application.getSharedPreferences("mts_teacher_session", Context.MODE_PRIVATE)

    val savedUsername: String
        get() = prefs.getString("saved_username", "") ?: ""

    val isRememberLoginEnabled: Boolean
        get() = prefs.getBoolean("remember_login", true)

    private val sessionManager = SessionManager(
        scope = viewModelScope,
        onAutoLogoutTriggered = {
            handleAutoLogout()
        }
    )

    init {
        // 1. Listen to real-time teacher updates from Firebase
        firebaseManager.startTeachersRealtimeListener { remoteTeachers ->
            viewModelScope.launch {
                for (teacher in remoteTeachers) {
                    database.teacherDao().insertTeacher(teacher)
                }
            }
        }

        // 2. Listen to real-time attendance updates from Firebase
        firebaseManager.startAttendancesRealtimeListener { remoteAttendances ->
            viewModelScope.launch {
                for (att in remoteAttendances) {
                    database.attendanceDao().insertAttendance(att)
                }
            }
        }

        // 3. Initial sync of existing local teachers to Firebase in cloud
        viewModelScope.launch {
            try {
                val currentLocalTeachers = database.teacherDao().getAllTeachersList()
                for (teacher in currentLocalTeachers) {
                    firebaseManager.syncTeacher(teacher)
                }
            } catch (e: Exception) {
                // Silently continue
            }
        }

        viewModelScope.launch {
            val savedTeacherId = prefs.getString("saved_teacher_id", null)
            val rememberLogin = prefs.getBoolean("remember_login", true)
            if (!savedTeacherId.isNullOrBlank() && rememberLogin) {
                val direct = repository.getTeacherById(savedTeacherId)
                if (direct != null) {
                    _currentTeacher.value = direct
                    sessionManager.startSession()
                    loadTodayAttendance(direct.id)
                } else {
                    repository.allTeachers.collect { teachers ->
                        if (_currentTeacher.value == null && teachers.isNotEmpty()) {
                            val matching = teachers.firstOrNull { it.id == savedTeacherId }
                            if (matching != null) {
                                _currentTeacher.value = matching
                                sessionManager.startSession()
                                loadTodayAttendance(matching.id)
                            }
                        }
                    }
                }
            }
        }
    }

    val sessionState: StateFlow<SessionState> = sessionManager.sessionState
    val locationState: StateFlow<LocationState> = locationHelper.locationState

    val allTeachers: StateFlow<List<Teacher>> = repository.allTeachers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val schoolGeofence: StateFlow<SchoolGeofence> = repository.schoolGeofence
        .combine(MutableStateFlow(SchoolGeofence())) { config, default ->
            config ?: default
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SchoolGeofence())

    val allAttendances: StateFlow<List<Attendance>> = repository.allAttendances
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val loginLogs: StateFlow<List<LoginLog>> = repository.loginLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active logged in teacher
    private val _currentTeacher = MutableStateFlow<Teacher?>(null)
    val currentTeacher: StateFlow<Teacher?> = _currentTeacher.asStateFlow()

    // Current navigation tab
    private val _selectedTab = MutableStateFlow(AppTab.PRESENSI)
    val selectedTab: StateFlow<AppTab> = _selectedTab.asStateFlow()

    // Today's attendance for current teacher
    private val _todayAttendance = MutableStateFlow<Attendance?>(null)
    val todayAttendance: StateFlow<Attendance?> = _todayAttendance.asStateFlow()

    // Filter month for Recap (e.g. "2026-09" or "ALL")
    private val _selectedRecapMonth = MutableStateFlow(getCurrentMonthPattern())
    val selectedRecapMonth: StateFlow<String> = _selectedRecapMonth.asStateFlow()

    // Selected teacher filter for Recap (null for all teachers, or specific NIP)
    private val _recapTeacherFilter = MutableStateFlow<String?>("ALL")
    val recapTeacherFilter: StateFlow<String?> = _recapTeacherFilter.asStateFlow()

    // UI Events (toast, alerts)
    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow: SharedFlow<UiEvent> = _eventFlow.asSharedFlow()

    // High accuracy Geofencing computation combining GPS location with Madrasah coordinates
    val geofenceStatus: StateFlow<GeofenceStatus> = combine(
        locationState,
        schoolGeofence
    ) { loc, school ->
        val distance = if (loc.latitude != 0.0 && loc.longitude != 0.0) {
            AttendanceRepository.calculateDistanceMeters(
                loc.latitude,
                loc.longitude,
                school.latitude,
                school.longitude
            )
        } else {
            999999.0f
        }
        val isWithin = distance <= school.radiusMeters

        GeofenceStatus(
            school = school,
            currentLat = loc.latitude,
            currentLng = loc.longitude,
            accuracy = loc.accuracy,
            distanceMeters = distance,
            isWithinRadius = isWithin,
            isGpsActive = loc.latitude != 0.0 && loc.longitude != 0.0
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GeofenceStatus())

    init {
        // Start GPS updates for geofencing
        locationHelper.startLocationUpdates()
    }

    fun selectTab(tab: AppTab) {
        _selectedTab.value = tab
        sessionManager.recordUserActivity()
    }

    fun onUserActivity() {
        sessionManager.recordUserActivity()
    }

    fun extendSession() {
        sessionManager.extendSession()
    }

    fun dismissAutoLogoutNotice() {
        sessionManager.dismissAutoLogoutNotice()
    }

    fun refreshLocation() {
        locationHelper.startLocationUpdates()
        sessionManager.recordUserActivity()
    }

    // Set current GPS as School Location (calibration feature)
    fun calibrateSchoolToCurrentGps() {
        val loc = locationState.value
        if (loc.latitude != 0.0 && loc.longitude != 0.0) {
            viewModelScope.launch {
                val currentConfig = schoolGeofence.value
                val updated = currentConfig.copy(
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    schoolAddress = "Titik Koordinat Terkalibrasi GPS (${String.format(Locale.US, "%.5f", loc.latitude)}, ${String.format(Locale.US, "%.5f", loc.longitude)})"
                )
                repository.updateGeofenceConfig(updated)
                _eventFlow.emit(UiEvent.ShowToast("Lokasi Madrasah berhasil disinkronkan dengan GPS Anda!"))
            }
        } else {
            viewModelScope.launch {
                _eventFlow.emit(UiEvent.ShowToast("Menunggu sinyal GPS sebelum kalibrasi..."))
            }
        }
    }

    fun updateGeofenceRadius(newRadiusMeters: Float) {
        viewModelScope.launch {
            val updated = schoolGeofence.value.copy(radiusMeters = newRadiusMeters)
            repository.updateGeofenceConfig(updated)
            _eventFlow.emit(UiEvent.ShowToast("Radius geofencing diubah menjadi ${newRadiusMeters.toInt()}m"))
        }
    }

    fun restoreDefaultGeofence() {
        viewModelScope.launch {
            val default = SchoolGeofence(
                id = 1,
                schoolName = "MTs Al-Asy'ari Prunggahan Kulon",
                schoolAddress = "Prunggahan Kulon, Kec. Semanding, Kabupaten Tuban, Jawa Timur",
                latitude = -6.914100,
                longitude = 112.039200,
                radiusMeters = 80.0f,
                websiteUrl = "https://mtsalasyari.my.id/login"
            )
            repository.updateGeofenceConfig(default)
            _eventFlow.emit(UiEvent.ShowToast("Koordinat MTs Al-Asy'ari dikembalikan ke default."))
        }
    }

    // Login process with Username/NIP and Password, persistent session, and Firebase realtime support
    fun loginTeacher(usernameOrNip: String, passwordText: String, rememberMe: Boolean = true) {
        viewModelScope.launch {
            val cleanInput = usernameOrNip.trim()
            val cleanPassword = passwordText.trim()

            if (cleanInput.isBlank()) {
                _eventFlow.emit(UiEvent.ShowToast("Harap masukkan Username atau NIP Guru!"))
                return@launch
            }
            if (cleanPassword.isBlank()) {
                _eventFlow.emit(UiEvent.ShowToast("Harap masukkan Password Akun!"))
                return@launch
            }

            var teacher = repository.getTeacherById(cleanInput)
            if (teacher == null) {
                val teachers = allTeachers.value
                teacher = teachers.firstOrNull { it.id.equals(cleanInput, ignoreCase = true) }
                    ?: teachers.firstOrNull { it.email.isNotBlank() && it.email.equals(cleanInput, ignoreCase = true) }
                    ?: teachers.firstOrNull { it.name.equals(cleanInput, ignoreCase = true) }
                    ?: teachers.firstOrNull { it.name.contains(cleanInput, ignoreCase = true) }
            }

            // Special fallback for admin account
            if (teacher == null && (cleanInput.equals("admin", ignoreCase = true) || cleanInput.equals("administrator", ignoreCase = true))) {
                val adminTeacher = Teacher(
                    id = "admin",
                    name = "Administrator MTs Al-Asy'ari",
                    subject = "Kepala Tata Usaha & IT Portal",
                    role = "ADMIN",
                    pin = "admin123",
                    email = "admin@mtsalasyari.my.id"
                )
                repository.saveTeacher(adminTeacher)
                teacher = adminTeacher
            }

            if (teacher == null) {
                _eventFlow.emit(UiEvent.PromptAccountRegistration(cleanInput, cleanPassword))
                _eventFlow.emit(UiEvent.ShowToast("Akun '$cleanInput' belum terdaftar. Silakan lengkapi pendaftaran untuk langsung menggunakan aplikasi."))
                return@launch
            }

            val isPasswordValid = teacher.pin == cleanPassword || 
                (teacher.isAdmin && (cleanPassword == "admin123" || cleanPassword == "123456"))

            if (!isPasswordValid) {
                _eventFlow.emit(UiEvent.ShowToast("Password salah! Silakan periksa kembali."))
                return@launch
            }

            // Device Binding Verification (1 Akun 1 Perangkat HP)
            val currentDeviceId = "${Build.MANUFACTURER}_${Build.MODEL}_${Build.BOARD}"
            if (teacher.registeredDeviceId.isNotBlank() && teacher.registeredDeviceId != currentDeviceId) {
                _eventFlow.emit(
                    UiEvent.ShowToast(
                        "Gagal: Akun ini sudah terikat pada perangkat lain! Hubungi Admin MTs untuk reset perangkat."
                    )
                )
                return@launch
            }

            // Bind device on first login if not yet bound
            if (teacher.registeredDeviceId.isBlank()) {
                val updatedTeacher = teacher.copy(
                    registeredDeviceId = currentDeviceId,
                    deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
                )
                repository.updateTeacher(updatedTeacher)
                _currentTeacher.value = updatedTeacher
            } else {
                _currentTeacher.value = teacher
            }

            // Save session preferences so login is remembered and user does not have to log in repeatedly
            prefs.edit().apply {
                putString("saved_username", cleanInput)
                if (rememberMe) {
                    putString("saved_teacher_id", teacher.id)
                    putBoolean("remember_login", true)
                } else {
                    remove("saved_teacher_id")
                    putBoolean("remember_login", false)
                }
                apply()
            }

            val nowTime = getCurrentTime()
            val nowDate = getCurrentDate()

            // 1. Send real Android security alert notification
            notificationHelper.sendLoginAlertNotification(
                teacherName = teacher.name,
                nip = teacher.id,
                timeFormatted = nowTime
            )

            // 2. Record to security audit trail
            val log = LoginLog(
                teacherId = teacher.id,
                teacherName = teacher.name,
                formattedTime = "$nowDate $nowTime",
                deviceInfo = "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})",
                ipOrSessionId = "SESSION-${System.currentTimeMillis() % 100000}",
                status = "BERHASIL",
                warningMessage = "Login berhasil via Username & Password. Akun tersimpan di perangkat."
            )
            repository.recordLoginLog(log)

            // 3. Mark session active indefinitely (no auto-logout)
            sessionManager.startSession()

            // 4. Load today's attendance for this teacher
            loadTodayAttendance(teacher.id)

            // 5. Emit security alert dialog event
            _eventFlow.emit(UiEvent.LoginSuccessAlert(teacher.name, nowTime))
        }
    }

    // Direct Teacher Registration so any teacher can immediately use the app
    fun registerAndLoginTeacher(
        nipOrUsername: String,
        fullName: String,
        subject: String,
        passwordText: String,
        rememberMe: Boolean = true
    ) {
        viewModelScope.launch {
            val cleanNip = nipOrUsername.trim()
            val cleanName = fullName.trim()
            val cleanSubject = subject.trim()
            val cleanPassword = passwordText.trim()

            if (cleanNip.isBlank()) {
                _eventFlow.emit(UiEvent.ShowToast("NIP atau Username wajib diisi!"))
                return@launch
            }
            if (cleanName.isBlank()) {
                _eventFlow.emit(UiEvent.ShowToast("Nama Guru wajib diisi!"))
                return@launch
            }
            if (cleanPassword.isBlank()) {
                _eventFlow.emit(UiEvent.ShowToast("Password Akun wajib diisi!"))
                return@launch
            }

            val currentDeviceId = "${Build.MANUFACTURER}_${Build.MODEL}_${Build.BOARD}"
            val newTeacher = Teacher(
                id = cleanNip,
                name = cleanName,
                subject = if (cleanSubject.isBlank()) "Guru Mata Pelajaran" else cleanSubject,
                role = if (cleanNip.equals("admin", ignoreCase = true)) "ADMIN" else "GURU",
                pin = cleanPassword,
                email = "",
                registeredDeviceId = currentDeviceId,
                deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
            )

            // Save to Room and sync to Firebase in real-time
            repository.saveTeacher(newTeacher)
            _currentTeacher.value = newTeacher

            // Save session preferences so login is remembered indefinitely
            prefs.edit().apply {
                putString("saved_username", cleanNip)
                if (rememberMe) {
                    putString("saved_teacher_id", newTeacher.id)
                    putBoolean("remember_login", true)
                } else {
                    remove("saved_teacher_id")
                    putBoolean("remember_login", false)
                }
                apply()
            }

            val nowTime = getCurrentTime()
            val nowDate = getCurrentDate()

            notificationHelper.sendLoginAlertNotification(
                teacherName = newTeacher.name,
                nip = newTeacher.id,
                timeFormatted = nowTime
            )

            val log = LoginLog(
                teacherId = newTeacher.id,
                teacherName = newTeacher.name,
                formattedTime = "$nowDate $nowTime",
                deviceInfo = "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})",
                ipOrSessionId = "SESSION-${System.currentTimeMillis() % 100000}",
                status = "BERHASIL",
                warningMessage = "Pendaftaran & Login Akun Guru Baru berhasil. Sinkronisasi online Firebase aktif."
            )
            repository.recordLoginLog(log)

            sessionManager.startSession()
            loadTodayAttendance(newTeacher.id)

            _eventFlow.emit(UiEvent.ShowToast("Selamat datang, ${newTeacher.name}! Akun berhasil dibuat dan tersimpan."))
            _eventFlow.emit(UiEvent.LoginSuccessAlert(newTeacher.name, nowTime))
        }
    }

    fun logout() {
        val teacher = _currentTeacher.value
        _currentTeacher.value = null
        _todayAttendance.value = null
        sessionManager.endSession()
        // Clear saved session so user or admin can switch accounts
        prefs.edit().remove("saved_teacher_id").apply()
        viewModelScope.launch {
            _eventFlow.emit(UiEvent.ShowToast("Akun ${teacher?.name ?: ""} telah keluar."))
        }
    }

    private fun handleAutoLogout() {
        val teacher = _currentTeacher.value
        _currentTeacher.value = null
        _todayAttendance.value = null
        if (teacher != null) {
            notificationHelper.sendAutoLogoutNotification(teacher.name)
        }
    }

    private fun loadTodayAttendance(teacherId: String) {
        viewModelScope.launch {
            val date = getCurrentDate()
            _todayAttendance.value = repository.getTodayAttendance(teacherId, date)
        }
    }

    // Attendance Check-in with STRICT geofencing
    fun checkIn(subjectNote: String) {
        sessionManager.recordUserActivity()
        val teacher = _currentTeacher.value ?: return
        val status = geofenceStatus.value

        // STRICT GEOFENCE ENFORCEMENT
        if (!status.isWithinRadius) {
            viewModelScope.launch {
                val distanceStr = if (status.distanceMeters >= 1000) {
                    "${String.format(Locale.US, "%.2f", status.distanceMeters / 1000f)} km"
                } else {
                    "${status.distanceMeters.toInt()} meter"
                }
                _eventFlow.emit(
                    UiEvent.ShowToast(
                        "ABSENSI DITOLAK! Anda berada di luar radius Madrasah ($distanceStr). " +
                        "Absensi hanya bisa dilakukan di lokasi ${status.school.schoolName} (Maks ${status.school.radiusMeters.toInt()}m)."
                    )
                )
            }
            return
        }

        val date = getCurrentDate()
        val time = getCurrentTime()

        // Determine if late based on threshold
        val cal = Calendar.getInstance()
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)
        val currentMinute = cal.get(Calendar.MINUTE)
        val school = status.school
        val isLate = (currentHour > school.lateThresholdHour) ||
                (currentHour == school.lateThresholdHour && currentMinute > school.lateThresholdMinute)

        val attendanceStatus = if (isLate) "TERLAMBAT" else "HADIR"
        val remarks = if (isLate) {
            "Terlambat hadir (setelah ${String.format("%02d:%02d", school.lateThresholdHour, school.lateThresholdMinute)} WIB)"
        } else {
            "Tepat Waktu"
        }

        viewModelScope.launch {
            val newAttendance = Attendance(
                teacherId = teacher.id,
                teacherName = teacher.name,
                date = date,
                checkInTime = time,
                checkOutTime = null,
                status = attendanceStatus,
                latitude = status.currentLat,
                longitude = status.currentLng,
                distanceMeters = status.distanceMeters,
                isWithinGeofence = true,
                subjectNote = subjectNote.ifBlank { teacher.subject },
                remarks = remarks
            )
            repository.recordCheckIn(newAttendance)
            _todayAttendance.value = newAttendance

            // Notification
            notificationHelper.sendAttendanceNotification(
                teacherName = teacher.name,
                type = "Masuk ($attendanceStatus)",
                time = time,
                distanceMeters = status.distanceMeters
            )

            _eventFlow.emit(
                UiEvent.ShowToast("Alhamdulillah, Presensi Masuk Berhasil! Status: $attendanceStatus")
            )
        }
    }

    // Attendance Check-out with Geofencing
    fun checkOut() {
        sessionManager.recordUserActivity()
        val current = _todayAttendance.value ?: run {
            viewModelScope.launch {
                _eventFlow.emit(UiEvent.ShowToast("Anda belum melakukan Presensi Masuk hari ini."))
            }
            return
        }

        val status = geofenceStatus.value
        if (!status.isWithinRadius) {
            viewModelScope.launch {
                _eventFlow.emit(
                    UiEvent.ShowToast(
                        "ABSENSI PULANG DITOLAK! Anda berada di luar radius Madrasah (${status.distanceMeters.toInt()}m). " +
                        "Presensi pulang hanya sah jika dilakukan di area madrasah."
                    )
                )
            }
            return
        }

        val time = getCurrentTime()
        viewModelScope.launch {
            val updated = current.copy(checkOutTime = time)
            repository.updateAttendance(updated)
            _todayAttendance.value = updated

            notificationHelper.sendAttendanceNotification(
                teacherName = current.teacherName,
                type = "Pulang",
                time = time,
                distanceMeters = status.distanceMeters
            )

            _eventFlow.emit(
                UiEvent.ShowToast("Presensi Pulang berhasil dicatat pada $time WIB. Hati-hati di jalan!")
            )
        }
    }

    // Permitted Absence / Sick / Official Duty submission
    fun submitSpecialPermit(statusType: String, reason: String) {
        sessionManager.recordUserActivity()
        val teacher = _currentTeacher.value ?: return
        val date = getCurrentDate()
        val time = getCurrentTime()
        val loc = locationState.value

        viewModelScope.launch {
            val permitRecord = Attendance(
                teacherId = teacher.id,
                teacherName = teacher.name,
                date = date,
                checkInTime = time,
                checkOutTime = time,
                status = statusType, // "IZIN", "SAKIT", "TUGAS_LUAR"
                latitude = loc.latitude,
                longitude = loc.longitude,
                distanceMeters = 0.0f,
                isWithinGeofence = false,
                subjectNote = "Permohonan $statusType",
                remarks = reason
            )
            repository.recordCheckIn(permitRecord)
            _todayAttendance.value = permitRecord
            _eventFlow.emit(UiEvent.ShowToast("Surat keterangan $statusType berhasil disimpan."))
        }
    }

    fun setRecapMonthFilter(month: String) {
        _selectedRecapMonth.value = month
        sessionManager.recordUserActivity()
    }

    fun setRecapTeacherFilter(teacherId: String?) {
        _recapTeacherFilter.value = teacherId
        sessionManager.recordUserActivity()
    }

    // Teacher Management for Admin
    fun addTeacher(teacher: Teacher) {
        viewModelScope.launch {
            repository.saveTeacher(teacher)
            _eventFlow.emit(UiEvent.ShowToast("Akun guru ${teacher.name} (NIP: ${teacher.id}) berhasil dibuat!"))
        }
    }

    fun updateTeacher(teacher: Teacher) {
        viewModelScope.launch {
            repository.updateTeacher(teacher)
            _eventFlow.emit(UiEvent.ShowToast("Data akun guru ${teacher.name} diperbarui."))
        }
    }

    fun deleteTeacher(teacherId: String) {
        viewModelScope.launch {
            repository.deleteTeacher(teacherId)
            _eventFlow.emit(UiEvent.ShowToast("Akun guru berhasil dihapus."))
        }
    }

    fun resetTeacherDevice(teacherId: String) {
        viewModelScope.launch {
            repository.resetTeacherDevice(teacherId)
            _eventFlow.emit(UiEvent.ShowToast("Perangkat guru berhasil di-reset. Guru dapat login di perangkat baru."))
        }
    }

    private fun getCurrentDate(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    private fun getCurrentTime(): String =
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

    private fun getCurrentMonthPattern(): String =
        SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

    override fun onCleared() {
        super.onCleared()
        locationHelper.stopLocationUpdates()
        sessionManager.endSession()
    }
}
