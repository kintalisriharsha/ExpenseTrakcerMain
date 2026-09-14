package com.example.expensetracker.frontend.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.EaseOut
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.example.expensetracker.frontend.important.Appstate.isDark
import com.example.expensetracker.ui.theme.TextPrimary
import kotlin.math.min

@Composable
fun CircularProgress(
    value: Float = 72f,
    size: Dp = 200.dp,
    strokeWidth: Dp = 12.dp,
    animated: Boolean = true,
) {
    val targetProgress = value.coerceIn(0f, 100f)

    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = if (animated) tween(durationMillis = 1000, easing = EaseOut) else tween(0),
        label = "circularProgress"
    )

    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isDark) Color.White else TextPrimary

    // ── Threshold-based arc color ──────────────────────────────────────────
    val arcColor = when {
        targetProgress > 100f -> Color(0xFFDC2626)   // exceeded  → red
        targetProgress >= 80f -> Color(0xFFF97316)   // nearly up → orange
        else                  -> Color(0xFF16A34A)   // healthy   → green
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(size)
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = strokeWidth.toPx()
            val diameter = min(this.size.width, this.size.height) - strokePx
            val topLeft = Offset(
                x = (this.size.width - diameter) / 2f,
                y = (this.size.height - diameter) / 2f
            )
            val arcSize = Size(diameter, diameter)
            val sweepAngle = 360f * (animatedProgress / 100f)

            // Background track
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            // Progress arc — color shifts with spend level
            drawArc(
                color = arcColor,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${animatedProgress.toInt()}%",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = arcColor,          // percentage text matches arc
                letterSpacing = (-0.5).sp
            )
            Text(
                text = "USED",
                fontSize = 14.sp,
                color = textColor
            )
        }
    }
}