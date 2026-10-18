package com.cleanroom.weekplate.data

import android.content.Context
import com.cleanroom.weekplate.core.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

data class CookingSession(
    val recipeId: String,
    val step: Int,
    val servings: Int,
    val remainingSeconds: Int,
    val running: Boolean,
    val endsAtEpochMillis: Long
) {
    fun remainingAt(nowEpochMillis: Long = System.currentTimeMillis()): Int =
        if (!running) remainingSeconds else
            ((endsAtEpochMillis - nowEpochMillis + 999) / 1_000).coerceAtLeast(0).toInt()
}

data class HouseholdLocalConnection(
    val baseUrl: String,
    val code: String,
    val revision: Long,
    val devices: Int
)

data class ImportedHouseholdPayload(
    val preferences: UserPreferences,
    val plan: WeekPlan?
)

class LocalPreferences(context: Context) {
    private val values = context.getSharedPreferences("weekplate.local", Context.MODE_PRIVATE)

    val onboardingComplete: Boolean get() = values.getBoolean("onboardingComplete", false)
    val favourites: Set<String> get() = values.getStringSet("favourites", emptySet()).orEmpty().toSet()
    val reminderEnabled: Boolean get() = values.getBoolean("reminderEnabled", false)

    fun loadUserPreferences(): UserPreferences {
        fun <T : Enum<T>> enumSet(key: String, entries: List<T>, defaults: Set<T>): Set<T> {
            val names = values.getStringSet(key, null) ?: return defaults
            return entries.filter { it.name in names }.toSet()
        }
        val market = Market.entries.firstOrNull { it.name == values.getString("market", null) } ?: Market.SPAIN
        val storeId = values.getString("store", null)
        return UserPreferences(
            market = market,
            store = SampleCatalog.stores.firstOrNull { it.id == storeId },
            budget = values.getFloat("budget", 65f).toDouble(),
            people = values.getInt("people", 2),
            priorities = enumSet(
                "priorities", Priority.entries,
                setOf(Priority.QUICK_PREP, Priority.HEALTHY)
            ),
            diets = enumSet("diets", Diet.entries, emptySet()),
            appliances = enumSet("appliances", Appliance.entries, setOf(Appliance.OVEN, Appliance.HOB)),
            dislikes = values.getStringSet("dislikes", emptySet()).orEmpty(),
            mealsPerDay = values.getInt("mealsPerDay", 1),
            cookingDays = enumSet("cookingDays", Weekday.entries, Weekday.entries.take(5).toSet()),
            kidFriendly = values.getBoolean("kidFriendly", false),
            allergyDisclaimerAccepted = values.getBoolean("allergyDisclaimer", false),
            alreadyHave = values.getStringSet("alreadyHave", emptySet()).orEmpty(),
            planAroundWhatIHave = values.getBoolean("planAround", false),
            postalCode = values.getString("postalCode", "").orEmpty()
        )
    }

    fun saveUserPreferences(preferences: UserPreferences, completeOnboarding: Boolean = false) {
        values.edit()
            .putString("market", preferences.market.name)
            .putString("store", preferences.store?.id)
            .putFloat("budget", preferences.budget.toFloat())
            .putInt("people", preferences.people)
            .putStringSet("priorities", preferences.priorities.map { it.name }.toSet())
            .putStringSet("diets", preferences.diets.map { it.name }.toSet())
            .putStringSet("appliances", preferences.appliances.map { it.name }.toSet())
            .putStringSet("dislikes", preferences.dislikes)
            .putInt("mealsPerDay", preferences.mealsPerDay)
            .putStringSet("cookingDays", preferences.cookingDays.map { it.name }.toSet())
            .putBoolean("kidFriendly", preferences.kidFriendly)
            .putBoolean("allergyDisclaimer", preferences.allergyDisclaimerAccepted)
            .putStringSet("alreadyHave", preferences.alreadyHave)
            .putBoolean("planAround", preferences.planAroundWhatIHave)
            .putString("postalCode", preferences.postalCode)
            .apply {
                if (completeOnboarding) putBoolean("onboardingComplete", true)
            }
            .apply()
    }

    fun saveFavourites(ids: Set<String>) {
        values.edit().putStringSet("favourites", ids).apply()
    }

    fun saveReminder(enabled: Boolean) {
        values.edit().putBoolean("reminderEnabled", enabled).apply()
    }

    fun loadCurrentPlan(): WeekPlan? = values.getString("currentPlan", null)
        ?.let { runCatching { JSONObject(it).toWeekPlan() }.getOrNull() }

