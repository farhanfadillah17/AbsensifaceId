package com.example.attendanceapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.attendanceapp.ui.theme.AttendanceAppTheme

// 1. Data Class untuk Menu
data class MenuConfig(
    val title: String,
    val icon: ImageVector,
    val roles: List<String>,
    val hasSubMenu: Boolean = false,
    val color: Color = Color(0xFF1A3A8F)
)

// 2. Enum Screen Lengkap
enum class Screen {
    LOGIN, DASHBOARD, HOME, EMPLOYEE_FORM, REGISTER_FACE,
    QR_SCAN, FACE_VERIFY, HISTORY, PROGRESS_MENU, PROGRESS_FORM,
    FRUIT_COUNTING, ANCAK_PANEN, SPB_MENU, SPB_FORM, AKP_FORM, RKH_VIEW
}

class MainActivity : ComponentActivity() {
    private lateinit var db: AttendanceDatabaseHelper
    private lateinit var faceHelper: FaceDataHelper
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        db = AttendanceDatabaseHelper(this)
        faceHelper = FaceDataHelper(db)
        sessionManager = SessionManager(this)

        setContent {
            AttendanceAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White
                ) {
                    AppNavigation(db = db, faceHelper = faceHelper, sessionManager = sessionManager)
                }
            }
        }
    }
}

@Composable
fun AppNavigation(
    db: AttendanceDatabaseHelper,
    faceHelper: FaceDataHelper,
    sessionManager: SessionManager,
) {
    val context = LocalContext.current
    val initialScreen = if (sessionManager.isLoggedIn()) Screen.DASHBOARD else Screen.LOGIN
    val backStack = remember { mutableStateListOf(initialScreen) }
    val currentScreen = backStack.last()

    // State Antar Screen
    var pendingEmployee by remember { mutableStateOf<Employee?>(null) }
    var currentAction by remember { mutableStateOf(AttendanceAction.CHECK_IN) }
    var verifiedEmployee by remember { mutableStateOf<Employee?>(null) }
    var selectedCategory by remember { mutableStateOf("") }
    var selectedSpbCategory by remember { mutableStateOf("") }

    fun navigateTo(screen: Screen) { backStack.add(screen) }
    fun navigateBack() { if (backStack.size > 1) backStack.removeLast() }
    fun navigateDashboard() {
        backStack.clear()
        backStack.add(Screen.DASHBOARD)
    }

    fun logout() {
        sessionManager.logout()
        backStack.clear()
        backStack.add(Screen.LOGIN)
    }

    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    DisposableEffect(dispatcher, currentScreen) {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (currentScreen == Screen.DASHBOARD || currentScreen == Screen.LOGIN) return
                navigateBack()
            }
        }
        dispatcher?.addCallback(callback)
        onDispose { callback.remove() }
    }

    when (currentScreen) {
        Screen.LOGIN -> LoginScreen(dbHelper = db, sessionManager = sessionManager, onLoginSuccess = { navigateDashboard() })

        Screen.DASHBOARD -> DashboardScreen(
            userName = sessionManager.getUserName() ?: "User",
            userRole = sessionManager.getUserRole() ?: "MANDOR",
            onNavigate = { screen -> navigateTo(screen) },
            onLogout = { logout() }
        )

        Screen.HOME -> HomeScreen(
            sessionManager = sessionManager, dbHelper = db,
            onRegisterFace = { navigateTo(Screen.EMPLOYEE_FORM) },
            onCheckIn = { currentAction = AttendanceAction.CHECK_IN; navigateTo(Screen.QR_SCAN) },
            onCheckOut = { currentAction = AttendanceAction.CHECK_OUT; navigateTo(Screen.QR_SCAN) },
            onHistory = { navigateTo(Screen.HISTORY) },
            onLogout = { logout() },
            onFeature2 = { navigateTo(Screen.PROGRESS_MENU) },
            onFeature3 = { Toast.makeText(context, "Fitur Laporan", Toast.LENGTH_SHORT).show() }
        )

        Screen.EMPLOYEE_FORM -> EmployeeFormScreen(dbHelper = db, onBack = { navigateBack() }, onNext = { emp -> pendingEmployee = emp; navigateTo(Screen.REGISTER_FACE) })

        Screen.REGISTER_FACE -> {
            pendingEmployee?.let { emp ->
                RegisterFaceScreen(dbHelper = db, employee = emp, faceDataHelper = faceHelper, onBack = { navigateBack() }, onSuccess = { pendingEmployee = null; navigateDashboard() })
            } ?: LaunchedEffect(Unit) { navigateBack() }
        }

        Screen.QR_SCAN -> QRScannerScreen(action = currentAction, dbHelper = db, onBack = { navigateBack() }, onQRVerified = { emp -> verifiedEmployee = emp; navigateTo(Screen.FACE_VERIFY) })

        Screen.FACE_VERIFY -> {
            verifiedEmployee?.let { emp ->
                AttendanceCameraScreen(action = currentAction, verifiedEmployee = emp, faceDataHelper = faceHelper, dbHelper = db, onBack = { navigateBack() }, onSuccess = { verifiedEmployee = null; navigateDashboard() })
            } ?: LaunchedEffect(Unit) { navigateBack() }
        }

        Screen.HISTORY -> AttendanceHistoryScreen(dbHelper = db, onBack = { navigateBack() })

        Screen.PROGRESS_MENU -> ProgressMenuScreen(onBack = { navigateBack() }, onCategorySelected = { cat -> selectedCategory = cat; navigateTo(Screen.PROGRESS_FORM) })

        Screen.PROGRESS_FORM -> ProgressFormScreen(category = selectedCategory, dbHelper = db, empId = sessionManager.getFccode() ?: "", onBack = { navigateBack() }, onSuccess = { navigateDashboard() })

        Screen.FRUIT_COUNTING -> FruitCountingScreen(dbHelper = db, empId = sessionManager.getFccode() ?: "", onBack = { navigateBack() }, onSuccess = { navigateDashboard() })

        Screen.ANCAK_PANEN -> AncakPanenScreen(dbHelper = db, empId = sessionManager.getFccode() ?: "", onBack = { navigateBack() }, onSuccess = { navigateDashboard() })

        Screen.SPB_MENU -> SPBMenuScreen(onBack = { navigateBack() }, onCategorySelected = { cat -> selectedSpbCategory = cat; navigateTo(Screen.SPB_FORM) })

        Screen.SPB_FORM -> SPBFormScreen(category = selectedSpbCategory, dbHelper = db, empId = sessionManager.getFccode() ?: "", onBack = { navigateBack() }, onSuccess = { navigateDashboard() })

        Screen.AKP_FORM -> AKPScreen(
            dbHelper = db,
            empId = sessionManager.getFccode() ?: "",
            onBack = { navigateBack() },
            onNavigateToRKH = { navigateTo(Screen.RKH_VIEW) }
        )

        Screen.RKH_VIEW -> RKHScreen(
            dbHelper = db,
            empId = sessionManager.getFccode() ?: "",
            onBack = { navigateBack() }
        )
    }
}

