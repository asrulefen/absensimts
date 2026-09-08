package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AppTab
import com.example.ui.MainViewModel
import com.example.ui.UiEvent
import com.example.ui.components.AppBottomNavBar
import com.example.ui.components.InactivityWarningDialog
import com.example.ui.screens.AdminTeacherManagementScreen
import com.example.ui.screens.AttendanceScreen
import com.example.ui.screens.GeofenceMapScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.RecapScreen
import com.example.ui.screens.SecurityLogScreen
import com.example.ui.screens.WebPortalScreen
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldTertiary
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainApp(viewModel: MainViewModel) {
    val context = LocalContext.current

    // Observe StateFlows safely with lifecycle
    val currentTeacher by viewModel.currentTeacher.collectAsStateWithLifecycle()
    val allTeachers by viewModel.allTeachers.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
    val geofenceStatus by viewModel.geofenceStatus.collectAsStateWithLifecycle()
    val locationState by viewModel.locationState.collectAsStateWithLifecycle()
    val todayAttendance by viewModel.todayAttendance.collectAsStateWithLifecycle()
    val allAttendances by viewModel.allAttendances.collectAsStateWithLifecycle()
    val loginLogs by viewModel.loginLogs.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedRecapMonth.collectAsStateWithLifecycle()
    val selectedTeacherFilter by viewModel.recapTeacherFilter.collectAsStateWithLifecycle()
    val firebaseSyncStatus by viewModel.firebaseSyncStatus.collectAsStateWithLifecycle()
    val isFirebaseOnline by viewModel.isFirebaseOnline.collectAsStateWithLifecycle()

    var loginSecurityAlertData by remember { mutableStateOf<Pair<String, String>?>(null) }
    var registrationPromptData by remember { mutableStateOf<Pair<String, String>?>(null) }

    // Request runtime permissions for GPS & Notification
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        viewModel.refreshLocation()
    }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val needsRequest = permissions.any {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needsRequest) {
            permissionsLauncher.launch(permissions.toTypedArray())
        }
    }

    // Handle UI Events
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is UiEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
                is UiEvent.LoginSuccessAlert -> {
                    loginSecurityAlertData = Pair(event.teacherName, event.time)
                }
                is UiEvent.PromptAccountRegistration -> {
                    registrationPromptData = Pair(event.usernameOrNip, event.passwordAttempt)
                }
            }
        }
    }

    val focusManager = LocalFocusManager.current

    // Clear active focus and any floating action modes whenever currentTeacher or selectedTab changes
    LaunchedEffect(currentTeacher, selectedTab) {
        focusManager.clearFocus(force = true)
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (currentTeacher == null) {
            // Login Screen (Direct Username & Password)
            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                LoginScreen(
                    initialUsername = viewModel.savedUsername,
                    firebaseSyncStatus = firebaseSyncStatus,
                    isFirebaseOnline = isFirebaseOnline,
                    promptRegistrationData = registrationPromptData,
                    onDismissRegistrationPrompt = { registrationPromptData = null },
                    onLogin = { username, password, rememberMe ->
                        focusManager.clearFocus(force = true)
                        viewModel.loginTeacher(username, password, rememberMe)
                    },
                    onRegister = { nip, name, subject, password, rememberMe ->
                        focusManager.clearFocus(force = true)
                        viewModel.registerAndLoginTeacher(nip, name, subject, password, rememberMe)
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        } else {
            // Main Logged-In Flow
            val teacher = currentTeacher!!

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
                    AppBottomNavBar(
                        selectedTab = selectedTab,
                        currentTeacher = teacher,
                        onTabSelected = { newTab ->
                            focusManager.clearFocus(force = true)
                            viewModel.selectTab(newTab)
                        }
                    )
                }
            ) { innerPadding ->
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "tab_transition",
                    modifier = Modifier.padding(innerPadding)
                ) { tab ->
                    when (tab) {
                        AppTab.PRESENSI -> {
                            AttendanceScreen(
                                teacher = teacher,
                                todayAttendance = todayAttendance,
                                geofenceStatus = geofenceStatus,
                                sessionRemainingSeconds = sessionState.secondsRemaining,
                                firebaseSyncStatus = firebaseSyncStatus,
                                onCheckIn = { subjectNote ->
                                    viewModel.checkIn(subjectNote)
                                },
                                onCheckOut = {
                                    viewModel.checkOut()
                                },
                                onSubmitPermit = { statusType, reason ->
                                    viewModel.submitSpecialPermit(statusType, reason)
                                },
                                onRefreshLocation = {
                                    viewModel.refreshLocation()
                                },
                                onCalibrateLocation = {
                                    viewModel.calibrateSchoolToCurrentGps()
                                },
                                onLogout = {
                                    viewModel.logout()
                                }
                            )
                        }

                        AppTab.REKAPITULASI -> {
                            RecapScreen(
                                currentTeacher = teacher,
                                attendances = allAttendances,
                                teachers = allTeachers,
                                selectedMonth = selectedMonth,
                                onSelectMonth = { viewModel.setRecapMonthFilter(it) },
                                selectedTeacherFilter = selectedTeacherFilter,
                                onSelectTeacherFilter = { viewModel.setRecapTeacherFilter(it) }
                            )
                        }

                        AppTab.KELOLA_GURU -> {
                            AdminTeacherManagementScreen(
                                teachers = allTeachers,
                                onAddTeacher = { newTeacher ->
                                    viewModel.addTeacher(newTeacher)
                                },
                                onUpdateTeacher = { updated ->
                                    viewModel.updateTeacher(updated)
                                },
                                onDeleteTeacher = { teacherId ->
                                    viewModel.deleteTeacher(teacherId)
                                },
                                onResetDevice = { teacherId ->
                                    viewModel.resetTeacherDevice(teacherId)
                                }
                            )
                        }

                        AppTab.PORTAL_WEB -> {
                            WebPortalScreen(
                                currentTeacher = teacher,
                                websiteUrl = geofenceStatus.school.websiteUrl.ifBlank { "https://mtsalasyari.my.id/login" }
                            )
                        }

                        AppTab.LOG_KEAMANAN -> {
                            SecurityLogScreen(
                                currentTeacher = teacher,
                                loginLogs = loginLogs,
                                sessionRemainingSeconds = sessionState.secondsRemaining,
                                onExtendSession = { viewModel.extendSession() }
                            )
                        }

                        AppTab.GEOFENCE_PETA -> {
                            GeofenceMapScreen(
                                schoolGeofence = geofenceStatus.school,
                                locationState = locationState,
                                geofenceStatus = geofenceStatus,
                                onCalibrateToCurrentLocation = {
                                    viewModel.calibrateSchoolToCurrentGps()
                                },
                                onUpdateRadius = { newRadius ->
                                    viewModel.updateGeofenceRadius(newRadius)
                                },
                                onRestoreDefaultLocation = {
                                    viewModel.restoreDefaultGeofence()
                                },
                                onRefreshGps = {
                                    viewModel.refreshLocation()
                                }
                            )
                        }
                    }
                }
            }
        }

        // Login Security Alert Dialog
        loginSecurityAlertData?.let { (teacherName, time) ->
            AlertDialog(
                onDismissRequest = { loginSecurityAlertData = null },
                icon = {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = "Peringatan Login",
                        tint = GoldTertiary
                    )
                },
                title = {
                    Text(
                        text = "⚠️ Notifikasi Peringatan Login",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                },
                text = {
                    Text(
                        text = "Peringatan Keamanan Madrasah:\n\n" +
                               "Akun $teacherName berhasil login pada $time WIB.\n\n" +
                               "Notifikasi sistem peringatan login telah dikirimkan ke perangkat ini. Akun Anda tersimpan di perangkat ini dan terproteksi geofencing GPS akurat madrasah.",
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { loginSecurityAlertData = null },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text("Saya Mengerti")
                    }
                }
            )
        }
    }
}
