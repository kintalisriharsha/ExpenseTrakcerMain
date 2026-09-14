package com.example.expensetracker.frontend.widgets

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
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
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.expensetracker.MainActivity
import com.example.expensetracker.frontend.services.TodoService.TodoEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val MAX_TODOS_IN_WIDGET = 3

// ── Palette ──────────────────────────────────────────────────────────────────
private val CardWhite     = Color(0xFFFFFFFF)
private val InkPrimary    = Color(0xFF111111)   // "outside" text — black
private val InkSecondary  = Color(0xFF6B7280)
private val IconWhite     = Color(0xFFFFFFFF)   // "inside" icon-circle text — white
private val FallbackTint  = Color(0xFF4F46E5)   // used only when a todo has no category
private val Green         = Color(0xFF16A34A)
private val GreenTint     = Color(0xFFDCFCE7)
private val Amber         = Color(0xFFB45309)
private val AmberTint     = Color(0xFFFEF3C7)
private val RowBorder     = Color(0xFFE5E7EB)   // curved border color around each todo row
private val CountPillBg   = Color(0xFFF3F4F6)

// ── Sizing ───────────────────────────────────────────────────────────────────
private object Dim {
    val CardCorner        = 24.dp
    val CardPadding       = 20.dp
    val TitleBottomSpace  = 18.dp
    val RowGap            = 12.dp
    val RowPaddingH       = 12.dp
    val RowPaddingV       = 10.dp
    val RowBorderWidth    = 1.5.dp
    val RowCorner         = 16.dp
    val AvatarSize        = 48.dp   // color lives ONLY inside this circle
    val AvatarCorner      = 24.dp   // half of AvatarSize -> perfect circle
    val PillPaddingH      = 12.dp
    val PillPaddingV      = 5.dp
    val PillCorner        = 10.dp
    val CountPillPaddingH = 10.dp
    val CountPillPaddingV = 4.dp
    val CountPillCorner   = 8.dp
}

class TodoWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val todos = WidgetRepository(context).getTodoSnapshot(MAX_TODOS_IN_WIDGET)
        provideContent { TodoWidgetContent(todos) }
    }
}

@SuppressLint("RestrictedApi")
@Composable
private fun TodoWidgetContent(todos: List<TodoEntity>) {
    val dateLabel = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date()) }
    val visibleTodos = todos.take(MAX_TODOS_IN_WIDGET)
    val doneCount = visibleTodos.count { it.checkBox }

    // Outer box stays fully transparent and fills the widget's grid allotment.
    // The card itself only wraps its own content, so it never stretches past
    // "header + up to 3 rows" — no leftover empty space underneath.
    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.TopStart
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(CardWhite)
                .cornerRadius(Dim.CardCorner)
                .clickable(actionStartActivity<MainActivity>())
                .padding(Dim.CardPadding)
        ) {
            // ── Title row: label+date on the left, "done/total" count pill on the right ──
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Text(
                    text = "Todo - $dateLabel",
                    style = TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        color = ColorProvider(InkPrimary)
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )
                if (visibleTodos.isNotEmpty()) {
                    Spacer(modifier = GlanceModifier.width(8.dp))
                    Box(
                        modifier = GlanceModifier
                            .background(CountPillBg)
                            .cornerRadius(Dim.CountPillCorner)
                            .padding(horizontal = Dim.CountPillPaddingH, vertical = Dim.CountPillPaddingV)
                    ) {
                        Text(
                            text = "$doneCount/${visibleTodos.size}",
                            style = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif,
                                color = ColorProvider(InkSecondary)
                            )
                        )
                    }
                }
            }
            Spacer(modifier = GlanceModifier.height(Dim.TitleBottomSpace))

            if (visibleTodos.isEmpty()) {
                Text(
                    text = "Nothing on your list 🎉",
                    style = TextStyle(
                        fontSize = 15.sp,
                        fontFamily = FontFamily.SansSerif,
                        color = ColorProvider(InkSecondary)
                    )
                )
            } else {
                visibleTodos.forEachIndexed { index, todo ->
                    TodoWidgetRow(todo)
                    if (index != visibleTodos.lastIndex) {
                        Spacer(modifier = GlanceModifier.height(Dim.RowGap))
                    }
                }
            }
        }
    }
}

@SuppressLint("RestrictedApi")
@Composable
private fun TodoWidgetRow(todo: TodoEntity) {
    // Glance has no native border modifier, so the curved border is faked with
    // two nested boxes: the outer box IS the border (its background shows only
    // as a thin ring because the inner box, inset by RowBorderWidth, covers the
    // rest with the card's white). Both share the same corner radius so the
    // ring reads as a clean rounded outline, not a rectangle behind a circle.
    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(RowBorder)
            .cornerRadius(Dim.RowCorner)
            .padding(Dim.RowBorderWidth)
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(CardWhite)
                .cornerRadius(Dim.RowCorner)
        ) {
            TodoWidgetRowContent(todo)
        }
    }
}

@SuppressLint("RestrictedApi")
@Composable
private fun TodoWidgetRowContent(todo: TodoEntity) {
    val category = todo.categoryEnum
    // Color lives ONLY inside the icon circle — the row itself stays white
    // (no background here) so the card reads as one clean white surface.
    val circleColor = category?.color ?: if (todo.checkBox) Green else FallbackTint
    val glyph = category?.emoji ?: if (todo.checkBox) "✓" else "•"

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(horizontal = Dim.RowPaddingH, vertical = Dim.RowPaddingV),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        // ── Icon-only avatar — the ONLY place category color appears ──
        Box(
            modifier = GlanceModifier
                .size(Dim.AvatarSize)
                .background(circleColor)
                .cornerRadius(Dim.AvatarCorner),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = glyph,
                style = TextStyle(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    color = ColorProvider(IconWhite)
                )
            )
        }

        Spacer(modifier = GlanceModifier.width(14.dp))

        // ── Name + amount — black/gray, on the white row ──
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = todo.Name,
                maxLines = 1,
                style = TextStyle(
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    color = ColorProvider(if (todo.checkBox) InkSecondary else InkPrimary),
                    textDecoration = if (todo.checkBox) TextDecoration.LineThrough else TextDecoration.None
                )
            )
            Spacer(modifier = GlanceModifier.height(2.dp))
            // ── Cost and quantity shown side by side ──
            Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                Text(
                    text = "₹${"%.0f".format(todo.Amount)}",
                    maxLines = 1,
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.SansSerif,
                        color = ColorProvider(InkSecondary)
                    )
                )
                if (todo.quantity > 1) {
                    Spacer(modifier = GlanceModifier.width(6.dp))
                    Text(
                        text = "· Qty ${todo.quantity}",
                        maxLines = 1,
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.SansSerif,
                            color = ColorProvider(InkSecondary)
                        )
                    )
                }
            }
        }

        Spacer(modifier = GlanceModifier.width(10.dp))

        // ── Status pill — Done / Pending ──
        Box(
            modifier = GlanceModifier
                .background(if (todo.checkBox) GreenTint else AmberTint)
                .cornerRadius(Dim.PillCorner)
                .padding(horizontal = Dim.PillPaddingH, vertical = Dim.PillPaddingV)
        ) {
            Text(
                text = if (todo.checkBox) "Done" else "Pending",
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    color = ColorProvider(if (todo.checkBox) Green else Amber)
                )
            )
        }
    }
}

class TodoWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodoWidget()
}