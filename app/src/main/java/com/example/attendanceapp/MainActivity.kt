package com.example.attendanceapp

import android.app.PendingIntent
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
import androidx.compose.runtime.produceState
import android.content.Context
import android.nfc.NfcAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.util.Log
import kotlinx.coroutines.withContext
import android.nfc.NdefMessage
import android.nfc.NdefRecord

import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.content.Intent



// 1. Data Class untuk Menu
data class MenuConfig(
    val title: String,
    val icon: ImageVector,
    val roles: List<String>,
    val hasSubMenu: Boolean = false,
    val color: Color = Color(0xFF1A3A8F),
    val route: String,
)

// 2. Enum Screen
enum class Screen {
    LOGIN, DASHBOARD, HOME, EMPLOYEE_FORM, REGISTER_FACE, QR_SCAN, FACE_VERIFY, HISTORY, PROGRESS_MENU, FRUIT_COUNTING, ANCAK_PANEN, SPB_MENU, SPB_FORM, AKP_FORM, RKH_VIEW,RKH_FORM, TRANSFER_DATA, SYNC, MASTER_DATA, EMPLOYEE_MENU, NFC_REGISTER, NFC_ATTENDANCE
}

class MainActivity : ComponentActivity() {
    private lateinit var db: AttendanceDatabaseHelper
    private lateinit var faceHelper: FaceDataHelper
    private lateinit var sessionManager: SessionManager
    private lateinit var apiClient: ApiClient

    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null

    // Simpan data yang ingin ditulis di sini
    var dataToWrite: String? = null
    var onNfcWriteSuccess: (() -> Unit)? = null // Callback untuk reset UI setelah sukses tulis
    var onNfcRead: ((String) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        db = AttendanceDatabaseHelper(this)
        faceHelper = FaceDataHelper(db)
        sessionManager = SessionManager(this)
        apiClient = ApiClient()

        // --- TAMBAHKAN KODE IMPORT DI SINI ---
        val sharedPref = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val isFirstRun = sharedPref.getBoolean("isFirstRun", true)
//
//        if (true) {
//            Log.d("TES IMPORT", "onCreate: MAIN MENU IMPORT")
//            lifecycleScope.launch(Dispatchers.IO) {
//                val files = listOf(
//                    "BUSINESSUNIT_202606011217.sql",
//                    "FIELD_202606011224.sql",
//                    "JOB_202606011226.sql",
//                    "TPH_202606011225.sql",
//                    "EMPLOYEE_202605200755.sql"
//                )
//
//                // Tambahkan perulangan forEach di sini:
//                files.forEach { fileName ->
//                    // Pastikan importSqlFromAssets menangani pembersihan kata TIMESTAMP
//                    db.importSqlFromAssets(fileName) { count ->
//                        Log.d("SETUP", "Berhasil mengimpor $fileName: $count data SRE.")
//                    }
//                }
//
//                // Tandai sudah pernah di-import agar tidak berjalan lagi
//                sharedPref.edit().putBoolean("isFirstRun", false).apply()
//                Log.d("SETUP", "Semua master data berhasil dimuat.")
//            }
//
//
//        }
        // -------------------------------------

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE
        )

