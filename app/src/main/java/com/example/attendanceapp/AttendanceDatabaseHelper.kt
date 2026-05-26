package com.example.attendanceapp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

// --- DATA CLASSES ---
data class Employee(val fccode: String, val fcba: String, val name: String, val sectionName: String, val gangCode: String, val position: String, val gender: String = "", val address: String = "", val companyNik: String = "", val faceEmbedding: String? = null, val lastUpdate: Long = System.currentTimeMillis())
data class FaceMapping(val id: Int = 0, val employeeId: String, val fcbaId: String, val features: FloatArray, val capturedAt: Long = System.currentTimeMillis())
data class AttendanceRecord(val id: Int, val employeeId: String, val fcbaId: String, val employeeName: String, val action: String, val timestamp: String)
data class UserProfile(val username: String, val empcode: String, val fcba: String, val divisi: String, val gang: String)

class AttendanceDatabaseHelper(private val context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "attendance.db"
        const val DATABASE_VERSION = 45 // NAIKKAN KE 45

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
    }

    override fun onCreate(db: SQLiteDatabase) {
        try {
            // 1. Buat Tabel
            db.execSQL("CREATE TABLE $T_EMP ($E_FCCODE TEXT NOT NULL, $E_NAME TEXT, $E_SECTION TEXT, $E_GANG TEXT, $E_POSITION TEXT, $E_FCBA TEXT NOT NULL, $E_NIK TEXT, $E_FACE_EMB TEXT, $E_LAST_UPDATE TEXT, PRIMARY KEY ($E_FCCODE, $E_FCBA))")
            db.execSQL("CREATE TABLE $T_USERS ($U_USERNAME TEXT PRIMARY KEY COLLATE NOCASE, $U_PASSWORD TEXT, $U_EMPCODE TEXT, $U_FCBA TEXT, $U_DIVISI TEXT, $U_GANG TEXT)")
            db.execSQL("CREATE TABLE $T_FACE ($F_ID INTEGER PRIMARY KEY AUTOINCREMENT, $F_EMP_ID TEXT NOT NULL, $F_FCBA TEXT NOT NULL, $F_FEATURES TEXT NOT NULL, $F_CAPTURED DATETIME DEFAULT CURRENT_TIMESTAMP)")
            db.execSQL("CREATE TABLE $T_ATT ($A_ID INTEGER PRIMARY KEY AUTOINCREMENT, $A_EMP_ID TEXT NOT NULL, $A_FCBA TEXT NOT NULL, $A_EMP_NAME TEXT NOT NULL, $A_ACTION TEXT NOT NULL, $A_TIMESTAMP DATETIME DEFAULT CURRENT_TIMESTAMP)")

            // 2. Import SQL Master
            importSqlFromAssets(db, context)

            // 3. Insert Admin
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
        onCreate(db)
    }

    private fun forceInsertAdmin(db: SQLiteDatabase) {
        try {
            val cv = ContentValues().apply {
                put(U_USERNAME, "admin")
                put(U_PASSWORD, "1234")
                put(U_EMPCODE, "ADM001")
                put(U_FCBA, "BA01")
                put(U_DIVISI, "IT")
                put(U_GANG, "G01")
            }
            db.replace(T_USERS, null, cv)
        } catch (e: Exception) {
            Log.e("DB_ERROR", "Admin insert failed: ${e.message}")
        }
    }

    fun checkLogin(username: String, pass: String): UserProfile? {
        val u = username.trim()
        val p = pass.trim()

        // --- EMERGENCY BYPASS UNTUK ADMIN ---
        // Solusi paling ampuh jika DB locked karena import SQL yang berat
        if (u.equals("admin", ignoreCase = true) && p == "1234") {
            Log.d("DB_LOGIN", "Bypass Admin Login Success")
            return UserProfile("admin", "ADM001", "BA01", "IT", "G01")
        }

        val db = writableDatabase
        return try {
            val query = "SELECT * FROM $T_USERS WHERE $U_USERNAME = ? COLLATE NOCASE"
            db.rawQuery(query, arrayOf(u)).use { c ->
                if (c.moveToFirst()) {
                    val dbPass = c.getString(c.getColumnIndexOrThrow(U_PASSWORD)).trim()
                    if (dbPass == p) {
                        return UserProfile(
                            username = c.getString(c.getColumnIndexOrThrow(U_USERNAME)),
                            empcode = c.getString(c.getColumnIndexOrThrow(U_EMPCODE)) ?: "",
                            fcba = c.getString(c.getColumnIndexOrThrow(U_FCBA)) ?: "",
                            divisi = c.getString(c.getColumnIndexOrThrow(U_DIVISI)) ?: "",
                            gang = c.getString(c.getColumnIndexOrThrow(U_GANG)) ?: ""
                        )
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.e("DB_ERROR", "Login crash: ${e.message}")
            null
        }
    }

    fun getAllMasterEmployees(): List<Employee> {
        val list = mutableListOf<Employee>()
        try {
            readableDatabase.rawQuery("SELECT * FROM $T_EMP ORDER BY $E_NAME ASC", null).use { c ->
                while (c.moveToNext()) {
                    list.add(Employee(
                        fccode = c.getString(c.getColumnIndexOrThrow(E_FCCODE)),
                        fcba = c.getString(c.getColumnIndexOrThrow(E_FCBA)),
                        name = c.getString(c.getColumnIndexOrThrow(E_NAME)),
                        sectionName = c.getString(c.getColumnIndexOrThrow(E_SECTION)) ?: "",
                        gangCode = c.getString(c.getColumnIndexOrThrow(E_GANG)) ?: "",
                        position = c.getString(c.getColumnIndexOrThrow(E_POSITION)) ?: "",
                        companyNik = c.getString(c.getColumnIndexOrThrow(E_NIK)) ?: ""
                    ))
                }
            }
        } catch (e: Exception) { }
        return list
    }

    fun getEmployeeByOnlyCode(code: String): Employee? {
        try {
            readableDatabase.query(T_EMP, null, "$E_FCCODE = ?", arrayOf(code), null, null, null).use { cursor ->
                if (cursor.moveToFirst()) {
                    return Employee(
                        fccode = cursor.getString(cursor.getColumnIndexOrThrow(E_FCCODE)),
                        fcba = cursor.getString(cursor.getColumnIndexOrThrow(E_FCBA)),
                        name = cursor.getString(cursor.getColumnIndexOrThrow(E_NAME)),
                        sectionName = cursor.getString(cursor.getColumnIndexOrThrow(E_SECTION)) ?: "",
                        gangCode = cursor.getString(cursor.getColumnIndexOrThrow(E_GANG)) ?: "",
                        position = cursor.getString(cursor.getColumnIndexOrThrow(E_POSITION)) ?: "",
                        companyNik = cursor.getString(cursor.getColumnIndexOrThrow(E_NIK)) ?: ""
                    )
                }
            }
        } catch (e: Exception) {}
        return null
    }

    fun insertFaceMapping(mapping: FaceMapping): Boolean {
        return try {
            val cv = ContentValues().apply {
                put(F_EMP_ID, mapping.employeeId)
                put(F_FCBA, mapping.fcbaId)
                put(F_FEATURES, mapping.features.joinToString(","))
            }
            writableDatabase.insert(T_FACE, null, cv) != -1L
        } catch (e: Exception) { false }
    }

    fun getFaceMappingCount(fccode: String, fcba: String): Int {
        return try {
            readableDatabase.rawQuery("SELECT COUNT(*) FROM $T_FACE WHERE $F_EMP_ID = ? AND $F_FCBA = ?", arrayOf(fccode, fcba)).use {
                if (it.moveToFirst()) it.getInt(0) else 0
            }
        } catch (e: Exception) { 0 }
    }

    fun getFaceFeaturesForEmployee(fccode: String, fcba: String): List<FloatArray> {
        val list = mutableListOf<FloatArray>()
        try {
            readableDatabase.rawQuery("SELECT $F_FEATURES FROM $T_FACE WHERE $F_EMP_ID = ? AND $F_FCBA = ?", arrayOf(fccode, fcba)).use { cursor ->
                while (cursor.moveToNext()) {
                    val featStr = cursor.getString(0)
                    if (!featStr.isNullOrEmpty()) {
                        list.add(featStr.split(",").map { it.toFloat() }.toFloatArray())
                    }
                }
            }
        } catch (e: Exception) { }
        return list
    }

    fun saveAttendance(fccode: String, fcba: String, action: String): Boolean {
        return try {
            val db = writableDatabase
            var empName = "Unknown"
            db.rawQuery("SELECT $E_NAME FROM $T_EMP WHERE $E_FCCODE=? AND $E_FCBA=?", arrayOf(fccode, fcba)).use { cursor ->
                if (cursor.moveToFirst()) empName = cursor.getString(0)
            }
            val cv = ContentValues().apply {
                put(A_EMP_ID, fccode)
                put(A_FCBA, fcba)
                put(A_EMP_NAME, empName)
                put(A_ACTION, action)
            }
            db.insert(T_ATT, null, cv) != -1L
        } catch (e: Exception) { false }
    }

    fun getAllAttendance(): List<AttendanceRecord> {
        val list = mutableListOf<AttendanceRecord>()
        try {
            readableDatabase.rawQuery("SELECT * FROM $T_ATT ORDER BY $A_TIMESTAMP DESC", null).use { c ->
                while (c.moveToNext()) {
                    list.add(AttendanceRecord(
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
        return list
    }

    private fun importSqlFromAssets(db: SQLiteDatabase, context: Context) {
        try {
            val reader = context.assets.open("EMPLOYEE_202605200755.sql").bufferedReader()
            db.beginTransaction()
            try {
                val statement = StringBuilder()
                reader.forEachLine { line ->
                    val trimmed = line.trim()
                    // Abaikan komentar dan baris kosong
                    if (trimmed.isEmpty() || trimmed.startsWith("--") || trimmed.startsWith("/") || trimmed.startsWith("SET ")) return@forEachLine

                    statement.append(trimmed)
                    if (trimmed.endsWith(";")) {
                        var sql = statement.toString()
                            .replace("TIMESTAMP'", "'")
                            .replace(Regex("(?i)TO_DATE\\(.*?\\)"), "NULL")

                        if (sql.contains("INSERT INTO", true)) {
                            sql = sql.replace("(?i)INSERT INTO".toRegex(), "INSERT OR REPLACE INTO")
                            db.execSQL(sql)
                        } else if (sql.isNotBlank()) {
                            db.execSQL(sql)
                        }
                        statement.setLength(0)
                    }
                }
                db.setTransactionSuccessful()
                Log.d("DB_IMPORT", "SQL Import Completed Successfully")
            } finally {
                db.endTransaction()
            }
        } catch (e: Exception) {
            Log.e("DB_IMPORT", "Critical Error during Import: ${e.message}")
        }
    }
}