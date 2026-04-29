package com.example.attendanceapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.example.attendanceapp.ui.theme.AttendanceAppTheme

class MainActivity : ComponentActivity() {

    private lateinit var dbHelper: AttendanceDatabaseHelper
    private lateinit var faceDataHelper: FaceDataHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        dbHelper = AttendanceDatabaseHelper(this)
        faceDataHelper = FaceDataHelper(this)

        setContent {
            AttendanceAppTheme {
                AppNavigation(dbHelper = dbHelper, faceDataHelper = faceDataHelper)
            }
        }
    }
}

enum class Screen {
    HOME,
    REGISTER_FACE,
    QR_SCAN,         // Langkah 1 — Scan QR
    FACE_VERIFY,     // Langkah 2 — Verifikasi wajah
    QR_GENERATOR,    // Halaman lihat/cetak QR karyawan
    HISTORY
}



@Composable
fun AppNavigation(
    dbHelper: AttendanceDatabaseHelper,
    faceDataHelper: FaceDataHelper
) {
    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    var currentAction by remember { mutableStateOf(AttendanceAction.CHECK_IN) }

    // Data yang dilewat antar screen (hasil QR scan)
    var verifiedEmployee by remember { mutableStateOf<QREmployee?>(null) }

    when (currentScreen) {
        Screen.HOME -> HomeScreen(
            faceDataHelper = faceDataHelper,
            onRegisterFace = { currentScreen = Screen.REGISTER_FACE },
            onCheckIn = {
                currentAction = AttendanceAction.CHECK_IN
                currentScreen = Screen.QR_SCAN
            },
            onCheckOut = {
                currentAction = AttendanceAction.CHECK_OUT
                currentScreen = Screen.QR_SCAN
            },
            onHistory = { currentScreen = Screen.HISTORY },
            onQRGenerator = { currentScreen = Screen.QR_GENERATOR }
        )

        Screen.QR_SCAN -> QRScannerScreen(
            action = currentAction,
            onBack = { currentScreen = Screen.HOME },
            onQRVerified = { employee ->
                verifiedEmployee = employee
                currentScreen = Screen.FACE_VERIFY
            }
        )

        Screen.FACE_VERIFY -> AttendanceCameraScreen(
            action = currentAction,
            verifiedEmployee = verifiedEmployee,
            faceDataHelper = faceDataHelper,
            dbHelper = dbHelper,
            onBack = { currentScreen = Screen.QR_SCAN },
            onSuccess = {
                verifiedEmployee = null
                currentScreen = Screen.HOME
            }
        )

        Screen.REGISTER_FACE -> RegisterFaceScreen(
            faceDataHelper = faceDataHelper,
            onBack = { currentScreen = Screen.HOME },
            onSuccess = { currentScreen = Screen.HOME }
        )

        Screen.QR_GENERATOR -> QRGeneratorScreen(
            faceDataHelper = faceDataHelper,
            onBack = { currentScreen = Screen.HOME }
        )

        Screen.HISTORY -> AttendanceHistoryScreen(
            dbHelper = dbHelper,
            onBack = { currentScreen = Screen.HOME }
        )
    }
}
