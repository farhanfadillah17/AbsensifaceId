package com.example.attendanceapp

import android.graphics.*
import android.media.Image
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream

object ImageUtils {

    /**
     * Mengonversi ImageProxy (dari CameraX) menjadi Bitmap yang dipotong (crop) sesuai bounding box wajah.
     */
    fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        val planeProxy = imageProxy.planes[0]
        val buffer = planeProxy.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        
        // Rotasi bitmap sesuai orientasi kamera
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        if (rotationDegrees == 0) return bitmap
        
        val matrix = Matrix()
        matrix.postRotate(rotationDegrees.toFloat())
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * Memotong Bitmap berdasarkan bounding box wajah.
     */
    fun cropFace(bitmap: Bitmap, boundingBox: Rect): Bitmap {
        // Beri margin sedikit agar tekstur sekitar wajah ikut teranalisis
        val margin = (boundingBox.width() * 0.1f).toInt()
        val left = (boundingBox.left - margin).coerceAtLeast(0)
        val top = (boundingBox.top - margin).coerceAtLeast(0)
        val width = (boundingBox.width() + 2 * margin).coerceAtMost(bitmap.width - left)
        val height = (boundingBox.height() + 2 * margin).coerceAtMost(bitmap.height - top)
        
        return Bitmap.createBitmap(bitmap, left, top, width, height)
    }

    /**
     * Alternatif konversi yang lebih cepat untuk format YUV_420_888 (Standar CameraX)
     */
    fun yuv420ToBitmap(imageProxy: ImageProxy): Bitmap? {
        val image = imageProxy.image ?: return null
        val nv21 = yuv420ToNv21(image)
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 90, out)
        val imageBytes = out.toByteArray()
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        if (rotationDegrees == 0) return bitmap

        val matrix = Matrix()
        matrix.postRotate(rotationDegrees.toFloat())
        // Jika kamera depan, flip horizontal untuk koreksi mirror
        if (imageProxy.imageInfo.rotationDegrees == 270 || imageProxy.imageInfo.rotationDegrees == 90) {
            // Bergantung pada implementasi LensFacing, biasanya perlu flip
        }
        
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun yuv420ToNv21(image: Image): ByteArray {
        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        return nv21
    }
}
