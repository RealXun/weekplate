package com.cleanroom.weekplate.core.engine

import com.cleanroom.weekplate.core.model.*

class ShoppingListAggregator {
    fun aggregate(
        plan: WeekPlan,
        recipes: List<Recipe>,
        alreadyHave: Set<String> = emptySet(),
        checkedIds: Set<String> = plan.checkedItems
    ): List<ShoppingItem> = aggregate(listOf(plan), recipes, alreadyHave, checkedIds)

    fun aggregate(
        plans: List<WeekPlan>,
        recipes: List<Recipe>,
        alreadyHave: Set<String> = emptySet(),
        checkedIds: Set<String> = plans.flatMapTo(mutableSetOf()) { it.checkedItems }
    ): List<ShoppingItem> {
        data class Accumulator(
            val id: String,
            val name: String,
            val unit: String,
            val aisle: Aisle,
            var amount: Double = 0.0,
            var price: Double = 0.0
        )

        val recipeById = recipes.associateBy(Recipe::id)
        val grouped = linkedMapOf<String, Accumulator>()

        plans.flatMap { it.meals }.filterNot { it.omitted }.forEach { meal ->
            val recipe = recipeById[meal.recipeId] ?: return@forEach
            val scale = meal.servings.toDouble() / recipe.serves
            recipe.ingredients
                .filterNot(Ingredient::pantryStaple)
                .filterNot { ingredient -> alreadyHave.any { it.equals(ingredient.id, true) || it.equals(ingredient.name, true) } }
                .forEach { ingredient ->
                    val key = "${ingredient.id.lowercase()}|${ingredient.unit.lowercase()}"
                    val acc = grouped.getOrPut(key) {
                        Accumulator(ingredient.id, ingredient.name, ingredient.unit, ingredient.aisle)
                    }
                    acc.amount += ingredient.amount * scale
                    acc.price += ingredient.estimatedUnitPrice * ingredient.amount * scale
                }
        }

        val generated = grouped.values.map {
            ShoppingItem(
                id = it.id,
                name = it.name,
                amount = it.amount,
                unit = it.unit,
                aisle = it.aisle,
                estimatedPrice = it.price,
                checked = it.id in checkedIds
            )
        }
        val extras = plans.flatMap { plan ->
            plan.extraItems.mapIndexed { index, name ->
                val id = "extra-${plan.weekStart}-$index-${name.lowercase()}"
                ShoppingItem(id, name, 1.0, "ud", Aisle.OTHER, 0.0, id in checkedIds, custom = true)
            }
        }
        return (generated + extras).sortedWith(compareBy<ShoppingItem> { it.aisle.ordinal }.thenBy { it.name })
    }
}
