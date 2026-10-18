package com.cleanroom.weekplate.backend

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

data class HouseholdSnapshot(
    val code: String,
    val payload: String,
    val revision: Long,
    val devices: Int,
    val updatedAt: Instant
)

class HouseholdRepository(private val random: SecureRandom = SecureRandom()) {
    private val values = ConcurrentHashMap<String, HouseholdSnapshot>()

    fun create(): HouseholdSnapshot {
        var code: String
        do code = (1..6).joinToString("") { "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"[random.nextInt(32)].toString() }
        while (values.containsKey(code))
        return HouseholdSnapshot(code, "{}", 0, 1, Instant.now()).also { values[code] = it }
    }

    fun find(code: String): HouseholdSnapshot? = values[code.uppercase()]

    fun join(code: String): HouseholdSnapshot? = values.computeIfPresent(code.uppercase()) { _, old ->
        old.copy(devices = old.devices + 1, updatedAt = Instant.now())
    }

    fun update(code: String, expectedRevision: Long, payload: String): HouseholdSnapshot? {
        var changed = false
        val snapshot = values.computeIfPresent(code.uppercase()) { _, old ->
            if (old.revision != expectedRevision) old
            else {
                changed = true
                old.copy(payload = payload, revision = old.revision + 1, updatedAt = Instant.now())
            }
        }
        return snapshot.takeIf { changed }
    }
}

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    val server = createHouseholdServer(port = port)
    server.start()
    println("Weekplate household service listening on http://127.0.0.1:${server.address.port}")
}

fun createHouseholdServer(
    host: String = "127.0.0.1",
    port: Int = 0,
    repository: HouseholdRepository = HouseholdRepository()
): HttpServer {
    val server = HttpServer.create(InetSocketAddress(host, port), 0)
    server.executor = Executors.newCachedThreadPool { runnable ->
        Thread(runnable, "weekplate-household").apply { isDaemon = true }
    }

    server.createContext("/health") { exchange ->
        exchange.respond(200, "{\"status\":\"ok\"}")
    }
    server.createContext("/v1/households") { exchange ->
        val path = exchange.requestURI.path.removePrefix("/v1/households").trim('/')
        when {
            exchange.requestMethod == "POST" && path.isEmpty() -> {
                val household = repository.create()
                exchange.respond(201, household.toJson())
            }
            exchange.requestMethod == "POST" && path.endsWith("/join") -> {
                val code = path.removeSuffix("/join").trim('/')
                repository.join(code)?.let { exchange.respond(200, it.toJson()) }
                    ?: exchange.respond(404, "{\"error\":\"unknown_household\"}")
            }
            exchange.requestMethod == "GET" && path.isNotEmpty() -> {
                repository.find(path)?.let { exchange.respond(200, it.toJson()) }
                    ?: exchange.respond(404, "{\"error\":\"unknown_household\"}")
            }
            exchange.requestMethod == "PUT" && path.isNotEmpty() -> {
                val expected = exchange.requestHeaders.getFirst("If-Match")?.toLongOrNull()
                val payloadBytes = exchange.requestBody.readNBytes(256_001)
                val payload = payloadBytes.toString(StandardCharsets.UTF_8)
                if (payloadBytes.size > 256_000) exchange.respond(413, "{\"error\":\"payload_too_large\"}")
                else if (expected == null) exchange.respond(400, "{\"error\":\"missing_revision\"}")
                else repository.update(path, expected, payload)?.let { exchange.respond(200, it.toJson()) }
                    ?: exchange.respond(409, "{\"error\":\"revision_conflict\"}")
            }
            else -> exchange.respond(405, "{\"error\":\"method_not_allowed\"}")
        }
    }
    return server
}

private fun HouseholdSnapshot.toJson(): String =
    "{\"code\":\"$code\",\"revision\":$revision,\"devices\":$devices," +
        "\"updatedAt\":\"$updatedAt\",\"payload\":${payload.ifBlank { "{}" }}}"

private fun HttpExchange.respond(status: Int, body: String) {
    responseHeaders.set("Content-Type", "application/json; charset=utf-8")
    responseHeaders.set("Cache-Control", "no-store")
    val bytes = body.toByteArray(StandardCharsets.UTF_8)
    sendResponseHeaders(status, bytes.size.toLong())
    responseBody.use { it.write(bytes) }
}
