package com.example.attendanceapp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class AttendanceRecord(
    val id: Int,
    val employeeId: String,
    val employeeName: String,
    val action: String,
    val timestamp: Long
)

class AttendanceDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "attendance.db"
        const val DATABASE_VERSION = 1

        const val TABLE_ATTENDANCE = "attendance"
        const val COL_ID = "id"
        const val COL_EMPLOYEE_ID = "employee_id"
        const val COL_EMPLOYEE_NAME = "employee_name"
        const val COL_ACTION = "action"
        const val COL_TIMESTAMP = "timestamp"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_ATTENDANCE (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_EMPLOYEE_ID TEXT NOT NULL,
                $COL_EMPLOYEE_NAME TEXT NOT NULL,
                $COL_ACTION TEXT NOT NULL,
                $COL_TIMESTAMP INTEGER NOT NULL
            )
        """.trimIndent()
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ATTENDANCE")
        onCreate(db)
    }

    fun insertAttendance(
        employeeId: String,
        employeeName: String,
        action: String,
        timestamp: Long
    ): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_EMPLOYEE_ID, employeeId)
            put(COL_EMPLOYEE_NAME, employeeName)
            put(COL_ACTION, action)
            put(COL_TIMESTAMP, timestamp)
        }
        return db.insert(TABLE_ATTENDANCE, null, values)
    }

    fun getAllAttendance(): List<AttendanceRecord> {
        val records = mutableListOf<AttendanceRecord>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_ATTENDANCE,
            null,
            null,
            null,
            null,
            null,
            "$COL_TIMESTAMP DESC"
        )
        with(cursor) {
            while (moveToNext()) {
                records.add(
                    AttendanceRecord(
                        id = getInt(getColumnIndexOrThrow(COL_ID)),
                        employeeId = getString(getColumnIndexOrThrow(COL_EMPLOYEE_ID)),
                        employeeName = getString(getColumnIndexOrThrow(COL_EMPLOYEE_NAME)),
                        action = getString(getColumnIndexOrThrow(COL_ACTION)),
                        timestamp = getLong(getColumnIndexOrThrow(COL_TIMESTAMP))
                    )
                )
            }
            close()
        }
        return records
    }

    fun getAttendanceByEmployee(employeeId: String): List<AttendanceRecord> {
        val records = mutableListOf<AttendanceRecord>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_ATTENDANCE,
            null,
            "$COL_EMPLOYEE_ID = ?",
            arrayOf(employeeId),
            null,
            null,
            "$COL_TIMESTAMP DESC"
        )
        with(cursor) {
            while (moveToNext()) {
                records.add(
                    AttendanceRecord(
                        id = getInt(getColumnIndexOrThrow(COL_ID)),
                        employeeId = getString(getColumnIndexOrThrow(COL_EMPLOYEE_ID)),
                        employeeName = getString(getColumnIndexOrThrow(COL_EMPLOYEE_NAME)),
                        action = getString(getColumnIndexOrThrow(COL_ACTION)),
                        timestamp = getLong(getColumnIndexOrThrow(COL_TIMESTAMP))
                    )
                )
            }
            close()
        }
        return records
    }

    fun getLastActionToday(employeeId: String): String? {
        val db = readableDatabase
        val startOfDay = getStartOfDayTimestamp()
        val cursor = db.query(
            TABLE_ATTENDANCE,
            arrayOf(COL_ACTION),
            "$COL_EMPLOYEE_ID = ? AND $COL_TIMESTAMP >= ?",
            arrayOf(employeeId, startOfDay.toString()),
            null,
            null,
            "$COL_TIMESTAMP DESC",
            "1"
        )
        return if (cursor.moveToFirst()) {
            val action = cursor.getString(cursor.getColumnIndexOrThrow(COL_ACTION))
            cursor.close()
            action
        } else {
            cursor.close()
            null
        }
    }

    private fun getStartOfDayTimestamp(): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
