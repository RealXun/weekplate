package com.cleanroom.weekplate

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cleanroom.weekplate.core.engine.PlanEngine
import com.cleanroom.weekplate.core.engine.RecipeFilter
import com.cleanroom.weekplate.core.engine.RecipeSearch
import com.cleanroom.weekplate.core.engine.ShoppingListAggregator
import com.cleanroom.weekplate.core.model.*
import com.cleanroom.weekplate.data.LocalPreferences
import com.cleanroom.weekplate.data.CookingSession
import com.cleanroom.weekplate.data.SampleCatalog
import com.cleanroom.weekplate.data.catalog.CatalogMatcher
import com.cleanroom.weekplate.data.catalog.CatalogProduct
import com.cleanroom.weekplate.data.catalog.CatalogRepository
import com.cleanroom.weekplate.data.catalog.CatalogSyncScheduler
import com.cleanroom.weekplate.data.catalog.CatalogUiState
import com.cleanroom.weekplate.notifications.CookTimerScheduler
import com.cleanroom.weekplate.notifications.ReminderScheduler
import com.cleanroom.weekplate.network.HouseholdClient
import com.cleanroom.weekplate.network.HouseholdConflictException
import com.cleanroom.weekplate.network.RemoteHousehold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.UUID

sealed interface Destination {
    data object Onboarding : Destination
    data object Main : Destination
    data object Shopping : Destination
    data class RecipeDetails(val recipeId: String) : Destination
    data class Cooking(val recipeId: String, val servings: Int) : Destination
    data object Preferences : Destination
    data object History : Destination
    data object Household : Destination
    data object About : Destination
}

data class WeekplateUiState(
    val rootState: RootState,
    val destination: Destination,
    val selectedTab: MainTab = MainTab.PLAN,
    val onboardingStep: Int = 0,
    val preferences: UserPreferences,
    val plan: WeekPlan? = null,
    val favourites: Set<String> = emptySet(),
    val checkedShoppingItems: Set<String> = emptySet(),
    val combineNextWeekShopping: Boolean = false,
    val reminderEnabled: Boolean = false,
    val searchQuery: String = "",
    val discoverPriority: Priority? = null,
    val discoverDiet: Diet? = null,
    val discoverCuisine: String? = null,
    val message: String? = null
)

data class HouseholdUiState(
    val serverUrl: String = "",
    val joinCode: String = "",
    val code: String = "",
    val revision: Long = 0,
    val devices: Int = 1,
    val loading: Boolean = false,
    val status: String = "Sin conectar"
) {
    val connected: Boolean get() = code.isNotBlank()
}

class WeekplateViewModel(application: Application) : AndroidViewModel(application) {
    private val local = LocalPreferences(application)
    private val catalogRepository = CatalogRepository(application)
    private val planEngine = PlanEngine()
    private val shoppingAggregator = ShoppingListAggregator()
    val recipes: List<Recipe> = SampleCatalog.recipes
    val stores: List<GroceryStore> = SampleCatalog.stores

    private val monday: LocalDate = LocalDate.now().with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
    private val initialPreferences = local.loadUserPreferences()
    private val initialPlan = if (local.onboardingComplete) restoredOrGeneratedPlan(initialPreferences) else null
    private val initialHousehold = local.loadHouseholdConnection()
    private val initialCatalog = catalogRepository.loadSnapshot()

    var uiState by mutableStateOf(
        WeekplateUiState(
            rootState = if (local.onboardingComplete) RootState.MAIN else RootState.ONBOARDING,
            destination = if (local.onboardingComplete) Destination.Main else Destination.Onboarding,
            preferences = initialPreferences,
            plan = initialPlan,
            favourites = local.favourites,
            checkedShoppingItems = initialPlan?.checkedItems.orEmpty(),
            reminderEnabled = local.reminderEnabled
        )
    )
        private set

