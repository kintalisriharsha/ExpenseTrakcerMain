package com.example.expensetracker.frontend.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.expensetracker.frontend.services.TodoService.TodoViewModel
import com.example.expensetracker.frontend.services.expenseService.ExpenseViewModel
import com.example.expensetracker.ui.theme.BackgroundDark
import com.example.expensetracker.ui.theme.Blue
import com.example.expensetracker.ui.theme.BlueDark
import com.example.expensetracker.ui.theme.BlueLight
import com.example.expensetracker.ui.theme.BorderDark
import com.example.expensetracker.ui.theme.BorderLight
import com.example.expensetracker.ui.theme.Green
import com.example.expensetracker.ui.theme.GreenDark
import com.example.expensetracker.ui.theme.GreenLight
import com.example.expensetracker.ui.theme.Orange
import com.example.expensetracker.ui.theme.OrangeDark
import com.example.expensetracker.ui.theme.OrangeLight
import com.example.expensetracker.ui.theme.Primary
import com.example.expensetracker.ui.theme.PrimaryLight
import com.example.expensetracker.ui.theme.SurfaceDark
import com.example.expensetracker.ui.theme.TextPrimary
import com.example.expensetracker.ui.theme.TextSecondary

// ─── A unified result item that works for both Expense and Todo results ─────

data class SearchResultItem(
    val id         : String,
    val title      : String,   // expense title or todo item name
    val subtitle   : String,   // expense date/time or todo category details
    val amount     : Double,
    val amountLabel: String,   // "-₹x.xx" or "₹x.xx"
    val amountColor: Color,
    val category   : String    // used to pick icon + colors
)

// ─── Category meta (icon + colours keyed by category name) ──────────────────

private data class CategoryMeta(
    val icon    : ImageVector,
    val bgLight : Color,
    val bgDark  : Color,
    val tint    : Color
)

private fun categoryMeta(category: String): CategoryMeta = when (category.lowercase()) {
    "food"        -> CategoryMeta(Icons.Outlined.Restaurant,   OrangeLight,        OrangeDark,        Orange)
    "transport"   -> CategoryMeta(Icons.Outlined.DirectionsCar,BlueLight,          BlueDark,          Blue)
    "shopping"    -> CategoryMeta(Icons.Outlined.ShoppingBag,  GreenLight,         GreenDark,         Green)
    "leisure"     -> CategoryMeta(Icons.Outlined.Movie,        BlueLight,          BlueDark,          Blue)
    "housing"     -> CategoryMeta(Icons.Outlined.Home,         Color(0xFFDBEAFE),  Color(0xFF1E2A40), Color(0xFF3B82F6))
    "health"      -> CategoryMeta(Icons.Outlined.MedicalServices, Color(0xFFFFE4E6), Color(0xFF3D1A1D), Color(0xFFE74C3C))
    "education"   -> CategoryMeta(Icons.Outlined.School,       Color(0xFFFEF9C3),  Color(0xFF2D2A0A), Color(0xFFD4A017))
    "todo"        -> CategoryMeta(Icons.Outlined.CheckCircle, Color(0xFFDCFCE7),  Color(0xFF0F2A1A), Color(0xFF16A34A))
    else          -> CategoryMeta(Icons.Outlined.MoreHoriz,    Color(0xFFF1F5F9),  Color(0xFF1E2230), Color(0xFF94A3B8))
}

// ─── Available filter categories ─────────────────────────────────────────────

private val expenseFilterCategories = listOf(
    "Food", "Transport", "Shopping", "Leisure", "Housing", "Health", "Education", "Other"
)

// ─── Dialog ──────────────────────────────────────────────────────────────────

