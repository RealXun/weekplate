package com.cleanroom.weekplate

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cleanroom.weekplate.data.LocalPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@RunWith(AndroidJUnit4::class)
class OnboardingSmokeTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Before
    fun resetLocalState() {
        compose.activity.runOnUiThread { compose.activity.resetForTests() }
        compose.waitForIdle()
    }

    @Test
    fun firstStepSelectsStoreAndAdvancesToBudget() {
        compose.onNodeWithText("¿Dónde haces la compra?").assertIsDisplayed()
        compose.onNodeWithText("Mercado local").performClick()
        compose.onNodeWithText("Continuar").performClick()
        compose.onNodeWithText("Marca tu presupuesto").assertIsDisplayed()
    }

    @Test
    fun completeOnboardingCreatesAWeeklyPlan() {
        completeOnboarding()
        compose.onNodeWithText("Tu menú").assertIsDisplayed()
        compose.onNodeWithText("Ver lista").assertIsDisplayed()
    }

    @Test
    fun weeklyPlanSurvivesActivityRecreation() {
        completeOnboarding()
        val local = LocalPreferences(compose.activity)
        val storedId = local.loadCurrentPlan()?.id
        assertNotNull(storedId)
        val exported = local.exportHouseholdPayload(local.loadUserPreferences())
        assertEquals(storedId, local.importHouseholdPayload(exported)?.plan?.id)

        compose.activityRule.scenario.recreate()
        compose.waitForIdle()

        assertEquals(storedId, LocalPreferences(compose.activity).loadCurrentPlan()?.id)
        compose.onNodeWithText("Tu menú").assertIsDisplayed()
    }

    @Test
    fun householdClientRequiresAnOperatorServerBeforeConnecting() {
        completeOnboarding()
        compose.onNodeWithText("Perfil").performClick()
        compose.onNodeWithText("Hogar compartido").performClick()

        compose.onNodeWithText("URL de tu servidor").assertIsDisplayed()
        compose.onNodeWithText("Crear hogar").assertIsDisplayed()
    }

    @Test
    fun nextWeekCanBeGeneratedAndReturnedToCurrentWeek() {
        completeOnboarding()
        val nextMonday = LocalDate.now()
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .plusWeeks(1)

        compose.onNodeWithContentDescription("Semana siguiente").performClick()
        compose.waitForIdle()

        compose.onNodeWithText("Volver a esta semana").assertIsDisplayed()
        assertNotNull(LocalPreferences(compose.activity).loadPlanForWeek(nextMonday))

        compose.onNodeWithText("Volver a esta semana").performClick()
        compose.onNodeWithText("SEMANA ACTUAL").assertIsDisplayed()
    }

    @Test
    fun shoppingListCanCombineTheFollowingWeek() {
        completeOnboarding()
        compose.onNodeWithText("Ver lista").performClick()
        compose.onNodeWithContentDescription("Combinar semana siguiente").performClick()
        compose.waitForIdle()

        compose.onNodeWithText("Lista · 2 semanas").assertIsDisplayed()
        compose.onNodeWithText("Suma esta semana y la siguiente").assertIsDisplayed()
    }

    private fun completeOnboarding() {
        compose.onNodeWithText("Mercado local").performClick()
        repeat(7) { compose.onNodeWithText("Continuar").performClick() }
        compose.onNodeWithText("Crear mi menú").performClick()
    }
}
