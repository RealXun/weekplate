package com.cleanroom.weekplate.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.cleanroom.weekplate.Destination
import com.cleanroom.weekplate.WeekplateViewModel
import com.cleanroom.weekplate.core.model.*
import com.cleanroom.weekplate.ui.components.*
import com.cleanroom.weekplate.ui.theme.Forest
import com.cleanroom.weekplate.ui.theme.Peach
import com.cleanroom.weekplate.ui.theme.Sage
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
private fun HomeHeader(title: String, subtitle: String? = null) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp)) {
        WeekplateWordmark()
        Spacer(Modifier.height(20.dp))
        Text(title, style = MaterialTheme.typography.displaySmall)
        if (subtitle != null) {
            Spacer(Modifier.height(5.dp))
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun PlanScreen(viewModel: WeekplateViewModel) {
    val plan = viewModel.uiState.plan
    val formatter = DateTimeFormatter.ofPattern("d MMM", Locale("es", "ES"))
    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            HomeHeader(
                "Tu menú",
                plan?.let { "${it.weekStart.format(formatter)} – ${it.weekStart.plusDays(6).format(formatter)}" }
            )
        }
        if (plan == null) {
            item { EmptyState("Aún no hay menú", "Completa tus preferencias para crear una semana.") }
            return@LazyColumn
        }
        item {
            WeekNavigator(
                current = viewModel.isViewingCurrentWeek,
                onPrevious = viewModel::showPreviousWeek,
                onCurrent = viewModel::showCurrentWeek,
                onNext = viewModel::showNextWeek
            )
        }
        item {
            BudgetCard(plan, onShopping = { viewModel.navigate(Destination.Shopping) })
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(onClick = viewModel::regeneratePlan, modifier = Modifier.weight(1f)) { Text("Rehacer semana") }
                Button(onClick = { viewModel.selectTab(MainTab.DISCOVER) }, modifier = Modifier.weight(1f)) { Text("Añadir plato") }
            }
        }
        Weekday.entries.forEach { day ->
            val meals = plan.meals.filter { it.day == day }
            if (meals.isNotEmpty()) {
                item {
                    Text(
                        day.fullLabel,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 8.dp)
                    )
                }
                items(meals, key = { it.id }) { meal ->
                    viewModel.recipe(meal.recipeId)?.let { recipe ->
                        PlannedMealCard(
                            meal = meal,
                            recipe = recipe,
                            onOpen = { viewModel.navigate(Destination.RecipeDetails(recipe.id)) },
                            onSwap = { viewModel.swapMeal(meal.id) },
                            onCooked = { viewModel.toggleMealCooked(meal.id) },
                            onOmit = { viewModel.omitMeal(meal.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekNavigator(
    current: Boolean,
    onPrevious: () -> Unit,
    onCurrent: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(
            onClick = onPrevious,
            modifier = Modifier.semantics { contentDescription = "Semana anterior" }
        ) { Text("‹", style = MaterialTheme.typography.headlineSmall) }
        if (current) {
            Text("SEMANA ACTUAL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        } else {
            TextButton(onClick = onCurrent) { Text("Volver a esta semana") }
        }
        IconButton(
            onClick = onNext,
            modifier = Modifier.semantics { contentDescription = "Semana siguiente" }
        ) { Text("›", style = MaterialTheme.typography.headlineSmall) }
    }
}

@Composable
private fun BudgetCard(plan: WeekPlan, onShopping: () -> Unit) {
    val ratio = if (plan.budget <= 0) 1f else (plan.estimatedSpend / plan.budget).toFloat().coerceIn(0f, 1f)
    ElevatedCard(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(26.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("ESTIMACIÓN DE LA SEMANA", style = MaterialTheme.typography.labelSmall, color = Sage)
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "%.2f € de %.0f €".format(Locale.US, plan.estimatedSpend, plan.budget),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                FilledTonalButton(onClick = onShopping) { Text("Ver lista") }
            }
            Spacer(Modifier.height(14.dp))
            LinearProgressIndicator(
                progress = { ratio },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
                color = if (plan.overBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = .18f)
            )
            Spacer(Modifier.height(7.dp))
            Text(
                if (plan.overBudget) "La estimación supera el presupuesto" else "Estimación local; puede variar en tienda",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .8f)
            )
        }
    }
}

@Composable
private fun PlannedMealCard(
    meal: PlannedMeal,
    recipe: Recipe,
    onOpen: () -> Unit,
    onSwap: () -> Unit,
    onCooked: () -> Unit,
    onOmit: () -> Unit
) {
    ElevatedCard(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 5.dp).clickable(onClick = onOpen),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (meal.isCooked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            RecipeArtwork(recipe, Modifier.size(78.dp))
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(meal.slot.label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(
                    recipe.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        textDecoration = if (meal.omitted) TextDecoration.LineThrough else TextDecoration.None
                    )
                )
                Text("${recipe.timeMinutes} min · ${meal.servings} raciones", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    TextButton(onClick = onSwap, contentPadding = PaddingValues(horizontal = 6.dp)) { Text("Cambiar") }
                    TextButton(onClick = onCooked, contentPadding = PaddingValues(horizontal = 6.dp)) {
                        Text(if (meal.isCooked) "Cocinado ✓" else "Marcar hecho")
                    }
                    TextButton(onClick = onOmit, contentPadding = PaddingValues(horizontal = 6.dp)) { Text(if (meal.omitted) "Restaurar" else "Omitir") }
                }
            }
        }
    }
}

@Composable
fun DiscoverScreen(viewModel: WeekplateViewModel) {
    val state = viewModel.uiState
    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item { HomeHeader("Descubrir", "Busca por plato, cocina o preferencia") }
        item {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::setSearchQuery,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                label = { Text("Buscar recetas") },
                singleLine = true,
                shape = RoundedCornerShape(18.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "PRIORIDAD",
                modifier = Modifier.padding(horizontal = 20.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { Pill("Todas", state.discoverPriority == null, { viewModel.setDiscoverPriority(null) }) }
                items(Priority.entries) { priority ->
                    Pill(priority.label, state.discoverPriority == priority, { viewModel.setDiscoverPriority(priority) })
                }
            }
            Spacer(Modifier.height(13.dp))
            Text(
                "COCINA",
                modifier = Modifier.padding(horizontal = 20.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { Pill("Todas", state.discoverCuisine == null, { viewModel.setDiscoverCuisine(null) }) }
                items(viewModel.discoverCuisines, key = { it }) { cuisine ->
                    Pill(cuisine, state.discoverCuisine == cuisine, { viewModel.setDiscoverCuisine(cuisine) })
                }
            }
            Spacer(Modifier.height(13.dp))
            Text(
                "DIETA",
                modifier = Modifier.padding(horizontal = 20.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { Pill("Todas", state.discoverDiet == null, { viewModel.setDiscoverDiet(null) }) }
                items(Diet.entries, key = { it.name }) { diet ->
                    Pill(diet.label, state.discoverDiet == diet, { viewModel.setDiscoverDiet(diet) })
                }
            }
            if (state.discoverPriority != null || state.discoverCuisine != null || state.discoverDiet != null) {
                TextButton(
                    onClick = viewModel::clearDiscoverFilters,
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) { Text("Quitar filtros") }
            }
            Spacer(Modifier.height(14.dp))
        }
        if (viewModel.filteredRecipes.isEmpty()) {
            item { EmptyState("Sin coincidencias", "Prueba otra búsqueda o elimina un filtro.") }
        } else {
            items(viewModel.filteredRecipes, key = { it.id }) { recipe ->
                RecipeCard(
                    recipe = recipe,
                    favourite = recipe.id in state.favourites,
                    onClick = { viewModel.navigate(Destination.RecipeDetails(recipe.id)) },
                    onFavourite = { viewModel.toggleFavourite(recipe.id) },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun FavouritesScreen(viewModel: WeekplateViewModel) {
    val favourites = viewModel.recipes.filter { it.id in viewModel.uiState.favourites }
    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item { HomeHeader("Favoritos", "Tus recetas guardadas") }
        if (favourites.isEmpty()) {
            item { EmptyState("Aún no hay favoritos", "Toca el corazón de una receta para guardarla aquí.") }
        } else {
            items(favourites, key = { it.id }) { recipe ->
                RecipeCard(
                    recipe,
                    favourite = true,
                    onClick = { viewModel.navigate(Destination.RecipeDetails(recipe.id)) },
                    onFavourite = { viewModel.toggleFavourite(recipe.id) },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun ProfileScreen(viewModel: WeekplateViewModel) {
    val prefs = viewModel.uiState.preferences
    val context = LocalContext.current
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.toggleReminder(true)
        else viewModel.showMessage("Activa las notificaciones para recibir el recordatorio")
    }
    val changeReminder: (Boolean) -> Unit = { enabled ->
        when {
            !enabled -> viewModel.toggleReminder(false)
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU -> viewModel.toggleReminder(true)
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED ->
                viewModel.toggleReminder(true)
            else -> notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 30.dp)
    ) {
        item { HomeHeader("Perfil", "Preferencias, historial y hogar") }
        item {
            Surface(
                color = Forest,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(26.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("TU CONFIGURACIÓN", style = MaterialTheme.typography.labelSmall, color = Sage)
                    Spacer(Modifier.height(8.dp))
                    Text("${prefs.people} personas · ${prefs.cookingDays.size} días", style = MaterialTheme.typography.headlineSmall)
                    Text("Presupuesto de %.0f € · ${prefs.store?.name ?: "Sin tienda"}".format(prefs.budget))
                }
            }
            Spacer(Modifier.height(18.dp))
            ProfileSection("PLANIFICACIÓN") {
                ProfileRow("Preferencias del menú", "Dietas, presupuesto y cocina") { viewModel.navigate(Destination.Preferences) }
                ProfileRow("Semanas anteriores", "Revisa tus últimos menús") { viewModel.navigate(Destination.History) }
                ProfileRow("Hogar compartido", "Sincronización mediante backend propio") { viewModel.navigate(Destination.Household) }
            }
            Spacer(Modifier.height(18.dp))
            ProfileSection("RECORDATORIOS") {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Recordatorio semanal", style = MaterialTheme.typography.titleMedium)
                        Text("Domingos a las 10:00 · aviso local", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = viewModel.uiState.reminderEnabled, onCheckedChange = changeReminder)
                }
            }
            Spacer(Modifier.height(18.dp))
            ProfileSection("AYUDA Y DATOS") {
                ProfileRow("Acerca de Weekplate", "Alcance clean-room y privacidad") { viewModel.navigate(Destination.About) }
                ProfileRow("Empezar de nuevo", "Borra preferencias y favoritos locales", destructive = true) { viewModel.resetAll() }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Weekplate 0.1.0 · prototipo independiente",
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProfileSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp)) {
        Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(7.dp))
        Surface(shape = RoundedCornerShape(22.dp), tonalElevation = 1.dp, content = { Column(content = content) })
    }
}

@Composable
private fun ProfileRow(
    title: String,
    subtitle: String,
    destructive: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(if (destructive) Peach else Sage),
            contentAlignment = Alignment.Center
        ) { Text(title.take(1), fontWeight = FontWeight.Black, color = Forest) }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("›", style = MaterialTheme.typography.headlineSmall)
    }
}
