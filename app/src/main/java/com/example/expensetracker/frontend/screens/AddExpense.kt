package com.example.expensetracker.frontend.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import com.example.expensetracker.frontend.components.CustomToast
import com.example.expensetracker.frontend.components.ToastMessage
import com.example.expensetracker.frontend.components.ToastType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.expensetracker.frontend.components.BottomNavBar
import com.example.expensetracker.frontend.components.Contact
import com.example.expensetracker.frontend.components.ContactSheet
import com.example.expensetracker.frontend.components.DatePickerModal
import com.example.expensetracker.frontend.services.expenseService.ExpenseViewModel
import com.example.expensetracker.services.entity.ExpenseEntity
import com.example.expensetracker.ui.theme.BackgroundDark
import com.example.expensetracker.ui.theme.BackgroundLight
import com.example.expensetracker.ui.theme.BorderDark
import com.example.expensetracker.ui.theme.BorderLight
import com.example.expensetracker.ui.theme.Primary
import com.example.expensetracker.ui.theme.PrimaryLight
import com.example.expensetracker.ui.theme.SurfaceDark
import com.example.expensetracker.ui.theme.SurfaceLight
import com.example.expensetracker.ui.theme.TextPrimary
import com.example.expensetracker.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

data class Category(
    val label: String,
    val icon: ImageVector,
    val bgColor: Color,
    val iconColor: Color
)

val categories = listOf(
    Category("Food",       Icons.Filled.Restaurant,      Color(0xFF1152D4), Color(0xFFFFFFFF)),
    Category("Transport",  Icons.Filled.DirectionsCar,   Color(0xFFFFEDD5), Color(0xFFEA580C)),
    Category("Shopping",   Icons.Filled.ShoppingBag,     Color(0xFFDCFCE7), Color(0xFF16A34A)),
    Category("Leisure",    Icons.Filled.Movie,           Color(0xFFF3E8FF), Color(0xFF9333EA)),
    Category("Housing",    Icons.Filled.Home,            Color(0xFFDBEAFE), Color(0xFF3B82F6)),
    Category("Health",     Icons.Filled.MedicalServices, Color(0xFFFFE4E6), Color(0xFFE74C3C)),
    Category("Education",  Icons.Filled.School,          Color(0xFFFEF9C3), Color(0xFFD4A017)),
    Category("Other",      Icons.Filled.MoreHoriz,       Color(0xFFF1F5F9), Color(0xFF94A3B8)),
)

private const val MAX_AMOUNT = 200000.0
private val amountRegex = Regex("^\\d*\\.?\\d{0,2}$")

// Maps merchant/description keywords to the fixed 8-category UI grid above, so
// typing "Swiggy" or "Uber" into Notes can auto-suggest the right category
// instead of requiring a manual tap every time.
private val uiCategoryKeywords: LinkedHashMap<String, List<String>> = linkedMapOf(
    "Food" to listOf(
        "swiggy", "zomato", "restaurant", "cafe", "food", "dominos", "pizza",
        "starbucks", "mcdonald", "kfc", "eatsure", "grocery", "groceries",
        "supermarket", "zepto", "blinkit", "instamart", "bigbasket", "dmart"
    ),
    "Transport" to listOf(
        "uber", "ola", "rapido", "irctc", "makemytrip", "goibibo", "indigo",
        "vistara", "airindia", "redbus", "yatra", "petrol", "diesel", "fuel",
        "metro", "bus fare", "cab", "taxi", "parking"
    ),
    "Shopping" to listOf(
        "amazon", "flipkart", "myntra", "ajio", "meesho", "nykaa", "reliance",
        "shop", "mall", "store"
    ),
    "Leisure" to listOf(
        "netflix", "spotify", "primevideo", "hotstar", "bookmyshow", "sonyliv",
        "movie", "cinema", "game", "gaming", "youtube premium"
    ),
    "Housing" to listOf(
        "rent", "nobroker", "housing.com", "electricity", "water bill",
        "maintenance", "broadband", "wifi", "gas", "dth"
    ),
    "Health" to listOf(
        "pharmacy", "apollo", "hospital", "clinic", "medplus", "practo",
        "1mg", "netmeds", "doctor", "medicine", "medical"
    ),
    "Education" to listOf(
        "school", "college", "tuition", "course", "udemy", "coursera",
        "books", "fees", "exam"
    ),
)

