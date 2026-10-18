package com.cleanroom.weekplate.core.model

import java.time.LocalDate
import java.util.UUID

enum class Market(val label: String, val currency: String) {
    SPAIN("España", "€"),
    UNITED_KINGDOM("Reino Unido", "£"),
    UNITED_STATES("Estados Unidos", "$"),
    AUSTRALIA("Australia", "$"),
    CANADA("Canadá", "$")
}

data class GroceryStore(
    val id: String,
    val name: String,
    val market: Market
)

enum class Weekday(val shortLabel: String, val fullLabel: String) {
    MONDAY("L", "Lunes"),
    TUESDAY("M", "Martes"),
    WEDNESDAY("X", "Miércoles"),
    THURSDAY("J", "Jueves"),
    FRIDAY("V", "Viernes"),
    SATURDAY("S", "Sábado"),
    SUNDAY("D", "Domingo")
}

enum class Priority(val label: String) {
    QUICK_PREP("Preparación rápida"),
    HIGH_PROTEIN("Alta en proteína"),
    FAMILY_FAVOURITES("Para toda la familia"),
    HEALTHY("Saludable"),
    LOW_CARB("Baja en hidratos"),
    GUT_FRIENDLY("Cuida la digestión"),
    COMFORT_FOOD("Comida reconfortante"),
    PLANT_FORWARD("Más vegetales"),
    BATCH_COOK("Cocinar para varios días"),
    ANTI_INFLAMMATORY("Antiinflamatoria")
}

enum class Diet(val label: String) {
    VEGETARIAN("Vegetariana"),
    VEGAN("Vegana"),
    PESCATARIAN("Pescetariana"),
    GLUTEN_FREE("Sin gluten"),
    DAIRY_FREE("Sin lácteos"),
    NUT_FREE("Sin frutos secos"),
    NO_PORK("Sin cerdo"),
    HALAL("Halal"),
    KOSHER("Kosher"),
    LOW_FODMAP("Baja en FODMAP"),
    KETO("Keto"),
    NO_FISH("Sin pescado"),
    NO_SHELLFISH("Sin marisco"),
    NO_BEEF("Sin vacuno"),
    NO_LAMB("Sin cordero"),
    NO_SEED_OILS("Sin aceites de semillas")
}

enum class Appliance(val label: String) {
    OVEN("Horno"),
    HOB("Fogones"),
    AIR_FRYER("Freidora de aire"),
    MICROWAVE("Microondas"),
    RICE_COOKER("Arrocera")
}

enum class Aisle(val label: String) {
    FRUIT_VEG("Fruta y verdura"),
    MEAT_FISH("Carne y pescado"),
    CHILLED_DAIRY("Refrigerados y lácteos"),
    BAKERY("Panadería"),
    CUPBOARD("Despensa"),
    FROZEN("Congelados"),
    OTHER("Otros")
}

enum class RecipeTier { BUDGET, EVERYDAY, TREAT }
enum class Familiarity { ANCHOR, VARIETY, SPECIALIST }
enum class Spice { NONE, MILD, MEDIUM, HOT }
enum class MealSlot(val label: String) { BREAKFAST("Desayuno"), LUNCH("Comida"), DINNER("Cena") }

data class Nutrition(
    val calories: Int,
    val proteinGrams: Int,
    val carbsGrams: Int,
    val fatGrams: Int
)

data class Ingredient(
    val id: String,
    val name: String,
    val amount: Double,
    val unit: String,
    val aisle: Aisle,
    val estimatedUnitPrice: Double,
    val pantryStaple: Boolean = false
)

data class Recipe(
    val id: String,
    val name: String,
    val blurb: String,
    val serves: Int,
    val timeMinutes: Int,
    val nutrition: Nutrition,
    val cuisine: String,
    val tier: RecipeTier,
    val familiarity: Familiarity,
    val spice: Spice,
    val priorities: Set<Priority>,
    val diets: Set<Diet>,
    val requiredAppliances: Set<Appliance>,
    val ingredients: List<Ingredient>,
    val steps: List<String>,
    val familyFriendly: Boolean = false,
    val onePan: Boolean = false
) {
    fun estimatedCost(servings: Int): Double =
        ingredients.sumOf { it.estimatedUnitPrice * it.amount * servings / serves }
}

data class UserPreferences(
    val market: Market = Market.SPAIN,
    val store: GroceryStore? = null,
    val budget: Double = 65.0,
    val people: Int = 2,
    val priorities: Set<Priority> = setOf(Priority.QUICK_PREP, Priority.HEALTHY),
    val diets: Set<Diet> = emptySet(),
    val appliances: Set<Appliance> = setOf(Appliance.OVEN, Appliance.HOB),
    val dislikes: Set<String> = emptySet(),
    val mealsPerDay: Int = 1,
    val cookingDays: Set<Weekday> = Weekday.entries.take(5).toSet(),
    val kidFriendly: Boolean = false,
    val allergyDisclaimerAccepted: Boolean = false,
    val alreadyHave: Set<String> = emptySet(),
    val planAroundWhatIHave: Boolean = false,
    val postalCode: String = ""
)

data class PlannedMeal(
    val id: String = UUID.randomUUID().toString(),
    val recipeId: String,
    val day: Weekday,
    val slot: MealSlot,
    val isCooked: Boolean = false,
    val omitted: Boolean = false,
    val servings: Int
)

data class WeekPlan(
    val id: String = UUID.randomUUID().toString(),
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val weekStart: LocalDate,
    val store: GroceryStore?,
    val budget: Double,
    val people: Int,
    val meals: List<PlannedMeal>,
    val estimatedSpend: Double,
    val overBudget: Boolean,
    val checkedItems: Set<String> = emptySet(),
    val extraItems: List<String> = emptyList(),
    val mealsPerDay: Int
)

data class ShoppingItem(
    val id: String,
    val name: String,
    val amount: Double,
    val unit: String,
    val aisle: Aisle,
    val estimatedPrice: Double,
    val checked: Boolean,
    val custom: Boolean = false
)

enum class RootState { ONBOARDING, LOADING, GATE, MAIN }
enum class MainTab { PLAN, DISCOVER, FAVOURITES, PROFILE }
