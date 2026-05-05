package com.example.attendanceapp

import android.util.Log
import androidx.compose.ui.tooling.data.position
// PENTING: Hapus import androidx.compose.ui.tooling.data.position
// Import yang benar untuk landmark posisi adalah:
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceLandmark
import kotlin.math.sqrt

class FaceDataHelper(private val db: AttendanceDatabaseHelper) {

    fun extractFeatures(face: Face): FloatArray {
        val box = face.boundingBox
        val feat = mutableListOf<Float>()

        val width = box.width().toFloat().coerceAtLeast(1f)
        val height = box.height().toFloat().coerceAtLeast(1f)

        feat.add(width / height)
        feat.add(face.headEulerAngleX / 45f)
        feat.add(face.headEulerAngleY / 45f)
        feat.add(face.headEulerAngleZ / 45f)

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
                // Gunakan lm.position (PointF dari ML Kit), bukan import Compose
                feat.add((lm.position.x - box.left) / width)
                feat.add((lm.position.y - box.top) / height)
            } else {
                feat.add(0f)
                feat.add(0f)
            }
        }
        return feat.toFloatArray()
    }

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

    fun findBestMatch(features: FloatArray, threshold: Float = 0.92f): Employee? {
        val allMappings = db.getAllFaceMappings()
        if (allMappings.isEmpty()) return null

        val scoreMap = mutableMapOf<String, MutableList<Float>>()
        for (mapping in allMappings) {
            val score = cosineSimilarity(features, mapping.features)
            scoreMap.getOrPut(mapping.employeeId) { mutableListOf() }.add(score)
        }

        var bestId = ""
        var maxAvgScore = 0f

        for ((empId, scores) in scoreMap) {
            val avg = scores.average().toFloat()
            if (avg > maxAvgScore) {
                maxAvgScore = avg
                bestId = empId
            }
        }

        Log.d("FaceDataHelper", "Best Match ID: $bestId, Score: $maxAvgScore")
        return if (maxAvgScore >= threshold) db.getEmployee(bestId) else null
    }

    fun saveFaceSample(employeeId: String, features: FloatArray): Boolean {
        return db.insertFaceMapping(
            FaceMapping(employeeId = employeeId, features = features)
        )
    }

    fun verifyFace(employeeId: String, currentFeatures: FloatArray, threshold: Float = 0.85f): Boolean {
        // 1. Ambil semua sampel wajah yang terdaftar untuk ID ini
        val storedMappings = db.getAllFaceMappings().filter { it.employeeId == employeeId }

        if (storedMappings.isEmpty()) {
            Log.e("FaceDataHelper", "Tidak ada data wajah untuk ID: $employeeId")
            return false
        }

        // 2. Bandingkan wajah saat ini dengan setiap sampel yang tersimpan
        var maxScore = 0f
        for (mapping in storedMappings) {
            val score = cosineSimilarity(currentFeatures, mapping.features)
            if (score > maxScore) {
                maxScore = score
            }
        }

        Log.d("FaceDataHelper", "Verifikasi ID: $employeeId, Skor Tertinggi: $maxScore")

        // 3. Jika skor tertinggi melewati ambang batas, maka dianggap cocok
        return maxScore >= threshold
    }

    fun getSampleCount(employeeId: String) = db.getFaceMappingCount(employeeId)

    fun hasEnoughSamples(employeeId: String) = db.hasEnoughFaceMappings(employeeId, 3)
}