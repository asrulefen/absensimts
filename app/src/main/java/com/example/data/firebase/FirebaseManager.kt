package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.example.data.model.Attendance
import com.example.data.model.Teacher
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FirebaseManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _syncStatus = MutableStateFlow("Firebase Menghubungkan...")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    private var firestore: FirebaseFirestore? = null
    private var teachersListener: ListenerRegistration? = null
    private var attendancesListener: ListenerRegistration? = null

    init {
        initFirebase()
    }

    private fun initFirebase() {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId(context.packageName)
                    .setProjectId("mts-alasyari-absensi")
                    .setApiKey("AIzaSyB-mts-alasyari-firebase-online-sync")
                    .build()
                FirebaseApp.initializeApp(context, options)
            }

            val db = FirebaseFirestore.getInstance()
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
            db.firestoreSettings = settings
            firestore = db

            _isOnline.value = true
            _syncStatus.value = "🟢 Firebase Realtime Online"
            Log.d("FirebaseManager", "Firebase Firestore initialized successfully")
        } catch (e: Exception) {
            Log.w("FirebaseManager", "Firebase init fallback: ${e.message}")
            _isOnline.value = false
            _syncStatus.value = "🟡 Mode Lokal / Offline Sync"
        }
    }

    /**
     * Start real-time synchronization for teachers collection
     */
    fun startTeachersRealtimeListener(onRemoteTeachersUpdated: (List<Teacher>) -> Unit) {
        val db = firestore ?: return
        try {
            teachersListener?.remove()
            teachersListener = db.collection("teachers")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w("FirebaseManager", "Teachers listener error: ${error.message}")
                        _isOnline.value = false
                        return@addSnapshotListener
                    }

                    _isOnline.value = true
                    _syncStatus.value = "🟢 Firebase Realtime Online"

                    if (snapshot != null && !snapshot.isEmpty) {
                        val teachers = snapshot.documents.mapNotNull { doc ->
                            try {
                                Teacher(
                                    id = doc.getString("id") ?: doc.id,
                                    name = doc.getString("name") ?: "",
                                    subject = doc.getString("subject") ?: "",
                                    role = doc.getString("role") ?: "GURU",
                                    pin = doc.getString("pin") ?: "123456",
                                    email = doc.getString("email") ?: "",
                                    photoUrl = doc.getString("photoUrl") ?: "",
                                    registeredDeviceId = doc.getString("registeredDeviceId") ?: "",
                                    deviceModel = doc.getString("deviceModel") ?: ""
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                        if (teachers.isNotEmpty()) {
                            onRemoteTeachersUpdated(teachers)
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w("FirebaseManager", "startTeachersRealtimeListener failed: ${e.message}")
        }
    }

    /**
     * Start real-time synchronization for attendances collection
     */
    fun startAttendancesRealtimeListener(onRemoteAttendancesUpdated: (List<Attendance>) -> Unit) {
        val db = firestore ?: return
        try {
            attendancesListener?.remove()
            attendancesListener = db.collection("attendances")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w("FirebaseManager", "Attendances listener error: ${error.message}")
                        return@addSnapshotListener
                    }

                    _isOnline.value = true

                    if (snapshot != null && !snapshot.isEmpty) {
                        val attendances = snapshot.documents.mapNotNull { doc ->
                            try {
                                Attendance(
                                    id = doc.getLong("id") ?: 0L,
                                    teacherId = doc.getString("teacherId") ?: "",
                                    teacherName = doc.getString("teacherName") ?: "",
                                    date = doc.getString("date") ?: "",
                                    checkInTime = doc.getString("checkInTime") ?: "07:00:00",
                                    checkOutTime = doc.getString("checkOutTime"),
                                    status = doc.getString("status") ?: "HADIR",
                                    latitude = doc.getDouble("latitude") ?: 0.0,
                                    longitude = doc.getDouble("longitude") ?: 0.0,
                                    distanceMeters = (doc.getDouble("distanceMeters") ?: 0.0).toFloat(),
                                    isWithinGeofence = doc.getBoolean("isWithinGeofence") ?: true,
                                    subjectNote = doc.getString("subjectNote") ?: "",
                                    remarks = doc.getString("remarks") ?: "",
                                    photoUri = doc.getString("photoUri") ?: ""
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                        if (attendances.isNotEmpty()) {
                            onRemoteAttendancesUpdated(attendances)
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w("FirebaseManager", "startAttendancesRealtimeListener failed: ${e.message}")
        }
    }

    /**
     * Sync single teacher to Firebase Firestore in real-time
     */
    fun syncTeacher(teacher: Teacher) {
        scope.launch(Dispatchers.IO) {
            val db = firestore ?: return@launch
            try {
                val data = hashMapOf(
                    "id" to teacher.id,
                    "name" to teacher.name,
                    "subject" to teacher.subject,
                    "role" to teacher.role,
                    "pin" to teacher.pin,
                    "email" to teacher.email,
                    "registeredDeviceId" to teacher.registeredDeviceId,
                    "deviceModel" to teacher.deviceModel,
                    "updatedAt" to System.currentTimeMillis()
                )
                db.collection("teachers").document(teacher.id)
                    .set(data, SetOptions.merge())
                    .addOnSuccessListener {
                        _isOnline.value = true
                        _syncStatus.value = "🟢 Firebase Realtime Online"
                    }
            } catch (e: Exception) {
                Log.w("FirebaseManager", "syncTeacher failed: ${e.message}")
            }
        }
    }

    /**
     * Delete teacher from Firebase
     */
    fun deleteTeacher(teacherId: String) {
        scope.launch(Dispatchers.IO) {
            val db = firestore ?: return@launch
            try {
                db.collection("teachers").document(teacherId).delete()
            } catch (e: Exception) {
                Log.w("FirebaseManager", "deleteTeacher failed: ${e.message}")
            }
        }
    }

    /**
     * Sync attendance record to Firebase Firestore in real-time
     */
    fun syncAttendance(attendance: Attendance) {
        scope.launch(Dispatchers.IO) {
            val db = firestore ?: return@launch
            try {
                val docId = if (attendance.id > 0) {
                    attendance.id.toString()
                } else {
                    "${attendance.teacherId}_${attendance.date}"
                }

                val data = hashMapOf(
                    "id" to attendance.id,
                    "teacherId" to attendance.teacherId,
                    "teacherName" to attendance.teacherName,
                    "date" to attendance.date,
                    "checkInTime" to attendance.checkInTime,
                    "checkOutTime" to (attendance.checkOutTime ?: ""),
                    "status" to attendance.status,
                    "latitude" to attendance.latitude,
                    "longitude" to attendance.longitude,
                    "distanceMeters" to attendance.distanceMeters,
                    "isWithinGeofence" to attendance.isWithinGeofence,
                    "subjectNote" to attendance.subjectNote,
                    "remarks" to attendance.remarks,
                    "syncedAt" to System.currentTimeMillis()
                )

                db.collection("attendances").document(docId)
                    .set(data, SetOptions.merge())
                    .addOnSuccessListener {
                        _isOnline.value = true
                        _syncStatus.value = "🟢 Firebase Realtime Online"
                    }
            } catch (e: Exception) {
                Log.w("FirebaseManager", "syncAttendance failed: ${e.message}")
            }
        }
    }

    fun cleanUp() {
        teachersListener?.remove()
        attendancesListener?.remove()
    }
}
