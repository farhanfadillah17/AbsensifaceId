package com.example.attendanceapp

import android.annotation.SuppressLint
import android.app.AlertDialog
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
import java.text.SimpleDateFormat
import java.util.*

object PrintHelper {

    private val PRINTER_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
    private const val MAX_PRINTER_WIDTH = 384

    @SuppressLint("MissingPermission")
    fun printDirect(context: Context, item: Map<String, String>) {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Toast.makeText(context, "Bluetooth tidak aktif!", Toast.LENGTH_SHORT).show()
            return
        }

        val pairedDevices: Set<BluetoothDevice> = bluetoothAdapter.bondedDevices

        if (pairedDevices.isEmpty()) {
            Toast.makeText(context, "Tidak ada printer Bluetooth terpasang. Silakan pairing terlebih dahulu.", Toast.LENGTH_LONG).show()
            return
        }

        val deviceList = pairedDevices.toList()
        val deviceNames = deviceList.map { "${it.name ?: "Unknown"}\n${it.address}" }.toTypedArray()

        AlertDialog.Builder(context)
            .setTitle("Pilih Printer Bluetooth")
            .setItems(deviceNames) { _, which ->
                startPrintJob(context, deviceList[which], item)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    @SuppressLint("MissingPermission")
    private fun startPrintJob(context: Context, printer: BluetoothDevice, item: Map<String, String>) {
        val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

        Thread {
            var socket: BluetoothSocket? = null
            var out: OutputStream? = null

            try {
                mainHandler.post { Toast.makeText(context, "Menghubungkan ke ${printer.name}...", Toast.LENGTH_SHORT).show() }

                socket = printer.createRfcommSocketToServiceRecord(PRINTER_UUID)
                socket.connect()
                out = socket.outputStream

                // 1. Reset Printer
                out.write(byteArrayOf(0x1B, 0x40))
                Thread.sleep(200)

                // 2. Judul Adaptif
                val isFruitCounting = item.containsKey("tph_code") && !item.containsKey("spb_no")
                val isSPB = item.containsKey("spb_no") // Deteksi apakah ini SPB

                val title = when {
                    isSPB -> "SURAT PENGANTAR BARANG"
                    isFruitCounting -> "BUKTI HITUNG BUAH"
                    else -> "BUKTI RENCANA KERJA"
                }

                out.write(byteArrayOf(0x1B, 0x61, 0x01)) // Center
                out.write(byteArrayOf(0x1B, 0x21, 0x08)) // Bold
                out.write("$title\n".toByteArray())
                out.write(byteArrayOf(0x1B, 0x21, 0x00)) // Normal
                out.write("--------------------------------\n".toByteArray())

                // 3. Isi Data
                out.write(byteArrayOf(0x1B, 0x61, 0x00)) // Left Align
                val format = "%-12s: %s\n"

                if (isSPB) {
                    // --- KHUSUS DATA SPB ---
                    out.write(String.format(format, "NO SPB", item["spb_no"] ?: "-").toByteArray())
                    out.write(String.format(format, "MILL", item["mill_code"] ?: "-").toByteArray())
                    out.write(String.format(format, "KENDARAAN", item["vehicle_code"] ?: "-").toByteArray())
                    out.write(String.format(format, "LOKASI", item["location_code"] ?: "-").toByteArray())
                    out.write(String.format(format, "KODE TPH", item["tph_code"] ?: "-").toByteArray())
                    out.write(String.format(format, "UNIT", item["unit"] ?: "Jjg").toByteArray())


                } else {
                    // --- DATA RKH / TPH (Existing) ---
                    val isTPH = item.containsKey("tph_code")
                    val noRkh = item["no_rkh"] ?: "-"
                    val tphCode = item["tph_code"] ?: "-"
                    val job = item["job_code"] ?: (if (isTPH) "HITUNG BUAH" else "-")
                    val loc = item["location_code"] ?: "-"
                    val unit = item["unit"] ?: "-"
                    val output = item["output"] ?: "0"
                    val hk = item["jumlah_hk"] ?: "0"

                    out.write(String.format(format, "NO RKH", noRkh).toByteArray())
                    if (isTPH) out.write(String.format(format, "KODE TPH", tphCode).toByteArray())
                    out.write(String.format(format, "JOB", job).toByteArray())
                    out.write(String.format(format, "LOKASI", loc).toByteArray())
                    out.write(String.format(format, "UNIT", unit).toByteArray())
                    out.write(String.format(format, "HASIL/TGT", output).toByteArray())

                    if (hk != "0" && !isTPH) {
                        out.write(String.format(format, "HK", hk).toByteArray())
                    }
                }

                out.write("--------------------------------\n".toByteArray())
                out.flush()
                Thread.sleep(300)

                // 4. QR CODE (Adaptif)
                val qrData = when {
                    isSPB -> "SPB:${item["spb_no"]}|MIL:${item["mill_code"]}|VEH:${item["vehicle_code"]}|LOC:${item["location_code"]}|TPH:${item["tph_code"]}|UNIT:${item["unit"]}"
                    isFruitCounting -> "RKH:${item["no_rkh"]}|TPH:${item["tph_code"]}|UNIT:${item["unit"]}|OUT:${item["output"]}"
                    else -> "RKH:${item["no_rkh"]}|JOB:${item["job_code"]}|UNIT:${item["unit"]}|OUT:${item["output"]}|HK:${item["jumlah_hk"]}"
                }

                val bitmap = generateQRCodeFullWidth(qrData)

                out.write(byteArrayOf(0x1B, 0x61, 0x01)) // Center
                printBitmapRaster(out, bitmap)

                Thread.sleep(500)
                out.write("\nScan untuk Verifikasi\n".toByteArray())

                // 5. Footer
                val date = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()).format(Date())
                out.write("Printer: ${printer.name}\n".toByteArray())
                out.write("Waktu  : $date\n".toByteArray())
                out.write("\n\n\n\n\n".toByteArray()) // Feed paper

                out.flush()
                Thread.sleep(1000)

                mainHandler.post { Toast.makeText(context, "Cetak Berhasil!", Toast.LENGTH_SHORT).show() }

            } catch (e: Exception) {
                e.printStackTrace()
                mainHandler.post { Toast.makeText(context, "Gagal: ${e.localizedMessage}", Toast.LENGTH_LONG).show() }
            } finally {
                try {
                    out?.close()
                    socket?.close()
                } catch (e: Exception) { }
            }
        }.start()
    }

    private fun generateQRCodeFullWidth(content: String): Bitmap {
        val hints = HashMap<com.google.zxing.EncodeHintType, Any>()
        hints[com.google.zxing.EncodeHintType.ERROR_CORRECTION] = com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.L
        hints[com.google.zxing.EncodeHintType.MARGIN] = 1
        hints[com.google.zxing.EncodeHintType.CHARACTER_SET] = "UTF-8"

        val matrix: BitMatrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0, hints)
        val rawWidth = matrix.width
        val scale = MAX_PRINTER_WIDTH / rawWidth
        val finalSize = rawWidth * scale

        val bitmap = Bitmap.createBitmap(finalSize, finalSize, Bitmap.Config.RGB_565)
        for (y in 0 until finalSize) {
            for (x in 0 until finalSize) {
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

        out.write(byteArrayOf(0x1D, 0x76, 0x30, 0x00))
        out.write(byteArrayOf((bwWidth % 256).toByte(), (bwWidth / 256).toByte()))
        out.write(byteArrayOf((height % 256).toByte(), (height / 256).toByte()))
        out.write(data)
    }
}