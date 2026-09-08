package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.sp
import com.example.data.model.Teacher
import com.example.ui.AppTab
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPrimary

@Composable
fun AppBottomNavBar(
    selectedTab: AppTab,
    currentTeacher: Teacher,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier.testTag("bottom_nav_bar")
    ) {
        // 1. Presensi (Semua guru & admin)
        NavigationBarItem(
            selected = selectedTab == AppTab.PRESENSI,
            onClick = { onTabSelected(AppTab.PRESENSI) },
            icon = {
                Icon(
                    imageVector = if (selectedTab == AppTab.PRESENSI) Icons.Filled.School else Icons.Outlined.School,
                    contentDescription = "Presensi"
                )
            },
            label = { Text("Presensi", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = EmeraldPrimary,
                indicatorColor = EmeraldContainer
            ),
            modifier = Modifier.testTag("nav_tab_presensi")
        )

        // 2. Rekapitulasi (Admin: Semua Guru; Guru: Individu)
        NavigationBarItem(
            selected = selectedTab == AppTab.REKAPITULASI,
            onClick = { onTabSelected(AppTab.REKAPITULASI) },
            icon = {
                Icon(
                    imageVector = if (selectedTab == AppTab.REKAPITULASI) Icons.Filled.Assessment else Icons.Outlined.Assessment,
                    contentDescription = "Rekapitulasi"
                )
            },
            label = {
                Text(
                    text = if (currentTeacher.isAdmin) "Rekap Madrasah" else "Rekap Saya",
                    fontSize = 10.sp
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = EmeraldPrimary,
                indicatorColor = EmeraldContainer
            ),
            modifier = Modifier.testTag("nav_tab_rekapitulasi")
        )

        // 3. Kelola Guru (KHUSUS ADMIN)
        if (currentTeacher.isAdmin) {
            NavigationBarItem(
                selected = selectedTab == AppTab.KELOLA_GURU,
                onClick = { onTabSelected(AppTab.KELOLA_GURU) },
                icon = {
                    Icon(
                        imageVector = if (selectedTab == AppTab.KELOLA_GURU) Icons.Filled.AdminPanelSettings else Icons.Outlined.AdminPanelSettings,
                        contentDescription = "Kelola Guru"
                    )
                },
                label = { Text("Kelola Guru", fontSize = 10.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = EmeraldPrimary,
                    indicatorColor = EmeraldContainer
                ),
                modifier = Modifier.testTag("nav_tab_kelola_guru")
            )
        }

        // 4. Portal Web https://mtsalasyari.my.id/login
        NavigationBarItem(
            selected = selectedTab == AppTab.PORTAL_WEB,
            onClick = { onTabSelected(AppTab.PORTAL_WEB) },
            icon = {
                Icon(
                    imageVector = if (selectedTab == AppTab.PORTAL_WEB) Icons.Filled.Language else Icons.Outlined.Language,
                    contentDescription = "Website Portal"
                )
            },
            label = { Text("Web MTs", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = EmeraldPrimary,
                indicatorColor = EmeraldContainer
            ),
            modifier = Modifier.testTag("nav_tab_portal_web")
        )

        // 5. Geofencing Map / GPS Status
        NavigationBarItem(
            selected = selectedTab == AppTab.GEOFENCE_PETA,
            onClick = { onTabSelected(AppTab.GEOFENCE_PETA) },
            icon = {
                Icon(
                    imageVector = if (selectedTab == AppTab.GEOFENCE_PETA) Icons.Filled.LocationOn else Icons.Outlined.LocationOn,
                    contentDescription = "Geofence"
                )
            },
            label = { Text("Geofence", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = EmeraldPrimary,
                indicatorColor = EmeraldContainer
            ),
            modifier = Modifier.testTag("nav_tab_geofence")
        )
    }
}