    fun loadPlanHistory(): List<WeekPlan> {
        val raw = values.getString("planHistory", null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    runCatching { array.getJSONObject(index).toWeekPlan() }.getOrNull()?.let(::add)
                }
            }
        }.getOrDefault(emptyList())
    }

    fun loadPlanForWeek(weekStart: LocalDate): WeekPlan? =
        loadCurrentPlan()?.takeIf { it.weekStart == weekStart }
            ?: loadPlanHistory().firstOrNull { it.weekStart == weekStart }

    fun loadAllPlans(): List<WeekPlan> =
        (listOfNotNull(loadCurrentPlan()) + loadPlanHistory())
            .distinctBy { it.weekStart }
            .sortedByDescending { it.weekStart }

    fun saveWeekPlan(plan: WeekPlan, currentWeekStart: LocalDate) {
        val storedCurrent = loadCurrentPlan()
        val storedHistory = loadPlanHistory()
        val editor = values.edit()

        if (plan.weekStart == currentWeekStart) {
            val history = (listOfNotNull(storedCurrent?.takeIf { it.weekStart != currentWeekStart }) + storedHistory)
                .filterNot { it.weekStart == currentWeekStart }
                .distinctBy { it.weekStart }
                .sortedByDescending { it.weekStart }
                .take(24)
            editor
                .putString("currentPlan", plan.toJson().toString())
                .putString("planHistory", history.toPlanJsonArray().toString())
                .apply()
        } else {
            val history = (listOf(plan) + storedHistory)
                .filterNot { it.weekStart == currentWeekStart }
                .distinctBy { it.weekStart }
                .sortedByDescending { it.weekStart }
                .take(24)
            editor.putString("planHistory", history.toPlanJsonArray().toString()).apply()
        }
    }

    fun saveCurrentPlan(plan: WeekPlan, archivePrevious: Boolean = false) {
        val editor = values.edit()
        if (archivePrevious) {
            val previous = loadCurrentPlan()
            if (previous != null && previous.id != plan.id) {
                val history = (listOf(previous) + loadPlanHistory())
                    .distinctBy { it.id }
                    .take(12)
                editor.putString("planHistory", history.toPlanJsonArray().toString())
            }
        }
        editor.putString("currentPlan", plan.toJson().toString()).apply()
    }

    fun loadCookingSession(recipeId: String? = null): CookingSession? {
        val storedRecipe = values.getString("cook.recipeId", null) ?: return null
        if (recipeId != null && storedRecipe != recipeId) return null
        return CookingSession(
            recipeId = storedRecipe,
            step = values.getInt("cook.step", 0),
            servings = values.getInt("cook.servings", 2),
            remainingSeconds = values.getInt("cook.remainingSeconds", 0),
            running = values.getBoolean("cook.running", false),
            endsAtEpochMillis = values.getLong("cook.endsAt", 0)
        )
    }

    fun saveCookingSession(session: CookingSession) {
        values.edit()
            .putString("cook.recipeId", session.recipeId)
            .putInt("cook.step", session.step)
            .putInt("cook.servings", session.servings)
            .putInt("cook.remainingSeconds", session.remainingSeconds)
            .putBoolean("cook.running", session.running)
            .putLong("cook.endsAt", session.endsAtEpochMillis)
            .apply()
    }

    fun finishCookingTimer() {
        val session = loadCookingSession() ?: return
        saveCookingSession(session.copy(remainingSeconds = 0, running = false, endsAtEpochMillis = 0))
    }

    fun clearCookingSession() {
        values.edit()
            .remove("cook.recipeId")
            .remove("cook.step")
            .remove("cook.servings")
            .remove("cook.remainingSeconds")
            .remove("cook.running")
            .remove("cook.endsAt")
            .apply()
    }

    fun loadHouseholdConnection(): HouseholdLocalConnection? {
        val baseUrl = values.getString("household.baseUrl", null) ?: return null
        val code = values.getString("household.code", null) ?: return null
        return HouseholdLocalConnection(
            baseUrl = baseUrl,
            code = code,
            revision = values.getLong("household.revision", 0),
            devices = values.getInt("household.devices", 1)
        )
    }

    fun savedHouseholdBaseUrl(): String = values.getString("household.baseUrl", "").orEmpty()

    fun saveHouseholdConnection(baseUrl: String, code: String, revision: Long, devices: Int) {
        values.edit()
            .putString("household.baseUrl", baseUrl.trim().trimEnd('/'))
            .putString("household.code", code)
            .putLong("household.revision", revision)
            .putInt("household.devices", devices)
            .apply()
    }

    fun clearHouseholdConnection() {
        values.edit()
            .remove("household.code")
            .remove("household.revision")
            .remove("household.devices")
            .apply()
    }

    fun exportHouseholdPayload(preferences: UserPreferences): String = JSONObject()
        .put("schema", 1)
        .put("preferences", preferences.toJson())
        .put("plan", loadCurrentPlan()?.toJson() ?: JSONObject.NULL)
        .toString()

    fun importHouseholdPayload(payload: String): ImportedHouseholdPayload? = runCatching {
        val root = JSONObject(payload)
        if (root.optInt("schema") != 1) return@runCatching null
        ImportedHouseholdPayload(
            preferences = root.getJSONObject("preferences").toUserPreferences(),
            plan = root.optJSONObject("plan")?.toWeekPlan()
        )
    }.getOrNull()

    fun reset() {
        values.edit().clear().apply()
    }

    private fun WeekPlan.toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("createdAt", createdAtEpochMillis)
        .put("weekStart", weekStart.toString())
        .put("storeId", store?.id ?: JSONObject.NULL)
        .put("budget", budget)
        .put("people", people)
        .put("estimatedSpend", estimatedSpend)
        .put("overBudget", overBudget)
        .put("mealsPerDay", mealsPerDay)
        .put("checkedItems", checkedItems.toStringJsonArray())
        .put("extraItems", extraItems.toStringJsonArray())
        .put("meals", JSONArray().apply {
            meals.forEach { meal ->
                put(
                    JSONObject()
                        .put("id", meal.id)
                        .put("recipeId", meal.recipeId)
                        .put("day", meal.day.name)
                        .put("slot", meal.slot.name)
                        .put("isCooked", meal.isCooked)
                        .put("omitted", meal.omitted)
                        .put("servings", meal.servings)
                )
            }
        })

    private fun JSONObject.toWeekPlan(): WeekPlan {
        val mealsJson = getJSONArray("meals")
        val meals = buildList {
            for (index in 0 until mealsJson.length()) {
                val meal = mealsJson.getJSONObject(index)
                add(
                    PlannedMeal(
                        id = meal.getString("id"),
                        recipeId = meal.getString("recipeId"),
                        day = Weekday.valueOf(meal.getString("day")),
                        slot = MealSlot.valueOf(meal.getString("slot")),
                        isCooked = meal.optBoolean("isCooked"),
                        omitted = meal.optBoolean("omitted"),
                        servings = meal.getInt("servings")
                    )
                )
            }
        }
        val storeId = optString("storeId").takeIf { it.isNotBlank() && it != "null" }
        return WeekPlan(
            id = getString("id"),
            createdAtEpochMillis = getLong("createdAt"),
            weekStart = LocalDate.parse(getString("weekStart")),
            store = SampleCatalog.stores.firstOrNull { it.id == storeId },
            budget = getDouble("budget"),
            people = getInt("people"),
            meals = meals,
            estimatedSpend = getDouble("estimatedSpend"),
            overBudget = optBoolean("overBudget"),
            checkedItems = optJSONArray("checkedItems").toStringSet(),
            extraItems = optJSONArray("extraItems").toStringList(),
            mealsPerDay = getInt("mealsPerDay")
        )
    }

    private fun UserPreferences.toJson(): JSONObject = JSONObject()
        .put("market", market.name)
        .put("storeId", store?.id ?: JSONObject.NULL)
        .put("budget", budget)
        .put("people", people)
        .put("priorities", priorities.map { it.name }.toStringJsonArray())
        .put("diets", diets.map { it.name }.toStringJsonArray())
        .put("appliances", appliances.map { it.name }.toStringJsonArray())
        .put("dislikes", dislikes.toStringJsonArray())
        .put("mealsPerDay", mealsPerDay)
        .put("cookingDays", cookingDays.map { it.name }.toStringJsonArray())
        .put("kidFriendly", kidFriendly)
        .put("allergyDisclaimer", allergyDisclaimerAccepted)
        .put("alreadyHave", alreadyHave.toStringJsonArray())
        .put("planAround", planAroundWhatIHave)
        .put("postalCode", postalCode)

    private fun JSONObject.toUserPreferences(): UserPreferences {
        val market = Market.entries.firstOrNull { it.name == optString("market") } ?: Market.SPAIN
        val storeId = optString("storeId").takeIf { it.isNotBlank() && it != "null" }
        return UserPreferences(
            market = market,
            store = SampleCatalog.stores.firstOrNull { it.id == storeId },
            budget = optDouble("budget", 65.0),
            people = optInt("people", 2).coerceIn(1, 12),
            priorities = optJSONArray("priorities").toEnumSet(Priority.entries),
            diets = optJSONArray("diets").toEnumSet(Diet.entries),
            appliances = optJSONArray("appliances").toEnumSet(Appliance.entries),
            dislikes = optJSONArray("dislikes").toStringSet(),
            mealsPerDay = optInt("mealsPerDay", 1).coerceIn(1, 3),
            cookingDays = optJSONArray("cookingDays").toEnumSet(Weekday.entries),
            kidFriendly = optBoolean("kidFriendly"),
            allergyDisclaimerAccepted = optBoolean("allergyDisclaimer"),
            alreadyHave = optJSONArray("alreadyHave").toStringSet(),
            planAroundWhatIHave = optBoolean("planAround"),
            postalCode = optString("postalCode")
        )
    }

    private fun Iterable<String>.toStringJsonArray(): JSONArray = JSONArray().apply { forEach(::put) }
    private fun Iterable<WeekPlan>.toPlanJsonArray(): JSONArray = JSONArray().apply { forEach { put(it.toJson()) } }

    private fun JSONArray?.toStringList(): List<String> = if (this == null) emptyList() else buildList {
        for (index in 0 until length()) add(getString(index))
    }

    private fun JSONArray?.toStringSet(): Set<String> = toStringList().toSet()

    private fun <T : Enum<T>> JSONArray?.toEnumSet(entries: List<T>): Set<T> {
        val names = toStringSet()
        return entries.filter { it.name in names }.toSet()
    }
}