    var householdState by mutableStateOf(
        HouseholdUiState(
            serverUrl = initialHousehold?.baseUrl ?: local.savedHouseholdBaseUrl(),
            code = initialHousehold?.code.orEmpty(),
            revision = initialHousehold?.revision ?: 0,
            devices = initialHousehold?.devices ?: 1,
            status = if (initialHousehold == null) "Sin conectar" else "Conectado"
        )
    )
        private set

    var catalogState by mutableStateOf(
        CatalogUiState(
            feedUrl = catalogRepository.feedUrl(),
            snapshot = initialCatalog,
            statusMessage = when {
                catalogRepository.feedUrl().isBlank() && initialCatalog != null -> "Caché disponible; sincronización desactivada"
                catalogRepository.feedUrl().isBlank() -> "Catálogo externo sin configurar"
                initialCatalog != null -> "Catálogo guardado disponible"
                else -> catalogRepository.lastError() ?: "Catálogo configurado; pendiente de sincronizar"
            },
            lastCheckedEpochMillis = catalogRepository.lastCheckedEpochMillis()
        )
    )
        private set

    val shoppingItems: List<ShoppingItem>
        get() = uiState.plan?.let { plan ->
            val plans = buildList {
                add(plan)
                if (uiState.combineNextWeekShopping) {
                    local.loadPlanForWeek(plan.weekStart.plusWeeks(1))?.let(::add)
                }
            }
            shoppingAggregator.aggregate(
                plans,
                recipes,
                uiState.preferences.alreadyHave,
                uiState.checkedShoppingItems
            )
        }.orEmpty()

    val shoppingWeekCount: Int
        get() = if (uiState.combineNextWeekShopping) 2 else 1

    fun catalogProductFor(item: ShoppingItem): CatalogProduct? = CatalogMatcher.productFor(
        snapshot = catalogState.snapshot,
        storeId = uiState.preferences.store?.id,
        itemName = item.name
    )

    fun setCatalogFeedUrl(value: String) {
        catalogState = catalogState.copy(feedUrl = value, statusMessage = "Cambios sin guardar")
    }

    fun saveCatalogFeedUrl() {
        val value = catalogState.feedUrl.trim()
        if (value.isNotEmpty() && !CatalogRepository.isAllowedFeedUrl(value)) {
            catalogState = catalogState.copy(statusMessage = "Usa una URL HTTPS válida")
            return
        }
        catalogRepository.saveFeedUrl(value)
        CatalogSyncScheduler.scheduleIfConfigured(getApplication())
        if (value.isBlank()) {
            catalogState = catalogState.copy(
                feedUrl = "",
                syncing = false,
                statusMessage = if (catalogState.snapshot == null) {
                    "Catálogo externo sin configurar"
                } else {
                    "Caché disponible; sincronización desactivada"
                }
            )
        } else {
            catalogState = catalogState.copy(feedUrl = value)
            refreshCatalog()
        }
    }

    fun refreshCatalog() {
        if (!CatalogRepository.isAllowedFeedUrl(catalogRepository.feedUrl())) {
            catalogState = catalogState.copy(statusMessage = "Guarda primero una URL HTTPS válida")
            return
        }
        catalogState = catalogState.copy(syncing = true, statusMessage = "Actualizando catálogo…")
        viewModelScope.launch {
            val result = runCatching { withContext(Dispatchers.IO) { catalogRepository.refresh() } }
            result.onSuccess { snapshot ->
                catalogState = catalogState.copy(
                    snapshot = snapshot,
                    syncing = false,
                    statusMessage = "${snapshot.productCount} precios actualizados",
                    lastCheckedEpochMillis = catalogRepository.lastCheckedEpochMillis()
                )
            }.onFailure { error ->
                catalogState = catalogState.copy(
                    syncing = false,
                    statusMessage = error.message ?: "No se pudo actualizar el catálogo"
                )
            }
        }
    }

