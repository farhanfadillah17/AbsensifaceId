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
        const val DATABASE_NAME = "attendance_reset_final_v219.db"
        const val DATABASE_VERSION = 219

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

        const val T_PROGRESS = "work_progress"
        const val P_ID = "id"
        const val P_EMP_ID = "emp_id"
        const val P_CATEGORY = "category"
        const val P_BLOCK = "block_code"
        const val P_RESULT = "work_result"
        const val P_NOTES = "notes"
        const val P_STATUS = "status"
        const val P_TIMESTAMP = "timestamp"

        const val T_MENU = "T_MENU"
        const val T_MENU_ACCESS = "T_MENU_ACCESS"
        }


    override fun onCreate(db: SQLiteDatabase) {
        try {
            Log.d("DB_CHECK", "--- Memulai Pembuatan Database v201 ---")

            // 1. Tabel Users
            db.execSQL("CREATE TABLE IF NOT EXISTS $T_USERS ($U_USERNAME TEXT PRIMARY KEY COLLATE NOCASE, $U_PASSWORD TEXT, $U_EMPCODE TEXT, $U_FCBA TEXT, $U_DIVISI TEXT, $U_GANG TEXT, $U_ROLE TEXT)")
            // 1. Buat Tabel Menu
            db.execSQL("CREATE TABLE T_MENU (CODE INTEGER PRIMARY KEY, NAME TEXT, ROUTE TEXT)")

            // 2. Buat Tabel Akses
            // Pastikan skema tabelnya seperti ini di onCreate
            db.execSQL("""
    CREATE TABLE T_MENU_ACCESS (
        ID INTEGER PRIMARY KEY AUTOINCREMENT,
        EMP_ID TEXT,
        MENU_CODE INTEGER,
        IS_GRANTED INTEGER DEFAULT 1,
        VALID_UNTIL LONG -- Pastikan kolom ini ada
    )
""")

            // 3. ISI DATA MENU (Wajib sama dengan 'route' di MainActivity)
            db.execSQL("INSERT INTO T_MENU VALUES (1, 'ABSENSI', 'HOME')")
            db.execSQL("INSERT INTO T_MENU VALUES (2, 'PROGRESS KERJA', 'PROGRESS_MENU')")
            db.execSQL("INSERT INTO T_MENU VALUES (3, 'PERHITUNGAN BUAH', 'FRUIT_COUNTING')")
            db.execSQL("INSERT INTO T_MENU VALUES (4, 'PEMBUATAN SPB', 'SPB_MENU')")
            db.execSQL("INSERT INTO T_MENU VALUES (5, 'ANCAK PANEN', 'ANCAK_PANEN')")
            db.execSQL("INSERT INTO T_MENU VALUES (6, 'AKP', 'AKP_FORM')")
            db.execSQL("INSERT INTO T_MENU VALUES (7, 'MASTER DATA', 'EMPLOYEE_FORM')")

            // 4. BERI AKSES DEFAULT (Contoh untuk user dengan ID '2024001')
            // Admin/Mandor biasanya dapat semua (1-7), Kerani mungkin hanya (1, 4)
            db.execSQL("INSERT INTO T_MENU_ACCESS (EMP_ID, MENU_CODE, IS_GRANTED) VALUES ('2024001', 1, 1)")
            db.execSQL("INSERT INTO T_MENU_ACCESS (EMP_ID, MENU_CODE, IS_GRANTED) VALUES ('2024001', 3, 1)")

            // 2. Tabel Employee (Disesuaikan dengan kolom SQL)
            // Agar aman, kita buat kolom yang disebutkan dalam error Logcat
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

            // 3. Tabel Lainnya (Sama seperti sebelumnya)
            db.execSQL("CREATE TABLE IF NOT EXISTS $T_ATT ($A_ID INTEGER PRIMARY KEY AUTOINCREMENT, $A_EMP_ID TEXT NOT NULL, $A_FCBA TEXT NOT NULL, $A_EMP_NAME TEXT NOT NULL, $A_ACTION TEXT NOT NULL, $A_TIMESTAMP DATETIME DEFAULT CURRENT_TIMESTAMP, $A_SOURCE TEXT DEFAULT 'INPUT')")
            db.execSQL("CREATE TABLE IF NOT EXISTS $T_FACE ($F_ID INTEGER PRIMARY KEY AUTOINCREMENT, $F_EMP_ID TEXT NOT NULL, $F_FCBA TEXT NOT NULL, $F_FEATURES TEXT NOT NULL, $F_CAPTURED DATETIME DEFAULT CURRENT_TIMESTAMP)")
            db.execSQL("CREATE TABLE IF NOT EXISTS $T_PROGRESS ($P_ID INTEGER PRIMARY KEY AUTOINCREMENT, $P_EMP_ID TEXT, $P_CATEGORY TEXT, $P_BLOCK TEXT, $P_RESULT TEXT, $P_NOTES TEXT, $P_STATUS TEXT, $P_TIMESTAMP DATETIME DEFAULT CURRENT_TIMESTAMP)")

            insertDefaultUsers(db)

        } catch (e: Exception) {
            Log.e("DB_ERROR", "Gagal di onCreate: ${e.message}")
        }
    }

    private fun insertDefaultUsers(db: SQLiteDatabase) {
        // 1. Daftar Akun Testing dengan Role Berbeda
        val users = listOf(
            arrayOf("admin", "1234", "ADM00", "ADMIN"),       // Full Access
            arrayOf("mandor1", "1234", "MND01", "MANDOR"),    // Full Access
            arrayOf("kerani1", "1234", "KRN01", "KERANI"),    // Akses Terbatas (Hanya Absensi & SPB)
            arrayOf("tester_expired", "1234", "EXP01", "KERANI") // Akses Kadaluarsa
        )

        users.forEach { user ->
            val username = user[0]
            val password = user[1]
            val empCode = user[2]
            val role = user[3]

            // A. Simpan ke Tabel Users
            val cvUser = ContentValues().apply {
                put(U_USERNAME, username)
                put(U_PASSWORD, password)
                put(U_EMPCODE, empCode)
                put(U_ROLE, role)
            }
            db.insertWithOnConflict(T_USERS, null, cvUser, SQLiteDatabase.CONFLICT_REPLACE)

            // B. Berikan Hak Akses Berdasarkan Skenario Testing
            when (role) {
                "ADMIN", "MANDOR" -> {
                    // Skenario 1: Beri akses SEMUA MENU (1-7)
                    for (menuCode in 1..7) {
                        insertAccess(db, empCode, menuCode, isGranted = 1, daysValid = 365)
                    }
                }
                "KERANI" -> {
                    if (username == "tester_expired") {
                        // Skenario 2: Akses diberikan tapi sudah KADALUARSA (untuk tes validUntil)
                        insertAccess(db, empCode, 1, isGranted = 1, daysValid = -1) // -1 hari (kemarin)
                    } else {
                        // Skenario 3: Kerani Normal hanya boleh menu 1 (ABSENSI) & 4 (SPB)
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
        db.execSQL("DROP TABLE IF EXISTS $T_PROGRESS")
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
            writableDatabase.insert(T_PROGRESS, null, cv) != -1L
        } catch (e: Exception) { false }
    }

    fun saveFruitCounting(empId: String, qty: Int, location: String): Boolean {
        return try {
            val cv = ContentValues().apply {
                put("emp_id", empId); put("quantity", qty)
                put("location", location); put("timestamp", System.currentTimeMillis())
            }
            writableDatabase.insert("fruit_counting", null, cv) != -1L
        } catch (e: Exception) { false }
    }

    fun saveAKP(empId: String, block: String, ripeBunches: Int, density: Double): Boolean {
        return try {
            val cv = ContentValues().apply {
                put(P_EMP_ID, empId); put(P_BLOCK, block)
                put(P_RESULT, "Janjang: $ripeBunches, AKP: ${String.format("%.2f", density)}")
                put(P_CATEGORY, "AKP"); put(P_STATUS, "COMPLETED")
            }
            writableDatabase.insert(T_PROGRESS, null, cv) != -1L
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
            readableDatabase.rawQuery("SELECT * FROM $T_PROGRESS WHERE $P_EMP_ID = ? ORDER BY $P_TIMESTAMP DESC", arrayOf(empId)).use { c ->
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
    fun getAttendanceForQRCode(): String {
        val sb = StringBuilder()
        try {
            readableDatabase.rawQuery(
                "SELECT * FROM $T_ATT WHERE date($A_TIMESTAMP) = date('now')",
                null
            ).use { c ->
                while (c.moveToNext()) {
                    sb.append("${c.getString(c.getColumnIndexOrThrow(A_EMP_ID))},")
                    sb.append("${c.getString(c.getColumnIndexOrThrow(A_FCBA))},")
                    sb.append("${c.getString(c.getColumnIndexOrThrow(A_ACTION))};")
                }
            }
        } catch (e: Exception) { }
        return sb.toString()
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

    // Fungsi untuk mengambil menu yang diizinkan untuk ID Karyawan tertentu
    // --- FUNGSI HAK AKSES MENU (FIX EXPIRED) ---
    fun getAllowedMenusForUser(empId: String): List<String> {
        val routes = mutableListOf<String>()
        val db = this.readableDatabase
        val currentTime = System.currentTimeMillis() // Waktu sekarang untuk cek kadaluarsa

        try {
            // Query ini mengecek 3 hal:
            // 1. IS_GRANTED = 1 (Akses diizinkan)
            // 2. EMP_ID cocok dengan user yang login
            // 3. VALID_UNTIL > Waktu sekarang (Belum kadaluarsa)
            val query = """
            SELECT m.ROUTE FROM $T_MENU m
            JOIN $T_MENU_ACCESS ma ON m.CODE = ma.MENU_CODE
            WHERE ma.EMP_ID = ? 
            AND ma.IS_GRANTED = 1 
            AND (ma.VALID_UNTIL IS NULL OR ma.VALID_UNTIL > ?)
        """.trimIndent()

            db.rawQuery(query, arrayOf(empId, currentTime.toString())).use { cursor ->
                if (cursor.moveToFirst()) {
                    do {
                        routes.add(cursor.getString(0))
                    } while (cursor.moveToNext())
                }
            }
        } catch (e: Exception) {
            Log.e("DB_ERROR", "Error getAllowedMenus: ${e.message}")
        }
        return routes
    }
} // Penutup kelas AttendanceDatabaseHelper