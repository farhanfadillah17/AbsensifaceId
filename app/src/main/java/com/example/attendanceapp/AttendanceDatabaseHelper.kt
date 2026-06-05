package com.example.attendanceapp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
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
        const val DATABASE_NAME = "attendance_reset_final_v234.db"
        const val DATABASE_VERSION = 234

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
        const val T_JOB = "job_code"
        const val T_LOCATION = "location_code"

        const val T_FRUIT_COUNTING = "fruit_counting"
        const val T_TPH = "master_tph"

    }


    override fun onCreate(db: SQLiteDatabase) {
        try {
            Log.d("DB_CHECK", "--- Memulai Pembuatan Database v221 ---")

            // === LANGKAH 1: BUAT SEMUA TABEL TERLEBIH DAHULU ===

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
            db.execSQL("CREATE TABLE IF NOT EXISTS $T_AFDELING (name TEXT)")
            db.execSQL("CREATE TABLE IF NOT EXISTS $T_GANG (name TEXT)")
            db.execSQL("CREATE TABLE IF NOT EXISTS $T_JOB (name TEXT)")
            db.execSQL("CREATE TABLE IF NOT EXISTS $T_LOCATION (name TEXT)")

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

            // 6. Tabel Employee (Pastikan ini dibuat SEBELUM ada proses insert data master)
            val createEmpTable = """
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
        """.trimIndent()


            db.execSQL(createEmpTable)

            // 7. Tabel Transaksi & Absensi
            db.execSQL("CREATE TABLE IF NOT EXISTS $T_ATT ($A_ID INTEGER PRIMARY KEY AUTOINCREMENT, $A_EMP_ID TEXT NOT NULL, $A_FCBA TEXT NOT NULL, $A_EMP_NAME TEXT NOT NULL, $A_ACTION TEXT NOT NULL, $A_TIMESTAMP DATETIME DEFAULT CURRENT_TIMESTAMP, $A_SOURCE TEXT DEFAULT 'INPUT')")
            db.execSQL("CREATE TABLE IF NOT EXISTS $T_FACE ($F_ID INTEGER PRIMARY KEY AUTOINCREMENT, $F_EMP_ID TEXT NOT NULL, $F_FCBA TEXT NOT NULL, $F_FEATURES TEXT NOT NULL, $F_CAPTURED DATETIME DEFAULT CURRENT_TIMESTAMP)")
            db.execSQL("""
    CREATE TABLE IF NOT EXISTS $T_MAINTENANCE (
        $M_ID INTEGER PRIMARY KEY AUTOINCREMENT,
        $M_FCBA TEXT,
        $M_RKH TEXT,
        $M_GANG TEXT,
        $M_S1 TEXT,
        $M_S2 TEXT,
        $M_S3 TEXT,
        $M_S4 TEXT,
        $M_WORKERS TEXT, 
        $M_LOCATION TEXT,
        $M_UNIT REAL,
        $M_OUTPUT REAL,
        $M_RATE REAL,
        $M_BERAS INTEGER,
        $M_LEMBUR REAL,
        $M_CREATED TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    )
""".trimIndent())

            db.execSQL("""
            CREATE TABLE IF NOT EXISTS $T_PROGRESS (
                $P_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $P_RKH TEXT,
                $P_CATEGORY TEXT,
                $P_EMP_ID TEXT,
                $P_EMP_IDS TEXT,
                $P_BLOCK TEXT,
                $P_UNIT REAL,
                $P_OUTPUT REAL,
                $P_RATE REAL,
                $P_LEMBUR REAL,
                $P_BERAS INTEGER,
                $P_RESULT TEXT,
                $P_NOTES TEXT,
                $P_STATUS TEXT,
                $P_TIMESTAMP DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        """.trimIndent())


            db.execSQL("""
    CREATE TABLE IF NOT EXISTS $T_FRUIT_COUNTING (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        fcba TEXT,
        tanggal TEXT,
        no_rkh TEXT,
        gang_code TEXT,
        supervisi1 TEXT,
        supervisi2 TEXT,
        supervisi3 TEXT,
        supervisi4 TEXT,
        karyawan_ids TEXT, -- Disimpan sebagai String dipisah koma (CSV)
        location_code TEXT,
        unit REAL,
        output REAL,
        tph_code TEXT,
        is_beras INTEGER,
        lembur REAL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    )
""".trimIndent())

            db.execSQL("CREATE TABLE IF NOT EXISTS $T_TPH (id INTEGER PRIMARY KEY AUTOINCREMENT, location_code TEXT, name TEXT)")
            // === LANGKAH 2: ISI DATA AWAL (SETELAH SEMUA TABEL JADI) ===

            // Isi Master Menu
            db.execSQL("INSERT INTO $T_MENU VALUES (1, 'ABSENSI', 'HOME')")
            db.execSQL("INSERT INTO $T_MENU VALUES (2, 'PROGRESS KERJA', 'PROGRESS_MENU')")
            db.execSQL("INSERT INTO $T_MENU VALUES (3, 'PERHITUNGAN BUAH', 'FRUIT_COUNTING')")
            db.execSQL("INSERT INTO $T_MENU VALUES (4, 'PEMBUATAN SPB', 'SPB_MENU')")
            db.execSQL("INSERT INTO $T_MENU VALUES (5, 'ANCAK PANEN', 'ANCAK_PANEN')")
            db.execSQL("INSERT INTO $T_MENU VALUES (6, 'AKP', 'AKP_FORM')")
            db.execSQL("INSERT INTO $T_MENU VALUES (7, 'MASTER DATA', 'EMPLOYEE_FORM')")
            db.execSQL("INSERT INTO $T_MENU VALUES (8, 'RENCANA KERJA', 'RKH_VIEW')")

            // Isi Master Dropdown RKH (Gunakan $ agar merujuk ke nama tabel yang benar)
            db.execSQL("INSERT INTO $T_AFDELING VALUES ('AFDELING ALPHA'), ('AFDELING BETA')")
            db.execSQL("INSERT INTO $T_GANG VALUES ('GANG 01'), ('GANG 02'), ('GANG 03')")
            db.execSQL("INSERT INTO $T_JOB VALUES ('PANEN'), ('PEMUPUKAN'), ('SEMPROT')")
            db.execSQL("INSERT INTO $T_LOCATION VALUES ('BLOCK A10'), ('BLOCK B12'), ('BLOCK C05')")

            db.execSQL("INSERT INTO $T_TPH (location_code, name) VALUES ('BLOCK A10', 'TPH 01'), ('BLOCK A10', 'TPH 02'), ('BLOCK B12', 'TPH 05')")


            // Isi User Default & Hak Akses
            insertDefaultUsers(db)

            Log.d("DB_CHECK", "--- Database Berhasil Dibuat Lengkap ---")

        } catch (e: Exception) {
            Log.e("DB_ERROR", "Gagal di onCreate: ${e.message}")
        }
    }

    private fun insertDefaultUsers(db: SQLiteDatabase) {
        val users = listOf(
            // Format: Username, Password, EmpCode, Role, FCBA (BA Code)
            arrayOf("admin", "1234", "ADM00", "ADMIN", "99"),
            arrayOf("mandor1", "1234", "MND01", "MANDOR", "41"),
            arrayOf("mandor2", "qwerty", "MND02", "MANDOR", "41"),
            arrayOf("kerani1", "1234", "KRN01", "KERANI", "41"),
            arrayOf("kerani2", "pass123", "KRN02", "KERANI", "41"),
            arrayOf("tester_expired", "1234", "EXP01", "KERANI", "41")
        )

        users.forEach { user ->
            val username = user[0]
            val password = user[1]
            val empCode = user[2]
            val role = user[3]
            val fcba = user[4] // Tambahkan BA Code

            val cvUser = ContentValues().apply {
                put(U_USERNAME, username)
                put(U_PASSWORD, password)
                put(U_EMPCODE, empCode)
                put(U_ROLE, role)
                put(U_FCBA, fcba) // Pastikan FCBA disimpan agar tidak kosong saat login
            }
            db.insertWithOnConflict(T_USERS, null, cvUser, SQLiteDatabase.CONFLICT_REPLACE)

            // Berikan hak akses menu
            when (role) {
                "ADMIN", "MANDOR" -> {
                    for (menuCode in 1..8) {
                        insertAccess(db, empCode, menuCode, isGranted = 1, daysValid = 365)
                    }
                }
                "KERANI" -> {
                    if (username == "tester_expired") {
                        insertAccess(db, empCode, 1, isGranted = 1, daysValid = -1) // Sudah expired
                    } else {
                        listOf(1, 4).forEach { menuCode ->
                            insertAccess(db, empCode, menuCode, isGranted = 1, daysValid = 30)
                        }
                    }
                }
            }
        }

        Log.d("DB_CHECK", "Data Tester dan Hak Akses berhasil dimasukkan.")
    }

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
            db.rawQuery("SELECT * FROM $T_USERS WHERE $U_USERNAME = ? COLLATE NOCASE", arrayOf(u)).use { c ->
                if (c.moveToFirst()) {
                    val dbPass = c.getString(c.getColumnIndexOrThrow(U_PASSWORD))
                    if (dbPass == p) {
                        UserProfile(
                            username = c.getString(c.getColumnIndexOrThrow(U_USERNAME)),
                            empcode = c.getString(c.getColumnIndexOrThrow(U_EMPCODE)) ?: "",
                            fcba = c.getString(c.getColumnIndexOrThrow(U_FCBA)) ?: "",
                            divisi = c.getString(c.getColumnIndexOrThrow(U_DIVISI)) ?: "",
                            gang = c.getString(c.getColumnIndexOrThrow(U_GANG)) ?: "",
                            role = c.getString(c.getColumnIndexOrThrow(U_ROLE)) ?: "MANDOR"
                        )
                    } else null
                } else null
            }
        } catch (e: Exception) {
            Log.e("LOGIN_ERROR", "Login Gagal: ${e.message}")
            null
        }
    }

    // --- FUNGSI SAVE (TANPA DB.CLOSE()) ---

    fun saveWorkProgress(empId: String, category: String, block: String, result: String, notes: String, status: String): Boolean {
        return try {
            val cv = ContentValues().apply {
                put(P_EMP_ID, empId); put(P_CATEGORY, category); put(P_BLOCK, block)
                put(P_RESULT, result); put(P_NOTES, notes); put(P_STATUS, status)
                put(P_TIMESTAMP, SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
            }
            writableDatabase.insert(T_MAINTENANCE, null, cv) != -1L
        } catch (e: Exception) { false }
    }

    fun saveFruitCountingDetailed(
        fcba: String, tanggal: String, rkh: String, gang: String,
        s1: String, s2: String, s3: String, s4: String,
        workers: String, loc: String, unit: Double, output: Double,
        tph: String, beras: Int, lembur: Double
    ): Long {
        return try {
            val db = writableDatabase
            val cv = ContentValues().apply {
                put("fcba", fcba)
                put("tanggal", tanggal)
                put("no_rkh", rkh)
                put("gang_code", gang)
                put("supervisi1", s1)
                put("supervisi2", s2)
                put("supervisi3", s3)
                put("supervisi4", s4)
                put("karyawan_ids", workers)
                put("location_code", loc)
                put("unit", unit)
                put("output", output)
                put("tph_code", tph)
                put("is_beras", beras)
                put("lembur", lembur)
            }
            db.insert(T_FRUIT_COUNTING, null, cv)
        } catch (e: Exception) {
            Log.e("DB_ERROR", "Save Fruit Error: ${e.message}")
            -1L
        }
    }

    fun saveAKP(empId: String, block: String, ripeBunches: Int, density: Double): Boolean {
        return try {
            val cv = ContentValues().apply {
                put(P_EMP_ID, empId); put(P_BLOCK, block)
                put(P_RESULT, "Janjang: $ripeBunches, AKP: ${String.format("%.2f", density)}")
                put(P_CATEGORY, "AKP"); put(P_STATUS, "COMPLETED")
            }
            writableDatabase.insert(T_MAINTENANCE, null, cv) != -1L
        } catch (e: Exception) { false }
    }

    fun saveAncakPanen(empId: String, block: String, quality: String, notes: String): Boolean {
        return try {
            val cv = ContentValues().apply {
                put("emp_id", empId); put("block", block)
                put("quality", quality); put("notes", notes)
                put("date", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
            }
            writableDatabase.insert("table_ancak_panen", null, cv) != -1L
        } catch (e: Exception) { false }
    }

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

    // --- FUNGSI GETTER ---

    fun getWorkProgressHistory(empId: String): List<Map<String, String>> {
        val list = mutableListOf<Map<String, String>>()
        try {
            readableDatabase.rawQuery("SELECT * FROM $T_MAINTENANCE WHERE $P_EMP_ID = ? ORDER BY $P_TIMESTAMP DESC", arrayOf(empId)).use { c ->
                while (c.moveToNext()) {
                    list.add(mapOf(
                        "category" to c.getString(c.getColumnIndexOrThrow(P_CATEGORY)),
                        "block" to c.getString(c.getColumnIndexOrThrow(P_BLOCK)),
                        "result" to c.getString(c.getColumnIndexOrThrow(P_RESULT)),
                        "status" to c.getString(c.getColumnIndexOrThrow(P_STATUS)),
                        "date" to c.getString(c.getColumnIndexOrThrow(P_TIMESTAMP))
                    ))
                }
            }
        } catch (e: Exception) { }
        return list
    }

    // In AttendanceDatabaseHelper.kt


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


fun checkFaceDataStatus(fccode: String): String {
    return try {
        val db = readableDatabase
        db.rawQuery("SELECT FACEEMBEDDING FROM $T_EMP WHERE $E_FCCODE = ?", arrayOf(fccode)).use { c ->
            if (c.moveToFirst()) {
                val data = c.getString(0)
                if (data.isNullOrBlank()) "KOSONG (NULL/BLANK)"
                else "ADA (Panjang Karakter: ${data.length})"
            } else "KARYAWAN TIDAK DITEMUKAN"
        }
    } catch (e: Exception) {
        "ERROR: ${e.message}"
    }
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


    // Helper to convert ByteArray back to FloatArray
    private fun byteArrayToFloatArray(byteArray: ByteArray): FloatArray {
        val buffer = java.nio.ByteBuffer.wrap(byteArray)
        val floatArray = FloatArray(byteArray.size / 4)
        buffer.asFloatBuffer().get(floatArray)
        return floatArray
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

    fun importSqlFromAssets(onProgress: (Int) -> Unit = {}) {
        val db = writableDatabase
        try {
            context.assets.open("EMPLOYEE_202605200755.sql").bufferedReader().use { reader ->
                db.beginTransaction()
                try {
                    val statement = StringBuilder()
                    var count = 0
                    reader.forEachLine { line ->
                        val trimmed = line.trim()
                        if (trimmed.isEmpty() || trimmed.startsWith("--")) return@forEachLine
                        statement.append(trimmed).append(" ")
                        if (trimmed.endsWith(";")) {
                            var sql = statement.toString().trim()
                            // Bersihkan sintaks Oracle
                            sql = sql.replace(Regex("(?i)TIMESTAMP\\s*'"), "'")
                            val clobRegex = Regex("(?i)TO_CLOB\\s*\\(\\s*(.*?)\\s*\\)", RegexOption.DOT_MATCHES_ALL)
                            sql = clobRegex.replace(sql) { it.groupValues[1] }
                            sql = sql.replace("INSERT INTO", "INSERT OR REPLACE INTO", true)

                            db.execSQL(sql)
                            count++
                            if (count % 20 == 0) onProgress(count)
                            statement.setLength(0)
                        }
                    }
                    db.setTransactionSuccessful()
                    Log.d("DB_CHECK", "IMPORT SELESAI: $count data.")
                } finally {
                    db.endTransaction()
                }
            }
        } catch (e: Exception) {
            Log.e("DB_ERROR", "Gagal baca file SQL: ${e.message}")
        }
    }

    // --- FITUR TRANSFER QR CODE ---
    // --- FITUR TRANSFER QR CODE ---
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

    fun getDropdownData(tableName: String): List<String> {
        val list = mutableListOf<String>()
        val db = readableDatabase
        val actualTable = if (tableName == "EMPLOYEE") T_EMP else tableName
        val column = if (tableName == "EMPLOYEE") E_NAME else "name"

        try {
            db.rawQuery("SELECT $column FROM $actualTable", null).use { cursor ->
                while (cursor.moveToNext()) {
                    list.add(cursor.getString(0))
                }
            }
        } catch (e: Exception) { Log.e("DB_ERROR", "getDropdownData: ${e.message}") }
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

    fun saveRKH(fcba: String, afd: String, gang: String, job: String, loc: String, hk: Double, unit: Double, out: Double): Long {
        return try {
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
            db.insert(T_RKH, null, cv)
        } catch (e: Exception) { -1L }
    }

    fun getRKHData(): List<Map<String, String>> {
        val list = mutableListOf<Map<String, String>>()
        val db = readableDatabase
        try {
            db.rawQuery("SELECT * FROM $T_RKH", null).use { cursor ->
                while (cursor.moveToNext()) {
                    list.add(mapOf(
                        "id" to cursor.getString(cursor.getColumnIndexOrThrow("id")),
                        "afd" to cursor.getString(cursor.getColumnIndexOrThrow("afdeling")),
                        "gang" to cursor.getString(cursor.getColumnIndexOrThrow("gangcode")),
                        "loc" to cursor.getString(cursor.getColumnIndexOrThrow("location_code")),
                        "hk" to cursor.getString(cursor.getColumnIndexOrThrow("jumlah_hk"))
                    ))
                }
            }
        } catch (e: Exception) { Log.e("DB_ERROR", "getRKHData: ${e.message}") }
        return list
    }

    fun getRKHDetailForValidation(noRkh: String): Map<String, String>? {
        val db = readableDatabase
        // Pastikan tabel T_RKH Anda punya kolom jumlah_hk
        val query = "SELECT gangcode, location_code, jumlah_hk FROM $T_RKH WHERE no_rkh = ?"
        db.rawQuery(query, arrayOf(noRkh)).use { c ->
            if (c.moveToFirst()) {
                return mapOf(
                    "gang" to (c.getString(0) ?: ""),
                    "location" to (c.getString(1) ?: ""),
                    "max_hk" to (c.getString(2) ?: "0")
                )
            }
        }
        return null
    }

    fun saveMaintenanceDetailed(
        fcba: String, rkh: String, gang: String,
        s1: String, s2: String, s3: String, s4: String,
        workers: String, loc: String, unit: Double,
        output: Double, rate: Double, beras: Int, lembur: Double
    ): Long {
        return try {
            val db = writableDatabase
            val cv = ContentValues().apply {
                put(M_FCBA, fcba)
                put(M_RKH, rkh)
                put(M_GANG, gang)
                put(M_S1, s1)
                put(M_S2, s2)
                put(M_S3, s3)
                put(M_S4, s4)
                put(M_WORKERS, workers)
                put(M_LOCATION, loc)
                put(M_UNIT, unit)
                put(M_OUTPUT, output)
                put(M_RATE, rate)
                put(M_BERAS, beras)
                put(M_LEMBUR, lembur)
            }
            db.insert(T_MAINTENANCE, null, cv)
        } catch (e: Exception) {
            Log.e("DB_ERROR", "Gagal Simpan Maintenance: ${e.message}")
            -1L
        }
    }

    fun savePlantationProgress(
        rkh: String,
        category: String,
        employees: List<String>,
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
    fun getEmployeesForMaintenance(fcba: String): List<Map<String, String>> {
        val list = mutableListOf<Map<String, String>>()
        val db = readableDatabase
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val query = """
        SELECT DISTINCT e.$E_FCCODE, e.$E_NAME 
        FROM $T_EMP e
        INNER JOIN $T_ATT a ON e.$E_FCCODE = a.$A_EMP_ID
        WHERE a.$A_FCBA = ? 
        AND a.$A_TIMESTAMP LIKE '$today%' 
        AND a.$A_ACTION = 'CHECK_IN'
    """.trimIndent()

        db.rawQuery(query, arrayOf(fcba)).use { c ->
            while (c.moveToNext()) {
                list.add(mapOf("id" to c.getString(0), "name" to c.getString(1)))
            }
        }
        return list
    }



    fun getEmployeesByRole(roleName: String): List<Map<String, String>> {
        val list = mutableListOf<Map<String, String>>()
        val db = this.readableDatabase
        // Mengambil dari tabel users sesuai kolom role
        val cursor = db.rawQuery("SELECT $U_EMPCODE, $U_USERNAME FROM $T_USERS WHERE $U_ROLE = ?", arrayOf(roleName))
        if (cursor.moveToFirst()) {
            do {
                list.add(mapOf(
                    "id" to (cursor.getString(0) ?: ""),
                    "name" to (cursor.getString(1) ?: "")
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun getPresentWorkers(fcba: String, date: String): List<Pair<String, String>> {
        val list = mutableListOf<Pair<String, String>>()
        val db = readableDatabase
        try {
            // Kita join tabel attendance dan employee
            val query = """
                SELECT DISTINCT a.$A_EMP_ID, e.$E_NAME 
                FROM $T_ATT a
                JOIN $T_EMP e ON a.$A_EMP_ID = e.$E_FCCODE
                WHERE a.$A_FCBA = ? AND date(a.$A_TIMESTAMP) = date(?)
            """.trimIndent()

            db.rawQuery(query, arrayOf(fcba, date)).use { cursor ->
                while (cursor.moveToNext()) {
                    list.add(cursor.getString(0) to cursor.getString(1))
                }
            }
        } catch (e: Exception) { Log.e("DB_ERROR", "getPresentWorkers: ${e.message}") }
        return list
    }

    fun getTPHByLocation(loc: String): List<String> {
        val list = mutableListOf<String>()
        try {
            readableDatabase.rawQuery("SELECT name FROM $T_TPH WHERE location_code = ?", arrayOf(loc)).use {
                while (it.moveToNext()) list.add(it.getString(0))
            }
        } catch (e: Exception) { Log.e("DB_ERROR", "getTPHByLocation: ${e.message}") }
        return list
    }

    fun saveMaintenance(
        fcba: String, rkh: String, gang: String,
        s1: String, s2: String, s3: String, s4: String,
        workers: String, loc: String, unit: Double,
        output: Double, rate: Double, beras: Int, lembur: Double
    ): Long {
        return try {
            val db = writableDatabase
            val cv = ContentValues().apply {
                put("fcba", fcba); put("no_rkh", rkh); put("gang_code", gang)
                put("supervisi1", s1); put("supervisi2", s2); put("supervisi3", s3); put("supervisi4", s4)
                put("karyawan_ids", workers); put("location_code", loc)
                put("unit", unit); put("output", output); put("rate", rate)
                put("is_beras", beras); put("lembur", lembur)
            }
            db.insert(T_MAINTENANCE, null, cv)
        } catch (e: Exception) { -1L }
    }

    // Di dalam AttendanceDatabaseHelper.kt

    fun getRKHList(): List<Map<String, String>> {
        val list = mutableListOf<Map<String, String>>()
        val db = readableDatabase
        // Pastikan kolom jumlah_hk (atau target_hk) ikut diambil
        val query = "SELECT no_rkh, gangcode, location_code, jumlah_hk FROM table_rkh"

        try {
            db.rawQuery(query, null).use { cursor ->
                while (cursor.moveToNext()) {
                    list.add(mapOf(
                        "no_rkh" to (cursor.getString(0) ?: "-"),
                        "gang_code" to (cursor.getString(1) ?: "-"),
                        "location" to (cursor.getString(2) ?: "-"),
                        "target_hk" to (cursor.getString(3) ?: "0") // KEY INI WAJIB ADA
                    ))
                }
            }
        } catch (e: Exception) {
            Log.e("DB_ERROR", "Error getRKHList: ${e.message}")
        }
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

    // Fungsi untuk mengambil menu yang diizinkan untuk ID Karyawan tertentu
    // --- FUNGSI HAK AKSES MENU (FIX EXPIRED) ---
    fun getAllowedMenusForUser(empId: String): List<String> {
        val routes = mutableListOf<String>()
        val db = this.readableDatabase
        val currentTime = System.currentTimeMillis()
        try {
            val query = """
                SELECT m.ROUTE FROM $T_MENU m
                JOIN $T_MENU_ACCESS ma ON m.CODE = ma.MENU_CODE
                WHERE ma.EMP_ID = ? AND ma.IS_GRANTED = 1 
                AND (ma.VALID_UNTIL IS NULL OR ma.VALID_UNTIL > ?)
            """.trimIndent()
            db.rawQuery(query, arrayOf(empId, currentTime.toString())).use { cursor ->
                if (cursor.moveToFirst()) {
                    do { routes.add(cursor.getString(0)) } while (cursor.moveToNext())
                }
            }
        } catch (e: Exception) { Log.e("DB_ERROR", "Error getAllowedMenus: ${e.message}") }
        return routes
    }
} // Penutup kelas AttendanceDatabaseHelper