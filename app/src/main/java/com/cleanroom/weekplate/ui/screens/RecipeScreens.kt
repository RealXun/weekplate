package com.cleanroom.weekplate.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.cleanroom.weekplate.Destination
import com.cleanroom.weekplate.WeekplateViewModel
import com.cleanroom.weekplate.core.model.Recipe
import com.cleanroom.weekplate.ui.components.RecipeArtwork
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun SimpleTopBar(title: String, onBack: () -> Unit, action: (@Composable () -> Unit)? = null) {
    Surface(shadowElevation = 2.dp) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().heightIn(min = 58.dp).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text("‹ Atrás") }
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Box(Modifier.widthIn(min = 72.dp), contentAlignment = Alignment.CenterEnd) { action?.invoke() }
        }
    }
}

@Composable
fun RecipeDetailScreen(viewModel: WeekplateViewModel, recipeId: String) {
    val recipe = viewModel.recipe(recipeId) ?: run {
        EmptyRecipe(viewModel)
        return
    }
    var servings by rememberSaveable(recipeId) { mutableIntStateOf(recipe.serves) }
    val favourite = recipe.id in viewModel.uiState.favourites
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        SimpleTopBar("Receta", viewModel::back) {
            TextButton(onClick = { viewModel.toggleFavourite(recipe.id) }) { Text(if (favourite) "♥" else "♡") }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            RecipeArtwork(recipe, Modifier.fillMaxWidth().height(230.dp).padding(20.dp))
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text(recipe.cuisine.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(7.dp))
                Text(recipe.name, style = MaterialTheme.typography.displaySmall)
                Spacer(Modifier.height(10.dp))
                Text(recipe.blurb, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatPill("${recipe.timeMinutes} min", Modifier.weight(1f))
                    StatPill("${recipe.nutrition.calories} kcal", Modifier.weight(1f))
                    StatPill("${recipe.nutrition.proteinGrams} g proteína", Modifier.weight(1f))
                }
                Spacer(Modifier.height(26.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Ingredientes", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = { if (servings > 1) servings-- }, contentPadding = PaddingValues(horizontal = 12.dp)) { Text("−") }
                    Text("$servings", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 12.dp))
                    OutlinedButton(onClick = { if (servings < 12) servings++ }, contentPadding = PaddingValues(horizontal = 12.dp)) { Text("+") }
                }
                Spacer(Modifier.height(9.dp))
                recipe.ingredients.forEach { ingredient ->
                    val scaled = ingredient.amount * servings / recipe.serves
                    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
                        Text(ingredient.name, modifier = Modifier.weight(1f))
                        Text("${formatAmount(scaled)} ${ingredient.unit}", fontWeight = FontWeight.SemiBold)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                Spacer(Modifier.height(26.dp))
                Text("Método", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(10.dp))
                recipe.steps.forEachIndexed { index, step ->
                    Row(Modifier.padding(vertical = 9.dp), verticalAlignment = Alignment.Top) {
                        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(50)) {
                            Text("${index + 1}", modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp), fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(step, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                Spacer(Modifier.height(24.dp))
                NutritionCard(recipe)
                Spacer(Modifier.height(28.dp))
            }
        }
        Surface(shadowElevation = 8.dp) {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { viewModel.addRecipeToPlan(recipe.id) },
                    modifier = Modifier.weight(1f).height(52.dp)
                ) { Text("Añadir al menú") }
                Button(
                    onClick = { viewModel.navigate(Destination.Cooking(recipe.id, servings)) },
                    modifier = Modifier.weight(1f).height(52.dp)
                ) { Text("Empezar a cocinar") }
            }
        }
    }
}

@Composable
private fun StatPill(text: String, modifier: Modifier = Modifier) {
    Surface(modifier, color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp)) {
        Text(text, modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
    }
}

@Composable
private fun NutritionCard(recipe: Recipe) {
    Surface(color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(18.dp)) {
            Text("NUTRICIÓN POR RACIÓN", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primaryContainer)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                Macro("Energía", "${recipe.nutrition.calories}")
                Macro("Proteína", "${recipe.nutrition.proteinGrams} g")
                Macro("Hidratos", "${recipe.nutrition.carbsGrams} g")
                Macro("Grasas", "${recipe.nutrition.fatGrams} g")
            }
        }
    }
}

@Composable
private fun Macro(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .75f))
    }
}

private fun formatAmount(value: Double): String = when {
    value % 1.0 == 0.0 -> value.toInt().toString()
    value < 10 -> String.format(Locale.US, "%.1f", value)
    else -> value.toInt().toString()
}

