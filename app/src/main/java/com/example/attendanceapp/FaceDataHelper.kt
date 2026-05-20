package com.example.attendanceapp

import android.util.Log
import androidx.compose.ui.tooling.data.position
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceLandmark
import kotlin.math.sqrt

class FaceDataHelper(private val db: AttendanceDatabaseHelper) {

    /**
     * Mengekstrak fitur wajah menjadi vektor (FloatArray).
     * Properti 'position' sekarang merujuk ke android.graphics.PointF milik ML Kit.
     */
    fun extractFeatures(face: Face): FloatArray {
        val box = face.boundingBox
        val feat = mutableListOf<Float>()

        // Normalisasi dimensi
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
                // Menghitung posisi relatif landmark terhadap bounding box wajah
                feat.add((lm.position.x - box.left) / width)
                feat.add((lm.position.y - box.top) / height)
            } else {
                // Jika landmark tidak terdeteksi, beri nilai netral
                feat.add(0f)
                feat.add(0f)
            }
        }
        return feat.toFloatArray()
    }

    /**
     * Menghitung skor kemiripan (Cosine Similarity) antara dua vektor.
     * Hasilnya berkisar antara -1.0 hingga 1.0 (1.0 berarti identik).
     */
    fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
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
     * Memverifikasi apakah wajah saat ini cocok dengan sampel di DB.
     * Menggunakan composite key fccode dan fcba.
     */
    fun verifyFace(fccode: String, fcba: String, currentFeatures: FloatArray, threshold: Float = 0.80f): Boolean {
        // Mengambil daftar FloatArray fitur yang tersimpan di DB
        val storedFeaturesList = db.getFaceFeaturesForEmployee(fccode, fcba)

        if (storedFeaturesList.isEmpty()) {
            Log.e("FaceDataHelper", "Verifikasi Gagal: Tidak ada sampel wajah untuk $fccode")
            return false
        }

        var maxScore = 0f
        for (storedFeatures in storedFeaturesList) {
            val score = cosineSimilarity(currentFeatures, storedFeatures)
            if (score > maxScore) maxScore = score
        }

        Log.d("FaceDataHelper", "Hasil Verifikasi $fccode: Skor Tertinggi = $maxScore")

        return maxScore >= threshold
    }

    /**
     * Menyimpan sampel wajah baru.
     */
    fun saveFaceSample(fccode: String, fcba: String, features: FloatArray): Boolean {
        return db.insertFaceMapping(
            FaceMapping(
                employeeId = fccode,
                fcbaId = fcba,
                features = features
            )
        )
    }

    /**
     * Mengambil jumlah sampel wajah dari database helper.
     */
    fun getSampleCount(fccode: String, fcba: String): Int {
        // Pastikan fungsi ini ada di AttendanceDatabaseHelper.kt
        return db.getFaceMappingCount(fccode, fcba)
    }

    /**
     * Mengecek kecukupan sampel wajah (minimal 3 sampel).
     */
    fun hasEnoughSamples(fccode: String, fcba: String): Boolean {
        return getSampleCount(fccode, fcba) >= 3
    }
}
