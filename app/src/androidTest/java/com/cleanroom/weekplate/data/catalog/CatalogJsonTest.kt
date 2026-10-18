package com.cleanroom.weekplate.data.catalog

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CatalogJsonTest {
    @Test
    fun parsesVersionOneFeed() {
        val snapshot = CatalogJson.decode(
            """
            {
              "schemaVersion": 1,
              "generatedAt": "2026-10-18T10:00:00Z",
              "source": {"name": "grocery-cli", "version": "test"},
              "stores": [{
                "id": "dia", "name": "DIA", "currency": "EUR", "status": "ok",
                "items": [{"term": "Huevos", "product": {
                  "id": "123", "name": "Huevos M", "price": 2.45,
                  "currency": "EUR", "available": true
                }}]
              }]
            }
            """.trimIndent()
        )
        assertEquals(1, snapshot.productCount)
        assertEquals("Huevos M", snapshot.stores.single().items.single().product?.name)
    }
}
