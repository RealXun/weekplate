package com.cleanroom.weekplate.data.catalog

import android.content.Context
import android.util.AtomicFile
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.URI
import javax.net.ssl.HttpsURLConnection

class CatalogFetchException(message: String, val retryable: Boolean) : IOException(message)

class CatalogRepository(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val cache = AtomicFile(File(appContext.filesDir, "catalog/latest.json"))

    fun feedUrl(): String = preferences.getString(KEY_FEED_URL, "").orEmpty()

    fun saveFeedUrl(value: String) {
        val normalized = value.trim()
        preferences.edit()
            .putString(KEY_FEED_URL, normalized)
            .apply {
                if (normalized != feedUrl()) remove(KEY_ETAG)
            }
            .apply()
    }

    fun lastCheckedEpochMillis(): Long = preferences.getLong(KEY_LAST_CHECKED, 0L)

    fun lastError(): String? = preferences.getString(KEY_LAST_ERROR, null)

    fun loadSnapshot(): CatalogSnapshot? {
        val file = cache.baseFile
        if (!file.isFile || file.length() <= 0L || file.length() > MAX_BYTES) return null
        return runCatching { CatalogJson.decode(file.readText(Charsets.UTF_8)) }.getOrNull()
    }

    @Throws(IOException::class, IllegalArgumentException::class)
    fun refresh(): CatalogSnapshot {
        return try {
            refreshUnsafe()
        } catch (error: Exception) {
            preferences.edit().putString(KEY_LAST_ERROR, error.message ?: "Error de sincronización").apply()
            throw error
        }
    }

    private fun refreshUnsafe(): CatalogSnapshot {
        val value = feedUrl()
        require(isAllowedFeedUrl(value)) { "Configura una URL HTTPS válida" }

        val connection = URI(value).toURL().openConnection() as? HttpsURLConnection
            ?: throw IllegalArgumentException("La URL debe usar HTTPS")
        connection.connectTimeout = 12_000
        connection.readTimeout = 20_000
        connection.instanceFollowRedirects = false
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("User-Agent", "WeekplateCleanRoom/0.2 catalog-sync")
        preferences.getString(KEY_ETAG, null)?.let { connection.setRequestProperty("If-None-Match", it) }

        try {
            val responseCode = connection.responseCode
            if (responseCode == HttpsURLConnection.HTTP_NOT_MODIFIED) {
                val cached = loadSnapshot() ?: throw CatalogFetchException("Respuesta 304 sin catálogo local", true)
                markSuccess(connection.getHeaderField("ETag"))
                return cached
            }
            if (responseCode !in 200..299) {
                throw CatalogFetchException(
                    message = "El catálogo respondió HTTP $responseCode",
                    retryable = responseCode == 408 || responseCode == 429 || responseCode >= 500
                )
            }
            val declaredLength = connection.contentLengthLong
            if (declaredLength > MAX_BYTES) throw IllegalArgumentException("El catálogo supera 8 MB")
            val raw = connection.inputStream.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(16 * 1024)
                var total = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    total += read
                    if (total > MAX_BYTES) throw IllegalArgumentException("El catálogo supera 8 MB")
                    output.write(buffer, 0, read)
                }
                output.toString(Charsets.UTF_8.name())
            }
            val snapshot = CatalogJson.decode(raw)
            cache.baseFile.parentFile?.mkdirs()
            val output = cache.startWrite()
            try {
                output.write(raw.toByteArray(Charsets.UTF_8))
                cache.finishWrite(output)
            } catch (error: Exception) {
                cache.failWrite(output)
                throw error
            }
            markSuccess(connection.getHeaderField("ETag"))
            return snapshot
        } finally {
            connection.disconnect()
        }
    }

    private fun markSuccess(etag: String?) {
        preferences.edit()
            .putLong(KEY_LAST_CHECKED, System.currentTimeMillis())
            .putString(KEY_LAST_ERROR, null)
            .apply {
                if (!etag.isNullOrBlank()) putString(KEY_ETAG, etag)
            }
            .apply()
    }

    companion object {
        private const val PREFERENCES = "external_catalog"
        private const val KEY_FEED_URL = "feed_url"
        private const val KEY_LAST_CHECKED = "last_checked"
        private const val KEY_LAST_ERROR = "last_error"
        private const val KEY_ETAG = "etag"
        private const val MAX_BYTES = 8 * 1024 * 1024L

        fun isAllowedFeedUrl(value: String): Boolean = runCatching {
            val uri = URI(value.trim())
            uri.scheme.equals("https", ignoreCase = true) &&
                !uri.host.isNullOrBlank() &&
                uri.userInfo == null &&
                uri.fragment == null
        }.getOrDefault(false)
    }
}