/** Best-guess UI category for whatever the user has typed so far (merchant name,
 *  description, etc.), or null if nothing matches — leaves the picker untouched. */
fun suggestUiCategory(text: String): String? {
    if (text.isBlank()) return null
    val lower = text.lowercase()
    for ((category, keywords) in uiCategoryKeywords) {
        if (keywords.any { lower.contains(it) }) return category
    }
    return null
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpense(
    navController: NavController,
    isDark: Boolean = false,
    expenseViewModel: ExpenseViewModel
) {
    val background  = if (isDark) BackgroundDark else BackgroundLight
    val surface     = if (isDark) SurfaceDark    else SurfaceLight
    val border      = if (isDark) BorderDark     else BorderLight
    val textPrimary = if (isDark) Color.White    else TextPrimary

    val uiState by expenseViewModel.uiState.collectAsState()

    var currentTerm       by remember { mutableStateOf("") }   // the term currently being typed
    var previousTerms     by remember { mutableStateOf(listOf<String>()) } // completed terms, joined with "+"
    var showLimitError    by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("") }
    // True while the current selectedCategory came from typing Notes rather than
    // a manual tap — lets auto-suggestion keep updating as they type, but a
    // deliberate tap on a category chip always overrides and "sticks" afterward.
    var categoryAutoPicked by remember { mutableStateOf(false) }
    var notes            by remember { mutableStateOf("") }
    var selectedDate by remember {
        mutableStateOf(SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date()))
    }
    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker   by remember { mutableStateOf(false) }
    var showContactSheet by remember { mutableStateOf(false) }
    var selectedContact  by remember { mutableStateOf<Contact?>(null) }

    var toast by remember { mutableStateOf<ToastMessage?>(null) }
    var addedSuccessfully by remember { mutableStateOf(false) }

    // Live running total = every completed term + whatever's being typed right now
    val totalAmount = previousTerms.sumOf { it.toDoubleOrNull() ?: 0.0 } + (currentTerm.toDoubleOrNull() ?: 0.0)
    // "450+225+180" style expression, shown as a chip once there's more than one term
    val expressionText = if (previousTerms.isNotEmpty()) {
        (previousTerms + listOfNotNull(currentTerm.ifBlank { null })).joinToString("+")
    } else null

    // --- Custom keypad handlers -------------------------------------------------
    fun handleDigit(digit: String) {
        val candidate = if (currentTerm == "0") digit else currentTerm + digit
        if (!candidate.matches(amountRegex)) return
        val prospectiveNumeric = candidate.toDoubleOrNull() ?: 0.0
        val prospectiveTotal   = previousTerms.sumOf { it.toDoubleOrNull() ?: 0.0 } + prospectiveNumeric
        if (prospectiveTotal <= MAX_AMOUNT) {
            currentTerm = candidate
            showLimitError = false
        } else {
            showLimitError = true
        }
    }

    fun handleDecimalPoint() {
        if (currentTerm.contains(".")) return
        currentTerm = if (currentTerm.isEmpty()) "0." else "$currentTerm."
        showLimitError = false
    }

    fun handleBackspace() {
        if (currentTerm.isNotEmpty()) {
            currentTerm = currentTerm.dropLast(1)
            showLimitError = false
        } else if (previousTerms.isNotEmpty()) {
            // Nothing left to erase in the current term — pull the last completed
            // term back out so it can be edited, undoing the last "+".
            currentTerm = previousTerms.last()
            previousTerms = previousTerms.dropLast(1)
        }
    }

    fun handlePlus() {
        val numeric = currentTerm.toDoubleOrNull() ?: 0.0
        if (numeric <= 0.0) return
        previousTerms = previousTerms + currentTerm
        currentTerm = ""
        showLimitError = false
    }

    // --- Submit -------------------------------------------------------------------
    fun submitExpense() {
        val amountDouble = totalAmount
        if (amountDouble <= 0.0) {
            toast = ToastMessage("Please enter a valid amount.", ToastType.ERROR)
            return
        }
        if (selectedCategory.isBlank()) {
            toast = ToastMessage("Please select a category", ToastType.ERROR)
            return
        }

        // Format ISO Date (yyyy-MM-dd)
        val storedDate = Instant.ofEpochMilli(selectedDateMillis)
            .atOffset(ZoneOffset.UTC)
            .toLocalDate()
            .format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH))

        // Get the current system time at creation moment (e.g., "14:30")
        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

        val newExpense = ExpenseEntity(
            tab           = true,
            category      = selectedCategory,
            amount        = amountDouble,
            notes         = notes.ifBlank { null },
            date          = storedDate,
            time          = currentTime, // Automatically captured creation time
            contactName   = selectedContact?.name ?: "",
            contactNumber = selectedContact?.phone ?: "",
            icon          = selectedCategory,
            title         = notes.ifBlank { selectedCategory }
        )

        expenseViewModel.addExpense(newExpense) {
            addedSuccessfully = true
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            toast = ToastMessage(it, ToastType.ERROR)
            expenseViewModel.clearError()
        }
    }

    // Auto-suggest a category from whatever's typed in Notes (merchant/description),
    // as long as nothing was manually picked (or the current pick was itself an
    // earlier auto-suggestion) — a manual tap always takes priority afterward.
    LaunchedEffect(notes) {
        if (selectedCategory.isBlank() || categoryAutoPicked) {
            val suggestion = suggestUiCategory(notes)
            if (suggestion != null) {
                selectedCategory = suggestion
                categoryAutoPicked = true
            }
        }
    }

    LaunchedEffect(addedSuccessfully) {
        if (addedSuccessfully) {
            toast = ToastMessage("Expense added successfully!", ToastType.SUCCESS)
            kotlinx.coroutines.delay(1_500)
            navController.navigateUp()
        }
    }

    if (showDatePicker) {
        DatePickerModal(
            onDateSelected = { date ->
                selectedDate = date
                runCatching {
                    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    selectedDateMillis = sdf.parse(date)?.time ?: System.currentTimeMillis()
                }
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }

    if (showContactSheet) {
        ContactSheet(
            onDismiss = { showContactSheet = false },
            onContactSelected = { contact ->
                selectedContact = contact
                showContactSheet = false
            },
            isDark                 = isDark,
            initialSelectedContact = selectedContact
        )
    }

    Scaffold(
        containerColor   = background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = background),
                title  = {
                    Text(
                        "Add Expense",
                        color      = textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 24.sp
                    )
                },
                actions = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector        = Icons.Default.Close,
                            contentDescription = "Close",
                            tint               = textPrimary,
                            modifier           = Modifier.size(32.dp)
                        )
                    }
                }
            )
        },
        bottomBar = {
            // The sticky action bar (submit button) sits above the numeric keypad,
            // pinned to the bottom regardless of scroll — separated from the keypad
            // itself so the keypad is purely for digit/term entry.
            Column(modifier = Modifier.fillMaxWidth()) {
                StickyActionButton(
                    label     = "Add Expense",
                    onClick   = ::submitExpense,
                    enabled   = !uiState.isLoading,
                    isLoading = uiState.isLoading,
                    isDark    = isDark,
                    surface   = surface,
                    border    = border
                )
                NumericKeypad(
                    onDigitClick     = ::handleDigit,
                    onDecimalClick   = ::handleDecimalPoint,
                    onBackspaceClick = ::handleBackspace,
                    onPlusClick      = ::handlePlus,
                    isDark           = isDark,
                    surface          = surface,
                    border           = border
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                AmountInputField(
                    amount         = totalAmount,
                    isEmpty        = currentTerm.isEmpty() && previousTerms.isEmpty(),
                    expressionText = expressionText,
                    showLimitError = showLimitError,
                    isDark         = isDark
                )

                HorizontalDivider(
                    modifier  = Modifier.padding(horizontal = 24.dp),
                    color     = if (isDark) Color(0xFF2A3347) else Color(0xFFE8E8EE),
                    thickness = 1.dp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Column(
                    modifier            = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CategoryGrid(
                        categories         = categories,
                        selectedCategory   = selectedCategory,
                        onCategorySelected = { selectedCategory = it; categoryAutoPicked = false },
                        textPrimary        = textPrimary
                    )

                    ShowDetails(
                        notes = notes,
                        onNotesChange = { notes = it },
                        selectedDate = selectedDate,
                        onDateClick = { showDatePicker = true },
                        selectedContact = selectedContact,
                        onContactClick = { showContactSheet = true },
                        onContactClear = { selectedContact = null },
                        surface = surface,
                        isDark = isDark,
                        textPrimary = textPrimary
                    )

                    Spacer(Modifier.height(32.dp))
                }
            }
            CustomToast(
                toast     = toast,
                onDismiss = { toast = null },
                modifier  = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = paddingValues.calculateTopPadding() + 8.dp)
            )
        }
    }
}

