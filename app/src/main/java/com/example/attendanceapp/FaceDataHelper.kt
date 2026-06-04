package com.example.attendanceapp

import android.util.Log
import androidx.compose.ui.geometry.isEmpty
import androidx.compose.ui.tooling.data.position
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceLandmark
import kotlin.math.sqrt

class FaceDataHelper(private val db: AttendanceDatabaseHelper) {

    /**
     * Mengekstrak fitur wajah menjadi vektor (FloatArray).
     * Memperbaiki error posisi agar mengambil PointF asli dari ML Kit.
     */
    fun extractFeatures(face: Face): FloatArray {
        val box = face.boundingBox
        val feat = mutableListOf<Float>()

        // Normalisasi dimensi agar hasil konsisten meski jarak wajah ke kamera berubah
        val width = box.width().toFloat().coerceAtLeast(1f)
        val height = box.height().toFloat().coerceAtLeast(1f)

        // 1. Fitur rotasi kepala (Euler Angles)
        feat.add(width / height) // Aspek rasio wajah
        feat.add(face.headEulerAngleX / 45f)
        feat.add(face.headEulerAngleY / 45f)
        feat.add(face.headEulerAngleZ / 45f)

        // 2. Fitur Landmark wajah
        val targetLandmarks = intArrayOf(
            FaceLandmark.LEFT_EYE,
            FaceLandmark.RIGHT_EYE,
            FaceLandmark.NOSE_BASE,
            FaceLandmark.MOUTH_LEFT,
            FaceLandmark.MOUTH_RIGHT,
            FaceLandmark.MOUTH_BOTTOM,
            FaceLandmark.LEFT_CHEEK,
            FaceLandmark.RIGHT_CHEEK
        )

        for (landmarkType in targetLandmarks) {
            val lm = face.getLandmark(landmarkType)
            if (lm != null) {
                // lm.position di sini adalah android.graphics.PointF (X dan Y)
                // Kita hitung posisi relatif landmark di dalam kotak wajah (0.0 sampai 1.0)
                feat.add((lm.position.x - box.left) / width)
                feat.add((lm.position.y - box.top) / height)
            } else {
                feat.add(0.5f) // Beri nilai tengah jika tidak terdeteksi
                feat.add(0.5f)
            }
        }
        return feat.toFloatArray()
    }

    /**
     * Menghitung skor kemiripan (Cosine Similarity).
     */
    fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        // Jika ukuran vektor berbeda (karena beda versi/sensor), tidak bisa dibanding
        if (a.size != b.size || a.isEmpty()) return 0f

        var dot = 0f
        var nA = 0f
        var nB = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
            nA += a[i] * a[i]
            nB += b[i] * b[i]
        }
        val denom = sqrt(nA.toDouble()) * sqrt(nB.toDouble())
        return if (denom < 1e-6) 0f else (dot / denom).toFloat()
    }

    /**
     * Memverifikasi wajah.
     * Tips: Gunakan threshold 0.85 untuk keamanan tinggi, atau 0.75 jika sulit terdeteksi.
     */
    fun verifyFace(fccode: String, fcba: String, currentFeatures: FloatArray, threshold: Float = 0.75f): Boolean {
        // Ambil data master dari dua tempat (Tabel EMPLOYEE dan Tabel Face Mappings)
        val storedFeaturesList = mutableListOf<FloatArray>()

        // 1. Ambil dari histori mapping
        storedFeaturesList.addAll(db.getFaceFeaturesForEmployee(fccode, fcba))

        if (storedFeaturesList.isEmpty()) {
            Log.w("FaceDataHelper", "DB Kosong: Tidak ada sampel wajah untuk $fccode")
            return false
        }

        var maxScore = -1f
        for (storedFeatures in storedFeaturesList) {
            val score = cosineSimilarity(currentFeatures, storedFeatures)
            if (score > maxScore) maxScore = score
        }

        Log.d("FaceDataHelper", "Verifikasi $fccode: Skor Terjauh = $maxScore (Target: $threshold)")

        return maxScore >= threshold
    }

    fun saveFaceSample(fccode: String, fcba: String, features: FloatArray): Boolean {
        return db.insertFaceMapping(fccode, fcba, features)
    }

    fun getSampleCount(fccode: String, fcba: String): Int {
        return db.getFaceMappingCount(fccode, fcba)
    }

    fun hasEnoughSamples(fccode: String, fcba: String): Boolean {
        return getSampleCount(fccode, fcba) >= 1 // Ubah ke 1 dulu untuk testing
    }
}