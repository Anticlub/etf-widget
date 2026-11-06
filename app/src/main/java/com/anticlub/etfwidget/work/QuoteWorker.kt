package com.anticlub.etfwidget.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.anticlub.etfwidget.data.QuoteUpdater
import java.util.concurrent.TimeUnit

class QuoteWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        QuoteUpdater.refresh(applicationContext)   // pide, guarda y repinta el widget
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "quote_refresh"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<QuoteWorker>(15, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)   // solo con red
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,   // si ya está programado, no lo dupliques
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }

        // Un refresco único e inmediato (lo usa la config al añadir un widget).
        fun runOnce(context: Context) {
            val request = OneTimeWorkRequestBuilder<QuoteWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