/**
 * @param selectedTab   0 = Expense History, 1 = Todo History
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchDialog(
    onDismiss        : () -> Unit,
    isDark           : Boolean = isSystemInDarkTheme(),
    expenseViewModel : ExpenseViewModel,
    todoViewModel    : TodoViewModel,
    selectedTab      : Int = 0          // 0 = Expense, 1 = Todo
) {
    val isExpenseMode = selectedTab == 0

    val background  = if (isDark) BackgroundDark else Color.White
    val surface     = if (isDark) SurfaceDark    else Color(0xFFF4F5F7)
    val border      = if (isDark) BorderDark     else BorderLight
    val textPrimary = if (isDark) Color.White    else TextPrimary

    var query            by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    var results by remember { mutableStateOf<List<SearchResultItem>?>(null) }
    var total   by remember { mutableStateOf(0) }

    // Only true once the user actually taps "Show Results". The dialog stays on
    // the search form (with the text field) until this flips to true — otherwise
    // the LaunchedEffects below fire on first composition (StateFlows already
    // have an initial value) and jump straight to the Results screen.
    var hasSearched by remember { mutableStateOf(false) }

    // Shown under the search field when the user tries to search with nothing typed.
    var searchError by remember { mutableStateOf<String?>(null) }

    // Force a clean slate every time this dialog is opened, regardless of how it
    // was left last time — guarantees the search icon always lands on the form,
    // never on stale results.
    LaunchedEffect(Unit) {
        hasSearched = false
        results = null
        total = 0
        searchError = null
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    // ── Collect ViewModel states ──────────────────────────────────────────────
    val expenseSearchResults by expenseViewModel.searchResultsSimple.collectAsState()
    val isExpenseSearching by expenseViewModel.isSearching.collectAsState()
    val todoSearchResults by todoViewModel.searchResults.collectAsState()

    val isLoading = if (isExpenseMode) isExpenseSearching else false

    // Map expense search result -> unified items
    LaunchedEffect(expenseSearchResults) {
        if (isExpenseMode && hasSearched) {
            val expenses = expenseSearchResults
            results = expenses.map { exp ->
                SearchResultItem(
                    id          = exp.id.toString(),
                    title       = exp.notes?.ifBlank { exp.category } ?: exp.category,
                    subtitle    = "${exp.date}  •  ${exp.time}",
                    amount      = exp.amount,
                    amountLabel = "-₹${"%.2f".format(exp.amount)}",
                    amountColor = Color(0xFFDC2626),
                    category    = exp.category
                )
            }
            total = expenses.size
        }
    }

    // Map todo search result -> unified items
    LaunchedEffect(todoSearchResults) {
        if (!isExpenseMode && hasSearched) {
            results = todoSearchResults.map { todo ->
                SearchResultItem(
                    id          = todo.id.toString(),
                    title       = todo.Name,
                    subtitle    = if (todo.checkBox) "Completed" else "Pending",
                    amount      = todo.Amount,
                    amountLabel = "₹${"%.2f".format(todo.Amount)}",
                    amountColor = if (todo.checkBox) Color(0xFF16A34A) else Color(0xFFF59E0B),
                    category    = "todo"
                )
            }
            total = todoSearchResults.size
        }
    }

    // ── Helper: fire the correct search ──────────────────────────────────────
    fun runSearch() {
        val trimmedQuery = query.trim()
        val nothingToSearch = trimmedQuery.isEmpty() && (!isExpenseMode || selectedCategory == null)

        if (nothingToSearch) {
            searchError = "Nothing to search — type a keyword first"
            return
        }

        searchError = null
        hasSearched = true
        if (isExpenseMode) {
            expenseViewModel.unifiedSearchSimple(
                q        = trimmedQuery,
                category = selectedCategory ?: ""
            )
        } else {
            todoViewModel.onSearchQueryChanged(trimmedQuery)
        }
    }

    // ── Dialog ────────────────────────────────────────────────────────────────
    Dialog(
        onDismissRequest = {
            expenseViewModel.clearError()
            onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress      = true,
            dismissOnClickOutside   = true
        )
    ) {
        Box(
            modifier         = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable {
                    expenseViewModel.clearError()
                    onDismiss()
                },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clickable(enabled = false) {},
                shape           = RoundedCornerShape(20.dp),
                color           = background,
                shadowElevation = 20.dp,
                tonalElevation  = 0.dp
            ) {
                Column {

                    // ── Title Row ────────────────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick  = {
                                if (hasSearched) {
                                    hasSearched = false
                                    results = null
                                } else {
                                    onDismiss()
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector        = Icons.Default.Close,
                                contentDescription = "Close",
                                tint               = textPrimary,
                                modifier           = Modifier.size(20.dp)
                            )
                        }

                        Text(
                            text = when {
                                hasSearched      -> "Results"
                                isExpenseMode    -> "Search Expenses"
                                else             -> "Search Todos"
                            },
                            modifier   = Modifier.weight(1f),
                            textAlign  = TextAlign.Center,
                            fontSize   = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color      = textPrimary
                        )

                        TextButton(onClick = { onDismiss() }) {
                            Text(
                                text       = "Cancel",
                                color      = Primary,
                                fontSize   = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    HorizontalDivider(color = border)

                    if (!hasSearched) {

                        // ── Search Form ──────────────────────────────────────

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(surface)
                                .padding(horizontal = 14.dp, vertical = 13.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector        = Icons.Default.Search,
                                    contentDescription = null,
                                    tint               = TextSecondary,
                                    modifier           = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Box(modifier = Modifier.weight(1f)) {
                                    if (query.isEmpty()) {
                                        Text(
                                            text = if (isExpenseMode)
                                                "Search transactions, notes, category…"
                                            else
                                                "Search todo items…",
                                            color    = TextSecondary,
                                            fontSize = 15.sp
                                        )
                                    }
                                    BasicTextField(
                                        value         = query,
                                        onValueChange = {
                                            query = it
                                            if (searchError != null) searchError = null
                                        },
                                        singleLine    = true,
                                        textStyle     = TextStyle(color = textPrimary, fontSize = 15.sp),
                                        cursorBrush   = SolidColor(Primary),
                                        modifier      = Modifier
                                            .fillMaxWidth()
                                            .focusRequester(focusRequester)
                                    )
                                }
                                if (query.isNotEmpty()) {
                                    Spacer(Modifier.width(8.dp))
                                    IconButton(
                                        onClick  = { query = "" },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            imageVector        = Icons.Default.Close,
                                            contentDescription = "Clear",
                                            tint               = TextSecondary,
                                            modifier           = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (searchError != null) {
                            Text(
                                text     = searchError!!,
                                color    = Color(0xFFDC2626),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(start = 30.dp, end = 16.dp, top = 0.dp, bottom = 10.dp)
                            )
                        }

                        HorizontalDivider(color = border)

                        if (isExpenseMode) {
                            val chips = expenseFilterCategories
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(surface)
                                    .padding(horizontal = 16.dp, vertical = 16.dp)
                            ) {
                                Text(
                                    text          = "FILTER BY CATEGORY",
                                    fontSize      = 12.sp,
                                    fontWeight    = FontWeight.Bold,
                                    color         = textPrimary,
                                    letterSpacing = 0.8.sp
                                )
                                Spacer(Modifier.height(14.dp))
                                chips.chunked(4).forEach { rowItems ->
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier              = Modifier.padding(bottom = 8.dp)
                                    ) {
                                        rowItems.forEach { category ->
                                            val isSelected = selectedCategory == category
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(50))
                                                    .background(
                                                        if (isSelected) Primary
                                                        else if (isDark) SurfaceDark else Color(0xFFE8ECF0)
                                                    )
                                                    .clickable {
                                                        selectedCategory =
                                                            if (isSelected) null else category
                                                        if (searchError != null) searchError = null
                                                    }
                                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                                            ) {
                                                Text(
                                                    text       = category,
                                                    fontSize   = 13.sp,
                                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                                    color      = if (isSelected) Color.White else textPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            HorizontalDivider(color = border)
                        }

                        // Show Results button
                        Box(modifier = Modifier.padding(16.dp)) {
                            Button(
                                onClick  = { runSearch() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape    = RoundedCornerShape(14.dp),
                                colors   = ButtonDefaults.buttonColors(containerColor = Primary),
                                enabled  = !isLoading
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        color       = Color.White,
                                        modifier    = Modifier.size(20.dp),
                                        strokeWidth = 2.5.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector        = Icons.Default.Search,
                                        contentDescription = null,
                                        modifier           = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text       = "Show Results",
                                        fontSize   = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                    } else {

                        // ── Results ──────────────────────────────────────────

                        val resultList = results ?: emptyList()

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Text(
                                text     = "$total result${if (total != 1) "s" else ""} found",
                                fontSize = 13.sp,
                                color    = TextSecondary
                            )
                            if (selectedCategory != null) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(PrimaryLight)
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text       = selectedCategory!!,
                                        fontSize   = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color      = Primary
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = border)

                        if (resultList.isEmpty()) {
                            Column(
                                modifier            = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier         = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector        = Icons.Default.Search,
                                        contentDescription = null,
                                        tint               = Primary,
                                        modifier           = Modifier.size(26.dp)
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "No results found",
                                    fontSize   = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color      = textPrimary
                                )
                                Text(
                                    "Try a different keyword or category",
                                    fontSize = 13.sp,
                                    color    = TextSecondary
                                )
                            }
                        } else {
                            LazyColumn(
                                contentPadding      = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier            = Modifier.heightIn(max = 420.dp)
                            ) {
                                items(resultList, key = { it.id }) { item ->
                                    UnifiedSearchResultRow(
                                        item        = item,
                                        isDark      = isDark,
                                        surface     = if (isDark) SurfaceDark else Color(0xFFF4F5F7),
                                        textPrimary = textPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Unified Result Row ───────────────────────────────────────────────────────

@Composable
private fun UnifiedSearchResultRow(
    item        : SearchResultItem,
    isDark      : Boolean,
    surface     : Color,
    textPrimary : Color
) {
    val meta = categoryMeta(item.category)

    Surface(
        modifier        = Modifier.fillMaxWidth(),
        shape           = RoundedCornerShape(14.dp),
        color           = surface,
        shadowElevation = 1.dp,
        tonalElevation  = 0.dp
    ) {
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier         = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDark) meta.bgDark else meta.bgLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = meta.icon,
                    contentDescription = null,
                    tint               = meta.tint,
                    modifier           = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = item.title,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color      = textPrimary,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text     = item.subtitle,
                    fontSize = 12.sp,
                    color    = TextSecondary
                )
            }

            Text(
                text       = item.amountLabel,
                fontSize   = 15.sp,
                fontWeight = FontWeight.Bold,
                color      = item.amountColor
            )
        }
    }
}