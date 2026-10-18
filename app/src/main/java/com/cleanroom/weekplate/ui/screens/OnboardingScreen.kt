package com.cleanroom.weekplate.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cleanroom.weekplate.WeekplateViewModel
import com.cleanroom.weekplate.core.model.*
import com.cleanroom.weekplate.ui.components.Pill
import com.cleanroom.weekplate.ui.components.WeekplateWordmark
import kotlin.math.roundToInt

private data class IntroStep(val eyebrow: String, val title: String, val subtitle: String)

private val onboardingSteps = listOf(
    IntroStep("TU TIENDA", "¿Dónde haces la compra?", "El presupuesto se adapta al tipo de tienda que eliges."),
    IntroStep("PRESUPUESTO SEMANAL", "Marca tu presupuesto", "Usaremos este límite para proponer una semana realista."),
    IntroStep("DÍAS DE COCINA", "¿Qué días vas a cocinar?", "Toca los días para los que quieres preparar una cena."),
    IntroStep("OBJETIVOS Y PREFERENCIAS", "¿Qué te apetece priorizar?", "Tus elecciones ordenan las recetas del menú."),
    IntroStep("TU COCINA", "¿Qué tienes en la cocina?", "Solo propondremos recetas que puedas preparar."),
    IntroStep("CASI ESTÁ", "Tu semana, a tu manera", "Puedes cambiar platos, ajustar cantidades y rehacer el menú cuando quieras."),
    IntroStep("DIETA Y ALERGIAS", "¿Cómo comes?", "Selecciona solo las opciones que deben cumplirse siempre."),
    IntroStep("LO QUE NO TE GUSTA", "¿Hay algo que prefieras evitar?", "Excluiremos estos ingredientes de las propuestas disponibles.")
)

