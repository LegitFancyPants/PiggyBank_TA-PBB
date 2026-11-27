package com.example.piggybank.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

suspend fun writeExcelBytesToUri(
    context: Context,
    uri: Uri,
    bytes: ByteArray
): Result<Unit> = withContext(Dispatchers.IO) {
    runCatching {
        val outputStream = context.contentResolver.openOutputStream(uri)
            ?: throw IOException("Tidak dapat membuka lokasi tujuan file.")
        outputStream.use { stream ->
            stream.write(bytes)
            stream.flush()
        }
    }
}

suspend fun writeExcelBytesToCacheForShare(
    context: Context,
    fileName: String,
    bytes: ByteArray
): Result<Uri> = withContext(Dispatchers.IO) {
    runCatching {
        val exportDir = File(context.cacheDir, "exports").apply {
            if (!exists()) mkdirs()
        }
        // Clean up older temporary exported files so cache does not grow unbounded
        exportDir.listFiles()?.forEach { existing ->
            if (existing.isFile && existing.name.endsWith(".xlsx")) {
                existing.delete()
            }
        }
        val safeName = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val outFile = File(exportDir, safeName)
        outFile.writeBytes(bytes)
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            outFile
        )
    }
}
