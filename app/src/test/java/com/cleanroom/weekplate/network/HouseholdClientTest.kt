package com.cleanroom.weekplate.network

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HouseholdClientTest {
    @Test
    fun `production-style endpoint requires https`() {
        assertTrue(HouseholdClient.isAllowedBaseUrl("https://household.example.test"))
        assertFalse(HouseholdClient.isAllowedBaseUrl("http://household.example.test"))
        assertFalse(HouseholdClient.isAllowedBaseUrl("https://user:secret@household.example.test"))
        assertFalse(HouseholdClient.isAllowedBaseUrl("https://"))
    }

    @Test
    fun `debug loopback endpoint is explicitly bounded`() {
        assertTrue(HouseholdClient.isAllowedBaseUrl("http://10.0.2.2:8080"))
        assertFalse(HouseholdClient.isAllowedBaseUrl("http://192.168.1.30:8080"))
    }
}
