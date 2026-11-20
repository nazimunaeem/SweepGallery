package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class GitHubReleaseInfo(
    val tagName: String,
    val title: String,
    val notes: String,
    val htmlUrl: String,
    val apkDownloadUrl: String?,
)

sealed interface UpdateCheckResult {
    data class UpdateAvailable(val release: GitHubReleaseInfo) : UpdateCheckResult
    data class UpToDate(val latestTag: String?) : UpdateCheckResult
    data class Error(val message: String) : UpdateCheckResult
}

object GitHubUpdateChecker {

    fun normalizeRepoSlug(rawInput: String): String {
        val trimmed = rawInput.trim().removeSuffix("/")
        val withoutPrefix = trimmed
            .removePrefix("https://github.com/")
            .removePrefix("http://github.com/")
            .removeSuffix(".git")
        val parts = withoutPrefix.split("/").filter { it.isNotBlank() }
        return if (parts.size >= 2) "${parts[0]}/${parts[1]}" else ""
    }

    fun isNewerVersion(remoteTag: String, currentVersion: String): Boolean {
        val remoteParts = parseVersionParts(remoteTag)
        val currentParts = parseVersionParts(currentVersion)
        if (remoteParts.isEmpty() || currentParts.isEmpty()) return false

        val maxLen = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }

    private fun parseVersionParts(version: String): List<Int> {
        val cleaned = version.trim().removePrefix("v").removePrefix("V")
            .substringBefore("-")
            .substringBefore("+")
        return cleaned.split(".")
            .mapNotNull { part -> part.filter { it.isDigit() }.toIntOrNull() }
    }

    suspend fun checkLatestRelease(
        repoInput: String,
        currentVersionName: String,
    ): UpdateCheckResult = withContext(Dispatchers.IO) {
        val slug = normalizeRepoSlug(repoInput)
        if (slug.isEmpty()) {
            return@withContext UpdateCheckResult.Error("Enter a valid GitHub repo (owner/repo)")
        }

        var connection: HttpURLConnection? = null
        try {
            val url = URL("https://api.github.com/repos/$slug/releases/latest")
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "TindPhoto-Android/$currentVersionName")
            }

            val code = connection.responseCode
            if (code == HttpURLConnection.HTTP_NOT_FOUND) {
                return@withContext UpdateCheckResult.UpToDate(latestTag = null)
            }
            if (code !in 200..299) {
                return@withContext UpdateCheckResult.Error("GitHub check failed (HTTP $code)")
            }

            val bodyText = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(bodyText)
            val tagName = json.optString("tag_name", "").trim()
            val htmlUrl = json.optString("html_url", "https://github.com/$slug/releases")
            val releaseName = json.optString("name", tagName).ifBlank { tagName }
            val releaseNotes = json.optString("body", "").trim()

            var apkUrl: String? = null
            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.optJSONObject(i) ?: continue
                    val assetName = asset.optString("name", "")
                    if (assetName.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url", "").takeIf { it.isNotBlank() }
                        break
                    }
                }
            }

            if (tagName.isNotBlank() && isNewerVersion(tagName, currentVersionName)) {
                UpdateCheckResult.UpdateAvailable(
                    GitHubReleaseInfo(
                        tagName = tagName,
                        title = releaseName,
                        notes = releaseNotes,
                        htmlUrl = htmlUrl,
                        apkDownloadUrl = apkUrl,
                    ),
                )
            } else {
                UpdateCheckResult.UpToDate(latestTag = tagName.ifBlank { null })
            }
        } catch (e: Exception) {
            UpdateCheckResult.Error(e.message ?: "Unable to reach GitHub")
        } finally {
            connection?.disconnect()
        }
    }
}
