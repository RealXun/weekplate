package com.cleanroom.weekplate.data.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CatalogMatcherTest {
    private val product = CatalogProduct(
        id = "p1",
        name = "Brocoli 500 g",
        price = 1.75,
        pricePerUnit = 3.5,
        unit = "kg",
        currency = "EUR",
        brand = null,
        category = null,
        available = true,
        url = null
    )
    private val snapshot = CatalogSnapshot(
        schemaVersion = 1,
        generatedAt = "2026-10-18T10:00:00Z",
        sourceName = "grocery-cli",
        sourceVersion = null,
        stores = listOf(
            CatalogStore(
                id = "mercadona",
                name = "Mercadona",
                currency = "EUR",
                status = "ok",
                error = null,
                items = listOf(CatalogItem("Brócoli", product))
            )
        )
    )

    @Test
    fun matchesAccentsAndCaseOnlyWithinSelectedStore() {
        assertEquals(product, CatalogMatcher.productFor(snapshot, "mercadona", "BROCOLI"))
        assertNull(CatalogMatcher.productFor(snapshot, "dia", "Brócoli"))
    }

    @Test
    fun doesNotGuessDifferentProducts() {
        assertNull(CatalogMatcher.productFor(snapshot, "mercadona", "Coliflor"))
    }
}
