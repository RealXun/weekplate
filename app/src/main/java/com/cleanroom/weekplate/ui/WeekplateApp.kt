package com.cleanroom.weekplate.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cleanroom.weekplate.*
import com.cleanroom.weekplate.core.model.MainTab
import com.cleanroom.weekplate.ui.screens.*

@Composable
fun WeekplateApp(viewModel: WeekplateViewModel) {
    val state = viewModel.uiState
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { outerPadding ->
        Box(Modifier.padding(outerPadding)) {
            when (val destination = state.destination) {
                Destination.Onboarding -> OnboardingScreen(viewModel)
                Destination.Main -> MainHome(viewModel)
                Destination.Shopping -> ShoppingScreen(viewModel)
                is Destination.RecipeDetails -> RecipeDetailScreen(viewModel, destination.recipeId)
                is Destination.Cooking -> CookingScreen(viewModel, destination.recipeId, destination.servings)
                Destination.Preferences -> PreferencesScreen(viewModel)
                Destination.History -> HistoryScreen(viewModel)
                Destination.Household -> HouseholdScreen(viewModel)
                Destination.About -> AboutScreen(viewModel)
            }
        }
    }
}

@Composable
private fun MainHome(viewModel: WeekplateViewModel) {
    val state = viewModel.uiState
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 700.dp
        if (wide) {
            Column {
                WideTabRow(state.selectedTab, viewModel::selectTab)
                Box(Modifier.weight(1f)) { MainTabContent(viewModel) }
            }
        } else {
            Scaffold(
                bottomBar = { CompactTabBar(state.selectedTab, viewModel::selectTab) }
            ) { padding ->
                Box(Modifier.padding(padding)) { MainTabContent(viewModel) }
            }
        }
    }
}

@Composable
private fun MainTabContent(viewModel: WeekplateViewModel) {
    when (viewModel.uiState.selectedTab) {
        MainTab.PLAN -> PlanScreen(viewModel)
        MainTab.DISCOVER -> DiscoverScreen(viewModel)
        MainTab.FAVOURITES -> FavouritesScreen(viewModel)
        MainTab.PROFILE -> ProfileScreen(viewModel)
    }
}

private data class TabUi(val tab: MainTab, val label: String, val glyph: String)
private val tabs = listOf(
    TabUi(MainTab.PLAN, "Menú", "M"),
    TabUi(MainTab.DISCOVER, "Descubrir", "D"),
    TabUi(MainTab.FAVOURITES, "Favoritos", "♥"),
    TabUi(MainTab.PROFILE, "Perfil", "P")
)

@Composable
private fun CompactTabBar(selected: MainTab, onSelect: (MainTab) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        tabs.forEach { item ->
            NavigationBarItem(
                selected = selected == item.tab,
                onClick = { onSelect(item.tab) },
                icon = { Text(item.glyph, style = MaterialTheme.typography.titleMedium) },
                label = { Text(item.label) }
            )
        }
    }
}

@Composable
private fun WideTabRow(selected: MainTab, onSelect: (MainTab) -> Unit) {
    Surface(shadowElevation = 2.dp) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            tabs.forEach { item ->
                Tab(
                    selected = selected == item.tab,
                    onClick = { onSelect(item.tab) },
                    text = { Text(item.label) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
