package com.example.security

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity

class SecurityNotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "channel_mts_security"
        const val CHANNEL_NAME = "Keamanan & Absensi MTs"
        const val CHANNEL_DESC = "Notifikasi peringatan login dan konfirmasi absensi guru MTs"
        const val LOGIN_NOTIFICATION_ID = 1001
        const val ATTENDANCE_NOTIFICATION_ID = 1002
        const val LOGOUT_NOTIFICATION_ID = 1003
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun sendLoginAlertNotification(teacherName: String, nip: String, timeFormatted: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setContentTitle("⚠️ Peringatan Keamanan Login MTs")
            .setContentText("Akun $teacherName (NIP: $nip) baru saja login pada $timeFormatted WIB.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "⚠️ PERINGATAN KEAMANAN SISTEM ABSENSI MTS\n\n" +
                        "Akun guru: $teacherName\n" +
                        "NIP/ID: $nip\n" +
                        "Waktu Login: $timeFormatted WIB\n\n" +
                        "Jika aktivitas ini bukan dilakukan oleh Anda, segera laporkan ke Admin Madrasah dan amankan akun Anda!"
                    )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(LOGIN_NOTIFICATION_ID, builder.build())
        } catch (_: SecurityException) {
            // Handled when notification permission is not yet granted
        }
    }

    fun sendAttendanceNotification(
        teacherName: String,
        type: String, // "Masuk" or "Pulang"
        time: String,
        distanceMeters: Float
    ) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
            .setContentTitle("✅ Presensi $type MTs Berhasil")
            .setContentText("Tercatat: $time WIB | Radius: ${String.format("%.1f", distanceMeters)}m dari madrasah")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(ATTENDANCE_NOTIFICATION_ID, builder.build())
        } catch (_: SecurityException) {}
    }

    fun sendAutoLogoutNotification(teacherName: String) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_power_off)
            .setContentTitle("🔒 Auto-Logout MTs Diaktifkan")
            .setContentText("Sesi akun $teacherName ditutup otomatis demi menjaga keamanan data absensi.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(LOGOUT_NOTIFICATION_ID, builder.build())
        } catch (_: SecurityException) {}
    }
}
