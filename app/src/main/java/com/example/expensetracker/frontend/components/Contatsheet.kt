package com.example.expensetracker.frontend.components

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.core.content.ContextCompat
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.expensetracker.frontend.important.Appstate.contactsGranted
import com.example.expensetracker.ui.theme.Primary
import com.example.expensetracker.ui.theme.SurfaceDark
import com.example.expensetracker.ui.theme.TextPrimary
import com.example.expensetracker.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class Contact(
    val id: String,
    val name: String,
    val phone: String,
    val photoUri: Uri? = null,
    val isRecent: Boolean = false,
    val initials: String = name.split(" ")
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
)

object ContactStore {
    val manualContacts = mutableStateListOf<com.example.expensetracker.frontend.components.Contact>()
    private var nextId = 1L

    fun add(name: String, phone: String) {
        manualContacts.add(
            0,
            _root_ide_package_.com.example.expensetracker.frontend.components.Contact(
                id = "manual_${nextId++}",
                name = name.trim(),
                phone = phone.trim(),
                isRecent = true
            )
        )
    }
}

private val avatarPalette = listOf(
    Color(0xFF4C6FFF) to Color(0xFFE8EDFF),
    Color(0xFF9B59B6) to Color(0xFFF3E6FF),
    Color(0xFF16A34A) to Color(0xFFE6F7ED),
    Color(0xFFEA580C) to Color(0xFFFFF0E6),
    Color(0xFFE74C3C) to Color(0xFFFFE6E6),
    Color(0xFF0EA5E9) to Color(0xFFE0F5FF),
)

private fun avatarColors(name: String): Pair<Color, Color> {
    val idx = (name.firstOrNull()?.code ?: 0) % _root_ide_package_.com.example.expensetracker.frontend.components.avatarPalette.size
    return _root_ide_package_.com.example.expensetracker.frontend.components.avatarPalette[idx]
}

private fun com.example.expensetracker.frontend.important.Contact.toUiContact(): com.example.expensetracker.frontend.components.Contact =
    _root_ide_package_.com.example.expensetracker.frontend.components.Contact(
        id = id,
        name = name,
        phone = phones.firstOrNull() ?: "",
        photoUri = photoUri,
        isRecent = false
    )

