package com.example.attendanceapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.attendanceapp.ui.theme.AttendanceAppTheme

class MainActivity : ComponentActivity() {

    private lateinit var db: AttendanceDatabaseHelper
    private lateinit var faceHelper: FaceDataHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Inisialisasi Database dan Helper
        db = AttendanceDatabaseHelper(this)
        faceHelper = FaceDataHelper(db)

        setContent {
            AttendanceAppTheme {
                AppNavigation(db = db, faceHelper = faceHelper)
            }
        }
    }
}

enum class Screen {
    HOME,
    EMPLOYEE_FORM,      // Isi data karyawan (Nama, ID, dll)
    REGISTER_FACE,      // Scan wajah (Simpan ke DB)
    QR_SCAN,            // Langkah 1 Absensi: Scan QR
    FACE_VERIFY,        // Langkah 2 Absensi: Verifikasi Wajah
    QR_GENERATOR,       // Menu untuk melihat/generate QR Code
    HISTORY             // Riwayat Absensi
}

@Composable
fun AppNavigation(db: AttendanceDatabaseHelper, faceHelper: FaceDataHelper) {
    // Back-stack sederhana menggunakan StateList
    val backStack = remember { mutableStateListOf(Screen.HOME) }
    val currentScreen = backStack.last()

    // State untuk menyimpan data sementara antar layar
    var pendingEmployee by remember { mutableStateOf<Employee?>(null) }
    var currentAction by remember { mutableStateOf(AttendanceAction.CHECK_IN) }
    var verifiedEmployee by remember { mutableStateOf<Employee?>(null) }

    // Fungsi Navigasi
    fun navigateTo(screen: Screen) { backStack.add(screen) }
    fun navigateBack() { if (backStack.size > 1) backStack.removeLast() }
    fun navigateHome() {
        backStack.clear()
        backStack.add(Screen.HOME)
    }

    // Menangani tombol back sistem Android
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    DisposableEffect(dispatcher) {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (backStack.size > 1) navigateBack() else dispatcher?.onBackPressed()
            }
        }
        dispatcher?.addCallback(callback)
        onDispose { callback.remove() }
    }

    // Routing Layar
    when (currentScreen) {
        Screen.HOME -> HomeScreen(
            dbHelper = db,
            faceDataHelper = faceHelper,
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
            onQRGenerator = { navigateTo(Screen.QR_GENERATOR) }
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
            // Pastikan pendingEmployee tidak null sebelum membuka layar pendaftaran
            pendingEmployee?.let { employee ->
                RegisterFaceScreen(
                    dbHelper = db, // Pastikan di RegisterFaceScreen.kt juga menerima dbHelper jika dibutuhkan
                    employee = employee,
                    faceDataHelper = faceHelper,
                    onBack = { navigateBack() },
                    onSuccess = {
                        pendingEmployee = null
                        navigateHome()
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
                navigateHome()
            }
        )

        Screen.QR_GENERATOR -> QRGeneratorScreen(
            dbHelper = db,
            onBack = { navigateBack() }
        )

        Screen.HISTORY -> AttendanceHistoryScreen(
            dbHelper = db,
            onBack = { navigateBack() }
        )
    }
}