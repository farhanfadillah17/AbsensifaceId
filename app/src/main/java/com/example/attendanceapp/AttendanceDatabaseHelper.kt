package com.example.attendanceapp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

// ─── Data classes ────────────────────────────────────────────────────────────

data class Employee(
    val id: String,
    val name: String,
    val department: String,
    val position: String,
    val phone: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class FaceMapping(
    val id: Int = 0,
    val employeeId: String,
    val features: FloatArray,
    val capturedAt: Long = System.currentTimeMillis()
)

data class AttendanceRecord(
    val id: Int,
    val employeeId: String,
    val employeeName: String,
    val action: String,
    val timestamp: Long
)

// ─── Database ─────────────────────────────────────────────────────────────────

class AttendanceDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "attendance.db"
        const val DATABASE_VERSION = 2

        // table: employees
        const val T_EMP = "employees"
        const val E_ID = "id"
        const val E_NAME = "name"
        const val E_DEPT = "department"
        const val E_POS = "position"
        const val E_PHONE = "phone"
        const val E_CREATED = "created_at"

        // table: face_mappings
        const val T_FACE = "face_mappings"
        const val F_ID = "id"
        const val F_EMP_ID = "employee_id"
        const val F_FEATURES = "features"
        const val F_CAPTURED = "captured_at"

        // table: attendance
        const val T_ATT = "attendance"
        const val A_ID = "id"
        const val A_EMP_ID = "employee_id"
        const val A_EMP_NAME = "employee_name"
        const val A_ACTION = "action"
        const val A_TIMESTAMP = "timestamp"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Tabel Karyawan
        db.execSQL("""
            CREATE TABLE $T_EMP (
                $E_ID TEXT PRIMARY KEY,
                $E_NAME TEXT NOT NULL,
                $E_DEPT TEXT,
                $E_POS TEXT,
                $E_PHONE TEXT,
                $E_CREATED INTEGER
            )
        """)

        // Tabel Mapping Wajah
        db.execSQL("""
            CREATE TABLE $T_FACE (
                $F_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $F_EMP_ID TEXT NOT NULL,
                $F_FEATURES TEXT NOT NULL,
                $F_CAPTURED INTEGER,
                FOREIGN KEY($F_EMP_ID) REFERENCES $T_EMP($E_ID) ON DELETE CASCADE
            )
        """)

        // Tabel Riwayat Absensi
        db.execSQL("""
            CREATE TABLE $T_ATT (
                $A_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $A_EMP_ID TEXT NOT NULL,
                $A_EMP_NAME TEXT NOT NULL,
                "$A_ACTION" TEXT NOT NULL,
                "$A_TIMESTAMP" INTEGER NOT NULL
            )
        """)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $T_ATT")
        db.execSQL("DROP TABLE IF EXISTS $T_FACE")
        db.execSQL("DROP TABLE IF EXISTS $T_EMP")
        onCreate(db)
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    // ── Employees ──────────────────────────────────────────────────────────────

    /**
     * Mengecek apakah ID Karyawan sudah ada di database.
     * DITAMBAHKAN untuk memperbaiki error 'Unresolved reference' di Form.
     */
    fun employeeExists(id: String): Boolean {
        val db = readableDatabase
        val cursor = db.query(
            T_EMP,
            arrayOf(E_ID),
            "$E_ID = ?",
            arrayOf(id),
            null, null, null
        )
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }

    fun insertEmployee(emp: Employee): Boolean {
        return try {
            val db = writableDatabase
            val cv = ContentValues().apply {
                put(E_ID, emp.id)
                put(E_NAME, emp.name)
                put(E_DEPT, emp.department)
                put(E_POS, emp.position)
                put(E_PHONE, emp.phone)
                put(E_CREATED, emp.createdAt)
            }
            db.insertWithOnConflict(T_EMP, null, cv, SQLiteDatabase.CONFLICT_REPLACE) != -1L
        } catch (e: Exception) {
            false
        }
    }

    fun getEmployee(id: String): Employee? {
        val db = readableDatabase
        return try {
            db.query(T_EMP, null, "$E_ID=?", arrayOf(id), null, null, null).use { c ->
                if (c.moveToFirst()) {
                    Employee(
                        id = c.getString(c.getColumnIndexOrThrow(E_ID)),
                        name = c.getString(c.getColumnIndexOrThrow(E_NAME)),
                        department = c.getString(c.getColumnIndexOrThrow(E_DEPT)) ?: "",
                        position = c.getString(c.getColumnIndexOrThrow(E_POS)) ?: "",
                        phone = c.getString(c.getColumnIndexOrThrow(E_PHONE)) ?: "",
                        createdAt = c.getLong(c.getColumnIndexOrThrow(E_CREATED))
                    )
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun getAllEmployees(): List<Employee> {
        val list = mutableListOf<Employee>()
        val db = readableDatabase
        db.query(T_EMP, null, null, null, null, null, "$E_NAME ASC").use { c ->
            while (c.moveToNext()) {
                list += Employee(
                    id = c.getString(c.getColumnIndexOrThrow(E_ID)),
                    name = c.getString(c.getColumnIndexOrThrow(E_NAME)),
                    department = c.getString(c.getColumnIndexOrThrow(E_DEPT)) ?: "",
                    position = c.getString(c.getColumnIndexOrThrow(E_POS)) ?: "",
                    phone = c.getString(c.getColumnIndexOrThrow(E_PHONE)) ?: "",
                    createdAt = c.getLong(c.getColumnIndexOrThrow(E_CREATED))
                )
            }
        }
        return list
    }

    // ── Face Mappings ──────────────────────────────────────────────────────────

    fun insertFaceMapping(mapping: FaceMapping): Boolean {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(F_EMP_ID, mapping.employeeId)
            put(F_FEATURES, mapping.features.joinToString(","))
            put(F_CAPTURED, mapping.capturedAt)
        }
        return db.insert(T_FACE, null, cv) != -1L
    }

    fun getFaceFeaturesForEmployee(employeeId: String): List<FloatArray> {
        val list = mutableListOf<FloatArray>()
        val db = readableDatabase
        db.query(T_FACE, arrayOf(F_FEATURES), "$F_EMP_ID=?", arrayOf(employeeId), null, null, null).use { c ->
            while (c.moveToNext()) {
                val featStr = c.getString(0)
                list += featStr.split(",").map { it.toFloat() }.toFloatArray()
            }
        }
        return list
    }

    fun getAllFaceMappings(): List<FaceMapping> {
        val list = mutableListOf<FaceMapping>()
        val db = readableDatabase
        db.query(T_FACE, null, null, null, null, null, null).use { c ->
            while (c.moveToNext()) {
                val featStr = c.getString(c.getColumnIndexOrThrow(F_FEATURES))
                list += FaceMapping(
                    id = c.getInt(c.getColumnIndexOrThrow(F_ID)),
                    employeeId = c.getString(c.getColumnIndexOrThrow(F_EMP_ID)),
                    features = featStr.split(",").map { it.toFloat() }.toFloatArray(),
                    capturedAt = c.getLong(c.getColumnIndexOrThrow(F_CAPTURED))
                )
            }
        }
        return list
    }

    fun getFaceMappingCount(employeeId: String): Int {
        val db = readableDatabase
        db.rawQuery("SELECT COUNT(*) FROM $T_FACE WHERE $F_EMP_ID=?", arrayOf(employeeId)).use { c ->
            return if (c.moveToFirst()) c.getInt(0) else 0
        }
    }

    fun hasEnoughFaceMappings(employeeId: String, minSamples: Int): Boolean {
        return getFaceMappingCount(employeeId) >= minSamples
    }

    // ── Attendance ─────────────────────────────────────────────────────────────

    fun saveAttendance(employeeId: String, action: String): Long {
        val employee = getEmployee(employeeId)
        val name = employee?.name ?: "Unknown"
        val currentTime = System.currentTimeMillis()

        val db = writableDatabase
        val cv = ContentValues().apply {
            put(A_EMP_ID, employeeId)
            put(A_EMP_NAME, name)
            put(A_ACTION, action)
            put(A_TIMESTAMP, currentTime)
        }
        return db.insert(T_ATT, null, cv)
    }

    fun getAllAttendance(): List<AttendanceRecord> {
        val list = mutableListOf<AttendanceRecord>()
        val db = readableDatabase
        db.query(T_ATT, null, null, null, null, null, "$A_TIMESTAMP DESC").use { c ->
            while (c.moveToNext()) {
                list += AttendanceRecord(
                    id = c.getInt(c.getColumnIndexOrThrow(A_ID)),
                    employeeId = c.getString(c.getColumnIndexOrThrow(A_EMP_ID)),
                    employeeName = c.getString(c.getColumnIndexOrThrow(A_EMP_NAME)),
                    action = c.getString(c.getColumnIndexOrThrow(A_ACTION)),
                    timestamp = c.getLong(c.getColumnIndexOrThrow(A_TIMESTAMP))
                )
            }
        }
        return list
    }

    fun deleteEmployee(id: String): Int {
        return writableDatabase.delete(T_EMP, "$E_ID=?", arrayOf(id))
    }

    fun deleteAllAttendance() {
        writableDatabase.delete(T_ATT, null, null)
    }
}