// ─────────────────────────────────────────────────────────────────────────────
//  Main Contact Sheet
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ContactSheet(
    onDismiss: () -> Unit,
    onContactSelected: ((com.example.expensetracker.frontend.components.Contact) -> Unit)? = null,
    isDark: Boolean = false,
    initialSelectedContact: com.example.expensetracker.frontend.components.Contact? = null          // ← pre-select support
) {
    val sheetBg      = if (isDark) SurfaceDark else Color.White
    val textPrimary  = if (isDark) Color.White else TextPrimary
    val searchBg     = if (isDark) Color(0xFF2A3347) else Color(0xFFF0F1F3)
    val closeBtnBg   = if (isDark) Color(0xFF2A3347) else Color(0xFFF0F1F3)

    val context      = LocalContext.current
    val scope        = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    var query          by remember { mutableStateOf("") }
    var selectedId     by remember { mutableStateOf(initialSelectedContact?.id) }  // ← pre-seed
    var showAddSheet   by remember { mutableStateOf(false) }
    var deviceContacts by remember { mutableStateOf<List<com.example.expensetracker.frontend.components.Contact>>(emptyList()) }
    var isLoading      by remember { mutableStateOf(true) }

    // Helper to (re-)fetch device contacts  (declared early so launchers can reference it)
    suspend fun loadContacts() {
        isLoading = true
        deviceContacts = withContext(Dispatchers.IO) {
            _root_ide_package_.com.example.expensetracker.frontend.important.fetchContacts(context)
                .map { it.toUiContact() }
        }
        isLoading = false
    }

    // ── Contacts permission: rationale + runtime request ──────────────────────
    var showContactsRationale by remember { mutableStateOf(false) }

    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        contactsGranted = isGranted
        if (isGranted) {
            scope.launch { loadContacts() }
        }
    }

    // Show rationale dialog the first time the sheet opens without permission
    LaunchedEffect(Unit) {
        val alreadyGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
        if (alreadyGranted) {
            contactsGranted = true
        } else {
            showContactsRationale = true
        }
    }

    // Rationale dialog explaining why contacts access is useful
    if (showContactsRationale) {
        AlertDialog(
            onDismissRequest = { showContactsRationale = false },
            title = {
                Text("Access your contacts?", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Expense Tracker can read your phone contacts so you can quickly " +
                            "tag who you paid or split an expense with — without typing names manually.\n\n" +
                            "You can still add contacts manually if you prefer."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showContactsRationale = false
                        contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                    }
                ) { Text("Allow") }
            },
            dismissButton = {
                TextButton(
                    onClick = { showContactsRationale = false }
                ) { Text("Not Now") }
            }
        )
    }

    // Initial load
    LaunchedEffect(Unit) { loadContacts() }

    // Re-fetch whenever the app resumes (user returns from Settings after granting permission)
    val lifecycle = lifecycleOwner.lifecycle
    LaunchedEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch { loadContacts() }
            }
        }
        lifecycle.addObserver(observer)
    }

    val allAvailable: List<com.example.expensetracker.frontend.components.Contact> = _root_ide_package_.com.example.expensetracker.frontend.components.ContactStore.manualContacts + deviceContacts

    val filtered = allAvailable.filter {
        query.isBlank() ||
                it.name.contains(query, ignoreCase = true) ||
                it.phone.contains(query)
    }
    val recentList = filtered.filter { it.isRecent }
    val allList    = filtered.filter { !it.isRecent }

    if (showAddSheet) {
        _root_ide_package_.com.example.expensetracker.frontend.components.AddContactSheet(
            onDismiss = { showAddSheet = false },
            onSaved = { showAddSheet = false },
            isDark = isDark
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress      = true,
            dismissOnClickOutside   = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(
                    indication       = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick          = onDismiss
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier          = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = false) {},
                shape             = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color             = sheetBg,
                shadowElevation   = 32.dp,
                tonalElevation    = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {

                    // Drag handle
                    Box(
                        modifier         = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, bottom = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(5.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFDDDDE3))
                        )
                    }

                    // Title + close
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 22.dp, end = 18.dp, top = 14.dp, bottom = 10.dp),
                        verticalAlignment    = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text       = "Contacts",
                            fontSize   = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color      = textPrimary
                        )
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(closeBtnBg)
                                .clickable { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector     = Icons.Default.Close,
                                contentDescription = "Close",
                                tint            = Color(0xFF6B7280),
                                modifier        = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Search bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(searchBg)
                            .padding(horizontal = 16.dp, vertical = 15.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector     = Icons.Default.Search,
                                contentDescription = null,
                                tint            = Color(0xFFADB5C7),
                                modifier        = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Box(modifier = Modifier.weight(1f)) {
                                if (query.isEmpty()) {
                                    Text(
                                        "Find a contact...",
                                        color    = Color(0xFFADB5C7),
                                        fontSize = 16.sp
                                    )
                                }
                                BasicTextField(
                                    value         = query,
                                    onValueChange = { query = it },
                                    singleLine    = true,
                                    textStyle     = TextStyle(color = textPrimary, fontSize = 16.sp),
                                    cursorBrush   = SolidColor(Primary),
                                    modifier      = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    // ── Body ──────────────────────────────────────────
                    LazyColumn(
                        modifier       = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        if (recentList.isNotEmpty()) {
                            item {
                                _root_ide_package_.com.example.expensetracker.frontend.components.SectionLabel(
                                    "RECENT CONTACTS"
                                )
                            }
                            items(recentList, key = { it.id }) { contact ->
                                _root_ide_package_.com.example.expensetracker.frontend.components.ContactRow(
                                    contact = contact,
                                    isSelected = selectedId == contact.id,
                                    textPrimary = textPrimary,
                                    onSelect = {
                                        selectedId =
                                            if (selectedId == contact.id) null else contact.id
                                        onContactSelected?.invoke(contact)
                                    }
                                )
                            }
                        }

                        if (allList.isNotEmpty()) {
                            item {
                                _root_ide_package_.com.example.expensetracker.frontend.components.SectionLabel(
                                    "ALL CONTACTS"
                                )
                            }
                            items(allList, key = { it.id }) { contact ->
                                _root_ide_package_.com.example.expensetracker.frontend.components.ContactRow(
                                    contact = contact,
                                    isSelected = selectedId == contact.id,
                                    textPrimary = textPrimary,
                                    onSelect = {
                                        selectedId =
                                            if (selectedId == contact.id) null else contact.id
                                        onContactSelected?.invoke(contact)
                                    }
                                )
                            }
                        }

                        // Empty / loading state
                        if (recentList.isEmpty() && allList.isEmpty()) {
                            item {
                                Box(
                                    modifier         = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    when {
                                        isLoading -> {
                                            CircularProgressIndicator(
                                                color    = Primary,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                        contactsGranted == true -> {
                                            Text(
                                                "No contacts found",
                                                color    = TextSecondary,
                                                fontSize = 15.sp
                                            )
                                        }
                                        else -> {
                                            _root_ide_package_.com.example.expensetracker.frontend.components.ContactsPermissionPrompt(
                                                isDark = isDark
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Add New Contact CTA
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        Button(
                            onClick   = { showAddSheet = true },
                            modifier  = Modifier
                                .fillMaxWidth()
                                .height(58.dp),
                            shape     = RoundedCornerShape(18.dp),
                            colors    = ButtonDefaults.buttonColors(containerColor = Primary),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                        ) {
                            Icon(
                                imageVector     = Icons.Default.PersonAdd,
                                contentDescription = null,
                                tint            = Color.White,
                                modifier        = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text       = "Add New Contact",
                                fontSize   = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color      = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Add Contact Sheet
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AddContactSheet(
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    isDark: Boolean = false
) {
    val sheetBg      = if (isDark) SurfaceDark else Color.White
    val fieldBg      = if (isDark) Color(0xFF2A3347) else Color(0xFFF0F2F5)
    val textPrimary  = if (isDark) Color.White else TextPrimary
    val labelColor   = Color(0xFF94A3B8)
    val hintColor    = Color(0xFFB0B8C8)

    var nameInput  by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }
    var nameError  by remember { mutableStateOf(false) }
    var phoneError by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties       = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress      = true,
            dismissOnClickOutside   = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
                .clickable(
                    indication        = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick           = onDismiss
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                modifier        = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = false) {},
                shape           = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color           = sheetBg,
                shadowElevation = 40.dp,
                tonalElevation  = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp)
                ) {

                    // Drag handle
                    Box(
                        modifier         = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp, bottom = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(5.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFDDDDE3))
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text       = "New Contact",
                        fontSize   = 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color      = textPrimary
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text     = "Add a recipient to your network",
                        fontSize = 15.sp,
                        color    = TextSecondary
                    )

                    Spacer(Modifier.height(28.dp))

                    // NAME
                    Text(
                        text          = "NAME",
                        fontSize      = 12.sp,
                        fontWeight    = FontWeight.Bold,
                        color         = labelColor,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (nameError) Color(0xFFFFF0F0) else fieldBg)
                            .then(
                                if (nameError) Modifier.border(
                                    1.dp, Color(0xFFE74C3C), RoundedCornerShape(14.dp)
                                ) else Modifier
                            )
                            .padding(horizontal = 18.dp, vertical = 18.dp)
                    ) {
                        if (nameInput.isEmpty()) {
                            Text("Full name", color = hintColor, fontSize = 16.sp)
                        }
                        BasicTextField(
                            value         = nameInput,
                            onValueChange = { nameInput = it; nameError = false },
                            singleLine    = true,
                            textStyle     = TextStyle(color = textPrimary, fontSize = 16.sp),
                            cursorBrush   = SolidColor(Primary),
                            modifier      = Modifier.fillMaxWidth()
                        )
                    }
                    if (nameError) {
                        Spacer(Modifier.height(4.dp))
                        Text("Name is required", fontSize = 12.sp, color = Color(0xFFE74C3C))
                    }

                    Spacer(Modifier.height(20.dp))

                    // MOBILE NUMBER
                    Text(
                        text          = "MOBILE NUMBER",
                        fontSize      = 12.sp,
                        fontWeight    = FontWeight.Bold,
                        color         = labelColor,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (phoneError) Color(0xFFFFF0F0) else fieldBg)
                            .then(
                                if (phoneError) Modifier.border(
                                    1.dp, Color(0xFFE74C3C), RoundedCornerShape(14.dp)
                                ) else Modifier
                            )
                            .padding(horizontal = 18.dp, vertical = 18.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier          = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                if (phoneInput.isEmpty()) {
                                    Text("+1 (000) 000-0000", color = hintColor, fontSize = 16.sp)
                                }
                                BasicTextField(
                                    value         = phoneInput,
                                    onValueChange = {
                                        if (it.all { c -> c.isDigit() || c in "+- ()" })
                                            phoneInput = it
                                        phoneError = false
                                    },
                                    singleLine      = true,
                                    textStyle       = TextStyle(color = textPrimary, fontSize = 16.sp),
                                    cursorBrush     = SolidColor(Primary),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    modifier        = Modifier.fillMaxWidth()
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isDark) Color(0xFF3A4357) else Color(0xFFDEE2EA)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector     = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint            = Color(0xFF64748B),
                                    modifier        = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    if (phoneError) {
                        Spacer(Modifier.height(4.dp))
                        Text("Enter a valid number", fontSize = 12.sp, color = Color(0xFFE74C3C))
                    }

                    Spacer(Modifier.height(28.dp))

                    Button(
                        onClick = {
                            nameError  = nameInput.isBlank()
                            phoneError = phoneInput.length < 6
                            if (!nameError && !phoneError) {
                                _root_ide_package_.com.example.expensetracker.frontend.components.ContactStore.add(nameInput, phoneInput)
                                onSaved()
                            }
                        },
                        modifier  = Modifier
                            .fillMaxWidth()
                            .height(62.dp),
                        shape     = RoundedCornerShape(50.dp),
                        colors    = ButtonDefaults.buttonColors(containerColor = Primary),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation  = 8.dp,
                            pressedElevation  = 4.dp
                        )
                    ) {
                        Text(
                            text       = "Save Contact",
                            fontSize   = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color      = Color.White
                        )
                    }

                    Spacer(Modifier.height(28.dp))
                }
            }
        }
    }
}

// ─── Private helpers ──────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text          = text,
        fontSize      = 12.sp,
        fontWeight    = FontWeight.Bold,
        color         = TextSecondary,
        letterSpacing = 0.8.sp,
        modifier      = Modifier.padding(start = 22.dp, end = 22.dp, top = 6.dp, bottom = 10.dp)
    )
}

@Composable
private fun ContactRow(
    contact: com.example.expensetracker.frontend.components.Contact,
    isSelected: Boolean,
    textPrimary: Color,
    onSelect: () -> Unit
) {
    val (fg, bg) = _root_ide_package_.com.example.expensetracker.frontend.components.avatarColors(
        contact.name
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .padding(horizontal = 22.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(bg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text       = contact.initials,
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold,
                color      = fg,
                textAlign  = TextAlign.Center
            )
        }

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                contact.name,
                fontSize   = 16.sp,
                fontWeight = FontWeight.Bold,
                color      = textPrimary
            )
            Spacer(Modifier.height(3.dp))
            Text(contact.phone, fontSize = 14.sp, color = TextSecondary)
        }

        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .then(
                    if (isSelected)
                        Modifier.background(Primary)
                    else
                        Modifier
                            .background(Color.Transparent)
                            .border(1.5.dp, Color(0xFFCDD0DA), CircleShape)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
        }
    }
}

@Composable
private fun ContactsPermissionPrompt(isDark: Boolean = false) {
    val context     = LocalContext.current
    val iconTint    = Primary
    val bgColor     = if (isDark) Color(0xFF1E2A3A) else Color(0xFFF0F4FF)
    val borderColor = Primary.copy(alpha = 0.3f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier            = Modifier.padding(horizontal = 24.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(bgColor)
                .border(1.dp, borderColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector     = Icons.Default.Phone,
                contentDescription = null,
                tint            = iconTint,
                modifier        = Modifier.size(28.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text       = "Contacts access is off",
            fontSize   = 16.sp,
            fontWeight = FontWeight.Bold,
            color      = if (isDark) Color.White else TextPrimary,
            textAlign  = TextAlign.Center
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text       = "Allow access to quickly find and\nadd people you already know,\nor add a new contact manually.",
            fontSize   = 14.sp,
            color      = TextSecondary,
            textAlign  = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(Modifier.height(20.dp))

        Box(
            modifier = Modifier.clickable {
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                ).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }
        ) {
            Text(
                text       = "Open Settings → Permissions",
                fontSize   = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color      = Primary,
            )
        }
    }
}