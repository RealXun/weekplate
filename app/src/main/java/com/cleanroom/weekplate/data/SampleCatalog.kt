package com.cleanroom.weekplate.data

import com.cleanroom.weekplate.core.model.*

/** Original clean-room sample content. No recipe text or image is copied from the reference APK. */
object SampleCatalog {
    val stores = listOf(
        // Keys intentionally match grocery-cli's public store registry so a
        // generated static catalog can be joined without a private backend.
        GroceryStore("mercadona", "Mercadona", Market.SPAIN),
        GroceryStore("dia", "DIA", Market.SPAIN),
        GroceryStore("lidl-es", "Lidl España", Market.SPAIN),
        GroceryStore("alcampo", "Alcampo", Market.SPAIN),
        GroceryStore("consum", "Consum", Market.SPAIN),
        GroceryStore("eroski", "Eroski", Market.SPAIN),
        GroceryStore("bonpreu", "Bonpreu i Esclat", Market.SPAIN),
        GroceryStore("ametller", "Ametller Origen", Market.SPAIN),
        GroceryStore("gadis", "Gadis", Market.SPAIN),
        GroceryStore("aldi", "ALDI España", Market.SPAIN),
        GroceryStore("makro", "Makro España", Market.SPAIN),
        GroceryStore("masymas", "masymas", Market.SPAIN),
        // Kept for backwards compatibility with clean-room builds already
        // installed. These entries continue to use local estimated prices.
        GroceryStore("mercado-local", "Mercado local", Market.SPAIN),
        GroceryStore("supermercado", "Supermercado", Market.SPAIN),
        GroceryStore("cooperativa", "Cooperativa", Market.SPAIN),
        GroceryStore("online", "Compra online", Market.SPAIN)
    )

    private fun i(
        id: String,
        name: String,
        amount: Double,
        unit: String,
        aisle: Aisle,
        price: Double,
        pantry: Boolean = false
    ) = Ingredient(id, name, amount, unit, aisle, price, pantry)

