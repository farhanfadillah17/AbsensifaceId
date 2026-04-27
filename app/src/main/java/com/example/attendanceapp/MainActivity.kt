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
                AppNavigation(
                    dbHelper = dbHelper,
                    faceDataHelper = faceDataHelper
                )
            }
        }
    }
}

enum class Screen {
    HOME, REGISTER_FACE, ATTENDANCE_CAMERA, HISTORY
}

@Composable
fun AppNavigation(
    dbHelper: AttendanceDatabaseHelper,
    faceDataHelper: FaceDataHelper
) {
    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    var currentAction by remember { mutableStateOf(AttendanceAction.CHECK_IN) }

    when (currentScreen) {
        Screen.HOME -> HomeScreen(
            faceDataHelper = faceDataHelper,
            onRegisterFace = { currentScreen = Screen.REGISTER_FACE },
            onCheckIn = {
                currentAction = AttendanceAction.CHECK_IN
                currentScreen = Screen.ATTENDANCE_CAMERA
            },
            onCheckOut = {
                currentAction = AttendanceAction.CHECK_OUT
                currentScreen = Screen.ATTENDANCE_CAMERA
            },
            onHistory = { currentScreen = Screen.HISTORY }
        )
        Screen.REGISTER_FACE -> RegisterFaceScreen(
            faceDataHelper = faceDataHelper,
            onBack = { currentScreen = Screen.HOME },
            onSuccess = { currentScreen = Screen.HOME }
        )
        Screen.ATTENDANCE_CAMERA -> AttendanceCameraScreen(
            action = currentAction,
            faceDataHelper = faceDataHelper,
            dbHelper = dbHelper,
            onBack = { currentScreen = Screen.HOME },
            onSuccess = { currentScreen = Screen.HOME }
        )
        Screen.HISTORY -> AttendanceHistoryScreen(
            dbHelper = dbHelper,
            onBack = { currentScreen = Screen.HOME }
        )
    }
}
