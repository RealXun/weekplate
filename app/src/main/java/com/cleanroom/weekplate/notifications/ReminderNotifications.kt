package com.cleanroom.weekplate.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.cleanroom.weekplate.MainActivity
import com.cleanroom.weekplate.R
import com.cleanroom.weekplate.core.engine.ReminderSchedule
import com.cleanroom.weekplate.data.CookingSession
import com.cleanroom.weekplate.data.LocalPreferences
import com.cleanroom.weekplate.data.SampleCatalog
import java.time.ZonedDateTime

object ReminderNotifications {
    private const val CHANNEL_ID = "weekplate.reminders"
    private const val NOTIFICATION_ID = 4100

    fun createChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Recordatorios del menú",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Avisos locales para preparar el menú de la semana"
            }
        )
    }

    fun showWeeklyReminder(context: Context) {
        createChannel(context)
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                action = "com.cleanroom.weekplate.action.OPEN_PLAN"
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("Tu próxima semana está cerca")
            .setContentText("Revisa el menú y prepara la lista de la compra.")
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification) }
    }
}

object ReminderScheduler {
    private const val ACTION = "com.cleanroom.weekplate.action.WEEKLY_REMINDER"
    private const val REQUEST_BASE = 4200
    private const val WEEKS_AHEAD = 8

    @SuppressLint("ScheduleExactAlarm")
    fun schedule(context: Context, now: ZonedDateTime = ZonedDateTime.now()) {
        val appContext = context.applicationContext
        val alarms = appContext.getSystemService(AlarmManager::class.java)
        cancel(appContext)
        val first = ReminderSchedule.next(now)
        repeat(WEEKS_AHEAD) { index ->
            val operation = alarmIntent(appContext, index)
            val triggerAt = first.plusWeeks(index.toLong()).toInstant().toEpochMilli()
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()) {
                alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
            } else {
                alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
            }
        }
    }

    fun cancel(context: Context) {
        val appContext = context.applicationContext
        val alarms = appContext.getSystemService(AlarmManager::class.java)
        repeat(WEEKS_AHEAD) { index -> alarms.cancel(alarmIntent(appContext, index)) }
    }

    private fun alarmIntent(context: Context, index: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_BASE + index,
            Intent(context, WeeklyReminderReceiver::class.java).setAction(ACTION),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
}

class WeeklyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (LocalPreferences(context).reminderEnabled) {
            ReminderNotifications.showWeeklyReminder(context)
        }
    }
}

class ReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val local = LocalPreferences(context)
        if (local.reminderEnabled) {
            ReminderScheduler.schedule(context)
        }
        local.loadCookingSession()?.takeIf { it.running }?.let { session ->
            if (session.remainingAt() > 0) {
                CookTimerScheduler.schedule(context, session)
            } else {
                local.finishCookingTimer()
                CookTimerNotifications.showFinished(context, session)
            }
        }
    }
}

object CookTimerNotifications {
    const val ACTION_OPEN_COOK = "com.cleanroom.weekplate.action.OPEN_COOK"
    const val EXTRA_RECIPE_ID = "recipeId"
    const val EXTRA_STEP = "step"
    const val EXTRA_SERVINGS = "servings"
    private const val CHANNEL_ID = "weekplate.cook_timers"
    private const val NOTIFICATION_ID = 4300

    fun createChannel(context: Context) {
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Temporizadores de cocina",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Avisos locales cuando termina un temporizador de cocina" }
        )
    }

    fun showFinished(context: Context, session: CookingSession) {
        createChannel(context)
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val recipeName = SampleCatalog.recipes.firstOrNull { it.id == session.recipeId }?.name ?: "tu receta"
        val openCooking = PendingIntent.getActivity(
            context,
            1,
            Intent(context, MainActivity::class.java).apply {
                action = ACTION_OPEN_COOK
                putExtra(EXTRA_RECIPE_ID, session.recipeId)
                putExtra(EXTRA_STEP, session.step)
                putExtra(EXTRA_SERVINGS, session.servings)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("Temporizador terminado")
            .setContentText("Continúa con $recipeName.")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openCooking)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification) }
    }
}

object CookTimerScheduler {
    private const val ACTION_TIMER = "com.cleanroom.weekplate.action.COOK_TIMER_FINISHED"
    private const val REQUEST_CODE = 4300

    @SuppressLint("ScheduleExactAlarm")
    fun schedule(context: Context, session: CookingSession) {
        val appContext = context.applicationContext
        val alarms = appContext.getSystemService(AlarmManager::class.java)
        val operation = timerIntent(appContext)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, session.endsAtEpochMillis, operation)
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, session.endsAtEpochMillis, operation)
        }
    }

    fun cancel(context: Context) {
        val appContext = context.applicationContext
        appContext.getSystemService(AlarmManager::class.java).cancel(timerIntent(appContext))
    }

    private fun timerIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, CookTimerReceiver::class.java).setAction(ACTION_TIMER),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

class CookTimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val local = LocalPreferences(context)
        val session = local.loadCookingSession() ?: return
        if (!session.running) return
        local.finishCookingTimer()
        CookTimerNotifications.showFinished(context, session)
    }
}
