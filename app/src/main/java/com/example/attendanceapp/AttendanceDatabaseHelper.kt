package com.example.attendanceapp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

// ─── Data classes ────────────────────────────────────────────────────────────

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
    val timestamp: Long
)

// ─── Database ─────────────────────────────────────────────────────────────────

class AttendanceDatabaseHelper(private val context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "attendance.db"
        // Dinaikkan ke 5 agar database di-reset dan mengimpor file SQL baru
        const val DATABASE_VERSION = 5

        const val T_EMP = "EMPLOYEE"
        const val E_FCCODE = "FCCODE"
        const val E_FCBA = "FCBA"
        const val E_NAME = "FCNAME"
        const val E_SECTION = "SECTIONNAME"
        const val E_GANG = "GANGCODE"
        const val E_POSITION = "POSITION"
        const val E_GENDER = "GENDER"
        const val E_ADDRESS = "ADDRESS"
        const val E_NIK = "COMPANY_NIK"
        const val E_FACE_EMB = "FACEEMBEDDING"
        const val E_LAST_UPDATE = "LASTUPDATE"

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
        // 1. Buat Tabel Employee dengan kolom lengkap sesuai file SQL
        db.execSQL("""
            CREATE TABLE $T_EMP (
                $E_FCCODE TEXT NOT NULL,
                $E_NAME TEXT,
                $E_SECTION TEXT,
                $E_GANG TEXT,
                DATEOFBIRTH TEXT,
                RELIGION TEXT,
                RACE TEXT,
                IDENTITYCARD TEXT,
                HIGHESTEDUCATION TEXT,
                YEAROFGRADUATION TEXT,
                DATEOFJOIN TEXT,
                DATEOFDESIGNATION TEXT,
                PRESENTGRADE TEXT,
                PRESENTGRADE_DETAIL TEXT,
                PRESENTRATE TEXT,
                PREVIOUSGRADE TEXT,
                PREVIOUSGRADE_DETAIL TEXT,
                PREVIOUSRATE TEXT,
                MARITALSTATUS TEXT,
                SPOUSE TEXT,
                CHILDNAME_1 TEXT,
                CHILDNAME_2 TEXT,
                DEPENDENTCHILD TEXT,
                JAMSOSTEKNO TEXT,
                JAMSOSTEKCATEGORY TEXT,
                $E_POSITION TEXT,
                DATETERMINATE TEXT,
                FCENTRY TEXT,
                FCEDIT TEXT,
                FCIP TEXT,
                $E_FCBA TEXT NOT NULL,
                NATIONALITY TEXT,
                NPWP TEXT,
                NATURETYPE TEXT,
                $E_LAST_UPDATE TEXT,
                LASTTIME TEXT,
                INCLUDEDTAXCALCULATION TEXT,
                TAXSTATUS TEXT,
                PAYMENTTYPE TEXT,
                BANKNAME TEXT,
                ACCOUNTNO TEXT,
                OWNERSHIPOFACCOUNT TEXT,
                $E_GENDER TEXT,
                TAXPAIDBYCOMPANY TEXT,
                $E_ADDRESS TEXT,
                BPJSKES_CATEGORY TEXT,
                BPJSKES_NO TEXT,
                $E_NIK TEXT,
                SUBGRADE TEXT,
                EMP_TYPE TEXT,
                PRESENTGRADE2 TEXT,
                PRESENTGRADE_DETAIL2 TEXT,
                PREVIOUSGRADE2 TEXT,
                PREVIOUSGRADE_DETAIL2 TEXT,
                REMARKS_TERMINATE TEXT,
                BLACKLIST TEXT,
                GOLONGAN TEXT,
                STRUKFUNGSI TEXT,
                SECTIONNAMEDTL TEXT,
                JOBCODE TEXT,
                TARGETTYPE TEXT,
                TARGETCODE TEXT,
                CUTI TEXT,
                BPJSUSINGTHP TEXT,
                GET_TKOM TEXT,
                GET_TRUMAH TEXT,
                GET_TTETAP TEXT,
                GET_TBK TEXT,
                GET_TTRANS TEXT,
                GET_TMAKAN TEXT,
                GET_TJAB TEXT,
                GET_JHT TEXT,
                GET_JP TEXT,
                $E_FACE_EMB TEXT,
                PRIMARY KEY ($E_FCCODE, $E_FCBA)
            )
        """)

        // 2. Buat Tabel Face Mapping
        db.execSQL("""
            CREATE TABLE $T_FACE (
                $F_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $F_EMP_ID TEXT NOT NULL,
                $F_FCBA TEXT NOT NULL,
                $F_FEATURES TEXT NOT NULL,
                $F_CAPTURED DATETIME DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY($F_EMP_ID, $F_FCBA) REFERENCES $T_EMP($E_FCCODE, $E_FCBA) ON DELETE CASCADE
            )
        """)

        // 3. Buat Tabel Attendance
        db.execSQL("""
            CREATE TABLE $T_ATT (
                $A_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $A_EMP_ID TEXT NOT NULL,
                $A_FCBA TEXT NOT NULL,
                $A_EMP_NAME TEXT NOT NULL,
                "$A_ACTION" TEXT NOT NULL,
                "$A_TIMESTAMP" DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        """)

        // 4. Import data dari assets
        importSqlFromAssets(db, context)
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

    private fun importSqlFromAssets(db: SQLiteDatabase, context: Context) {
        try {
            val inputStream = context.assets.open("EMPLOYEE_202605200755.sql")
            val reader = inputStream.bufferedReader()

            // Membaca seluruh isi file sebagai satu String
            val fullSql = reader.readText()

            // Membersihkan sintaks TIMESTAMP yang tidak didukung SQLite
            // Mengubah TIMESTAMP'2025-01-01 00:00:00.0' menjadi '2025-01-01 00:00:00.0'
            val cleanedSql = fullSql.replace("TIMESTAMP'", "'")

            // Memisahkan berdasarkan titik koma (;) untuk mengeksekusi perintah satu per satu
            val statements = cleanedSql.split(";")

            db.beginTransaction()
            try {
                for (statement in statements) {
                    val st = statement.trim()
                    if (st.isNotEmpty() && st.uppercase().startsWith("INSERT")) {
                        try {
                            db.execSQL(st)
                        } catch (e: Exception) {
                            // Jika satu baris gagal (misal kolom tidak cocok),
                            // aplikasi tidak akan Force Close, hanya mencatat error di log
                            Log.e("DB_IMPORT_ERROR", "Gagal eksekusi baris: ${e.message}")
                        }
                    }
                }
                db.setTransactionSuccessful()
                Log.d("DB_IMPORT", "Proses Import Selesai")
            } finally {
                db.endTransaction()
            }
        } catch (e: Exception) {
            Log.e("DB_IMPORT", "Gagal Membaca File SQL: ${e.message}")
        }
    }

    // ── Employees & Master Data ────────────────────────────────────────────────

    fun getAllMasterEmployees(): List<Employee> {
        val list = mutableListOf<Employee>()
        val db = readableDatabase
        // OPTIMASI: Hanya ambil kolom yang benar-benar dibutuhkan untuk list
        val query = "SELECT $E_FCCODE, $E_FCBA, $E_NAME, $E_SECTION, $E_GANG, $E_POSITION, $E_NIK FROM $T_EMP ORDER BY $E_NAME ASC"

        db.rawQuery(query, null).use { c ->
            while (c.moveToNext()) {
                list += Employee(
                    fccode = c.getString(0),
                    fcba = c.getString(1),
                    name = c.getString(2),
                    sectionName = c.getString(3),
                    gangCode = c.getString(4),
                    position = c.getString(5) ?: "",
                    companyNik = c.getString(6) ?: ""
                )
            }
        }
        return list
    }

    fun getEmployeeByOnlyCode(fccode: String): Employee? {
        val db = readableDatabase
        val query = "SELECT *, strftime('%s', $E_LAST_UPDATE) * 1000 AS last_upd_ms FROM $T_EMP WHERE $E_FCCODE = ?"
        return try {
            db.rawQuery(query, arrayOf(fccode)).use { c ->
                if (c.moveToFirst()) {
                    Employee(
                        fccode = c.getString(c.getColumnIndexOrThrow(E_FCCODE)),
                        fcba = c.getString(c.getColumnIndexOrThrow(E_FCBA)),
                        name = c.getString(c.getColumnIndexOrThrow(E_NAME)),
                        sectionName = c.getString(c.getColumnIndexOrThrow(E_SECTION)),
                        gangCode = c.getString(c.getColumnIndexOrThrow(E_GANG)),
                        position = c.getString(c.getColumnIndexOrThrow(E_POSITION)) ?: "",
                        gender = c.getString(c.getColumnIndexOrThrow(E_GENDER)) ?: "",
                        address = c.getString(c.getColumnIndexOrThrow(E_ADDRESS)) ?: "",
                        companyNik = c.getString(c.getColumnIndexOrThrow(E_NIK)) ?: "",
                        faceEmbedding = c.getString(c.getColumnIndexOrThrow(E_FACE_EMB)),
                        lastUpdate = c.getLong(c.getColumnIndexOrThrow("last_upd_ms"))
                    )
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun insertEmployee(emp: Employee): Boolean {
        return try {
            val db = writableDatabase
            val cv = ContentValues().apply {
                put(E_FCCODE, emp.fccode)
                put(E_FCBA, emp.fcba)
                put(E_NAME, emp.name)
                put(E_SECTION, emp.sectionName)
                put(E_GANG, emp.gangCode)
                put(E_POSITION, emp.position)
                put(E_GENDER, emp.gender)
                put(E_ADDRESS, emp.address)
                put(E_NIK, emp.companyNik)
                put(E_FACE_EMB, emp.faceEmbedding)
            }
            db.insertWithOnConflict(T_EMP, null, cv, SQLiteDatabase.CONFLICT_REPLACE) != -1L
        } catch (e: Exception) {
            false
        }
    }

    fun getEmployee(fccode: String, fcba: String): Employee? {
        val db = readableDatabase
        return try {
            val query = "SELECT *, strftime('%s', $E_LAST_UPDATE) * 1000 AS last_upd_ms FROM $T_EMP WHERE $E_FCCODE=? AND $E_FCBA=?"
            db.rawQuery(query, arrayOf(fccode, fcba)).use { c ->
                if (c.moveToFirst()) {
                    Employee(
                        fccode = c.getString(c.getColumnIndexOrThrow(E_FCCODE)),
                        fcba = c.getString(c.getColumnIndexOrThrow(E_FCBA)),
                        name = c.getString(c.getColumnIndexOrThrow(E_NAME)),
                        sectionName = c.getString(c.getColumnIndexOrThrow(E_SECTION)),
                        gangCode = c.getString(c.getColumnIndexOrThrow(E_GANG)),
                        position = c.getString(c.getColumnIndexOrThrow(E_POSITION)) ?: "",
                        gender = c.getString(c.getColumnIndexOrThrow(E_GENDER)) ?: "",
                        address = c.getString(c.getColumnIndexOrThrow(E_ADDRESS)) ?: "",
                        companyNik = c.getString(c.getColumnIndexOrThrow(E_NIK)) ?: "",
                        faceEmbedding = c.getString(c.getColumnIndexOrThrow(E_FACE_EMB)),
                        lastUpdate = c.getLong(c.getColumnIndexOrThrow("last_upd_ms"))
                    )
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    // ── Face Mappings ──────────────────────────────────────────────────────────

    fun insertFaceMapping(mapping: FaceMapping): Boolean {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(F_EMP_ID, mapping.employeeId)
            put(F_FCBA, mapping.fcbaId)
            put(F_FEATURES, mapping.features.joinToString(","))
        }
        return db.insert(T_FACE, null, cv) != -1L
    }

    // ── Face Mappings ──────────────────────────────────────────────────────────

    /**
     * Menghitung jumlah sampel wajah yang tersimpan untuk karyawan tertentu.
     */
    fun getFaceMappingCount(fccode: String, fcba: String): Int {
        val db = readableDatabase
        val query = "SELECT COUNT(*) FROM $T_FACE WHERE $F_EMP_ID = ? AND $F_FCBA = ?"
        return try {
            db.rawQuery(query, arrayOf(fccode, fcba)).use { c ->
                if (c.moveToFirst()) c.getInt(0) else 0
            }
        } catch (e: Exception) {
            0
        }
    }

    fun getFaceFeaturesForEmployee(fccode: String, fcba: String): List<FloatArray> {
        val list = mutableListOf<FloatArray>()
        val db = readableDatabase
        db.query(T_FACE, arrayOf(F_FEATURES), "$F_EMP_ID=? AND $F_FCBA=?", arrayOf(fccode, fcba), null, null, null).use { c ->
            while (c.moveToNext()) {
                val featStr = c.getString(0)
                list += featStr.split(",").map { it.toFloat() }.toFloatArray()
            }
        }
        return list
    }

    // ── Attendance ─────────────────────────────────────────────────────────────

    fun saveAttendance(fccode: String, fcba: String, action: String): Long {
        val employee = getEmployee(fccode, fcba)
        val name = employee?.name ?: "Unknown"

        val db = writableDatabase
        val cv = ContentValues().apply {
            put(A_EMP_ID, fccode)
            put(A_FCBA, fcba)
            put(A_EMP_NAME, name)
            put(A_ACTION, action)
        }
        return db.insert(T_ATT, null, cv)
    }

    fun getAllAttendance(): List<AttendanceRecord> {
        val list = mutableListOf<AttendanceRecord>()
        val db = readableDatabase
        val query = "SELECT *, strftime('%s', $A_TIMESTAMP) * 1000 AS ts_ms FROM $T_ATT ORDER BY $A_TIMESTAMP DESC"

        db.rawQuery(query, null).use { c ->
            while (c.moveToNext()) {
                list += AttendanceRecord(
                    id = c.getInt(c.getColumnIndexOrThrow(A_ID)),
                    employeeId = c.getString(c.getColumnIndexOrThrow(A_EMP_ID)),
                    fcbaId = c.getString(c.getColumnIndexOrThrow(A_FCBA)),
                    employeeName = c.getString(c.getColumnIndexOrThrow(A_EMP_NAME)),
                    action = c.getString(c.getColumnIndexOrThrow(A_ACTION)),
                    timestamp = c.getLong(c.getColumnIndexOrThrow("ts_ms"))
                )
            }
        }
        return list
    }

    fun deleteEmployee(fccode: String, fcba: String): Int {
        return writableDatabase.delete(T_EMP, "$E_FCCODE=? AND $E_FCBA=?", arrayOf(fccode, fcba))
    }
}