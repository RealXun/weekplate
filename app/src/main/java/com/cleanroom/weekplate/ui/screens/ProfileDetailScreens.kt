package com.cleanroom.weekplate.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cleanroom.weekplate.WeekplateViewModel
import com.cleanroom.weekplate.core.model.*
import com.cleanroom.weekplate.ui.components.Pill
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun PreferencesScreen(viewModel: WeekplateViewModel) {
    val prefs = viewModel.uiState.preferences
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        SimpleTopBar("Preferencias", viewModel::back)
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                PreferenceHeading("Presupuesto semanal", "${prefs.budget.roundToInt()} €")
                Slider(
                    value = prefs.budget.toFloat(),
                    onValueChange = { value -> viewModel.updatePreferences { it.copy(budget = value.roundToInt().toDouble()) } },
                    valueRange = 25f..160f,
                    steps = 26
                )
            }
            item {
                PreferenceHeading("Personas", prefs.people.toString())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (1..6).forEach { people ->
                        Pill("$people", people == prefs.people, { viewModel.updatePreferences { it.copy(people = people) } })
                    }
                }
            }
            item {
                PreferenceHeading("Días de cocina", "${prefs.cookingDays.size}")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Weekday.entries.forEach { day ->
                        FilterChip(
                            selected = day in prefs.cookingDays,
                            onClick = {
                                viewModel.updatePreferences { current ->
                                    current.copy(cookingDays = current.cookingDays.toMutableSet().apply { if (!add(day)) remove(day) })
                                }
                            },
                            label = { Text(day.shortLabel) }
                        )
                    }
                }
            }
            item {
                PreferenceHeading("Prioridades", "")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Priority.entries.forEach { priority ->
                        Pill(priority.label, priority in prefs.priorities, {
                            viewModel.updatePreferences { current ->
                                current.copy(priorities = current.priorities.toMutableSet().apply { if (!add(priority)) remove(priority) })
                            }
                        })
                    }
                }
            }
            item {
                PreferenceHeading("Dieta y exclusiones", "")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Diet.entries.forEach { diet ->
                        Pill(diet.label, diet in prefs.diets, {
                            viewModel.updatePreferences { current ->
                                current.copy(diets = current.diets.toMutableSet().apply { if (!add(diet)) remove(diet) })
                            }
                        })
                    }
                }
            }
            item {
                PreferenceHeading("Equipamiento", "")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Appliance.entries.forEach { appliance ->
                        Pill(appliance.label, appliance in prefs.appliances, {
                            viewModel.updatePreferences { current ->
                                current.copy(appliances = current.appliances.toMutableSet().apply { if (!add(appliance)) remove(appliance) })
                            }
                        })
                    }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Solo apto para niños", style = MaterialTheme.typography.titleMedium)
                        Text("Limita el catálogo disponible", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(prefs.kidFriendly, onCheckedChange = { value -> viewModel.updatePreferences { it.copy(kidFriendly = value) } })
                }
            }
        }
        Surface(shadowElevation = 8.dp) {
            Button(
                onClick = {
                    if (prefs.cookingDays.isEmpty() || prefs.appliances.isEmpty()) {
                        viewModel.showMessage("Selecciona al menos un día y un equipo")
                    } else viewModel.savePreferencesAndRegenerate()
                },
                modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp)
            ) { Text("Guardar y rehacer el menú") }
        }
    }
}