    val recipes = listOf(
        Recipe(
            id = "lemon-chickpeas", name = "Garbanzos al limón con espinacas",
            blurb = "Una sartén, verduras tiernas y una salsa cítrica suave.", serves = 2, timeMinutes = 22,
            nutrition = Nutrition(485, 23, 58, 16), cuisine = "Mediterránea", tier = RecipeTier.BUDGET,
            familiarity = Familiarity.ANCHOR, spice = Spice.MILD,
            priorities = setOf(Priority.QUICK_PREP, Priority.HEALTHY, Priority.PLANT_FORWARD),
            diets = setOf(Diet.VEGETARIAN, Diet.VEGAN, Diet.DAIRY_FREE, Diet.NO_FISH, Diet.NO_SHELLFISH),
            requiredAppliances = setOf(Appliance.HOB), familyFriendly = true, onePan = true,
            ingredients = listOf(
                i("chickpeas", "Garbanzos cocidos", 400.0, "g", Aisle.CUPBOARD, .003),
                i("spinach", "Espinacas", 160.0, "g", Aisle.FRUIT_VEG, .008),
                i("lemon", "Limón", 1.0, "ud", Aisle.FRUIT_VEG, .55),
                i("garlic", "Ajo", 2.0, "dientes", Aisle.FRUIT_VEG, .12),
                i("olive-oil", "Aceite de oliva", 1.0, "cda", Aisle.CUPBOARD, .18, true)
            ),
            steps = listOf(
                "Escurre y enjuaga los garbanzos. Pica el ajo y lava las espinacas.",
                "Calienta el aceite y cocina el ajo durante un minuto sin que se dore.",
                "Añade garbanzos y espinacas; cocina hasta que la hoja se ablande.",
                "Ralla medio limón, exprímelo sobre la sartén y ajusta de sal."
            )
        ),
        Recipe(
            id = "paprika-chicken", name = "Pollo al pimentón con patata",
            blurb = "Una bandeja de horno con bordes dorados y salsa de yogur.", serves = 2, timeMinutes = 42,
            nutrition = Nutrition(610, 48, 53, 22), cuisine = "Casera", tier = RecipeTier.EVERYDAY,
            familiarity = Familiarity.ANCHOR, spice = Spice.MILD,
            priorities = setOf(Priority.HIGH_PROTEIN, Priority.FAMILY_FAVOURITES, Priority.QUICK_PREP),
            diets = setOf(Diet.GLUTEN_FREE, Diet.NUT_FREE, Diet.NO_FISH, Diet.NO_SHELLFISH),
            requiredAppliances = setOf(Appliance.OVEN), familyFriendly = true, onePan = true,
            ingredients = listOf(
                i("chicken", "Contramuslos de pollo", 360.0, "g", Aisle.MEAT_FISH, .012),
                i("potato", "Patatas", 450.0, "g", Aisle.FRUIT_VEG, .002),
                i("pepper", "Pimiento rojo", 1.0, "ud", Aisle.FRUIT_VEG, .85),
                i("yogurt", "Yogur natural", 125.0, "g", Aisle.CHILLED_DAIRY, .004),
                i("paprika", "Pimentón", 1.0, "cdta", Aisle.CUPBOARD, .08, true)
            ),
            steps = listOf(
                "Calienta el horno a 210 °C. Corta la patata y el pimiento en piezas pequeñas.",
                "Mezcla pollo y verduras con pimentón, aceite y sal sobre una bandeja.",
                "Hornea 30–35 minutos, girando el pollo a mitad de cocción.",
                "Sirve con el yogur batido y una pizca adicional de pimentón."
            )
        ),
        Recipe(
            id = "salmon-couscous", name = "Salmón con cuscús verde",
            blurb = "Cuscús con hierbas, guisantes y salmón jugoso.", serves = 2, timeMinutes = 25,
            nutrition = Nutrition(640, 42, 61, 25), cuisine = "Europea", tier = RecipeTier.TREAT,
            familiarity = Familiarity.VARIETY, spice = Spice.NONE,
            priorities = setOf(Priority.HIGH_PROTEIN, Priority.HEALTHY, Priority.QUICK_PREP),
            diets = setOf(Diet.PESCATARIAN, Diet.NUT_FREE, Diet.NO_SHELLFISH),
            requiredAppliances = setOf(Appliance.HOB),
            ingredients = listOf(
                i("salmon", "Lomos de salmón", 280.0, "g", Aisle.MEAT_FISH, .023),
                i("couscous", "Cuscús", 160.0, "g", Aisle.CUPBOARD, .004),
                i("peas", "Guisantes", 140.0, "g", Aisle.FROZEN, .004),
                i("parsley", "Perejil", 15.0, "g", Aisle.FRUIT_VEG, .025),
                i("lemon", "Limón", 1.0, "ud", Aisle.FRUIT_VEG, .55)
            ),
            steps = listOf(
                "Cubre el cuscús con agua hirviendo y déjalo reposar tapado.",
                "Cocina los guisantes y mézclalos con cuscús, perejil y limón.",
                "Marca el salmón en una sartén, primero por el lado de la piel.",
                "Reparte el cuscús y coloca el salmón encima."
            )
        ),
        Recipe(
            id = "mushroom-orzo", name = "Orzo cremoso de setas",
            blurb = "Pasta pequeña cocinada como un risotto, sin complicaciones.", serves = 2, timeMinutes = 28,
            nutrition = Nutrition(560, 21, 76, 18), cuisine = "Italiana", tier = RecipeTier.EVERYDAY,
            familiarity = Familiarity.VARIETY, spice = Spice.NONE,
            priorities = setOf(Priority.COMFORT_FOOD, Priority.QUICK_PREP, Priority.PLANT_FORWARD),
            diets = setOf(Diet.VEGETARIAN, Diet.NUT_FREE, Diet.NO_FISH, Diet.NO_SHELLFISH),
            requiredAppliances = setOf(Appliance.HOB), onePan = true,
            ingredients = listOf(
                i("orzo", "Pasta orzo", 180.0, "g", Aisle.CUPBOARD, .005),
                i("mushrooms", "Setas", 250.0, "g", Aisle.FRUIT_VEG, .009),
                i("stock", "Caldo de verduras", 500.0, "ml", Aisle.CUPBOARD, .002),
                i("parmesan", "Queso curado", 45.0, "g", Aisle.CHILLED_DAIRY, .018),
                i("onion", "Cebolla", 1.0, "ud", Aisle.FRUIT_VEG, .35)
            ),
            steps = listOf(
                "Corta las setas y la cebolla. Dora ambas en una cazuela amplia.",
                "Añade el orzo y remueve durante un minuto.",
                "Incorpora el caldo poco a poco y cocina hasta que la pasta esté tierna.",
                "Apaga el fuego, añade el queso y deja reposar dos minutos."
            )
        ),
        Recipe(
            id = "tofu-rice", name = "Arroz crujiente con tofu y brócoli",
            blurb = "Contraste de tofu dorado, arroz y verduras con lima.", serves = 2, timeMinutes = 32,
            nutrition = Nutrition(525, 27, 70, 16), cuisine = "Asiática", tier = RecipeTier.BUDGET,
            familiarity = Familiarity.VARIETY, spice = Spice.MEDIUM,
            priorities = setOf(Priority.PLANT_FORWARD, Priority.HIGH_PROTEIN, Priority.HEALTHY),
            diets = setOf(Diet.VEGETARIAN, Diet.VEGAN, Diet.DAIRY_FREE, Diet.NO_FISH, Diet.NO_SHELLFISH),
            requiredAppliances = setOf(Appliance.HOB),
            ingredients = listOf(
                i("tofu", "Tofu firme", 300.0, "g", Aisle.CHILLED_DAIRY, .009),
                i("rice", "Arroz", 160.0, "g", Aisle.CUPBOARD, .003),
                i("broccoli", "Brócoli", 300.0, "g", Aisle.FRUIT_VEG, .006),
                i("lime", "Lima", 1.0, "ud", Aisle.FRUIT_VEG, .55),
                i("soy-sauce", "Salsa de soja", 2.0, "cda", Aisle.CUPBOARD, .12)
            ),
            steps = listOf(
                "Cuece el arroz hasta que quede suelto.",
                "Seca el tofu, córtalo y dóralo en una sartén caliente.",
                "Añade el brócoli con un chorrito de agua y tapa cinco minutos.",
                "Incorpora arroz y soja, deja que se tueste y termina con lima."
            )
        ),
        Recipe(
            id = "turkey-pitas", name = "Pitas de pavo y pepino",
            blurb = "Cena fresca, rápida y fácil de montar en la mesa.", serves = 2, timeMinutes = 18,
            nutrition = Nutrition(510, 39, 55, 15), cuisine = "Mediterránea", tier = RecipeTier.BUDGET,
            familiarity = Familiarity.ANCHOR, spice = Spice.MILD,
            priorities = setOf(Priority.QUICK_PREP, Priority.HIGH_PROTEIN, Priority.FAMILY_FAVOURITES),
            diets = setOf(Diet.NUT_FREE, Diet.NO_FISH, Diet.NO_SHELLFISH),
            requiredAppliances = setOf(Appliance.HOB), familyFriendly = true,
            ingredients = listOf(
                i("turkey", "Carne picada de pavo", 300.0, "g", Aisle.MEAT_FISH, .011),
                i("pita", "Pan de pita", 4.0, "ud", Aisle.BAKERY, .40),
                i("cucumber", "Pepino", 1.0, "ud", Aisle.FRUIT_VEG, .70),
                i("yogurt", "Yogur natural", 125.0, "g", Aisle.CHILLED_DAIRY, .004),
                i("tomato", "Tomate", 2.0, "ud", Aisle.FRUIT_VEG, .45)
            ),
            steps = listOf(
                "Dora el pavo desmenuzándolo con una cuchara.",
                "Corta pepino y tomate; mezcla el pepino con el yogur.",
                "Calienta las pitas brevemente.",
                "Rellena cada pita con pavo, verduras y salsa de pepino."
            )
        ),
        Recipe(
            id = "lentil-soup", name = "Sopa roja de lentejas y zanahoria",
            blurb = "Una sopa suave, económica y reconfortante para varios días.", serves = 4, timeMinutes = 38,
            nutrition = Nutrition(390, 22, 57, 9), cuisine = "Casera", tier = RecipeTier.BUDGET,
            familiarity = Familiarity.ANCHOR, spice = Spice.MILD,
            priorities = setOf(Priority.BATCH_COOK, Priority.HEALTHY, Priority.PLANT_FORWARD),
            diets = setOf(Diet.VEGETARIAN, Diet.VEGAN, Diet.GLUTEN_FREE, Diet.DAIRY_FREE, Diet.NUT_FREE),
            requiredAppliances = setOf(Appliance.HOB), familyFriendly = true, onePan = true,
            ingredients = listOf(
                i("red-lentils", "Lentejas rojas", 280.0, "g", Aisle.CUPBOARD, .004),
                i("carrot", "Zanahorias", 3.0, "ud", Aisle.FRUIT_VEG, .22),
                i("tomato-tin", "Tomate triturado", 400.0, "g", Aisle.CUPBOARD, .003),
                i("stock", "Caldo de verduras", 900.0, "ml", Aisle.CUPBOARD, .002),
                i("onion", "Cebolla", 1.0, "ud", Aisle.FRUIT_VEG, .35)
            ),
            steps = listOf(
                "Pica cebolla y zanahoria y cocínalas hasta que se ablanden.",
                "Añade lentejas, tomate y caldo; lleva a ebullición.",
                "Cocina a fuego suave 22 minutos, removiendo alguna vez.",
                "Tritura parcialmente para conservar algo de textura."
            )
        ),
        Recipe(
            id = "egg-tray", name = "Huevos al horno con tomate y judías",
            blurb = "Una fuente compartida con salsa de tomate especiada.", serves = 2, timeMinutes = 30,
            nutrition = Nutrition(470, 29, 44, 20), cuisine = "Norteafricana", tier = RecipeTier.BUDGET,
            familiarity = Familiarity.VARIETY, spice = Spice.MEDIUM,
            priorities = setOf(Priority.HIGH_PROTEIN, Priority.COMFORT_FOOD, Priority.QUICK_PREP),
            diets = setOf(Diet.VEGETARIAN, Diet.GLUTEN_FREE, Diet.NUT_FREE),
            requiredAppliances = setOf(Appliance.OVEN), onePan = true,
            ingredients = listOf(
                i("eggs", "Huevos", 4.0, "ud", Aisle.CHILLED_DAIRY, .30),
                i("white-beans", "Judías blancas", 300.0, "g", Aisle.CUPBOARD, .003),
                i("tomato-tin", "Tomate triturado", 400.0, "g", Aisle.CUPBOARD, .003),
                i("pepper", "Pimiento rojo", 1.0, "ud", Aisle.FRUIT_VEG, .85)
            ),
            steps = listOf(
                "Calienta el horno a 200 °C y corta el pimiento.",
                "Mezcla tomate, judías y pimiento en una fuente; hornea diez minutos.",
                "Haz cuatro huecos, casca los huevos y vuelve a hornear.",
                "Retira cuando las claras estén cuajadas y sirve enseguida."
            )
        )
    )
}