    val filteredRecipes: List<Recipe>
        get() = RecipeSearch.filter(
            recipes,
            RecipeFilter(
                query = uiState.searchQuery,
                priority = uiState.discoverPriority,
                diet = uiState.discoverDiet,
                cuisine = uiState.discoverCuisine
            )
        )

    val discoverCuisines: List<String>
        get() = recipes.map { it.cuisine }.distinct().sorted()

    val isViewingCurrentWeek: Boolean
        get() = uiState.plan?.weekStart == monday

    fun selectTab(tab: MainTab) {
        uiState = uiState.copy(selectedTab = tab, destination = Destination.Main, message = null)
    }

    fun navigate(destination: Destination) {
        uiState = uiState.copy(destination = destination, message = null)
    }

    fun back() {
        uiState = uiState.copy(destination = Destination.Main, message = null)
    }

    fun updatePreferences(transform: (UserPreferences) -> UserPreferences) {
        val revised = transform(uiState.preferences)
        uiState = uiState.copy(preferences = revised, message = null)
    }

    fun nextOnboardingStep(lastStep: Int) {
        if (uiState.onboardingStep < lastStep) {
            uiState = uiState.copy(onboardingStep = uiState.onboardingStep + 1, message = null)
        } else {
            finishOnboarding()
        }
    }

    fun previousOnboardingStep() {
        uiState = uiState.copy(onboardingStep = (uiState.onboardingStep - 1).coerceAtLeast(0), message = null)
    }

    fun onboardingValidationMessage(step: Int): String? = when (step) {
        0 -> if (uiState.preferences.store == null) "Selecciona dónde haces la compra" else null
        2 -> if (uiState.preferences.cookingDays.isEmpty()) "Elige al menos un día" else null
        3 -> if (uiState.preferences.priorities.isEmpty()) "Elige al menos una prioridad" else null
        4 -> if (uiState.preferences.appliances.isEmpty()) "Elige al menos un equipo de cocina" else null
        else -> null
    }

    fun showMessage(text: String) {
        uiState = uiState.copy(message = text)
    }

    fun consumeMessage() {
        uiState = uiState.copy(message = null)
    }

    private fun finishOnboarding() {
        local.saveUserPreferences(uiState.preferences, completeOnboarding = true)
        val plan = generate(uiState.preferences)
        plan?.let { local.saveWeekPlan(it, monday) }
        uiState = uiState.copy(
            rootState = RootState.MAIN,
            destination = Destination.Main,
            plan = plan,
            checkedShoppingItems = plan?.checkedItems.orEmpty(),
            message = null
        )
    }

    fun savePreferencesAndRegenerate() {
        local.saveUserPreferences(uiState.preferences)
        val plan = generate(uiState.preferences)
        plan?.let { local.saveWeekPlan(it, monday) }
        uiState = uiState.copy(
            plan = plan,
            checkedShoppingItems = plan?.checkedItems.orEmpty(),
            destination = Destination.Main
        )
    }

    fun regeneratePlan() {
        val weekStart = uiState.plan?.weekStart ?: monday
        val plan = runCatching {
            planEngine.generate(uiState.preferences, recipes, weekStart, System.nanoTime())
        }.getOrElse {
            uiState = uiState.copy(
                plan = null,
                message = "No hay recetas compatibles con todas las restricciones"
            )
            return
        }
        local.saveWeekPlan(plan, monday)
        uiState = uiState.copy(
            plan = plan,
            checkedShoppingItems = emptySet(),
            combineNextWeekShopping = false,
            message = "Semana renovada"
        )
    }

    fun showPreviousWeek() {
        showWeek((uiState.plan?.weekStart ?: monday).minusWeeks(1))
    }

    fun showNextWeek() {
        showWeek((uiState.plan?.weekStart ?: monday).plusWeeks(1))
    }

    fun showCurrentWeek() {
        showWeek(monday)
    }

