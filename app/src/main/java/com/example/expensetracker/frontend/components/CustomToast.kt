package com.example.expensetracker.frontend.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay

// ─── Toast Type ───────────────────────────────────────────────────────────────

enum class ToastType {
    SUCCESS, ERROR, INFO
}

// ─── Toast State (hoist this in your screen) ──────────────────────────────────

data class ToastMessage(
    val message : String,
    val type    : ToastType = ToastType.SUCCESS,
    val id      : Long      = System.currentTimeMillis()   // unique key triggers reshow
)

// ─── Toast Host ───────────────────────────────────────────────────────────────

/**
 * Drop this into any Scaffold's content area (or Box overlay).
 * Pass [toast] = null to hide.  Set a new [ToastMessage] to show.
 */
@Composable
fun CustomToast(
    toast     : ToastMessage?,
    onDismiss : () -> Unit,
    modifier  : Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }

    // Show whenever a new toast arrives (id changes)
    LaunchedEffect(toast?.id) {
        if (toast != null) {
            visible = true
            delay(3000L)
            visible = false
            delay(400L)   // wait for exit animation
            onDismiss()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .zIndex(100f),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = visible,
            enter   = slideInVertically(tween(350)) { -it } + fadeIn(tween(350)),
            exit    = slideOutVertically(tween(300)) { -it } + fadeOut(tween(300))
        ) {
            toast?.let { t ->
                val (bgColor, iconColor, icon) = when (t.type) {
                    ToastType.SUCCESS -> Triple(Color(0xFF1A2E1A), Color(0xFF4ADE80), Icons.Default.CheckCircle)
                    ToastType.ERROR   -> Triple(Color(0xFF2E1A1A), Color(0xFFF87171), Icons.Default.Error)
                    ToastType.INFO    -> Triple(Color(0xFF1A1F2E), Color(0xFF60A5FA), Icons.Default.Info)
                }

                Row(
                    modifier = Modifier
                        .shadow(12.dp, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .background(bgColor)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
                    Text(
                        text       = t.message,
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = Color.White,
                        lineHeight = 19.sp
                    )
                }
            }
        }
    }
}