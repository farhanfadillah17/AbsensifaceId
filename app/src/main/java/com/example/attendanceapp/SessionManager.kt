package com.example.attendanceapp

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("attendance_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_LOGIN_TIMESTAMP = "login_timestamp"
        private const val SESSION_DURATION = 3600000L // 1 Jam dalam milidetik (60 * 60 * 1000)
    }

    /**
     * Menyimpan data login ke SharedPreferences termasuk waktu login
     */
    fun saveSession(fccode: String, fcba: String, name: String) {
        val editor = prefs.edit()
        editor.putString("fccode", fccode)
        editor.putString("fcba", fcba)
        editor.putString("name", name)
        editor.putBoolean("is_logged_in", true)
        // Simpan waktu saat ini sebagai waktu login
        editor.putLong(KEY_LOGIN_TIMESTAMP, System.currentTimeMillis())
        editor.apply()
    }

    /**
     * Mengambil status apakah user sudah login dan mengecek durasi sesi
     */
    fun isLoggedIn(): Boolean {
        val isLoggedIn = prefs.getBoolean("is_logged_in", false)
        if (!isLoggedIn) return false

        // Ambil waktu login dan waktu sekarang
        val loginTimestamp = prefs.getLong(KEY_LOGIN_TIMESTAMP, 0L)
        val currentTime = System.currentTimeMillis()

        // Cek apakah selisih waktu lebih besar dari durasi sesi (1 jam)
        if (currentTime - loginTimestamp > SESSION_DURATION) {
            logout() // Otomatis hapus data jika sudah lewat 1 jam
            return false
        }

        return true
    }

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

    /**
     * Fungsi alias jika Anda masih ingin menggunakan getName() di tempat lain
     */
    fun getName(): String? = getUserName()
}