package com.cleanroom.weekplate.core

import com.cleanroom.weekplate.core.engine.RecipeFilter
import com.cleanroom.weekplate.core.engine.RecipeSearch
import com.cleanroom.weekplate.core.model.Appliance
import com.cleanroom.weekplate.core.model.Diet
import com.cleanroom.weekplate.core.model.Familiarity
import com.cleanroom.weekplate.core.model.Nutrition
import com.cleanroom.weekplate.core.model.Priority
import com.cleanroom.weekplate.core.model.Recipe
import com.cleanroom.weekplate.core.model.RecipeTier
import com.cleanroom.weekplate.core.model.Spice
import org.junit.Assert.assertEquals
import org.junit.Test

class RecipeSearchTest {
    private val recipes = listOf(
        recipe("lentejas", "Guiso de lentejas", "Española", setOf(Priority.HEALTHY), setOf(Diet.VEGAN)),
        recipe("pasta", "Pasta rápida", "Italiana", setOf(Priority.QUICK_PREP), setOf(Diet.VEGETARIAN)),
        recipe("pollo", "Pollo al limón", "Mediterránea", setOf(Priority.HIGH_PROTEIN), emptySet())
    )

    @Test
    fun `query searches name cuisine priority and diet labels`() {
        assertEquals(listOf("pasta"), RecipeSearch.filter(recipes, RecipeFilter(query = "italiana")).map { it.id })
        assertEquals(listOf("lentejas"), RecipeSearch.filter(recipes, RecipeFilter(query = "vegana")).map { it.id })
    }

    @Test
    fun `structured filters are combined`() {
        val result = RecipeSearch.filter(
            recipes,
            RecipeFilter(priority = Priority.HEALTHY, diet = Diet.VEGAN, cuisine = "Española")
        )
        assertEquals(listOf("lentejas"), result.map { it.id })
    }

    @Test
    fun `blank filter preserves catalog order`() {
        assertEquals(recipes, RecipeSearch.filter(recipes, RecipeFilter()))
    }

    private fun recipe(
        id: String,
        name: String,
        cuisine: String,
        priorities: Set<Priority>,
        diets: Set<Diet>
    ) = Recipe(
        id = id,
        name = name,
        blurb = "",
        serves = 2,
        timeMinutes = 20,
        nutrition = Nutrition(400, 20, 40, 12),
        cuisine = cuisine,
        tier = RecipeTier.EVERYDAY,
        familiarity = Familiarity.ANCHOR,
        spice = Spice.NONE,
        priorities = priorities,
        diets = diets,
        requiredAppliances = setOf(Appliance.HOB),
        ingredients = emptyList(),
        steps = listOf("Cocinar")
    )
}
