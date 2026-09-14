package com.example.expensetracker.frontend.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn              // ← NEW
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration             // ← NEW
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import com.example.expensetracker.R
import com.example.expensetracker.ui.theme.BackgroundDark
import com.example.expensetracker.ui.theme.BackgroundLight
import com.example.expensetracker.ui.theme.BorderDark
import com.example.expensetracker.ui.theme.BorderLight
import com.example.expensetracker.ui.theme.SurfaceDark
import com.example.expensetracker.ui.theme.SurfaceLight
import com.example.expensetracker.ui.theme.TextPrimary
import kotlinx.coroutines.launch
import kotlin.collections.lastIndex
import kotlin.math.absoluteValue


// ─── Data model ───────────────────────────────────────────────────────────────

data class ActionCard(
    val title: String,
    val subtitle: String,
    val containerColor: Color,
    val icon: ImageVector,
    val route: String,
    val backgroundImage: Int
)


// ─── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun ActionCardPickerScreen(
    navController: NavController,
    isDark: Boolean = false
) {
    val background = if (isDark) BackgroundDark else BackgroundLight

    val cards = listOf(
        ActionCard(
            "Add Expense",
            "Track your daily spending",
            Color(0xFF1152D4),
            Icons.Default.Payments,
            "add_expense",
            R.drawable.expense_bg
        ),
        ActionCard(
            "Todo",
            "Plan your week ahead",
            Color(0xFF7C3AED),
            Icons.Default.CalendarMonth,
            "todo",
            R.drawable.budget_bg
        ),
        ActionCard(
            "Analytics",
            "Get Detailed Analysis of your spending's",
            Color(0xFFDEA404),
            Icons.Default.Analytics,
            "analytics",
            R.drawable.analytics_img
        )
    )

    // ── Fold / tablet adaptivity ──────────────────────────────────────────
    val screenWidthDp   = LocalConfiguration.current.screenWidthDp
    val isWide          = screenWidthDp >= 600
    // Cap the card stack width so it stays portrait-proportioned on foldables.
    // 460 dp gives a natural ~9:16-ish feel; the rest of the screen stays as
    // the plain background colour behind it.
    val contentMaxWidth = if (isWide) 460.dp else Int.MAX_VALUE.dp

    DisposableEffect(Unit) {
        com.example.expensetracker.frontend.important.Appstate.isPickerOpen = true
        onDispose { com.example.expensetracker.frontend.important.Appstate.isPickerOpen = false }
    }

    Scaffold(
        containerColor = background,
        bottomBar = {
            BottomNavBar(
                selectedTab  = 2,
                navController = navController,
                surface      = if (isDark) SurfaceDark else SurfaceLight,
                border       = if (isDark) BorderDark else BorderLight,
                textPrimary  = if (isDark) Color.White else TextPrimary,
                isDark       = isDark
            )
        }
    ) { paddingValues ->
        // Full-size background
        Box(
            modifier         = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(background),
            contentAlignment = Alignment.TopCenter   // centre the card column horizontally
        ) {
            // ── Width-constrained wrapper ─────────────────────────────────
            // On wide screens this Box caps to 460 dp and stays centred.
            // On narrow phones it fills the full width as before.
            Box(
                modifier         = Modifier
                    .widthIn(max = contentMaxWidth)
                    .fillMaxHeight(),
                contentAlignment = Alignment.TopCenter
            ) {
                SwipeableCardStack(
                    cards          = cards,
                    onCardSelected = { route ->
                        navController.navigate(route) { launchSingleTop = true }
                    }
                )
            }
        }
    }
}


// ─── Swipeable card stack ─────────────────────────────────────────────────────

