package com.example.attendanceapp

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.widget.Toast


import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import java.io.OutputStream
import java.util.*


object PrintHelper {

    // UUID standar untuk printer Bluetooth SPP
    private val PRINTER_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")

    @SuppressLint("MissingPermission")
    fun printDirect(context: Context, item: Map<String, String>) {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Toast.makeText(context, "Bluetooth tidak aktif!", Toast.LENGTH_SHORT).show()
            return
        }

        val pairedDevices: Set<BluetoothDevice> = bluetoothAdapter.bondedDevices
        val printer = pairedDevices.find { device ->
            val name = device.name ?: ""
            name.contains("RPP02N", true) || name.contains("Thermal", true) || name.contains("MPT", true)
        }

        if (printer == null) {
            Toast.makeText(context, "Printer RPP02N tidak ditemukan!", Toast.LENGTH_LONG).show()
            return
        }

        val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

        Thread {
            var socket: BluetoothSocket? = null
            var out: OutputStream? = null

            try {
                mainHandler.post { Toast.makeText(context, "Menghubungkan...", Toast.LENGTH_SHORT).show() }

                socket = printer.createRfcommSocketToServiceRecord(PRINTER_UUID)
                socket.connect()
                out = socket.outputStream

                // 1. Inisialisasi & Reset Printer
                out.write(byteArrayOf(0x1B, 0x40))
                Thread.sleep(200) // Jeda lebih lama agar printer siap

                // 2. Judul (Bold + Center)
                out.write(byteArrayOf(0x1B, 0x61, 0x01)) // Center
                out.write(byteArrayOf(0x1B, 0x21, 0x08)) // Bold On
                out.write("BUKTI RENCANA KERJA\n".toByteArray())
                out.write(byteArrayOf(0x1B, 0x21, 0x00)) // Bold Off
                out.write("--------------------------------\n".toByteArray())

                // 3. Isi Data (Gunakan format 32 karakter untuk printer 58mm)
                out.write(byteArrayOf(0x1B, 0x61, 0x00)) // Left
                val format = "%-10s: %s\n"
                val noRkh = item["no_rkh"] ?: "-"
                val job = item["job_code"] ?: "-"
                val hk = item["jumlah_hk"] ?: "0"
                val output = item["output"] ?: "0"



                // Jeda sebelum cetak gambar (Sangat Penting untuk Printer Thermal murah)
                out.flush()
                Thread.sleep(300)

                // 4. QR CODE (Gunakan Ukuran 180 agar pasti muat di tengah)
                val qrData = "RKH:$noRkh|Job:$job|HK:$hk|Out:$output"
                val bitmap = generateQRCodeFullWidth(qrData)

                out.write(byteArrayOf(0x1B, 0x61, 0x01)) // Center
                printBitmapRaster(out, bitmap)

                // Jeda setelah cetak gambar agar printer tidak kaget
                Thread.sleep(500)

                out.write("\nScan untuk Verifikasi\n".toByteArray())

                // 5. Footer & Spasi Potong Kertas
                val date = java.text.SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()).format(Date())
                out.write("Dicetak: $date\n".toByteArray())

                // Beri spasi banyak di akhir agar tidak terpotong saat disobek
                out.write("\n\n\n\n\n".toByteArray())

                out.flush()
                Thread.sleep(1000) // Jeda 1 detik sebelum tutup koneksi

                mainHandler.post { Toast.makeText(context, "Cetak Berhasil", Toast.LENGTH_SHORT).show() }

            } catch (e: Exception) {
                e.printStackTrace()
                mainHandler.post {
                    Toast.makeText(context, "Gagal Cetak: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            } finally {
                try {
                    out?.close()
                    socket?.close()
                } catch (e: Exception) { e.printStackTrace() }
            }
        }.start()
    }

    // Standar printer thermal 58mm memiliki 384 titik horizontal
    private const val MAX_PRINTER_WIDTH = 384

    private fun generateQRCodeFullWidth(content: String): Bitmap {
        val hints = HashMap<com.google.zxing.EncodeHintType, Any>()
        // Level L membuat kotak QR lebih sedikit & lebih besar (bagus untuk thermal)
        hints[com.google.zxing.EncodeHintType.ERROR_CORRECTION] = com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.L
        hints[com.google.zxing.EncodeHintType.MARGIN] = 1
        hints[com.google.zxing.EncodeHintType.CHARACTER_SET] = "UTF-8"

        // 1. Dapatkan matrix terkecil dari data
        val matrix: BitMatrix = MultiFormatWriter().encode(
            content, BarcodeFormat.QR_CODE, 0, 0, hints
        )

        val rawWidth = matrix.width

        // 2. Hitung skala (Berapa kali lipat agar mendekati 384 pixel)
        // Kita gunakan pembagian bulat agar setiap kotak QR memiliki ukuran pixel yang sama (tajam)
        val scale = MAX_PRINTER_WIDTH / rawWidth
        val finalSize = rawWidth * scale

        // 3. Buat Bitmap dengan skala manual (Nearest Neighbor)
        val bitmap = Bitmap.createBitmap(finalSize, finalSize, Bitmap.Config.RGB_565)

        for (y in 0 until finalSize) {
            for (x in 0 until finalSize) {
                // Ambil data dari matrix mentah berdasarkan skala
                val isBlack = matrix.get(x / scale, y / scale)
                bitmap.setPixel(x, y, if (isBlack) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }


    private fun printBitmapRaster(out: OutputStream, bitmap: Bitmap) {
        val width = bitmap.width
        val height = bitmap.height
        val bwWidth = (width + 7) / 8
        val data = ByteArray(bwWidth * height)

        for (y in 0 until height) {
            for (x in 0 until width) {
                if (Color.red(bitmap.getPixel(x, y)) < 128) {
                    data[y * bwWidth + x / 8] = (data[y * bwWidth + x / 8].toInt() or (0x80 shr (x % 8))).toByte()
                }
            }
        }

        // Perintah GS v 0 (Mode Raster paling stabil)
        out.write(byteArrayOf(0x1D, 0x76, 0x30, 0x00))
        out.write(byteArrayOf((bwWidth % 256).toByte(), (bwWidth / 256).toByte()))
        out.write(byteArrayOf((height % 256).toByte(), (height / 256).toByte()))
        out.write(data)
    }
}