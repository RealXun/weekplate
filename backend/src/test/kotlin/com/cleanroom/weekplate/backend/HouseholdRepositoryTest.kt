package com.cleanroom.weekplate.backend

import org.junit.Assert.*
import org.junit.Test

class HouseholdRepositoryTest {
    @Test fun `revision protects concurrent updates`() {
        val repository = HouseholdRepository()
        val created = repository.create()
        val updated = repository.update(created.code, 0, "{\"plan\":1}")
        assertEquals(1L, updated?.revision)
        assertNull(repository.update(created.code, 0, "{\"plan\":2}"))
        assertEquals("{\"plan\":1}", repository.find(created.code)?.payload)
    }

    @Test fun `join tracks devices`() {
        val repository = HouseholdRepository()
        val created = repository.create()
        assertEquals(2, repository.join(created.code.lowercase())?.devices)
    }
}
