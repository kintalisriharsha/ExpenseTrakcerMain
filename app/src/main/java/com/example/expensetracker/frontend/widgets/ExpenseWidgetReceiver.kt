package com.example.expensetracker.frontend.widgets

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.expensetracker.MainActivity

// ── Palette ───────────────────────────────────────────────────────────────
private val Blue          = Color(0xFF2563EB)
private val BlueTint      = Color(0xFFEFF4FF)
private val Emerald       = Color(0xFF059669)
private val Violet        = Color(0xFF7C3AED)
private val Red           = Color(0xFFDC2626)
private val Track         = Color(0xFFEDEFF3)
private val InkPrimary    = Color(0xFF1A1D29)
private val InkSecondary  = Color(0xFF8A8F9C)
private val CardWhite     = Color(0xFFFFFFFF)

class ExpenseWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data    = WidgetRepository(context).getExpenseSnapshot()
        val density = context.resources.displayMetrics.density
        provideContent { ExpenseWidgetContent(data, density) }
    }
}

@SuppressLint("RestrictedApi")
@Composable
private fun ExpenseWidgetContent(data: ExpenseWidgetSnapshot, density: Float) {

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(CardWhite)
            .cornerRadius(24.dp)
            .clickable(actionStartActivity<MainActivity>())
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(20.dp)
        ) {

            Text(
                text = "Spending Overview",
                style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ColorProvider(InkPrimary))
            )

            Spacer(modifier = GlanceModifier.height(20.dp))

            // ── Three circles: daily / weekly / monthly ─────────────────
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally
            ) {
                SpendCircle(
                    label   = "Daily",
                    spent   = data.spentToday,
                    limit   = data.dailyLimit,
                    accent  = Blue,
                    density = density,
                    modifier = GlanceModifier.defaultWeight()
                )
                Spacer(modifier = GlanceModifier.width(10.dp))
                SpendCircle(
                    label   = "Weekly",
                    spent   = data.weeklySpent,
                    limit   = data.weeklyBudget,
                    accent  = Emerald,
                    density = density,
                    modifier = GlanceModifier.defaultWeight()
                )
                Spacer(modifier = GlanceModifier.width(10.dp))
                SpendCircle(
                    label   = "Monthly",
                    spent   = data.monthlySpent,
                    limit   = data.monthlyBudget,
                    accent  = Violet,
                    density = density,
                    modifier = GlanceModifier.defaultWeight()
                )
            }

            Spacer(modifier = GlanceModifier.defaultWeight())
            Spacer(modifier = GlanceModifier.height(18.dp))

            // ── Bottom rectangle: latest expense ─────────────────────────
            LatestExpenseCard(data)
        }
    }
}

@SuppressLint("RestrictedApi")
@Composable
private fun SpendCircle(
    label: String,
    spent: Double,
    limit: Double,
    accent: Color,
    density: Float,
    modifier: GlanceModifier = GlanceModifier
) {
    val pct       = if (limit > 0) (spent / limit).toFloat().coerceIn(0f, 1f) else 0f
    val overLimit = limit > 0 && spent > limit
    val ringColor = if (overLimit) Red else accent
    val pctLabel  = if (limit > 0) "${(pct * 100).toInt()}%" else "—"

    val ringBitmap = drawProgressRing(
        percent       = pct,
        sizeDp        = 96,
        strokeWidthDp = 9,
        trackColor    = Track.toArgb(),
        progressColor = ringColor.toArgb(),
        density       = density
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally
    ) {
        Box(modifier = GlanceModifier.size(96.dp), contentAlignment = Alignment.Center) {
            Image(provider = ImageProvider(ringBitmap), contentDescription = "$label spend progress")
            Column(horizontalAlignment = Alignment.Horizontal.CenterHorizontally) {
                Text(
                    text = pctLabel,
                    style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ColorProvider(ringColor))
                )
                Spacer(modifier = GlanceModifier.height(3.dp))
                Text(
                    text = "₹${"%.0f".format(spent)}",
                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ColorProvider(InkPrimary))
                )
            }
        }
        Spacer(modifier = GlanceModifier.height(8.dp))
        Text(
            text = label,
            style = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = ColorProvider(InkSecondary),
                textAlign = TextAlign.Center
            )
        )
        Spacer(modifier = GlanceModifier.height(2.dp))
        Text(
            text = "of ₹${"%.0f".format(limit)}",
            style = TextStyle(
                fontSize = 14.sp,
                color = ColorProvider(InkSecondary),
                textAlign = TextAlign.Center
            )
        )
    }
}

@SuppressLint("RestrictedApi")
@Composable
private fun LatestExpenseCard(data: ExpenseWidgetSnapshot) {
    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(BlueTint)
            .cornerRadius(16.dp)
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = "Latest expense",
                    style = TextStyle(fontSize = 16.sp, color = ColorProvider(InkSecondary))
                )
                Spacer(modifier = GlanceModifier.height(3.dp))
                Text(
                    text = data.latestExpenseLabel,
                    style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ColorProvider(InkPrimary)),
                    maxLines = 1
                )
            }
            Spacer(modifier = GlanceModifier.width(14.dp))
            Text(
                text = "₹${"%.0f".format(data.latestExpenseAmount)}",
                style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ColorProvider(Blue))
            )
        }
    }
}

/**
 * Renders a circular progress ring to a Bitmap using plain android.graphics —
 * Glance's composable set has no Canvas/vector drawing, so this is the standard
 * way to get a custom ring into a widget (shown afterwards as a Glance Image).
 */
private fun drawProgressRing(
    percent: Float,
    sizeDp: Int,
    strokeWidthDp: Int,
    trackColor: Int,
    progressColor: Int,
    density: Float
): Bitmap {
    val sizePx   = (sizeDp * density).toInt()
    val strokePx = strokeWidthDp * density

    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = trackColor
        style = Paint.Style.STROKE
        strokeWidth = strokePx
        strokeCap = Paint.Cap.ROUND
    }
    val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = progressColor
        style = Paint.Style.STROKE
        strokeWidth = strokePx
        strokeCap = Paint.Cap.ROUND
    }

    val inset = strokePx / 2f
    val rect  = RectF(inset, inset, sizePx - inset, sizePx - inset)

    canvas.drawArc(rect, 0f, 360f, false, trackPaint)
    canvas.drawArc(rect, -90f, 360f * percent, false, progressPaint)

    return bitmap
}

private fun Color.toArgb(): Int {
    return android.graphics.Color.argb(
        (alpha * 255).toInt(),
        (red * 255).toInt(),
        (green * 255).toInt(),
        (blue * 255).toInt()
    )
}

class ExpenseWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ExpenseWidget()
}