@Composable
fun AmountInputField(
    amount: Double,
    isEmpty: Boolean,
    expressionText: String? = null,
    showLimitError: Boolean = false,
    isDark: Boolean = false
) {
    val digitColor  = if (isDark) Color.White else Color(0xFF0F172A)
    val hintColor   = if (isDark) Color(0xFF3A4357) else Color(0xFFCDD0DA)
    val labelColor  = if (isDark) Color(0xFF6B7589) else Color(0xFF9AA5B4)
    val chipBg      = if (isDark) Color(0xFF1E2536) else Color(0xFFF1F5F9)
    val chipText    = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)

    val infiniteTransition = rememberInfiniteTransition(label = "cursor_blink")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue  = 1f,
        targetValue   = 0f,
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = 500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursor_alpha"
    )

    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text          = "Enter Amount",
            fontSize      = 13.sp,
            color         = labelColor,
            fontWeight    = FontWeight.Normal,
            letterSpacing = 0.4.sp
        )
        Spacer(Modifier.height(14.dp))

        Box(
            modifier         = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (isEmpty) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = hintColor, fontSize = 34.sp, fontWeight = FontWeight.Bold)) { append("₹") }
                        withStyle(SpanStyle(color = hintColor, fontSize = 58.sp, fontWeight = FontWeight.Bold)) { append("0") }
                        withStyle(SpanStyle(color = Primary.copy(alpha = cursorAlpha), fontSize = 58.sp, fontWeight = FontWeight.Thin)) { append("|") }
                        withStyle(SpanStyle(color = hintColor.copy(alpha = 0.6f), fontSize = 42.sp, fontWeight = FontWeight.Normal)) { append(".00") }
                    },
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = Primary, fontSize = 34.sp, fontWeight = FontWeight.Bold)) { append("₹") }
                        withStyle(SpanStyle(color = digitColor, fontSize = 58.sp, fontWeight = FontWeight.Bold)) { append(formatAmount(amount)) }
                        withStyle(SpanStyle(color = Primary.copy(alpha = cursorAlpha), fontSize = 58.sp, fontWeight = FontWeight.Thin)) { append("|") }
                    },
                    textAlign = TextAlign.Center
                )
            }
        }

        // "450+225+180" style chip — only shown once more than one amount has
        // been chained together with the keypad's "+" key.
        if (!expressionText.isNullOrBlank()) {
            Spacer(Modifier.height(14.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = chipBg
            ) {
                Text(
                    text      = expressionText,
                    fontSize  = 14.sp,
                    color     = chipText,
                    fontWeight = FontWeight.Medium,
                    modifier  = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }

        if (showLimitError) {
            Spacer(Modifier.height(12.dp))
            Text(
                text      = "Maximum amount is ₹2,00,000",
                fontSize  = 12.sp,
                color     = Color(0xFFE53935),
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier  = Modifier.fillMaxWidth()
            )
        }
    }
}

/** Formats a total for display: whole numbers show with no decimals ("855"),
 *  fractional ones show up to 2 decimal places with no trailing zeros ("42.5"). */
private fun formatAmount(value: Double): String {
    val rounded = kotlin.math.round(value * 100) / 100.0
    return if (rounded == rounded.toLong().toDouble()) {
        rounded.toLong().toString()
    } else {
        "%.2f".format(rounded).trimEnd('0').trimEnd('.')
    }
}

/**
 * A sticky, full-width action bar shown pinned above the numeric keypad
 * (and thus above the on-screen system keyboard area entirely). Unlike the
 * old submit button that lived inside the keypad's Surface, this is its own
 * component so it can be reused wherever a "commit" action is needed without
 * dragging the whole keypad along with it.
 */
@Composable
fun StickyActionButton(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    isDark: Boolean = false,
    surface: Color = if (isDark) SurfaceDark else SurfaceLight,
    border: Color = if (isDark) BorderDark else BorderLight
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color    = surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Button(
                onClick  = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Primary),
                enabled  = enabled
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color       = Color.White,
                        modifier    = Modifier.size(20.dp),
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Icon(
                        imageVector        = Icons.Default.Add,
                        contentDescription = null,
                        tint               = Color.White,
                        modifier           = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text       = label,
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color.White
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = border, thickness = 1.dp)
        }
    }
}

