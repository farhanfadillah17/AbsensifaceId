package com.example.attendanceapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.attendanceapp.ui.theme.AttendanceAppTheme

// 1. Enum Screen - Tetap ada HISTORY untuk diakses dari layar HOME
enum class Screen {
    LOGIN,
    DASHBOARD,
    HOME,
    EMPLOYEE_FORM,
    REGISTER_FACE,
    QR_SCAN,
    FACE_VERIFY,
    HISTORY
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
                    color = MaterialTheme.colorScheme.background
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
    val initialScreen = if (sessionManager.isLoggedIn()) Screen.DASHBOARD else Screen.LOGIN
    val backStack = remember { mutableStateListOf(initialScreen) }
    val currentScreen = backStack.last()

    var pendingEmployee by remember { mutableStateOf<Employee?>(null) }
    var currentAction by remember { mutableStateOf(AttendanceAction.CHECK_IN) }
    var verifiedEmployee by remember { mutableStateOf<Employee?>(null) }

    fun navigateTo(screen: Screen) {
        backStack.add(screen)
    }

    fun navigateBack() {
        if (backStack.size > 1) backStack.removeLast()
    }

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
                if (currentScreen == Screen.DASHBOARD || currentScreen == Screen.LOGIN) {
                    return
                }
                navigateBack()
            }
        }
        dispatcher?.addCallback(callback)
        onDispose { callback.remove() }
    }

    when (currentScreen) {
        Screen.LOGIN -> LoginScreen(
            dbHelper = db,
            sessionManager = sessionManager,
            onLoginSuccess = { navigateDashboard() }
        )

        Screen.DASHBOARD -> DashboardScreen(
            userName = sessionManager.getUserName() ?: "Karyawan",
            onStartAttendance = { navigateTo(Screen.HOME) },
            onLogout = { logout() }
        )

        Screen.HOME -> HomeScreen(
            sessionManager = sessionManager,
            dbHelper = db,
            onRegisterFace = { navigateTo(Screen.EMPLOYEE_FORM) },
            onCheckIn = {
                currentAction = AttendanceAction.CHECK_IN
                navigateTo(Screen.QR_SCAN)
            },
            onCheckOut = {
                currentAction = AttendanceAction.CHECK_OUT
                navigateTo(Screen.QR_SCAN)
            },
            onHistory = { navigateTo(Screen.HISTORY) },
            onLogout = { logout() }

        )

        Screen.EMPLOYEE_FORM -> EmployeeFormScreen(
            dbHelper = db,
            onBack = { navigateBack() },
            onNext = { emp ->
                pendingEmployee = emp
                navigateTo(Screen.REGISTER_FACE)
            }
        )

        Screen.REGISTER_FACE -> {
            pendingEmployee?.let { employee ->
                RegisterFaceScreen(
                    dbHelper = db,
                    employee = employee,
                    faceDataHelper = faceHelper,
                    onBack = { navigateBack() },
                    onSuccess = {
                        pendingEmployee = null
                        navigateDashboard()
                    }
                )
            }
        }

        Screen.QR_SCAN -> QRScannerScreen(
            action = currentAction,
            dbHelper = db,
            onBack = { navigateBack() },
            onQRVerified = { emp ->
                verifiedEmployee = emp
                navigateTo(Screen.FACE_VERIFY)
            }
        )

        Screen.FACE_VERIFY -> AttendanceCameraScreen(
            action = currentAction,
            verifiedEmployee = verifiedEmployee,
            faceDataHelper = faceHelper,
            dbHelper = db,
            onBack = { navigateBack() },
            onSuccess = {
                verifiedEmployee = null
                navigateDashboard()
            }
        )

        Screen.HISTORY -> AttendanceHistoryScreen(
            dbHelper = db,
            onBack = { navigateBack() }
        )
    }
}

// ─── UI COMPONENTS ────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    userName: String,
    onStartAttendance: () -> Unit,
    onLogout: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("MENU UTAMA", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp) },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout", tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Selamat Datang,", color = MaterialTheme.colorScheme.onPrimary, fontSize = 14.sp)
                    Text(
                        userName.uppercase(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(80.dp))

            // Menu Utama hanya menampilkan Absensi
            DashboardCard(
                title = "MASUK MENU ABSENSI",
                icon = Icons.Default.CameraFront,
                onClick = onStartAttendance,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Silakan tekan tombol di atas untuk memulai absensi",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun DashboardCard(title: String, icon: ImageVector, onClick: () -> Unit, color: androidx.compose.ui.graphics.Color) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .height(180.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = color.copy(alpha = 0.1f),
                modifier = Modifier.size(70.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.padding(15.dp).size(40.dp),
                    tint = color
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        }
    }
}
