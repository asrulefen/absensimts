package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Attendance
import com.example.data.model.LoginLog
import com.example.data.model.SchoolGeofence
import com.example.data.model.Teacher
import kotlinx.coroutines.flow.Flow

@Dao
interface TeacherDao {
    @Query("SELECT * FROM teachers ORDER BY name ASC")
    fun getAllTeachers(): Flow<List<Teacher>>

    @Query("SELECT * FROM teachers ORDER BY name ASC")
    suspend fun getAllTeachersList(): List<Teacher>

    @Query("SELECT * FROM teachers WHERE id = :id LIMIT 1")
    suspend fun getTeacherById(id: String): Teacher?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeacher(teacher: Teacher)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeachers(teachers: List<Teacher>)

    @Update
    suspend fun updateTeacher(teacher: Teacher)

    @Query("DELETE FROM teachers WHERE id = :id")
    suspend fun deleteTeacher(id: String)

    @Query("UPDATE teachers SET registeredDeviceId = '', deviceModel = '' WHERE id = :id")
    suspend fun resetTeacherDevice(id: String)
}

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendances ORDER BY id DESC")
    fun getAllAttendances(): Flow<List<Attendance>>

    @Query("SELECT * FROM attendances WHERE teacherId = :teacherId ORDER BY date DESC, id DESC")
    fun getAttendancesByTeacher(teacherId: String): Flow<List<Attendance>>

    @Query("SELECT * FROM attendances WHERE teacherId = :teacherId AND date = :date LIMIT 1")
    suspend fun getTodayAttendance(teacherId: String, date: String): Attendance?

    @Query("SELECT * FROM attendances WHERE teacherId = :teacherId AND date = :date LIMIT 1")
    fun getTodayAttendanceFlow(teacherId: String, date: String): Flow<Attendance?>

    @Query("SELECT * FROM attendances WHERE date LIKE :monthYearPattern ORDER BY date DESC, id DESC")
    fun getAttendancesByMonth(monthYearPattern: String): Flow<List<Attendance>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: Attendance): Long

    @Update
    suspend fun updateAttendance(attendance: Attendance)

    @Query("DELETE FROM attendances WHERE id = :id")
    suspend fun deleteAttendance(id: Long)
}

@Dao
interface SchoolGeofenceDao {
    @Query("SELECT * FROM school_geofence WHERE id = 1 LIMIT 1")
    fun getGeofenceConfig(): Flow<SchoolGeofence?>

    @Query("SELECT * FROM school_geofence WHERE id = 1 LIMIT 1")
    suspend fun getGeofenceConfigDirect(): SchoolGeofence?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(config: SchoolGeofence)
}

@Dao
interface LoginLogDao {
    @Query("SELECT * FROM login_logs ORDER BY timestamp DESC LIMIT 50")
    fun getAllLogs(): Flow<List<LoginLog>>

    @Query("SELECT * FROM login_logs WHERE teacherId = :teacherId ORDER BY timestamp DESC LIMIT 30")
    fun getLogsForTeacher(teacherId: String): Flow<List<LoginLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: LoginLog): Long
}
