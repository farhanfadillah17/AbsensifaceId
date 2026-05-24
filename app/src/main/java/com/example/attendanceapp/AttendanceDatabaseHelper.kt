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
    val lastUpdate: Long = System.currentTimeMillis(),
)

data class FaceMapping(
    val id: Int = 0,
    val employeeId: String,
    val fcbaId: String,
    val features: FloatArray,
    val capturedAt: Long = System.currentTimeMillis(),
)

data class AttendanceRecord(
    val id: Int,
    val employeeId: String,
    val fcbaId: String,
    val employeeName: String,
    val action: String,
    val timestamp: Long,
)

data class User(
    val fccode: String,
    val fcba: String,
    val name: String
)

// ─── Database ─────────────────────────────────────────────────────────────────

class AttendanceDatabaseHelper(private val context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "attendance.db"
        // Naikkan ke 8 untuk memastikan database lama yang rusak terhapus total
        const val DATABASE_VERSION = 18

        const val T_EMP = "EMPLOYEE"
        const val E_FCCODE = "FCCODE"
        const val E_FCBA = "FCBA"
        const val E_NAME = "FCNAME"
        const val E_SECTION = "SECTIONNAME"
        const val E_GANG = "GANGCODE"
        const val E_POSITION = "POSITION"
        const val E_GENDER = "GENDER"
        const val E_ADDRESS = "ADDRESS"
        const val E_NIK = "IDENTITYCARD"
        const val E_COMPANY_NIK = "COMPANY_NIK"
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
        db.execSQL("""
            CREATE TABLE $T_EMP (
                $E_FCCODE TEXT NOT NULL,
                $E_NAME TEXT,
                $E_SECTION TEXT,
                $E_GANG TEXT,
                DATEOFBIRTH TEXT,
                RELIGION TEXT,
                RACE TEXT,
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
                COMPANY_NIK TEXT,
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

        db.execSQL("""
            CREATE TABLE $T_ATT (
                $A_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $A_EMP_ID TEXT NOT NULL,
                $A_FCBA TEXT NOT NULL,
                $A_EMP_NAME TEXT NOT NULL,
                $A_ACTION TEXT NOT NULL,
                $A_TIMESTAMP DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        """)

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

            // Membaca file dan membersihkan karakter sampah di awal (BOM)
            var fullSql = inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            fullSql = fullSql.replace("\uFEFF", "")

            // Pembersihan Sintaks agar cocok dengan SQLite
            val cleanedSql = fullSql
                .replace("TIMESTAMP'", "'") // Ubah TIMESTAMP'2025...' jadi '2025...'
                .replace(Regex("(?i)TO_CLOB\\('(.*?)'\\)"), "'$1'") // Bersihkan TO_CLOB jika ada
                .replace("\"DBA\".", "") // Hapus schema
                .replace("\"POSITION\"", "POSITION") // Hapus kutip pada kata cadangan
                .replace("\"EMPLOYEE\"", "EMPLOYEE") // Nama tabel tanpa kutip

            // Pisahkan berdasarkan titik koma (;) untuk mendapatkan setiap blok INSERT
            val statements = cleanedSql.split(";")

            db.beginTransaction()
            try {
                var totalDataInput = 0
                for (st in statements) {
                    val sql = st.trim()
                    if (sql.isNotEmpty() && sql.uppercase().contains("INSERT INTO")) {
                        try {
                            db.execSQL("$sql;")
                            totalDataInput++
                        } catch (e: Exception) {
                            Log.e("DB_IMPORT_ERROR", "Gagal di blok INSERT: ${e.message}")
                        }
                    }
                }
                db.setTransactionSuccessful()

                // Cek jumlah data yang benar-benar masuk ke tabel
                val cursor = db.rawQuery("SELECT COUNT(*) FROM EMPLOYEE", null)
                if (cursor.moveToFirst()) {
                    val count = cursor.getInt(0)
                    Log.d("DB_IMPORT", "BERHASIL! Total data di database: $count")
                }
                cursor.close()

            } finally {
                db.endTransaction()
            }
        } catch (e: Exception) {
            Log.e("DB_IMPORT", "KESALAHAN FATAL: ${e.message}")
        }
    }

    fun getAllMasterEmployees(): List<Employee> {
        val list = mutableListOf<Employee>()
        val db = readableDatabase
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

    fun updateEmployeeFaceData(fccode: String, fcba: String, faceData: String): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            // Asumsi nama kolom di tabel karyawan Anda adalah 'face_embedding'
            // atau sesuaikan dengan nama kolom yang Anda buat di onCreate
            put("face_embedding", faceData)
        }

        // Mengupdate berdasarkan FCCode dan FCBA (kunci unik karyawan)
        val result = db.update(
            "TABLE_EMPLOYEE", // Ganti dengan nama konstanta tabel karyawan Anda
            values,
            "fccode = ? AND fcba = ?",
            arrayOf(fccode, fcba)
        )

        return result > 0
    }

    fun insertAttendance(fccode: String, fcba: String, name: String, action: String): Boolean {
        val db = writableDatabase
        return try {
            val values = ContentValues().apply {
                put(A_EMP_ID, fccode)
                put(A_FCBA, fcba)
                put(A_EMP_NAME, name)
                put(A_ACTION, action)
                // A_TIMESTAMP akan terisi otomatis oleh DEFAULT CURRENT_TIMESTAMP
            }
            val result = db.insert(T_ATT, null, values)
            result != -1L
        } catch (e: Exception) {
            Log.e("DB_ERROR", "Gagal simpan absen: ${e.message}")
            false
        }
    }

    fun getEmployee(fccode: String, fcba: String): Employee? {
        val db = readableDatabase
        return try {
            val query = "SELECT * FROM $T_EMP WHERE $E_FCCODE=? AND $E_FCBA=?"
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
                        faceEmbedding = c.getString(c.getColumnIndexOrThrow(E_FACE_EMB))
                    )
                } else null
            }
        } catch (e: Exception) { null }
    }

    fun getEmployeeByOnlyCode(fccode: String): Employee? {
        val db = readableDatabase
        val query = "SELECT * FROM $T_EMP WHERE $E_FCCODE = ?"
        return try {
            db.rawQuery(query, arrayOf(fccode.trim())).use { c ->
                if (c.moveToFirst()) {
                    Employee(
                        fccode = c.getString(c.getColumnIndexOrThrow(E_FCCODE)),
                        fcba = c.getString(c.getColumnIndexOrThrow(E_FCBA)),
                        name = c.getString(c.getColumnIndexOrThrow(E_NAME)),
                        sectionName = c.getString(c.getColumnIndexOrThrow(E_SECTION)) ?: "",
                        gangCode = c.getString(c.getColumnIndexOrThrow(E_GANG)) ?: "",
                        position = c.getString(c.getColumnIndexOrThrow(E_POSITION)) ?: "",
                        companyNik = c.getString(c.getColumnIndexOrThrow(E_NIK)) ?: "",
                        gender = c.getString(c.getColumnIndexOrThrow(E_GENDER)) ?: "",
                        address = c.getString(c.getColumnIndexOrThrow(E_ADDRESS)) ?: "",
                        faceEmbedding = c.getString(c.getColumnIndexOrThrow(E_FACE_EMB))
                    )
                } else null
            }
        } catch (e: Exception) { null }
    }

    fun checkLogin(fccode: String, passwordKtp: String): User? { // Hapus com.google.firebase...
        val db = readableDatabase
        val query = """
        SELECT $E_FCCODE, $E_FCBA, $E_NAME 
        FROM $T_EMP 
        WHERE UPPER(TRIM($E_FCCODE)) = UPPER(TRIM(?)) 
        AND UPPER(TRIM($E_NIK)) = UPPER(TRIM(?))
    """.trimIndent()

        return try {
            db.rawQuery(query, arrayOf(fccode, passwordKtp)).use { cursor ->
                if (cursor.moveToFirst()) {
                    User( // Gunakan User class yang baru kita buat
                        fccode = cursor.getString(0),
                        fcba = cursor.getString(1),
                        name = cursor.getString(2)
                    )
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("DB_ERROR", "Login failed: ${e.message}")
            null
        }
    }

    fun insertFaceMapping(mapping: FaceMapping): Boolean {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(F_EMP_ID, mapping.employeeId)
            put(F_FCBA, mapping.fcbaId)
            put(F_FEATURES, mapping.features.joinToString(","))
        }
        return db.insert(T_FACE, null, cv) != -1L
    }

    fun getFaceMappingCount(fccode: String, fcba: String): Int {
        val db = readableDatabase
        val query = "SELECT COUNT(*) FROM $T_FACE WHERE $F_EMP_ID = ? AND $F_FCBA = ?"
        return try {
            db.rawQuery(query, arrayOf(fccode, fcba)).use { c ->
                if (c.moveToFirst()) c.getInt(0) else 0
            }
        } catch (e: Exception) { 0 }
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
        val query = "SELECT * FROM $T_ATT ORDER BY $A_TIMESTAMP DESC"
        db.rawQuery(query, null).use { c ->
            while (c.moveToNext()) {
                list += AttendanceRecord(
                    id = c.getInt(c.getColumnIndexOrThrow(A_ID)),
                    employeeId = c.getString(c.getColumnIndexOrThrow(A_EMP_ID)),
                    fcbaId = c.getString(c.getColumnIndexOrThrow(A_FCBA)),
                    employeeName = c.getString(c.getColumnIndexOrThrow(A_EMP_NAME)),
                    action = c.getString(c.getColumnIndexOrThrow(A_ACTION)),
                    timestamp = System.currentTimeMillis()
                )
            }
        }
        return list
    }
}