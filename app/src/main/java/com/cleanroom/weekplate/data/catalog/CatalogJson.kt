package com.cleanroom.weekplate.data.catalog

import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

object CatalogJson {
    const val SCHEMA_VERSION = 1

    fun decode(raw: String): CatalogSnapshot {
        val root = JSONObject(raw)
        val schemaVersion = root.getInt("schemaVersion")
        require(schemaVersion == SCHEMA_VERSION) {
            "Versión de catálogo no compatible: $schemaVersion"
        }

        val generatedAt = root.getString("generatedAt").trim()
        Instant.parse(generatedAt)
        val source = root.getJSONObject("source")
        val storesJson = root.getJSONArray("stores")
        require(storesJson.length() <= 100) { "El catálogo contiene demasiadas tiendas" }

        val stores = buildList(storesJson.length()) {
            for (index in 0 until storesJson.length()) {
                add(decodeStore(storesJson.getJSONObject(index)))
            }
        }
        require(stores.map(CatalogStore::id).distinct().size == stores.size) {
            "El catálogo contiene tiendas duplicadas"
        }

        return CatalogSnapshot(
            schemaVersion = schemaVersion,
            generatedAt = generatedAt,
            sourceName = source.getString("name").trim().also { require(it.isNotEmpty()) },
            sourceVersion = source.optionalString("version"),
            stores = stores
        )
    }

    private fun decodeStore(value: JSONObject): CatalogStore {
        val itemsJson = value.optJSONArray("items") ?: JSONArray()
        require(itemsJson.length() <= 5_000) { "Demasiados artículos en una tienda" }
        val items = buildList(itemsJson.length()) {
            for (index in 0 until itemsJson.length()) {
                val item = itemsJson.getJSONObject(index)
                val term = item.getString("term").trim()
                require(term.isNotEmpty()) { "Consulta de producto vacía" }
                add(
                    CatalogItem(
                        term = term,
                        product = if (item.isNull("product")) null else decodeProduct(item.getJSONObject("product"))
                    )
                )
            }
        }
        return CatalogStore(
            id = value.getString("id").trim().also { require(it.matches(Regex("[a-z0-9-]{1,40}"))) },
            name = value.getString("name").trim().also { require(it.isNotEmpty()) },
            currency = value.optString("currency", "EUR").trim().ifEmpty { "EUR" },
            status = value.optString("status", "ok").trim().ifEmpty { "ok" },
            error = value.optionalString("error"),
            items = items
        )
    }

    private fun decodeProduct(value: JSONObject): CatalogProduct {
        val price = value.getDouble("price")
        require(price.isFinite() && price > 0.0 && price <= 100_000.0) { "Precio de producto no válido" }
        return CatalogProduct(
            id = value.getString("id").trim().also { require(it.isNotEmpty()) },
            name = value.getString("name").trim().also { require(it.isNotEmpty()) },
            price = price,
            pricePerUnit = value.optionalPositiveDouble("pricePerUnit"),
            unit = value.optionalString("unit"),
            currency = value.optString("currency", "EUR").trim().ifEmpty { "EUR" },
            brand = value.optionalString("brand"),
            category = value.optionalString("category"),
            available = value.optBoolean("available", true),
            url = value.optionalString("url")
        )
    }

    private fun JSONObject.optionalString(key: String): String? =
        if (!has(key) || isNull(key)) null else optString(key).trim().takeIf(String::isNotEmpty)

    private fun JSONObject.optionalPositiveDouble(key: String): Double? {
        if (!has(key) || isNull(key)) return null
        return optDouble(key).takeIf { it.isFinite() && it > 0.0 }
    }
}