@Composable
fun SwipeableCardStack(
    cards: List<ActionCard>,
    onCardSelected: (String) -> Unit
) {
    val scope        = rememberCoroutineScope()
    var activeIndex  by remember { mutableIntStateOf(0) }
    val dragOffset   = remember { Animatable(0f) }
    var dragProgress by remember { mutableFloatStateOf(0f) }

    fun next() {
        scope.launch {
            dragOffset.snapTo(0f)
            dragProgress = 0f
            activeIndex  = (activeIndex + 1) % cards.size
        }
    }

    fun prev() {
        scope.launch {
            dragOffset.snapTo(0f)
            dragProgress = 0f
            activeIndex  = (activeIndex - 1 + cards.size) % cards.size
        }
    }

    Column(
        modifier            = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // ── Card stack area ───────────────────────────────────────────────
        Box(
            modifier         = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.78f)
                .padding(top = 100.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            val drawOrder = cards.indices.sortedByDescending { i ->
                ((i - activeIndex) + cards.size) % cards.size
            }

            drawOrder.forEach { index ->
                val dist    = ((index - activeIndex) + cards.size) % cards.size
                val isFront = dist == 0
                val card    = cards[index]

                val restingTranslateY = when (dist) {
                    0    ->   0f
                    1    -> -100f
                    else -> -180f
                }
                val restingScale = when (dist) {
                    0    -> 1.00f
                    1    -> 0.93f
                    else -> 0.86f
                }

                val promotedTranslateY = when (dist) {
                    0    ->   0f
                    1    -> -80f
                    else -> -145f
                }
                val promotedScale = when (dist) {
                    0    -> 1.00f
                    1    -> 0.95f
                    else -> 0.90f
                }

                val targetTranslateY = if (!isFront)
                    lerp(restingTranslateY, promotedTranslateY, dragProgress)
                else restingTranslateY

                val targetScale = if (!isFront)
                    lerp(restingScale, promotedScale, dragProgress)
                else restingScale

                val animScale by animateFloatAsState(
                    targetValue   = targetScale,
                    animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
                    label         = "scale_$index"
                )
                val animY by animateFloatAsState(
                    targetValue   = targetTranslateY,
                    animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
                    label         = "ty_$index"
                )

                val overlayAlpha = when (dist) {
                    1    -> lerp(0.38f, 0f, dragProgress)
                    2    -> lerp(0.52f, 0.38f, dragProgress)
                    else -> 0f
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .zIndex(if (isFront) 10f else (cards.size - dist).toFloat())
                        .graphicsLayer {
                            scaleX = animScale
                            scaleY = animScale
                            translationY = animY
                            if (isFront) {
                                translationX = dragOffset.value
                                rotationZ    = dragOffset.value * 0.03f
                            }
                        }
                        .then(
                            if (isFront) Modifier.pointerInput(activeIndex) {
                                val vt = VelocityTracker()
                                detectHorizontalDragGestures(
                                    onDragEnd = {
                                        val velocity = vt.calculateVelocity().x
                                        scope.launch {
                                            when {
                                                dragOffset.value < -120f || velocity < -800f -> {
                                                    dragOffset.animateTo(-900f, tween(280))
                                                    next()
                                                }
                                                dragOffset.value > 120f || velocity > 800f -> {
                                                    dragOffset.animateTo(900f, tween(280))
                                                    prev()
                                                }
                                                else -> {
                                                    dragOffset.animateTo(0f, spring(Spring.DampingRatioMediumBouncy))
                                                    dragProgress = 0f
                                                }
                                            }
                                        }
                                    },
                                    onDragCancel = {
                                        scope.launch {
                                            dragOffset.animateTo(0f, spring())
                                            dragProgress = 0f
                                        }
                                    },
                                    onHorizontalDrag = { change, delta ->
                                        vt.addPosition(change.uptimeMillis, change.position)
                                        scope.launch { dragOffset.snapTo(dragOffset.value + delta) }
                                        dragProgress = (dragOffset.value.absoluteValue / 150f).coerceIn(0f, 1f)
                                    }
                                )
                            } else Modifier
                        )
                        .clickable(
                            indication        = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            if (!isFront) { activeIndex = index; dragProgress = 0f }
                            else onCardSelected(card.route)
                        },
                    shape           = RoundedCornerShape(28.dp),
                    color           = Color.Transparent,
                    shadowElevation = if (isFront) 20.dp else (4 - dist).coerceAtLeast(1).dp,
                    tonalElevation  = 0.dp
                ) {
                    if (isFront) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Image(
                                painter            = painterResource(id = card.backgroundImage),
                                contentDescription = null,
                                contentScale       = ContentScale.Crop,
                                modifier           = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.25f),
                                                Color.Black.copy(alpha = 0.82f)
                                            )
                                        )
                                    )
                            )
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 20.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(card.containerColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector        = card.icon,
                                        contentDescription = null,
                                        tint               = Color.White,
                                        modifier           = Modifier.size(30.dp)
                                    )
                                }
                                Text(
                                    text       = card.title,
                                    fontSize   = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color      = Color.White
                                )
                                Text(
                                    text     = card.subtitle,
                                    fontSize = 14.sp,
                                    color    = Color.White.copy(alpha = 0.75f)
                                )
                                HorizontalDivider(color = Color.White.copy(alpha = 0.20f), thickness = 1.dp)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(
                                            indication        = null,
                                            interactionSource = remember { MutableInteractionSource() }
                                        ) { onCardSelected(card.route) },
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment     = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text          = "SELECT ACTION",
                                        fontSize      = 12.sp,
                                        fontWeight    = FontWeight.Bold,
                                        color         = Color.White,
                                        letterSpacing = 1.5.sp
                                    )
                                    Icon(
                                        imageVector        = Icons.Default.ArrowForward,
                                        contentDescription = null,
                                        tint               = Color.White,
                                        modifier           = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        // Back card — image + animated dark overlay
                        Box(modifier = Modifier.fillMaxSize()) {
                            Image(
                                painter            = painterResource(id = card.backgroundImage),
                                contentDescription = null,
                                contentScale       = ContentScale.Crop,
                                modifier           = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = overlayAlpha))
                            )
                        }
                    }
                }
            }
        } // end card-stack Box

        Spacer(Modifier.height(16.dp))

        // ── Prev / Next navigation buttons ────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable { prev() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = Icons.Default.ArrowBack,
                    contentDescription = "Previous",
                    tint               = Color(0xFF1152D4),
                    modifier           = Modifier.size(26.dp)
                )
            }

            Spacer(Modifier.width(32.dp))

            // Dot indicators
            cards.indices.forEach { i ->
                val isActive = i == activeIndex
                Box(
                    modifier = Modifier
                        .size(if (isActive) 10.dp else 7.dp)
                        .clip(CircleShape)
                        .background(
                            if (isActive) Color(0xFF1152D4)
                            else Color(0xFF1152D4).copy(alpha = 0.25f)
                        )
                )
                if (i < cards.lastIndex) Spacer(Modifier.width(6.dp))
            }

            Spacer(Modifier.width(32.dp))

            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1152D4))
                    .clickable { next() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = Icons.Default.ArrowForward,
                    contentDescription = "Next",
                    tint               = Color.White,
                    modifier           = Modifier.size(26.dp)
                )
            }
        }
    }
}


// ─── Lerp helper ─────────────────────────────────────────────────────────────

private fun lerp(start: Float, end: Float, fraction: Float): Float =
    start + (end - start) * fraction