package com.cleanroom.weekplate.network

import com.cleanroom.weekplate.BuildConfig
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

data class RemoteHousehold(
    val code: String,
    val revision: Long,
    val devices: Int,
    val payload: String
)

class HouseholdConflictException : Exception("El hogar cambió en otro dispositivo")
class HouseholdServiceException(message: String) : Exception(message)

class HouseholdClient(baseUrl: String) {
    private val root = normalizeAndValidate(baseUrl)

    fun create(): RemoteHousehold = request("POST", "/v1/households")

    fun join(code: String): RemoteHousehold =
        request("POST", "/v1/households/${validatedCode(code)}/join")

    fun get(code: String): RemoteHousehold =
        request("GET", "/v1/households/${validatedCode(code)}")

    fun update(code: String, revision: Long, payload: String): RemoteHousehold =
        request("PUT", "/v1/households/${validatedCode(code)}", revision, payload)

    private fun request(
        method: String,
        path: String,
        revision: Long? = null,
        body: String? = null
    ): RemoteHousehold {
        val connection = URL(root + path).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = method
            connection.connectTimeout = 5_000
            connection.readTimeout = 8_000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Cache-Control", "no-store")
            revision?.let { connection.setRequestProperty("If-Match", it.toString()) }
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val status = connection.responseCode
            val response = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            when {
                status == 409 -> throw HouseholdConflictException()
                status !in 200..299 -> throw HouseholdServiceException("El servidor respondió $status")
                else -> JSONObject(response).toRemoteHousehold()
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun JSONObject.toRemoteHousehold(): RemoteHousehold = RemoteHousehold(
        code = getString("code"),
        revision = getLong("revision"),
        devices = getInt("devices"),
        payload = opt("payload")?.toString() ?: "{}"
    )

    companion object {
        fun isAllowedBaseUrl(value: String): Boolean = runCatching {
            val uri = URI(value.trim().trimEnd('/'))
            val loopback = uri.host in setOf("127.0.0.1", "localhost", "10.0.2.2")
            !uri.host.isNullOrBlank() && uri.userInfo == null && uri.query == null && uri.fragment == null &&
                (uri.scheme == "https" || (BuildConfig.DEBUG && uri.scheme == "http" && loopback))
        }.getOrDefault(false)

        private fun normalizeAndValidate(value: String): String {
            require(isAllowedBaseUrl(value)) {
                "Usa HTTPS; en depuración solo se permite HTTP contra loopback"
            }
            return value.trim().trimEnd('/')
        }

        private fun validatedCode(value: String): String = value.trim().uppercase().also {
            require(it.matches(Regex("[A-Z2-9]{6}"))) { "Código de hogar no válido" }
        }
    }
}