/**
 * A custom on-screen numeric keypad used to enter the expense amount, replacing
 * the system keyboard. Renders digits 1-9, a "+" key (chains another term into
 * the running total), a decimal point, 0, and backspace. The submit action now
 * lives separately in StickyActionButton, so this keypad is purely for entry.
 */
@Composable
fun NumericKeypad(
    onDigitClick: (String) -> Unit,
    onDecimalClick: () -> Unit,
    onBackspaceClick: () -> Unit,
    onPlusClick: () -> Unit = {},
    isDark: Boolean = false,
    surface: Color = if (isDark) SurfaceDark else SurfaceLight,
    border: Color = if (isDark) BorderDark else BorderLight
) {
    val keyBg   = if (isDark) Color(0xFF1E2536) else Color.White
    val keyText = if (isDark) Color.White else Color(0xFF0F172A)

    // "plusDot" is a single merged key (left half "+", right half ".") so every
    // row keeps exactly 3 slots and lines up under the digit columns above it.
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("plusDot", "0", "⌫")
    )

    Surface(
        modifier        = Modifier.fillMaxWidth(),
        color           = surface,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            rows.forEach { row ->
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    row.forEach { key ->
                        if (key == "plusDot") {
                            PlusDecimalKey(
                                onPlusClick    = onPlusClick,
                                onDecimalClick = onDecimalClick,
                                keyBg          = keyBg,
                                keyText        = keyText,
                                modifier       = Modifier.weight(1f)
                            )
                        } else {
                            KeypadKey(
                                label     = key,
                                bgColor   = keyBg,
                                textColor = keyText,
                                onClick   = {
                                    when (key) {
                                        "⌫" -> onBackspaceClick()
                                        else -> onDigitClick(key)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * A single key split into two independently-tappable halves: "+" on the left
 * (chains a new term into the running total) and "." on the right (decimal
 * point). Keeping this as one key — rather than two separate grid slots —
 * is what lets the bottom row stay 3-wide and aligned with the digit rows
 * above it, instead of the old 4-wide row that threw off the columns.
 */
@Composable
private fun PlusDecimalKey(
    onPlusClick: () -> Unit,
    onDecimalClick: () -> Unit,
    keyBg: Color,
    keyText: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(horizontal = 6.dp)
            .height(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(keyBg)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .background(Primary.copy(alpha = 0.12f))
                .clickable(
                    indication        = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onPlusClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Default.Add,
                contentDescription = "Add term",
                tint               = Primary,
                modifier           = Modifier.size(20.dp)
            )
        }
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxSize()
                .background(Primary.copy(alpha = 0.25f))
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .clickable(
                    indication        = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onDecimalClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text       = ".",
                fontSize   = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color      = keyText
            )
        }
    }
}

@Composable
private fun KeypadKey(
    label: String,
    bgColor: Color,
    textColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(horizontal = 6.dp)
            .height(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .clickable(
                indication        = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        when (label) {
            "⌫" -> Icon(
                imageVector        = Icons.AutoMirrored.Filled.Backspace,
                contentDescription = "Backspace",
                tint               = textColor,
                modifier           = Modifier.size(22.dp)
            )
            "+" -> Icon(
                imageVector        = Icons.Default.Add,
                contentDescription = "Add term",
                tint               = textColor,
                modifier           = Modifier.size(24.dp)
            )
            else -> Text(
                text       = label,
                fontSize   = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color      = textColor
            )
        }
    }
}

@Composable
fun CategoryGrid(
    categories: List<Category>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    textPrimary: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader("category", textSize = 20)
        val rows = categories.chunked(4)
        rows.forEach { rowItems ->
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                rowItems.forEach { category ->
                    val isSelected = selectedCategory == category.label
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier            = Modifier
                            .weight(1f)
                            .clickable(
                                indication        = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { onCategorySelected(category.label) }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(
                                    color = if (isSelected) Primary else category.bgColor,
                                    shape = RoundedCornerShape(20.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector        = category.icon,
                                contentDescription = category.label,
                                tint               = if (isSelected) Color.White else category.iconColor,
                                modifier           = Modifier.size(30.dp)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text       = category.label,
                            fontSize   = 13.sp,
                            color      = if (isSelected) Primary else textPrimary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            textAlign  = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ShowDetails(
    notes: String,
    onNotesChange: (String) -> Unit,
    selectedDate: String,
    onDateClick: () -> Unit,
    selectedContact: Contact?,
    onContactClick: () -> Unit,
    onContactClear: () -> Unit,
    surface: Color,
    isDark: Boolean,
    textPrimary: Color,
    selectedTime: String = "",
    onTimeClick: () -> Unit = {},
    showTimeField: Boolean = true,
) {
    val cardBg       = if (isDark) surface else Color(0xFFF4F5F7)
    val iconTint     = if (isDark) Color(0xFF8A93A8) else Color(0xFFADB5C7)
    val chevronColor = if (isDark) Color(0xFF6B7280) else Color(0xFFADB5C7)

    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader("details", 20)

        Surface(
            modifier        = Modifier.fillMaxWidth(),
            shape           = RoundedCornerShape(16.dp),
            color           = cardBg,
            shadowElevation = 0.dp,
            tonalElevation  = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onContactClick() }
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector        = Icons.Default.PersonAdd,
                    contentDescription = "Contact",
                    tint               = iconTint,
                    modifier           = Modifier.size(26.dp)
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Contact", fontSize = 13.sp, color = textPrimary, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(2.dp))
                    if (selectedContact != null) {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = selectedContact.initials, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Primary)
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(text = selectedContact.name,  fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
                                Text(text = selectedContact.phone, fontSize = 12.sp, color = TextSecondary)
                            }
                        }
                    } else {
                        Text(text = "Select contact...", fontSize = 15.sp, color = Color(0xFFCDD0DA), fontWeight = FontWeight.Normal)
                    }
                }
                Spacer(Modifier.width(8.dp))
                if (selectedContact != null) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE8E8F0))
                            .clickable { onContactClear() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector        = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint               = Color(0xFF6B7280),
                            modifier           = Modifier.size(14.dp)
                        )
                    }
                } else {
                    Icon(
                        imageVector        = Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint               = chevronColor,
                        modifier           = Modifier.size(22.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Surface(
            modifier        = Modifier.fillMaxWidth(),
            shape           = RoundedCornerShape(16.dp),
            color           = cardBg,
            shadowElevation = 0.dp,
            tonalElevation  = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDateClick() }
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector        = Icons.Filled.CalendarMonth,
                    contentDescription = "Date",
                    tint               = iconTint,
                    modifier           = Modifier.size(26.dp)
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Date", fontSize = 13.sp, color = textPrimary, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(2.dp))
                    Text(text = selectedDate, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
                }
                Icon(
                    imageVector        = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint               = chevronColor,
                    modifier           = Modifier.size(22.dp)
                )
            }
        }


        Spacer(Modifier.height(10.dp))

        Surface(
            modifier        = Modifier.fillMaxWidth(),
            shape           = RoundedCornerShape(16.dp),
            color           = cardBg,
            shadowElevation = 0.dp,
            tonalElevation  = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector        = Icons.Default.Notes,
                    contentDescription = "Notes",
                    tint               = iconTint,
                    modifier           = Modifier
                        .size(26.dp)
                        .padding(top = 2.dp)
                )
                Spacer(Modifier.width(14.dp))
                BasicTextField(
                    value         = notes,
                    onValueChange = onNotesChange,
                    textStyle     = androidx.compose.ui.text.TextStyle(fontSize = 15.sp, color = textPrimary),
                    modifier      = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp),
                    decorationBox = { innerTextField ->
                        Column {
                            Text(text = "Notes", fontSize = 13.sp, color = textPrimary, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(4.dp))
                            Box {
                                if (notes.isEmpty()) {
                                    Text(text = "Add a description...", color = Color(0xFFCDD0DA), fontSize = 15.sp)
                                }
                                innerTextField()
                            }
                        }
                    }
                )
            }
        }
    }
}