    private fun showWeek(weekStart: LocalDate) {
        val plan = local.loadPlanForWeek(weekStart) ?: generate(uiState.preferences, weekStart)?.also {
            local.saveWeekPlan(it, monday)
        }
        uiState = if (plan == null) {
            uiState.copy(message = "No hay recetas compatibles con todas las restricciones")
        } else {
            uiState.copy(
                plan = plan,
                checkedShoppingItems = plan.checkedItems,
                combineNextWeekShopping = false,
                message = null
            )
        }
    }

    fun toggleCombineNextWeekShopping() {
        val current = uiState.plan ?: return
        if (uiState.combineNextWeekShopping) {
            uiState = uiState.copy(combineNextWeekShopping = false)
            return
        }
        val nextStart = current.weekStart.plusWeeks(1)
        val next = local.loadPlanForWeek(nextStart) ?: generate(uiState.preferences, nextStart)?.also {
            local.saveWeekPlan(it, monday)
        }
        uiState = if (next == null) {
            uiState.copy(message = "No se pudo crear la semana siguiente con estas restricciones")
        } else {
            uiState.copy(combineNextWeekShopping = true, message = "Lista combinada con la semana siguiente")
        }
    }

    fun toggleFavourite(recipeId: String) {
        val revised = uiState.favourites.toMutableSet().apply {
            if (!add(recipeId)) remove(recipeId)
        }.toSet()
        local.saveFavourites(revised)
        uiState = uiState.copy(favourites = revised)
    }

    fun toggleMealCooked(mealId: String) {
        val plan = uiState.plan ?: return
        val revised = plan.copy(meals = plan.meals.map {
            if (it.id == mealId) it.copy(isCooked = !it.isCooked) else it
        })
        local.saveWeekPlan(revised, monday)
        uiState = uiState.copy(plan = revised)
    }

    fun swapMeal(mealId: String) {
        val plan = uiState.plan ?: return
        val revised = planEngine.swapMeal(plan, mealId, recipes, uiState.preferences)
        local.saveWeekPlan(revised, monday)
        uiState = uiState.copy(plan = revised)
    }

    fun omitMeal(mealId: String) {
        val plan = uiState.plan ?: return
        val revised = plan.copy(meals = plan.meals.map {
            if (it.id == mealId) it.copy(omitted = !it.omitted) else it
        })
        local.saveWeekPlan(revised, monday)
        uiState = uiState.copy(plan = revised)
    }

    fun addRecipeToPlan(recipeId: String) {
        val plan = uiState.plan ?: return
        val occupiedDays = plan.meals.map { it.day }.toSet()
        val day = Weekday.entries.firstOrNull { it !in occupiedDays } ?: Weekday.entries.first()
        val meal = PlannedMeal(recipeId = recipeId, day = day, slot = MealSlot.DINNER, servings = plan.people)
        val recipe = recipes.first { it.id == recipeId }
        val cost = plan.estimatedSpend + recipe.estimatedCost(plan.people)
        val revised = plan.copy(meals = plan.meals + meal, estimatedSpend = cost, overBudget = cost > plan.budget)
        local.saveWeekPlan(revised, monday)
        uiState = uiState.copy(
            plan = revised,
            message = "Añadida al menú"
        )
    }

    fun addExtraShoppingItem(name: String) {
        if (name.isBlank()) return
        val plan = uiState.plan ?: return
        val revised = plan.copy(extraItems = plan.extraItems + name.trim())
        local.saveWeekPlan(revised, monday)
        uiState = uiState.copy(plan = revised)
    }

    fun toggleShoppingItem(id: String) {
        val checked = uiState.checkedShoppingItems.toMutableSet().apply {
            if (!add(id)) remove(id)
        }.toSet()
        val revised = uiState.plan?.copy(checkedItems = checked)
        revised?.let { local.saveWeekPlan(it, monday) }
        uiState = uiState.copy(plan = revised ?: uiState.plan, checkedShoppingItems = checked)
    }

    fun setSearchQuery(query: String) {
        uiState = uiState.copy(searchQuery = query)
    }

