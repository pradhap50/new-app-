package com.example.ui

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.input.pointer.pointerInput
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChemDoseApp(viewModel: CalculatorViewModel) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    
    // UI State Observables
    val activeSlideOpt by viewModel.activeSlide.collectAsStateWithLifecycle()
    val filteredSlides by viewModel.filteredSlides.collectAsStateWithLifecycle()
    val allSlidesCount by viewModel.allSlides.collectAsStateWithLifecycle()
    val followSystemTheme by viewModel.followSystemTheme.collectAsStateWithLifecycle()
    val manualDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val systemInDark = isSystemInDarkTheme()
    val isDarkMode = if (followSystemTheme) systemInDark else manualDarkMode
    val isAdminMode by viewModel.isAdminMode.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val activeSlideId by viewModel.activeSlideId.collectAsStateWithLifecycle()

    // --- NEW: Security, AI and Logs State Observers ---
    val decimalPlaces by viewModel.decimalPlaces.collectAsStateWithLifecycle()

    // --- Universal Flow Unit Conversion System states ---
    val defaultFlowUnit by viewModel.defaultFlowUnit.collectAsStateWithLifecycle()

    // --- WEBSITE & CLOUD SYNCHRONIZATION STATES ---
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val lastSyncReport by viewModel.lastSyncReport.collectAsStateWithLifecycle()
    val cloudConfig by viewModel.cloudConfig.collectAsStateWithLifecycle()
    val currentUserRole by viewModel.currentUserRole.collectAsStateWithLifecycle()

    val firebaseConfig by viewModel.firebaseConfig.collectAsStateWithLifecycle()
    val firebaseUserUid by viewModel.firebaseUserUid.collectAsStateWithLifecycle()
    val firebaseAuthStatus by viewModel.firebaseAuthStatus.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    val lastBackupTime by viewModel.lastBackupTime.collectAsStateWithLifecycle()
    val lastBackupStatus by viewModel.lastBackupStatus.collectAsStateWithLifecycle()
    val adminPassword by viewModel.adminPassword.collectAsStateWithLifecycle()

    val autoCalculation by viewModel.autoCalculation.collectAsStateWithLifecycle()
    val fontScale by viewModel.fontScale.collectAsStateWithLifecycle()
    val openaiApiKey by viewModel.openaiApiKey.collectAsStateWithLifecycle()
    val enableAiCalculations by viewModel.enableAiCalculations.collectAsStateWithLifecycle()
    val aiSuggestions by viewModel.aiSuggestions.collectAsStateWithLifecycle()
    val userDefinedUnits by viewModel.userDefinedUnits.collectAsStateWithLifecycle()

    // Session State Observables
    val loggedInUserId by viewModel.loggedInUserId.collectAsStateWithLifecycle()
    val loggedInName by viewModel.loggedInName.collectAsStateWithLifecycle()
    val loggedInEmail by viewModel.loggedInEmail.collectAsStateWithLifecycle()
    val isUserLoggedIn by viewModel.isUserLoggedIn.collectAsStateWithLifecycle()
    val isAdminLoggedIn by viewModel.isAdminLoggedIn.collectAsStateWithLifecycle()

    var showProfileSessionDialog by remember { mutableStateOf(false) }

    // Dialog state controllers
    var showSlidesSearchDialog by remember { mutableStateOf(false) }
    var showBackupRestoreDialog by remember { mutableStateOf(false) }
    var showEditSlideMetaDialog by remember { mutableStateOf(false) }
    var showAddVarDialog by remember { mutableStateOf(false) }
    var showEditVarSpecsDialog by remember { mutableStateOf<Variable?>(null) }
    var variableToDelete by remember { mutableStateOf<Variable?>(null) }
    var showFormulaDetailDialog by remember { mutableStateOf(false) }
    var logToDelete by remember { mutableStateOf<CalculatorViewModel.LocalCalculationLog?>(null) }
    var showClearMyHistoryDialog by remember { mutableStateOf(false) }
    var showClearAllHistoryDialog by remember { mutableStateOf(false) }

    val formulaSaveStatus by viewModel.formulaSaveStatus.collectAsStateWithLifecycle()
    var showSaveErrorDialogMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(formulaSaveStatus) {
        when (val status = formulaSaveStatus) {
            is com.example.ui.CalculatorViewModel.FormulaOperationStatus.Success -> {
                showEditSlideMetaDialog = false
                Toast.makeText(context, "Formula Updated Successfully", Toast.LENGTH_SHORT).show()
                viewModel.resetFormulaSaveStatus()
            }
            is com.example.ui.CalculatorViewModel.FormulaOperationStatus.Error -> {
                showSaveErrorDialogMessage = "Formula update failed. Please check the formula and try again.\n\nError: ${status.message}"
            }
            else -> {}
        }
    }

    // --- NEW: Extensible Modals state controllers ---
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showAddSlideDialog by remember { mutableStateOf(false) }
    var showFormulaToggle by remember { mutableStateOf(true) }
    var unitBeingEdited by remember { mutableStateOf<String?>(null) }
    var unitEditInput by remember { mutableStateOf("") }
    var showAdminCommandCenter by remember { mutableStateOf(false) }
    var pendingAdminAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showAdminToolsMenu by remember { mutableStateOf(false) }
    var selectedAdminTab by remember { mutableStateOf(0) }
    var currentSubScreen by remember { mutableStateOf<String?>(null) }
    var activeDashboardTab by remember { mutableStateOf("Calculator") }
    var activeCalculationMode by remember { mutableStateOf("Dosage") } // "Flow", "Dosage", "Production", "GPL"
    var dosingFlow by remember { mutableStateOf("12.5") }
    var dosingGpl by remember { mutableStateOf("8.5") }
    var dosingProduction by remember { mutableStateOf("250.0") }
    var dosingDosage by remember { mutableStateOf("1.5") }
    var dosingCalculatedValue by remember { mutableStateOf<Double?>(null) }
    var dosingCalculatedTimestamp by remember { mutableStateOf("") }
    var showFloatingResult by remember { mutableStateOf(false) }

    LaunchedEffect(dosingCalculatedValue, dosingCalculatedTimestamp) {
        if (dosingCalculatedValue != null) {
            showFloatingResult = true
            kotlinx.coroutines.delay(5000)
            showFloatingResult = false
        }
    }

    // State controllers for custom link popup, inactivity timeouts, and approvals
    var lastAsaResultValue by remember { mutableStateOf(0.0) }
    var showLinkPopup by remember { mutableStateOf(false) }
    var pendingAsaFlowValue by remember { mutableStateOf(0.0) }
    var lastInteractionTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var confirmedAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    
    val runAdminAction: (() -> Unit) -> Unit = { action ->
        if (isAdminMode) {
            lastInteractionTime = System.currentTimeMillis()
            action()
        } else {
            pendingAdminAction = action
        }
    }

    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }

    // Automatic admin logout timer tracking (5 minutes / 300 seconds session limit)
    LaunchedEffect(isAdminMode) {
        if (isAdminMode) {
            lastInteractionTime = System.currentTimeMillis()
            currentTime = System.currentTimeMillis()
            while (isAdminMode) {
                kotlinx.coroutines.delay(1000)
                currentTime = System.currentTimeMillis()
                val elapsed = currentTime - lastInteractionTime
                if (elapsed > 5 * 60 * 1000) {
                    viewModel.setAdminMode(false)
                    viewModel.clearAuthSession()
                    Toast.makeText(context, "Logged out due to 5 minutes of inactivity", Toast.LENGTH_LONG).show()
                    break
                }
            }
        }
    }

    val remainingTimerStr = remember(currentTime, lastInteractionTime, isAdminMode) {
        if (!isAdminMode) ""
        else {
            val remainingMs = (5 * 60 * 1000) - (currentTime - lastInteractionTime)
            val totalSecs = (remainingMs / 1000).coerceAtLeast(0)
            val mins = totalSecs / 60
            val secs = totalSecs % 60
            String.format(Locale.getDefault(), "%d:%02d", mins, secs)
        }
    }

    // Launcher for JSON file restoration from system file explorer (Works directly with remote Google Drive files)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val jsonBytes = stream.readBytes()
                    val jsonStr = String(jsonBytes, Charsets.UTF_8)
                    viewModel.importBackup(jsonStr) { success ->
                        if (success) {
                            Toast.makeText(context, "Database Restored Successfully!", Toast.LENGTH_SHORT).show()
                            showBackupRestoreDialog = false
                        } else {
                            Toast.makeText(context, "Invalid Backup Schema!", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to read backup file!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- NEW: File Saver Contract to write native JSON directly to Google Drive ---
    val fileSaverLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    val backupJson = viewModel.getBackupDataJson()
                    stream.write(backupJson.toByteArray(Charsets.UTF_8))
                    Toast.makeText(context, "Backup Saved Natively to Drive!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to write backup file to Google Drive!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Material 3 Color Schemes - Premium slate navy / industrial dark branding
    val darkColorScheme = darkColorScheme(
        primary = Color(0xFF3B82F6),
        secondary = Color(0xFF22C55E),
        tertiary = Color(0xFFF59E0B),
        background = Color(0xFF090A0F),     // Near Black
        surface = Color(0xFF1E293B),        // Dark Blue/Grey
        surfaceVariant = Color(0xFF334155),
        onPrimary = Color.White,
        onSecondary = Color.White,
        onTertiary = Color.White,
        onBackground = Color(0xFFFFFFFF),   // White
        onSurface = Color(0xFFFFFFFF),      // White
        primaryContainer = Color(0xFF1E3A8A),
        onPrimaryContainer = Color(0xFFEFF6FF),
        outline = Color(0xFF475569),
        outlineVariant = Color(0xFF334155),
        error = Color(0xFFEF4444),
        onError = Color.White
    )

    val lightColorScheme = lightColorScheme(
        primary = Color(0xFF2563EB),
        secondary = Color(0xFF16A34A),
        tertiary = Color(0xFFD97706),
        background = Color(0xFFFFFFFF),     // White
        surface = Color(0xFFF1F5F9),        // Light Grey
        surfaceVariant = Color(0xFFE2E8F0),
        onPrimary = Color.White,
        onSecondary = Color.White,
        onTertiary = Color.White,
        onBackground = Color(0xFF0F172A),   // Black Text
        onSurface = Color(0xFF0F172A),      // Black Text
        primaryContainer = Color(0xFFDBEAFE),
        onPrimaryContainer = Color(0xFF1E40AF),
        outline = Color(0xFF94A3B8),
        outlineVariant = Color(0xFFE2E8F0),
        error = Color(0xFFDC2626),
        onError = Color.White
    )

    val customShapes = Shapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(16.dp),
        large = RoundedCornerShape(20.dp),
        extraLarge = RoundedCornerShape(24.dp)
    )

    MaterialTheme(
        colorScheme = if (isDarkMode) darkColorScheme else lightColorScheme,
        shapes = customShapes
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent()
                            lastInteractionTime = System.currentTimeMillis()
                        }
                    }
                }
        ) {
            if (!isUserLoggedIn && !isAdminLoggedIn) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(200f)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    LoginSelectionScreen(viewModel)
                }
            }

            if (showProfileSessionDialog) {
                Dialog(onDismissRequest = { showProfileSessionDialog = false }) {
                    NeumorphicCard(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        cornerRadius = 24.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (isAdminMode) "System Administrator" else (loggedInName ?: "Chemical Operator"),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = if (isAdminMode) "Role: master_admin" else "Role: operator (operator_${loggedInUserId})",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (!isAdminMode && loggedInEmail != null) {
                                    Text(
                                        text = loggedInEmail ?: "",
                                        fontSize = 12.sp,
                                        color = Color(0xFF334155)
                                    )
                                }
                            }

                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            Text(
                                text = if (isAdminMode) "Full Admin Privileges Active. Master override controls are enabled." 
                                       else "Operational Mode Active. Formulas, custom parameters, and calculation sheets are fully accessible in read/view modes. Standard settings and presets are restricted to administrative accounts.",
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                color = Color(0xFF334155),
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            Button(
                                onClick = {
                                    showProfileSessionDialog = false
                                    viewModel.clearAuthSession()
                                    Toast.makeText(context, "Logged out of current session.", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("logout_app_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Log Out / Signs Off", fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            OutlinedButton(
                                onClick = { showProfileSessionDialog = false },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Close Details")
                            }
                        }
                    }
                }
            }

            Scaffold(
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    navigationIcon = {
                        Box(
                            modifier = Modifier
                                .padding(start = 12.dp, end = 8.dp)
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF3B82F6),
                                            Color(0xFF10B981)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "🧪",
                                fontSize = 18.sp,
                                color = Color.White
                            )
                        }
                    },
                    title = {
                        Column(verticalArrangement = Arrangement.Center) {
                            Text(
                                text = "Chemical Dosing Dashboard",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.3.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isAdminMode) "👑 Master Administrator ($remainingTimerStr)"
                                       else if (isUserLoggedIn) "🏭 Operator: ${loggedInName ?: "Online"}"
                                       else "🏭 Chemical Operator Console (Read-Only)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isAdminMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    },
                    actions = {
                        // User Profile Action
                        IconButton(
                            onClick = {
                                showProfileSessionDialog = true
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("admin_status_badge")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isAdminMode) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        else if (isUserLoggedIn) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                    )
                                    .border(
                                        1.dp,
                                        if (isAdminMode) MaterialTheme.colorScheme.primary 
                                        else if (isUserLoggedIn) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                        else Color.Transparent,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "User Profile Mode",
                                    tint = if (isAdminMode) MaterialTheme.colorScheme.primary 
                                           else if (isUserLoggedIn) MaterialTheme.colorScheme.primary
                                           else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Compact Admin tools Menu if in Admin mode
                        if (isAdminMode) {
                            Box {
                                IconButton(
                                    onClick = { showAdminToolsMenu = true },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("admin_tools_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VpnKey,
                                        contentDescription = "Admin Tools Menu",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = showAdminToolsMenu,
                                    onDismissRequest = { showAdminToolsMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("🧪 Formula Management", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                        onClick = {
                                            showAdminToolsMenu = false
                                            selectedAdminTab = 0
                                            showAdminCommandCenter = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("🚨 Global Reset", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                        onClick = {
                                            showAdminToolsMenu = false
                                            selectedAdminTab = 1
                                            showAdminCommandCenter = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("💾 Import / Export", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                        onClick = {
                                            showAdminToolsMenu = false
                                            currentSubScreen = "data_mgmt"
                                            showBackupRestoreDialog = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("👤 User Management", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                        onClick = {
                                            showAdminToolsMenu = false
                                            currentSubScreen = "account"
                                            showBackupRestoreDialog = true
                                        }
                                    )
                                }
                            }
                        }

                        // Search Icon Action
                        IconButton(
                            onClick = { showSlidesSearchDialog = true },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("app_search_icon")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search Slides",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Quick Light/Dark Mode Theme Toggle Action
                        IconButton(
                            onClick = { viewModel.toggleDarkMode() },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("app_quick_theme_toggle")
                        ) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Toggle Theme on/off",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Settings Icon Action
                        IconButton(
                            onClick = { activeDashboardTab = "Settings" },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("app_settings_icon")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings Panel",
                                tint = if (activeDashboardTab == "Settings") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                )
            },
            bottomBar = {
                // Bottom controls: Previous, Search / Slide Overview, Next
                BottomAppBar(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Prev Button
                        Button(
                            onClick = { viewModel.previousSlide() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("prev_slide_btn")
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Previous")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Prev", fontSize = 14.sp)
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Page Search Trigger Button showing current slide index
                        Button(
                            onClick = { showSlidesSearchDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier
                                .weight(1.5f)
                                .height(48.dp)
                                .testTag("slide_search_trigger_btn")
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Slide Index Search")
                            Spacer(modifier = Modifier.width(6.dp))
                            val activeIndex = remember(allSlidesCount, activeSlideId) {
                                allSlidesCount.indexOfFirst { it.slide.id == activeSlideId }
                            }
                            val displayNum = if (activeIndex != -1) activeIndex + 1 else activeSlideId
                            Text("Slide #$displayNum / ${allSlidesCount.size}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Next Button
                        Button(
                            onClick = { viewModel.nextSlide() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("next_slide_btn")
                        ) {
                            Text("Next", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = "Next")
                        }
                    }
                }
            },
            floatingActionButton = {
                if (isAdminMode) {
                    FloatingActionButton(
                        onClick = { 
                            showAddSlideDialog = true
                        },
                        containerColor = if (isDarkMode) Color(0x22FFFFFF) else Color(0x73FFFFFF),
                        contentColor = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .testTag("add_slide_fab")
                            .border(1.dp, if (isDarkMode) Color(0x33FFFFFF) else Color(0x666750A4), RoundedCornerShape(20.dp))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add New Slide", modifier = Modifier.size(28.dp))
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(
                        if (isDarkMode) {
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF0B132B),
                                    Color(0xFF030712)
                                )
                            )
                        } else {
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFF8FAFC),
                                    Color(0xFFF1F5F9)
                                )
                            )
                        }
                    )
                    .drawBehind {
                        if (isDarkMode) {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0x183B82F6), Color.Transparent),
                                    center = androidx.compose.ui.geometry.Offset(size.width * 0.85f, size.height * 0.15f),
                                    radius = size.width * 0.6f
                                ),
                                radius = size.width * 0.6f,
                                center = androidx.compose.ui.geometry.Offset(size.width * 0.85f, size.height * 0.15f)
                            )
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0x1010B981), Color.Transparent),
                                    center = androidx.compose.ui.geometry.Offset(size.width * 0.15f, size.height * 0.85f),
                                    radius = size.width * 0.7f
                                ),
                                radius = size.width * 0.7f,
                                center = androidx.compose.ui.geometry.Offset(size.width * 0.15f, size.height * 0.85f)
                            )
                        }
                    }
            ) {
                // Dynamic floating success result card at top
                AnimatedVisibility(
                    visible = showFloatingResult && activeDashboardTab == "Calculator",
                    enter = fadeIn(animationSpec = spring()) + 
                            slideInVertically(
                                initialOffsetY = { -it },
                                animationSpec = spring()
                            ),
                    exit = fadeOut(animationSpec = spring()) + 
                           slideOutVertically(
                                targetOffsetY = { -it },
                                animationSpec = spring()
                           ),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .zIndex(99f)
                ) {
                    val displayUnit = when (activeCalculationMode) {
                        "Dosage" -> "kg/T"
                        "Flow" -> "LPM"
                        "Production" -> "TPD"
                        "GPL" -> "g/L"
                        else -> ""
                    }
                    val finalCalculatedVal = dosingCalculatedValue ?: 0.0
                    val formattedValue = String.format(Locale.US, "%,.${decimalPlaces}f", finalCalculatedVal)
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .shadow(12.dp, RoundedCornerShape(16.dp))
                            .testTag("floating_success_result_card")
                            .clickable {
                                val copyText = "$formattedValue $displayUnit"
                                clipboardManager.setText(AnnotatedString(copyText))
                                Toast.makeText(context, "Copied result: $copyText", Toast.LENGTH_SHORT).show()
                                showFloatingResult = false
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDarkMode) Color(0xF21E293B) else Color(0xF2E5EDFF)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF3B82F6), Color(0xFF10B981))
                            )
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x2210B981)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Success check",
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "CALCULATED $activeCalculationMode".uppercase(Locale.US),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF475569),
                                        letterSpacing = 0.5.sp
                                    )
                                    Text(
                                        text = "Tap to Copy • Auto-dismisses",
                                        fontSize = 9.sp,
                                        color = if (isDarkMode) Color(0x8094A3B8) else Color(0x80475569)
                                    )
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text(
                                    text = formattedValue,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isDarkMode) Color(0xFF10B981) else Color(0xFF0F172A)
                                )
                                Text(
                                    text = " $displayUnit",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                            }
                        }
                    }
                }
                Column(modifier = Modifier.fillMaxSize()) {
                    // --- SEGMENTED NAVIGATION ROW ---
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val tabs = listOf("Calculator", "Settings")
                            tabs.forEach { tab ->
                                val isSelected = activeDashboardTab == tab
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary 
                                            else Color.Transparent
                                        )
                                        .clickable { activeDashboardTab = tab }
                                        .testTag("nav_tab_$tab"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = tab,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary 
                                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }

                    // SWITCH CONTENT DISPLAY ACCORDING TO THE SELECTED TAB STATE
                    when (activeDashboardTab) {
                        "Calculator" -> {
                            activeSlideOpt?.let { currentSlide ->
                                val vars = currentSlide.variables.associate { it.symbol to it.value }
                                val displayResultValue = FormulaEvaluator.evaluate(currentSlide.slide.formula, vars)
                                val displayResultUnit = currentSlide.slide.resultUnit
                                val displayActiveFormula = currentSlide.slide.formula
                                val displayDetails = currentSlide.slide.description
                                val isDosingSlide = false

                                if (isDosingSlide) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 16.dp)
                                            .verticalScroll(rememberScrollState())
                                    ) {
                                        // 1. Formula Card
                                        var isFormulaCardExpanded by remember { mutableStateOf(false) }
                                        Card(
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = "FORMULA ID #${currentSlide.slide.id} • ${currentSlide.slide.category.uppercase(Locale.US)}",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            letterSpacing = 1.sp
                                                        )
                                                        Text(
                                                            text = currentSlide.slide.title,
                                                            fontSize = 16.sp,
                                                            fontWeight = FontWeight.Black,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = { isFormulaCardExpanded = !isFormulaCardExpanded },
                                                        modifier = Modifier.testTag("toggle_formula_card_btn")
                                                    ) {
                                                        Icon(
                                                            imageVector = if (isFormulaCardExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                            contentDescription = "Expand or Collapse Formula Card"
                                                        )
                                                    }
                                                }

                                                if (isFormulaCardExpanded) {
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    
                                                    // Center Mathematical Fraction View
                                                    DosingMathematicalFractionView(
                                                        mode = activeCalculationMode,
                                                        modifier = Modifier.fillMaxWidth().testTag("formula_fraction_preview")
                                                    )
                                                    
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Text(
                                                        text = currentSlide.slide.description.split("\n---outputs---\n")[0],
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                                        lineHeight = 15.sp
                                                    )
                                                }
                                            }
                                        }

                                        // 2. Calculation Mode Card
                                        Card(
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text(
                                                    text = "SELECT CALCULATION TARGET",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    letterSpacing = 1.sp
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    val modes = listOf("Flow", "Dosage", "Production", "GPL")
                                                    modes.forEach { mode ->
                                                        val isSelected = activeCalculationMode == mode
                                                        Box(
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .height(36.dp)
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .background(
                                                                    if (isSelected) MaterialTheme.colorScheme.primary
                                                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                                                )
                                                                .clickable { activeCalculationMode = mode }
                                                                .testTag("calc_mode_$mode"),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = mode,
                                                                fontSize = 11.sp,
                                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // 3. Input Section
                                        Text(
                                            text = "INPUT PARAMETERS",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                        
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            if (activeCalculationMode != "Flow") {
                                                DosingInputField(
                                                    value = dosingFlow,
                                                    onValueChange = { dosingFlow = it },
                                                    label = "Flow",
                                                    unit = "LPM",
                                                    testTag = "flow_input_field"
                                                )
                                            }
                                            if (activeCalculationMode != "Dosage") {
                                                DosingInputField(
                                                    value = dosingDosage,
                                                    onValueChange = { dosingDosage = it },
                                                    label = "Dosage",
                                                    unit = "kg/T",
                                                    testTag = "dosage_input_field"
                                                )
                                            }
                                            if (activeCalculationMode != "Production") {
                                                DosingInputField(
                                                    value = dosingProduction,
                                                    onValueChange = { dosingProduction = it },
                                                    label = "Production",
                                                    unit = "TPD",
                                                    testTag = "production_input_field"
                                                )
                                            }
                                            if (activeCalculationMode != "GPL") {
                                                DosingInputField(
                                                    value = dosingGpl,
                                                    onValueChange = { dosingGpl = it },
                                                    label = "GPL Concentration",
                                                    unit = "g/L",
                                                    testTag = "gpl_input_field"
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))

                                        // 4. Calculate Button
                                        Button(
                                            onClick = {
                                                val flowD = dosingFlow.toDoubleOrNull() ?: 0.0
                                                val gplD = dosingGpl.toDoubleOrNull() ?: 0.0
                                                val prodD = dosingProduction.toDoubleOrNull() ?: 0.0
                                                val dosD = dosingDosage.toDoubleOrNull() ?: 0.0
                                                
                                                val result = when (activeCalculationMode) {
                                                    "Dosage" -> if (prodD > 0.0) (flowD * gplD * 1.44) / prodD else 0.0
                                                    "Flow" -> if (gplD > 0.0) (dosD * prodD) / (gplD * 1.44) else 0.0
                                                    "Production" -> if (dosD > 0.0) (flowD * gplD * 1.44) / dosD else 0.0
                                                    "GPL" -> if (flowD > 0.0) (dosD * prodD) / (flowD * 1.44) else 0.0
                                                    else -> 0.0
                                                }
                                                
                                                dosingCalculatedValue = result
                                                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                                                dosingCalculatedTimestamp = sdf.format(java.util.Date())
                                                
                                                val inputsStr = when (activeCalculationMode) {
                                                    "Dosage" -> "Flow=$dosingFlow, GPL=$dosingGpl, Production=$dosingProduction"
                                                    "Flow" -> "Dosage=$dosingDosage, GPL=$dosingGpl, Production=$dosingProduction"
                                                    "Production" -> "Flow=$dosingFlow, GPL=$dosingGpl, Dosage=$dosingDosage"
                                                    "GPL" -> "Flow=$dosingFlow, Dosage=$dosingDosage, Production=$dosingProduction"
                                                    else -> ""
                                                }
                                                val resultUnitStr = when (activeCalculationMode) {
                                                    "Dosage" -> "kg/T"
                                                    "Flow" -> "LPM"
                                                    "Production" -> "TPD"
                                                    "GPL" -> "g/L"
                                                    else -> ""
                                                }
                                                val resultFormatted = String.format(Locale.US, "%,.${decimalPlaces}f", result)
                                                viewModel.addToLocalHistory(
                                                    currentSlide.slide.id,
                                                    inputsStr,
                                                    "$resultFormatted $resultUnitStr"
                                                )
                                                Toast.makeText(context, "Calculation recorded and cached offline!", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(52.dp)
                                                .shadow(6.dp, RoundedCornerShape(16.dp))
                                                .testTag("calculate_master_button"),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isDarkMode) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                                                contentColor = if (isDarkMode) Color(0xFF070B18) else MaterialTheme.colorScheme.onPrimary
                                            ),
                                            shape = RoundedCornerShape(16.dp)
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = "Calculate Logo", modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("RUN CALCULATION ENGINE", fontWeight = FontWeight.Black, fontSize = 14.sp, letterSpacing = 1.sp)
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))

                                        // 5. Result Card (Permanently Visible)
                                        if (true) {
                                            val tempFlowD = dosingFlow.toDoubleOrNull() ?: 0.0
                                            val tempGplD = dosingGpl.toDoubleOrNull() ?: 0.0
                                            val tempProdD = dosingProduction.toDoubleOrNull() ?: 0.0
                                            val tempDosD = dosingDosage.toDoubleOrNull() ?: 0.0
                                            val currentRealtimeResult = when (activeCalculationMode) {
                                                "Dosage" -> if (tempProdD > 0.0) (tempFlowD * tempGplD * 1.44) / tempProdD else 0.0
                                                "Flow" -> if (tempGplD > 0.0) (tempDosD * tempProdD) / (tempGplD * 1.44) else 0.0
                                                "Production" -> if (tempDosD > 0.0) (tempFlowD * tempGplD * 1.44) / tempDosD else 0.0
                                                "GPL" -> if (tempFlowD > 0.0) (tempDosD * tempProdD) / (tempFlowD * 1.44) else 0.0
                                                else -> 0.0
                                            }
                                            val finalCalculatedVal = dosingCalculatedValue ?: currentRealtimeResult
                                            val displayUnit = when (activeCalculationMode) {
                                                "Dosage" -> "kg/T"
                                                "Flow" -> "LPM"
                                                "Production" -> "TPD"
                                                "GPL" -> "g/L"
                                                else -> ""
                                            }
                                            Card(
                                                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp).testTag("calculated_result_card"),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                                ),
                                                border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column {
                                                        Text(
                                                            text = "CALCULATED $activeCalculationMode".uppercase(Locale.US),
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            letterSpacing = 0.5.sp
                                                        )
                                                        Row(verticalAlignment = Alignment.Bottom) {
                                                            Text(
                                                                text = String.format(Locale.US, "%,.${decimalPlaces}f", finalCalculatedVal),
                                                                fontSize = 32.sp,
                                                                fontWeight = FontWeight.Black,
                                                                fontFamily = FontFamily.Monospace,
                                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                                            )
                                                            Text(
                                                                text = " $displayUnit",
                                                                fontSize = 18.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier.padding(bottom = 4.dp)
                                                            )
                                                        }
                                                        Text(
                                                            text = "Saved At: $dosingCalculatedTimestamp",
                                                            fontSize = 9.sp,
                                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                                        )
                                                    }

                                                    IconButton(
                                                        onClick = {
                                                            val formatted = String.format(Locale.US, "%,.${decimalPlaces}f", finalCalculatedVal)
                                                            val copyText = "$formatted $displayUnit"
                                                            clipboardManager.setText(AnnotatedString(copyText))
                                                            Toast.makeText(context, "Copied result: $copyText", Toast.LENGTH_SHORT).show()
                                                        },
                                                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)).size(40.dp)
                                                    ) {
                                                        Icon(Icons.Default.Share, contentDescription = "Copy text result", tint = MaterialTheme.colorScheme.primary)
                                                    }
                                                }
                                            }
                                        }
                                        
                                        // Compact History disabled
                                        val localHistory by viewModel.localHistory.collectAsStateWithLifecycle()
                                        val activeHistory = remember(localHistory, currentSlide.slide.id) {
                                            localHistory.filter { it.slideId == currentSlide.slide.id }.take(5)
                                        }
                                        if (false) {
                                            var isHistoryExpanded by remember(currentSlide.slide.id) { mutableStateOf(false) }
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                TextButton(
                                                    onClick = { isHistoryExpanded = !isHistoryExpanded },
                                                    modifier = Modifier.testTag("toggle_history_button")
                                                ) {
                                                    Text(
                                                        text = if (isHistoryExpanded) "Hide History ▲" else "View History ▼",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }

                                            if (isHistoryExpanded) {
                                                Card(
                                                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp).testTag("history_container"),
                                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                                                ) {
                                                    Column(modifier = Modifier.padding(12.dp)) {
                                                        activeHistory.forEachIndexed { index, log ->
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Column(modifier = Modifier.weight(1f)) {
                                                                    Text(
                                                                        text = log.inputs,
                                                                        fontSize = 10.sp,
                                                                        fontFamily = FontFamily.Monospace,
                                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                                                        maxLines = 1,
                                                                        overflow = TextOverflow.Ellipsis
                                                                    )
                                                                    Text(
                                                                        text = log.timestamp,
                                                                        fontSize = 8.sp,
                                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                                                    )
                                                                }
                                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                                    Text(
                                                                        text = log.result,
                                                                        fontSize = 11.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = MaterialTheme.colorScheme.secondary
                                                                    )
                                                                    IconButton(
                                                                        onClick = { logToDelete = log },
                                                                        modifier = Modifier.size(28.dp).testTag("delete_history_item_$index")
                                                                    ) {
                                                                        Icon(
                                                                            imageVector = Icons.Default.Delete,
                                                                            contentDescription = "Delete record",
                                                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                                                            modifier = Modifier.size(16.dp)
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
                                } else {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 16.dp)
                                            .verticalScroll(rememberScrollState())
                                    ) {
                                        val formulaCardBg = MaterialTheme.colorScheme.surface
                                        val formulaCardText = MaterialTheme.colorScheme.onSurface
                                        val formulaCardLabel = MaterialTheme.colorScheme.primary
                                        val formulaCardExampleValue = MaterialTheme.colorScheme.secondary
                                        val formulaCardBorder = MaterialTheme.colorScheme.outline

                                        var isFormulaExpanded by remember(currentSlide.slide.id) { mutableStateOf(false) }

                                        Card(
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                            colors = CardDefaults.cardColors(containerColor = formulaCardBg),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, formulaCardBorder),
                                            shape = RoundedCornerShape(16.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "FORMULA ID #${currentSlide.slide.id} • ${currentSlide.slide.category.uppercase(Locale.US)}",
                                                        color = formulaCardLabel,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        letterSpacing = 1.2.sp,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    
                                                    if (isAdminMode) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .clickable { showEditSlideMetaDialog = true }
                                                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f))
                                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Edit,
                                                                contentDescription = "Edit Formula",
                                                               tint = formulaCardLabel,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text(
                                                                text = "Edit",
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = formulaCardLabel
                                                            )
                                                        }
                                                    }
                                                }
                                                
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = currentSlide.slide.title,
                                                    fontSize = 20.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = formulaCardText
                                                )

                                                val shortDescription = remember(currentSlide.slide.description) {
                                                    currentSlide.slide.description.split("\n---outputs---\n")[0].trim()
                                                }
                                                if (shortDescription.isNotEmpty()) {
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = shortDescription,
                                                        fontSize = 12.sp,
                                                        color = formulaCardText.copy(alpha = 0.7f),
                                                        lineHeight = 16.sp
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(12.dp))
                                                Box(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .clickable { isFormulaExpanded = !isFormulaExpanded }
                                                            .border(1.dp, formulaCardBorder.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.Center
                                                    ) {
                                                        Text(
                                                            text = if (isFormulaExpanded) "Hide Formula ▲" else "Show Formula ▼",
                                                            color = formulaCardLabel,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                     }
                                                 }

                                                 AnimatedVisibility(
                                                     visible = isFormulaExpanded,
                                                     enter = expandVertically(animationSpec = tween(250)) + fadeIn(animationSpec = tween(250)),
                                                     exit = shrinkVertically(animationSpec = tween(250)) + fadeOut(animationSpec = tween(250))
                                                 ) {
                                                     Column(modifier = Modifier.fillMaxWidth()) {
                                                         Spacer(modifier = Modifier.height(16.dp))
                                                         HorizontalDivider(color = formulaCardBorder.copy(alpha = 0.3f))
                                                         Spacer(modifier = Modifier.height(16.dp))

                                                         // 1. Formula Header & Centered Text
                                                         Text(
                                                             text = "Formula:",
                                                             color = formulaCardLabel,
                                                             fontSize = 12.sp,
                                                             fontWeight = FontWeight.Bold
                                                         )
                                                         Spacer(modifier = Modifier.height(4.dp))
                                                         
                                                         // Formula centered expression
                                                         val resultLabel = remember(currentSlide.slide.title) {
                                                             currentSlide.slide.title.replace(" Calculator", "").trim()
                                                         }
                                                         val formulaExpressionStr = remember(displayActiveFormula, currentSlide.variables) {
                                                             translateFormulaToDescriptions(displayActiveFormula, currentSlide.variables)
                                                         }
                                                         Text(
                                                             text = "$resultLabel = $formulaExpressionStr",
                                                             fontSize = 18.sp,
                                                             fontWeight = FontWeight.Bold,
                                                             color = formulaCardText,
                                                             textAlign = TextAlign.Center,
                                                             modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                                                         )

                                                         Spacer(modifier = Modifier.height(12.dp))

                                                         // 2. Example Section
                                                         val exampleFormulaStringJoined = remember(displayActiveFormula, currentSlide.variables) {
                                                             val tokens = if (displayActiveFormula.trim().startsWith("[")) {
                                                                 FormulaSerializer.deserialize(displayActiveFormula) ?: FormulaEvaluator.tokenize(displayActiveFormula)
                                                             } else {
                                                                 FormulaEvaluator.tokenize(displayActiveFormula)
                                                             }
                                                             val exprText = FormulaEvaluator.tokensToPlainExpression(tokens)
                                                             val regex = Regex("([a-zA-Z_][a-zA-Z0-9_]*|\\d+\\.?\\d*|[^a-zA-Z0-9_\\s])")
                                                             val matchedTokens = regex.findAll(exprText).map { it.value }.toList()
                                                             matchedTokens.joinToString("") { token ->
                                                                 val variable = currentSlide.variables.find { it.symbol.equals(token, ignoreCase = true) }
                                                                 when {
                                                                     variable != null -> formatDouble(variable.value)
                                                                     token == "*" -> " × "
                                                                     token == "/" -> " ÷ "
                                                                     token == "+" -> " + "
                                                                     token == "-" -> " - "
                                                                     token == "(" -> " ("
                                                                     token == ")" -> ") "
                                                                     else -> token
                                                                 }
                                                             }.trim().replace(Regex("\\s+"), " ")
                                                         }

                                                         val exampleResult = remember(displayActiveFormula, currentSlide.variables) {
                                                             val valuesMap = currentSlide.variables.associate { it.symbol to it.value }
                                                             FormulaEvaluator.evaluate(displayActiveFormula, valuesMap)
                                                         }

                                                         Text(
                                                             text = "Example:",
                                                             color = formulaCardLabel,
                                                             fontSize = 12.sp,
                                                             fontWeight = FontWeight.Bold
                                                         )
                                                         Spacer(modifier = Modifier.height(4.dp))
                                                         Text(
                                                             text = "$exampleFormulaStringJoined = ${String.format(Locale.US, "%,.${decimalPlaces}f", exampleResult)} ${currentSlide.slide.resultUnit}",
                                                             fontSize = 15.sp,
                                                             fontWeight = FontWeight.SemiBold,
                                                             fontFamily = FontFamily.Monospace,
                                                             color = formulaCardExampleValue,
                                                             textAlign = TextAlign.Center,
                                                             modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                                         )

                                                         Spacer(modifier = Modifier.height(12.dp))

                                                         // 3. Units Section
                                                         Text(
                                                             text = "Units:",
                                                             color = formulaCardLabel,
                                                             fontSize = 12.sp,
                                                             fontWeight = FontWeight.Bold
                                                         )
                                                         Spacer(modifier = Modifier.height(4.dp))
                                                         
                                                         Column(
                                                             verticalArrangement = Arrangement.spacedBy(4.dp),
                                                             modifier = Modifier.padding(start = 8.dp)
                                                         ) {
                                                             currentSlide.variables.forEach { variable ->
                                                                 val vUnit = if (variable.unit.isNotEmpty()) variable.unit else "unit"
                                                                 Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                                     Text(
                                                                         text = variable.name,
                                                                         fontSize = 13.sp,
                                                                         color = formulaCardText.copy(alpha = 0.8f),
                                                                         fontWeight = FontWeight.Medium
                                                                     )
                                                                     Text(
                                                                         text = "= $vUnit",
                                                                         fontSize = 13.sp,
                                                                         color = formulaCardLabel,
                                                                         fontWeight = FontWeight.Bold,
                                                                         fontFamily = FontFamily.Monospace
                                                                     )
                                                                 }
                                                             }
                                                             Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                                 Text(
                                                                     text = "Output ($resultLabel)",
                                                                     fontSize = 13.sp,
                                                                     color = formulaCardText.copy(alpha = 0.8f),
                                                                     fontWeight = FontWeight.Medium
                                                                 )
                                                                 Text(
                                                                     text = "= ${currentSlide.slide.resultUnit}",
                                                                     fontSize = 13.sp,
                                                                     color = formulaCardLabel,
                                                                     fontWeight = FontWeight.Bold,
                                                                     fontFamily = FontFamily.Monospace
                                                                 )
                                                             }
                                                         }
                                                     }
                                                 }

                                                 if (false) {
                                                     Spacer(modifier = Modifier.height(4.dp))
                                                     Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                 ) {
                                                     Text(
                                                         text = currentSlide.slide.title,
                                                         fontSize = 18.sp,
                                                         fontWeight = FontWeight.Bold,
                                                         color = MaterialTheme.colorScheme.onSurface,
                                                         modifier = Modifier.weight(1f)
                                                     )
                                                     
                                                     Row(
                                                         verticalAlignment = Alignment.CenterVertically,
                                                         modifier = Modifier
                                                             .clip(RoundedCornerShape(8.dp))
                                                             .clickable(enabled = isAdminMode) { showEditSlideMetaDialog = true }
                                                             .background(if (isAdminMode) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f) else Color.Transparent)
                                                             .padding(horizontal = if (isAdminMode) 8.dp else 0.dp, vertical = if (isAdminMode) 4.dp else 0.dp)
                                                     ) {
                                                         Icon(
                                                             imageVector = Icons.Default.Edit,
                                                             contentDescription = "Edit Formula",
                                                             tint = MaterialTheme.colorScheme.primary,
                                                             modifier = Modifier.size(16.dp)
                                                         )
                                                         Spacer(modifier = Modifier.width(4.dp))
                                                         Text(
                                                             text = if (isAdminMode) "Edit Formula Details" else "",
                                                             fontSize = 11.sp,
                                                             fontWeight = FontWeight.Bold,
                                                             color = MaterialTheme.colorScheme.primary
                                                         )
                                                     }
                                                 }

                                                 if (true) {
                                                     Spacer(modifier = Modifier.height(8.dp))
                                                     MathematicalFractionView(
                                                         formula = displayActiveFormula,
                                                         variables = currentSlide.variables,
                                                         resultUnit = currentSlide.slide.resultUnit,
                                                         title = currentSlide.slide.title,
                                                         modifier = Modifier.fillMaxWidth().testTag("formula_expression_text")
                                                     )
                                                     Spacer(modifier = Modifier.height(8.dp))
                                                     Text(
                                                         text = "Formula Expression: ${if (displayActiveFormula.trim().startsWith("[")) (FormulaSerializer.deserialize(displayActiveFormula)?.let { FormulaEvaluator.tokensToPlainExpression(it) } ?: displayActiveFormula) else displayActiveFormula}",
                                                         fontFamily = FontFamily.Monospace,
                                                         fontSize = 11.sp,
                                                         color = MaterialTheme.colorScheme.secondary,
                                                         fontWeight = FontWeight.Bold
                                                     )
                                                     
                                                     // Example Substitution calculation
                                                     val exampleFormulaString = remember(displayActiveFormula, currentSlide.variables) {
                                                         val tokens = if (displayActiveFormula.trim().startsWith("[")) {
                                                             FormulaSerializer.deserialize(displayActiveFormula) ?: FormulaEvaluator.tokenize(displayActiveFormula)
                                                         } else {
                                                             FormulaEvaluator.tokenize(displayActiveFormula)
                                                         }
                                                         var exprText = FormulaEvaluator.tokensToPlainExpression(tokens)
                                                         currentSlide.variables.forEach { variable ->
                                                             exprText = exprText.replace(Regex("\\b${variable.symbol}\\b"), variable.value.toString())
                                                         }
                                                         exprText
                                                     }
                                                     val exampleResult = remember(displayActiveFormula, currentSlide.variables) {
                                                         val valuesMap = currentSlide.variables.associate { it.symbol to it.value }
                                                         FormulaEvaluator.evaluate(displayActiveFormula, valuesMap)
                                                     }
                                                     Spacer(modifier = Modifier.height(4.dp))
                                                     Text(
                                                         text = "Example Calculation:\n$exampleFormulaString = ${String.format(Locale.US, "%.${decimalPlaces}f", exampleResult)} ${currentSlide.slide.resultUnit}",
                                                         fontFamily = FontFamily.Monospace,
                                                         fontSize = 10.sp,
                                                         color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                                         lineHeight = 14.sp
                                                     )

                                                     if (currentSlide.slide.description.isNotEmpty()) {
                                                         Spacer(modifier = Modifier.height(6.dp))
                                                         Text(
                                                             text = currentSlide.slide.description,
                                                             fontSize = 11.sp,
                                                             color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                                             lineHeight = 15.sp
                                                         )
                                                     }
                                                 }
                                                 } // closes if (false) block
                                             }
                                         }

                                         // Permanent Output Card
                                         val displayResultFormatted = String.format(Locale.US, "%,.${decimalPlaces}f", displayResultValue)
                                         Spacer(modifier = Modifier.height(12.dp))
                                         Card(
                                             modifier = Modifier.fillMaxWidth().testTag("calculated_result_card"),
                                             colors = CardDefaults.cardColors(
                                                 containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                             ),
                                             border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                                         ) {
                                             Row(
                                                 modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                                 verticalAlignment = Alignment.CenterVertically,
                                                 horizontalArrangement = Arrangement.SpaceBetween
                                             ) {
                                                 Column {
                                                     Text(
                                                         text = "CALCULATED RESULT",
                                                         fontSize = 10.sp,
                                                         fontWeight = FontWeight.Bold,
                                                         color = MaterialTheme.colorScheme.primary,
                                                         letterSpacing = 0.5.sp
                                                     )
                                                     Row(verticalAlignment = Alignment.Bottom) {
                                                         Text(
                                                             text = displayResultFormatted,
                                                             fontSize = 32.sp,
                                                             fontWeight = FontWeight.Black,
                                                             fontFamily = FontFamily.Monospace,
                                                             color = MaterialTheme.colorScheme.onPrimaryContainer
                                                         )
                                                         Text(
                                                             text = " $displayResultUnit",
                                                             fontSize = 18.sp,
                                                             fontWeight = FontWeight.Bold,
                                                             color = MaterialTheme.colorScheme.primary,
                                                             modifier = Modifier.padding(bottom = 4.dp)
                                                         )
                                                     }
                                                 }
                                                 IconButton(
                                                     onClick = {
                                                         clipboardManager.setText(AnnotatedString("$displayResultFormatted $displayResultUnit"))
                                                         Toast.makeText(context, "Copied result: $displayResultFormatted $displayResultUnit", Toast.LENGTH_SHORT).show()
                                                     },
                                                     modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)).size(40.dp)
                                                 ) {
                                                     Icon(Icons.Default.Share, contentDescription = "Copy text result", tint = MaterialTheme.colorScheme.primary)
                                                 }
                                             }
                                         }

                                        val sortedVariables = remember(currentSlide.variables) {
                                            currentSlide.variables.sortedWith(compareBy({ it.sortOrder }, { it.id }))
                                        }
                                        val displayedVariables = remember(sortedVariables, isAdminMode) {
                                            if (isAdminMode) sortedVariables else sortedVariables.filter { !it.isHidden }
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "INPUT PARAMETERS",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            if (isAdminMode) {
                                                TextButton(
                                                    onClick = { showAddVarDialog = true },
                                                    modifier = Modifier.testTag("add_parameter_button")
                                                ) {
                                                    Icon(Icons.Default.Add, contentDescription = "Add Parameter", modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Add Param", fontSize = 11.sp)
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))

                                        LazyColumn(
                                            verticalArrangement = Arrangement.spacedBy(8.dp), userScrollEnabled = false,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            if (displayedVariables.isEmpty()) {
                                                item {
                                                    Text(
                                                        text = if (isAdminMode) "No parameters configured." else "No visible parameters for this template.",
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                                    )
                                                }
                                            } else {
                                                item { displayedVariables.forEachIndexed { index, variable ->
                                                    VariableInputRow(
                                                        variable = variable,
                                                        isAdmin = isAdminMode,
                                                        defaultFlowUnit = defaultFlowUnit,
                                                        onValueChange = { doubleVal ->
                                                            viewModel.updateVariableValue(variable, doubleVal)
                                                        },
                                                        onEditSpecs = {
                                                            runAdminAction {
                                                                 showEditVarSpecsDialog = variable
                                                            }
                                                        },
                                                        onDelete = {
                                                            runAdminAction {
                                                                variableToDelete = variable
                                                            }
                                                        },
                                                        onMoveUp = if (index > 0) {
                                                            {
                                                                val listCopy = sortedVariables.toMutableList()
                                                                val targetIdx = listCopy.indexOf(variable)
                                                                if (targetIdx > 0) {
                                                                    val temp = listCopy[targetIdx]
                                                                    listCopy[targetIdx] = listCopy[targetIdx - 1]
                                                                    listCopy[targetIdx - 1] = temp
                                                                    viewModel.updateVariablesOrder(listCopy)
                                                                }
                                                            }
                                                        } else null,
                                                        onMoveDown = if (index < displayedVariables.size - 1) {
                                                            {
                                                                val listCopy = sortedVariables.toMutableList()
                                                                val targetIdx = listCopy.indexOf(variable)
                                                                if (targetIdx >= 0 && targetIdx < listCopy.size - 1) {
                                                                    val temp = listCopy[targetIdx]
                                                                    listCopy[targetIdx] = listCopy[targetIdx + 1]
                                                                    listCopy[targetIdx + 1] = temp
                                                                    viewModel.updateVariablesOrder(listCopy)
                                                                }
                                                            }
                                                        } else null
                                                    )
                                                } }
                                            }
                                        }
                                    }
                                }
                            } ?: run {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                        
                        "History" -> {
                            val localHistory by viewModel.localHistory.collectAsStateWithLifecycle()
                            val filteredHistory = remember(localHistory, searchQuery) {
                                if (searchQuery.isEmpty()) {
                                    localHistory
                                } else {
                                    localHistory.filter { 
                                        it.inputs.contains(searchQuery, ignoreCase = true) || 
                                        it.result.contains(searchQuery, ignoreCase = true)
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp)
                            ) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { viewModel.updateSearchQuery(it) },
                                    placeholder = { Text("Filter logs by input value or result...", fontSize = 12.sp) },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search icon", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("history_logs_search"),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                if (filteredHistory.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().weight(1f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("📋", fontSize = 48.sp)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("No logs found matching search.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                                        }
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        itemsIndexed(filteredHistory) { idx, log ->
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = "Calculation Run #${log.slideId}",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Text(
                                                            text = log.inputs,
                                                            fontSize = 12.sp,
                                                            fontFamily = FontFamily.Monospace,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Spacer(modifier = Modifier.height(1.dp))
                                                        Text(
                                                            text = log.timestamp,
                                                            fontSize = 9.sp,
                                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                                        )
                                                    }
                                                    
                                                    Column(horizontalAlignment = Alignment.End) {
                                                        Text(
                                                            text = log.result,
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Black,
                                                            color = MaterialTheme.colorScheme.secondary
                                                        )
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        IconButton(
                                                            onClick = { logToDelete = log },
                                                            modifier = Modifier.size(28.dp).testTag("delete_history_item_$idx")
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Delete,
                                                                contentDescription = "Delete record",
                                                                tint = MaterialTheme.colorScheme.error,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    if (isAdminMode) {
                                        Button(
                                            onClick = { showClearAllHistoryDialog = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                            modifier = Modifier.fillMaxWidth().testTag("clear_all_history_button_tab")
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Clear All")
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Clear All History (Admin Only)", fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        Button(
                                            onClick = { showClearMyHistoryDialog = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface),
                                            modifier = Modifier.fillMaxWidth().testTag("clear_my_history_button_tab")
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Clear Mine")
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Clear My History", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        "Reports" -> {
                            val localHistory by viewModel.localHistory.collectAsStateWithLifecycle()
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "FACTORY DOSING ANALYTICS",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.primary,
                                            letterSpacing = 1.2.sp
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text("Total Runs Saved", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                                Text(
                                                    text = "${localHistory.size}",
                                                    fontSize = 32.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("Cloud Link status", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                                Text(
                                                    text = if (isOnline) "ONLINE ●" else "OFFLINE ○",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isOnline) Color(0xFF22C55E) else Color(0xFFEF4444)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "ACTIVE DOSING SLOTS PERFORMANCE",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                val chemicalCategoriesList = listOf(
                                    Triple("ASA Sizing", 0.85f, "12.5 LPM"),
                                    Triple("C-Starch", 0.62f, "30.0 LPH"),
                                    Triple("AKD Sizing", 0.45f, "8.0 LPH"),
                                    Triple("PAC Solution", 0.92f, "15.2 LPH"),
                                    Triple("Alum Solution", 0.73f, "25.0 LPH"),
                                    Triple("Bentonite Slurry", 0.50f, "10.0 LPH")
                                )

                                chemicalCategoriesList.forEach { categoryTriple ->
                                    val (catName, progress, currentRateStr) = categoryTriple
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(text = catName, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                Text(text = currentRateStr, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            LinearProgressIndicator(
                                                progress = progress,
                                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                                color = MaterialTheme.colorScheme.primary,
                                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        "Settings" -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text("⚙", fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                                            Text(
                                                text = "Data Management",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Black,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Divider(color = MaterialTheme.colorScheme.outlineVariant)
                                        Spacer(modifier = Modifier.height(12.dp))

                                        Text(
                                            text = "BACKUP & RESTORE",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            letterSpacing = 1.sp
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    try {
                                                        fileSaverLauncher.launch("chemdose_backup.json")
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                modifier = Modifier.weight(1f).testTag("backup_export_btn"),
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                            ) {
                                                Text("Export Backup", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                            
                                            Button(
                                                onClick = {
                                                    try {
                                                        filePickerLauncher.launch("*/*")
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "Import error: ${e.message}", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                modifier = Modifier.weight(1f).testTag("backup_import_btn"),
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface)
                                            ) {
                                                Text("Import Backup", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(18.dp))
                                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                        Spacer(modifier = Modifier.height(12.dp))

                                        Text(
                                            text = "CLOUD SYNCHRONIZATION",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            letterSpacing = 1.sp
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text("Sync Status:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (isOnline) Color(0xFF22C55E) else Color(0xFFEF4444)))
                                                Text(
                                                    text = if (isOnline) "Online ●" else "Offline ○",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isOnline) Color(0xFF22C55E) else Color(0xFFEF4444)
                                                )
                                            }
                                            
                                            TextButton(
                                                onClick = {
                                                    viewModel.syncNow { }
                                                    Toast.makeText(context, "Starting Cloud Synchronization...", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.testTag("sync_now_btn")
                                            ) {
                                                Text("Sync Now ↻", fontWeight = FontWeight.Black, fontSize = 12.sp)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(18.dp))
                                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                        Spacer(modifier = Modifier.height(12.dp))

                                        Text(
                                            text = "ADMIN CONSOLE PROTECTION",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            letterSpacing = 1.sp
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text("Admin Mode Access", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                Text(
                                                    text = if (isAdminMode) "Status: UNLOCKED 👑" else "Status: Locked",
                                                    fontSize = 11.sp,
                                                    color = if (isAdminMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                                )
                                            }
                                            
                                            Switch(
                                                checked = isAdminMode,
                                                onCheckedChange = { checked ->
                                                    if (checked) {
                                                        pendingAdminAction = {
                                                            Toast.makeText(context, "Admin access confirmed!", Toast.LENGTH_SHORT).show()
                                                        }
                                                    } else {
                                                        viewModel.logout()
                                                        Toast.makeText(context, "Logged out of admin console.", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                modifier = Modifier.testTag("admin_mode_switch")
                                            )
                                        }

                                        if (isAdminMode) {
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Button(
                                                onClick = {
                                                    viewModel.logout()
                                                    Toast.makeText(context, "Operator Logged Out", Toast.LENGTH_SHORT).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                                modifier = Modifier.fillMaxWidth().testTag("admin_logout_btn")
                                            ) {
                                                Text("Logout Admin Session", fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                Card(
                                    modifier = Modifier.fillMaxWidth().testTag("appearance_settings_card"),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text("🎨", fontSize = 20.sp)
                                            Text(
                                                text = "Appearance & Theme Preferences",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Black,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                        // 1. Follow System Theme
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                                                Text(
                                                    text = "Follow System Theme",
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 14.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "Follow device's light or dark mode setting automatically.",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                )
                                            }
                                            Switch(
                                                checked = followSystemTheme,
                                                onCheckedChange = { viewModel.setFollowSystemTheme(it) },
                                                modifier = Modifier.testTag("appearance_follow_system_theme")
                                            )
                                        }

                                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                                        // 2. Manual Dark Mode
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                                                Text(
                                                    text = "Manual Dark Mode Theme",
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 14.sp,
                                                    color = if (followSystemTheme) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = if (followSystemTheme) "Disabled because 'Follow System Theme' is active." else "Force dynamic industrial deep slate dark theme manually.",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                                )
                                            }
                                            Switch(
                                                checked = if (followSystemTheme) systemInDark else manualDarkMode,
                                                enabled = !followSystemTheme,
                                                onCheckedChange = { viewModel.toggleDarkMode() },
                                                modifier = Modifier.testTag("appearance_manual_dark_mode")
                                            )
                                        }

                                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                                        // 3. Font Scale
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(
                                                text = "Display Text Sizing Scales (Current: ${fontScale}x)",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            
                                            val fontScales = listOf(
                                                0.85f to "Small (85%)",
                                                1.0f to "Normal (100%)",
                                                1.15f to "Large (115%)",
                                                1.3f to "Max (130%)"
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                fontScales.forEach { (scale, labelStr) ->
                                                    val isSelected = fontScale == scale
                                                    Button(
                                                        onClick = { viewModel.setFontScale(scale) },
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                                        ),
                                                        modifier = Modifier.weight(1f).height(36.dp),
                                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(labelStr.substringBefore(" "), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Card(
                                    modifier = Modifier.fillMaxWidth().testTag("firebase_settings_card"),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text("🔥", fontSize = 20.sp)
                                            Text(
                                                text = "Firebase Firestore Cloud Sync",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Black,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Sync formula slides, chemical configurations, and system logs across your workspace with an offline-first Firebase cloud database.",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                            lineHeight = 16.sp
                                        )

                                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                        // Status Indicators
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = "Cloud Sync (ON/OFF):",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                if (!isAdminMode) {
                                                    Text(
                                                        text = "Admin login required to toggle",
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            }
                                            Switch(
                                                checked = firebaseConfig.isEnabled,
                                                onCheckedChange = { isChecked ->
                                                    viewModel.updateFirebaseConfig(firebaseConfig.copy(isEnabled = isChecked))
                                                },
                                                enabled = isAdminMode,
                                                modifier = Modifier.testTag("firebase_toggle_services")
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Automatic Backup (Auto-Sync):",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Switch(
                                                checked = firebaseConfig.autoSync,
                                                onCheckedChange = { isChecked ->
                                                    viewModel.updateFirebaseConfig(firebaseConfig.copy(autoSync = isChecked))
                                                },
                                                enabled = isAdminMode,
                                                modifier = Modifier.testTag("firebase_toggle_autosync")
                                            )
                                        }

                                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Sync Status:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                                            val statusColor = when (syncStatus) {
                                                "Connected" -> Color(0xFF22C55E)
                                                "Offline" -> Color.Gray
                                                "Connecting..." -> Color(0xFFEAB308)
                                                else -> Color(0xFFEF4444)
                                            }
                                            Text(
                                                text = syncStatus,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = statusColor,
                                                modifier = Modifier.testTag("sync_status_text")
                                            )
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text("Auth Status:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                                            Text(
                                                text = firebaseAuthStatus,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (firebaseUserUid != null) Color(0xFF22C55E) else MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        if (firebaseUserUid != null) {
                                            Text(
                                                text = "User Cloud UID: $firebaseUserUid",
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            )
                                        }

                                        Text(
                                            text = "Using local secure google-services.json configuration.",
                                            fontSize = 11.sp,
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // Operations Buttons Setup
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    viewModel.retrySync()
                                                    Toast.makeText(context, "Retrying sync status...", Toast.LENGTH_SHORT).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                                ),
                                                modifier = Modifier.weight(1.1f).height(40.dp).testTag("firebase_retry_btn")
                                            ) {
                                                Text("Retry Sync", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }

                                            if (firebaseUserUid != null) {
                                                Button(
                                                    onClick = {
                                                        viewModel.signOutFirebase()
                                                        Toast.makeText(context, "Disconnected from Firebase", Toast.LENGTH_SHORT).show()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                                    ),
                                                    modifier = Modifier.weight(0.9f).height(40.dp).testTag("firebase_signout_btn")
                                                ) {
                                                    Text("Disconnect", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = { activeDashboardTab = "Calculator" },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface),
                                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("close_settings_btn")
                                ) {
                                    Text("Close Panel ☑", fontWeight = FontWeight.Bold)
                                }
                            }
      
                }          /* END_REDUNDANT */ }
            }
        }
    }

        // ================== DIALOGS AND MODALS ==================

        // 100 Slides searchable jumping dashboard
        if (showSlidesSearchDialog) {
            var isSmallIconOnlyView by remember { mutableStateOf(false) }

            Dialog(
                onDismissRequest = { showSlidesSearchDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Title
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Calculator Slide Directory",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = { isSmallIconOnlyView = !isSmallIconOnlyView },
                                    modifier = Modifier.size(28.dp).testTag("toggle_small_icon_view")
                                ) {
                                    Icon(
                                        imageVector = if (isSmallIconOnlyView) Icons.Default.Apps else Icons.Default.List,
                                        contentDescription = "Toggle Small Icon View",
                                        tint = if (isSmallIconOnlyView) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            IconButton(onClick = { showSlidesSearchDialog = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        }

                        // Row with Search box and Jump to Slide box
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.updateSearchQuery(it) },
                                placeholder = { Text("Search title, formula...", fontSize = 11.sp) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) },
                                modifier = Modifier
                                    .weight(1.3f)
                                    .testTag("slide_search_input"),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )

                            var jumpPageStr by remember { mutableStateOf("") }
                            OutlinedTextField(
                                value = jumpPageStr,
                                onValueChange = { inputVal ->
                                    if (inputVal.length <= 3) {
                                        val cleanVal = inputVal.filter { it.isDigit() }
                                        jumpPageStr = cleanVal
                                        val targetPageNum = cleanVal.toIntOrNull()
                                        if (targetPageNum != null && targetPageNum >= 1) {
                                            viewModel.selectSlide(targetPageNum)
                                            showSlidesSearchDialog = false
                                        }
                                    }
                                },
                                label = { Text("Go to Pg #", fontSize = 8.sp) },
                                placeholder = { Text("ID", fontSize = 10.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .weight(0.7f)
                                    .testTag("jump_slide_input"),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Category tabs scrollable selector
                        val categories = listOf("All", "Chemical Dosage", "GSM & Dimension", "Consistency", "Production", "Stock Flow & Dilution", "Custom")
                        ScrollableTabRow(
                            selectedTabIndex = categories.indexOf(selectedCategory).coerceAtLeast(0),
                            edgePadding = 0.dp,
                            containerColor = Color.Transparent,
                            divider = {}
                        ) {
                            categories.forEach { cat ->
                                Tab(
                                    selected = selectedCategory == cat,
                                    onClick = { viewModel.selectCategory(cat) },
                                    text = { Text(cat, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Grid List of Slides (1 to 100) - Compact 2-Column or Adaptive Icon Grid
                        val columnCount = if (isSmallIconOnlyView) GridCells.Adaptive(minSize = 64.dp) else GridCells.Fixed(2)
                        LazyVerticalGrid(
                            columns = columnCount,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(filteredSlides) { item ->
                                val isActive = item.slide.id == activeSlideId
                                if (isSmallIconOnlyView) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isActive) MaterialTheme.colorScheme.primaryContainer 
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable {
                                                viewModel.selectSlide(item.slide.id)
                                                showSlidesSearchDialog = false
                                            }
                                            .padding(6.dp)
                                            .aspectRatio(1f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            val categoryEmoji = when (item.slide.category) {
                                                "Chemical Dosage" -> "🧪"
                                                "GSM & Dimension" -> "📐"
                                                "Consistency" -> "💧"
                                                "Production" -> "🏭"
                                                "Stock Flow & Dilution" -> "🌊"
                                                "Custom" -> "🛠️"
                                                else -> "📝"
                                            }
                                            Text(categoryEmoji, fontSize = 20.sp)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "PAGE ${item.slide.id}",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (isActive) MaterialTheme.colorScheme.primaryContainer 
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable {
                                                viewModel.selectSlide(item.slide.id)
                                                showSlidesSearchDialog = false
                                            }
                                            .padding(10.dp)
                                            .heightIn(min = 72.dp),
                                        contentAlignment = Alignment.TopStart
                                    ) {
                                        Column {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "PAGE ${item.slide.id}",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                                )

                                                if (isAdminMode) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        IconButton(
                                                            onClick = {
                                                                viewModel.swapSlides(item.slide.id, item.slide.id - 1)
                                                            },
                                                            modifier = Modifier.size(22.dp)
                                                        ) {
                                                            Text(
                                                                text = "▲",
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.primary
                                                            )
                                                        }
                                                        IconButton(
                                                            onClick = {
                                                                viewModel.swapSlides(item.slide.id, item.slide.id + 1)
                                                            },
                                                            modifier = Modifier.size(22.dp)
                                                        ) {
                                                            Text(
                                                                text = "▼",
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.primary
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = item.slide.title,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = item.slide.category,
                                                fontSize = 8.sp,
                                                maxLines = 1,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
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

        // Backup and Restore Management modal
        if (showBackupRestoreDialog) {
            var blueprintJsonText by remember { mutableStateOf("") }
            var blueprintStatusMsg by remember { mutableStateOf("") }
            var blueprintIsSuccess by remember { mutableStateOf(true) }

            var changePwdCurrent by remember { mutableStateOf("") }
            var changePwdNew by remember { mutableStateOf("") }
            var changePwdConfirm by remember { mutableStateOf("") }
            var changePwdErrorMsg by remember { mutableStateOf<String?>(null) }
            var changePwdSuccessMsg by remember { mutableStateOf<String?>(null) }

            // Nested section state controller inside Settings Dialog
            // Possibilities: null (main settings panel list), "account", "formula_settings", "flow_and_unit", "admin_panel", "data_mgmt", "appearance", "ai_assistant", "about"

            // Text field for custom added flow unit
            var customUnitInputInput by remember { mutableStateOf("") }

            // Admin password unlock field inside settings (for convenience)
            var adminUnlockPasswordInput by remember { mutableStateOf("") }
            var adminUnlockErrorMsg by remember { mutableStateOf<String?>(null) }

            AlertDialog(
                onDismissRequest = { 
                    showBackupRestoreDialog = false 
                    currentSubScreen = null
                },
                properties = DialogProperties(usePlatformDefaultWidth = false),
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.92f)
                    .padding(8.dp),
                containerColor = Color(0xFFF0F0F3),
                title = { 
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when (currentSubScreen) {
                                    "account" -> Icons.Default.Person
                                    "formula_settings" -> Icons.Default.Build
                                    "flow_and_unit" -> Icons.Default.List
                                    "admin_panel" -> Icons.Default.Lock
                                    "data_mgmt" -> Icons.Default.Cloud
                                    "appearance" -> Icons.Default.Settings
                                    "ai_assistant" -> Icons.Default.Face
                                    "about" -> Icons.Default.Info
                                    else -> Icons.Default.Settings
                                },
                                contentDescription = null, 
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = when (currentSubScreen) {
                                    "account" -> "👤 Account Settings"
                                    "formula_settings" -> "🧪 Formula Settings"
                                    "flow_and_unit" -> "📊 Flow & Unit Settings"
                                    "admin_panel" -> "🔒 Admin Panel"
                                    "data_mgmt" -> "💾 Data Management"
                                    "appearance" -> "🎨 Appearance Settings"
                                    "ai_assistant" -> "🤖 AI Assistant"
                                    "about" -> "ℹ️ About"
                                    else -> "⚙️ SETTINGS"
                                },
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }

                        if (currentSubScreen != null) {
                            TextButton(
                                onClick = { currentSubScreen = null },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack, 
                                    contentDescription = "Back", 
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Back", 
                                    fontSize = 20.sp, 
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Text(
                            text = "Manage remote Google Drive backups, manual cloud sync triggers, local document streams, and AES-128 cryptographic encryption.",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF334155)
                        )

                        if (currentSubScreen == null) {
                            Text(
                                text = "ChemDose Formula Configuration Hub",
                                color = Color(0xFF475569),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )

                            // 1. Account Category Card
                            NeumorphicCard(
                                onClick = { currentSubScreen = "account" },
                                modifier = Modifier.fillMaxWidth().testTag("setting_cat_account"),
                                cornerRadius = 16.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(
                                            imageVector = Icons.Default.Person, 
                                            contentDescription = null, 
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text(
                                                text = "👥 Account & Session", 
                                                fontWeight = FontWeight.SemiBold, 
                                                fontSize = 24.sp,
                                                color = Color(0xFF0F172A)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Admin session status, password management", 
                                                fontSize = 18.sp, 
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF334155)
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward, 
                                        contentDescription = null, 
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }

                            // 2. Formula Settings
                            NeumorphicCard(
                                onClick = { currentSubScreen = "formula_settings" },
                                modifier = Modifier.fillMaxWidth().testTag("setting_cat_formula"),
                                cornerRadius = 16.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(
                                            imageVector = Icons.Default.Build, 
                                            contentDescription = null, 
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text(
                                                text = "🧪 Formula Settings", 
                                                fontWeight = FontWeight.SemiBold, 
                                                fontSize = 24.sp,
                                                color = Color(0xFF0F172A)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Decimal precision, default outputs, and calculation mode", 
                                                fontSize = 18.sp, 
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF334155)
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward, 
                                        contentDescription = null, 
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }

                            // 3. Flow & Unit settings (Hidden from normal users, visible to Admin)
                            if (isAdminMode) {
                                NeumorphicCard(
                                    onClick = { currentSubScreen = "flow_and_unit" },
                                    modifier = Modifier.fillMaxWidth().testTag("setting_cat_unit"),
                                    cornerRadius = 16.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp).fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Icon(
                                                imageVector = Icons.Default.List, 
                                                contentDescription = null, 
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(32.dp)
                                            )
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Column {
                                                Text(
                                                    text = "📊 Flow & Unit Settings", 
                                                    fontWeight = FontWeight.SemiBold, 
                                                    fontSize = 24.sp,
                                                    color = Color(0xFF0F172A)
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Manage standard volumetric metrics & add custom units", 
                                                    fontSize = 18.sp, 
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color(0xFF334155)
                                                )
                                            }
                                        }
                                        Icon(
                                            imageVector = Icons.Default.ArrowForward, 
                                            contentDescription = null, 
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                }
                            }

                            // 4. Admin Panel
                            if (isAdminMode) {
                                NeumorphicCard(
                                    onClick = { currentSubScreen = "admin_panel" },
                                    modifier = Modifier.fillMaxWidth().testTag("setting_cat_admin"),
                                    cornerRadius = 16.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp).fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Icon(
                                                imageVector = Icons.Default.Lock, 
                                                contentDescription = null, 
                                                tint = if (isAdminMode) Color(0xFF22C55E) else MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(32.dp)
                                            )
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Column {
                                                Text(
                                                    text = "🔒 Admin Panel", 
                                                    fontWeight = FontWeight.SemiBold, 
                                                    fontSize = 24.sp,
                                                    color = Color(0xFF0F172A)
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Authenticate, set system connections, manage roles", 
                                                    fontSize = 18.sp, 
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color(0xFF334155)
                                                )
                                            }
                                        }
                                        Icon(
                                            imageVector = Icons.Default.ArrowForward, 
                                            contentDescription = null, 
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                }
                            }

                            // 5. Data Management
                            if (isAdminMode) {
                                NeumorphicCard(
                                    onClick = { currentSubScreen = "data_mgmt" },
                                    modifier = Modifier.fillMaxWidth().testTag("setting_cat_data"),
                                    cornerRadius = 16.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp).fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Icon(
                                                imageVector = Icons.Default.Cloud, 
                                                contentDescription = null, 
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(32.dp)
                                            )
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Column {
                                                Text(
                                                    text = "💾 Data Management", 
                                                    fontWeight = FontWeight.SemiBold, 
                                                    fontSize = 24.sp,
                                                    color = Color(0xFF0F172A)
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Backups, restoring from Firebase, and offline payloads", 
                                                    fontSize = 18.sp, 
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color(0xFF334155)
                                                )
                                            }
                                        }
                                        Icon(
                                            imageVector = Icons.Default.ArrowForward, 
                                            contentDescription = null, 
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                }
                            }

                            // 6. Appearance settings
                            NeumorphicCard(
                                onClick = { currentSubScreen = "appearance" },
                                modifier = Modifier.fillMaxWidth().testTag("setting_cat_appearance"),
                                cornerRadius = 16.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(
                                            imageVector = Icons.Default.Settings, 
                                            contentDescription = null, 
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text(
                                                text = "🎨 Appearance", 
                                                fontWeight = FontWeight.SemiBold, 
                                                fontSize = 24.sp,
                                                color = Color(0xFF0F172A)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Display scale, typography size preferences", 
                                                fontSize = 18.sp, 
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF334155)
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward, 
                                        contentDescription = null, 
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }

                            // 7. AI Assistant settings
                            NeumorphicCard(
                                onClick = { currentSubScreen = "ai_assistant" },
                                modifier = Modifier.fillMaxWidth().testTag("setting_cat_ai"),
                                cornerRadius = 16.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(
                                            imageVector = Icons.Default.Face, 
                                            contentDescription = null, 
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text(
                                                text = "🤖 AI Assistant", 
                                                fontWeight = FontWeight.SemiBold, 
                                                fontSize = 24.sp,
                                                color = Color(0xFF0F172A)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "OpenAI keys, automated chemical dosage help", 
                                                fontSize = 18.sp, 
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF334155)
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward, 
                                        contentDescription = null, 
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }

                            // 8. About panel
                            NeumorphicCard(
                                onClick = { currentSubScreen = "about" },
                                modifier = Modifier.fillMaxWidth().testTag("setting_cat_about"),
                                cornerRadius = 16.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(
                                            imageVector = Icons.Default.Info, 
                                            contentDescription = null, 
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text(
                                                text = "ℹ️ About", 
                                                fontWeight = FontWeight.SemiBold, 
                                                fontSize = 24.sp,
                                                color = Color(0xFF0F172A)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "App version, telemetry references, and contact details", 
                                                fontSize = 18.sp, 
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF334155)
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward, 
                                        contentDescription = null, 
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                        } else {
                            // Subscreen contents
                            when (currentSubScreen) {
                                "account" -> {
                                    Text(
                                        text = "Manage details and check session validity below.", 
                                        fontSize = 18.sp, 
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF334155)
                                    )
                                    
                                    NeumorphicCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        cornerRadius = 16.dp
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "Session Status", 
                                                        fontWeight = FontWeight.SemiBold, 
                                                        fontSize = 24.sp,
                                                        color = Color(0xFF0F172A)
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = if (isAdminMode) "👑 Admin Mode Active ($remainingTimerStr)" else "🔒 User Mode (Read-Only)",
                                                        color = if (isAdminMode) MaterialTheme.colorScheme.primary else Color(0xFF64748B),
                                                        fontSize = 18.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                                if (isAdminMode) {
                                                    TextButton(
                                                        onClick = { viewModel.setAdminMode(false) },
                                                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                                        modifier = Modifier.minimumInteractiveComponentSize()
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.ExitToApp, 
                                                            contentDescription = "Log Out", 
                                                            modifier = Modifier.size(32.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            text = "Log Out", 
                                                            fontSize = 20.sp, 
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (isAdminMode) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Change Security Password", 
                                            fontWeight = FontWeight.SemiBold, 
                                            fontSize = 24.sp,
                                            color = Color(0xFF0F172A)
                                        )
                                        NeumorphicCard(
                                            modifier = Modifier.fillMaxWidth(),
                                            cornerRadius = 16.dp
                                        ) {
                                            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                                OutlinedTextField(
                                                    value = changePwdCurrent,
                                                    onValueChange = { 
                                                        changePwdCurrent = it
                                                        changePwdErrorMsg = null
                                                     },
                                                    label = { Text("Current Password", fontSize = 18.sp, fontWeight = FontWeight.Medium) },
                                                    visualTransformation = PasswordVisualTransformation(),
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                                    modifier = Modifier.fillMaxWidth().height(64.dp),
                                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp),
                                                    singleLine = true
                                                )

                                                OutlinedTextField(
                                                    value = changePwdNew,
                                                    onValueChange = { 
                                                        changePwdNew = it
                                                        changePwdErrorMsg = null
                                                    },
                                                    label = { Text("New Password (must be numeric)", fontSize = 18.sp, fontWeight = FontWeight.Medium) },
                                                    visualTransformation = PasswordVisualTransformation(),
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                                    modifier = Modifier.fillMaxWidth().height(64.dp),
                                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp),
                                                    singleLine = true
                                                )

                                                OutlinedTextField(
                                                    value = changePwdConfirm,
                                                    onValueChange = { 
                                                        changePwdConfirm = it
                                                        changePwdErrorMsg = null
                                                    },
                                                    label = { Text("Confirm New Password", fontSize = 18.sp, fontWeight = FontWeight.Medium) },
                                                    visualTransformation = PasswordVisualTransformation(),
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                                    modifier = Modifier.fillMaxWidth().height(64.dp),
                                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp),
                                                    singleLine = true
                                                )

                                                if (changePwdErrorMsg != null) {
                                                    Text(
                                                        text = changePwdErrorMsg!!,
                                                        color = MaterialTheme.colorScheme.error,
                                                        fontSize = 18.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                if (changePwdSuccessMsg != null) {
                                                    Text(
                                                        text = changePwdSuccessMsg!!,
                                                        color = Color(0xFF22C55E),
                                                        fontSize = 18.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }

                                                NeumorphicButton(
                                                    onClick = {
                                                        if (changePwdCurrent != adminPassword) {
                                                            changePwdErrorMsg = "Incorrect Current Password!"
                                                        } else if (changePwdNew.isEmpty()) {
                                                            changePwdErrorMsg = "New Password cannot be empty!"
                                                        } else if (changePwdNew != changePwdConfirm) {
                                                            changePwdErrorMsg = "New Passwords do not match!"
                                                        } else {
                                                            viewModel.changeAdminPassword(changePwdNew)
                                                            changePwdSuccessMsg = "Admin Password updated successfully!"
                                                            changePwdCurrent = ""
                                                            changePwdNew = ""
                                                            changePwdConfirm = ""
                                                            changePwdErrorMsg = null
                                                        }
                                                    },
                                                    cornerRadius = 16.dp,
                                                    modifier = Modifier.fillMaxWidth().height(56.dp).testTag("change_pwd_submit_btn")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Lock, 
                                                        contentDescription = "Change Password", 
                                                        modifier = Modifier.size(28.dp),
                                                        tint = Color(0xFF0F172A)
                                                    )
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Text("Change Admin Password", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                                }
                                            }
                                        }
                                    } else {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(18.dp).fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Warning, 
                                                    contentDescription = null, 
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(32.dp)
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    text = "Unlock administrator mode to view password specifications, configure secure custom cloud environments, or modify chemical formula lists.",
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onErrorContainer
                                                )
                                            }
                                        }
                                    }
                                }
                                "formula_settings" -> {
                                    Text(
                                        text = "Modify decimal places, default flow rate selector options, and automation mechanics parameters.", 
                                        fontSize = 18.sp, 
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF334155)
                                    )

                                    // DECIMAL PLACES SLIDER / SELECTOR
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Decimal Precision Indicator", 
                                        fontWeight = FontWeight.SemiBold, 
                                        fontSize = 24.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                    NeumorphicCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        cornerRadius = 16.dp
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Text(
                                                text = "Set display places for results: Currently $decimalPlaces", 
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF334155)
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                (0..4).forEach { option ->
                                                    val isSel = decimalPlaces == option
                                                    NeumorphicButton(
                                                        onClick = { viewModel.setDecimalPlaces(option) },
                                                        cornerRadius = 10.dp,
                                                        containerColor = if (isSel) MaterialTheme.colorScheme.primary else Color(0xFFF0F0F3),
                                                        contentColor = if (isSel) MaterialTheme.colorScheme.onPrimary else Color(0xFF0F172A),
                                                        modifier = Modifier.weight(1f).height(56.dp).testTag("decimal_btn_$option")
                                                    ) {
                                                        Text("$option d", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // DEFAULT FLOW SELECTION
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Default Volumetric Unit", 
                                        fontWeight = FontWeight.SemiBold, 
                                        fontSize = 24.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                    NeumorphicCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        cornerRadius = 16.dp
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            val defaultUnitOptions = listOf("LPH", "LPM", "m³/hr", "GPH")
                                            defaultUnitOptions.forEach { option ->
                                                val isSelected = defaultFlowUnit == option
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .minimumInteractiveComponentSize()
                                                        .clickable { viewModel.updateDefaultFlowUnit(option) }
                                                        .padding(vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Checkbox(
                                                        checked = isSelected,
                                                        onCheckedChange = { viewModel.updateDefaultFlowUnit(option) },
                                                        modifier = Modifier.size(32.dp).testTag("default_unit_checkbox_$option")
                                                    )
                                                    Spacer(modifier = Modifier.width(16.dp))
                                                    Text(
                                                        text = option, 
                                                        fontSize = 18.sp, 
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        color = Color(0xFF0F172A)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // AUTO CALCULATION TOGGLE
                                    Spacer(modifier = Modifier.height(6.dp))
                                    NeumorphicCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        cornerRadius = 16.dp
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp).fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                                                Text(
                                                    text = "Real-Time Calculation", 
                                                    fontWeight = FontWeight.SemiBold, 
                                                    fontSize = 24.sp,
                                                    color = Color(0xFF0F172A)
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Formulas recalculate automatically as numbers change", 
                                                    fontSize = 18.sp, 
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color(0xFF334155)
                                                )
                                            }
                                            Switch(
                                                checked = autoCalculation,
                                                onCheckedChange = { viewModel.setAutoCalculation(it) },
                                                modifier = Modifier.size(48.dp).testTag("auto_calc_switch")
                                            )
                                        }
                                    }
                                }
                                "flow_and_unit" -> {
                                    Text(
                                        text = "Manage system registries of allowed volumetric units.", 
                                        fontSize = 18.sp, 
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF334155)
                                    )

                                    // PREDEFINED FLOW UNITS DISPLAY
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Standard Built-In Volumetric Units", 
                                        fontWeight = FontWeight.SemiBold, 
                                        fontSize = 24.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                    NeumorphicCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        cornerRadius = 16.dp
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            val stdUnits = listOf("LPH", "LPM", "m³/hr", "GPH")
                                            stdUnits.forEach { unit ->
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            imageVector = Icons.Default.CheckCircle, 
                                                            contentDescription = null, 
                                                            tint = Color(0xFF22C55E), 
                                                            modifier = Modifier.size(32.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(16.dp))
                                                        Text(
                                                            text = "$unit (Predefined Standard Unit)", 
                                                            fontSize = 18.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = Color(0xFF0F172A)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // USER DEFINED UNITS SETUP
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Custom Volumetric Units", 
                                        fontWeight = FontWeight.SemiBold, 
                                        fontSize = 24.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                    NeumorphicCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        cornerRadius = 16.dp
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                            if (userDefinedUnits.isEmpty()) {
                                                Text(
                                                    text = "No custom units defined yet.", 
                                                    fontSize = 18.sp, 
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color(0xFF64748B)
                                                )
                                            } else {
                                                userDefinedUnits.forEach { unit ->
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                            Icon(
                                                                imageVector = Icons.Default.Info, 
                                                                contentDescription = null, 
                                                                tint = MaterialTheme.colorScheme.primary, 
                                                                modifier = Modifier.size(32.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(16.dp))
                                                            Text(
                                                                text = unit, 
                                                                fontSize = 18.sp, 
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color(0xFF0F172A)
                                                            )
                                                        }
                                                        Row {
                                                            IconButton(
                                                                onClick = { 
                                                                    unitBeingEdited = unit
                                                                    unitEditInput = unit
                                                                },
                                                                modifier = Modifier.size(48.dp).testTag("edit_unit_$unit")
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Edit, 
                                                                    contentDescription = "Edit Unit", 
                                                                    tint = MaterialTheme.colorScheme.primary, 
                                                                    modifier = Modifier.size(32.dp)
                                                                )
                                                            }
                                                            IconButton(
                                                                onClick = { viewModel.deleteUnit(unit) },
                                                                modifier = Modifier.size(48.dp).testTag("delete_unit_$unit")
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Delete, 
                                                                    contentDescription = "Delete Unit", 
                                                                    tint = MaterialTheme.colorScheme.error, 
                                                                    modifier = Modifier.size(32.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            if (unitBeingEdited != null) {
                                                AlertDialog(
                                                    onDismissRequest = { unitBeingEdited = null },
                                                    title = { Text("Edit Volumetric Unit", fontWeight = FontWeight.Bold) },
                                                    text = {
                                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                            Text("Rename this unit globally. It will automatically update all linking variables and formulas with the new unit symbol.", fontSize = 12.sp)
                                                            OutlinedTextField(
                                                                value = unitEditInput,
                                                                onValueChange = { unitEditInput = it },
                                                                label = { Text("Unit Symbol") },
                                                                singleLine = true,
                                                                modifier = Modifier.fillMaxWidth().testTag("edit_unit_input_dialog")
                                                            )
                                                        }
                                                    },
                                                    confirmButton = {
                                                        Button(
                                                            onClick = {
                                                                val old = unitBeingEdited ?: ""
                                                                if (old.isNotEmpty() && unitEditInput.isNotEmpty()) {
                                                                    viewModel.editUnit(old, unitEditInput)
                                                                }
                                                                unitBeingEdited = null
                                                            }
                                                        ) {
                                                            Text("Save")
                                                        }
                                                    },
                                                    dismissButton = {
                                                        TextButton(onClick = { unitBeingEdited = null }) {
                                                            Text("Cancel")
                                                        }
                                                    }
                                                )
                                            }

                                            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 8.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                OutlinedTextField(
                                                    value = customUnitInputInput,
                                                    onValueChange = { customUnitInputInput = it },
                                                    placeholder = { Text("e.g. gpm, lps", fontSize = 18.sp, fontWeight = FontWeight.Normal) },
                                                    modifier = Modifier.weight(1f).height(64.dp).testTag("custom_unit_input"),
                                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp),
                                                    singleLine = true
                                                )
                                                NeumorphicButton(
                                                    onClick = {
                                                        if (customUnitInputInput.isNotBlank()) {
                                                            viewModel.addUserDefinedUnit(customUnitInputInput)
                                                            customUnitInputInput = ""
                                                        }
                                                    },
                                                    cornerRadius = 12.dp,
                                                    modifier = Modifier.height(56.dp).width(120.dp).testTag("add_unit_button")
                                                ) {
                                                    Text("Add", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                                }
                                            }
                                        }
                                    }
                                }
                                "admin_panel" -> {
                                    Text(
                                        text = "Secure configuration interface. Only authenticated administrator level parameters are adjustable.", 
                                        fontSize = 18.sp, 
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF334155)
                                    )

                                    if (!isAdminMode) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        NeumorphicCard(
                                            modifier = Modifier.fillMaxWidth(),
                                            cornerRadius = 16.dp
                                        ) {
                                            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                                Text(
                                                    text = "Unlock Admin Credentials", 
                                                    fontWeight = FontWeight.SemiBold, 
                                                    fontSize = 24.sp,
                                                    color = Color(0xFF0F172A)
                                                )
                                                OutlinedTextField(
                                                    value = adminUnlockPasswordInput,
                                                    onValueChange = { 
                                                        adminUnlockPasswordInput = it
                                                        adminUnlockErrorMsg = null
                                                    },
                                                    label = { Text("Enter Admin Password", fontSize = 18.sp, fontWeight = FontWeight.Medium) },
                                                    visualTransformation = PasswordVisualTransformation(),
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                                    modifier = Modifier.fillMaxWidth().height(64.dp).testTag("admin_panel_unlock_input"),
                                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp),
                                                    singleLine = true
                                                )
                                                if (adminUnlockErrorMsg != null) {
                                                    Text(
                                                        text = adminUnlockErrorMsg!!, 
                                                        color = MaterialTheme.colorScheme.error, 
                                                        fontSize = 18.sp, 
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                NeumorphicButton(
                                                    onClick = {
                                                        if (adminUnlockPasswordInput == adminPassword) {
                                                            viewModel.setAdminMode(true)
                                                            adminUnlockPasswordInput = ""
                                                            adminUnlockErrorMsg = null
                                                        } else {
                                                            adminUnlockErrorMsg = "Incorrect admin password!"
                                                        }
                                                    },
                                                    cornerRadius = 12.dp,
                                                    containerColor = MaterialTheme.colorScheme.error,
                                                    contentColor = MaterialTheme.colorScheme.onError,
                                                    modifier = Modifier.fillMaxWidth().height(56.dp).testTag("admin_panel_unlock_button")
                                                ) {
                                                    Text("Unlock Privileges", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                }
                                            }
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        NeumorphicCard(
                                            modifier = Modifier.fillMaxWidth(),
                                            cornerRadius = 16.dp
                                        ) {
                                            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                                Text(
                                                    text = "User Role Authorization Mode Selection", 
                                                    fontWeight = FontWeight.SemiBold, 
                                                    fontSize = 24.sp,
                                                    color = Color(0xFF0F172A)
                                                )
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    val isOpSelected = !isAdminMode
                                                    NeumorphicButton(
                                                        onClick = { viewModel.loginAsRole("Operator") },
                                                        cornerRadius = 10.dp,
                                                        containerColor = if (isOpSelected) MaterialTheme.colorScheme.secondary else Color(0xFFF0F0F3),
                                                        contentColor = if (isOpSelected) MaterialTheme.colorScheme.onSecondary else Color(0xFF0F172A),
                                                        modifier = Modifier.weight(1f).height(56.dp)
                                                    ) {
                                                        Text("Set Operator (Read-Only)", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                    val isAdminSelected = isAdminMode
                                                    NeumorphicButton(
                                                        onClick = { viewModel.loginAsRole("Admin") },
                                                        cornerRadius = 10.dp,
                                                        containerColor = if (isAdminSelected) MaterialTheme.colorScheme.primary else Color(0xFFF0F0F3),
                                                        contentColor = if (isAdminSelected) MaterialTheme.colorScheme.onPrimary else Color(0xFF0F172A),
                                                        modifier = Modifier.weight(1f).height(56.dp)
                                                    ) {
                                                        Text("Set Admin (Master Mode)", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }

                                        // Firestore credentials card from old drawers
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Firebase API Connection Settings", 
                                            fontWeight = FontWeight.SemiBold, 
                                            fontSize = 24.sp,
                                            color = Color(0xFF0F172A)
                                        )
                                        NeumorphicCard(
                                            modifier = Modifier.fillMaxWidth(),
                                            cornerRadius = 16.dp
                                        ) {
                                            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {








                                                 Row(
                                                     modifier = Modifier.fillMaxWidth(),
                                                     verticalAlignment = Alignment.CenterVertically
                                                 ) {
                                                     Checkbox(
                                                         checked = firebaseConfig.isEnabled,
                                                         onCheckedChange = { isChecked ->
                                                             viewModel.updateFirebaseConfig(firebaseConfig.copy(isEnabled = isChecked))
                                                         },
                                                         modifier = Modifier.size(32.dp).testTag("admin_firebase_enable_checkbox")
                                                     )
                                                     Spacer(modifier = Modifier.width(16.dp))
                                                     Text(
                                                         text = "Cloud Sync (ON/OFF)", 
                                                         fontSize = 18.sp, 
                                                         fontWeight = FontWeight.Medium,
                                                         color = Color(0xFF0F172A)
                                                     )
                                                 }

                                                 Row(
                                                     modifier = Modifier.fillMaxWidth(),
                                                     verticalAlignment = Alignment.CenterVertically
                                                 ) {
                                                     Checkbox(
                                                         checked = firebaseConfig.autoSync,
                                                         onCheckedChange = { isChecked ->
                                                             viewModel.updateFirebaseConfig(firebaseConfig.copy(autoSync = isChecked))
                                                         },
                                                         modifier = Modifier.size(32.dp).testTag("admin_firebase_autosync_checkbox")
                                                     )
                                                     Spacer(modifier = Modifier.width(16.dp))
                                                     Text(
                                                         text = "Automatic Backup (Auto-Sync)", 
                                                         fontSize = 18.sp, 
                                                         fontWeight = FontWeight.Medium,
                                                         color = Color(0xFF0F172A)
                                                     )
                                                 }

                                                 Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                                 Row(
                                                     verticalAlignment = Alignment.CenterVertically,
                                                     horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                     modifier = Modifier.fillMaxWidth()
                                                 ) {
                                                     Text("Sync Status:", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A))
                                                     val statusColor = when (syncStatus) {
                                                         "Connected" -> Color(0xFF22C55E)
                                                         "Offline" -> Color.Gray
                                                         "Connecting..." -> Color(0xFFEAB308)
                                                         else -> Color(0xFFEF4444)
                                                     }
                                                     Text(
                                                         text = syncStatus,
                                                         fontSize = 16.sp,
                                                         fontWeight = FontWeight.Bold,
                                                         color = statusColor
                                                     )
                                                 }

                                                 Row(
                                                     verticalAlignment = Alignment.CenterVertically,
                                                     horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                 ) {
                                                     Text("Auth Status:", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A))
                                                     Text(
                                                         text = firebaseAuthStatus,
                                                         fontSize = 16.sp,
                                                         fontWeight = FontWeight.Bold,
                                                         color = if (firebaseUserUid != null) Color(0xFF22C55E) else MaterialTheme.colorScheme.primary
                                                     )
                                                 }

                                                 if (firebaseUserUid != null) {
                                                     Text(
                                                         text = "UID: $firebaseUserUid",
                                                         fontSize = 12.sp,
                                                         fontFamily = FontFamily.Monospace,
                                                         color = Color.Gray
                                                     )
                                                 }

                                                 Text(
                                                     text = "Using local secure google-services.json configuration.",
                                                     fontSize = 12.sp,
                                                     fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                                     color = Color.Gray
                                                 )

                                                 Spacer(modifier = Modifier.height(4.dp))

                                                 NeumorphicButton(
                                                     onClick = {
                                                         viewModel.retrySync()
                                                     },
                                                     containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                     contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                                     cornerRadius = 10.dp,
                                                     modifier = Modifier.fillMaxWidth().height(52.dp).testTag("admin_firebase_retry_btn")
                                                 ) {
                                                     Text("Retry Sync Connection", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                                 }

                                                if (firebaseUserUid != null) {
                                                    NeumorphicButton(
                                                        onClick = { viewModel.signOutFirebase() },
                                                        containerColor = MaterialTheme.colorScheme.error,
                                                        contentColor = MaterialTheme.colorScheme.onError,
                                                        cornerRadius = 10.dp,
                                                        modifier = Modifier.fillMaxWidth().height(56.dp)
                                                    ) {
                                                        Text("Log Out Connection", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                "data_mgmt" -> {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(20.dp)
                                    ) {
                                        // 1. BACKUP & RESTORE SECTION
                                        NeumorphicCard(
                                            modifier = Modifier.fillMaxWidth().testTag("backup_restore_card"),
                                            cornerRadius = 16.dp
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Text(
                                                    text = "[ Backup & Restore ]",
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    textAlign = TextAlign.Center
                                                )
                                                
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    NeumorphicButton(
                                                        onClick = {
                                                            viewModel.exportBackup { success, message ->
                                                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                                            }
                                                        },
                                                        modifier = Modifier.weight(1f).testTag("button_export"),
                                                        cornerRadius = 12.dp
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.CloudUpload,
                                                            contentDescription = "Export backup",
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text("Export", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                                    }

                                                    NeumorphicButton(
                                                        onClick = {
                                                            filePickerLauncher.launch("application/json")
                                                        },
                                                        modifier = Modifier.weight(1f).testTag("button_import"),
                                                        cornerRadius = 12.dp
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.CloudDownload,
                                                            contentDescription = "Import backup",
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text("Import", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }

                                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                                        // 2. CLOUD SYNC SECTION
                                        NeumorphicCard(
                                            modifier = Modifier.fillMaxWidth().testTag("cloud_sync_card"),
                                            cornerRadius = 16.dp
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Text(
                                                    text = "[ Cloud Sync ]",
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    textAlign = TextAlign.Center
                                                )

                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text(
                                                        text = if (isOnline) "Status: Online " else "Status: Offline ",
                                                        fontSize = 15.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .size(10.dp)
                                                            .clip(CircleShape)
                                                            .background(if (isOnline) Color(0xFF22C55E) else Color(0xFFEF4444))
                                                    )
                                                }

                                                NeumorphicButton(
                                                    onClick = {
                                                        if (isAdminMode) {
                                                            viewModel.syncNow { finalReport ->
                                                                Toast.makeText(context, finalReport.message, Toast.LENGTH_LONG).show()
                                                            }
                                                        } else {
                                                            Toast.makeText(context, "Sync is restricted to Admin users. Please activate Admin mode.", Toast.LENGTH_LONG).show()
                                                        }
                                                    },
                                                    enabled = !isSyncing,
                                                    modifier = Modifier.fillMaxWidth().testTag("button_sync_now"),
                                                    cornerRadius = 12.dp
                                                ) {
                                                    if (isSyncing) {
                                                        CircularProgressIndicator(
                                                            modifier = Modifier.size(18.dp),
                                                            strokeWidth = 2.dp,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text("Syncing...", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                                    } else {
                                                        Icon(
                                                            imageVector = Icons.Default.Refresh,
                                                            contentDescription = "Sync Now",
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text("Sync Now", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }

                                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                                        // 3. ADMIN SECTION
                                        NeumorphicCard(
                                            modifier = Modifier.fillMaxWidth().testTag("admin_card"),
                                            cornerRadius = 16.dp
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Text(
                                                    text = "[ Admin ]",
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    textAlign = TextAlign.Center
                                                )

                                                Text(
                                                    text = if (isAdminMode) "Admin Mode: ON" else "Admin Mode: OFF",
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isAdminMode) Color(0xFF22C55E) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                )

                                                NeumorphicButton(
                                                    onClick = {
                                                        if (isAdminMode) {
                                                            viewModel.logout()
                                                            Toast.makeText(context, "Logged out of Admin Mode", Toast.LENGTH_SHORT).show()
                                                        } else {
                                                            Toast.makeText(context, "You are already logged out", Toast.LENGTH_SHORT).show()
                                                        }
                                                    },
                                                    enabled = isAdminMode,
                                                    modifier = Modifier.fillMaxWidth().testTag("button_logout"),
                                                    cornerRadius = 12.dp
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.ExitToApp,
                                                        contentDescription = "Logout",
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Logout", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }

                                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                                        // 4. CLOSE ACTION
                                        NeumorphicButton(
                                            onClick = {
                                                showBackupRestoreDialog = false
                                                currentSubScreen = null
                                            },
                                            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("button_close_data_mgmt"),
                                            cornerRadius = 12.dp,
                                            containerColor = MaterialTheme.colorScheme.errorContainer,
                                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Close Data Management",
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Close", fontSize = 16.sp, fontWeight = FontWeight.Black)
                                        }
                                    }
                                }
                                "data_mgmt_old" -> {
                                    val localBackups by viewModel.localBackups.collectAsState()
                                    var backupToRestore by remember { mutableStateOf<com.example.ui.CalculatorViewModel.LocalBackupInfo?>(null) }
                                    
                                    LaunchedEffect(Unit) {
                                        viewModel.refreshLocalBackups()
                                    }

                                    if (backupToRestore != null) {
                                        AlertDialog(
                                            onDismissRequest = { backupToRestore = null },
                                            title = { Text("Confirm Offline Restore", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color(0xFF0F172A)) },
                                            text = { 
                                                Text(
                                                    "Are you sure you want to restore from ${backupToRestore?.name}?\n\nThis will completely restore all formulas, chemicals, categories, system settings, and calculation logs. Current state will be replaced.", 
                                                    fontSize = 18.sp, 
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color(0xFF334155)
                                                ) 
                                            },
                                            confirmButton = {
                                                TextButton(
                                                    onClick = {
                                                        val target = backupToRestore
                                                        if (target != null) {
                                                            try {
                                                                val content = java.io.File(target.filePath).readText()
                                                                viewModel.importBackup(content) { success ->
                                                                    if (success) {
                                                                        Toast.makeText(context, "Backup Restored Successfully!", Toast.LENGTH_SHORT).show()
                                                                        viewModel.refreshLocalBackups()
                                                                    } else {
                                                                        Toast.makeText(context, "Invalid Backup Json Schema!", Toast.LENGTH_LONG).show()
                                                                    }
                                                                }
                                                            } catch (e: Exception) {
                                                                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                                            }
                                                        }
                                                        backupToRestore = null
                                                    }
                                                ) {
                                                    Text("Restore", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                                }
                                            },
                                            dismissButton = {
                                                TextButton(onClick = { backupToRestore = null }) {
                                                    Text("Cancel", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                                                }
                                            },
                                            containerColor = Color(0xFFF0F0F3)
                                        )
                                    }

                                    Text(
                                        text = "Securely manage fully offline backups and cloud synchronization interfaces.", 
                                        fontSize = 18.sp, 
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF334155)
                                    )

                                    // --- OFFLINE BACKUP SYSTEM CARD ---
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Offline Backup & Restore", 
                                            fontWeight = FontWeight.SemiBold, 
                                            fontSize = 24.sp,
                                            color = Color(0xFF0F172A)
                                        )
                                        
                                        IconButton(
                                            onClick = { viewModel.refreshLocalBackups() }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = "Refresh discovered backups",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                    
                                    NeumorphicCard(
                                        modifier = Modifier.fillMaxWidth().testTag("offline_backup_card"),
                                        cornerRadius = 16.dp
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                            Text(
                                                text = "Store or restore local snapshotted copies of database configurations, decimal precision variables, dark mode theme preferences, administration password, and calculation logs. Files are safely managed within Downloads/ChemicalDashboard directory.",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF334155)
                                            )
                                            
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                NeumorphicButton(
                                                    onClick = {
                                                        viewModel.exportBackup { success, message ->
                                                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                                        }
                                                    },
                                                    cornerRadius = 10.dp,
                                                    modifier = Modifier.weight(1f).height(56.dp).testTag("export_backup_button")
                                                 ) {
                                                     Icon(
                                                         imageVector = Icons.Default.CloudUpload, 
                                                         contentDescription = null, 
                                                         modifier = Modifier.size(24.dp),
                                                         tint = Color(0xFF0F172A)
                                                     )
                                                     Spacer(modifier = Modifier.width(8.dp))
                                                     Text("Export Backup", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                                 }
                                                 
                                                 NeumorphicButton(
                                                     onClick = {
                                                         filePickerLauncher.launch("application/json")
                                                     },
                                                     cornerRadius = 10.dp,
                                                     modifier = Modifier.weight(1f).height(56.dp).testTag("import_backup_button")
                                                 ) {
                                                     Icon(
                                                         imageVector = Icons.Default.CloudDownload, 
                                                         contentDescription = null, 
                                                         modifier = Modifier.size(24.dp),
                                                         tint = Color(0xFF0F172A)
                                                     )
                                                     Spacer(modifier = Modifier.width(8.dp))
                                                     Text("Import Backup", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                                 }
                                             }
                                             
                                             Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 4.dp))
                                             
                                             Text(
                                                 text = "Discovered Local Backups (${localBackups.size})",
                                                 fontWeight = FontWeight.Bold,
                                                 fontSize = 20.sp,
                                                 color = Color(0xFF0F172A)
                                             )
                                             
                                             if (localBackups.isEmpty()) {
                                                 Text(
                                                     text = "No backup files found inside the device Downloads/ChemicalDashboard folder. Click 'Export Backup' to trigger a snapshot.",
                                                     fontSize = 18.sp,
                                                     fontWeight = FontWeight.Normal,
                                                     color = Color(0xFF64748B),
                                                     modifier = Modifier.padding(vertical = 8.dp)
                                                 )
                                             } else {
                                                 Column(
                                                     modifier = Modifier.fillMaxWidth(),
                                                     verticalArrangement = Arrangement.spacedBy(12.dp)
                                                 ) {
                                                     localBackups.forEach { backup ->
                                                         NeumorphicCard(
                                                             modifier = Modifier.fillMaxWidth().testTag("local_backup_item_${backup.name}"),
                                                             cornerRadius = 12.dp,
                                                             containerColor = if (backup.isValid) Color(0xFFF8FAFC) else Color(0xFFFEF2F2)
                                                         ) {
                                                             Row(
                                                                 modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp).fillMaxWidth(),
                                                                 horizontalArrangement = Arrangement.SpaceBetween,
                                                                 verticalAlignment = Alignment.CenterVertically
                                                             ) {
                                                                 Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                                                     Row(verticalAlignment = Alignment.CenterVertically) {
                                                                         Icon(
                                                                             imageVector = if (backup.isValid) Icons.Default.CheckCircle else Icons.Default.Warning,
                                                                             contentDescription = null,
                                                                             tint = if (backup.isValid) Color(0xFF22C55E) else Color(0xFFEF4444),
                                                                             modifier = Modifier.size(20.dp)
                                                                         )
                                                                         Spacer(modifier = Modifier.width(8.dp))
                                                                         Text(
                                                                             text = backup.name,
                                                                             fontWeight = FontWeight.Bold,
                                                                             fontSize = 18.sp,
                                                                             color = Color(0xFF0F172A)
                                                                         )
                                                                     }
                                                                     
                                                                     Spacer(modifier = Modifier.height(4.dp))
                                                                     Text(
                                                                         text = "Date: ${backup.date}\nVersion: ${backup.version} • Size: ${backup.size}",
                                                                         fontSize = 16.sp,
                                                                         fontWeight = FontWeight.Medium,
                                                                         color = Color(0xFF475569)
                                                                     )
                                                                 }
                                                                 
                                                                 Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                                     if (backup.isValid) {
                                                                         Button(
                                                                             onClick = { backupToRestore = backup },
                                                                             modifier = Modifier.height(38.dp).testTag("restore_local_${backup.name}"),
                                                                             contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                                             colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                                                         ) {
                                                                             Text("Restore", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                                         }
                                                                     }
                                                                     
                                                                     IconButton(
                                                                         onClick = {
                                                                             try {
                                                                                 val fileObj = java.io.File(backup.filePath)
                                                                                 if (fileObj.delete()) {
                                                                                     Toast.makeText(context, "Backup file deleted!", Toast.LENGTH_SHORT).show()
                                                                                     viewModel.refreshLocalBackups()
                                                                                 } else {
                                                                                     Toast.makeText(context, "Could not delete backup file.", Toast.LENGTH_SHORT).show()
                                                                                 }
                                                                             } catch (e: Exception) {
                                                                                 Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                                                             }
                                                                         },
                                                                         modifier = Modifier.size(38.dp).testTag("delete_local_${backup.name}")
                                                                     ) {
                                                                         Icon(
                                                                             imageVector = Icons.Default.Delete,
                                                                             contentDescription = "Delete local backup",
                                                                             tint = Color(0xFFEF4444),
                                                                             modifier = Modifier.size(20.dp)
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

                                     Spacer(modifier = Modifier.height(14.dp))
                                     Text(
                                         text = "Firebase Synchronizer System", 
                                         fontWeight = FontWeight.SemiBold, 
                                         fontSize = 24.sp,
                                         color = Color(0xFF0F172A)
                                     )
                                    NeumorphicCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        cornerRadius = 16.dp
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    val statusColor = when {
                                                        lastBackupStatus == "Success" -> Color(0xFF22C55E)
                                                        lastBackupStatus.startsWith("Failed") -> Color(0xFFEF4444)
                                                        else -> Color(0xFFF59E0B)
                                                     }
                                                    Box(modifier = Modifier.size(16.dp).clip(RoundedCornerShape(8.dp)).background(statusColor))
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Text(
                                                        text = "Last Status: $lastBackupStatus", 
                                                        fontSize = 18.sp, 
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF0F172A)
                                                    )
                                                }
                                                Text(
                                                    text = lastBackupTime, 
                                                    fontSize = 18.sp, 
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color(0xFF64748B)
                                                )
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                NeumorphicButton(
                                                    onClick = {
                                                        if (firebaseUserUid == null) {
                                                            viewModel.signInFirebaseAnonymously()
                                                        }
                                                        viewModel.backupToFirebase { _, msg ->
                                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                        }
                                                    },
                                                    cornerRadius = 10.dp,
                                                    modifier = Modifier.weight(1f).height(56.dp).testTag("backup_now_button")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.CloudUpload, 
                                                        contentDescription = null, 
                                                        modifier = Modifier.size(24.dp),
                                                        tint = Color(0xFF0F172A)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("Sync Backup", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                                }

                                                NeumorphicButton(
                                                    onClick = {
                                                        if (firebaseUserUid == null) {
                                                            viewModel.signInFirebaseAnonymously()
                                                        }
                                                        viewModel.restoreFromFirebase { _, msg ->
                                                             Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                        }
                                                    },
                                                    cornerRadius = 10.dp,
                                                    containerColor = MaterialTheme.colorScheme.secondary,
                                                    contentColor = MaterialTheme.colorScheme.onSecondary,
                                                    modifier = Modifier.weight(1f).height(56.dp).testTag("restore_data_button")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.CloudDownload, 
                                                        contentDescription = null, 
                                                        modifier = Modifier.size(24.dp),
                                                        tint = Color.White
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("Sync Restore", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                }
                                            }

                                            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 4.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                                                    Text(
                                                        text = "Autosave dynamic values", 
                                                        fontWeight = FontWeight.SemiBold, 
                                                        fontSize = 24.sp,
                                                        color = Color(0xFF0F172A)
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = "Update web cache immediately on variable edits", 
                                                        fontSize = 18.sp, 
                                                        fontWeight = FontWeight.Medium,
                                                        color = Color(0xFF334155)
                                                    )
                                                }
                                                Switch(
                                                    checked = firebaseConfig.autoSync,
                                                    onCheckedChange = { viewModel.updateFirebaseConfig(firebaseConfig.copy(autoSync = it)) },
                                                    modifier = Modifier.size(48.dp).testTag("auto_backup_switch")
                                                )
                                            }
                                        }
                                    }

                                    // JSON EXPORTER & SANDBOX BLUEPRINT
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Dynamic Cloud JSON Transpiration Sandbox", 
                                        fontWeight = FontWeight.SemiBold, 
                                        fontSize = 24.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                    NeumorphicCard(
                                        modifier = Modifier.fillMaxWidth().testTag("sync_sandbox_blueprint"),
                                        cornerRadius = 16.dp
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                            Text(
                                                text = "Inter-process dry-runs inspects transfer format data without sending to active Firestore registers. Ensure formulas comply to NetworkFormula JSON conventions.",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF334155)
                                            )

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Presets:", 
                                                    fontSize = 18.sp, 
                                                    color = MaterialTheme.colorScheme.primary, 
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Button(
                                                    onClick = {
                                                        blueprintJsonText = "[{\"id\": 99, \"title\": \"Wet-End Retention Optimizer\", \"category\": \"Chemical Dosage\", \"formula\": \"D * P / R\", \"resultUnit\": \"kg/H\", \"description\": \"Effective dosage flow rate polymer calculations\", \"variables\": [{\"symbol\":\"D\",\"name\":\"Design Dosage\",\"value\":0.45,\"unit\":\"kg/MT\"},{\"symbol\":\"P\",\"name\":\"Production Rate\",\"value\":15.0,\"unit\":\"MT/H\"},{\"symbol\":\"R\",\"name\":\"Dilution Ratio\",\"value\":0.15,\"unit\":\"ratio\"}]}]"
                                                    },
                                                    modifier = Modifier.height(48.dp).minimumInteractiveComponentSize(),
                                                    contentPadding = PaddingValues(horizontal = 12.dp)
                                                ) {
                                                    Text("Retention", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }

                                            OutlinedTextField(
                                                value = blueprintJsonText,
                                                onValueChange = { blueprintJsonText = it },
                                                label = { Text("Transfer Payload JSON", fontSize = 18.sp, fontWeight = FontWeight.Medium) },
                                                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 16.sp),
                                                modifier = Modifier.fillMaxWidth().height(180.dp)
                                            )

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                NeumorphicButton(
                                                    onClick = {
                                                        blueprintJsonText = viewModel.getCloudSyncBlueprintJson()
                                                        blueprintStatusMsg = "Successfully serialized local state database to standard cloud blueprint!"
                                                    },
                                                    cornerRadius = 10.dp,
                                                    modifier = Modifier.weight(1f).height(56.dp)
                                                ) {
                                                    Text("Serialize to JSON", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                                }
                                                NeumorphicButton(
                                                    onClick = {
                                                        if (isAdminMode) {
                                                            if (blueprintJsonText.isNotBlank()) {
                                                                 viewModel.importCloudSyncBlueprintJson(blueprintJsonText) { _, msg ->
                                                                     blueprintStatusMsg = msg
                                                                 }
                                                            } else {
                                                                 blueprintStatusMsg = "JSON is empty. Provide correct schema layout."
                                                            }
                                                        } else {
                                                            blueprintStatusMsg = "Write denied. Credentials must be authorized."
                                                        }
                                                    },
                                                    cornerRadius = 10.dp,
                                                    containerColor = MaterialTheme.colorScheme.error,
                                                    contentColor = MaterialTheme.colorScheme.onError,
                                                    modifier = Modifier.weight(1f).height(56.dp).testTag("simulate_sync_overwrite")
                                                ) {
                                                    Text("Write Blueprint (Admin)", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                }
                                            }

                                            if (blueprintStatusMsg.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                NeumorphicCard(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    cornerRadius = 8.dp
                                                ) {
                                                    Text(
                                                        text = blueprintStatusMsg, 
                                                        fontSize = 18.sp, 
                                                        fontWeight = FontWeight.Medium,
                                                        color = Color(0xFF0F172A),
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                "appearance" -> {
                                    Text(
                                        text = "Customize general viewport, typography details, and custom themes settings.", 
                                        fontSize = 18.sp, 
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Theme Preferences", 
                                        fontWeight = FontWeight.SemiBold, 
                                        fontSize = 22.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    NeumorphicCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        cornerRadius = 16.dp
                                    ) {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp).fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                                                    Text(
                                                        text = "Follow System Theme", 
                                                        fontWeight = FontWeight.SemiBold, 
                                                        fontSize = 18.sp,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = "Detect and follow your device's light or dark mode setting automatically.", 
                                                        fontSize = 13.sp, 
                                                        fontWeight = FontWeight.Medium,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                    )
                                                }
                                                Switch(
                                                    checked = followSystemTheme,
                                                    onCheckedChange = { viewModel.setFollowSystemTheme(it) },
                                                    modifier = Modifier.size(48.dp).testTag("follow_system_theme_switch")
                                                )
                                            }
                                            
                                            HorizontalDivider(
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                                modifier = Modifier.padding(horizontal = 12.dp)
                                            )
                                            
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp).fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                                                    Text(
                                                        text = "Manual Dark Mode Theme", 
                                                        fontWeight = FontWeight.SemiBold, 
                                                        fontSize = 18.sp,
                                                        color = if (followSystemTheme) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = if (followSystemTheme) "Disabled because 'Follow System Theme' is active." else "Force dynamic deep slate navy dark theme manually.", 
                                                        fontSize = 13.sp, 
                                                        fontWeight = FontWeight.Medium,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                                    )
                                                }
                                                Switch(
                                                    checked = if (followSystemTheme) systemInDark else manualDarkMode,
                                                    enabled = !followSystemTheme,
                                                    onCheckedChange = { viewModel.toggleDarkMode() },
                                                    modifier = Modifier.size(48.dp).testTag("theme_mode_switch")
                                                )
                                            }

                                            HorizontalDivider(
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                                modifier = Modifier.padding(horizontal = 12.dp)
                                            )

                                            Row(
                                                modifier = Modifier
                                                    .padding(horizontal = 12.dp, vertical = 16.dp)
                                                    .fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(
                                                        text = "Current Theme",
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 18.sp,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    val themeDebugText = if (followSystemTheme) {
                                                        if (systemInDark) "System (Dark)" else "System (Light)"
                                                    } else {
                                                        if (manualDarkMode) "Manual (Dark)" else "Manual (Light)"
                                                    }
                                                    Text(
                                                        text = themeDebugText,
                                                        fontSize = 15.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.testTag("theme_debug_indicator")
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Display Text Sizing Scales", 
                                        fontWeight = FontWeight.SemiBold, 
                                        fontSize = 22.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    NeumorphicCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        cornerRadius = 16.dp
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Text(
                                                text = "Adjust font visual scaling factors dynamically. Current factor: ${fontScale}x", 
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                            )
                                            
                                            val fontScales = listOf(
                                                0.85f to "Small (85%)",
                                                1.0f to "Normal (100%)",
                                                1.15f to "Large (115%)",
                                                1.3f to "Max (130%)"
                                             )
                                            fontScales.forEach { (scale, labelStr) ->
                                                val isSelected = fontScale == scale
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .minimumInteractiveComponentSize()
                                                        .clickable { viewModel.setFontScale(scale) }
                                                        .padding(vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Checkbox(
                                                        checked = isSelected,
                                                        onCheckedChange = { viewModel.setFontScale(scale) },
                                                        modifier = Modifier.size(32.dp).testTag("font_scale_checkbox_$scale")
                                                    )
                                                    Spacer(modifier = Modifier.width(16.dp))
                                                    Text(
                                                        text = labelStr, 
                                                        fontSize = 16.sp, 
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    NeumorphicCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        cornerRadius = 16.dp
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp).fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                                                Text(
                                                    text = "Slate Material Theme Accent", 
                                                    fontWeight = FontWeight.SemiBold, 
                                                    fontSize = 18.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Toggle classic industrial look versus dynamic Material 3 colors.", 
                                                    fontSize = 13.sp, 
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                )
                                            }
                                            Switch(
                                                checked = true,
                                                onCheckedChange = { /* Auto managed */ },
                                                enabled = false,
                                                modifier = Modifier.size(48.dp)
                                            )
                                        }
                                    }
                                }
                                "ai_assistant" -> {
                                    Text(
                                        text = "Enhance formulary calculations using OpenAI cloud assistance.", 
                                        fontSize = 18.sp, 
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF334155)
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "API Access Keys", 
                                        fontWeight = FontWeight.SemiBold, 
                                        fontSize = 24.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                    NeumorphicCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        cornerRadius = 16.dp
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                            OutlinedTextField(
                                                value = openaiApiKey,
                                                onValueChange = { viewModel.setOpenaiApiKey(it) },
                                                label = { Text("OpenAI API Token / Secret", fontSize = 18.sp, fontWeight = FontWeight.Medium) },
                                                visualTransformation = PasswordVisualTransformation(),
                                                modifier = Modifier.fillMaxWidth().height(64.dp).testTag("openai_key_input"),
                                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp),
                                                singleLine = true
                                            )
                                            Text(
                                                text = "Set personalized OpenAI credentials to authorize and unlock AI recommendations context.", 
                                                fontSize = 18.sp, 
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF334155)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    NeumorphicCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        cornerRadius = 16.dp
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp).fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                                                Text(
                                                    text = "Enable Smart Calculation Help", 
                                                    fontWeight = FontWeight.SemiBold, 
                                                    fontSize = 24.sp,
                                                    color = Color(0xFF0F172A)
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Runs intelligent estimations automatically on execution", 
                                                    fontSize = 18.sp, 
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color(0xFF334155)
                                                )
                                            }
                                            Switch(
                                                checked = enableAiCalculations,
                                                onCheckedChange = { viewModel.setEnableAiCalculations(it) },
                                                modifier = Modifier.size(48.dp).testTag("ai_calc_help_switch")
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    NeumorphicCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        cornerRadius = 16.dp
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp).fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                                                Text(
                                                    text = "AI Dosage Recommendations", 
                                                    fontWeight = FontWeight.SemiBold, 
                                                    fontSize = 24.sp,
                                                    color = Color(0xFF0F172A)
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Display suggestions and smart insights on formula cards", 
                                                    fontSize = 18.sp, 
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color(0xFF334155)
                                                )
                                            }
                                            Switch(
                                                checked = aiSuggestions,
                                                onCheckedChange = { viewModel.setAiSuggestions(it) },
                                                modifier = Modifier.size(48.dp).testTag("ai_suggestions_switch")
                                            )
                                        }
                                    }
                                }
                                "about" -> {
                                    Text(
                                        text = "Software production references and build details.", 
                                        fontSize = 18.sp, 
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF334155)
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))
                                    NeumorphicCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        cornerRadius = 16.dp
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Text(
                                                text = "🏫 ChemDose Formula Hub", 
                                                fontWeight = FontWeight.SemiBold, 
                                                fontSize = 24.sp,
                                                color = Color(0xFF0F172A)
                                            )
                                            Text(
                                                text = "Release Version: v2.5.4 Stable Production Build", 
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF0F172A)
                                            )
                                            Text(
                                                text = "Framework: Jetpack Compose Material 3 Client-Side Architecture", 
                                                fontSize = 18.sp, 
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF334155)
                                            )
                                            Text(
                                                text = "Encryption Standard: AES-128 Local Vault Storage", 
                                                fontSize = 18.sp, 
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF334155)
                                            )
                                            
                                            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))
                                            
                                            Text(
                                                text = "Developer Support: support@chemdose.org", 
                                                fontSize = 20.sp, 
                                                fontWeight = FontWeight.Bold, 
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "For inquiries, customized system formulas, or central website server integrations, message support directly.", 
                                                fontSize = 18.sp, 
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF334155)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // --- END_OF_CATEGORIZED_SETTINGS ---

                        // --- CLOUD BACKUP & SYNC REMOVED ---

                        // --- WEBSITE CENTRAL SYNCHRONIZATION AND ACCOUNT CONTROL HUB ---
                        Text("Website Integration & Cloud Sync", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        NeumorphicCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 20.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // 1. Current Network Status & Web Sync Status Indicators
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (isOnline) Color(0xFF4CAF50) else Color(0xFFF44336))
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isOnline) "Web Connection: ONLINE" else "Web Connection: OFFLINE (Cache Active)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    
                                    NeumorphicButton(
                                        onClick = { viewModel.refreshNetworkStatus() },
                                        cornerRadius = 6.dp,
                                        modifier = Modifier.height(26.dp)
                                    ) {
                                        Text("Check Net", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Text(
                                    text = "Configure connectivity parameters to connect with remote databases. Fully offline-capable client-side architecture synchronizes automatically once networks reconnect.",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )

                                // 2. Provider Database Choice Selector Chips (Supports change of configuration without modifying formula screens)
                                Text("Select Target Website Server Database Type:", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    CloudDbProvider.values().forEach { providerType ->
                                        val isSelected = cloudConfig.provider == providerType
                                        NeumorphicButton(
                                            onClick = {
                                                viewModel.updateCloudConfig(cloudConfig.copy(provider = providerType))
                                            },
                                            cornerRadius = 6.dp,
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f).height(30.dp)
                                        ) {
                                            Text(providerType.name, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                // 3. Server URL and Api Key Inputs
                                var tempApiUrl by remember(cloudConfig) { mutableStateOf(cloudConfig.apiUrl) }
                                var tempApiKey by remember(cloudConfig) { mutableStateOf(cloudConfig.apiKey) }

                                OutlinedTextField(
                                    value = tempApiUrl,
                                    onValueChange = {
                                        tempApiUrl = it
                                        viewModel.updateCloudConfig(cloudConfig.copy(apiUrl = it))
                                    },
                                    label = { Text("API Backend Server URL", fontSize = 9.sp) },
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 10.sp),
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = tempApiKey,
                                    onValueChange = {
                                        tempApiKey = it
                                        viewModel.updateCloudConfig(cloudConfig.copy(apiKey = it))
                                    },
                                    label = { Text("Cloud Authorization Token / API Key", fontSize = 9.sp) },
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 10.sp),
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    singleLine = true
                                )

                                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                                // 4. Role Authentication Status & Setup Options
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("User Role Authorization:", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = "Active Session: $currentUserRole Mode",
                                            fontSize = 11.sp,
                                            color = if (isAdminMode) MaterialTheme.colorScheme.primary else Color.Gray,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                    
                                    // Quick Toggle roles to satisfy: "Prepare login system for future website integration. Admin role and User role support."
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        NeumorphicButton(
                                            onClick = { viewModel.loginAsRole("Operator") },
                                            cornerRadius = 6.dp,
                                            containerColor = if (!isAdminMode) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text("Set Operator", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                        }
                                        NeumorphicButton(
                                            onClick = { viewModel.loginAsRole("Admin") },
                                            cornerRadius = 6.dp,
                                            containerColor = if (isAdminMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text("Set Admin", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                Text(
                                    text = if (isAdminMode) "✓ Admin privileges verified! You are authorized to sync, edit, add, and fully modify formulas." else "⚠ Read-Only Active. Operators can run full offline calculations, but only Admin credentials can trigger central database uploads / sync overrides.",
                                    fontSize = 9.sp,
                                    lineHeight = 11.sp,
                                    color = if (isAdminMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )

                                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                                // 5. Last Sync Status & Synchronization Details
                                lastSyncReport?.let { report ->
                                    NeumorphicCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        containerColor = if (report.success) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                                        cornerRadius = 12.dp
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                text = if (report.success) "Sync Succeeded" else "Sync Cancelled/Failed",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                color = if (report.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                            )
                                            Text(
                                                text = report.message,
                                                fontSize = 9.sp,
                                                lineHeight = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }

                                // 6. Trigger Sync Button (Restricted to Admin Mode)
                                NeumorphicButton(
                                    onClick = {
                                        if (isAdminMode) {
                                            viewModel.syncNow { finalReport ->
                                                Toast.makeText(context, finalReport.message, Toast.LENGTH_LONG).show()
                                            }
                                        } else {
                                            Toast.makeText(context, "Privileges Denied. Standard operators cannot sync. Toggle Session to Admin above first.", Toast.LENGTH_LONG).show()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(42.dp).testTag("sync_now_button"),
                                    containerColor = if (isAdminMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isAdminMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    enabled = !isSyncing,
                                    cornerRadius = 12.dp
                                ) {
                                    if (isSyncing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Syncing with central website...", fontSize = 10.sp)
                                    } else {
                                        Icon(Icons.Default.Refresh, contentDescription = "Sync", modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isAdminMode) "SYNC NOW WITH WEBSITE CENTRAL DATABASE" else "SYNC NOW (ADMIN ONLY)",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // --- FUTURE CLOUD SYNC SANDBOX BLUEPRINT (JSON-BASED SIMULATOR) ---
                        Text("Cloud Sync JSON Sandbox Blueprint", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        NeumorphicCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 20.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "This utility generates and validates JSON data using the exact schema structures designed for the future cloud synchronization REST API endpoints (List<NetworkFormula>). Use it to dry-run changes offline.",
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )

                                // Preset Loader Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Simulate Presets:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    NeumorphicButton(
                                        onClick = {
                                            blueprintJsonText = """[
  {
    "id": 99,
    "title": "Wet-End Retention Optimizer",
    "category": "Chemical Dosage",
    "formula": "D * P / R",
    "resultUnit": "kg/H",
    "description": "Calculates wet-end polymer retention aid solid dosage flow based on dilution factor.\n---outputs---\nEffective Dosage Flow Rate:D * P / R:kg/H",
    "variables": [
      {
        "symbol": "D",
        "name": "Design Dosage Target",
        "value": 0.45,
        "unit": "kg/MT"
      },
      {
        "symbol": "P",
        "name": "Dry Production Rate",
        "value": 15.0,
        "unit": "MT/H"
      },
      {
        "symbol": "R",
        "name": "Dilution Factor",
        "value": 0.15,
        "unit": "ratio"
      }
    ]
  }
]"""
                                            blueprintStatusMsg = "Retention Aid Optimizer blueprint template loaded in editor."
                                            blueprintIsSuccess = true
                                        },
                                        cornerRadius = 6.dp,
                                        containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f),
                                        modifier = Modifier.height(26.dp)
                                    ) {
                                        Text("Retention Aid", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }

                                    NeumorphicButton(
                                        onClick = {
                                            blueprintJsonText = """[
  {
    "id": 100,
    "title": "Defoamer Feed Flow Rate",
    "category": "Chemical Dosage",
    "formula": "F * S / 1000",
    "resultUnit": "L/Min",
    "description": "Calculates defoamer pump dosing feed flow rate based on targeted specific feed factor S.\n---outputs---\nDefoamer Target Feed Rate:F * S / 1000:L/Min",
    "variables": [
      {
        "symbol": "F",
        "name": "Stock Flow Rate",
        "value": 2500.0,
        "unit": "L/Min"
      },
      {
        "symbol": "S",
        "name": "Specific Target Feed",
        "value": 0.8,
        "unit": "mL/L"
      }
    ]
  }
]"""
                                            blueprintStatusMsg = "Defoamer Feed Flow blueprint template loaded in editor."
                                            blueprintIsSuccess = true
                                        },
                                        cornerRadius = 6.dp,
                                        containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f),
                                        modifier = Modifier.height(26.dp)
                                    ) {
                                        Text("Defoamer Flow", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // Interactive JSON input/editor
                                OutlinedTextField(
                                    value = blueprintJsonText,
                                    onValueChange = { blueprintJsonText = it },
                                    label = { Text("Transfer Payload JSON (List<NetworkFormula>)", fontSize = 9.sp) },
                                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 9.sp),
                                    placeholder = { Text("Paste or generate JSON payload layout to inspect transfer format structure...", fontSize = 9.sp) },
                                    modifier = Modifier.fillMaxWidth().height(160.dp),
                                    maxLines = 15
                                )

                                // Action Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            blueprintJsonText = viewModel.getCloudSyncBlueprintJson()
                                            blueprintStatusMsg = "Successfully serialized all database records to standardized Cloud Schema representation!"
                                            blueprintIsSuccess = true
                                        },
                                        modifier = Modifier.weight(1f).height(32.dp),
                                        contentPadding = PaddingValues(horizontal = 2.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Text("Export Current DB", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            if (blueprintJsonText.isNotBlank()) {
                                                clipboardManager.setText(AnnotatedString(blueprintJsonText))
                                                Toast.makeText(context, "Cloud Blueprint JSON copied to clipboard!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Editor is empty!", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.weight(1f).height(32.dp),
                                        contentPadding = PaddingValues(horizontal = 2.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                    ) {
                                        Text("Copy to Clip", fontSize = 9.sp)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            val clip = clipboardManager.getText()?.text
                                            if (!clip.isNullOrBlank()) {
                                                blueprintJsonText = clip
                                                blueprintStatusMsg = "Clipboard pasted."
                                                blueprintIsSuccess = true
                                            } else {
                                                Toast.makeText(context, "Clipboard empty!", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.weight(1f).height(32.dp),
                                        contentPadding = PaddingValues(horizontal = 2.dp)
                                    ) {
                                        Text("Paste Clip", fontSize = 9.sp)
                                    }
                                }

                                if (blueprintStatusMsg.isNotBlank()) {
                                    Text(
                                        text = blueprintStatusMsg,
                                        fontSize = 10.sp,
                                        color = if (blueprintIsSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                }

                                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                                // Simulator REST Update trigger (Requires Admin authorization)
                                Button(
                                    onClick = {
                                        if (isAdminMode) {
                                            if (blueprintJsonText.isNotBlank()) {
                                                viewModel.importCloudSyncBlueprintJson(blueprintJsonText) { success, msg ->
                                                    blueprintIsSuccess = success
                                                    blueprintStatusMsg = msg
                                                    if (success) {
                                                        Toast.makeText(context, "Cloud Sync Simulation Successful!", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            } else {
                                                blueprintIsSuccess = false
                                                blueprintStatusMsg = "JSON Editor is empty. Load a preset or paste custom data first."
                                            }
                                        } else {
                                            Toast.makeText(context, "Simulation action denied. Administrator privileges are required to edit or pull synced formulas.", Toast.LENGTH_LONG).show()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(38.dp).testTag("simulate_sync_overwrite"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isAdminMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Icon(Icons.Default.Done, contentDescription = "Simulate Overlay", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isAdminMode) "VALIDATE & WRITE TO ROOM CACHE" else "VALIDATE & WRITE (ADMIN ONLY)",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }

                        // EXTRA_SETTINGS_REMOVED
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showBackupRestoreDialog = false }) {
                        Text("Close Settings")
                    }
                }
            )
        }

        // Slide Metadata Editing dialog (Title, Formula, Unit, description editing)
        activeSlideOpt?.let { currentSlide ->
            if (showEditSlideMetaDialog) {
                val isBuiltIn = currentSlide.slide.id <= 10 && !isAdminMode
                
                var editTitle by remember { mutableStateOf(currentSlide.slide.title) }
                var editFormula by remember { mutableStateOf(currentSlide.slide.formula) }
                var editResultUnit by remember { mutableStateOf(currentSlide.slide.resultUnit) }
                var editDescription by remember { mutableStateOf(currentSlide.slide.description) }
                var editCategory by remember { mutableStateOf(currentSlide.slide.category) }

                // Dynamic Tokens List or preview
                val parsedTokens = remember(editFormula) {
                    if (editFormula.trim().startsWith("[")) {
                        FormulaSerializer.deserialize(editFormula) ?: FormulaEvaluator.tokenize(editFormula)
                    } else {
                        FormulaEvaluator.tokenize(editFormula)
                    }
                }
                val tokensFormattedString = remember(parsedTokens) {
                    FormulaEvaluator.tokensToFormattedExpression(parsedTokens)
                }

                val allowedVariablesMap = remember(currentSlide.variables) {
                    currentSlide.variables.associate { it.symbol to it.value }
                }
                val syntaxError = remember(parsedTokens, allowedVariablesMap) {
                    FormulaEvaluator.validateTokens(parsedTokens, allowedVariablesMap)
                }

                // Test calculation input values
                var testValues by remember {
                    mutableStateOf(currentSlide.variables.associate { it.symbol to it.value.toString() })
                }

                AlertDialog(
                    onDismissRequest = { showEditSlideMetaDialog = false },
                    title = { Text(if (isBuiltIn) "View Slide Config (Preset)" else "Edit Slide Config (Admin)") },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Title
                            OutlinedTextField(
                                value = editTitle,
                                onValueChange = { if (!isBuiltIn) editTitle = it },
                                label = { Text("Slide Title") },
                                enabled = !isBuiltIn,
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (!isBuiltIn) {
                                // Formula Builder controls (Only for editable formulas)
                                Text("Formula Builder Keyboard", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                
                                // Math Toolbar
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf("+" to " + ", "-" to " - ", "×" to " * ", "÷" to " / ", "(" to " ( ", ")" to " ) ").forEach { (label, toInsert) ->
                                        Button(
                                            onClick = {
                                                val sep = if (editFormula.isNotEmpty() && !editFormula.endsWith(" ")) "" else ""
                                                editFormula = "$editFormula$sep$toInsert"
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                            ),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.testTag("builder_math_btn_$label")
                                        ) {
                                            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                // Variable insertion buttons
                                if (currentSlide.variables.isNotEmpty()) {
                                    Text("Insert slide params:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Row(
                                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        currentSlide.variables.forEach { variable ->
                                            Button(
                                                onClick = {
                                                    val sep = if (editFormula.isNotEmpty() && !editFormula.endsWith(" ")) " " else ""
                                                    editFormula = "$editFormula$sep${variable.symbol} "
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                                ),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.testTag("builder_var_${variable.symbol}")
                                            ) {
                                                Text("${variable.symbol} (${variable.name})", fontSize = 10.sp)
                                            }
                                        }
                                    }
                                }

                                // Constants insertion buttons
                                Text("Insert constants:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf("1.44", "1000", "60", "0.01").forEach { cn ->
                                        Button(
                                            onClick = {
                                                val sep = if (editFormula.isNotEmpty() && !editFormula.endsWith(" ")) " " else ""
                                                editFormula = "$editFormula$sep$cn "
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.testTag("builder_cn_$cn")
                                        ) {
                                            Text(cn, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                        }
                                    }
                                }
                            }

                            // Formula text input box
                            OutlinedTextField(
                                value = editFormula,
                                onValueChange = { if (!isBuiltIn) editFormula = it },
                                label = { Text("Formula pattern (e.g., A * B / C)") },
                                placeholder = { Text("A * B") },
                                enabled = !isBuiltIn,
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("formula_builder_input")
                            )

                            // Formula Preview
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Mathematical View (Live Preview):", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Text(
                                        text = tokensFormattedString.ifBlank { "Empty formula expression" },
                                        fontSize = 14.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            // Validation message
                            if (syntaxError != null) {
                                Text(
                                    text = "⚠️ $syntaxError",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            } else {
                                Text(
                                    text = "✅ Formula syntax validated and safe",
                                    color = Color(0xFF10B981),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }

                            // Dynamic test evaluation mode
                            if (syntaxError == null) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("🔬 Live Interactive Test Mode", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            currentSlide.variables.forEach { v ->
                                                val currVal = testValues[v.symbol] ?: v.value.toString()
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "${v.symbol} (${v.name}):",
                                                        fontSize = 11.sp,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    androidx.compose.foundation.text.BasicTextField(
                                                        value = currVal,
                                                        onValueChange = { newVal: String ->
                                                            testValues = testValues + (v.symbol to newVal)
                                                        },
                                                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurface),
                                                        modifier = Modifier
                                                            .width(80.dp)
                                                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                                                            .padding(4.dp)
                                                            .testTag("test_input_${v.symbol}")
                                                    )
                                                }
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(6.dp))
                                        val doubleTestValues = testValues.mapValues { it.value.toDoubleOrNull() ?: 0.0 }
                                        val testResult = remember(editFormula, doubleTestValues) {
                                            FormulaEvaluator.evaluate(editFormula, doubleTestValues)
                                        }
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Calculated output:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                            Text(
                                                text = if (testResult.isNaN() || testResult.isInfinite()) "Error" else String.format(Locale.US, "%.4f %s", testResult, editResultUnit),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            }

                            // Result Unit selection
                            OutlinedTextField(
                                value = editResultUnit,
                                onValueChange = { if (!isBuiltIn) editResultUnit = it },
                                label = { Text("Result Output Unit (e.g. g/t, %, mL, kg)") },
                                enabled = !isBuiltIn,
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Category
                            OutlinedTextField(
                                value = editCategory,
                                onValueChange = { if (!isBuiltIn) editCategory = it },
                                label = { Text("Category Classification") },
                                enabled = !isBuiltIn,
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Description
                            OutlinedTextField(
                                value = editDescription,
                                onValueChange = { if (!isBuiltIn) editDescription = it },
                                label = { Text("Formula description or utility guide") },
                                enabled = !isBuiltIn,
                                maxLines = 3,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        if (!isBuiltIn) {
                            Button(
                                enabled = syntaxError == null && editTitle.isNotBlank(),
                                onClick = {
                                    val serializedFormula = FormulaSerializer.serialize(parsedTokens)
                                    viewModel.updateSlideDetails(
                                        slideId = currentSlide.slide.id,
                                        title = editTitle,
                                        formula = serializedFormula,
                                        resultUnit = editResultUnit,
                                        description = editDescription,
                                        category = editCategory
                                    )
                                }
                            ) {
                                Text("Save Changes")
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showEditSlideMetaDialog = false }) {
                            Text(if (isBuiltIn) "Close" else "Cancel")
                        }
                    }
                )
            }
        }

        // Error message dialog if formula save operation fails
        if (showSaveErrorDialogMessage != null) {
            AlertDialog(
                onDismissRequest = {
                    showSaveErrorDialogMessage = null
                    viewModel.resetFormulaSaveStatus()
                },
                title = { Text("Update Failed", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
                text = { Text(showSaveErrorDialogMessage ?: "") },
                confirmButton = {
                    Button(
                        onClick = {
                            showSaveErrorDialogMessage = null
                            viewModel.resetFormulaSaveStatus()
                        }
                    ) {
                        Text("OK")
                    }
                }
            )
        }

        // Add Variable parameter dialog
        activeSlideOpt?.let { currentSlide ->
            if (showAddVarDialog) {
                var sym by remember { mutableStateOf("") }
                var lbl by remember { mutableStateOf("") }
                var initValStr by remember { mutableStateOf("0.0") }
                var unitStr by remember { mutableStateOf("g/t") }
                var isRequired by remember { mutableStateOf(true) }
                var isHidden by remember { mutableStateOf(false) }

                AlertDialog(
                    onDismissRequest = { showAddVarDialog = false },
                    title = { Text("Add Param Input Field") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Define a math variable parameter mapping to a dynamic symbol in your formula.", fontSize = 12.sp)

                            OutlinedTextField(
                                value = sym,
                                onValueChange = { sym = it.replace(" ", "") },
                                label = { Text("Symbol Symbol (e.g., Q, Density, A, P)") },
                                placeholder = { Text("A") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = lbl,
                                onValueChange = { lbl = it },
                                label = { Text("Full parameter label") },
                                placeholder = { Text("Pulp stock Flow") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = initValStr,
                                onValueChange = { initValStr = it },
                                label = { Text("Default double value") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = unitStr,
                                onValueChange = { unitStr = it },
                                label = { Text("Unit (g/t, %, mL, kg, etc.)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isRequired = !isRequired }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isRequired,
                                    onCheckedChange = { isRequired = it }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Required Parameter", fontSize = 13.sp)
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isHidden = !isHidden }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isHidden,
                                    onCheckedChange = { isHidden = it }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Hide from users", fontSize = 13.sp)
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (sym.isNotBlank() && lbl.isNotBlank()) {
                                    val valD = initValStr.toDoubleOrNull() ?: 0.0
                                    viewModel.addVariableToSlide(
                                        slideId = currentSlide.slide.id,
                                        symbol = sym,
                                        name = lbl,
                                        value = valD,
                                        unit = unitStr,
                                        isRequired = isRequired,
                                        isHidden = isHidden
                                    )
                                    showAddVarDialog = false
                                    Toast.makeText(context, "Added $sym parameter!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Fill in symbols & labels!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Text("Create parameter")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddVarDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }

        // Edit existing parameter specifications (Admin Mode only)
        showEditVarSpecsDialog?.let { variable ->
            var sym by remember { mutableStateOf(variable.symbol) }
            var lbl by remember { mutableStateOf(variable.name) }
            var valStr by remember { mutableStateOf(variable.value.toString()) }
            var unitStr by remember { mutableStateOf(variable.unit) }
            var isRequired by remember { mutableStateOf(variable.isRequired) }
            var isHidden by remember { mutableStateOf(variable.isHidden) }

            AlertDialog(
                onDismissRequest = { showEditVarSpecsDialog = null },
                title = { Text("Edit Parameter specs (Admin)") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = sym,
                            onValueChange = { sym = it.replace(" ", "") },
                            label = { Text("Symbol Symbol") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = lbl,
                            onValueChange = { lbl = it },
                            label = { Text("Parameter label") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = valStr,
                            onValueChange = { valStr = it },
                            label = { Text("User Value") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = unitStr,
                            onValueChange = { unitStr = it },
                            label = { Text("Unit") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isRequired = !isRequired }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isRequired,
                                onCheckedChange = { isRequired = it }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Required Parameter", fontSize = 13.sp)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isHidden = !isHidden }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isHidden,
                                onCheckedChange = { isHidden = it }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Hide from users", fontSize = 13.sp)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (sym.isNotBlank() && lbl.isNotBlank()) {
                                val vD = valStr.toDoubleOrNull() ?: 0.0
                                viewModel.updateVariableSpecs(
                                    variable = variable,
                                    symbol = sym,
                                    name = lbl,
                                    value = vD,
                                    unit = unitStr,
                                    isRequired = isRequired,
                                    isHidden = isHidden
                                )
                                showEditVarSpecsDialog = null
                                Toast.makeText(context, "Specs updated!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text("Save Specs")
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(
                            onClick = {
                                showEditVarSpecsDialog = null
                                variableToDelete = variable
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Delete")
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        TextButton(onClick = { showEditVarSpecsDialog = null }) {
                            Text("Cancel")
                        }
                    }
                }
            )
        }

        // Delete Parameter Confirmation Dialog
        variableToDelete?.let { variable ->
            AlertDialog(
                onDismissRequest = { variableToDelete = null },
                title = { Text("Confirm Deletion") },
                text = {
                    Text("Are you sure you want to delete parameter \"${variable.name}\" (${variable.symbol})? This action cannot be undone.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteVariableFromSlide(variable)
                            variableToDelete = null
                            Toast.makeText(context, "Deleted parameter ${variable.symbol}!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.onError)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { variableToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // --- NEW MODALS: SECURITY, SLIDES REORDER, AI RECOMMEND & LOGS SYSTEM ---

        // A. ADMIN PASSWORD DIALOG LAYER (PASSWORD: 4735)
        if (pendingAdminAction != null) {
            var inputPassword by remember { mutableStateOf("") }
            var isError by remember { mutableStateOf(false) }
            AlertDialog(
                onDismissRequest = {
                    pendingAdminAction = null
                    inputPassword = ""
                    isError = false
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Admin Authentication Required", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Please enter the admin password to authorize this formula edit mode or configuration action:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        OutlinedTextField(
                            value = inputPassword,
                            onValueChange = { 
                                inputPassword = it
                                isError = false
                            },
                            label = { Text("Admin Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            isError = isError,
                            modifier = Modifier.fillMaxWidth().testTag("admin_password_input"),
                            singleLine = true
                        )
                        if (isError) {
                            Text(
                                text = "Incorrect Admin Password!",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (inputPassword == adminPassword) {
                                viewModel.setAdminMode(true)
                                pendingAdminAction?.invoke()
                                pendingAdminAction = null
                                inputPassword = ""
                            } else {
                                isError = true
                            }
                        },
                        modifier = Modifier.testTag("admin_password_submit")
                    ) {
                        Text("Authorize")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            pendingAdminAction = null
                            inputPassword = ""
                            isError = false
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        // B. ADMIN RESET FORMULA PROTECTION DIALOG
        if (showResetConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showResetConfirmDialog = false },
                title = { Text("Reset Confirmation") },
                text = { Text("Are you sure you want to reset this formula? Factories presets will be fully restored.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.resetActiveSlide()
                            showResetConfirmDialog = false
                            Toast.makeText(context, "Active formula restored to defaults!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("YES")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetConfirmDialog = false }) {
                        Text("NO")
                    }
                }
            )
        }

        // C. ADD NEW SLIDES DISCOVERY PANEL (PLUS BUTTON ACTION WITH DYNAMIC 1-10 INPUTS AND 1-5 OUTPUTS)
        if (showAddSlideDialog) {
            var title by remember { mutableStateOf("") }
            var category by remember { mutableStateOf("Custom") }
            var desc by remember { mutableStateOf("Custom ChemDose Formula") }
            var numInputs by remember { mutableStateOf(2) }
            var numOutputs by remember { mutableStateOf(1) }

            // Dynamic specifications for up to 10 inputs
            var inputSymbols by remember { mutableStateOf(List(10) { index -> ('A' + index).toString() }) }
            var inputNames by remember { mutableStateOf(List(10) { index -> "Parameter ${index + 1}" }) }
            var inputValues by remember { mutableStateOf(List(10) { "1.0" }) }
            var inputUnits by remember { mutableStateOf(List(10) { "kg" }) }

            // Dynamic specifications for up to 5 outputs
            var outputNames by remember { mutableStateOf(List(5) { index -> if (index == 0) "Main Output" else "Sub Output $index" }) }
            var outputFormulas by remember { mutableStateOf(List(5) { index -> if (index == 0) "A * B" else "A + B" }) }
            var outputUnits by remember { mutableStateOf(List(5) { "kg/h" }) }

            AlertDialog(
                onDismissRequest = { showAddSlideDialog = false },
                title = { Text("Add Custom Formula Slide", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Formula Name") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Column {
                            Text("Category", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val catOptions = listOf("Custom", "Chemical Dosage", "Production", "Consistency")
                                catOptions.forEach { cat ->
                                    Button(
                                        onClick = { category = cat },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (category == cat) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (category == cat) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                    ) {
                                        Text(cat, fontSize = 10.sp)
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = desc,
                            onValueChange = { desc = it },
                            label = { Text("Description / Guidance") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Selector for number of inputs
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("No. of Inputs (1-10):", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { if (numInputs > 1) numInputs-- },
                                        enabled = numInputs > 1
                                    ) {
                                         Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Text(numInputs.toString(), fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 8.dp))
                                    IconButton(
                                        onClick = { if (numInputs < 10) numInputs++ },
                                        enabled = numInputs < 10
                                    ) {
                                         Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Selector for number of outputs
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("No. of Outputs (1-5):", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { if (numOutputs > 1) numOutputs-- },
                                        enabled = numOutputs > 1
                                    ) {
                                         Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Text(numOutputs.toString(), fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 8.dp))
                                    IconButton(
                                        onClick = { if (numOutputs < 5) numOutputs++ },
                                        enabled = numOutputs < 5
                                    ) {
                                         Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Inputs list specs definition
                        Text("Define Inputs (1 to $numInputs)", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                        for (i in 0 until numInputs) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("Input Parameter ${i + 1}", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    OutlinedTextField(
                                        value = inputSymbols[i],
                                        onValueChange = { sym ->
                                            val newList = inputSymbols.toMutableList()
                                            newList[i] = sym.replace(" ", "")
                                            inputSymbols = newList
                                        },
                                        label = { Text("Symbol") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = inputNames[i],
                                        onValueChange = { name ->
                                            val newList = inputNames.toMutableList()
                                            newList[i] = name
                                            inputNames = newList
                                        },
                                        label = { Text("Label") },
                                        modifier = Modifier.weight(2f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = inputUnits[i],
                                        onValueChange = { unit ->
                                            val newList = inputUnits.toMutableList()
                                            newList[i] = unit
                                            inputUnits = newList
                                        },
                                        label = { Text("Unit") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }
                            }
                        }

                        // Outputs list specs definition
                        Text("Define Outputs (1 to $numOutputs)", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                        for (i in 0 until numOutputs) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(if (i == 0) "Main Calculated Result" else "Supplementary Output ${i + 1}", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                OutlinedTextField(
                                    value = outputNames[i],
                                    onValueChange = { name ->
                                        val newList = outputNames.toMutableList()
                                        newList[i] = name
                                        outputNames = newList
                                    },
                                    label = { Text("Output Label / Name") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    OutlinedTextField(
                                        value = outputFormulas[i],
                                        onValueChange = { form ->
                                            val newList = outputFormulas.toMutableList()
                                            newList[i] = form
                                            outputFormulas = newList
                                        },
                                        label = { Text("Formula") },
                                        modifier = Modifier.weight(2f).testTag("output_formula_${i}"),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = outputUnits[i],
                                        onValueChange = { unit ->
                                            val newList = outputUnits.toMutableList()
                                            newList[i] = unit
                                            outputUnits = newList
                                        },
                                        label = { Text("Unit") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }

                                // Interactive layout preview & validation check
                                val parsedTokens = remember(outputFormulas[i]) {
                                    FormulaEvaluator.tokenize(outputFormulas[i])
                                }
                                val formattedPreview = remember(parsedTokens) {
                                    FormulaEvaluator.tokensToFormattedExpression(parsedTokens)
                                }
                                val definedInputSymbols = remember(inputSymbols, numInputs) {
                                    (0 until numInputs).map { inputSymbols[it].trim() }.filter { it.isNotEmpty() }
                                }
                                val mapForValidation = remember(definedInputSymbols) {
                                    definedInputSymbols.associate { it to 1.0 }
                                }
                                val syntaxError = remember(parsedTokens, mapForValidation) {
                                    FormulaEvaluator.validateTokens(parsedTokens, mapForValidation)
                                }

                                if (formattedPreview.isNotEmpty()) {
                                    Text("Preview: $formattedPreview", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
                                }
                                if (syntaxError != null) {
                                    Text("⚠️ $syntaxError", color = MaterialTheme.colorScheme.error, fontSize = 10.sp)
                                } else if (outputFormulas[i].isNotEmpty()) {
                                    Text("✅ Syntax Valid", color = Color(0xFF10B981), fontSize = 10.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                val primarySyntaxError = FormulaEvaluator.validateTokens(FormulaEvaluator.tokenize(outputFormulas[0]), (0 until numInputs).map { inputSymbols[it].trim() }.filter { it.isNotEmpty() }.associate { it to 1.0 })
                                if (title.isBlank() || outputFormulas[0].isBlank()) {
                                    Toast.makeText(context, "Title and primary Formula are required!", Toast.LENGTH_SHORT).show()
                                } else if (primarySyntaxError != null) {
                                    Toast.makeText(context, primarySyntaxError, Toast.LENGTH_LONG).show()
                                } else {
                                    confirmedAction = {
                                        val variablesToSave = mutableListOf<Variable>()
                                        for (i in 0 until numInputs) {
                                            variablesToSave.add(
                                                Variable(
                                                    slideId = 0,
                                                    symbol = inputSymbols[i],
                                                    name = inputNames[i],
                                                    value = inputValues[i].toDoubleOrNull() ?: 1.0,
                                                    unit = inputUnits[i]
                                                )
                                            )
                                        }

                                        // Primary output config
                                        val primFormula = FormulaSerializer.serialize(FormulaEvaluator.tokenize(outputFormulas[0]))
                                        val primUnit = outputUnits[0]

                                        // Append secondary outputs to slide description
                                        val descPart = desc
                                        val finalDesc = if (numOutputs > 1) {
                                            val outputsPart = (1 until numOutputs).map { i ->
                                                "${outputNames[i]}:${FormulaSerializer.serialize(FormulaEvaluator.tokenize(outputFormulas[i]))}:${outputUnits[i]}"
                                            }.joinToString("\n")
                                            "$descPart\n---outputs---\n$outputsPart"
                                        } else {
                                            descPart
                                        }

                                        viewModel.addNewCustomSlide(title, category, primFormula, primUnit, finalDesc, variablesToSave)
                                        showAddSlideDialog = false
                                        Toast.makeText(context, "New Formula slide registered!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("SAVE NEW FORMULA SLIDE")
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAddSlideDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // F. UNIFIED ADMIN ONLY COMMAND CENTER CONSOLE (FORMULAS, ACTIVITY LOGS, GLOBAL DESTROY RESTORE)
        if (showAdminCommandCenter) {
            val adminTabsList = listOf("Formulas", "Destruction Hub")

            AlertDialog(
                onDismissRequest = { showAdminCommandCenter = false },
                properties = DialogProperties(usePlatformDefaultWidth = false),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f)
                    .padding(16.dp),
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Admin",
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Unified Admin Command Center",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Horizontal Tab Row
                        TabRow(
                            selectedTabIndex = selectedAdminTab,
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.primary
                        ) {
                            adminTabsList.forEachIndexed { index, title ->
                                Tab(
                                    selected = selectedAdminTab == index,
                                    onClick = { selectedAdminTab = index },
                                    text = { Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                                )
                            }
                        }

                        // Tab Content
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            when (selectedAdminTab) {
                                0 -> {
                                    // 1. Management of Formulas
                                    val allSlidesList = allSlidesCount
                                    var formulaSearchQuery by remember { mutableStateOf("") }
                                    
                                    val filteredAdminSlides = remember(allSlidesList, formulaSearchQuery) {
                                        if (formulaSearchQuery.isBlank()) {
                                            allSlidesList
                                        } else {
                                            val queryLower = formulaSearchQuery.lowercase(Locale.US)
                                            allSlidesList.filter {
                                                val plainFormula = if (it.slide.formula.trim().startsWith("[")) {
                                                    val tokens = FormulaSerializer.deserialize(it.slide.formula) ?: FormulaEvaluator.tokenize(it.slide.formula)
                                                    FormulaEvaluator.tokensToPlainExpression(tokens)
                                                } else {
                                                    it.slide.formula
                                                }
                                                it.slide.title.lowercase(Locale.US).contains(queryLower) ||
                                                it.slide.category.lowercase(Locale.US).contains(queryLower) ||
                                                plainFormula.lowercase(Locale.US).contains(queryLower)
                                            }
                                        }
                                    }

                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = formulaSearchQuery,
                                            onValueChange = { formulaSearchQuery = it },
                                            placeholder = { Text("Search 100 industrial slots...") },
                                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) },
                                            modifier = Modifier.fillMaxWidth().testTag("formula_admin_search_input"),
                                            singleLine = true
                                        )

                                        LazyColumn(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            items(filteredAdminSlides) { slideWithVars ->
                                                val isCurrentActive = slideWithVars.slide.id == activeSlideId
                                                Card(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .border(
                                                            1.dp,
                                                            if (isCurrentActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                                            RoundedCornerShape(12.dp)
                                                        ),
                                                    colors = CardDefaults.cardColors(
                                                        containerColor = if (isCurrentActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                                                    )
                                                ) {
                                                    Column(modifier = Modifier.padding(10.dp)) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = "SLIDE ${slideWithVars.slide.id} • ${slideWithVars.slide.category.uppercase(Locale.US)}",
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (isCurrentActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                                            )
                                                            if (isCurrentActive) {
                                                                Text("ACTIVE", fontSize = 8.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                                                            }
                                                        }
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text(
                                                            text = slideWithVars.slide.title,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 13.sp,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        val displayFormulaText = remember(slideWithVars.slide.formula) {
                                                            val tokens = if (slideWithVars.slide.formula.trim().startsWith("[")) {
                                                                FormulaSerializer.deserialize(slideWithVars.slide.formula) ?: FormulaEvaluator.tokenize(slideWithVars.slide.formula)
                                                            } else {
                                                                FormulaEvaluator.tokenize(slideWithVars.slide.formula)
                                                            }
                                                            FormulaEvaluator.tokensToFormattedExpression(tokens)
                                                        }
                                                        Text(
                                                            text = "Formula: $displayFormulaText",
                                                            fontFamily = FontFamily.Monospace,
                                                            fontSize = 11.sp,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                        Spacer(modifier = Modifier.height(6.dp))
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            // Reorder Up and Down Actions
                                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                                IconButton(
                                                                    onClick = {
                                                                        val currentIndex = allSlidesList.indexOf(slideWithVars)
                                                                        if (currentIndex > 0) {
                                                                            val other = allSlidesList[currentIndex - 1]
                                                                            viewModel.swapSlides(slideWithVars.slide.id, other.slide.id)
                                                                            Toast.makeText(context, "Reordered Formula Up", Toast.LENGTH_SHORT).show()
                                                                        }
                                                                    },
                                                                    enabled = allSlidesList.indexOf(slideWithVars) > 0,
                                                                    modifier = Modifier.size(28.dp).testTag("reorder_up_${slideWithVars.slide.id}")
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.ArrowUpward,
                                                                        contentDescription = "Move Up",
                                                                        tint = if (allSlidesList.indexOf(slideWithVars) > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                                                        modifier = Modifier.size(16.dp)
                                                                    )
                                                                }
                                                                IconButton(
                                                                    onClick = {
                                                                        val currentIndex = allSlidesList.indexOf(slideWithVars)
                                                                        if (currentIndex < allSlidesList.size - 1) {
                                                                            val other = allSlidesList[currentIndex + 1]
                                                                            viewModel.swapSlides(slideWithVars.slide.id, other.slide.id)
                                                                            Toast.makeText(context, "Reordered Formula Down", Toast.LENGTH_SHORT).show()
                                                                        }
                                                                    },
                                                                    enabled = allSlidesList.indexOf(slideWithVars) < allSlidesList.size - 1,
                                                                    modifier = Modifier.size(28.dp).testTag("reorder_down_${slideWithVars.slide.id}")
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.ArrowDownward,
                                                                        contentDescription = "Move Down",
                                                                        tint = if (allSlidesList.indexOf(slideWithVars) < allSlidesList.size - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                                                        modifier = Modifier.size(16.dp)
                                                                    )
                                                                }
                                                            }

                                                            Row(
                                                                horizontalArrangement = Arrangement.End,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                IconButton(
                                                                    onClick = {
                                                                        viewModel.duplicateSlide(slideWithVars.slide.id)
                                                                        Toast.makeText(context, "Formula duplicated successfully!", Toast.LENGTH_SHORT).show()
                                                                    },
                                                                    modifier = Modifier.size(28.dp).testTag("duplicate_slide_${slideWithVars.slide.id}")
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.ContentCopy,
                                                                        contentDescription = "Duplicate Formula",
                                                                        tint = MaterialTheme.colorScheme.secondary,
                                                                        modifier = Modifier.size(16.dp)
                                                                    )
                                                                }
                                                                Spacer(modifier = Modifier.width(4.dp))
                                                                IconButton(
                                                                    onClick = {
                                                                        viewModel.deleteSlide(slideWithVars.slide.id)
                                                                        Toast.makeText(context, "Formula deleted successfully!", Toast.LENGTH_SHORT).show()
                                                                    },
                                                                    modifier = Modifier.size(28.dp).testTag("delete_slide_${slideWithVars.slide.id}")
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.Delete,
                                                                        contentDescription = "Delete Formula",
                                                                        tint = MaterialTheme.colorScheme.error,
                                                                        modifier = Modifier.size(16.dp)
                                                                    )
                                                                }
                                                                Spacer(modifier = Modifier.width(4.dp))
                                                                TextButton(
                                                                    onClick = {
                                                                        viewModel.selectSlide(slideWithVars.slide.id)
                                                                        Toast.makeText(context, "Navigated to Slide ${slideWithVars.slide.id}", Toast.LENGTH_SHORT).show()
                                                                    },
                                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                                ) {
                                                                    Text("Navigate", fontSize = 11.sp)
                                                                }
                                                                Spacer(modifier = Modifier.width(4.dp))
                                                                Button(
                                                                    onClick = {
                                                                        viewModel.selectSlide(slideWithVars.slide.id)
                                                                        showEditSlideMetaDialog = true
                                                                    },
                                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                                ) {
                                                                     Icon(if (slideWithVars.slide.id > 10) Icons.Default.Edit else Icons.Default.Info, contentDescription = "Edit", modifier = Modifier.size(10.dp))
                                                                    Spacer(modifier = Modifier.width(2.dp))
                                                                     Text(if (slideWithVars.slide.id > 10) "Modify Details" else "View Details", fontSize = 11.sp)
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                1 -> {
                                    // 3. Destruction Hub - Global Database reset with confirmation security layer
                                    var confirmationText by remember { mutableStateOf("") }
                                    val isConfirmValValid = confirmationText == "CONFIRM RESET"

                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(rememberScrollState()),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Card(
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                                            ),
                                            border = androidx.compose.foundation.BorderStroke(
                                                1.dp,
                                                MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Warning, contentDescription = "Critical Alert", tint = MaterialTheme.colorScheme.error)
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        "CRITICAL SECURITY RESTORE AREA",
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                                Text(
                                                    text = "Executing a global reset will completely purge the local SQLite database. All customized formulas, newly registered parameters, and modified default presets will be permanently erased. Factory slide slots (1 to 100) will be fully re-instated and restored to factory specifications.",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                                    lineHeight = 16.sp
                                                )
                                            }
                                        }

                                        Text(
                                            text = "Requested Confirmation Security Layer",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                text = "Please write the exact phrase \"CONFIRM RESET\" below to authorize this complete factory rebuilding sequence:",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            )
                                            OutlinedTextField(
                                                value = confirmationText,
                                                onValueChange = { confirmationText = it },
                                                placeholder = { Text("CONFIRM RESET") },
                                                isError = confirmationText.isNotEmpty() && !isConfirmValValid,
                                                modifier = Modifier.fillMaxWidth().testTag("global_reset_confirmation_input"),
                                                singleLine = true
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                if (isConfirmValValid) {
                                                    viewModel.triggerGlobalWipeReset()
                                                    Toast.makeText(context, "Global Database Fully Reset to Defaults!", Toast.LENGTH_LONG).show()
                                                    viewModel.logAction("GLOBAL_RESET", "Operator executed secure global factory reset successfully.")
                                                    showAdminCommandCenter = false
                                                }
                                            },
                                            enabled = isConfirmValValid,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.error,
                                                disabledContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.3f),
                                                contentColor = Color.White
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp)
                                                .testTag("global_reset_destructive_button")
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                "EXECUTE DESTRUCTIVE GLOBAL RESET",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showAdminCommandCenter = false }
                    ) {
                        Text("Close Room")
                    }
                }
            )
        }

        // Link Option popup
        if (showLinkPopup) {
            AlertDialog(
                onDismissRequest = { showLinkPopup = false },
                title = { Text("Update C-Starch Calculator?", fontWeight = FontWeight.Bold) },
                text = { Text("Would you like to sync the newly computed ASA Flow rate (${String.format(Locale.US, "%,.2f", pendingAsaFlowValue)} kg/H) with the C-Starch Calculator?") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.updateStarchKitchenAsaFlowInput(pendingAsaFlowValue)
                            showLinkPopup = false
                            Toast.makeText(context, "C-Starch kitchen updated!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Yes")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showLinkPopup = false
                        }
                    ) {
                        Text("No")
                    }
                }
            )
        }

        // Generic Confirmation dialog layer
        if (confirmedAction != null) {
            AlertDialog(
                onDismissRequest = { confirmedAction = null },
                title = { Text("Are you sure?", fontWeight = FontWeight.Bold) },
                text = { Text("Please confirm if you want to proceed with this action.") },
                confirmButton = {
                    Button(
                        onClick = {
                            confirmedAction?.invoke()
                            confirmedAction = null
                        }
                    ) {
                        Text("Yes")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { confirmedAction = null }) {
                        Text("No")
                    }
                }
            )
        }

        // Equation / Formula detail dialog showing large fraction
        if (showFormulaDetailDialog) {
            val currentSlide = activeSlideOpt
            if (currentSlide != null) {
                AlertDialog(
                    onDismissRequest = { showFormulaDetailDialog = false },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Formula Icon",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Mathematical Equation Detail",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = currentSlide.slide.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            // Beautiful Fraction Display in Dialog
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    MathematicalFractionView(
                                        formula = currentSlide.slide.formula,
                                        variables = currentSlide.variables,
                                        resultUnit = currentSlide.slide.resultUnit,
                                        title = currentSlide.slide.title,
                                        baseFontSize = 14.sp,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }

                            // Variable legend mapping symbols to their names
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "VARIABLE LEGEND",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                
                                currentSlide.variables.forEach { variable ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${variable.symbol} : ${variable.name}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = variable.unit,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = { showFormulaDetailDialog = false },
                            modifier = Modifier.testTag("close_formula_dialog_btn")
                        ) {
                            Text("CLOSE", fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }
        }

        // --- NEW: History Deletion & Clearing Dialogs ---
        if (logToDelete != null) {
            AlertDialog(
                onDismissRequest = { logToDelete = null },
                title = { Text("Delete History Record", fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to delete this calculation record?\nInputs: ${logToDelete?.inputs}\nResult: ${logToDelete?.result}\nTimestamp: ${logToDelete?.timestamp}") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            logToDelete?.let { viewModel.deleteLocalHistoryLog(it) }
                            logToDelete = null
                        },
                        modifier = Modifier.testTag("confirm_delete_history_item")
                    ) {
                        Text("DELETE", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { logToDelete = null }) {
                        Text("CANCEL")
                    }
                }
            )
        }

        if (showClearMyHistoryDialog) {
            AlertDialog(
                onDismissRequest = { showClearMyHistoryDialog = false },
                title = { Text("Clear Your History", fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to clear your local calculation history on this device? This action cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.clearUserHistory()
                            showClearMyHistoryDialog = false
                        },
                        modifier = Modifier.testTag("confirm_clear_my_history")
                    ) {
                        Text("CLEAR MY HISTORY", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearMyHistoryDialog = false }) {
                        Text("CANCEL")
                    }
                }
            )
        }

        if (showClearAllHistoryDialog) {
            AlertDialog(
                onDismissRequest = { showClearAllHistoryDialog = false },
                title = { Text("Clear All Calculation History", fontWeight = FontWeight.Bold) },
                text = { Text("ADMIN OVERRIDE: Are you sure you want to permanently delete ALL local calculation history records on this device? This action cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.clearAllHistory()
                            showClearAllHistoryDialog = false
                        },
                        modifier = Modifier.testTag("confirm_clear_all_history")
                    ) {
                        Text("CLEAR ALL HISTORY", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearAllHistoryDialog = false }) {
                        Text("CANCEL")
                    }
                }
            )
        }
        }
    }
}

/**
 * Custom dynamic Variable Input row. Handles keyboard typing elegantly, preserving decimals.
 */
@Composable
fun VariableInputRow(
    variable: Variable,
    isAdmin: Boolean,
    defaultFlowUnit: String = "LPH",
    onValueChange: (Double) -> Unit,
    onEditSpecs: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null
) {
    val focusManager = LocalFocusManager.current
    
    val isVariableFlow = FlowConverter.isFlowUnit(variable.unit)
    val displaysVarUnit = if (isVariableFlow) defaultFlowUnit else variable.unit

    // Maintain a local String state representing current text typing so decimals, blanks, and negative signs can be typed
    var localTextState by remember(variable.id, variable.value, defaultFlowUnit) {
        val initialDisplayVal = if (isVariableFlow) {
            FlowConverter.convert(variable.value, variable.unit, defaultFlowUnit)
        } else {
            variable.value
        }
        val initialTextValue = if (initialDisplayVal == 0.0) "" else initialDisplayVal.toString()
        mutableStateOf(initialTextValue)
    }

    var dragAccumulatedY by remember { mutableStateOf(0f) }

    val isValidationWarning = variable.isRequired && localTextState.isBlank()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isAdmin) {
                    Modifier.pointerInput(variable.id) {
                        detectDragGestures(
                            onDragStart = { dragAccumulatedY = 0f },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragAccumulatedY += dragAmount.y
                                if (dragAccumulatedY > 150f) {
                                    onMoveDown?.invoke()
                                    dragAccumulatedY = 0f
                                } else if (dragAccumulatedY < -150f) {
                                    onMoveUp?.invoke()
                                    dragAccumulatedY = 0f
                                }
                            },
                            onDragEnd = { dragAccumulatedY = 0f }
                        )
                    }
                } else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (variable.isHidden) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isValidationWarning) {
                MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (variable.isHidden) 0.15f else 0.4f)
            }
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (variable.isHidden) 0.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left block - Drag Handle (Admin only)
            if (isAdmin) {
                Column(
                    modifier = Modifier.padding(end = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    IconButton(
                        onClick = { onMoveUp?.invoke() },
                        enabled = onMoveUp != null,
                        modifier = Modifier.size(24.dp).testTag("move_up_${variable.symbol}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Move Up",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Drag Handle",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                    IconButton(
                        onClick = { onMoveDown?.invoke() },
                        enabled = onMoveDown != null,
                        modifier = Modifier.size(24.dp).testTag("move_down_${variable.symbol}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Move Down",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Specs Column
            Column(modifier = Modifier.weight(1.3f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (variable.isHidden) MaterialTheme.colorScheme.secondaryContainer
                                else MaterialTheme.colorScheme.primaryContainer
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = variable.symbol,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (variable.isHidden) MaterialTheme.colorScheme.onSecondaryContainer
                            else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = buildString {
                            append(variable.name)
                            if (variable.isRequired) append(" *")
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (variable.isHidden) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (isVariableFlow && displaysVarUnit != variable.unit) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Original base: " + String.format(Locale.US, "%.2f", variable.value) + " " + variable.unit,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    if (variable.isHidden) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "Hidden from users",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    if (isAdmin) {
                        Text(
                            text = "Specs / Delete",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable { onEditSpecs() }
                                .padding(vertical = 4.dp)
                                .testTag("edit_specs_${variable.symbol}")
                        )
                    } else if (variable.isRequired) {
                        Text(
                            text = "Required",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isValidationWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    } else {
                        Text(
                            text = "Optional",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Value Input Field Box
            OutlinedTextField(
                value = localTextState,
                onValueChange = { newText ->
                    // Standard sanitization supporting negative values and digits
                    var sanitizedText = newText.replace(",", ".")
                    if (sanitizedText.count { it == '.' } <= 1) {
                        localTextState = sanitizedText
                        
                        val doubleVal = sanitizedText.toDoubleOrNull()
                        if (doubleVal != null) {
                            val databaseReadyVal = if (isVariableFlow) {
                                FlowConverter.convert(doubleVal, defaultFlowUnit, variable.unit)
                            } else {
                                doubleVal
                            }
                            onValueChange(databaseReadyVal)
                        } else if (sanitizedText.isEmpty() || sanitizedText == "-") {
                            onValueChange(0.0) // fallback during dynamic editing
                        }
                    }
                },
                placeholder = { 
                    Text(
                        text = if (variable.isRequired) "Required" else "0.0", 
                        fontSize = 11.sp, 
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    ) 
                },
                suffix = {
                    Text(
                        text = displaysVarUnit,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                ),
                maxLines = 1,
                singleLine = true,
                modifier = Modifier
                    .weight(1.2f)
                    .height(52.dp)
                    .testTag("val_input_${variable.symbol}"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (isValidationWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = if (isValidationWarning) MaterialTheme.colorScheme.error.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                    focusedContainerColor = MaterialTheme.colorScheme.background,
                    unfocusedContainerColor = MaterialTheme.colorScheme.background
                ),
                shape = RoundedCornerShape(10.dp)
            )
        }
    }
}

/**
 * Text selection container support helper. Allows copying output.
 */
@Composable
fun SelectionContainer(content: @Composable () -> Unit) {
    androidx.compose.foundation.text.selection.SelectionContainer {
        content()
    }
}

@Composable
fun NeumorphicCard(
    modifier: Modifier = Modifier,
    cornerRadius: androidx.compose.ui.unit.Dp = 20.dp,
    containerColor: Color = Color.Unspecified,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val baseBg = if (containerColor == Color.Unspecified) MaterialTheme.colorScheme.surface else containerColor
    val isDark = MaterialTheme.colorScheme.background == Color(0xFF090A0F)
    val shadowDark = if (isDark) Color(0xFF020617).copy(alpha = 0.6f) else Color(0xFF9E9E9E).copy(alpha = 0.25f)
    val shadowLight = if (isDark) Color(0xFF334155).copy(alpha = 0.15f) else Color(0xFFFFFFFF).copy(alpha = 0.9f)
    
    Box(
        modifier = modifier
            .padding(6.dp)
    ) {
        // Dark shadow (Bottom-Right)
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(4.dp, 4.dp)
                .background(shadowDark, RoundedCornerShape(cornerRadius))
        )
        // Light shadow (Top-Left)
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset((-4).dp, (-4).dp)
                .background(shadowLight, RoundedCornerShape(cornerRadius))
        )
        // Main Surface with subtle border and optional click trigger
        Box(
            modifier = Modifier
                .background(baseBg, RoundedCornerShape(cornerRadius))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(cornerRadius)
                )
                .then(
                    if (onClick != null) {
                        Modifier.clickable { onClick() }
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
fun NeumorphicButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cornerRadius: androidx.compose.ui.unit.Dp = 20.dp,
    containerColor: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val baseContainerColor = if (containerColor == Color.Unspecified) MaterialTheme.colorScheme.surface else containerColor
    val baseContentColor = if (contentColor == Color.Unspecified) MaterialTheme.colorScheme.onSurface else contentColor
    
    val isDark = MaterialTheme.colorScheme.background == Color(0xFF090A0F)
    val shadowDark = if (isDark) Color(0xFF020617).copy(alpha = 0.6f) else Color(0xFF9E9E9E).copy(alpha = if (enabled) 0.25f else 0.05f)
    val shadowLight = if (isDark) Color(0xFF334155).copy(alpha = 0.15f) else Color(0xFFFFFFFF).copy(alpha = if (enabled) 0.9f else 0.3f)

    Box(
        modifier = modifier
            .padding(6.dp)
    ) {
        // Dark shadow (Bottom-Right)
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(3.dp, 3.dp)
                .background(shadowDark, RoundedCornerShape(cornerRadius))
        )
        // Light shadow (Top-Left)
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset((-3).dp, (-3).dp)
                .background(shadowLight, RoundedCornerShape(cornerRadius))
        )
        // Main button touch area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (enabled) baseContainerColor else baseContainerColor.copy(alpha = 0.5f), RoundedCornerShape(cornerRadius))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (enabled) 0.5f else 0.2f), RoundedCornerShape(cornerRadius))
                .clickable(enabled = enabled) { onClick() }
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompositionLocalProvider(LocalContentColor provides if (enabled) baseContentColor else baseContentColor.copy(alpha = 0.4f)) {
                content()
            }
        }
    }
}

@Composable
fun MathematicalFractionView(
    formula: String,
    variables: List<Variable>,
    resultUnit: String,
    title: String,
    modifier: Modifier = Modifier,
    baseFontSize: androidx.compose.ui.unit.TextUnit = 11.sp
) {
    val trimmed = formula.trim()
    val tokens = remember(trimmed) {
        if (trimmed.startsWith("[")) {
            FormulaSerializer.deserialize(trimmed) ?: FormulaEvaluator.tokenize(trimmed)
        } else {
            FormulaEvaluator.tokenize(trimmed)
        }
    }

    val fractionSplit = remember(tokens) {
        splitTokensByDivision(tokens)
    }

    val resultLabel = title.replace(" Calculator", "").trim()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            Text(
                text = resultLabel,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            if (resultUnit.isNotEmpty()) {
                Text(
                    text = " ($resultUnit)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Text(
                text = " =",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        if (fractionSplit != null) {
            val numDisplay = translateTokensToDescriptions(fractionSplit.first, variables)
            val denDisplay = translateTokensToDescriptions(fractionSplit.second, variables)

            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = numDisplay,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 2.dp).fillMaxWidth()
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .padding(vertical = 4.dp)
                        .height(2.dp)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                )

                Text(
                    text = denDisplay,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 2.dp).fillMaxWidth()
                )
            }
        } else {
            val fullExpressionDesc = translateTokensToDescriptions(tokens, variables)
            Text(
                text = fullExpressionDesc,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            )
        }
    }
}

fun splitTokensByDivision(tokens: List<FormulaToken>): Pair<List<FormulaToken>, List<FormulaToken>>? {
    var parenCount = 0
    var splitIndex = -1
    for (i in tokens.indices) {
        val t = tokens[i]
        if (t.type == "bracket" && t.value == "(") parenCount++
        else if (t.type == "bracket" && t.value == ")") parenCount--
        else if (t.type == "operator" && (t.value == "/" || t.value == "÷") && parenCount == 0) {
            splitIndex = i
            break
        }
    }
    if (splitIndex != -1) {
        return Pair(tokens.subList(0, splitIndex), tokens.subList(splitIndex + 1, tokens.size))
    }
    return null
}

fun translateTokensToDescriptions(tokens: List<FormulaToken>, variables: List<Variable>): String {
    val result = tokens.joinToString("") { token ->
        when (token.type) {
            "variable" -> {
                val variable = variables.find { 
                    it.symbol.equals(token.value, ignoreCase = true) || 
                    it.name.equals(token.value, ignoreCase = true) ||
                    "${it.name} (${it.unit})".equals(token.value, ignoreCase = true)
                }
                if (variable != null) {
                    val unitSuffix = if (variable.unit.isNotEmpty()) " (${variable.unit})" else ""
                    " ${variable.name}$unitSuffix "
                } else {
                    " ${token.value} "
                }
            }
            "operator" -> {
                when (token.value) {
                    "*" -> " × "
                    "×" -> " × "
                    "/" -> " ÷ "
                    "÷" -> " ÷ "
                    "+" -> " + "
                    "-" -> " - "
                    else -> " ${token.value} "
                }
            }
            "bracket" -> {
                if (token.value == "(") " (" else ") "
            }
            else -> " ${token.value} "
        }
    }
    return result.trim().replace(Regex("\\s+"), " ")
}

fun splitFraction(formula: String): Pair<String, String>? {
    var parenCount = 0
    var splitIndex = -1
    for (i in formula.indices) {
        val char = formula[i]
        if (char == '(') parenCount++
        else if (char == ')') parenCount--
        else if (char == '/' && parenCount == 0) {
            splitIndex = i
            break
        }
    }
    if (splitIndex != -1) {
        val num = formula.substring(0, splitIndex).trim()
        val den = formula.substring(splitIndex + 1).trim()
        return Pair(cleanParens(num), cleanParens(den))
    }
    return null
}

fun cleanParens(s: String): String {
    var current = s.trim()
    while (current.startsWith("(") && current.endsWith(")")) {
        var balance = 0
        var isGlobal = true
        for (i in 0 until current.length - 1) {
            val c = current[i]
            if (c == '(') balance++
            else if (c == ')') balance--
            if (balance == 0) {
                isGlobal = false
                break
            }
        }
        if (isGlobal && balance == 1) {
            current = current.substring(1, current.length - 1).trim()
        } else {
            break
        }
    }
    return current
}

fun translateFormulaToDescriptions(expr: String, variables: List<Variable>): String {
    val trimmed = expr.trim()
    val tokens = if (trimmed.startsWith("[")) {
        FormulaSerializer.deserialize(trimmed) ?: FormulaEvaluator.tokenize(trimmed)
    } else {
        FormulaEvaluator.tokenize(trimmed)
    }
    return translateTokensToDescriptions(tokens, variables)
}

@Composable
fun DosingMathematicalFractionView(
    mode: String,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        val lhs = when (mode) {
            "Dosage" -> "Dosage"
            "Flow" -> "Flow (LPM)"
            "Production" -> "Production (TPD)"
            "GPL" -> "GPL (g/L)"
            else -> ""
        }
        Text(
            text = lhs,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = " =",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            modifier = Modifier.padding(horizontal = 6.dp)
        )
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val numerator = when (mode) {
                "Dosage" -> "Flow (LPM) × GPL × 1.44"
                "Flow" -> "Dosage × Production"
                "Production" -> "Flow (LPM) × GPL × 1.44"
                "GPL" -> "Dosage × Production"
                else -> ""
            }
            val denominator = when (mode) {
                "Dosage" -> "Production (TPD)"
                "Flow" -> "GPL × 1.44"
                "Production" -> "Dosage"
                "GPL" -> "Flow (LPM) × 1.44"
                else -> ""
            }
            
            Text(
                text = numerator,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Box(
                modifier = Modifier
                    .padding(vertical = 3.dp)
                    .height(1.5.dp)
                    .width(180.dp)
                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f))
            )
            Text(
                text = denominator,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
fun DosingInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    unit: String,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0x3B1E293B) else MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isDark) Color(0x3094A3B8) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label.uppercase(Locale.US),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.5.sp
                )
                androidx.compose.foundation.text.BasicTextField(
                    value = value,
                    onValueChange = { input ->
                        val sanitized = input.replace(",", ".").filter { it.isDigit() || it == '.' }
                        if (sanitized.count { it == '.' } <= 1) {
                            onValueChange(sanitized)
                        }
                    },
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = FontFamily.Monospace
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp)
                        .testTag(testTag)
                )
            }
            if (value.isNotEmpty()) {
                IconButton(
                    onClick = { onValueChange("") },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear input",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Text(
                text = unit,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

private fun formatDouble(v: Double): String {
    return if (v % 1.0 == 0.0) {
        String.format(java.util.Locale.US, "%,.0f", v)
    } else {
        String.format(java.util.Locale.US, "%,.2f", v)
    }
}

@Composable
fun LoginSelectionScreen(viewModel: CalculatorViewModel) {
    val isAuthenticating by viewModel.isAuthenticating.collectAsStateWithLifecycle()
    val authError by viewModel.authError.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedMode by remember { mutableStateOf("user_login") } // "user_login", "user_signup", "admin_login", "forgot"

    // Form inputs state
    var emailOrUserIn by remember { mutableStateOf("") }
    var userPassIn by remember { mutableStateOf("") }

    var signUpName by remember { mutableStateOf("") }
    var signUpEmail by remember { mutableStateOf("") }
    var signUpUserId by remember { mutableStateOf("") }
    var signUpPass by remember { mutableStateOf("") }
    var signUpConfirmPass by remember { mutableStateOf("") }

    var adminPassIn by remember { mutableStateOf("") }

    var forgotEmail by remember { mutableStateOf("") }
    var forgotRoleSearch by remember { mutableStateOf("password") } // "password", "userId"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("login_selection_container"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Logo
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF3B82F6),
                                Color(0xFF10B981)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🏭",
                    fontSize = 32.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "ChemDose Formula Control",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Secure Chemical Operator Portal System",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Selector Tabs (Material 3 Pill Selector Row)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val modes = listOf(
                    "user_login" to "Login",
                    "user_signup" to "Sign Up",
                    "admin_login" to "Admin",
                    "forgot" to "Forgot"
                )
                modes.forEach { (mode, label) ->
                    val isSelected = selectedMode == mode
                    Button(
                        onClick = { selectedMode = mode },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("tab_$mode"),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Credentials Card
            NeumorphicCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 20.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(18.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    when (selectedMode) {
                        "user_login" -> {
                            Text(
                                text = "Operator Login",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Enter your Email or custom User ID to access the Chemical Operator Console:",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            OutlinedTextField(
                                value = emailOrUserIn,
                                onValueChange = { emailOrUserIn = it },
                                label = { Text("Email or User ID", fontSize = 14.sp) },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .testTag("login_user_email_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = userPassIn,
                                onValueChange = { userPassIn = it },
                                label = { Text("Password", fontSize = 14.sp) },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .testTag("login_user_password_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = {
                                    if (emailOrUserIn.isBlank() || userPassIn.isBlank()) {
                                        Toast.makeText(context, "Please enter all credential fields", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    viewModel.signInWithCredentials(emailOrUserIn.trim(), userPassIn) { success, msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("login_submit_btn"),
                                enabled = !isAuthenticating,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isAuthenticating) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp).testTag("login_spinner"), color = MaterialTheme.colorScheme.onPrimary)
                                } else {
                                    Text("Log In to Console", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        "user_signup" -> {
                            Text(
                                text = "Create Operator Account",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Register profile parameters below. New registers default to safe 'operator' roles.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            OutlinedTextField(
                                value = signUpEmail,
                                onValueChange = { signUpEmail = it },
                                label = { Text("Email Address", fontSize = 14.sp) },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .testTag("signup_email_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = signUpPass,
                                onValueChange = { signUpPass = it },
                                label = { Text("Secret Password", fontSize = 14.sp) },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .testTag("signup_password_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = signUpConfirmPass,
                                onValueChange = { signUpConfirmPass = it },
                                label = { Text("Confirm Password", fontSize = 14.sp) },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .testTag("signup_confirm_password_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = {
                                    if (signUpEmail.isBlank() || signUpPass.isBlank() || signUpConfirmPass.isBlank()) {
                                        Toast.makeText(context, "Please complete all registration parameters", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(signUpEmail.trim()).matches()) {
                                        Toast.makeText(context, "Invalid email address format.", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (signUpPass.length < 6) {
                                        Toast.makeText(context, "Password must be at least 6 characters.", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (signUpPass != signUpConfirmPass) {
                                        Toast.makeText(context, "Passwords do not match.", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    viewModel.signUpUser(signUpEmail.trim(), signUpPass, signUpConfirmPass) { success, msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("signup_submit_btn"),
                                enabled = !isAuthenticating,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isAuthenticating) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp).testTag("login_spinner"), color = MaterialTheme.colorScheme.onPrimary)
                                } else {
                                    Text("Register Console Profile", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        "admin_login" -> {
                            Text(
                                text = "System Administrator Sign-On",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                "Enter master Administrator console passcode. Master admin accounts are restricted access.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            OutlinedTextField(
                                value = adminPassIn,
                                onValueChange = { adminPassIn = it },
                                label = { Text("Admin Passcode", fontSize = 14.sp) },
                                leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .testTag("admin_password_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = {
                                    if (adminPassIn.isBlank()) {
                                        Toast.makeText(context, "Please enter admin passcode", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    viewModel.loginAsAdminWithPassword(adminPassIn) { success, msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("admin_submit_btn"),
                                enabled = !isAuthenticating,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isAuthenticating) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp).testTag("login_spinner"), color = MaterialTheme.colorScheme.onPrimary)
                                } else {
                                    Text("Authenticate Admin Rights", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                        "forgot" -> {
                            Text(
                                text = "Forgot Credentials Link",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Lookup User ID or trigger password resets to registered emails:",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            
                            // Segmented Option Toggle
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val isResetSel = forgotRoleSearch == "password"
                                Button(
                                    onClick = { forgotRoleSearch = "password" },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isResetSel) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        contentColor = if (isResetSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).height(36.dp).testTag("forgot_choice_password"),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Reset Password", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { forgotRoleSearch = "id" },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (!isResetSel) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        contentColor = if (!isResetSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).height(36.dp).testTag("forgot_choice_id"),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Find User ID", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            OutlinedTextField(
                                value = forgotEmail,
                                onValueChange = { forgotEmail = it },
                                label = { Text("Registered Email Address") },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .testTag("forgot_email_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = {
                                    if (forgotEmail.isBlank() || !forgotEmail.contains("@")) {
                                        Toast.makeText(context, "Please enter a valid email address layout.", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (forgotRoleSearch == "password") {
                                        viewModel.sendPasswordReset(forgotEmail.trim()) { success, msg ->
                                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                        }
                                    } else {
                                        viewModel.lookupUserId(forgotEmail.trim()) { success, msg ->
                                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("forgot_submit_btn"),
                                enabled = !isAuthenticating,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isAuthenticating) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp).testTag("login_spinner"), color = MaterialTheme.colorScheme.onPrimary)
                                } else {
                                    Text(if (forgotRoleSearch == "password") "Send Reset Link" else "Query User ID", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Global Auth Error Display
                    authError?.let { err ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_error_card"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = err,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(10.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

