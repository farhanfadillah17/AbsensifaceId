package com.example.attendanceapp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import androidx.compose.ui.semantics.role
import kotlin.text.uppercase

// --- DATA CLASSES ---
data class Employee(
    val fccode: String,
    val fcba: String,
    val name: String,
    val sectionName: String,
    val gangCode: String,
    val position: String,
    val gender: String = "",
    val address: String = "",
    val companyNik: String = "",
    val faceEmbedding: String? = null,
    val lastUpdate: Long = System.currentTimeMillis()
)

data class FaceMapping(
    val id: Int = 0,
    val employeeId: String,
    val fcbaId: String,
    val features: FloatArray,
    val capturedAt: Long = System.currentTimeMillis()
)

data class AttendanceRecord(
    val id: Int,
    val employeeId: String,
    val fcbaId: String,
    val employeeName: String,
    val action: String,
    val timestamp: String
)

data class UserProfile(
    val username: String,
    val empcode: String,
    val fcba: String,
    val divisi: String,
    val gang: String,
    val role: String
)

class AttendanceDatabaseHelper(private val context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "attendance.db"
        const val DATABASE_VERSION = 71 // NAIKKAN LAGI

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

        const val T_FACE = "face_mappings"
        const val F_ID = "id"
        const val F_EMP_ID = "employee_id"
        const val F_FCBA = "fcba_id"
        const val F_FEATURES = "features"
        const val F_CAPTURED = "captured_at"

        const val T_ATT = "attendance"
        const val A_ID = "id"
        const val A_EMP_ID = "employee_id"
        const val A_FCBA = "fcba_id"
        const val A_EMP_NAME = "employee_name"
        const val A_ACTION = "action"
        const val A_TIMESTAMP = "timestamp"

        const val T_PROGRESS = "work_progress"
        const val P_ID = "id"
        const val P_EMP_ID = "emp_id"
        const val P_CATEGORY = "category"
        const val P_BLOCK = "block_code"
        const val P_RESULT = "work_result"
        const val P_NOTES = "notes"
        const val P_STATUS = "status"
        const val P_TIMESTAMP = "timestamp"

        const val T_FRUIT = "fruit_counting"
        const val FR_ID = "id"
        const val FR_EMP_ID = "emp_id"
        const val FR_QTY = "quantity"
        const val FR_LOCATION = "location"
        const val FR_TIMESTAMP = "timestamp"

        const val T_HARVEST_QUAL = "HARVESTING_QUALITY"
        const val HQ_ID = "id"
        const val HQ_EMP_ID = "emp_id"
        const val HQ_BLOCK = "block_code"
        const val HQ_LOOSE_FRUIT = "loose_fruit_score"
        const val HQ_NOTES = "notes"
        const val HQ_TIMESTAMP = "timestamp"

        const val T_SPB = "SPB_MAKING"
        const val S_ID = "id"
        const val S_EMP_ID = "emp_id"
        const val S_TYPE = "transport_type"
        const val S_VEHICLE = "vehicle_number"
        const val S_STAMP = "timestamp"

        const val T_AKP = "AKP_DATA"
        const val AKP_ID = "id"
        const val AKP_EMP = "emp_id"
        const val AKP_BLOCK = "block_code"
        const val AKP_BUNCHES = "bunches_count"
        const val AKP_DENSITY = "density_result"
        const val AKP_STAMP = "timestamp"

        const val T_RKH = "RKH_DATA"
        const val RKH_ID = "id"
        const val RKH_EMP = "emp_id"
        const val RKH_ACT = "activity_name"
        const val RKH_LOC = "location"
        const val RKH_TARGET = "target_qty"
        const val RKH_STAMP = "timestamp"
    }

    override fun onCreate(db: SQLiteDatabase) {
        try {
            db.execSQL("CREATE TABLE $T_EMP ($E_FCCODE TEXT NOT NULL, $E_NAME TEXT, $E_SECTION TEXT, $E_GANG TEXT, $E_POSITION TEXT, $E_FCBA TEXT NOT NULL, $E_NIK TEXT, $E_FACE_EMB TEXT, $E_LAST_UPDATE TEXT, PRIMARY KEY ($E_FCCODE, $E_FCBA))")
            db.execSQL("CREATE TABLE $T_USERS ($U_USERNAME TEXT PRIMARY KEY COLLATE NOCASE, $U_PASSWORD TEXT, $U_EMPCODE TEXT, $U_FCBA TEXT, $U_DIVISI TEXT, $U_GANG TEXT, $U_ROLE TEXT)")
            db.execSQL("CREATE TABLE $T_FACE ($F_ID INTEGER PRIMARY KEY AUTOINCREMENT, $F_EMP_ID TEXT NOT NULL, $F_FCBA TEXT NOT NULL, $F_FEATURES TEXT NOT NULL, $F_CAPTURED DATETIME DEFAULT CURRENT_TIMESTAMP)")
            db.execSQL("CREATE TABLE $T_ATT ($A_ID INTEGER PRIMARY KEY AUTOINCREMENT, $A_EMP_ID TEXT NOT NULL, $A_FCBA TEXT NOT NULL, $A_EMP_NAME TEXT NOT NULL, $A_ACTION TEXT NOT NULL, $A_TIMESTAMP DATETIME DEFAULT CURRENT_TIMESTAMP)")
            db.execSQL("CREATE TABLE $T_PROGRESS ($P_ID INTEGER PRIMARY KEY AUTOINCREMENT, $P_EMP_ID TEXT, $P_CATEGORY TEXT, $P_BLOCK TEXT, $P_RESULT TEXT, $P_NOTES TEXT, $P_STATUS TEXT, $P_TIMESTAMP DATETIME DEFAULT CURRENT_TIMESTAMP)")
            db.execSQL("CREATE TABLE $T_FRUIT ($FR_ID INTEGER PRIMARY KEY AUTOINCREMENT, $FR_EMP_ID TEXT, $FR_QTY INTEGER, $FR_LOCATION TEXT, $FR_TIMESTAMP DATETIME DEFAULT CURRENT_TIMESTAMP)")
            db.execSQL("CREATE TABLE $T_HARVEST_QUAL ($HQ_ID INTEGER PRIMARY KEY AUTOINCREMENT, $HQ_EMP_ID TEXT, $HQ_BLOCK TEXT, $HQ_LOOSE_FRUIT TEXT, $HQ_NOTES TEXT, $HQ_TIMESTAMP DATETIME DEFAULT CURRENT_TIMESTAMP)")
            db.execSQL("CREATE TABLE $T_SPB ($S_ID INTEGER PRIMARY KEY AUTOINCREMENT, $S_EMP_ID TEXT, $S_TYPE TEXT, $S_VEHICLE TEXT, $S_STAMP DATETIME DEFAULT CURRENT_TIMESTAMP)")
            db.execSQL("CREATE TABLE $T_AKP ($AKP_ID INTEGER PRIMARY KEY AUTOINCREMENT, $AKP_EMP TEXT, $AKP_BLOCK TEXT, $AKP_BUNCHES INTEGER, $AKP_DENSITY REAL, $AKP_STAMP DATETIME DEFAULT CURRENT_TIMESTAMP)")
            db.execSQL("CREATE TABLE $T_RKH ($RKH_ID INTEGER PRIMARY KEY AUTOINCREMENT, $RKH_EMP TEXT, $RKH_ACT TEXT, $RKH_LOC TEXT, $RKH_TARGET TEXT, $RKH_STAMP DATETIME DEFAULT CURRENT_TIMESTAMP)")

            importSqlFromAssets(db, context)
            forceInsertAdmin(db)
        } catch (e: Exception) {
            Log.e("DB_ERROR", "Error onCreate: ${e.message}")
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $T_USERS")
        db.execSQL("DROP TABLE IF EXISTS $T_ATT")
        db.execSQL("DROP TABLE IF EXISTS $T_FACE")
        db.execSQL("DROP TABLE IF EXISTS $T_EMP")
        db.execSQL("DROP TABLE IF EXISTS $T_PROGRESS")
        db.execSQL("DROP TABLE IF EXISTS $T_FRUIT")
        db.execSQL("DROP TABLE IF EXISTS $T_HARVEST_QUAL")
        db.execSQL("DROP TABLE IF EXISTS $T_SPB")
        db.execSQL("DROP TABLE IF EXISTS $T_AKP")
        db.execSQL("DROP TABLE IF EXISTS $T_RKH")
        onCreate(db)
    }

    // --- FUNGSI LOGIN ---
    fun checkLogin(username: String, pass: String): UserProfile? {
        val u = username.trim()
        val p = pass.trim()
        if (u.equals("admin", ignoreCase = true) && p == "1234") {
            return UserProfile("admin", "ADM001", "BA01", "IT", "G01", "ADMIN")
        }
        val db = readableDatabase
        return try {
            val query = "SELECT * FROM $T_USERS WHERE $U_USERNAME = ? COLLATE NOCASE"
            db.rawQuery(query, arrayOf(u)).use { c ->
                if (c.moveToFirst()) {
                    if (c.getString(c.getColumnIndexOrThrow(U_PASSWORD)) == p) {
                        return UserProfile(
                            username = c.getString(c.getColumnIndexOrThrow(U_USERNAME)),
                            empcode = c.getString(c.getColumnIndexOrThrow(U_EMPCODE)) ?: "",
                            fcba = c.getString(c.getColumnIndexOrThrow(U_FCBA)) ?: "",
                            divisi = c.getString(c.getColumnIndexOrThrow(U_DIVISI)) ?: "",
                            gang = c.getString(c.getColumnIndexOrThrow(U_GANG)) ?: "",
                            role = c.getString(c.getColumnIndexOrThrow(U_ROLE)) ?: "MANDOR"
                        )
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    // --- FUNGSI PROGRESS ---
    fun saveWorkProgress(empId: String, category: String, block: String, result: String, notes: String, status: String): Boolean {
        val db = this.writableDatabase
        return try {
            val cv = ContentValues().apply {
                put(P_EMP_ID, empId)
                put(P_CATEGORY, category)
                put(P_BLOCK, block)
                put(P_RESULT, result)
                put(P_NOTES, notes)
                put(P_STATUS, status)
            }
            // Gunakan insertOrThrow untuk menangkap pesan error spesifik dari SQLite
            val resultId = db.insertOrThrow(T_PROGRESS, null, cv)
            Log.d("DB_SAVE", "Berhasil simpan progress. ID: $resultId")
            true
        } catch (e: android.database.sqlite.SQLiteException) {
            // Log ini akan memunculkan alasan asli (misal: tabel tidak ada atau kolom salah)
            Log.e("DB_SAVE", "SQLite Error: ${e.message}")
            false
        } catch (e: Exception) {
            Log.e("DB_SAVE", "General Error: ${e.message}")
            false
        }
    }

    fun getWorkProgressHistory(empId: String): List<Map<String, String>> {
        val list = mutableListOf<Map<String, String>>()
        try {
            val db = readableDatabase
            db.rawQuery(
                "SELECT * FROM $T_PROGRESS WHERE $P_EMP_ID = ? ORDER BY $P_TIMESTAMP DESC",
                arrayOf(empId)
            ).use { c ->
                while (c.moveToNext()) {
                    list.add(
                        mapOf(
                            "category" to (c.getString(c.getColumnIndexOrThrow(P_CATEGORY)) ?: ""),
                            "block" to (c.getString(c.getColumnIndexOrThrow(P_BLOCK)) ?: ""),
                            "result" to (c.getString(c.getColumnIndexOrThrow(P_RESULT)) ?: ""),
                            "status" to (c.getString(c.getColumnIndexOrThrow(P_STATUS)) ?: ""),
                            "notes" to (c.getString(c.getColumnIndexOrThrow(P_NOTES)) ?: ""),
                            "date" to (c.getString(c.getColumnIndexOrThrow(P_TIMESTAMP)) ?: "")
                        )
                    )
                }
            }
        } catch (e: Exception) {
        }
        return list
    }

    fun getAllAttendance(): List<AttendanceRecord> {
        val attendanceList = mutableListOf<AttendanceRecord>()
        val db = this.readableDatabase
        val selectQuery = "SELECT * FROM $T_ATT ORDER BY $A_ID DESC"
        db.rawQuery(selectQuery, null).use { cursor ->
            if (cursor.moveToFirst()) {
                do {
                    attendanceList.add(
                        AttendanceRecord(
                            id = cursor.getInt(cursor.getColumnIndexOrThrow(A_ID)),
                            employeeId = cursor.getString(cursor.getColumnIndexOrThrow(A_EMP_ID)),
                            fcbaId = cursor.getString(cursor.getColumnIndexOrThrow(A_FCBA)),
                            employeeName = cursor.getString(cursor.getColumnIndexOrThrow(A_EMP_NAME)),
                            timestamp = cursor.getString(cursor.getColumnIndexOrThrow(A_TIMESTAMP)),
                            action = cursor.getString(cursor.getColumnIndexOrThrow(A_ACTION))
                        )
                    )
                } while (cursor.moveToNext())
            }
        }
        return attendanceList
    }

    fun getEmployeeByOnlyCode(scannedId: String): Employee? {
        val db = this.readableDatabase
        val cursor = db.query(
            T_EMP,
            null,
            "$E_FCCODE = ?",
            arrayOf(scannedId),
            null, null, null
        )

        return cursor.use { c ->
            if (c.moveToFirst()) {
                Employee(
                    fccode = c.getString(c.getColumnIndexOrThrow(E_FCCODE)),
                    fcba = c.getString(c.getColumnIndexOrThrow(E_FCBA)),
                    name = c.getString(c.getColumnIndexOrThrow(E_NAME)),
                    sectionName = c.getString(c.getColumnIndexOrThrow(E_SECTION)) ?: "",
                    gangCode = c.getString(c.getColumnIndexOrThrow(E_GANG)) ?: "",
                    position = c.getString(c.getColumnIndexOrThrow(E_POSITION)) ?: "",
                    companyNik = c.getString(c.getColumnIndexOrThrow(E_NIK)) ?: "",
                    faceEmbedding = c.getString(c.getColumnIndexOrThrow(E_FACE_EMB)),
                    lastUpdate = try {
                        c.getLong(c.getColumnIndexOrThrow(E_LAST_UPDATE))
                    } catch (e: Exception) {
                        0L
                    }
                )
            } else null
        }
    }

    fun getAllMasterEmployees(): List<Employee> {
        val employeeList = mutableListOf<Employee>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $T_EMP ORDER BY $E_NAME ASC", null)

        cursor.use { c ->
            if (c.moveToFirst()) {
                do {
                    employeeList.add(
                        Employee(
                            fccode = c.getString(c.getColumnIndexOrThrow(E_FCCODE)),
                            fcba = c.getString(c.getColumnIndexOrThrow(E_FCBA)),
                            name = c.getString(c.getColumnIndexOrThrow(E_NAME)),
                            sectionName = c.getString(c.getColumnIndexOrThrow(E_SECTION)) ?: "",
                            gangCode = c.getString(c.getColumnIndexOrThrow(E_GANG)) ?: "",
                            position = c.getString(c.getColumnIndexOrThrow(E_POSITION)) ?: "",
                            companyNik = c.getString(c.getColumnIndexOrThrow(E_NIK)) ?: "",
                            faceEmbedding = c.getString(c.getColumnIndexOrThrow(E_FACE_EMB)),
                            lastUpdate = try {
                                c.getLong(c.getColumnIndexOrThrow(E_LAST_UPDATE))
                            } catch (e: Exception) {
                                0L
                            }
                        )
                    )
                } while (c.moveToNext())
            }
        }
        return employeeList
    }

    fun insertFaceMapping(mapping: FaceMapping): Boolean {
        return try {
            val db = this.writableDatabase
            val values = ContentValues().apply {
                put(F_EMP_ID, mapping.employeeId)
                put(F_FCBA, mapping.fcbaId)
                put(F_FEATURES, mapping.features.joinToString(","))
            }
            db.insert(T_FACE, null, values) != -1L
        } catch (e: Exception) {
            false
        }
    }

    fun getFaceMappingCount(fccode: String, fcba: String): Int {
        val db = this.readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM $T_FACE WHERE $F_EMP_ID = ? AND $F_FCBA = ?",
            arrayOf(fccode, fcba)
        )
        return cursor.use { if (it.moveToFirst()) it.getInt(0) else 0 }
    }

    fun getFaceFeaturesForEmployee(fccode: String, fcba: String): List<FloatArray> {
        val featureList = mutableListOf<FloatArray>()
        val db = this.readableDatabase
        val query = "SELECT $F_FEATURES FROM $T_FACE WHERE $F_EMP_ID = ? AND $F_FCBA = ?"
        db.rawQuery(query, arrayOf(fccode, fcba)).use { cursor ->
            while (cursor.moveToNext()) {
                val featureString = cursor.getString(0)
                if (!featureString.isNullOrEmpty()) {
                    featureList.add(stringToFloatArray(featureString))
                }
            }
        }
        return featureList
    }

    private fun stringToFloatArray(str: String): FloatArray {
        if (str.isEmpty()) return floatArrayOf()
        return try {
            str.split(",").map { it.toFloat() }.toFloatArray()
        } catch (e: Exception) {
            floatArrayOf()
        }
    }

    fun saveAttendance(fccode: String, fcba: String, action: String): Boolean {
        return try {
            val db = writableDatabase
            var empName = "Unknown"
            val queryName = "SELECT $E_NAME FROM $T_EMP WHERE $E_FCCODE = ? AND $E_FCBA = ?"
            db.rawQuery(queryName, arrayOf(fccode, fcba)).use { cursor ->
                if (cursor.moveToFirst()) empName = cursor.getString(0) ?: "Unknown"
            }
            val cv = ContentValues().apply {
                put(A_EMP_ID, fccode)
                put(A_FCBA, fcba)
                put(A_EMP_NAME, empName)
                put(A_ACTION, action)
            }
            db.insert(T_ATT, null, cv) != -1L
        } catch (e: Exception) {
            Log.e("DB_ERROR", "Gagal simpan absensi: ${e.message}")
            false
        }
    }

    fun addUser(user: UserProfile, password: String): Boolean {
        return try {
            val db = writableDatabase
            val cv = ContentValues().apply {
                put(U_USERNAME, user.username)
                put(U_PASSWORD, password)
                put(U_EMPCODE, user.empcode)
                put(U_FCBA, user.fcba)
                put(U_DIVISI, user.divisi)
                put(U_GANG, user.gang)
                put(U_ROLE, user.role.uppercase())
            }
            db.insertWithOnConflict(T_USERS, null, cv, SQLiteDatabase.CONFLICT_REPLACE) != -1L
        } catch (e: Exception) {
            Log.e("DB_ERROR", "Gagal add user: ${e.message}")
            false
        }
    }

    // --- FUNGSI MANAJEMEN ROLE & USER ---

    /**
     * Mengambil semua daftar user untuk ditampilkan di menu Admin
     */
    fun getAllUsers(): List<UserProfile> {
        val userList = mutableListOf<UserProfile>()
        val db = readableDatabase
        try {
            db.rawQuery("SELECT * FROM $T_USERS", null).use { c ->
                while (c.moveToNext()) {
                    userList.add(
                        UserProfile(
                            username = c.getString(c.getColumnIndexOrThrow(U_USERNAME)),
                            empcode = c.getString(c.getColumnIndexOrThrow(U_EMPCODE)) ?: "",
                            fcba = c.getString(c.getColumnIndexOrThrow(U_FCBA)) ?: "",
                            divisi = c.getString(c.getColumnIndexOrThrow(U_DIVISI)) ?: "",
                            gang = c.getString(c.getColumnIndexOrThrow(U_GANG)) ?: "",
                            role = c.getString(c.getColumnIndexOrThrow(U_ROLE)) ?: "MANDOR"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("DB_ERROR", "Gagal ambil daftar user: ${e.message}")
        }
        return userList
    }

    /**
     * Memperbarui Role user (Misal: Mengubah MANDOR menjadi ADMIN)
     */
    fun updateUserRole(username: String, newRole: String): Boolean {
        return try {
            val db = writableDatabase
            val cv = ContentValues().apply {
                put(U_ROLE, newRole.uppercase())
            }
            db.update(T_USERS, cv, "$U_USERNAME = ?", arrayOf(username)) > 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Menghapus user (Hanya bisa dilakukan oleh ADMIN)
     */
    fun deleteUser(username: String): Boolean {
        if (username.lowercase() == "admin") return false // Admin utama tidak bisa dihapus
        return try {
            val db = writableDatabase
            db.delete(T_USERS, "$U_USERNAME = ?", arrayOf(username)) > 0
        } catch (e: Exception) {
            false
        }
    }

    fun saveFruitCounting(empId: String, qty: Int, location: String): Boolean {
        return try {
            val cv = ContentValues().apply {
                put(FR_EMP_ID, empId)
                put(FR_QTY, qty)
                put(FR_LOCATION, location)
            }
            writableDatabase.insert(T_FRUIT, null, cv) != -1L
        } catch (e: Exception) { false }
    }

    fun saveAncakPanen(empId: String, block: String, quality: String, notes: String): Boolean {
        return try {
            val cv = ContentValues().apply {
                put(HQ_EMP_ID, empId)
                put(HQ_BLOCK, block)
                put(HQ_LOOSE_FRUIT, quality)
                put(HQ_NOTES, notes)
            }
            writableDatabase.insert(T_HARVEST_QUAL, null, cv) != -1L
        } catch (e: Exception) { false }
    }

    fun saveSPB(empId: String, type: String, vehicle: String): Boolean {
        return try {
            val cv = ContentValues().apply {
                put(S_EMP_ID, empId)
                put(S_TYPE, type)
                put(S_VEHICLE, vehicle)
            }
            writableDatabase.insert(T_SPB, null, cv) != -1L
        } catch (e: Exception) { false }
    }

    fun saveAKP(empId: String, block: String, bunches: Int, density: Double): Boolean {
        return try {
            val cv = ContentValues().apply {
                put(AKP_EMP, empId)
                put(AKP_BLOCK, block)
                put(AKP_BUNCHES, bunches)
                put(AKP_DENSITY, density)
            }
            writableDatabase.insert(T_AKP, null, cv) != -1L
        } catch (e: Exception) { false }
    }

    private fun forceInsertAdmin(db: SQLiteDatabase) {
        val cv = ContentValues().apply {
            put(U_USERNAME, "admin")
            put(U_PASSWORD, "1234")
            put(U_ROLE, "ADMIN")
            put(U_EMPCODE, "ADM001")
            put(U_FCBA, "BA01")
        }
        db.replace(T_USERS, null, cv)
    }

    private fun importSqlFromAssets(db: SQLiteDatabase, context: Context) {
        try {
            val fileName = "EMPLOYEE_202605200755.sql"
            val inputStream = context.assets.open(fileName)
            val reader = inputStream.bufferedReader()
            val statement = StringBuilder()

            db.beginTransaction()
            try {
                reader.forEachLine { line ->
                    val trimmedLine = line.trim()
                    if (trimmedLine.isNotEmpty() && !trimmedLine.startsWith("--")) {
                        statement.append(trimmedLine)
                        if (trimmedLine.endsWith(";")) {
                            db.execSQL(statement.toString().replace("INSERT INTO", "INSERT OR REPLACE INTO", true))
                            statement.setLength(0)
                        }
                    }
                }
                db.setTransactionSuccessful()
                Log.d("DB_IMPORT", "Import Berhasil: $fileName")
            } finally {
                db.endTransaction()
                reader.close()
            }
        } catch (e: Exception) {
            Log.e("DB_ERROR", "Gagal import SQL: ${e.message}")
        }
    }
}



