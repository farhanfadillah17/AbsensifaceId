package com.example.attendanceapp

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

object PrintHelper {

    private val PRINTER_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")

    // Standar printer 58mm: 384 dots, area cetak aman 360-384
    private const val MAX_PRINTER_WIDTH = 384
    private const val LINE_CHARS = 32 // Standar karakter per baris

    @SuppressLint("MissingPermission")
    fun printDirect(context: Context, item: Map<String, String>) {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Toast.makeText(context, "Bluetooth tidak aktif!", Toast.LENGTH_SHORT).show()
            return
        }

        val pairedDevices = bluetoothAdapter.bondedDevices
        if (pairedDevices.isEmpty()) {
            Toast.makeText(context, "Printer belum dipasangkan.", Toast.LENGTH_LONG).show()
            return
        }

        val deviceList = pairedDevices.toList()
        val deviceNames = deviceList.map { "${it.name ?: "Unknown"}\n${it.address}" }.toTypedArray()

        AlertDialog.Builder(context)
            .setTitle("Pilih Printer Bluetooth")
            .setItems(deviceNames) { _, which ->
                Thread {
                    executePrintLogic(context, deviceList[which], item)
                }.start()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    @SuppressLint("MissingPermission")
    fun printMultiple(context: Context, items: List<Map<String, String>>) {
        if (items.isEmpty()) return
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) return

        val pairedDevices = bluetoothAdapter.bondedDevices
        if (pairedDevices.isEmpty()) return

        val deviceList = pairedDevices.toList()
        val deviceNames = deviceList.map { "${it.name ?: "Unknown"}\n${it.address}" }.toTypedArray()

        AlertDialog.Builder(context)
            .setTitle("Cetak ${items.size} Data Massal")
            .setItems(deviceNames) { _, which ->
                val selectedPrinter = deviceList[which]
                Thread {
                    val mainHandler = Handler(Looper.getMainLooper())
                    try {
                        items.forEachIndexed { index, item ->
                            mainHandler.post {
                                Toast.makeText(context, "Mencetak (${index + 1}/${items.size})...", Toast.LENGTH_SHORT).show()
                            }
                            executePrintLogic(context, selectedPrinter, item)

                            // Jeda 7 detik antar struk agar printer benar-benar siap (Sangat Penting)
                            Thread.sleep(7000)
                        }
                        mainHandler.post { Toast.makeText(context, "Selesai!", Toast.LENGTH_SHORT).show() }
                    } catch (e: Exception) {
                        mainHandler.post { Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show() }
                    }
                }.start()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    @SuppressLint("MissingPermission")
    private fun executePrintLogic(context: Context, printer: BluetoothDevice, item: Map<String, String>) {
        var socket: BluetoothSocket? = null
        var out: OutputStream? = null

        try {
            socket = printer.createRfcommSocketToServiceRecord(PRINTER_UUID)
            socket.connect()
            out = socket.outputStream

            // 1. Inisialisasi: Jeda 1 detik agar baud rate stabil
            Thread.sleep(1000)
            out.write(byteArrayOf(0x1B, 0x40)) // Reset printer
            Thread.sleep(100)

            // Set Line Spacing ke 30 dots agar tidak terlalu renggang/berantakan
            out.write(byteArrayOf(0x1B, 0x33, 0x1E))

            val isSPB = item.containsKey("spb_no")
            val title = if (isSPB) "SURAT PENGANTAR BARANG" else "BUKTI RENCANA KERJA"

            // 2. Header (Center + Bold)
            out.write(byteArrayOf(0x1B, 0x61, 0x01)) // Center
            out.write(byteArrayOf(0x1B, 0x21, 0x08)) // Bold
            sendText(out, "$title\n")

            out.write(byteArrayOf(0x1B, 0x21, 0x00)) // Normal
            sendText(out, "--------------------------------\n")

            // 3. Body (Left Align)
            out.write(byteArrayOf(0x1B, 0x61, 0x00))
            if (isSPB) {
                out.write(formatRow("NO SPB", item["spb_no"]))
                out.write(formatRow("MILL", item["mill_code"]))
                out.write(formatRow("UNIT", "${item["unit"]} Jjg"))
                out.write(formatRow("LOKASI", item["location_code"]))
            } else {
                out.write(formatRow("NO RKH", item["no_rkh"]))
                if (item.containsKey("tph_code")) out.write(formatRow("TPH", item["tph_code"]))
                out.write(formatRow("JOB", item["job_code"]))
                out.write(formatRow("HASIL", "${item["output"]} ${item["unit"]}"))
            }

            sendText(out, "--------------------------------\n")
            out.flush()
            Thread.sleep(500)

            // 4. QR Code
            val qrData = if (isSPB) "SPB:${item["spb_no"]}" else "RKH:${item["no_rkh"]}"
            val bitmap = generateQRCodeFullWidth(qrData)
            out.write(byteArrayOf(0x1B, 0x61, 0x01)) // Center
            printBitmapRaster(out, bitmap)

            // 5. Footer
            val date = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()).format(Date())
            out.write(byteArrayOf(0x1B, 0x61, 0x01))
            sendText(out, "\n$date\n")

            // Feed paper
            sendText(out, "\n\n\n\n\n")

            out.flush()
            Thread.sleep(500)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try { out?.close(); socket?.close() } catch (e: Exception) {}
        }
    }

    // Helper untuk mengirim teks dengan encoding GBK (Mencegah karakter aneh)
    private fun sendText(out: OutputStream, text: String) {
        try {
            out.write(text.toByteArray(charset("GBK")))
        } catch (e: Exception) {
            out.write(text.toByteArray())
        }
    }

    private fun formatRow(label: String, value: String?): ByteArray {
        val valStr = value ?: "-"
        val labelWidth = 11
        val formattedLabel = if (label.length > labelWidth) label.substring(0, labelWidth) else label.padEnd(labelWidth)

        // Sisa karakter (32 - 11 label - 2 titik dua spasi = 19 karakter sisa)
        val maxValWidth = LINE_CHARS - labelWidth - 2
        val formattedValue = if (valStr.length > maxValWidth) valStr.substring(0, maxValWidth) else valStr

        val row = "$formattedLabel: $formattedValue\n"
        return try { row.toByteArray(charset("GBK")) } catch (e: Exception) { row.toByteArray() }
    }

    private fun generateQRCodeFullWidth(content: String): Bitmap {
        val hints = mutableMapOf<EncodeHintType, Any>()
        hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.M
        hints[EncodeHintType.MARGIN] = 0

        val matrix: BitMatrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, 200, 200, hints)
        val bitmap = Bitmap.createBitmap(matrix.width, matrix.height, Bitmap.Config.RGB_565)
        for (y in 0 until matrix.height) {
            for (x in 0 until matrix.width) {
                bitmap.setPixel(x, y, if (matrix.get(x, y)) Color.BLACK else Color.WHITE)
            }
        }
        // Paksa ke 384 dot agar full lebar kertas
        return Bitmap.createScaledBitmap(bitmap, MAX_PRINTER_WIDTH, MAX_PRINTER_WIDTH, false)
    }

    private fun printBitmapRaster(out: OutputStream, bitmap: Bitmap) {
        val width = bitmap.width
        val height = bitmap.height
        val bwWidth = (width + 7) / 8

        val rowBatch = 24
        for (i in 0 until height step rowBatch) {
            val currentBatchHeight = if (i + rowBatch > height) height - i else rowBatch
            out.write(byteArrayOf(0x1D, 0x76, 0x30, 0x00))
            out.write(byteArrayOf((bwWidth % 256).toByte(), (bwWidth / 256).toByte()))
            out.write(byteArrayOf((currentBatchHeight % 256).toByte(), (currentBatchHeight / 256).toByte()))

            val data = ByteArray(bwWidth * currentBatchHeight)
            for (y in 0 until currentBatchHeight) {
                for (x in 0 until width) {
                    if (Color.red(bitmap.getPixel(x, i + y)) < 128) {
                        data[y * bwWidth + x / 8] = (data[y * bwWidth + x / 8].toInt() or (0x80 shr (x % 8))).toByte()
                    }
                }
            }
            out.write(data)
            out.flush()
            Thread.sleep(100)
        }
    }
}