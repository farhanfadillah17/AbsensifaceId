package com.example.attendanceapp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import androidx.compose.foundation.layout.size
import androidx.compose.ui.geometry.isEmpty
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

// --- DATA CLASSES ---
data class Employee(
    val fccode: String, val fcba: String, val name: String,
    val sectionName: String, val gangCode: String, val position: String,
    val gender: String = "", val address: String = "", val companyNik: String = "",
    val faceEmbedding: String? = null,
    val lastUpdate: String = System.currentTimeMillis().toString(), // Tambahkan .toString()
)


data class FaceMapping(
    val id: Int = 0, val employeeId: String, val fcbaId: String,
    val features: FloatArray, val capturedAt: Long = System.currentTimeMillis(),
)

data class AttendanceRecord(
    val id: Int, val employeeId: String, val fcbaId: String,
    val employeeName: String, val action: String, val timestamp: String,
)

data class UserProfile(
    val username: String, val empcode: String, val fcba: String,
    val divisi: String, val gang: String, val role: String,
)

class AttendanceDatabaseHelper(private val context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        // Ganti nama ke v11 untuk reset total terakhir kali
        const val DATABASE_NAME = "attendance_reset_final_v286.db"
        const val DATABASE_VERSION = 286

        const val T_EMP = "EMPLOYEE"
        const val E_FCCODE = "FCCODE"
        const val E_FCBA = "FCBA"
        const val E_NAME = "FCNAME"
        const val E_SECTION = "SECTIONNAME"
        const val E_GANG = "GANGCODE"
        const val E_POSITION = "POSITION"
        const val E_NIK = "IDENTITYCARD"
        const val E_FACE_EMB = "FACEEMBEDDING"
        const val E_LAST_UPDATE = "LASTUPDATE"

        const val T_USERS = "users"
        const val U_USERNAME = "username"
        const val U_PASSWORD = "password"
        const val U_EMPCODE = "empcode"
        const val U_FCBA = "fcba"
        const val U_DIVISI = "divisi"
        const val U_GANG = "gang"
        const val U_ROLE = "role"

        const val T_ATT = "attendance"
        const val A_ID = "id"
        const val A_EMP_ID = "employee_id"
        const val A_FCBA = "fcba_id"
        const val A_EMP_NAME = "employee_name"
        const val A_ACTION = "action"
        const val A_TIMESTAMP = "timestamp"
        const val A_SOURCE = "source"

        const val T_FACE = "face_mappings"
        const val F_ID = "id"
        const val F_EMP_ID = "employee_id"
        const val F_FCBA = "fcba_id"
        const val F_FEATURES = "features"
        const val F_CAPTURED = "captured_at"


            // ... konstanta lainnya ...

            const val T_PROGRESS = "work_progress" // Tabel untuk progres umum
            const val P_ID = "id"
            const val P_RKH = "no_rkh"
            const val P_EMP_ID = "emp_id"
            const val P_CATEGORY = "category"
            const val P_BLOCK = "block_code"
            const val P_RESULT = "work_result"
            const val P_EMP_IDS = "emp_ids"
            const val P_UNIT = "unit"
            const val P_OUTPUT = "output"
            const val P_RATE = "rate"
            const val P_LEMBUR = "lembur"
            const val P_BERAS = "beras"
            const val P_NOTES = "notes"
            const val P_STATUS = "status"
            const val P_TIMESTAMP = "timestamp"

            // ...



        // Di dalam companion object
        const val T_MAINTENANCE = "maintenance_nursery"
        const val M_ID = "id"
        const val M_FCBA = "fcba"
        const val M_RKH = "no_rkh"
        const val M_GANG = "gang_code"
        const val M_S1 = "supervisi1"
        const val M_S2 = "supervisi2"
        const val M_S3 = "supervisi3"
        const val M_S4 = "supervisi4"
        const val M_WORKERS = "karyawan_ids" // Simpan CSV: "EMP001,EMP002"
        const val M_LOCATION = "location_code"
        const val M_UNIT = "unit"
        const val M_OUTPUT = "output"
        const val M_RATE = "rate"
        const val M_BERAS = "is_beras"
        const val M_LEMBUR = "lembur"
        const val M_CREATED = "created_at"

        const val T_MENU = "T_MENU"
        const val T_MENU_ACCESS = "T_MENU_ACCESS"

        const val T_RKH = "table_rkh"
        const val T_AFDELING = "afdeling"
        const val T_GANG = "gangcode"
        const val T_JOB = "JOB"
        const val T_LOCATION = "location_code"

        const val T_FRUIT_COUNTING = "fruit_counting"
        const val T_TPH = "TPH"

    }


    override fun onCreate(db: SQLiteDatabase) {
        try {
            Log.d("DB_CHECK", "--- Memulai Pembuatan Database v$DATABASE_VERSION ---")

            // 1. Tabel Users
            db.execSQL("CREATE TABLE IF NOT EXISTS $T_USERS ($U_USERNAME TEXT PRIMARY KEY COLLATE NOCASE, $U_PASSWORD TEXT, $U_EMPCODE TEXT, $U_FCBA TEXT, $U_DIVISI TEXT, $U_GANG TEXT, $U_ROLE TEXT)")

            // 2. Tabel Menu
            db.execSQL("CREATE TABLE IF NOT EXISTS $T_MENU (CODE INTEGER PRIMARY KEY, NAME TEXT, ROUTE TEXT)")

            // 3. Tabel Akses Menu
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS $T_MENU_ACCESS (
                    ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    EMP_ID TEXT,
                    MENU_CODE INTEGER,
                    IS_GRANTED INTEGER DEFAULT 1,
                    VALID_UNTIL LONG
                )
            """.trimIndent())

            // 4. Tabel Master Dropdown RKH
            db.execSQL("CREATE TABLE IF NOT EXISTS $T_AFDELING (FCCODE TEXT PRIMARY KEY, FCNAME TEXT, FCBA TEXT)")

            db.execSQL("DROP TABLE IF EXISTS $T_GANG")
            db.execSQL("CREATE TABLE IF NOT EXISTS $T_GANG (FCCODE TEXT PRIMARY KEY, FCNAME TEXT, FCBA TEXT, FCENTRY TEXT, FCEDIT TEXT)")

            // PERBAIKAN DI SINI: Tabel JOB sudah dibersihkan dari error string
            db.execSQL("DROP TABLE IF EXISTS $T_JOB")
            val createJobTable = """
    CREATE TABLE IF NOT EXISTS $T_JOB (
        FCCODE TEXT PRIMARY KEY,
        FCNAME TEXT,
        JOBGROUPCODE TEXT,
        JOB_FORCROPTYPE TEXT,
        JOB_FORFIELDSTATUS TEXT,
        JOB_CATEGORY TEXT,
        UNITOFMEASUREMENT TEXT,
        ACCOUNTCODE TEXT,
        FCENTRY TEXT,
        FCEDIT TEXT,
        FCIP TEXT,
        FCBA TEXT,
        JOB_FORACCOUNTTYPE TEXT,
        LASTUPDATE TEXT,
        LASTTIME TEXT,
        JOB_FORBALANCESHEETCODE TEXT,
        JOB_FORBALANCESHEETNAME TEXT,
        JOB_FORPROFITANDLOSSCODE TEXT,
        JOB_FORPROFITANDLOSSNAME TEXT,
        JOB_OWN_TB TEXT,
        PASIVA_AKTIVA TEXT,
        RATE REAL,
        HK TEXT,
        JOB_FORCASHFLOWCODE TEXT,
        JOB_FORCASHFLOWNAME TEXT,
        AKTIVA_01 TEXT,
        AKTIVA_02 TEXT,
        AKTIVA_03 TEXT,
        AKTIVA_04 TEXT,
        AKTIVA_05 TEXT,
        AKTIVA_06 TEXT,
        AKTIVA_07 TEXT,
        AKTIVA_08 TEXT,
        AKTIVA_09 TEXT,
        PASIVA_01 TEXT,
        PASIVA_02 TEXT,
        PASIVA_03 TEXT,
        PASIVA_04 TEXT,
        PASIVA_05 TEXT,
        PASIVA_06 TEXT,
        PASIVA_07 TEXT,
        PASIVA_08 TEXT,
        PASIVA_09 TEXT,
        COA TEXT,
        COA_NAME TEXT,
        OPENING_TB REAL,
        UOM_UNIT TEXT,
        HEADCODE TEXT,
        HEADNAME TEXT,
        SUB_GROUP_CODE TEXT,
        SUB_GROUP_NAME TEXT,
        PROFITANDLOSS_01 TEXT,
        PROFITANDLOSS_02 TEXT,
        PROFITANDLOSS_03 TEXT,
        PROFITANDLOSS_04 TEXT,
        PROFITANDLOSS_05 TEXT,
        PROFITANDLOSS_06 TEXT,
        PROFITANDLOSS_07 TEXT,
        PROFITANDLOSS_08 TEXT
    )
""".trimIndent()
            db.execSQL(createJobTable)

            db.execSQL("CREATE TABLE IF NOT EXISTS $T_LOCATION (FCCODE TEXT PRIMARY KEY, FCNAME TEXT, FCBA TEXT)")

            // 5. Tabel RKH
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS $T_RKH (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    fcba TEXT,
                    afdeling TEXT,
                    gangcode TEXT,
                    job_code TEXT,
                    location_code TEXT,
                    jumlah_hk REAL,
                    unit REAL,
                    output REAL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """.trimIndent())

            // 6. Tabel Employee
            db.execSQL("DROP TABLE IF EXISTS $T_EMP")
            db.execSQL("""
    CREATE TABLE IF NOT EXISTS $T_EMP (
                FCCODE TEXT NOT NULL, FCNAME TEXT, SECTIONNAME TEXT, GANGCODE TEXT,
                DATEOFBIRTH TEXT, RELIGION TEXT, RACE TEXT, IDENTITYCARD TEXT,
                HIGHESTEDUCATION TEXT, YEAROFGRADUATION TEXT, DATEOFJOIN TEXT,
                DATEOFDESIGNATION TEXT, PRESENTGRADE TEXT, PRESENTGRADE_DETAIL TEXT,
                PRESENTRATE REAL, PREVIOUSGRADE TEXT, PREVIOUSGRADE_DETAIL TEXT,
                PREVIOUSRATE REAL, MARITALSTATUS TEXT, SPOUSE TEXT, CHILDNAME_1 TEXT,
                CHILDNAME_2 TEXT, DEPENDENTCHILD TEXT, JAMSOSTEKNO TEXT,
                JAMSOSTEKCATEGORY TEXT, "POSITION" TEXT, DATETERMINATE TEXT,
                FCENTRY TEXT, FCEDIT TEXT, FCIP TEXT, FCBA TEXT NOT NULL,
                NATIONALITY TEXT, NPWP TEXT, NATURETYPE TEXT, LASTUPDATE TEXT,
                LASTTIME TEXT, INCLUDEDTAXCALCULATION TEXT, TAXSTATUS TEXT,
                PAYMENTTYPE TEXT, BANKNAME TEXT, ACCOUNTNO TEXT, OWNERSHIPOFACCOUNT TEXT,
                GENDER TEXT, TAXPAIDBYCOMPANY TEXT, ADDRESS TEXT, BPJSKES_CATEGORY TEXT,
                BPJSKES_NO TEXT, COMPANY_NIK TEXT, SUBGRADE TEXT, EMP_TYPE TEXT,
                PRESENTGRADE2 TEXT, PRESENTGRADE_DETAIL2 TEXT, PREVIOUSGRADE2 TEXT,
                PREVIOUSGRADE_DETAIL2 TEXT, REMARKS_TERMINATE TEXT, BLACKLIST TEXT,
                GOLONGAN TEXT, STRUKFUNGSI TEXT, SECTIONNAMEDTL TEXT, JOBCODE TEXT,
                TARGETTYPE TEXT, TARGETCODE TEXT, CUTI TEXT, BPJSUSINGTHP TEXT,
                GET_TKOM TEXT, GET_TRUMAH TEXT, GET_TTETAP TEXT, GET_TBK TEXT,
                GET_TTRANS TEXT, GET_TMAKAN TEXT, GET_TJAB TEXT, GET_JHT TEXT,
                GET_JP TEXT, FACEEMBEDDING TEXT,
                PRIMARY KEY (FCCODE, FCBA)
            )
        """.trimIndent() )


                    // 7. Tabel Transaksi & Absensi
            db.execSQL("CREATE TABLE IF NOT EXISTS $T_ATT ($A_ID INTEGER PRIMARY KEY AUTOINCREMENT, $A_EMP_ID TEXT NOT NULL, $A_FCBA TEXT NOT NULL, $A_EMP_NAME TEXT NOT NULL, $A_ACTION TEXT NOT NULL, $A_TIMESTAMP DATETIME DEFAULT CURRENT_TIMESTAMP, $A_SOURCE TEXT DEFAULT 'INPUT')")
            db.execSQL("CREATE TABLE IF NOT EXISTS $T_FACE ($F_ID INTEGER PRIMARY KEY AUTOINCREMENT, $F_EMP_ID TEXT NOT NULL, $F_FCBA TEXT NOT NULL, $F_FEATURES TEXT NOT NULL, $F_CAPTURED DATETIME DEFAULT CURRENT_TIMESTAMP)")

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS $T_MAINTENANCE (
                    $M_ID INTEGER PRIMARY KEY AUTOINCREMENT, $M_FCBA TEXT, $M_RKH TEXT, $M_GANG TEXT,
                    $M_S1 TEXT, $M_S2 TEXT, $M_S3 TEXT, $M_S4 TEXT, $M_WORKERS TEXT, 
                    $M_LOCATION TEXT, $M_UNIT REAL, $M_OUTPUT REAL, $M_RATE REAL,
                    $M_BERAS INTEGER, $M_LEMBUR REAL, $M_CREATED TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """.trimIndent())

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS $T_PROGRESS (
                    $P_ID INTEGER PRIMARY KEY AUTOINCREMENT, $P_RKH TEXT, $P_CATEGORY TEXT,
                    $P_EMP_ID TEXT, $P_EMP_IDS TEXT, $P_BLOCK TEXT, $P_UNIT REAL,
                    $P_OUTPUT REAL, $P_RATE REAL, $P_LEMBUR REAL, $P_BERAS INTEGER,
                    $P_RESULT TEXT, $P_NOTES TEXT, $P_STATUS TEXT, $P_TIMESTAMP DATETIME DEFAULT CURRENT_TIMESTAMP
                )
            """.trimIndent())

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS $T_FRUIT_COUNTING (
                    id INTEGER PRIMARY KEY AUTOINCREMENT, fcba TEXT, tanggal TEXT, no_rkh TEXT,
                    gang_code TEXT, supervisi1 TEXT, supervisi2 TEXT, supervisi3 TEXT, supervisi4 TEXT,
                    karyawan_ids TEXT, location_code TEXT, unit REAL, output REAL, tph_code TEXT,
                    is_beras INTEGER, lembur REAL, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """.trimIndent())

            db.execSQL("DROP TABLE IF EXISTS TPH")
            db.execSQL("""
    CREATE TABLE IF NOT EXISTS TPH (
        FCCODE TEXT PRIMARY KEY, FCNAME TEXT, FIELDCODE TEXT, 
        "SECTION" TEXT, FCBA TEXT, LATITUDE REAL, LONGITUDE REAL,
        TDATE TEXT, LT REAL, LG REAL,
        FCENTRY TEXT, FCEDIT TEXT, FCIP TEXT -- Tambahkan ini
    )
""".trimIndent())

            // Tambahkan ini di dalam onCreate
            db.execSQL("""
    CREATE TABLE IF NOT EXISTS T_SPB (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        spb_no TEXT,
        fcba TEXT,
        no_rkh TEXT,
        location_code TEXT,
        mill_name TEXT,
        driver_name TEXT,
        vehicle_no TEXT,
        tph_code TEXT,
        total_janjang INTEGER,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    )
""".trimIndent())

            db.execSQL("""
            CREATE TABLE IF NOT EXISTS BUSINESSUNIT (
                FCCODE TEXT PRIMARY KEY,
                FCNAME TEXT,
                FCCOMPANYCODE TEXT,
                FCCOMPANYNAME TEXT,
                FCAREA TEXT,
                FCOWNERSHIP TEXT,
                FCMANAGER TEXT,
                FCTELEPHONE TEXT,
                FCFAX TEXT,
                FCEMAIL TEXT,
                FCKTU TEXT,
                FCTYPE TEXT,
                FCCROP TEXT,
                LASTUPDATE TEXT,
                LASTTIME TEXT,
                ORDERDATA INTEGER,
                GROUPPLANTATION TEXT,
                PLANTATION TEXT,
                GROUPREGION TEXT,
                REGION TEXT,
                ISACTIVE TEXT,
                MILLCAPACITY TEXT,
                NPWPCOMPANY TEXT,
                NPWPOWNERCOMPANY TEXT,
                NPWPDIRECTOR TEXT,
                NPWPOWNERDIRECTOR TEXT,
                SAPLOCATION TEXT,
                ADDRESS TEXT
            )
        """.trimIndent())

            db.execSQL("""
    CREATE TABLE IF NOT EXISTS FIELD (
        FCCODE TEXT PRIMARY KEY,
        FCNAME TEXT,
        DIVISION TEXT,
        HECTARAGEPLANTED REAL,
        TERRAINTYPE TEXT,
        SOILTYPE TEXT,
        OWNERSHIP TEXT,
        ACTIVATION TEXT,
        CROPTYPE TEXT,
        PLANTINGDATE TEXT,
        PLANTINGMATERIAL TEXT,
        TOTALSTAND INTEGER,
        STATUS TEXT,
        PREVSEMESTER_ABW REAL,
        PRESSEMESTER_ABW REAL,
        HARVESTINGBASED_ABW REAL,
        MAIN_ROAD TEXT,
        COLLECTIONROAD TEXT,
        MAINDRAIN INTEGER,
        COLLECTIONDRAIN INTEGER,
        SUBSIDIARYDRAIN INTEGER,
        COLLECTIONPOINT INTEGER,
        HARVESTINGBRIDGE INTEGER,
        PERMANENTBRIDGE INTEGER,
        WOODBRIDGE INTEGER,
        BUIST INTEGER,
        OWLNEST INTEGER,
        FCENTRY TEXT,
        FCEDIT TEXT,
        FCIP TEXT,
        FCBA TEXT,
        LASTUPDATE TEXT,
        LASTTIME TEXT,
        HARVESTINGSECTION TEXT,
        FIELD_INFO_01 TEXT,
        FIELD_INFO_02 TEXT,
        FIELD_INFO_03 TEXT,
        FIELD_INFO_04 TEXT,
        FIELD_INFO_05 TEXT,
        ISACTIVE_HARVESTED TEXT,
        VALID_FROM TEXT,
        AREAL_LC REAL,
        CADANGAN REAL,
        OKUPASI REAL,
        BIBITAN REAL,
        PABRIK REAL,
        EMPLASMEN REAL,
        PARIT REAL,
        JALAN REAL,
        PRASARANA_LAINNYA REAL,
        QUARRY REAL,
        TANGGUL REAL,
        ENCLAVE REAL,
        TAMBANG REAL,
        KONSERVASI REAL,
        BUKIT_SUNGAI REAL,
        NO_IMAGERY TEXT,
        LUAS_GROSS REAL,
        HECTARAGEHARVESTED REAL,
        PRODUCTIVESTAND REAL,
        HARVESTINGSECTIONNUMBER TEXT,
        PLANTINGPOINT REAL,
        FULLSTAND TEXT,
        STATUS_TANAMAN TEXT,
        KOPERASI TEXT
    )
""".trimIndent())



            // === LANGKAH 2: ISI DATA AWAL ===

            // Isi Master Menu (Gunakan INSERT OR REPLACE agar tidak error jika dijalankan ulang)
            db.execSQL("INSERT OR REPLACE INTO $T_MENU VALUES (1, 'ABSENSI', 'HOME')")
            db.execSQL("INSERT OR REPLACE INTO $T_MENU VALUES (2, 'PROGRESS KERJA', 'PROGRESS_MENU')")
            db.execSQL("INSERT OR REPLACE INTO $T_MENU VALUES (3, 'PERHITUNGAN BUAH', 'FRUIT_COUNTING')")
            db.execSQL("INSERT OR REPLACE INTO $T_MENU VALUES (4, 'PEMBUATAN SPB', 'SPB_MENU')")
            db.execSQL("INSERT OR REPLACE INTO $T_MENU VALUES (5, 'ANCAK PANEN', 'ANCAK_PANEN')")
            db.execSQL("INSERT OR REPLACE INTO $T_MENU VALUES (6, 'AKP', 'AKP_FORM')")
            db.execSQL("INSERT OR REPLACE INTO $T_MENU VALUES (7, 'MASTER DATA', 'EMPLOYEE_FORM')")
            db.execSQL("INSERT OR REPLACE INTO $T_MENU VALUES (8, 'RENCANA KERJA', 'RKH_VIEW')")

            // Isi User Default & Hak Akses
            insertDefaultUsers(db)

            Log.d("DB_CHECK", "--- Database Berhasil Dibuat Lengkap ---")

        } catch (e: Exception) {
            Log.e("DB_ERROR", "Gagal di onCreate: ${e.message}")
        }
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        // Cara Ampuh: Paksa input user setiap kali DB dibuka
        // agar tidak bergantung pada suksesnya onCreate
        try {
            insertDefaultUsers(db)
            Log.d("DB_CHECK", "onOpen: Memastikan user default tersedia.")
        } catch (e: Exception) {
            Log.e("DB_ERROR", "Gagal di onOpen: ${e.message}")
        }
    }

    private fun insertDefaultUsers(db: SQLiteDatabase) {
        val users = listOf(
            // Format: Username, Password, EmpCode, Role, FCBA (BA Code)
            arrayOf("admin", "1234", "ADM00", "ADMIN", "SRE"),
            arrayOf("mandor1", "1234", "MND01", "MANDOR", "SRE"),
            arrayOf("mandor2", "qwerty", "MND02", "MANDOR", "SRE"),
            arrayOf("kerani1", "1234", "KRN01", "KERANI", "SRE"),
            arrayOf("kerani2", "pass123", "KRN02", "KERANI", "SRE"),
            arrayOf("tester_expired", "1234", "EXP01", "KERANI", "SRE")
        ) // TIDAK ADA KURUNG TUTUP FUNGSI DI SINI

        users.forEach { user ->
            val username = user[0]
            val password = user[1]
            val empCode = user[2]
            val role = user[3]
            val fcba = user[4]

            val cvUser = ContentValues().apply {
                put(U_USERNAME, username)
                put(U_PASSWORD, password)
                put(U_EMPCODE, empCode)
                put(U_ROLE, role)
                put(U_FCBA, fcba)
            }
            // Menggunakan insertWithOnConflict agar data masuk/terupdate
            db.insertWithOnConflict(T_USERS, null, cvUser, SQLiteDatabase.CONFLICT_REPLACE)

            // Logika Hak Akses Menu
            when (role) {
                "ADMIN", "MANDOR" -> {
                    // Admin & Mandor dapat menu 1 sampai 9
                    for (menuCode in 1..9) {
                        insertAccess(db, empCode, menuCode, isGranted = 1, daysValid = 365)
                    }
                }
                "KERANI" -> {
                    if (username == "tester_expired") {
                        insertAccess(db, empCode, 1, isGranted = 1, daysValid = -1)
                    } else {
                        // Kerani sekarang dapat menu 1 (Absen), 4 (SPB), dan 9 (Kirim Data/QR)
                        listOf(1, 4, 9).forEach { menuCode ->
                            insertAccess(db, empCode, menuCode, isGranted = 1, daysValid = 30)
                        }
                    }
                }
            }
        }
        Log.d("DB_CHECK", "Data Tester dan Hak Akses berhasil dimasukkan.")
    } // KURUNG TUTUP FUNGSI SEHARUSNYA DI SINI

    // Fungsi pembantu (helper) agar kode lebih rapi
    private fun insertAccess(db: SQLiteDatabase, empId: String, menuCode: Int, isGranted: Int, daysValid: Int) {
        val cvAccess = ContentValues().apply {
            put("EMP_ID", empId)
            put("MENU_CODE", menuCode)
            put("IS_GRANTED", isGranted)
            // Menghitung masa berlaku
            val validityPeriod = daysValid.toLong() * 24 * 60 * 60 * 1000
            put("VALID_UNTIL", System.currentTimeMillis() + validityPeriod)
        }
        db.insert("T_MENU_ACCESS", null, cvAccess)
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $T_USERS")
        db.execSQL("DROP TABLE IF EXISTS $T_EMP")
        db.execSQL("DROP TABLE IF EXISTS $T_ATT")
        db.execSQL("DROP TABLE IF EXISTS $T_FACE")
        db.execSQL("DROP TABLE IF EXISTS $T_MAINTENANCE")
        db.execSQL("DROP TABLE IF EXISTS fruit_counting")
        db.execSQL("DROP TABLE IF EXISTS table_ancak_panen")
        db.execSQL("DROP TABLE IF EXISTS T_MENU")
        db.execSQL("DROP TABLE IF EXISTS T_MENU_ACCESS")
        onCreate(db)
    }

    fun checkLogin(username: String, pass: String): UserProfile? {
        val u = username.trim()
        val p = pass.trim()

        return try {
            val db = readableDatabase
            val query = "SELECT * FROM $T_USERS WHERE $U_USERNAME = ? COLLATE NOCASE"

            db.rawQuery(query, arrayOf(u)).use { c ->
                if (c.moveToFirst()) {
                    val dbPass = c.getString(c.getColumnIndexOrThrow(U_PASSWORD))

                    Log.d("LOGIN_DEBUG", "User ditemukan! DB_Pass: '$dbPass' | Input: '$p'")

                    if (dbPass == p) {
                        return UserProfile(
                            username = c.getString(c.getColumnIndexOrThrow(U_USERNAME)),
                            empcode = c.getString(c.getColumnIndexOrThrow(U_EMPCODE)),
                            fcba = c.getString(c.getColumnIndexOrThrow(U_FCBA)) ?: "",
                            divisi = c.getString(c.getColumnIndexOrThrow(U_DIVISI)) ?: "",
                            gang = c.getString(c.getColumnIndexOrThrow(U_GANG)) ?: "",
                            role = c.getString(c.getColumnIndexOrThrow(U_ROLE)) ?: ""
                        )
                    } else {
                        Log.e("LOGIN_DEBUG", "Password salah.")
                    }
                } else {
                    Log.e("LOGIN_DEBUG", "Username '$u' tidak ditemukan di tabel $T_USERS")
                }
                null
            }
        } catch (e: Exception) {
            Log.e("DB_ERROR", "Login failed: ${e.message}")
            null
        }
    }

    // Tambahkan kurung tutup untuk class AttendanceDatabaseHelper di paling bawah


    // --- FUNGSI SAVE (TANPA DB.CLOSE()) ---



    fun getEmployeeByOnlyCode(code: String): Employee? {
        return try {
            val db = this.readableDatabase
            db.query(T_EMP, null, "$E_FCCODE = ?", arrayOf(code), null, null, null).use { cursor ->
                if (cursor.moveToFirst()) {
                    Employee(
                        fccode = cursor.getString(cursor.getColumnIndexOrThrow(E_FCCODE)),
                        fcba = cursor.getString(cursor.getColumnIndexOrThrow(E_FCBA)),
                        name = cursor.getString(cursor.getColumnIndexOrThrow(E_NAME)) ?: "",
                        sectionName = cursor.getString(cursor.getColumnIndexOrThrow(E_SECTION)) ?: "",
                        gangCode = cursor.getString(cursor.getColumnIndexOrThrow(E_GANG)) ?: "",
                        position = cursor.getString(cursor.getColumnIndexOrThrow(E_POSITION)) ?: ""
                    )
                } else null
            }
        } catch (e: Exception) {
            Log.e("DB_ERROR", "Error getEmployeeByOnlyCode: ${e.message}")
            null
        }
    }





    fun insertFaceMapping(fccode: String, fcba: String, features: FloatArray): Boolean {
        val db = this.writableDatabase
        return try {
            db.beginTransaction()

            // Simpan ke tabel face_mappings (untuk histori/backup)
            val valuesMapping = ContentValues().apply {
                put("fccode", fccode)
                put("fcba", fcba)
                put("face_features", features.joinToString(","))
            }
            db.insert("face_mappings", null, valuesMapping)

            // UPDATE juga ke tabel EMPLOYEE di kolom FACEEMBEDDING
            // Ini agar saat scanner jalan, dia cukup cek satu tabel saja
            val valuesEmp = ContentValues().apply {
                put("FACEEMBEDDING", features.joinToString(","))
                put("LASTUPDATE", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
            }

            val updated = db.update(
                T_EMP,
                valuesEmp,
                "FCCODE = ? AND FCBA = ?",
                arrayOf(fccode, fcba)
            )

            db.setTransactionSuccessful()
            db.endTransaction()
            updated > 0
        } catch (e: Exception) {
            db.endTransaction()
            Log.e("DB_ERROR", "Gagal simpan wajah: ${e.message}")
            false
        }
    }


    /**
     * Also ensure you have this function which is used by FaceDataHelper
     */
    fun getFaceFeaturesForEmployee(fccode: String, fcba: String): List<FloatArray> {
        val featuresList = mutableListOf<FloatArray>()
        val db = this.readableDatabase
        try {
            // 1. Ambil dari tabel Face Mapping (Hasil Registrasi Manual di HP)
            db.rawQuery("SELECT $F_FEATURES FROM $T_FACE WHERE $F_EMP_ID = ? AND $F_FCBA = ?", arrayOf(fccode, fcba)).use { cursor ->
                while (cursor.moveToNext()) {
                    cursor.getString(0)?.let { str ->
                        featuresList.add(str.split(",").map { it.toFloat() }.toFloatArray())
                    }
                }
            }

            // 2. Ambil dari tabel EMPLOYEE (Hasil Impor SQL Oracle)
            db.rawQuery("SELECT $E_FACE_EMB FROM $T_EMP WHERE $E_FCCODE = ? AND $E_FCBA = ?", arrayOf(fccode, fcba)).use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(0)?.let { str ->
                        // Bersihkan karakter [ ] jika ada di data SQL
                        val cleaned = str.replace("[", "").replace("]", "").trim()
                        if (cleaned.isNotEmpty()) {
                            featuresList.add(cleaned.split(",").map { it.toFloat() }.toFloatArray())
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("DB_ERROR", "Gagal ambil fitur wajah: ${e.message}")
        }
        return featuresList
    }

    fun getAllAttendance(): List<AttendanceRecord> {
        val recordList = mutableListOf<AttendanceRecord>()
        try {
            val db = this.readableDatabase
            db.rawQuery("SELECT * FROM $T_ATT ORDER BY $A_TIMESTAMP DESC", null).use { c ->
                while (c.moveToNext()) {
                    recordList.add(AttendanceRecord(
                        id = c.getInt(c.getColumnIndexOrThrow(A_ID)),
                        employeeId = c.getString(c.getColumnIndexOrThrow(A_EMP_ID)),
                        fcbaId = c.getString(c.getColumnIndexOrThrow(A_FCBA)),
                        employeeName = c.getString(c.getColumnIndexOrThrow(A_EMP_NAME)),
                        action = c.getString(c.getColumnIndexOrThrow(A_ACTION)),
                        timestamp = c.getString(c.getColumnIndexOrThrow(A_TIMESTAMP))
                    ))
                }
            }
        } catch (e: Exception) { }
        return recordList
    }



    fun getFaceMappingCount(fccode: String, fcba: String): Int {
        val db = this.readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM face_mappings WHERE fccode = ? AND fcba = ?",
            arrayOf(fccode, fcba)
        )
        var count = 0
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0)
        }
        cursor.close()
        return count
    }



    // In AttendanceDatabaseHelper.kt

    // GANTI fungsi saveAttendance yang lama dengan yang ini:
    fun saveAttendance(empId: String, fcba: String, name: String, action: String, source: String = "FACE"): Boolean {
        return try {
            val db = this.writableDatabase
            val cv = ContentValues().apply {
                put(A_EMP_ID, empId)
                put(A_FCBA, fcba)
                put(A_EMP_NAME, name)
                put(A_ACTION, action)
                put(A_SOURCE, source)
            }
            val result = db.insert(T_ATT, null, cv)
            Log.d("DB_CHECK", "Absensi Berhasil Disimpan: $result")
            result != -1L
        } catch (e: Exception) {
            Log.e("DB_ERROR", "Gagal simpan absensi: ${e.message}")
            false
        }
    }



    fun getAllMasterEmployees(): List<Employee> {
        val list = mutableListOf<Employee>()
        try {
            val db = readableDatabase
            db.rawQuery("SELECT * FROM $T_EMP ORDER BY $E_NAME ASC", null).use { cursor ->
                while (cursor.moveToNext()) {
                    list.add(Employee(
                        fccode = cursor.getString(cursor.getColumnIndexOrThrow(E_FCCODE)),
                        fcba = cursor.getString(cursor.getColumnIndexOrThrow(E_FCBA)),
                        name = cursor.getString(cursor.getColumnIndexOrThrow(E_NAME)) ?: "",
                        sectionName = cursor.getString(cursor.getColumnIndexOrThrow(E_SECTION)) ?: "",
                        gangCode = cursor.getString(cursor.getColumnIndexOrThrow(E_GANG)) ?: "",
                        position = cursor.getString(cursor.getColumnIndexOrThrow(E_POSITION)) ?: ""
                    ))
                }
            }
            Log.d("DB_CHECK", "Berhasil mengambil ${list.size} data employee.")
        } catch (e: Exception) {
            Log.e("DB_ERROR", "Error getAllMasterEmployees: ${e.message}")
        }
        return list
    }

    // Tambahkan parameter kedua: onProgress (sebuah fungsi callback)
    fun importSqlFromAssets(fileName: String, onProgress: (Int) -> Unit = {}) {
        val db = writableDatabase
        try {
            val inputStream = context.assets.open(fileName)
            val content = inputStream.bufferedReader().use { it.readText() }

            // 1. Bersihkan sintaks Oracle secara menyeluruh
            // Hapus "TIMESTAMP'" dan kata "TIMESTAMP" saja agar SQLite tidak bingung
            val cleanSql = content.replace("TIMESTAMP'", "'", ignoreCase = true)
                .replace("TIMESTAMP", "", ignoreCase = true)

            // 2. Pecah per perintah SQL berdasarkan titik koma
            val statements = cleanSql.split(";")
            val totalStatements = statements.size
            var processedCount = 0

            db.beginTransaction()
            try {
                statements.forEach { statement ->
                    val sql = statement.trim()
                    if (sql.isNotEmpty()) {
                        // Gunakan REPLACE agar data SRE yang sudah ada tertimpa yang baru (tidak duplikat/error)
                        val finalSql = if (sql.uppercase().startsWith("INSERT INTO")) {
                            sql.replaceFirst("INSERT INTO", "INSERT OR REPLACE INTO", ignoreCase = true)
                        } else {
                            sql
                        }

                        try {
                            db.execSQL(finalSql)
                            processedCount++

                            // Kirim progress ke UI dalam persentase (0-100)
                            if (totalStatements > 0) {
                                val progress = (processedCount * 100) / totalStatements
                                onProgress(progress)
                            }
                        } catch (e: Exception) {
                            // Log error baris tertentu tapi lanjut ke baris berikutnya
                            Log.e("IMPORT_ERROR", "Gagal di $fileName (baris $processedCount): ${e.message}")
                        }
                    }
                }
                db.setTransactionSuccessful()
                Log.d("DB_CHECK", "IMPORT BERHASIL: $fileName ($processedCount data SRE dimasukkan)")
            } finally {
                db.endTransaction()
            }
        } catch (e: Exception) {
            Log.e("DB_ERROR", "Error membaca file $fileName: ${e.message}")
        }
    }

    fun getSupervisors(): List<String> {
        val list = mutableListOf<String>()
        val db = readableDatabase
        // Mencari karyawan yang jabatannya mengandung kata MANDOR atau SUPERVISI
        val query = "SELECT $E_NAME FROM $T_EMP WHERE $E_POSITION LIKE '%MANDOR%' OR $E_POSITION LIKE '%SUPERVISI%'"
        val cursor = db.rawQuery(query, null)
        while (cursor.moveToNext()) {
            list.add(cursor.getString(0))
        }
        cursor.close()
        return list.ifEmpty { listOf("Tidak ada data Supervisi") }
    }



    fun getAttendanceForQRCode(): String {
        val db = readableDatabase
        val result = StringBuilder()

        // Gunakan konstanta A_EMP_ID, A_FCBA, dan A_ACTION agar tidak error 'no such column'
        // Kita filter berdasarkan tanggal hari ini menggunakan date(A_TIMESTAMP)
        val query = "SELECT $A_EMP_ID, $A_FCBA, $A_ACTION FROM $T_ATT WHERE date($A_TIMESTAMP) = date('now')"

        try {
            db.rawQuery(query, null).use { cursor ->
                while (cursor.moveToNext()) {
                    val empId = cursor.getString(0) ?: ""
                    val fcba = cursor.getString(1) ?: ""
                    val action = cursor.getString(2) ?: ""

                    // Format Barcode harus disesuaikan dengan fungsi receiveTransferredData:
                    // Format: "fccode,fcba,action;fccode,fcba,action"
                    if (result.isNotEmpty()) result.append(";")
                    result.append("$empId,$fcba,$action")
                }
            }
        } catch (e: Exception) {
            Log.e("DB_ERROR", "Gagal mengambil data QR: ${e.message}")
        }

        return result.toString()
    }


    // --- Tambahkan di bawah data class Employee ---

    fun receiveTransferredData(rawString: String): Int {
        val db = this.writableDatabase
        var count = 0
        try {
            // Format Barcode Mandor: "fccode,fcba,action;fccode,fcba,action"
            val rows = rawString.split(";")
            rows.forEach { row ->
                val data = row.split(",")
                if (data.size >= 3) {
                    val cv = ContentValues().apply {
                        put(A_EMP_ID, data[0])
                        put(A_FCBA, data[1])
                        put(A_ACTION, data[2])
                        put(A_EMP_NAME, "Worker ${data[0]}") // Nama sementara
                        put(A_SOURCE, "TRANSFER")
                        put(A_TIMESTAMP, SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
                    }
                    val result = db.insert(T_ATT, null, cv)
                    if (result != -1L) count++
                }
            }
        } catch (e: Exception) {
            Log.e("DB_ERROR", "Gagal proses transfer: ${e.message}")
        }
        return count
    }

    fun getDropdownData(type: String, userFcba: String): List<String> {
        val list = mutableListOf<String>()
        val db = readableDatabase

        // 1. Sinkronisasi Nama Tabel (Case Insensitive & Mapping UI Key)
        val tableName = when (type.uppercase()) {
            "JOB", "JOB_CODE" -> T_JOB        // Tabel: JOB
            "GANG", "GANGCODE" -> T_GANG      // Tabel: gangcode
            "AFDELING" -> T_AFDELING          // Tabel: afdeling
            "LOCATION", "LOCATION_CODE" -> T_LOCATION
            "BLOCK" -> "TPH"                  // Tabel: TPH
            else -> ""
        }

        if (tableName.isEmpty()) {
            Log.e("DB_ERROR", "Tabel untuk type $type tidak ditemukan")
            return list
        }

        try {
            // 2. Tentukan Kolom yang akan diambil
            // JOB menggunakan FCNAME, TPH (Block) menggunakan FIELDCODE
            val column = when (type.uppercase()) {
                "BLOCK" -> "FIELDCODE"
                "LOCATION", "LOCATION_CODE" -> "FCCODE" // Biasanya Location Code mengambil ID/FCCODE
                else -> "FCNAME"
            }

            // 3. Query dengan Filter FCBA (Penting agar data SRE tidak muncul di BA lain)
            // Jika Admin (99), tampilkan semua. Jika mandor, filter sesuai BA-nya.
            // Di dalam getDropdownData
            val query: String
            val args: Array<String>?

            if (userFcba == "99") {
                query = "SELECT DISTINCT $column FROM $tableName WHERE $column IS NOT NULL ORDER BY $column ASC"
                args = null
            } else {
                // Paksa menggunakan SRE jika bukan admin
                query = "SELECT DISTINCT $column FROM $tableName WHERE FCBA = 'SRE' AND $column IS NOT NULL ORDER BY $column ASC"
                args = null
            }

            db.rawQuery(query, args).use { cursor ->
                while (cursor.moveToNext()) {
                    val value = cursor.getString(0)
                    if (!value.isNullOrEmpty()) {
                        list.add(value)
                    }
                }
            }
            Log.d("DB_CHECK", "Dropdown $type berhasil mengambil ${list.size} data")
        } catch (e: Exception) {
            Log.e("DB_ERROR", "Gagal ambil dropdown $type: ${e.message}")
        }
        return list
    }



    fun insertRKH(fcba: String, afd: String, gang: String, job: String, loc: String, hk: Double, unit: Double, out: Double): Long {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("fcba", fcba)
            put("afdeling", afd)
            put("gangcode", gang)
            put("job_code", job)
            put("location_code", loc)
            put("jumlah_hk", hk)
            put("unit", unit)
            put("output", out)
        }
        return db.insert(T_RKH, null, cv)
    }



    fun savePlantationProgress(
        rkh: String,
        category: String,
        employees: List<String>,
        supervisors: List<String>,
        unit: Double,
        output: Double,
        rate: Double,
        lembur: Int,
        beras: Int
    ): Long {
        val db = this.writableDatabase
        return try {
            val values = ContentValues().apply {
                put(P_RKH, rkh)
                put(P_CATEGORY, category)
                // Menggabungkan list ID karyawan menjadi satu string dipisahkan koma
                put(P_EMP_IDS, employees.joinToString(","))
                put(P_UNIT, unit)
                put(P_OUTPUT, output)
                put(P_RATE, rate)
                put(P_LEMBUR, lembur)
                put(P_BERAS, beras)
                put(P_STATUS, "COMPLETED")
            }
            db.insert(T_PROGRESS, null, values)
        } catch (e: Exception) {
            Log.e("DB_ERROR", "Gagal simpan progres plantation: ${e.message}")
            -1L
        }
    }


    fun getTPHByLocation(fieldCode: String): List<String> {
        val list = mutableListOf<String>()
        val db = readableDatabase
        // Mencari FCCODE (Nama TPH) berdasarkan FIELDCODE yang dipilih
        val query = "SELECT FCCODE FROM TPH WHERE FIELDCODE = ?"

        try {
            db.rawQuery(query, arrayOf(fieldCode)).use { cursor ->
                while (cursor.moveToNext()) {
                    list.add(cursor.getString(0))
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("DB_ERROR", "Gagal ambil TPH: ${e.message}")
        }
        return list
    }

    fun getGangsByAfdeling(afdeling: String, fcba: String = "SRE"): List<String> {
        val list = mutableListOf<String>()
        val db = readableDatabase

        // Kita mencari Gang yang bekerja di Afdeling tersebut (berdasarkan data Karyawan)
        // Atau jika Anda punya kolom AFDELING di tabel GANG, sesuaikan querynya
        val query = """
        SELECT DISTINCT GANGCODE 
        FROM $T_EMP 
        WHERE SECTIONNAME = ? AND FCBA = ? 
        ORDER BY GANGCODE ASC
    """.trimIndent()

        db.rawQuery(query, arrayOf(afdeling, fcba)).use { cursor ->
            while (cursor.moveToNext()) {
                val code = cursor.getString(0)
                if (!code.isNullOrEmpty()) {
                    list.add(code)
                }
            }
        }
        return list
    }


    // Di dalam AttendanceDatabaseHelper.kt

    fun getRKHList(): List<Map<String, String>> {
        val list = mutableListOf<Map<String, String>>()
        val db = readableDatabase
        val query = """
        SELECT r.id, r.fcba, r.gangcode, r.job_code, r.location_code, r.jumlah_hk, f.FCNAME 
        FROM $T_RKH r
        INNER JOIN FIELD f ON r.location_code = f.FCCODE
        WHERE r.fcba = 'SRE'
    """.trimIndent() // Tambahkan filter SRE di sini

        val cursor = db.rawQuery(query, null)
        while (cursor.moveToNext()) {
            val map = mutableMapOf<String, String>()
            map["no_rkh"] = cursor.getString(0)
            map["fcba"] = cursor.getString(1)
            map["gang_code"] = cursor.getString(2)
            map["job_code"] = cursor.getString(3)
            map["location"] = cursor.getString(4)
            map["jumlah_hk"] = cursor.getString(5)
            map["location_name"] = cursor.getString(6)
            list.add(map)
        }
        cursor.close()
        return list
    }

    fun getEmployeesAlreadyCheckedIn(fcba: String): List<Map<String, String>> {
        val list = mutableListOf<Map<String, String>>()
        val db = readableDatabase
        // Ambil tanggal hari ini format YYYY-MM-DD
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val query = """
        SELECT DISTINCT e.$E_FCCODE, e.$E_NAME 
        FROM $T_EMP e
        INNER JOIN $T_ATT a ON e.$E_FCCODE = a.$A_EMP_ID
        WHERE a.$A_FCBA = ? 
        AND a.$A_TIMESTAMP LIKE '$today%' 
        AND a.$A_ACTION = 'CHECK_IN'
    """.trimIndent()

        try {
            db.rawQuery(query, arrayOf(fcba)).use { c ->
                while (c.moveToNext()) {
                    list.add(mapOf(
                        "id" to (c.getString(0) ?: ""),
                        "name" to (c.getString(1) ?: "")
                    ))
                }
            }
        } catch (e: Exception) {
            Log.e("DB_ERROR", "Gagal ambil karyawan absen: ${e.message}")
        }
        return list
    }

    fun getBlockList(userFcba: String): List<String> {
        val list = mutableListOf<String>()
        val db = readableDatabase
        val query = if (userFcba == "99") "SELECT DISTINCT FIELDCODE FROM $T_TPH ORDER BY FIELDCODE ASC"
        else "SELECT DISTINCT FIELDCODE FROM $T_TPH WHERE FCBA = ? ORDER BY FIELDCODE ASC"
        val args = if (userFcba == "99") null else arrayOf(userFcba)

        try {
            db.rawQuery(query, args).use { c ->
                while (c.moveToNext()) {
                    c.getString(0)?.let { list.add(it) }
                }
            }
        } catch (e: Exception) { Log.e("DB_ERROR", "Block: ${e.message}") }
        return list
    }



    // 1. Ambil JOB (Pekerjaan)
    fun getJobList(userFcba: String): List<String> {
        val list = mutableListOf<String>()
        try {
            val db = readableDatabase
            // Jika admin (99), lihat semua.
            // Jika data masih kosong, coba hilangkan filter FCBA sementara untuk testing
            val query = if (userFcba == "99") {
                "SELECT DISTINCT FCNAME FROM $T_JOB ORDER BY FCNAME ASC"
            } else {
                // Coba ganti query ini jika data tetap tidak muncul:
                // "SELECT DISTINCT FCNAME FROM $T_JOB ORDER BY FCNAME ASC" (Tanpa filter FCBA)
                "SELECT DISTINCT FCNAME FROM $T_JOB WHERE FCBA = ? ORDER BY FCNAME ASC"
            }

            val args = if (userFcba == "99") null else arrayOf(userFcba)

            db.rawQuery(query, args).use { c ->
                while (c.moveToNext()) {
                    list.add(c.getString(0))
                }
            }
        } catch (e: Exception) {
            Log.e("DB_ERROR", "Gagal ambil Job: ${e.message}")
        }
        return list
    }




    fun saveFruitCalculation(
        fcba: String,
        rkh: String,
        gang: String,
        supervisors: List<String>,
        employees: List<String>,
        location: String,
        tph: String,
        unit: Double,
        output: Double,
        rate: Double,
        beras: Int,
        lembur: Double,
        category: String = "PERHITUNGAN_BUAH"
    ): Long {
        val db = this.writableDatabase
        return try {
            val values = ContentValues().apply {
                put("fcba", fcba)
                put("no_rkh", rkh)
                put("gang_code", gang)
                put("supervisi1", supervisors.getOrNull(0) ?: "")
                put("supervisi2", supervisors.getOrNull(1) ?: "")
                put("supervisi3", supervisors.getOrNull(2) ?: "")
                put("supervisi4", supervisors.getOrNull(3) ?: "")
                put("karyawan_ids", employees.joinToString(","))
                put("location_code", location)
                put("tph_code", tph)
                put("unit", unit)
                put("output", output)
                // put("rate", rate) // Kolom 'rate' tidak ada di onCreate Anda, hati-hati
                put("is_beras", beras)
                put("lembur", lembur)
                // Gunakan kolom 'tanggal' sesuai onCreate
                put("tanggal", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
            }
            // GUNAKAN NAMA TABEL: "fruit_counting" (sesuai T_FRUIT_COUNTING)
            db.insert("fruit_counting", null, values)
        } catch (e: Exception) {
            Log.e("DB_ERROR", "Gagal simpan: ${e.message}")
            -1L
        }
    }


    // Ambil Blok berdasarkan Location Code
    fun getBlocksByLocation(locationCode: String): List<String> {
        val list = mutableListOf<String>()
        val db = readableDatabase

        // ANALISA:
        // 1. Di getRKHList, Anda melakukan JOIN ke tabel 'FIELD' menggunakan kolom 'FCCODE'.
        // 2. Jadi, tabel master untuk blok/lokasi adalah 'FIELD'.
        // 3. Kita gunakan kolom 'FCCODE' untuk kode blok dan 'FCNAME' (opsional) untuk nama.

        // Jika locationCode dari RKH adalah kode afdeling (misal: 'SRE'),
        // maka kita cari semua FCCODE di tabel FIELD yang berawalan 'SRE'.
        val query = "SELECT DISTINCT FCCODE FROM FIELD WHERE FCCODE LIKE ? ORDER BY FCCODE ASC"
        val selectionArgs = arrayOf("$locationCode%")

        try {
            db.rawQuery(query, selectionArgs).use { cursor ->
                while (cursor.moveToNext()) {
                    val blockCode = cursor.getString(0)
                    if (!blockCode.isNullOrEmpty()) {
                        list.add(blockCode)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("DB_ERROR", "Gagal getBlocksByLocation: ${e.message}")
        }

        // Jika masih kosong, kita coba ambil data dari tabel TPH (seperti fungsi getBlockList Anda)
        if (list.isEmpty()) {
            val queryTph = "SELECT DISTINCT FIELDCODE FROM TPH WHERE FIELDCODE LIKE ? ORDER BY FIELDCODE ASC"
            try {
                db.rawQuery(queryTph, selectionArgs).use { cursor ->
                    while (cursor.moveToNext()) {
                        cursor.getString(0)?.let { list.add(it) }
                    }
                }
            } catch (e: Exception) {
                Log.e("DB_ERROR", "Gagal getBlocks dari TPH: ${e.message}")
            }
        }

        return list
    }


    fun saveSPB(
        spbNo: String,
        fcba: String,
        rkh: String,
        location: String,
        mill: String,

        vehicle: String,
        tph: String,
        janjang: Int
    ): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put("spb_no", spbNo)
            put("fcba", fcba)
            put("no_rkh", rkh)
            put("location_code", location)
            put("mill_name", mill)

            put("vehicle_no", vehicle)
            put("tph_code", tph)
            put("total_janjang", janjang)
        }
        return db.insert("T_SPB", null, values)
    }



    fun getAfdelingList(userFcba: String): List<String> {
        val list = mutableListOf<String>()
        val db = readableDatabase
        // Di SQL anda, Afdeling ada di kolom "SECTION" pada tabel TPH
        val query = if (userFcba == "99") "SELECT DISTINCT \"SECTION\" FROM $T_TPH WHERE \"SECTION\" IS NOT NULL"
        else "SELECT DISTINCT \"SECTION\" FROM $T_TPH WHERE FCBA = ?"
        val args = if (userFcba == "99") null else arrayOf(userFcba)

        try {
            db.rawQuery(query, args).use { c ->
                while (c.moveToNext()) list.add(c.getString(0))
            }
        } catch (e: Exception) { Log.e("DB_ERROR", "Afd: ${e.message}") }
        return list
    }

    fun getGangList(userFcba: String): List<String> {
        val list = mutableListOf<String>()
        val db = readableDatabase
        val query = if (userFcba == "99") "SELECT DISTINCT GANGCODE FROM $T_EMP WHERE GANGCODE IS NOT NULL"
        else "SELECT DISTINCT GANGCODE FROM $T_EMP WHERE FCBA = ?"
        val args = if (userFcba == "99") null else arrayOf(userFcba)

        try {
            db.rawQuery(query, args).use { c ->
                while (c.moveToNext()) list.add(c.getString(0))
            }
        } catch (e: Exception) { }
        return list
    }

    fun getDrivers(): List<String> {
        val list = mutableListOf<String>()
        val db = readableDatabase
        // Query ini mencari karyawan dengan posisi/jabatan yang mengandung kata SUPIR atau DRIVER
        val query = "SELECT FCNAME FROM EMPLOYEE WHERE \"POSITION\" LIKE '%SUPIR%' OR \"POSITION\" LIKE '%DRIVER%'"

        try {
            db.rawQuery(query, null).use { cursor ->
                while (cursor.moveToNext()) {
                    list.add(cursor.getString(0))
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("DB_ERROR", "Gagal mengambil daftar supir: ${e.message}")
        }
        return list
    }

    fun getBusinessUnits(): List<String> {
        val list = mutableListOf<String>()
        val db = readableDatabase
        // Ambil yang namanya mengandung SRE atau SUNGAI ROTAN
        val query = "SELECT FCNAME FROM BUSINESSUNIT WHERE FCNAME LIKE '%SRE%' OR FCNAME LIKE '%SUNGAI ROTAN%' ORDER BY FCNAME ASC"

        try {
            db.rawQuery(query, null).use { cursor ->
                while (cursor.moveToNext()) {
                    cursor.getString(0)?.let { list.add(it) }
                }
            }
        } catch (e: Exception) {
            Log.e("DB_ERROR", "Gagal ambil BusinessUnit SRE: ${e.message}")
        }
        return list
    }





    fun getAllFieldCodes(): List<String> {
        val list = mutableListOf<String>()
        val db = readableDatabase
        // Mengambil FCCODE dari tabel FIELD (hasil import FIELD_202606011224.sql)
        val query = "SELECT FCCODE FROM FIELD ORDER BY FCCODE ASC"
        db.rawQuery(query, null).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(cursor.getString(0))
            }
        }
        return list
    }

    // Tambahkan di dalam class AttendanceDatabaseHelper
    fun getAllFruitCounting(): List<Map<String, String>> {
        val list = mutableListOf<Map<String, String>>()
        val db = readableDatabase
        // Gunakan 'fruit_counting' dan urutkan berdasarkan 'id' atau 'created_at'
        val query = "SELECT * FROM fruit_counting ORDER BY id DESC"
        try {
            db.rawQuery(query, null).use { cursor ->
                while (cursor.moveToNext()) {
                    val map = mutableMapOf<String, String>()
                    cursor.columnNames.forEach { col ->
                        map[col] = cursor.getString(cursor.getColumnIndexOrThrow(col)) ?: ""
                    }
                    list.add(map)
                }
            }
        } catch (e: Exception) {
            Log.e("DB_ERROR", "Error getAllFruitCounting: ${e.message}")
        }
        return list
    }



    // Fungsi untuk mengambil menu yang diizinkan untuk ID Karyawan tertentu
    // --- FUNGSI HAK AKSES MENU (FIX EXPIRED) ---
    fun getAllowedMenusForUser(empCode: String): List<String> {
        val list = mutableListOf<String>()
        val db = readableDatabase

        // Melakukan JOIN antara tabel akses (angka) dan tabel menu (teks rute)
        val query = """
        SELECT m.ROUTE 
        FROM $T_MENU m
        JOIN $T_MENU_ACCESS a ON m.CODE = a.MENU_CODE
        WHERE a.EMP_ID = ? AND a.IS_GRANTED = 1
    """.trimIndent()

        try {
            db.rawQuery(query, arrayOf(empCode)).use { cursor ->
                while (cursor.moveToNext()) {
                    val route = cursor.getString(0)
                    list.add(route)
                }
            }
            Log.d("DB_CHECK", "User $empCode memiliki akses ke: $list")
        } catch (e: Exception) {
            Log.e("DB_ERROR", "Gagal ambil menu: ${e.message}")
        }
        return list
    }

} // Penutup kelas AttendanceDatabaseHelper