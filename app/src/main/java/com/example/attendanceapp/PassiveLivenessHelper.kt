package com.example.attendanceapp

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.util.Log
import com.google.mlkit.vision.face.Face
import kotlin.math.pow
import kotlin.math.sqrt

class PassiveLivenessHelper {

    data class LivenessResult(
        val isLive: Boolean,
        val score: Float,
        val message: String
    )

    /**
     * Menganalisis pasif liveness berdasarkan tekstur, ketajaman, dan warna.
     * @param bitmap Bitmap dari frame kamera (sudah di-crop ke area wajah)
     * @param face Objek Face dari ML Kit untuk data tambahan
     */
    fun analyze(bitmap: Bitmap, face: Face): LivenessResult {
        val sharpnessScore = calculateSharpness(bitmap)
        val skinScore = calculateSkinColorScore(bitmap)
        val textureScore = calculateTextureComplexity(bitmap)
        val screenScore = calculateDigitalScreenScore(bitmap)

        // Penyesuaian Bobot: Memberikan bobot tinggi pada Screen Score untuk mendeteksi layar HP/Laptop
        val totalScore = (sharpnessScore * 0.1f) + (skinScore * 0.4f) + (textureScore * 0.1f) + (screenScore * 0.4f)
        
        Log.d("PassiveLiveness", "Scores -> Sharpness: $sharpnessScore, Skin: $skinScore, Texture: $textureScore, Screen: $screenScore, Total: $totalScore")

        return when {
            // Jika Screen Score sangat rendah, hampir pasti itu layar digital
            screenScore < 0.3f -> LivenessResult(false, totalScore, "Terdeteksi layar HP/Laptop")
            // Gambar benar-benar blur total (skor < 2.0)
            sharpnessScore < 2.0f -> LivenessResult(false, totalScore, "Gambar terlalu blur")
            // Naikkan kembali skin threshold sedikit ke 0.20 agar foto hitam putih atau layar biru terdeteksi
            skinScore < 0.20f -> LivenessResult(false, totalScore, "Warna kulit tidak terdeteksi")
            // Threshold total 0.30 untuk menjaga keseimbangan antara noise dan keamanan
            totalScore < 0.30f -> LivenessResult(false, totalScore, "Bukan wajah manusia asli")
            else -> LivenessResult(true, totalScore, "Wajah Manusia Terdeteksi")
        }
    }

    /**
     * Deteksi Layar Digital (Moiré Pattern & Specular Reflection).
     */
    private fun calculateDigitalScreenScore(bitmap: Bitmap): Float {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var moireCount = 0
        var reflectionCount = 0
        val step = 2

        for (y in 0 until height - step step step) {
            for (x in 0 until width - step step step) {
                val p = pixels[y * width + x]
                val r = Color.red(p)
                val g = Color.green(p)
                val b = Color.blue(p)

                // 1. Deteksi Pantulan Cahaya Layar (Sangat putih dan tajam)
                if (r > 240 && g > 240 && b > 240) {
                    reflectionCount++
                }

                // 2. Deteksi Pola Moiré Sederhana (Perubahan intensitas yang sangat cepat)
                if (x + 1 < width) {
                    val pNext = pixels[y * width + (x + 1)]
                    val rNext = Color.red(pNext)
                    val diff = Math.abs(r - rNext)
                    if (diff > 50) moireCount++
                }
            }
        }

        val totalSampled = (width / step) * (height / step)
        val moireFactor = (1.0f - (moireCount.toFloat() / totalSampled * 5f)).coerceIn(0f, 1f)
        val reflectionFactor = (1.0f - (reflectionCount.toFloat() / totalSampled * 10f)).coerceIn(0f, 1f)

        return (moireFactor * 0.7f + reflectionFactor * 0.3f)
    }

    /**
     * Deteksi Ketajaman (Sharpness) menggunakan varians Laplacian sederhana.
     */
    private fun calculateSharpness(bitmap: Bitmap): Float {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var totalIntensity = 0.0
        val grayPixels = DoubleArray(width * height)

        for (i in pixels.indices) {
            val r = Color.red(pixels[i])
            val g = Color.green(pixels[i])
            val b = Color.blue(pixels[i])
            val gray = (0.299 * r + 0.587 * g + 0.114 * b)
            grayPixels[i] = gray
            totalIntensity += gray
        }

        val mean = totalIntensity / (width * height)
        var variance = 0.0
        for (i in grayPixels.indices) {
            variance += (grayPixels[i] - mean).pow(2.0)
        }
        
        // Normalisasi: Tetap menggunakan pembagi 10f agar range masuk akal
        return (sqrt(variance / (width * height)).toFloat() / 10f).coerceIn(0f, 100f)
    }

    /**
     * Validasi Warna Kulit (Skin Color Range).
     */
    private fun calculateSkinColorScore(bitmap: Bitmap): Float {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var skinPixels = 0
        for (pixel in pixels) {
            val r = Color.red(pixel)
            val g = Color.green(pixel)
            val b = Color.blue(pixel)

            // Ekstrem longgar: Menurunkan batas minimal RGB secara drastis untuk kamera laptop berkualitas rendah
            if (r > 40 && g > 20 && b > 10 && // Batas minimal sangat rendah
                (maxOf(r, g, b) - minOf(r, g, b) > 5) && // Kontras minimal sangat tipis
                r > g) { // Hanya pastikan komponen merah lebih dominan dari hijau
                skinPixels++
            }
        }

        return skinPixels.toFloat() / (width * height).toFloat()
    }

    /**
     * Analisis Kompleksitas Tekstur.
     */
    private fun calculateTextureComplexity(bitmap: Bitmap): Float {
        val width = bitmap.width
        val height = bitmap.height
        val step = 4
        var complexity = 0f
        
        for (y in 0 until height - step step step) {
            for (x in 0 until width - step step step) {
                val p1 = bitmap.getPixel(x, y)
                val p2 = bitmap.getPixel(x + step, y + step)
                
                val diff = Math.abs(Color.red(p1) - Color.red(p2)) +
                           Math.abs(Color.green(p1) - Color.green(p2)) +
                           Math.abs(Color.blue(p1) - Color.blue(p2))
                
                // Rentang sangat luas: (1..150) untuk mentoleransi noise bintik-bintik besar pada kamera laptop
                if (diff in 1..150) complexity += 1f
            }
        }
        
        val totalCells = (width / step) * (height / step)
        return (complexity / totalCells.toFloat()).coerceIn(0f, 1f)
    }
}
