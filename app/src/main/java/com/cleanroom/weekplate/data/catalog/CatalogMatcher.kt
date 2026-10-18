package com.cleanroom.weekplate.data.catalog

import java.text.Normalizer
import java.util.Locale

object CatalogMatcher {
    fun productFor(snapshot: CatalogSnapshot?, storeId: String?, itemName: String): CatalogProduct? {
        if (snapshot == null || storeId.isNullOrBlank()) return null
        val wanted = normalize(itemName)
        return snapshot.stores.firstOrNull { it.id == storeId }
            ?.items
            ?.firstOrNull { normalize(it.term) == wanted }
            ?.product
            ?.takeIf(CatalogProduct::available)
    }

    internal fun normalize(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()
}
