package com.cleanroom.weekplate.core

import com.cleanroom.weekplate.core.engine.PlanEngine
import com.cleanroom.weekplate.core.engine.ReminderSchedule
import com.cleanroom.weekplate.core.engine.ShoppingListAggregator
import com.cleanroom.weekplate.core.model.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class PlanEngineTest {
    private val tomato = Ingredient("tomato", "Tomate", 2.0, "ud", Aisle.FRUIT_VEG, 0.45)
    private val rice = Ingredient("rice", "Arroz", 160.0, "g", Aisle.CUPBOARD, 0.002)
    private val recipes = listOf(
        Recipe(
            id = "rice-bowl", name = "Bol de arroz", blurb = "", serves = 2, timeMinutes = 20,
            nutrition = Nutrition(420, 18, 62, 10), cuisine = "Mediterránea",
            tier = RecipeTier.BUDGET, familiarity = Familiarity.ANCHOR, spice = Spice.NONE,
            priorities = setOf(Priority.QUICK_PREP, Priority.HEALTHY), diets = setOf(Diet.VEGETARIAN),
            requiredAppliances = setOf(Appliance.HOB), ingredients = listOf(tomato, rice), steps = listOf("Cocinar")
        ),
        Recipe(
            id = "slow-stew", name = "Estofado", blurb = "", serves = 2, timeMinutes = 80,
            nutrition = Nutrition(550, 35, 40, 22), cuisine = "Casera",
            tier = RecipeTier.TREAT, familiarity = Familiarity.VARIETY, spice = Spice.MILD,
            priorities = setOf(Priority.COMFORT_FOOD), diets = emptySet(),
            requiredAppliances = setOf(Appliance.OVEN), ingredients = listOf(tomato), steps = listOf("Hornear")
        )
    )

    @Test fun `generator is deterministic and honours hard filters`() {
        val preferences = UserPreferences(
            budget = 25.0,
            people = 2,
            diets = setOf(Diet.VEGETARIAN),
            appliances = setOf(Appliance.HOB),
            cookingDays = setOf(Weekday.MONDAY, Weekday.WEDNESDAY)
        )
        val first = PlanEngine().generate(preferences, recipes, LocalDate.of(2026, 10, 19), 7)
        val second = PlanEngine().generate(preferences, recipes, LocalDate.of(2026, 10, 19), 7)
        assertEquals(listOf("rice-bowl", "rice-bowl"), first.meals.map { it.recipeId })
        assertEquals(first.meals.map { it.recipeId }, second.meals.map { it.recipeId })
        assertFalse(first.overBudget)
    }

    @Test fun `shopping list scales combines and excludes pantry or owned items`() {
        val plan = WeekPlan(
            weekStart = LocalDate.of(2026, 10, 19), store = null, budget = 40.0, people = 4,
            meals = listOf(
                PlannedMeal(recipeId = "rice-bowl", day = Weekday.MONDAY, slot = MealSlot.DINNER, servings = 4),
                PlannedMeal(recipeId = "rice-bowl", day = Weekday.TUESDAY, slot = MealSlot.DINNER, servings = 2)
            ), estimatedSpend = 0.0, overBudget = false, mealsPerDay = 1
        )
        val items = ShoppingListAggregator().aggregate(plan, recipes, alreadyHave = setOf("rice"))
        assertEquals(1, items.size)
        assertEquals("tomato", items.single().id)
        assertEquals(6.0, items.single().amount, 0.001)
    }

    @Test fun `shopping list combines ingredients from consecutive plans`() {
        fun plan(start: LocalDate, servings: Int) = WeekPlan(
            weekStart = start, store = null, budget = 40.0, people = servings,
            meals = listOf(
                PlannedMeal(recipeId = "rice-bowl", day = Weekday.MONDAY, slot = MealSlot.DINNER, servings = servings)
            ),
            estimatedSpend = 0.0, overBudget = false, mealsPerDay = 1
        )

        val items = ShoppingListAggregator().aggregate(
            listOf(plan(LocalDate.of(2026, 10, 19), 2), plan(LocalDate.of(2026, 10, 26), 4)),
            recipes
        )

        assertEquals(6.0, items.first { it.id == "tomato" }.amount, 0.001)
        assertEquals(480.0, items.first { it.id == "rice" }.amount, 0.001)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `empty cooking days are rejected`() {
        PlanEngine().generate(UserPreferences(cookingDays = emptySet()), recipes, LocalDate.now())
    }

    @Test fun `an exclusion diet is enforced without requiring a copied recipe taxonomy`() {
        val pork = recipes.last().copy(
            id = "pork-stew",
            name = "Estofado de cerdo",
            ingredients = listOf(tomato.copy(id = "pork", name = "Cerdo"))
        )
        val preferences = UserPreferences(
            diets = setOf(Diet.NO_PORK),
            appliances = setOf(Appliance.HOB, Appliance.OVEN),
            cookingDays = setOf(Weekday.MONDAY)
        )

        val plan = PlanEngine().generate(preferences, recipes + pork, LocalDate.of(2026, 10, 19), 3)

        assertFalse(plan.meals.any { it.recipeId == pork.id })
    }

    @Test fun `weekly reminder uses the next Sunday at ten`() {
        val madrid = ZoneId.of("Europe/Madrid")
        val before = ZonedDateTime.of(2026, 10, 18, 9, 0, 0, 0, madrid)
        val atBoundary = ZonedDateTime.of(2026, 10, 18, 10, 0, 0, 0, madrid)

        assertEquals(ZonedDateTime.of(2026, 10, 18, 10, 0, 0, 0, madrid), ReminderSchedule.next(before))
        assertEquals(ZonedDateTime.of(2026, 10, 25, 10, 0, 0, 0, madrid), ReminderSchedule.next(atBoundary))
    }
}