@Composable
fun OnboardingScreen(viewModel: WeekplateViewModel) {
    val state = viewModel.uiState
    val step = state.onboardingStep.coerceIn(onboardingSteps.indices)
    val info = onboardingSteps[step]
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).systemBarsPadding()
    ) {
        Column(Modifier.padding(horizontal = 22.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (step > 0) TextButton(onClick = viewModel::previousOnboardingStep) { Text("Atrás") }
                else Spacer(Modifier.width(64.dp))
                Spacer(Modifier.weight(1f))
                WeekplateWordmark()
                Spacer(Modifier.weight(1f))
                Text("${step + 1}/${onboardingSteps.size}", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(14.dp))
            LinearProgressIndicator(
                progress = { (step + 1f) / onboardingSteps.size },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text(info.eyebrow, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(10.dp))
            Text(info.title, style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(10.dp))
            Text(info.subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(28.dp))
            when (step) {
                0 -> StoreStep(viewModel)
                1 -> BudgetStep(viewModel)
                2 -> DaysStep(viewModel)
                3 -> PrioritiesStep(viewModel)
                4 -> AppliancesStep(viewModel)
                5 -> ReadyStep()
                6 -> DietStep(viewModel)
                7 -> DislikesStep(viewModel)
            }
        }

        Surface(shadowElevation = 7.dp) {
            Button(
                onClick = {
                    val validation = viewModel.onboardingValidationMessage(step)
                    if (validation == null) viewModel.nextOnboardingStep(onboardingSteps.lastIndex)
                    else viewModel.showMessage(validation)
                },
                modifier = Modifier.fillMaxWidth().padding(20.dp).height(54.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Text(if (step == onboardingSteps.lastIndex) "Crear mi menú" else "Continuar")
            }
        }
    }
}

@Composable
private fun StoreStep(viewModel: WeekplateViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        viewModel.stores.forEach { store ->
            val selected = viewModel.uiState.preferences.store?.id == store.id
            Surface(
                modifier = Modifier.fillMaxWidth().selectable(
                    selected = selected,
                    role = Role.RadioButton,
                    onClick = { viewModel.updatePreferences { it.copy(store = store) } }
                ),
                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.large,
                border = ButtonDefaults.outlinedButtonBorder
            ) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selected, onClick = null)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(store.name, style = MaterialTheme.typography.titleMedium)
                        Text("España · estimación local", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetStep(viewModel: WeekplateViewModel) {
    val prefs = viewModel.uiState.preferences
    Column {
        Text("${prefs.budget.roundToInt()} €", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
        Slider(
            value = prefs.budget.toFloat(),
            onValueChange = { value -> viewModel.updatePreferences { it.copy(budget = value.roundToInt().toDouble()) } },
            valueRange = 25f..160f,
            steps = 26
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("25 €", style = MaterialTheme.typography.bodySmall)
            Text("160 €", style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(28.dp))
        Text("¿Para cuántas personas?", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        Stepper(prefs.people, 1..8) { n -> viewModel.updatePreferences { it.copy(people = n) } }
        Spacer(Modifier.height(28.dp))
        Text("Comidas al día", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (1..3).forEach { count ->
                Pill("$count", prefs.mealsPerDay == count, { viewModel.updatePreferences { it.copy(mealsPerDay = count) } })
            }
        }
    }
}

@Composable
private fun Stepper(value: Int, range: IntRange, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = { onChange((value - 1).coerceAtLeast(range.first)) }, enabled = value > range.first) { Text("−") }
        Text("$value", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(horizontal = 24.dp))
        OutlinedButton(onClick = { onChange((value + 1).coerceAtMost(range.last)) }, enabled = value < range.last) { Text("+") }
    }
}

@Composable
private fun DaysStep(viewModel: WeekplateViewModel) {
    val selected = viewModel.uiState.preferences.cookingDays
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Weekday.entries.forEach { day ->
            FilterChip(
                selected = day in selected,
                onClick = {
                    viewModel.updatePreferences { prefs ->
                        prefs.copy(cookingDays = prefs.cookingDays.toMutableSet().apply {
                            if (!add(day)) remove(day)
                        })
                    }
                },
                label = { Text(day.shortLabel, fontWeight = FontWeight.Bold) }
            )
        }
    }
    Spacer(Modifier.height(18.dp))
    Text("${selected.size} días seleccionados", style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun PrioritiesStep(viewModel: WeekplateViewModel) {
    val selected = viewModel.uiState.preferences.priorities
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Priority.entries.forEach { priority ->
            Pill(priority.label, priority in selected, {
                viewModel.updatePreferences { prefs ->
                    prefs.copy(priorities = prefs.priorities.toMutableSet().apply { if (!add(priority)) remove(priority) })
                }
            })
        }
    }
}

@Composable
private fun AppliancesStep(viewModel: WeekplateViewModel) {
    val selected = viewModel.uiState.preferences.appliances
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Appliance.entries.forEach { appliance ->
            val checked = appliance in selected
            Surface(
                Modifier.fillMaxWidth().selectable(checked, role = Role.Checkbox) {
                    viewModel.updatePreferences { prefs ->
                        prefs.copy(appliances = prefs.appliances.toMutableSet().apply { if (!add(appliance)) remove(appliance) })
                    }
                },
                color = if (checked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.large
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked, onCheckedChange = null)
                    Text(appliance.label, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun ReadyStep() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf(
            "Un menú organizado por días",
            "Ingredientes combinados por pasillos",
            "Cantidades adaptadas a tu mesa",
            "Recetas que puedes cambiar en cualquier momento"
        ).forEachIndexed { index, text ->
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.large) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${index + 1}", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(14.dp))
                    Text(text, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun DietStep(viewModel: WeekplateViewModel) {
    val prefs = viewModel.uiState.preferences
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Diet.entries.forEach { diet ->
            Pill(diet.label, diet in prefs.diets, {
                viewModel.updatePreferences { current ->
                    current.copy(diets = current.diets.toMutableSet().apply { if (!add(diet)) remove(diet) })
                }
            })
        }
    }
    Spacer(Modifier.height(20.dp))
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.large) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Checkbox(
                checked = prefs.allergyDisclaimerAccepted,
                onCheckedChange = { checked -> viewModel.updatePreferences { it.copy(allergyDisclaimerAccepted = checked) } }
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Entiendo que debo comprobar siempre etiquetas y contaminación cruzada. Esta app no sustituye consejo médico.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun DislikesStep(viewModel: WeekplateViewModel) {
    val choices = listOf("Champiñones", "Pescado", "Picante", "Cilantro", "Berenjena", "Queso azul")
    val selected = viewModel.uiState.preferences.dislikes
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        choices.forEach { dislike ->
            Pill(dislike, dislike in selected, {
                viewModel.updatePreferences { prefs ->
                    prefs.copy(dislikes = prefs.dislikes.toMutableSet().apply { if (!add(dislike)) remove(dislike) })
                }
            })
        }
    }
}
