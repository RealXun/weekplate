package com.cleanroom.weekplate.core.engine

import com.cleanroom.weekplate.core.model.*
import java.time.LocalDate
import java.util.Random

/**
 * Clean-room planning heuristic. The reference application's exact ranking algorithm is unknown;
 * this implementation is deterministic, testable, and intentionally contains no recovered code.
 */
class PlanEngine {
    fun generate(
        preferences: UserPreferences,
        recipes: List<Recipe>,
        weekStart: LocalDate,
        seed: Long = weekStart.toEpochDay()
    ): WeekPlan {
        require(preferences.cookingDays.isNotEmpty()) { "At least one cooking day is required" }
        require(preferences.mealsPerDay in 1..3) { "Meals per day must be between 1 and 3" }

        val eligible = recipes.filter { it.matches(preferences) }
        require(eligible.isNotEmpty()) { "No recipe matches the selected constraints" }

        val random = Random(seed)
        val ranked = eligible
            .map { recipe ->
                val preferenceScore = recipe.priorities.count(preferences.priorities::contains) * 100
                val budgetScore = when (recipe.tier) {
                    RecipeTier.BUDGET -> 24
                    RecipeTier.EVERYDAY -> 12
                    RecipeTier.TREAT -> 0
                }
                val varietyNoise = random.nextInt(20)
                recipe to (preferenceScore + budgetScore + varietyNoise)
            }
            .sortedByDescending { it.second }
            .map { it.first }

        val slots = when (preferences.mealsPerDay) {
            1 -> listOf(MealSlot.DINNER)
            2 -> listOf(MealSlot.LUNCH, MealSlot.DINNER)
            else -> MealSlot.entries
        }
        val requested = preferences.cookingDays
            .sortedBy { it.ordinal }
            .flatMap { day -> slots.map { day to it } }

        var runningCost = 0.0
        val meals = requested.mapIndexed { index, (day, slot) ->
            val candidates = ranked.indices.map { ranked[(index + it) % ranked.size] }
            val recipe = candidates.firstOrNull { candidate ->
                runningCost + candidate.estimatedCost(preferences.people) <= preferences.budget
            } ?: candidates.minBy { it.estimatedCost(preferences.people) }
            runningCost += recipe.estimatedCost(preferences.people)
            PlannedMeal(
                recipeId = recipe.id,
                day = day,
                slot = slot,
                servings = preferences.people
            )
        }

        return WeekPlan(
            weekStart = weekStart,
            store = preferences.store,
            budget = preferences.budget,
            people = preferences.people,
            meals = meals,
            estimatedSpend = runningCost,
            overBudget = runningCost > preferences.budget,
            mealsPerDay = preferences.mealsPerDay
        )
    }

    fun swapMeal(
        plan: WeekPlan,
        mealId: String,
        recipes: List<Recipe>,
        preferences: UserPreferences
    ): WeekPlan {
        val current = plan.meals.firstOrNull { it.id == mealId } ?: return plan
        val currentRecipe = recipes.firstOrNull { it.id == current.recipeId }
        val replacement = recipes
            .asSequence()
            .filter { it.id != current.recipeId }
            .filter { it.matches(preferences) }
            .minByOrNull { it.estimatedCost(current.servings) }
            ?: return plan

        val revisedMeals = plan.meals.map {
            if (it.id == mealId) it.copy(recipeId = replacement.id, isCooked = false) else it
        }
        val revisedSpend = plan.estimatedSpend -
            (currentRecipe?.estimatedCost(current.servings) ?: 0.0) +
            replacement.estimatedCost(current.servings)
        return plan.copy(
            meals = revisedMeals,
            estimatedSpend = revisedSpend,
            overBudget = revisedSpend > plan.budget
        )
    }

    private fun Recipe.matches(preferences: UserPreferences): Boolean =
        requiredAppliances.all(preferences.appliances::contains) &&
            (!preferences.kidFriendly || familyFriendly) &&
            preferences.diets.all { matchesDiet(it) } &&
            preferences.dislikes.none { dislike -> containsTerm(dislike) }

    /**
     * Positive diets require an explicit recipe tag. Exclusion-style restrictions can be
     * proven against this app's own, human-reviewed Spanish ingredient list. A recipe is never
     * admitted by relaxing a selected restriction.
     */
    private fun Recipe.matchesDiet(diet: Diet): Boolean {
        if (diet in diets) return true
        val forbiddenTerms = when (diet) {
            Diet.NO_PORK -> listOf("cerdo", "jamón", "bacon", "panceta", "chorizo")
            Diet.NO_FISH -> listOf("pescado", "salmón", "atún", "bacalao", "merluza", "anchoa")
            Diet.NO_SHELLFISH -> listOf("marisco", "gamba", "langostino", "camarón", "mejillón", "almeja")
            Diet.NO_BEEF -> listOf("vacuno", "ternera", "buey")
            Diet.NO_LAMB -> listOf("cordero")
            Diet.DAIRY_FREE -> listOf("leche", "queso", "yogur", "mantequilla", "nata")
            Diet.GLUTEN_FREE -> listOf("pasta", "pan", "cuscús", "harina", "orzo", "trigo")
            Diet.NUT_FREE -> listOf("nuez", "almendra", "avellana", "cacahuete", "pistacho", "anacardo")
            Diet.NO_SEED_OILS -> listOf("aceite de girasol", "canola", "colza", "aceite de semillas")
            else -> return false
        }
        return forbiddenTerms.none { containsTerm(it) }
    }

    private fun Recipe.containsTerm(term: String): Boolean =
        name.contains(term, ignoreCase = true) ||
            ingredients.any { it.name.contains(term, ignoreCase = true) }
}