// ─── DASHBOARD COMPONENTS ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(userName: String, userRole: String, onNavigate: (Screen) -> Unit, onLogout: () -> Unit) {
    val context = LocalContext.current
    val allMenus = listOf(
        MenuConfig("ABSENSI", Icons.Default.PersonSearch, listOf("ADMIN", "MANDOR", "KERANI"), true),
        MenuConfig("PROGRESS KERJA", Icons.Default.TrendingUp, listOf("ADMIN", "MANDOR"), true),
        MenuConfig("PERHITUNGAN BUAH", Icons.Default.Analytics, listOf("ADMIN", "MANDOR")),
        MenuConfig("PEMBUATAN SPB", Icons.Default.LocalShipping, listOf("ADMIN", "KERANI"), true),
        MenuConfig("ANCAK PANEN", Icons.Default.Agriculture, listOf("ADMIN", "MANDOR")),
        MenuConfig("AKP", Icons.Default.Assessment, listOf("ADMIN", "MANDOR")),
        MenuConfig("RKH", Icons.Default.Assignment, listOf("ADMIN", "MANDOR", "KERANI")),
        MenuConfig("MASTER DATA", Icons.Default.Storage, listOf("ADMIN"), color = Color(0xFFD32F2F))

    )
    val filteredMenus = allMenus.filter { it.roles.contains(userRole.uppercase()) }

    Scaffold(
        containerColor = Color(0xFFF5F7FA),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("ESTATE SYSTEM", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp) },
                actions = { IconButton(onClick = onLogout) { Icon(Icons.Default.Logout, "Logout", tint = Color.Red) } },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            Spacer(modifier = Modifier.height(20.dp))
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF1A3A8F)), shape = RoundedCornerShape(16.dp)) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Selamat Datang,", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                        Text(userName.uppercase(), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Surface(modifier = Modifier.padding(top = 4.dp), color = Color.White.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                            Text(userRole.uppercase(), modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Icon(Icons.Default.AccountCircle, null, tint = Color.White, modifier = Modifier.size(44.dp))
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text("MENU OPERASIONAL", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.DarkGray)
            Spacer(modifier = Modifier.height(16.dp))
            LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.weight(1f)) {
                items(filteredMenus.size) { index ->
                    val menu = filteredMenus[index]
                    DashboardCard(title = menu.title, icon = menu.icon, hasSub = menu.hasSubMenu, color = menu.color, onClick = {
                        when (menu.title) {
                            "ABSENSI" -> onNavigate(Screen.HOME)
                            "PROGRESS KERJA" -> onNavigate(Screen.PROGRESS_MENU)
                            "PERHITUNGAN BUAH" -> onNavigate(Screen.FRUIT_COUNTING)
                            "ANCAK PANEN" -> onNavigate(Screen.ANCAK_PANEN)
                            "PEMBUATAN SPB" -> onNavigate(Screen.SPB_MENU)
                            "AKP" -> onNavigate(Screen.AKP_FORM)
                            "RKH" -> onNavigate(Screen.RKH_VIEW)
                            "MASTER DATA" -> onNavigate(Screen.EMPLOYEE_FORM)
                            else -> Toast.makeText(context, "Modul ${menu.title} sedang dikembangkan", Toast.LENGTH_SHORT).show()
                        }
                    })
                }
            }
            Text(text = "Estate Management v1.0.45", modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), textAlign = TextAlign.Center, fontSize = 10.sp, color = Color.LightGray)
        }
    }
}

@Composable
fun DashboardCard(title: String, icon: ImageVector, hasSub: Boolean, color: Color, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.height(130.dp), shape = RoundedCornerShape(12.dp), colors = CardDefaults.elevatedCardColors(containerColor = Color.White)) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(icon, null, modifier = Modifier.size(32.dp), tint = color)
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF333333), textAlign = TextAlign.Center)
            }
            if (hasSub) {
                Surface(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp), color = color.copy(alpha = 0.1f), shape = CircleShape) {
                    Icon(Icons.Default.List, null, modifier = Modifier.size(14.dp).padding(2.dp), tint = color)
                }
            }
        }
    }
}




