package com.example.attendanceapp

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_LOGIN_TIMESTAMP = "login_timestamp"
        private const val KEY_ROLE = "user_role" // Kunci baru untuk Role
        private const val SESSION_DURATION = 3600000L // 1 Jam
    }

    /**
     * PERBAIKAN: Menambahkan parameter 'role' saat menyimpan sesi
     */
    fun saveSession(fccode: String, fcba: String, name: String, role: String) {
        val editor = prefs.edit()
        editor.putString("fccode", fccode)
        editor.putString("fcba", fcba)
        editor.putString("name", name)
        editor.putString(KEY_ROLE, role) // Simpan Role (ADMIN/MANDOR/KERANI)
        editor.putBoolean("is_logged_in", true)
        editor.putLong(KEY_LOGIN_TIMESTAMP, System.currentTimeMillis())
        editor.apply()
    }

    /**
     * Mengecek apakah sesi masih aktif
     */
    fun isLoggedIn(): Boolean {
        val isLoggedIn = prefs.getBoolean("is_logged_in", false)
        if (!isLoggedIn) return false

        val loginTimestamp = prefs.getLong(KEY_LOGIN_TIMESTAMP, 0L)
        val currentTime = System.currentTimeMillis()

        if (currentTime - loginTimestamp > SESSION_DURATION) {
            logout()
            return false
        }
        return true
    }

    /**
     * MENGAMBIL ROLE USER (Sangat penting untuk Dashboard)
     */
    fun getUserRole(): String? = prefs.getString(KEY_ROLE, "MANDOR")

    /**
     * Mengambil Nama Karyawan
     */
    fun getUserName(): String? = prefs.getString("name", "User")

    // Mengambil ID Karyawan (FCCODE)
    fun getFccode(): String? = prefs.getString("fccode", null)

    // Mengambil Kode Cabang (FCBA)
    fun getFcba(): String? = prefs.getString("fcba", null)

    // Menghapus sesi (Logout)
    fun logout() {
        prefs.edit().clear().apply()
    }

    fun getName(): String? = getUserName()

    fun getEmpCode(): String? {
        // Pastikan "emp_code" adalah kunci yang sama saat kamu menyimpan data di login
        return prefs.getString("emp_code", null)
    }
}
