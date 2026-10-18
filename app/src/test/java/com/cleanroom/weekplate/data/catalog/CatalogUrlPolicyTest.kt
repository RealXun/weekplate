package com.cleanroom.weekplate.data.catalog

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogUrlPolicyTest {
    @Test
    fun acceptsPublicHttpsUrl() {
        assertTrue(CatalogRepository.isAllowedFeedUrl("https://example.org/catalog/latest.json"))
    }

    @Test
    fun rejectsInsecureCredentialsAndFragments() {
        assertFalse(CatalogRepository.isAllowedFeedUrl("http://example.org/latest.json"))
        assertFalse(CatalogRepository.isAllowedFeedUrl("https://user:pass@example.org/latest.json"))
        assertFalse(CatalogRepository.isAllowedFeedUrl("https://example.org/latest.json#secret"))
    }
}