@Composable
private fun PreferenceHeading(title: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        if (value.isNotBlank()) Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
fun HistoryScreen(viewModel: WeekplateViewModel) {
    val formatter = DateTimeFormatter.ofPattern("d 'de' MMMM", Locale("es", "ES"))
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        SimpleTopBar("Semanas anteriores", viewModel::back)
        LazyColumn(
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            viewModel.planHistory().forEachIndexed { index, plan ->
                item(key = plan.weekStart.toString()) {
                    ElevatedCard(shape = RoundedCornerShape(22.dp)) {
                        Column(Modifier.fillMaxWidth().padding(18.dp)) {
                            Text(if (index == 0) "SEMANA ACTUAL" else "MENÚ ANTERIOR", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(5.dp))
                            Text("Desde el ${plan.weekStart.format(formatter)}", style = MaterialTheme.typography.headlineSmall)
                            Spacer(Modifier.height(9.dp))
                            Text("${plan.meals.size} platos · estimación %.2f €".format(Locale.US, plan.estimatedSpend))
                            Text("${plan.meals.count { it.isCooked }} marcados como cocinados", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HouseholdScreen(viewModel: WeekplateViewModel) {
    val state = viewModel.householdState
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        SimpleTopBar("Hogar compartido", viewModel::back)
        LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(26.dp)) {
                    Column(Modifier.padding(20.dp)) {
                        Text("BACKEND PROPIO DISPONIBLE", style = MaterialTheme.typography.labelSmall)
                        Spacer(Modifier.height(8.dp))
                        Text("Comparte un menú sin usar el servicio privado de la app de referencia.", style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Crea códigos de seis caracteres y sincroniza menú, checks y preferencias con control optimista de revisiones.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = state.serverUrl,
                    onValueChange = viewModel::setHouseholdServerUrl,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("URL de tu servidor") },
                    placeholder = { Text("https://hogar.ejemplo.com") },
                    enabled = !state.loading && !state.connected,
                    singleLine = true
                )
                Text(
                    "Se exige HTTPS. En esta compilación debug se permite http://10.0.2.2:8080 para probar el backend local.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                Text("Estado", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                StatusRow("Conexión", state.status)
                StatusRow("Hogar", state.code.ifBlank { "Sin conectar" })
                StatusRow("Revisión", state.revision.toString())
                StatusRow("Dispositivos", state.devices.toString())
                if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            item {
                if (state.connected) {
                    Button(
                        onClick = viewModel::publishHousehold,
                        enabled = !state.loading,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Publicar cambios locales") }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = viewModel::refreshHousehold,
                        enabled = !state.loading,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Traer cambios del hogar") }
                    TextButton(
                        onClick = viewModel::leaveHousehold,
                        enabled = !state.loading,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Desconectar este dispositivo") }
                } else {
                    Button(
                        onClick = viewModel::createHousehold,
                        enabled = !state.loading && state.serverUrl.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Crear hogar") }
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = state.joinCode,
                        onValueChange = viewModel::setHouseholdJoinCode,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Código de seis caracteres") },
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = viewModel::joinHousehold,
                        enabled = !state.loading && state.joinCode.length == 6,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Unirme con el código") }
                }
            }
            item {
                Text(
                    "La aplicación no contacta dominios ni usa credenciales de Whipp. El servidor debe ser desplegado y administrado por ti.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatusRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(label, modifier = Modifier.weight(1f))
        Text(value, fontWeight = FontWeight.SemiBold)
    }
    HorizontalDivider()
}

@Composable
fun AboutScreen(viewModel: WeekplateViewModel) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        SimpleTopBar("Acerca de", viewModel::back)
        LazyColumn(contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            item {
                Text("Weekplate", style = MaterialTheme.typography.displaySmall)
                Text("0.1.0 · implementación clean-room", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                AboutBlock("Independiente", "Código, nombre, recetas, ilustraciones, identificador de aplicación, almacenamiento y backend propios.")
            }
            item {
                AboutBlock("Privacidad", "El prototipo funciona localmente. No contiene claves, credenciales, telemetría ni servicios privados extraídos del APK de referencia.")
            }
            item {
                AboutBlock("Límites actuales", "Precios en vivo, compra online, facturación, avisos persistentes y sincronización desplegada están pendientes o dependen de proveedores externos.")
            }
            item {
                AboutBlock("Seguridad alimentaria", "Comprueba siempre etiquetas, alérgenos, conservación y cocción. Las sugerencias no sustituyen consejo profesional.")
            }
        }
    }
}

@Composable
private fun AboutBlock(title: String, body: String) {
    Surface(shape = RoundedCornerShape(22.dp), tonalElevation = 1.dp) {
        Column(Modifier.padding(18.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(7.dp))
            Text(body, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