    fun setDiscoverPriority(priority: Priority?) {
        uiState = uiState.copy(discoverPriority = priority)
    }

    fun setDiscoverDiet(diet: Diet?) {
        uiState = uiState.copy(discoverDiet = diet)
    }

    fun setDiscoverCuisine(cuisine: String?) {
        uiState = uiState.copy(discoverCuisine = cuisine)
    }

    fun clearDiscoverFilters() {
        uiState = uiState.copy(discoverPriority = null, discoverDiet = null, discoverCuisine = null)
    }

    fun toggleReminder(enabled: Boolean) {
        local.saveReminder(enabled)
        if (enabled) ReminderScheduler.schedule(getApplication()) else ReminderScheduler.cancel(getApplication())
        uiState = uiState.copy(reminderEnabled = enabled)
    }

    fun cookingSession(recipeId: String): CookingSession? = local.loadCookingSession(recipeId)

    fun cookingRemainingSeconds(recipeId: String): Int =
        local.loadCookingSession(recipeId)?.remainingAt() ?: 0

    fun updateCookingStep(recipeId: String, step: Int, servings: Int) {
        val existing = local.loadCookingSession(recipeId)
        local.saveCookingSession(
            existing?.copy(step = step, servings = servings)
                ?: CookingSession(recipeId, step, servings, 0, false, 0)
        )
    }

    fun startCookingTimer(recipeId: String, step: Int, servings: Int, seconds: Int): CookingSession {
        val safeSeconds = seconds.coerceAtLeast(1)
        val session = CookingSession(
            recipeId = recipeId,
            step = step,
            servings = servings,
            remainingSeconds = safeSeconds,
            running = true,
            endsAtEpochMillis = System.currentTimeMillis() + safeSeconds * 1_000L
        )
        local.saveCookingSession(session)
        CookTimerScheduler.schedule(getApplication(), session)
        return session
    }

    fun pauseCookingTimer(recipeId: String, step: Int, servings: Int): Int {
        val remaining = cookingRemainingSeconds(recipeId)
        CookTimerScheduler.cancel(getApplication())
        local.saveCookingSession(CookingSession(recipeId, step, servings, remaining, false, 0))
        return remaining
    }

    fun savePausedCookingTimer(recipeId: String, step: Int, servings: Int, seconds: Int) {
        CookTimerScheduler.cancel(getApplication())
        local.saveCookingSession(CookingSession(recipeId, step, servings, seconds.coerceAtLeast(0), false, 0))
    }

    fun clearCookingTimer() {
        CookTimerScheduler.cancel(getApplication())
        local.clearCookingSession()
    }

    fun setHouseholdServerUrl(value: String) {
        householdState = householdState.copy(serverUrl = value, status = if (householdState.connected) householdState.status else "Sin conectar")
    }

    fun setHouseholdJoinCode(value: String) {
        householdState = householdState.copy(
            joinCode = value.filter(Char::isLetterOrDigit).uppercase().take(6)
        )
    }

    fun createHousehold() {
        val url = householdState.serverUrl
        val payload = local.exportHouseholdPayload(uiState.preferences)
        runHouseholdAction("Creando hogar…") {
            val client = HouseholdClient(url)
            val created = client.create()
            client.update(created.code, created.revision, payload)
        }
    }

    fun joinHousehold() {
        val url = householdState.serverUrl
        val code = householdState.joinCode
        runHouseholdAction("Uniendo dispositivo…", importRemote = true) {
            HouseholdClient(url).join(code)
        }
    }

    fun refreshHousehold() {
        if (!householdState.connected) return
        val url = householdState.serverUrl
        val code = householdState.code
        runHouseholdAction("Actualizando…", importRemote = true) {
            HouseholdClient(url).get(code)
        }
    }

