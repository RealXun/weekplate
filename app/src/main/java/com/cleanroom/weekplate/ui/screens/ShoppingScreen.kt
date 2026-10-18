package com.cleanroom.weekplate.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.cleanroom.weekplate.WeekplateViewModel
import com.cleanroom.weekplate.core.model.Aisle
import com.cleanroom.weekplate.core.model.ShoppingItem
import com.cleanroom.weekplate.data.catalog.CatalogProduct
import com.cleanroom.weekplate.data.catalog.CatalogUiState
import java.util.Locale

@Composable
fun ShoppingScreen(viewModel: WeekplateViewModel) {
    val items = viewModel.shoppingItems
    val context = LocalContext.current
    var newItem by remember { mutableStateOf("") }
    val checked = items.count(ShoppingItem::checked)
    val localEstimate = items.sumOf(ShoppingItem::estimatedPrice)
    val quotedProducts = items.associate { item -> item.id to viewModel.catalogProductFor(item) }
    val quoteCount = quotedProducts.values.count { it != null }
    val quoteTotal = quotedProducts.values.filterNotNull().sumOf(CatalogProduct::price)
    val priceableItems = items.count { !it.custom }
    val selectedStore = viewModel.uiState.preferences.store

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        SimpleTopBar(
            if (viewModel.shoppingWeekCount == 2) "Lista · 2 semanas" else "Lista de la compra",
            viewModel::back
        ) {
            TextButton(onClick = {
                val text = items.groupBy(ShoppingItem::aisle).entries.joinToString("\n\n") { (aisle, aisleItems) ->
                    aisle.label + "\n" + aisleItems.joinToString("\n") { item ->
                        "${if (item.checked) "✓" else "•"} ${item.name} — ${formatShoppingAmount(item)}"
                    }
                }
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Lista de la compra")
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                context.startActivity(Intent.createChooser(intent, "Compartir lista"))
            }) { Text("Compartir") }
        }
        Surface(color = MaterialTheme.colorScheme.primary) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
                Row {
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (quoteCount > 0) "$quoteCount de $priceableItems precios encontrados" else "$checked de ${items.size} artículos",
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            if (quoteCount > 0) "Cesta indicativa %.2f €".format(Locale.US, quoteTotal)
                            else "Estimación local %.2f €".format(Locale.US, localEstimate),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        if (quoteCount > 0) {
                            Text(
                                "Una unidad del producto elegido por artículo",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .82f)
                            )
                        }
                    }
                    Text(
                        if (items.isEmpty()) "0 %" else "${checked * 100 / items.size} %",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { if (items.isEmpty()) 0f else checked.toFloat() / items.size },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = .2f)
                )
            }
        }
        CatalogStatusCard(
            state = viewModel.catalogState,
            storeName = selectedStore?.name ?: "Sin tienda seleccionada",
            onUrlChange = viewModel::setCatalogFeedUrl,
            onSave = viewModel::saveCatalogFeedUrl,
            onRefresh = viewModel::refreshCatalog
        )
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Combinar semanas", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Suma esta semana y la siguiente",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = viewModel.uiState.combineNextWeekShopping,
                onCheckedChange = { viewModel.toggleCombineNextWeekShopping() },
                modifier = Modifier.semantics { contentDescription = "Combinar semana siguiente" }
            )
        }
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newItem,
                onValueChange = { newItem = it },
                modifier = Modifier.weight(1f),
                label = { Text("Añadir un artículo") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                viewModel.addExtraShoppingItem(newItem)
                newItem = ""
            }, enabled = newItem.isNotBlank()) { Text("Añadir") }
        }
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 24.dp)) {
            Aisle.entries.forEach { aisle ->
                val aisleItems = items.filter { it.aisle == aisle }
                if (aisleItems.isNotEmpty()) {
                    item(key = "header-${aisle.name}") {
                        Text(
                            aisle.label.uppercase(),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    items(aisleItems, key = { it.id + it.unit }) { item ->
                        ShoppingRow(item, quotedProducts[item.id]) { viewModel.toggleShoppingItem(item.id) }
                    }
                }
            }
            item {
                Text(
                    if (quoteCount > 0) {
                        "Precios externos obtenidos del último catálogo guardado. Cada importe corresponde a una unidad del producto coincidente; no incluye cantidades, promociones ni disponibilidad de tu tienda y no es un total de compra garantizado."
                    } else {
                        "Los precios mostrados son estimaciones del catálogo local de demostración hasta que haya coincidencias en un catálogo externo."
                    },
                    modifier = Modifier.padding(20.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CatalogStatusCard(
    state: CatalogUiState,
    storeName: String,
    onUrlChange: (String) -> Unit,
    onSave: () -> Unit,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Precios de supermercados", style = MaterialTheme.typography.titleMedium)
            Text(
                "$storeName · ${state.statusMessage}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            state.snapshot?.let { snapshot ->
                Text(
                    "Fuente ${snapshot.sourceName} · ${snapshot.generatedAt.take(16).replace('T', ' ')} UTC",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = state.feedUrl,
                onValueChange = onUrlChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("URL HTTPS del catálogo JSON") },
                placeholder = { Text("https://usuario.github.io/repositorio/catalog/latest.json") },
                singleLine = true,
                enabled = !state.syncing
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSave, enabled = !state.syncing) {
                    Text(if (state.feedUrl.isBlank()) "Desactivar" else "Guardar y actualizar")
                }
                OutlinedButton(
                    onClick = onRefresh,
                    enabled = !state.syncing && state.feedUrl.isNotBlank()
                ) { Text("Actualizar") }
            }
            if (state.syncing) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun ShoppingRow(item: ShoppingItem, product: CatalogProduct?, onToggle: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp).clickable(onClick = onToggle),
        shape = RoundedCornerShape(17.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(item.checked, onCheckedChange = { onToggle() })
            Column(Modifier.weight(1f)) {
                Text(
                    item.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None
                    )
                )
                Text(formatShoppingAmount(item), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!item.custom) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "%.2f €".format(Locale.US, product?.price ?: item.estimatedPrice),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        if (product == null) "estimado" else "1 producto",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun formatShoppingAmount(item: ShoppingItem): String {
    val amount = if (item.amount % 1.0 == 0.0) item.amount.toInt().toString() else String.format(Locale.US, "%.1f", item.amount)
    return "$amount ${item.unit}"
}
