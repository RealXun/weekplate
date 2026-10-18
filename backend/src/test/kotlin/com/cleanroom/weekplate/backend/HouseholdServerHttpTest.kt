package com.cleanroom.weekplate.backend

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class HouseholdServerHttpTest {
    @Test
    fun `http contract creates joins updates and rejects stale revisions`() {
        val server = createHouseholdServer()
        server.start()
        try {
            val client = HttpClient.newHttpClient()
            val root = "http://127.0.0.1:${server.address.port}/v1/households"
            val created = client.send(
                HttpRequest.newBuilder(URI(root)).POST(HttpRequest.BodyPublishers.noBody()).build(),
                HttpResponse.BodyHandlers.ofString()
            )
            assertEquals(201, created.statusCode())
            val code = Regex("\\\"code\\\":\\\"([A-Z2-9]{6})\\\"").find(created.body())!!.groupValues[1]

            val updated = client.send(
                HttpRequest.newBuilder(URI("$root/$code"))
                    .header("If-Match", "0")
                    .PUT(HttpRequest.BodyPublishers.ofString("{\"schema\":1}"))
                    .build(),
                HttpResponse.BodyHandlers.ofString()
            )
            assertEquals(200, updated.statusCode())
            assertTrue(updated.body().contains("\"revision\":1"))

            val stale = client.send(
                HttpRequest.newBuilder(URI("$root/$code"))
                    .header("If-Match", "0")
                    .PUT(HttpRequest.BodyPublishers.ofString("{}"))
                    .build(),
                HttpResponse.BodyHandlers.ofString()
            )
            assertEquals(409, stale.statusCode())

            val joined = client.send(
                HttpRequest.newBuilder(URI("$root/$code/join")).POST(HttpRequest.BodyPublishers.noBody()).build(),
                HttpResponse.BodyHandlers.ofString()
            )
            assertEquals(200, joined.statusCode())
            assertTrue(joined.body().contains("\"devices\":2"))
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun `http contract rejects oversized snapshots`() {
        val server = createHouseholdServer()
        server.start()
        try {
            val client = HttpClient.newHttpClient()
            val root = "http://127.0.0.1:${server.address.port}/v1/households"
            val created = client.send(
                HttpRequest.newBuilder(URI(root)).POST(HttpRequest.BodyPublishers.noBody()).build(),
                HttpResponse.BodyHandlers.ofString()
            )
            val code = Regex("\\\"code\\\":\\\"([A-Z2-9]{6})\\\"").find(created.body())!!.groupValues[1]
            val response = client.send(
                HttpRequest.newBuilder(URI("$root/$code"))
                    .header("If-Match", "0")
                    .PUT(HttpRequest.BodyPublishers.ofString("x".repeat(256_001)))
                    .build(),
                HttpResponse.BodyHandlers.ofString()
            )
            assertEquals(413, response.statusCode())
        } finally {
            server.stop(0)
        }
    }
}
