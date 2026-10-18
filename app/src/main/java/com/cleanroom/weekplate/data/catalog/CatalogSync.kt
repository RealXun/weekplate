package com.cleanroom.weekplate.data.catalog

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.io.IOException
import java.util.concurrent.TimeUnit

class CatalogSyncWorker(appContext: Context, parameters: WorkerParameters) : Worker(appContext, parameters) {
    override fun doWork(): Result {
        val repository = CatalogRepository(applicationContext)
        if (repository.feedUrl().isBlank()) return Result.success()
        return try {
            repository.refresh()
            Result.success()
        } catch (error: CatalogFetchException) {
            if (error.retryable) Result.retry() else Result.failure()
        } catch (_: IOException) {
            Result.retry()
        } catch (_: IllegalArgumentException) {
            Result.failure()
        }
    }
}

object CatalogSyncScheduler {
    private const val UNIQUE_WORK = "external-catalog-refresh"

    fun scheduleIfConfigured(context: Context) {
        val workManager = WorkManager.getInstance(context)
        if (CatalogRepository(context).feedUrl().isBlank()) {
            workManager.cancelUniqueWork(UNIQUE_WORK)
            return
        }
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
        val request = PeriodicWorkRequest.Builder(
            CatalogSyncWorker::class.java,
            24, TimeUnit.HOURS,
            6, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .build()
        workManager.enqueueUniquePeriodicWork(UNIQUE_WORK, ExistingPeriodicWorkPolicy.UPDATE, request)
    }
}