    fun publishHousehold() {
        if (!householdState.connected) return
        val url = householdState.serverUrl
        val code = householdState.code
        val revision = householdState.revision
        val payload = local.exportHouseholdPayload(uiState.preferences)
        runHouseholdAction("Publicando cambios…") {
            HouseholdClient(url).update(code, revision, payload)
        }
    }

    fun leaveHousehold() {
        local.clearHouseholdConnection()
        householdState = householdState.copy(code = "", revision = 0, devices = 1, status = "Desconectado localmente")
    }

    private fun runHouseholdAction(
        pending: String,
        importRemote: Boolean = false,
        block: () -> RemoteHousehold
    ) {
        if (!HouseholdClient.isAllowedBaseUrl(householdState.serverUrl)) {
            householdState = householdState.copy(status = "Usa una URL HTTPS válida")
            return
        }
        householdState = householdState.copy(loading = true, status = pending)
        viewModelScope.launch {
            val result = runCatching { withContext(Dispatchers.IO) { block() } }
            result.onSuccess { remote -> applyRemoteHousehold(remote, importRemote) }
                .onFailure { error ->
                    householdState = householdState.copy(
                        loading = false,
                        status = when (error) {
                            is HouseholdConflictException -> "Conflicto: actualiza antes de publicar"
                            is IllegalArgumentException -> error.message ?: "Configuración no válida"
                            else -> error.message ?: "No se pudo contactar el servidor"
                        }
                    )
                }
        }
    }

    private fun applyRemoteHousehold(remote: RemoteHousehold, importRemote: Boolean) {
        if (importRemote && remote.payload != "{}") {
            val imported = local.importHouseholdPayload(remote.payload)
            val allowedRecipeIds = recipes.mapTo(mutableSetOf()) { it.id }
            val valid = imported != null &&
                imported.preferences.cookingDays.isNotEmpty() &&
                imported.preferences.appliances.isNotEmpty() &&
                imported.plan?.meals.orEmpty().all { it.recipeId in allowedRecipeIds }
            if (!valid) {
                householdState = householdState.copy(loading = false, status = "Instantánea remota incompatible")
                return
            }
            local.saveUserPreferences(imported!!.preferences)
            imported.plan?.let { local.saveWeekPlan(it, monday) }
            uiState = uiState.copy(
                preferences = imported.preferences,
                plan = imported.plan ?: uiState.plan,
                checkedShoppingItems = imported.plan?.checkedItems ?: uiState.checkedShoppingItems
            )
        }
        local.saveHouseholdConnection(householdState.serverUrl, remote.code, remote.revision, remote.devices)
        householdState = householdState.copy(
            code = remote.code,
            joinCode = remote.code,
            revision = remote.revision,
            devices = remote.devices,
            loading = false,
            status = "Sincronizado"
        )
    }

    fun resetAll() {
        ReminderScheduler.cancel(getApplication())
        CookTimerScheduler.cancel(getApplication())
        local.reset()
        val defaults = UserPreferences()
        uiState = WeekplateUiState(
            rootState = RootState.ONBOARDING,
            destination = Destination.Onboarding,
            preferences = defaults
        )
        householdState = HouseholdUiState()
    }

    fun recipe(id: String): Recipe? = recipes.firstOrNull { it.id == id }

    fun planHistory(): List<WeekPlan> = (listOfNotNull(uiState.plan) + local.loadAllPlans())
        .filter { !it.weekStart.isAfter(monday) }
        .distinctBy { it.weekStart }
        .sortedByDescending { it.weekStart }

    private fun generate(preferences: UserPreferences, weekStart: LocalDate = monday): WeekPlan? =
        runCatching { planEngine.generate(preferences, recipes, weekStart) }.getOrNull()

    private fun restoredOrGeneratedPlan(preferences: UserPreferences): WeekPlan? {
        val stored = local.loadPlanForWeek(monday)
        if (stored != null) {
            local.saveWeekPlan(stored, monday)
            return stored
        }
        return generate(preferences)?.also { generated ->
            local.saveWeekPlan(generated, monday)
        }
    }
}
