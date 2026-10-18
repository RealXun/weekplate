package com.cleanroom.weekplate.core.engine

import com.cleanroom.weekplate.core.model.Diet
import com.cleanroom.weekplate.core.model.Priority
import com.cleanroom.weekplate.core.model.Recipe

data class RecipeFilter(
    val query: String = "",
    val priority: Priority? = null,
    val diet: Diet? = null,
    val cuisine: String? = null
)

object RecipeSearch {
    fun filter(recipes: List<Recipe>, filter: RecipeFilter): List<Recipe> {
        val query = filter.query.trim()
        return recipes.filter { recipe ->
            val matchesQuery = query.isBlank() ||
                recipe.name.contains(query, ignoreCase = true) ||
                recipe.cuisine.contains(query, ignoreCase = true) ||
                recipe.priorities.any { it.label.contains(query, ignoreCase = true) } ||
                recipe.diets.any { it.label.contains(query, ignoreCase = true) }
            val matchesPriority = filter.priority?.let { it in recipe.priorities } ?: true
            val matchesDiet = filter.diet?.let { it in recipe.diets } ?: true
            val matchesCuisine = filter.cuisine?.let { recipe.cuisine.equals(it, ignoreCase = true) } ?: true
            matchesQuery && matchesPriority && matchesDiet && matchesCuisine
        }
    }
}