@Composable
private fun EmptyRecipe(viewModel: WeekplateViewModel) {
    Column(Modifier.fillMaxSize()) {
        SimpleTopBar("Receta", viewModel::back)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("La receta ya no está disponible") }
    }
}

@Composable
fun CookingScreen(viewModel: WeekplateViewModel, recipeId: String, servings: Int) {
    val recipe = viewModel.recipe(recipeId) ?: return
    val context = LocalContext.current
    val restored = remember(recipeId) { viewModel.cookingSession(recipeId) }
    var step by rememberSaveable(recipeId) {
        mutableIntStateOf(restored?.step?.coerceIn(0, recipe.steps.lastIndex) ?: 0)
    }
    var remainingSeconds by rememberSaveable(recipeId) {
        mutableIntStateOf(restored?.remainingAt() ?: 0)
    }
    var timerRunning by rememberSaveable(recipeId) {
        mutableStateOf(restored?.running == true && (restored.remainingAt() > 0))
    }

    val startTimer: () -> Unit = {
        val seconds = remainingSeconds.takeIf { it > 0 } ?: 300
        viewModel.startCookingTimer(recipeId, step, servings, seconds)
        remainingSeconds = seconds
        timerRunning = true
    }
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) viewModel.showMessage("El temporizador seguirá activo, pero Android no mostrará el aviso")
        startTimer()
    }

    val startWithPermission: () -> Unit = {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startTimer()
        }
    }

    LaunchedEffect(timerRunning, recipeId) {
        while (timerRunning) {
            remainingSeconds = viewModel.cookingRemainingSeconds(recipeId)
            if (remainingSeconds <= 0) {
                timerRunning = false
            } else {
                delay(500)
            }
        }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        SimpleTopBar("Modo cocina", viewModel::back) {
            Text("${step + 1}/${recipe.steps.size}", style = MaterialTheme.typography.labelLarge)
        }
        LinearProgressIndicator(
            progress = { (step + 1f) / recipe.steps.size },
            modifier = Modifier.fillMaxWidth().height(6.dp)
        )
        Column(
            Modifier.weight(1f).padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("PASO ${step + 1}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(15.dp))
                Text(recipe.steps[step], style = MaterialTheme.typography.displaySmall)
                Spacer(Modifier.height(18.dp))
                Text("Para $servings raciones", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TimerPanel(remainingSeconds, timerRunning, onAddMinute = {
                remainingSeconds += 60
                if (timerRunning) {
                    viewModel.startCookingTimer(recipeId, step, servings, remainingSeconds)
                } else {
                    viewModel.savePausedCookingTimer(recipeId, step, servings, remainingSeconds)
                }
            }, onToggle = {
                if (timerRunning) {
                    remainingSeconds = viewModel.pauseCookingTimer(recipeId, step, servings)
                    timerRunning = false
                } else {
                    startWithPermission()
                }
            }, onReset = {
                remainingSeconds = 0
                timerRunning = false
                viewModel.clearCookingTimer()
            })
        }
        Surface(shadowElevation = 8.dp) {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        step = (step - 1).coerceAtLeast(0)
                        viewModel.updateCookingStep(recipeId, step, servings)
                    },
                    enabled = step > 0,
                    modifier = Modifier.weight(1f).height(54.dp)
                ) { Text("Anterior") }
                Button(
                    onClick = {
                        if (step < recipe.steps.lastIndex) {
                            step++
                            viewModel.updateCookingStep(recipeId, step, servings)
                        } else {
                            if (!timerRunning && remainingSeconds == 0) viewModel.clearCookingTimer()
                            viewModel.back()
                        }
                    },
                    modifier = Modifier.weight(1f).height(54.dp)
                ) { Text(if (step == recipe.steps.lastIndex) "Terminar" else "Siguiente") }
            }
        }
    }
}

@Composable
private fun TimerPanel(
    seconds: Int,
    running: Boolean,
    onAddMinute: () -> Unit,
    onToggle: () -> Unit,
    onReset: () -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(26.dp)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("TEMPORIZADOR", style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(8.dp))
            Text("%02d:%02d".format(seconds / 60, seconds % 60), style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onAddMinute) { Text("+1 min") }
                Button(onClick = onToggle) { Text(if (running) "Pausar" else if (seconds > 0) "Continuar" else "5 min") }
                TextButton(onClick = onReset, enabled = seconds > 0) { Text("Borrar") }
            }
            Text(
                when {
                    running -> "La alarma continúa si sales de la app"
                    seconds > 0 -> "Temporizador en pausa"
                    else -> "Inicia cinco minutos o añade tiempo"
                },
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
