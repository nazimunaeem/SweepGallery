package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL

sealed interface ApkDownloadResult {
    data class Success(val apkFile: File) : ApkDownloadResult
    data class Failure(val message: String) : ApkDownloadResult
}

object InAppUpdater {

    private const val UPDATES_DIR = "updates"
    private const val MAX_REDIRECTS = 5

    fun canRequestPackageInstalls(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching { context.packageManager.canRequestPackageInstalls() }.getOrDefault(false)
        } else {
            true
        }
    }

    fun buildManageUnknownSourcesIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.fromParts("package", context.packageName, null),
            )
        } else {
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null),
            )
        }
    }

    fun buildInstallApkIntent(context: Context, apkFile: File): Intent {
        val authority = "${context.packageName}.fileprovider"
        val contentUri = FileProvider.getUriForFile(context, authority, apkFile)
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun targetApkFile(context: Context, tagName: String): File {
        val dir = File(context.cacheDir, UPDATES_DIR).apply { mkdirs() }
        val sanitized = tagName.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "latest" }
        return File(dir, "SweepGallery-$sanitized.apk")
    }

    fun isValidApkFile(file: File, expectedSizeBytes: Long = 0L): Boolean {
        if (!file.exists() || !file.isFile || file.length() < 4096L) return false
        if (expectedSizeBytes > 0L && file.length() != expectedSizeBytes) return false
        return runCatching {
            RandomAccessFile(file, "r").use { raf ->
                val b0 = raf.read()
                val b1 = raf.read()
                b0 == 0x50 && b1 == 0x4B // 'PK' ZIP/APK magic header
            }
        }.getOrDefault(false)
    }

    suspend fun downloadApk(
        context: Context,
        release: GitHubReleaseInfo,
        userAgentVersion: String,
        onProgress: (bytesDownloaded: Long, totalBytes: Long) -> Unit,
    ): ApkDownloadResult = withContext(Dispatchers.IO) {
        val downloadUrl = release.apkDownloadUrl
            ?.takeIf { it.isNotBlank() }
            ?: return@withContext ApkDownloadResult.Failure("No APK asset found on this release")

        val destFile = targetApkFile(context, release.tagName)
        if (isValidApkFile(destFile, release.apkSizeBytes)) {
            onProgress(destFile.length(), destFile.length())
            return@withContext ApkDownloadResult.Success(destFile)
        }

        // Remove older cached APK files before downloading the new one
        destFile.parentFile?.listFiles()?.forEach { oldFile ->
            if (oldFile.name != destFile.name) {
                runCatching { oldFile.delete() }
            }
        }

        val tempFile = File(destFile.parentFile, "${destFile.name}.part")
        var connection: HttpURLConnection? = null
        try {
            connection = openFollowingRedirects(downloadUrl, userAgentVersion)
            val code = connection.responseCode
            if (code !in 200..299) {
                return@withContext ApkDownloadResult.Failure("Download failed (HTTP $code)")
            }

            val totalBytes = connection.contentLengthLong.takeIf { it > 0L }
                ?: release.apkSizeBytes.takeIf { it > 0L }
                ?: -1L

            var downloaded = 0L
            onProgress(0L, totalBytes)

            connection.inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    var lastReportTime = 0L
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        val now = System.currentTimeMillis()
                        if (now - lastReportTime >= 100L || (totalBytes > 0L && downloaded >= totalBytes)) {
                            lastReportTime = now
                            onProgress(downloaded, totalBytes)
                        }
                    }
                    output.flush()
                }
            }

            if (!isValidApkFile(tempFile)) {
                runCatching { tempFile.delete() }
                return@withContext ApkDownloadResult.Failure("Downloaded file is not a valid APK")
            }

            if (destFile.exists()) destFile.delete()
            if (!tempFile.renameTo(destFile)) {
                tempFile.copyTo(destFile, overwrite = true)
                tempFile.delete()
            }

            onProgress(destFile.length(), destFile.length())
            ApkDownloadResult.Success(destFile)
        } catch (ce: CancellationException) {
            runCatching { tempFile.delete() }
            throw ce
        } catch (t: Throwable) {
            runCatching { tempFile.delete() }
            ApkDownloadResult.Failure(t.localizedMessage ?: "Failed to download APK")
        } finally {
            connection?.disconnect()
        }
    }

    private fun openFollowingRedirects(startUrl: String, userAgentVersion: String): HttpURLConnection {
        var currentUrl = startUrl
        repeat(MAX_REDIRECTS) {
            val conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 30_000
                setRequestProperty("Accept", "application/octet-stream")
                setRequestProperty("User-Agent", "SweepGallery-Android/$userAgentVersion")
            }
            val code = conn.responseCode
            if (code == HttpURLConnection.HTTP_MOVED_PERM ||
                code == HttpURLConnection.HTTP_MOVED_TEMP ||
                code == HttpURLConnection.HTTP_SEE_OTHER ||
                code == 307 ||
                code == 308
            ) {
                val location = conn.getHeaderField("Location")
                conn.disconnect()
                if (location.isNullOrBlank()) {
                    throw IllegalStateException("Redirect missing Location header")
                }
                currentUrl = URL(URL(currentUrl), location).toString()
            } else {
                return conn
            }
        }
        throw IllegalStateException("Too many redirects while downloading APK")
    }
}
