package com.example.expensetracker.frontend.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensetracker.ui.theme.Primary
import com.example.expensetracker.ui.theme.TextPrimary

// ─────────────────────────────────────────────────────────────────────────────
//  ErrorStateContent
//
//  A minimal icon-based error state — no image, no Scaffold.
//  No image because the top and bottom bars already reduce available space,
//  and a large illustration would feel cramped. A simple icon fits cleanly.
//
//  Slots directly into your existing screen's Scaffold content lambda.
//
//  Usage
//  -----
//      Scaffold(
//          topBar    = { TopAppBar(...) },
//          bottomBar = { BottomNavBar(...) }
//      ) { padding ->
//          when (val s = uiState) {
//              is UiState.Error   -> ErrorStateContent(
//                                        errorMessage = s.message,
//                                        onRetry      = { viewModel.reload() },
//                                        isDark       = isDark,
//                                        modifier     = Modifier.padding(padding)
//                                    )
//              is UiState.Loaded  -> { /* normal screen content */ }
//              is UiState.Loading -> { /* loading shimmer */ }
//          }
//      }
// ─────────────────────────────────────────────────────────────────────────────

private val BrandBlue  = Color(0xFF2B4BF2)
private val ErrorRed   = Color(0xFFE24B4A)
private val ErrorRedBg = Color(0xFFFEF2F2)
private val MutedGray  = Color(0xFF9AA5B4)

@Composable
fun ErrorStateContent(
    errorMessage: String = "Something went wrong. Please try again.",
    onRetry: () -> Unit = {},
    isDark: Boolean = false,
    modifier: Modifier = Modifier
) {
    val textPrimary = if (isDark) Color.White else TextPrimary

    Column(
        modifier            = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // ── Icon circle ───────────────────────────────────────────────────
        Box(
            modifier         = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(ErrorRedBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint               = ErrorRed,
                modifier           = Modifier.size(36.dp)
            )
        }

        Spacer(Modifier.height(20.dp))

        // ── Title ─────────────────────────────────────────────────────────
        Text(
            text       = "Something went wrong",
            fontSize   = 18.sp,
            fontWeight = FontWeight.Bold,
            color      = textPrimary,
            textAlign  = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        // ── Error detail ──────────────────────────────────────────────────
        Text(
            text       = errorMessage,
            fontSize   = 13.sp,
            color      = MutedGray,
            textAlign  = TextAlign.Center,
            lineHeight = 20.sp,
            modifier   = Modifier.padding(horizontal = 48.dp)
        )

        Spacer(Modifier.height(28.dp))

        // ── Retry button ──────────────────────────────────────────────────
        Button(
            onClick        = onRetry,
            modifier       = Modifier
                .fillMaxWidth(0.55f)
                .height(50.dp),
            shape          = RoundedCornerShape(16.dp),
            colors         = ButtonDefaults.buttonColors(containerColor = Primary),
            contentPadding = PaddingValues(horizontal = 24.dp)
        ) {
            Text(
                text       = "Try again",
                fontSize   = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color      = Color.White
            )
        }
    }
}