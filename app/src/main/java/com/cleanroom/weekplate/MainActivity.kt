package com.cleanroom.weekplate

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.cleanroom.weekplate.notifications.CookTimerNotifications
import com.cleanroom.weekplate.notifications.ReminderNotifications
import com.cleanroom.weekplate.notifications.ReminderScheduler
import com.cleanroom.weekplate.data.catalog.CatalogSyncScheduler
import com.cleanroom.weekplate.ui.WeekplateApp
import com.cleanroom.weekplate.ui.theme.WeekplateTheme

class MainActivity : ComponentActivity() {
    private val viewModel: WeekplateViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ReminderNotifications.createChannel(this)
        CookTimerNotifications.createChannel(this)
        if (viewModel.uiState.reminderEnabled) ReminderScheduler.schedule(this)
        CatalogSyncScheduler.scheduleIfConfigured(this)
        handleLaunchIntent(intent)
        enableEdgeToEdge()
        setContent {
            WeekplateTheme {
                BackHandler(enabled = viewModel.uiState.destination !is Destination.Main && viewModel.uiState.destination !is Destination.Onboarding) {
                    viewModel.back()
                }
                WeekplateApp(viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleLaunchIntent(intent)
    }

    private fun handleLaunchIntent(intent: Intent?) {
        if (intent?.action != CookTimerNotifications.ACTION_OPEN_COOK) return
        val recipeId = intent.getStringExtra(CookTimerNotifications.EXTRA_RECIPE_ID) ?: return
        val servings = intent.getIntExtra(CookTimerNotifications.EXTRA_SERVINGS, 2).coerceIn(1, 12)
        if (viewModel.recipe(recipeId) != null && viewModel.uiState.rootState == com.cleanroom.weekplate.core.model.RootState.MAIN) {
            viewModel.navigate(Destination.Cooking(recipeId, servings))
        }
    }

    /** Deterministic hook used only by the instrumented clean-room smoke tests. */
    fun resetForTests() {
        check(BuildConfig.DEBUG)
        viewModel.resetAll()
    }
}
