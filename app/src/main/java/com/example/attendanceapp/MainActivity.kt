package com.example.attendanceapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
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
import androidx.compose.foundation.shape.CircleShape

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
                    color = Color.White // Menggunakan latar putih agar selaras
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
            onLogout = { logout() },
            // Menambahkan parameter fitur baru agar tidak error
            onFeature2 = { Toast.makeText(context, "Fitur 2 Segera Hadir", Toast.LENGTH_SHORT).show() },
            onFeature3 = { Toast.makeText(context, "Fitur 3 Segera Hadir", Toast.LENGTH_SHORT).show() }
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
    onLogout: () -> Unit,
) {
    Scaffold(
        containerColor = Color.White, // Background Putih
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("DASHBOARD", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp) },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout", tint = Color.Red)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
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
            // Card Selamat Datang
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A3A8F)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Selamat Datang,", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                    Text(
                        userName.uppercase(),
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "PILIH MENU",
                modifier = Modifier.fillMaxWidth(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Baris Menu: Absensi dan Fitur 2 (Berdampingan)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DashboardCard(
                    title = "ABSENSI",
                    icon = Icons.Default.CameraFront,
                    onClick = onStartAttendance,
                    color = Color(0xFF1A3A8F),
                    modifier = Modifier.weight(1f)
                )

                DashboardCard(
                    title = "FITUR 2",
                    icon = Icons.Default.Extension,
                    onClick = { /* Aksi Fitur 2 */ },
                    color = Color.Gray,
                    isUpcoming = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Baris Menu: Fitur 3
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DashboardCard(
                    title = "FITUR 3",
                    icon = Icons.Default.AddCircleOutline,
                    onClick = { /* Aksi Fitur 3 */ },
                    color = Color.Gray,
                    isUpcoming = true,
                    modifier = Modifier.weight(1f)
                )

                // Spacer agar Fitur 3 tetap di kiri dan ukurannya konsisten
                Spacer(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Versi Aplikasi 1.0.0",
                fontSize = 12.sp,
                color = Color.LightGray
            )
        }
    }
}

@Composable
fun DashboardCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    color: Color,
    isUpcoming: Boolean = false,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.height(140.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = color.copy(alpha = 0.1f),
                modifier = Modifier.size(50.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(12.dp)
                        .size(24.dp),
                    tint = color
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                color = if (isUpcoming) Color.Gray else Color(0xFF1A3A8F),
                textAlign = TextAlign.Center
            )
            if (isUpcoming) {
                Text(
                    text = "Segera Hadir",
                    fontSize = 10.sp,
                    color = Color.LightGray
                )
            }
        }
    }
}