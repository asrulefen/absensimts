package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.AttendanceDao
import com.example.data.dao.LoginLogDao
import com.example.data.dao.SchoolGeofenceDao
import com.example.data.dao.TeacherDao
import com.example.data.model.Attendance
import com.example.data.model.LoginLog
import com.example.data.model.SchoolGeofence
import com.example.data.model.Teacher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Teacher::class,
        Attendance::class,
        SchoolGeofence::class,
        LoginLog::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun teacherDao(): TeacherDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun schoolGeofenceDao(): SchoolGeofenceDao
    abstract fun loginLogDao(): LoginLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "absensi_mts_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database)
                    }
                }
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        val count = database.teacherDao().getAllTeachersList().size
                        if (count == 0) {
                            populateInitialData(database)
                        }
                    }
                }
            }
        }

        private suspend fun populateInitialData(db: AppDatabase) {
            val teachers = listOf(
                Teacher(
                    id = "admin",
                    name = "Administrator MTs Al-Asy'ari",
                    subject = "Kepala Tata Usaha & IT Portal",
                    role = "ADMIN",
                    pin = "123456",
                    email = "admin@mtsalasyari.my.id",
                    registeredDeviceId = ""
                ),
                Teacher(
                    id = "197804122005011003",
                    name = "Drs. H. Ahmad Fauzi, M.Pd.I",
                    subject = "Fiqih & Ushul Fiqih",
                    role = "GURU",
                    pin = "123456",
                    email = "ahmadfauzi@mtsalasyari.my.id"
                ),
                Teacher(
                    id = "198506242010012015",
                    name = "Dra. Hj. Siti Nurjanah, M.Ag",
                    subject = "Al-Qur'an Hadits",
                    role = "GURU",
                    pin = "123456",
                    email = "sitinurjanah@mtsalasyari.my.id"
                ),
                Teacher(
                    id = "199109152019031008",
                    name = "Muhammad Rofi'i, S.Pd",
                    subject = "Bahasa Arab",
                    role = "GURU",
                    pin = "123456",
                    email = "m.rofii@mtsalasyari.my.id"
                ),
                Teacher(
                    id = "199402102022032011",
                    name = "Fatimah Zahra, S.Pd",
                    subject = "Matematika",
                    role = "GURU",
                    pin = "123456",
                    email = "f.zahra@mtsalasyari.my.id"
                ),
                Teacher(
                    id = "198911032015051002",
                    name = "Ust. Syarif Hidayatullah, S.Th.I",
                    subject = "Akidah Akhlak & SKI",
                    role = "GURU",
                    pin = "123456",
                    email = "syarif@mtsalasyari.my.id"
                )
            )
            db.teacherDao().insertTeachers(teachers)

            // Default School Geofence MTs Al-Asy'ari Prunggahan Kulon
            // Koordinat Prunggahan Kulon, Semanding, Kab. Tuban: -6.914100, 112.039200
            val defaultGeofence = SchoolGeofence(
                id = 1,
                schoolName = "MTs Al-Asy'ari Prunggahan Kulon",
                schoolAddress = "Prunggahan Kulon, Kec. Semanding, Kab. Tuban, Jawa Timur",
                websiteUrl = "https://mtsalasyari.my.id/login",
                latitude = -6.914100,
                longitude = 112.039200,
                radiusMeters = 100.0f,
                workStartHour = 7,
                workStartMinute = 0,
                lateThresholdHour = 7,
                lateThresholdMinute = 15,
                workEndHour = 14,
                workEndMinute = 0,
                autoCheckoutHour = 18,
                autoCheckoutMinute = 0
            )
            db.schoolGeofenceDao().insertOrUpdate(defaultGeofence)

            // Seed initial attendance recapitulation records for demo & visualization
            val sampleRecords = listOf(
                Attendance(
                    teacherId = "197804122005011003",
                    teacherName = "Drs. H. Ahmad Fauzi, M.Pd.I",
                    date = "2026-09-01",
                    checkInTime = "06:48:12",
                    checkOutTime = "14:35:00",
                    status = "HADIR",
                    latitude = -6.175380,
                    longitude = 106.827140,
                    distanceMeters = 15.4f,
                    isWithinGeofence = true,
                    subjectNote = "Fiqih Kelas 9A (Sholat Jenazah)",
                    remarks = "Tepat Waktu"
                ),
                Attendance(
                    teacherId = "197804122005011003",
                    teacherName = "Drs. H. Ahmad Fauzi, M.Pd.I",
                    date = "2026-09-02",
                    checkInTime = "06:55:40",
                    checkOutTime = "14:40:22",
                    status = "HADIR",
                    latitude = -6.175400,
                    longitude = 106.827160,
                    distanceMeters = 21.0f,
                    isWithinGeofence = true,
                    subjectNote = "Fiqih Kelas 9B",
                    remarks = "Tepat Waktu"
                ),
                Attendance(
                    teacherId = "197804122005011003",
                    teacherName = "Drs. H. Ahmad Fauzi, M.Pd.I",
                    date = "2026-09-03",
                    checkInTime = "07:38:15",
                    checkOutTime = "14:32:10",
                    status = "TERLAMBAT",
                    latitude = -6.175360,
                    longitude = 106.827130,
                    distanceMeters = 34.2f,
                    isWithinGeofence = true,
                    subjectNote = "Fiqih Kelas 7A",
                    remarks = "Terlambat 8 menit (Macet hujan)"
                ),
                Attendance(
                    teacherId = "197804122005011003",
                    teacherName = "Drs. H. Ahmad Fauzi, M.Pd.I",
                    date = "2026-09-04",
                    checkInTime = "06:42:05",
                    checkOutTime = "14:30:45",
                    status = "HADIR",
                    latitude = -6.175390,
                    longitude = 106.827150,
                    distanceMeters = 12.1f,
                    isWithinGeofence = true,
                    subjectNote = "Imam Sholat Dhuha bersama",
                    remarks = "Tepat Waktu"
                ),
                Attendance(
                    teacherId = "198506242010012015",
                    teacherName = "Dra. Hj. Siti Nurjanah, M.Ag",
                    date = "2026-09-01",
                    checkInTime = "06:52:10",
                    checkOutTime = "14:45:00",
                    status = "HADIR",
                    latitude = -6.175395,
                    longitude = 106.827155,
                    distanceMeters = 18.0f,
                    isWithinGeofence = true,
                    subjectNote = "Al-Qur'an Hadits Kelas 8A",
                    remarks = "Tepat Waktu"
                ),
                Attendance(
                    teacherId = "198506242010012015",
                    teacherName = "Dra. Hj. Siti Nurjanah, M.Ag",
                    date = "2026-09-02",
                    checkInTime = "06:49:30",
                    checkOutTime = "14:35:10",
                    status = "HADIR",
                    latitude = -6.175388,
                    longitude = 106.827145,
                    distanceMeters = 14.5f,
                    isWithinGeofence = true,
                    subjectNote = "Tadarrus Al-Qur'an Pagi",
                    remarks = "Tepat Waktu"
                ),
                Attendance(
                    teacherId = "198506242010012015",
                    teacherName = "Dra. Hj. Siti Nurjanah, M.Ag",
                    date = "2026-09-03",
                    checkInTime = "07:00:00",
                    checkOutTime = "14:30:00",
                    status = "IZIN",
                    latitude = -6.175392,
                    longitude = 106.827153,
                    distanceMeters = 0.0f,
                    isWithinGeofence = true,
                    subjectNote = "Pelatihan Kurikulum Merdeka Kemenag",
                    remarks = "Surat Tugas Kemenag No. 421/B/2026"
                )
            )
            for (rec in sampleRecords) {
                db.attendanceDao().insertAttendance(rec)
            }
        }
    }
}
