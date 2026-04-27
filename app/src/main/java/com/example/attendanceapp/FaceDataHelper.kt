package com.example.attendanceapp

import android.content.Context
import android.content.SharedPreferences
import com.google.mlkit.vision.face.Face
import kotlin.math.sqrt

/**
 * Menyimpan data wajah sebagai vektor fitur (embedding) sederhana
 * berbasis bounding box + landmark ML Kit.
 * Untuk produksi, gunakan TensorFlow Lite face embedding model.
 */
class FaceDataHelper(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("face_data", Context.MODE_PRIVATE)

    data class FaceProfile(
        val employeeId: String,
        val employeeName: String,
        val features: FloatArray // vektor fitur wajah
    )

    // Simpan profil wajah
    fun saveFaceProfile(employeeId: String, employeeName: String, features: FloatArray) {
        val featStr = features.joinToString(",")
        prefs.edit()
            .putString("face_$employeeId", featStr)
            .putString("name_$employeeId", employeeName)
            .putStringSet("employee_ids", getEmployeeIds() + employeeId)
            .apply()
    }

    // Ambil semua profil
    fun getAllProfiles(): List<FaceProfile> {
        return getEmployeeIds().mapNotNull { id ->
            val featStr = prefs.getString("face_$id", null) ?: return@mapNotNull null
            val name = prefs.getString("name_$id", id) ?: id
            val features = featStr.split(",").map { it.toFloat() }.toFloatArray()
            FaceProfile(id, name, features)
        }
    }

    fun getEmployeeIds(): Set<String> =
        prefs.getStringSet("employee_ids", emptySet()) ?: emptySet()

    fun isRegistered(): Boolean = getEmployeeIds().isNotEmpty()

    fun deleteProfile(employeeId: String) {
        val ids = getEmployeeIds().toMutableSet()
        ids.remove(employeeId)
        prefs.edit()
            .remove("face_$employeeId")
            .remove("name_$employeeId")
            .putStringSet("employee_ids", ids)
            .apply()
    }

    // Ekstrak fitur dari Face ML Kit
    fun extractFeatures(face: Face): FloatArray {
        val box = face.boundingBox
        val features = mutableListOf<Float>()

        // Geometri wajah
        features.add(box.width().toFloat())
        features.add(box.height().toFloat())
        val ratio = if (box.height() > 0) box.width().toFloat() / box.height() else 0f
        features.add(ratio)

        // Rotasi
        features.add(face.headEulerAngleX)
        features.add(face.headEulerAngleY)
        features.add(face.headEulerAngleZ)

        // Landmark posisi relatif terhadap bounding box
        val landmarks = face.allLandmarks
        for (landmark in landmarks) {
            val relX = (landmark.position.x - box.left) / box.width().coerceAtLeast(1)
            val relY = (landmark.position.y - box.top) / box.height().coerceAtLeast(1)
            features.add(relX)
            features.add(relY)
        }

        // Kontur wajah (titik-titik tepi wajah)
        val contours = face.allContours
        for (contour in contours) {
            for (point in contour.points) {
                val relX = (point.x - box.left) / box.width().coerceAtLeast(1)
                val relY = (point.y - box.top) / box.height().coerceAtLeast(1)
                features.add(relX)
                features.add(relY)
            }
        }

        return features.toFloatArray()
    }

    // Hitung kemiripan cosine antara dua vektor fitur
    fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        val minLen = minOf(a.size, b.size)
        if (minLen == 0) return 0f

        var dot = 0f
        var normA = 0f
        var normB = 0f
        for (i in 0 until minLen) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        val denom = sqrt(normA) * sqrt(normB)
        return if (denom == 0f) 0f else dot / denom
    }

    // Cari wajah yang paling mirip
    fun findBestMatch(features: FloatArray, threshold: Float = 0.82f): FaceProfile? {
        val profiles = getAllProfiles()
        if (profiles.isEmpty()) return null

        var bestMatch: FaceProfile? = null
        var bestScore = 0f

        for (profile in profiles) {
            val score = cosineSimilarity(features, profile.features)
            if (score > bestScore) {
                bestScore = score
                bestMatch = profile
            }
        }

        return if (bestScore >= threshold) bestMatch else null
    }
}
