package com.example.attendanceapp

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("attendance_prefs", Context.MODE_PRIVATE)

    /**
     * Menyimpan data login ke SharedPreferences
     */
    fun saveSession(fccode: String, fcba: String, name: String) {
        val editor = prefs.edit()
        editor.putString("fccode", fccode)
        editor.putString("fcba", fcba)
        editor.putString("name", name)
        editor.putBoolean("is_logged_in", true)
        editor.apply()
    }

    // Mengambil status apakah user sudah login
    fun isLoggedIn(): Boolean = prefs.getBoolean("is_logged_in", false)

    /**
     * Mengambil Nama Karyawan
     * Diubah menjadi getUserName() agar sesuai dengan panggilan di MainActivity
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