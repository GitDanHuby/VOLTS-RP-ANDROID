package com.voltsrp.mobile

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipFile

object GameCacheDownloader {

    private const val FILE_ID =
        "1h3-Ld9Ti3HKgBAMFF7NtpfS3NAffpkOC"

    suspend fun downloadAndExtract(
        context: Context,
        onProgress: (Int, String) -> Unit
    ) = withContext(Dispatchers.IO) {

        val base = context.getExternalFilesDir(null)
            ?: throw Exception("Armazenamento indisponível")

        val destination = File(base, "VICE")
        val zipFile = File(base, "volts_rp_cache.zip")

        destination.mkdirs()

        val marker = File(destination, ".volts_cache_complete")

if (
    marker.exists() &&
    File(destination, "gta3.img").exists() &&
    File(destination, "gta_int.img").exists() &&
    File(destination, "main.scm").exists()
) {
    onProgress(100, "ARQUIVOS JÁ INSTALADOS!")
    return@withContext
}

        try {
            onProgress(0, "CONECTANDO AO SERVIDOR...")

            val url = URL(
                "https://drive.usercontent.google.com/download?id=$FILE_ID&export=download&confirm=t"
            )

            val connection =
                url.openConnection() as HttpURLConnection

            connection.connectTimeout = 30000
            connection.readTimeout = 60000
            connection.instanceFollowRedirects = true
            connection.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0"
            )

            connection.connect()

            if (connection.responseCode !in 200..299) {
                throw Exception(
                    "Erro HTTP: ${connection.responseCode}"
                )
            }

            val total = connection.contentLengthLong

            if (total <= 0) {
                throw Exception(
                    "O Google Drive não informou o tamanho do arquivo."
                )
            }

            onProgress(0, "BAIXANDO ARQUIVOS DO GTA SA...")

            BufferedInputStream(connection.inputStream).use { input ->
                FileOutputStream(zipFile).use { output ->

                    val buffer = ByteArray(1024 * 1024)
                    var downloaded = 0L

                    while (true) {
                        val count = input.read(buffer)
                        if (count == -1) break

                        output.write(buffer, 0, count)
                        downloaded += count

                        val percent =
                            ((downloaded * 70) / total).toInt()

                        onProgress(
                            percent.coerceIn(0, 70),
                            "BAIXANDO GTA SA..."
                        )
                    }
                }
            }

            connection.disconnect()

            // Confere se o arquivo baixado realmente é ZIP.
            FileInputStream(zipFile).use {
                val signature = ByteArray(4)
                if (it.read(signature) != 4 ||
                    signature[0] != 0x50.toByte() ||
                    signature[1] != 0x4B.toByte()
                ) {
                    throw Exception(
                        "O Google Drive não entregou um ZIP válido. Verifique o compartilhamento do arquivo."
                    )
                }
            }

            onProgress(70, "VERIFICANDO ARQUIVOS...")

            ZipFile(zipFile).use { zip ->

                val entries = zip.entries()
                var totalBytes = 0L

                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (!entry.isDirectory && entry.size > 0) {
                        totalBytes += entry.size
                    }
                }

                if (totalBytes <= 0) {
                    throw Exception("O ZIP está vazio ou inválido.")
                }

                var extracted = 0L
                val extractionEntries = zip.entries()

                while (extractionEntries.hasMoreElements()) {
                    val entry = extractionEntries.nextElement()

                    val outputFile = File(destination, entry.name)
                    val safePath = outputFile.canonicalPath
                    val rootPath = destination.canonicalPath +
                        File.separator

                    if (safePath != destination.canonicalPath &&
                        !safePath.startsWith(rootPath)
                    ) {
                        throw Exception(
                            "O ZIP contém um caminho inválido."
                        )
                    }

                    if (entry.isDirectory) {
                        outputFile.mkdirs()
                    } else {
                        outputFile.parentFile?.mkdirs()

                        zip.getInputStream(entry).use { input ->
                            FileOutputStream(outputFile).use { output ->
                                val buffer = ByteArray(1024 * 1024)

                                while (true) {
                                    val count = input.read(buffer)
                                    if (count == -1) break

                                    output.write(buffer, 0, count)
                                    extracted += count

                                    val percent =
                                        70 + ((extracted * 30) /
                                            totalBytes).toInt()

                                    onProgress(
                                        percent.coerceIn(70, 100),
                                        "EXTRAINDO ARQUIVOS..."
                                    )
                                }
                            }
                        }
                    }
                }
            }

            onProgress(100, "ARQUIVOS EXTRAÍDOS!")

            marker.writeText("VOLTS_CACHE_OK")

        } catch (error: Exception) {
            zipFile.delete()
            throw error
        }

        zipFile.delete()
    }
}
