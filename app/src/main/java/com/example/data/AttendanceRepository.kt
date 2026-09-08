package com.example.data

import android.location.Location
import com.example.data.dao.AttendanceDao
import com.example.data.dao.LoginLogDao
import com.example.data.dao.SchoolGeofenceDao
import com.example.data.dao.TeacherDao
import com.example.data.firebase.FirebaseManager
import com.example.data.model.Attendance
import com.example.data.model.LoginLog
import com.example.data.model.SchoolGeofence
import com.example.data.model.Teacher
import kotlinx.coroutines.flow.Flow

class AttendanceRepository(
    private val teacherDao: TeacherDao,
    private val attendanceDao: AttendanceDao,
    private val schoolGeofenceDao: SchoolGeofenceDao,
    private val loginLogDao: LoginLogDao,
    val firebaseManager: FirebaseManager? = null
) {
    val allTeachers: Flow<List<Teacher>> = teacherDao.getAllTeachers()
    val allAttendances: Flow<List<Attendance>> = attendanceDao.getAllAttendances()
    val schoolGeofence: Flow<SchoolGeofence?> = schoolGeofenceDao.getGeofenceConfig()
    val loginLogs: Flow<List<LoginLog>> = loginLogDao.getAllLogs()

    suspend fun getTeacherById(id: String): Teacher? = teacherDao.getTeacherById(id)

    suspend fun saveTeacher(teacher: Teacher) {
        teacherDao.insertTeacher(teacher)
        firebaseManager?.syncTeacher(teacher)
    }

    suspend fun updateTeacher(teacher: Teacher) {
        teacherDao.updateTeacher(teacher)
        firebaseManager?.syncTeacher(teacher)
    }

    suspend fun deleteTeacher(teacherId: String) {
        teacherDao.deleteTeacher(teacherId)
        firebaseManager?.deleteTeacher(teacherId)
    }

    suspend fun resetTeacherDevice(teacherId: String) {
        teacherDao.resetTeacherDevice(teacherId)
        val teacher = teacherDao.getTeacherById(teacherId)
        if (teacher != null) {
            firebaseManager?.syncTeacher(teacher)
        }
    }

    fun getAttendancesByTeacher(teacherId: String): Flow<List<Attendance>> =
        attendanceDao.getAttendancesByTeacher(teacherId)

    suspend fun getTodayAttendance(teacherId: String, date: String): Attendance? =
        attendanceDao.getTodayAttendance(teacherId, date)

    fun getTodayAttendanceFlow(teacherId: String, date: String): Flow<Attendance?> =
        attendanceDao.getTodayAttendanceFlow(teacherId, date)

    fun getAttendancesByMonth(monthYearPattern: String): Flow<List<Attendance>> =
        attendanceDao.getAttendancesByMonth(monthYearPattern)

    suspend fun recordCheckIn(attendance: Attendance): Long {
        val insertedId = attendanceDao.insertAttendance(attendance)
        firebaseManager?.syncAttendance(attendance.copy(id = insertedId))
        return insertedId
    }

    suspend fun updateAttendance(attendance: Attendance) {
        attendanceDao.updateAttendance(attendance)
        firebaseManager?.syncAttendance(attendance)
    }

    suspend fun getGeofenceConfigDirect(): SchoolGeofence =
        schoolGeofenceDao.getGeofenceConfigDirect() ?: SchoolGeofence()

    suspend fun updateGeofenceConfig(config: SchoolGeofence) =
        schoolGeofenceDao.insertOrUpdate(config)

    suspend fun recordLoginLog(log: LoginLog): Long =
        loginLogDao.insertLog(log)

    fun getLoginLogsForTeacher(teacherId: String): Flow<List<LoginLog>> =
        loginLogDao.getLogsForTeacher(teacherId)

    companion object {
        /**
         * Calculates distance between user GPS coordinate and target geofence in meters.
         */
        fun calculateDistanceMeters(
            userLat: Double,
            userLng: Double,
            targetLat: Double,
            targetLng: Double
        ): Float {
            val results = FloatArray(1)
            Location.distanceBetween(userLat, userLng, targetLat, targetLng, results)
            return results[0]
        }
    }
}
