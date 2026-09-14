package com.example.expensetracker.frontend.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.expensetracker.frontend.important.Appstate
import com.example.expensetracker.ui.theme.Primary
import com.example.expensetracker.ui.theme.TextSecondary

// ─── BottomNavBar ─────────────────────────────────────────────────────────────
// Plain, flat 4-item bottom nav bar — no floating/raised action button.

@Composable
fun BottomNavBar(
    selectedTab: Int,
    navController: NavController,
    surface: Color,
    border: Color,
    textPrimary: Color,
    isDark: Boolean
) {
    fun navigate(route: String, tabIndex: Int) {
        if (selectedTab == tabIndex) return  // ✅ already on this tab, do nothing
        navController.navigate(route) {
            popUpTo("home") { inclusive = false }
            launchSingleTop = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(surface)
            .navigationBarsPadding()
    ) {
        HorizontalDivider(color = border, thickness = 0.5.dp)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavItem(
                icon     = Icons.Filled.Home,
                label    = "Home",
                selected = selectedTab == 0,
                onClick  = { navigate("home", 0) }
            )
            NavItem(
                icon     = Icons.Filled.History,
                label    = "History",
                selected = selectedTab == 1,
                onClick  = { navigate("history", 1) }
            )
            NavItem(
                icon     = Icons.Filled.Settings,
                label    = "Settings",
                selected = selectedTab == 3,
                onClick  = { navigate("settings", 3) }
            )
            NavItem(
                icon     = Icons.Filled.Layers,
                label    = "others",
                selected = selectedTab == 2,
                onClick  = {
                    if (Appstate.isPickerOpen) {
                        Appstate.isPickerOpen = false
                        navController.popBackStack()
                    } else {
                        Appstate.isPickerOpen = true
                        navController.navigate("show_screens") { launchSingleTop = true }
                    }
                }
            )
        }
    }
}

// ─── NavItem ──────────────────────────────────────────────────────────────────

@Composable
fun NavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val color = if (selected) Primary else TextSecondary

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .padding(6.dp)
    ) {
        IconButton(
            onClick  = onClick,
            modifier = Modifier.size(_root_ide_package_.com.example.expensetracker.frontend.screens.UiScale.navIconButton)
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = label,
                tint               = color,
                modifier           = Modifier.size(_root_ide_package_.com.example.expensetracker.frontend.screens.UiScale.navIcon)
            )
        }
        Text(
            text          = label.uppercase(),
            fontSize      = _root_ide_package_.com.example.expensetracker.frontend.screens.UiScale.navLabel,
            fontWeight    = FontWeight.Bold,
            color         = color,
            letterSpacing = 0.6.sp
        )
    }
}