        setContent {
            AttendanceAppTheme {

                val scope = rememberCoroutineScope()

                // 2. Jalankan pengecekan dan import data master
                LaunchedEffect(Unit) {
                    scope.launch(Dispatchers.IO) {

                        if (db.isTableEmpty("NURSERY")) {
                            Log.d("DB_INIT", "Mengimport data Nursery...")
                            db.importSqlFromAssets("nursery.sql")
                        }
                        // Cek Tabel Mill (CUSTOMER)
                        if (db.isTableEmpty("CUSTOMER")) {
                            db.importSqlFromAssets("mill.sql")
                        }
                        // Cek Tabel Vehicle (VEHICLE)
                        if (db.isTableEmpty("VEHICLE")) {
                            db.importSqlFromAssets("vehicle.sql")
                        }
                    }
                }


                Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
                    AppNavigation(
                        db = db,
                        faceHelper = faceHelper,
                        sessionManager = sessionManager,
                        apiClient = apiClient
                    )
                }
            }
        }
    }


    // 1. Aktifkan deteksi NFC saat aplikasi dibuka
    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, null, null)
    }

    // 2. Matikan deteksi NFC saat aplikasi di background
    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    // 3. Tangkap kartu NFC saat ditempelkan
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        // Pastikan Intent yang datang adalah Intent NFC
        val action = intent.action
        if (NfcAdapter.ACTION_TAG_DISCOVERED == action ||
            NfcAdapter.ACTION_TECH_DISCOVERED == action ||
            NfcAdapter.ACTION_NDEF_DISCOVERED == action
        ) {

            val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            tag?.let {
                if (dataToWrite != null) {
                    // --- MODE MENULIS (Untuk Fruit Counting) ---
                    val success = writeNdefMessage(it, dataToWrite!!)
                    if (success) {
                        Toast.makeText(this, "Data Berhasil Disimpan ke Kartu!", Toast.LENGTH_LONG)
                            .show()
                        dataToWrite = null // Reset agar tidak menulis ulang secara tidak sengaja
                        onNfcWriteSuccess?.invoke() // Reset UI di Screen
                        onNfcWriteSuccess = null
                    } else {
                        Toast.makeText(
                            this,
                            "Gagal Menulis! Pastikan kartu tidak terkunci.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else if (onNfcRead != null) {
                    // --- MODE MEMBACA (Untuk SPB Form) ---
                    val data = readNdefMessage(it)
                    if (data != null) {
                        onNfcRead?.invoke(data) // Kirim data ke SPBFormScreen
                    } else {
                        Toast.makeText(this, "Kartu Kosong atau Format Salah", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
        }
    }

    private fun readNdefMessage(tag: Tag): String? {
        val ndef = Ndef.get(tag) ?: return null
        return try {
            ndef.connect()
            val msg = ndef.ndefMessage ?: return null
            if (msg.records.isEmpty()) return null

            val record = msg.records[0]
            val payload = record.payload

            // Standar NDEF Text Record: byte pertama berisi panjang kode bahasa
            val languageCodeLength = payload[0].toInt() and 63

            // Ambil string setelah prefix bahasa
            String(
                payload,
                languageCodeLength + 1,
                payload.size - languageCodeLength - 1,
                Charsets.UTF_8
            )
        } catch (e: Exception) {
            Log.e("NFC_READ", "Error: ${e.message}")
            null
        } finally {
            try {
                ndef.close()
            } catch (e: Exception) {
            }
        }
    }


    private fun writeNdefMessage(tag: Tag, data: String): Boolean {
        val record = NdefRecord.createTextRecord("en", data)
        val message = NdefMessage(arrayOf(record))

        return try {
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                ndef.connect()
                ndef.writeNdefMessage(message)
                ndef.close()
                true
            } else {
                val format = NdefFormatable.get(tag)
                if (format != null) {
                    format.connect()
                    format.format(message)
                    format.close()
                    true
                } else false
            }
        } catch (e: Exception) {
            Log.e("NFC_WRITE", "Error: ${e.message}")
            false
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class) // Tambahkan ini
@Composable
fun AppNavigation(
    db: AttendanceDatabaseHelper,
    faceHelper: FaceDataHelper,
    sessionManager: SessionManager,
    apiClient: ApiClient,
) {
    val context = LocalContext.current
    val initialScreen = if (sessionManager.isLoggedIn()) Screen.DASHBOARD else Screen.LOGIN
    val backStack = remember { mutableStateListOf(initialScreen) }
    val currentScreen = backStack.last()

    var pendingEmployee by remember { mutableStateOf<Employee?>(null) }
    var employeeWorkFlow by remember { mutableStateOf("FACE") } // "FACE" or "NFC"
    var currentAction by remember { mutableStateOf(AttendanceAction.CHECK_IN) }
    var verifiedEmployee by remember { mutableStateOf<Employee?>(null) }
    var selectedCategory by remember { mutableStateOf("") }
    var selectedSpbCategory by remember { mutableStateOf("") }
    var nfcAction by remember { mutableStateOf("READ") } // "READ" or "WRITE"



    fun navigateTo(screen: Screen) {
        backStack.add(screen)
    }

    fun navigateBack() {
        if (backStack.size > 1) backStack.removeAt(backStack.size - 1)
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
                if (currentScreen == Screen.DASHBOARD || currentScreen == Screen.LOGIN) return
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
            apiClient = apiClient,
            onLoginSuccess = { navigateDashboard() })

        Screen.DASHBOARD -> DashboardScreen(
            userName = sessionManager.getUserName() ?: "User",
            userRole = sessionManager.getUserRole() ?: "MANDOR",
            empId = sessionManager.getFccode() ?: "",
            dbHelper = db,
            apiClient = apiClient,
            onNavigate = { screen -> navigateTo(screen) },
            onLogout = { logout() })

        Screen.HOME -> HomeScreen(
            sessionManager = sessionManager,
            dbHelper = db,
            onRegisterFace = { navigateTo(Screen.EMPLOYEE_FORM) },
            onCheckIn = { currentAction = AttendanceAction.CHECK_IN; navigateTo(Screen.NFC_ATTENDANCE) },
            onCheckOut = { currentAction = AttendanceAction.CHECK_OUT; navigateTo(Screen.NFC_ATTENDANCE) },
            onHistory = { navigateTo(Screen.HISTORY) },
            onFeature2 = { navigateTo(Screen.TRANSFER_DATA) },
            onFeature3 = {
                currentAction = AttendanceAction.RECEIVE
                navigateTo(Screen.QR_SCAN)
            })

        Screen.EMPLOYEE_FORM -> EmployeeFormScreen(
            dbHelper = db,
            onBack = { navigateBack() },
            onNext = { emp -> 
                pendingEmployee = emp
                if (employeeWorkFlow == "NFC") {
                    navigateTo(Screen.NFC_REGISTER)
                } else {
                    navigateTo(Screen.REGISTER_FACE)
                }
            })

        Screen.REGISTER_FACE -> {
            pendingEmployee?.let { emp ->
                RegisterFaceScreen(
                    dbHelper = db,
                    employee = emp,
                    faceDataHelper = faceHelper,
                    onBack = { navigateBack() },
                    onSuccess = {
                        pendingEmployee = null
                        while (backStack.lastOrNull() != Screen.HOME && backStack.size > 1) {
                            backStack.removeAt(backStack.size - 1)
                        }
                    })
            } ?: LaunchedEffect(Unit) { navigateBack() }
        }

        Screen.QR_SCAN -> QRScannerScreen(
            action = currentAction,
            dbHelper = db,
            onBack = { navigateBack() },
            onQRVerified = { result ->
                if (currentAction == AttendanceAction.RECEIVE) {
                    val count = db.receiveTransferredData(result.fccode)
                    if (count > 0) Toast.makeText(
                        context, "Berhasil terima $count data", Toast.LENGTH_SHORT
                    ).show()
                    else Toast.makeText(context, "Gagal: Data tidak valid", Toast.LENGTH_SHORT)
                        .show()
                    navigateBack()
                } else {
                    verifiedEmployee = result
                    navigateTo(Screen.FACE_VERIFY)
                }
            })

        Screen.NFC_ATTENDANCE -> AttendanceNFCScreen(
            action = currentAction,
            dbHelper = db,
            onBack = { navigateBack() },
            onNfcVerified = { result ->
                if (currentAction == AttendanceAction.RECEIVE) {
                    val count = db.receiveTransferredData(result.fccode)
                    if (count > 0) Toast.makeText(
                        context, "Berhasil terima $count data", Toast.LENGTH_SHORT
                    ).show()
                    else Toast.makeText(context, "Gagal: Data tidak valid", Toast.LENGTH_SHORT)
                        .show()
                    navigateBack()
                } else {
                    verifiedEmployee = result
                    navigateTo(Screen.FACE_VERIFY)
                }
            })

        Screen.FACE_VERIFY -> {
            verifiedEmployee?.let { emp ->
                AttendanceCameraScreen(
                    action = currentAction,
                    verifiedEmployee = emp,
                    faceDataHelper = faceHelper,
                    dbHelper = db,
                    onBack = { navigateBack() },
                    onSuccess = {
                        verifiedEmployee = null
                        while (backStack.lastOrNull() != Screen.HOME && backStack.size > 1) {
                            backStack.removeAt(backStack.size - 1)
                        }
                    })
            } ?: LaunchedEffect(Unit) { navigateBack() }
        }

        Screen.TRANSFER_DATA -> AttendanceTransferScreen(
            dbHelper = db,
            userRole = sessionManager.getUserRole() ?: "GUEST",
            onBack = { navigateBack() }
        )

        Screen.HISTORY -> AttendanceHistoryScreen(dbHelper = db, onBack = { navigateBack() })

        Screen.PROGRESS_MENU -> ProgressMenuScreen(
            dbHelper = db,
            empId = sessionManager.getFccode() ?: "",
            fcba = sessionManager.getFcba() ?: "",
            onBack = { navigateBack() },
            onSaveSuccess = { navigateDashboard() }
        )






        Screen.FRUIT_COUNTING -> FruitCountingScreen(
            dbHelper = db,
            empId = sessionManager.getFccode() ?: "",
            fcba = sessionManager.getFcba() ?: "",
            onBack = { navigateBack() },
            onSuccess = { navigateDashboard() })

        Screen.ANCAK_PANEN -> AncakPanenScreen(
            dbHelper = db,
            empId = sessionManager.getFccode() ?: "",
            onBack = { navigateBack() },
            onSuccess = { navigateDashboard() })

        // Menangani navigasi Menu SPB (Daftar) dan Form SPB (Input) secara terpisah
        Screen.SPB_MENU -> SPBMainScreen(
            dbHelper = db,
            empId = sessionManager.getFccode() ?: "",
            fcba = sessionManager.getFcba() ?: "",
            onBackMenu = { navigateBack() }
        )

        Screen.SPB_FORM -> SPBFormScreen(
            dbHelper = db,
            empId = sessionManager.getFccode() ?: "",
            fcba = sessionManager.getFcba() ?: "",
            onBack = { navigateBack() },
            onSuccess = {
                // Setelah sukses simpan, kembali ke list data
                navigateBack()
            }
        )

        Screen.AKP_FORM -> AKPScreen(
            dbHelper = db,
            empId = sessionManager.getFccode() ?: "",
            onBack = { navigateBack() },
            onNavigateToRKH = { navigateTo(Screen.RKH_VIEW) })

        Screen.RKH_VIEW -> RKHMainScreen(
            dbHelper = db,
            empId = sessionManager.getFccode() ?: "",
            fcba = sessionManager.getFcba() ?: "",
            onBack = { navigateBack() },
            onAddClick = {
                // Navigasi ke Form untuk tambah data baru
                navigateTo(Screen.RKH_FORM)
            },
            onEditClick = { rkhId ->
                // Navigasi ke Form untuk edit data (opsional)
                // Sementara bisa dikosongkan atau arahkan ke rute edit jika sudah ada
            }
        )

        Screen.RKH_FORM -> RKHFormScreen(
            dbHelper = db,
            empId = sessionManager.getFccode() ?: "",
            fcba = sessionManager.getFcba() ?: "",
            onBack = { navigateBack() },
            onSuccess = {
                // Setelah sukses simpan RKH, kembali ke list data RKH (bukan ke dashboard)
                navigateBack()
            }
        )

        Screen.SYNC -> SyncScreen(
            sessionManager = sessionManager,
            dbHelper = db,
            apiClient = apiClient,
            onBack = { navigateBack() },
            context = context,
        )

        Screen.MASTER_DATA -> MasterDataMainScreen(
            sessionManager = sessionManager,
            onEmployeeClick = { navigateTo(Screen.EMPLOYEE_MENU) },
            onFieldClick = { 
                // Sementara tampilkan toast atau arahkan ke layar dummy jika belum ada form Field
                Toast.makeText(context, "Menu Field segera hadir", Toast.LENGTH_SHORT).show()
            },
            onBack = { navigateBack() }
        )

        Screen.EMPLOYEE_MENU -> EmployeeMenuScreen(
            sessionManager = sessionManager,
            onFaceRegisterClick = { 
                employeeWorkFlow = "FACE"
                navigateTo(Screen.EMPLOYEE_FORM) 
            },
            onNfcRegisterClick = { 
                employeeWorkFlow = "NFC"
                navigateTo(Screen.EMPLOYEE_FORM) 
            },
            onBack = { navigateBack() }
        )

        Screen.NFC_REGISTER -> {
            pendingEmployee?.let { emp ->
                NfcRegisterScreen(
                    dbHelper = db,
                    employee = emp,
                    onBack = { navigateBack() },
                    onStartWriting = { jsonString, onComplete ->
                        val mainActivity = (context as? MainActivity)
                        mainActivity?.dataToWrite = jsonString
                        mainActivity?.onNfcWriteSuccess = onComplete
                        
                        if (jsonString.isNotEmpty()) {
                            Toast.makeText(context, "Siap menulis, tempelkan kartu!", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            } ?: LaunchedEffect(Unit) { navigateBack() }
        }
    }
}

@Composable
fun AKPScreen(dbHelper: AttendanceDatabaseHelper, empId: String, onBack: () -> Unit, onNavigateToRKH: () -> Unit) {

}

@Composable
fun AncakPanenScreen(dbHelper: AttendanceDatabaseHelper, empId: String, onBack: () -> Unit, onSuccess: () -> Unit) {

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    userName: String,
    userRole: String,
    empId: String,
    dbHelper: AttendanceDatabaseHelper,
    apiClient: ApiClient,
    onNavigate: (Screen) -> Unit,
    onLogout: () -> Unit
) {

    val scope = rememberCoroutineScope()
    var isImporting by remember { mutableStateOf(false) }
    var importStatus by remember { mutableStateOf("") }


    val context = LocalContext.current
    val sharedPref = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
    val fcba = sharedPref.getString("fcba", "") ?: ""

    LaunchedEffect(Unit) {

        isImporting = true
//        importStatus = "Menyiapkan data karyawan..."

        try {

            // =========================
            // EMPLOYEE
            // =========================

            var existingData = withContext(Dispatchers.IO) {
                dbHelper.getAllMasterEmployees().size
            }

            if (existingData == 0) {

                importStatus = "Menyiapkan data karyawan..."

                withContext(Dispatchers.IO) {

                    val response = apiClient.getEmployee(fcba)
//                    Log.d("test import", "DashboardScreen: $response")

                    dbHelper.insertEmployee(response) { count ->

                        scope.launch(Dispatchers.Main) {
                            importStatus = "Mengimpor Employee $count%"
                        }

                    }

                }

                importStatus = "Data karyawan siap!"
            }

            // =========================
            // JOB
            // =========================

            existingData = withContext(Dispatchers.IO) {
                dbHelper.getJobList(fcba).size
            }

            if (existingData == 0) {

                importStatus = "Menyiapkan data job..."

                withContext(Dispatchers.IO) {

                    val response = apiClient.getJob(fcba)
//                    Log.d("test import", "DashboardScreen: $response")

                    dbHelper.insertJob(response) { count ->

                        scope.launch(Dispatchers.Main) {
                            importStatus = "Mengimpor Job $count%"
                        }

                    }

                }

                importStatus = "Data job siap!"
            }

            // =========================
            // FIELD
            // =========================

            existingData = withContext(Dispatchers.IO) {
                dbHelper.getBlockList(fcba).size
            }

            if (existingData == 0) {

                importStatus = "Menyiapkan data field..."

                withContext(Dispatchers.IO) {

                    val response = apiClient.getField(fcba)
//                    Log.d("test import", "DashboardScreen: $response")

                    dbHelper.insertField(response) { count ->

                        scope.launch(Dispatchers.Main) {
                            importStatus = "Mengimpor field $count%"
                        }

                    }

                }

                importStatus = "Data field siap!"
            }

            // =========================
            // TPH
            // =========================

            existingData = withContext(Dispatchers.IO) {
                dbHelper.getAllTph(fcba).size
            }

            if (existingData == 0) {

                importStatus = "Menyiapkan data tph..."

                withContext(Dispatchers.IO) {

                    val response = apiClient.getTph(fcba)
//                    Log.d("test import", "DashboardScreen: $response")

                    dbHelper.insertTph(response) { count ->

                        scope.launch(Dispatchers.Main) {
                            importStatus = "Mengimpor tph $count%"
                        }

                    }

                }

                importStatus = "Data tph siap!"
            }


            // =========================
// ACCESS CONTROL (HAK AKSES)
// =========================
            existingData = withContext(Dispatchers.IO) {
                dbHelper.getAllowedMenusForUser(empId).size
            }

            if (existingData == 0) {
                importStatus = "Menyiapkan hak akses menu..."
                withContext(Dispatchers.IO) {
                    try {
                        val response = apiClient.getAccess() // Panggil API Access
                        dbHelper.insertAccess(response)
                    } catch (e: Exception) {
                        Log.e("IMPORT_ACCESS", "Gagal download access: ${e.message}")
                    }
                }
                importStatus = "Hak akses siap!"
            }

            // =========================
            // FCBA
            // =========================

            existingData = withContext(Dispatchers.IO) {
                dbHelper.getAllFcba().size
            }

            if (existingData == 0) {

                importStatus = "Menyiapkan data fcba..."

                withContext(Dispatchers.IO) {

                    val response = apiClient.getFcba()
//                    Log.d("test import", "DashboardScreen: $response")

                    dbHelper.insertFcba(response) { count ->

                        scope.launch(Dispatchers.Main) {
                            importStatus = "Mengimpor fcba $count%"
                        }

                    }

                }

                importStatus = "Data fcba siap!"
            }

            importStatus = "Sinkronisasi selesai"

        } catch (e: Exception) {

            Log.e(
                "IMPORT_DATA",
                "Error: ${e.message}",
                e
            )

            importStatus =
                "Gagal mengunduh data: ${e.localizedMessage}"

        } finally {

            isImporting = false

        }
    }



    // Di dalam DashboardScreen
    // Perbaiki baris ini
    val allowedMenuRoutes by produceState<List<String>>(initialValue = emptyList(), key1 = empId) { // Ganti empcode ke empId
        // Dan baris ini juga
        value = dbHelper.getAllowedMenusForUser(empId) // Ganti empcode ke empId
    }

    val allMenus = listOf(
        // Sesuaikan angka route dengan urutan menu di database/API Anda
        MenuConfig("ABSENSI", Icons.Default.PersonSearch, listOf(), true, route = "1"),
        MenuConfig("PROGRESS KERJA", Icons.Default.TrendingUp, listOf(), true, route = "2"),
        MenuConfig("PERHITUNGAN BUAH", Icons.Default.Analytics, listOf(), route = "3"),
        MenuConfig("PEMBUATAN SPB", Icons.Default.LocalShipping, listOf(), true, route = "4"),
        MenuConfig("ANCAK PANEN", Icons.Default.Agriculture, listOf(), route = "5"),
        MenuConfig("AKP", Icons.Default.Assessment, listOf(), route = "6"),
        MenuConfig("MASTER DATA", Icons.Default.Storage, listOf(), color = Color(0xFF1A3A8F), route = "7"),
        MenuConfig("RENCANA KERJA", Icons.Default.EditNote, listOf(), route = "8"),
        MenuConfig("SINKRON DATA", Icons.Default.Sync, listOf(), route = "9")
    )

    val filteredMenus = allMenus.filter { menu -> allowedMenuRoutes.contains(menu.route) }

//    if (isImporting || importStatus.contains("siap")) {


    Scaffold(
        containerColor = Color(0xFFF5F7FA),
        topBar = {
            TopAppBar(
                title = { Text("Dashboard", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onLogout) { Icon(Icons.Default.Logout, "Logout") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Selamat Datang,", fontSize = 14.sp, color = Color.Gray)
            Text(userName.uppercase(), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A3A8F))
            Text("Role: $userRole", fontSize = 12.sp, color = Color.Gray)

            val context = LocalContext.current
            val sharedPref = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            val isFirstRun = sharedPref.getBoolean("isFirstRun", true)
            // loading api jika belum import
            if (isFirstRun) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isImporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color.Cyan
                            )
                            Spacer(Modifier.width(12.dp))
                        }
                        Text(
                            text = importStatus,
                            color = if (isImporting) Color.Gray else Color.Green,
                            fontSize = 12.sp
                        )
                    }
                }
            }else{
                Spacer(modifier = Modifier.height(24.dp))
            }
            Text("MENU OPERASIONAL", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.DarkGray)
            Spacer(modifier = Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredMenus.size) { index ->
                    val menu = filteredMenus[index]
                    DashboardCard(
                        title = menu.title,
                        icon = menu.icon,
                        hasSub = menu.hasSubMenu,
                        color = menu.color,
                        onClick = {
                            when (menu.route) {
                                "1" -> onNavigate(Screen.HOME)
                                "2" -> onNavigate(Screen.PROGRESS_MENU)
                                "3" -> onNavigate(Screen.FRUIT_COUNTING)
                                "4" -> onNavigate(Screen.SPB_MENU)
                                "5" -> onNavigate(Screen.ANCAK_PANEN)
                                "6" -> onNavigate(Screen.AKP_FORM)
                                "7" -> onNavigate(Screen.MASTER_DATA)
                                "8" -> onNavigate(Screen.RKH_VIEW)
                                "9" -> onNavigate(Screen.SYNC)
                            }
                        }
                    )
                }
            }
            Text(
                text = "Versi 1.0.2",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                textAlign = TextAlign.Center,
                fontSize = 10.sp,
                color = Color.LightGray
            )
        }
    }
}



@Composable
fun DashboardCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, hasSub: Boolean, color: Color, onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.height(130.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier
                .fillMaxSize()
                .padding(16.dp), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(icon, null, modifier = Modifier.size(32.dp), tint = color)
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
            if (hasSub) {
                Surface(modifier = Modifier
                    .align(androidx.compose.ui.Alignment.TopEnd)
                    .padding(8.dp), color = color.copy(alpha = 0.1f), shape = CircleShape) {
                    Icon(Icons.Default.List, null, modifier = Modifier
                        .size(14.dp)
                        .padding(2.dp), tint = color)
                }
            }
        }
    }
}