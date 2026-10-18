package com.cleanroom.weekplate.data.catalog

data class CatalogSnapshot(
    val schemaVersion: Int,
    val generatedAt: String,
    val sourceName: String,
    val sourceVersion: String?,
    val stores: List<CatalogStore>
) {
    val productCount: Int get() = stores.sumOf { store -> store.items.count { it.product != null } }
}

data class CatalogStore(
    val id: String,
    val name: String,
    val currency: String,
    val status: String,
    val error: String?,
    val items: List<CatalogItem>
)

data class CatalogItem(
    val term: String,
    val product: CatalogProduct?
)

data class CatalogProduct(
    val id: String,
    val name: String,
    val price: Double,
    val pricePerUnit: Double?,
    val unit: String?,
    val currency: String,
    val brand: String?,
    val category: String?,
    val available: Boolean,
    val url: String?
)

data class CatalogUiState(
    val feedUrl: String,
    val snapshot: CatalogSnapshot? = null,
    val syncing: Boolean = false,
    val statusMessage: String = "Catálogo externo sin configurar",
    val lastCheckedEpochMillis: Long = 0L
)
