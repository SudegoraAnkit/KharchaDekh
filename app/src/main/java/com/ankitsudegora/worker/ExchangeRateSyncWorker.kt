package com.ankitsudegora.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import com.ankitsudegora.data.AppDatabase
import com.ankitsudegora.data.ExchangeRate
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class ExchangeRateSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("ExchangeRateSyncWorker", "Starting daily exchange rate synchronization...")
        val db = AppDatabase.getDatabase(applicationContext)

        try {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("https://open.er-api.com/v6/latest/INR")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("ExchangeRateSyncWorker", "API returned error code: ${response.code}")
                    return Result.retry()
                }

                val json = response.body?.string() ?: return Result.failure()
                
                val moshi = Moshi.Builder()
                    .add(KotlinJsonAdapterFactory())
                    .build()
                
                val adapter = moshi.adapter(ExchangeRateResponse::class.java)
                val rateResponse = adapter.fromJson(json)

                if (rateResponse != null && rateResponse.result == "success") {
                    val timestamp = System.currentTimeMillis()
                    val dbRates = rateResponse.rates.map { (code, rate) ->
                        // rate is target per 1 INR (e.g., USD = 0.012).
                        // Store how much INR per 1 unit of foreign currency (e.g. USD -> INR = 1 / 0.012).
                        val rateToInr = if (rate > 0) 1.0 / rate else 1.0
                        ExchangeRate(
                            baseCurrency = code.uppercase(),
                            targetCurrency = "INR",
                            rate = rateToInr,
                            timestamp = timestamp
                        )
                    }

                    db.exchangeRateDao().insertRates(dbRates)
                    Log.d("ExchangeRateSyncWorker", "Successfully stored ${dbRates.size} exchange rates relative to INR.")
                    return Result.success()
                } else {
                    Log.e("ExchangeRateSyncWorker", "API response marked as unsuccessful or empty.")
                    return Result.failure()
                }
            }
        } catch (e: Exception) {
            Log.e("ExchangeRateSyncWorker", "Failed to fetch exchange rates", e)
            return Result.retry()
        }
    }

    companion object {
        fun scheduleDailySync(context: Context, forceRestart: Boolean = false) {
            val workManager = WorkManager.getInstance(context)
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<ExchangeRateSyncWorker>(24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            val policy = if (forceRestart) ExistingPeriodicWorkPolicy.UPDATE else ExistingPeriodicWorkPolicy.KEEP

            workManager.enqueueUniquePeriodicWork(
                "kharchadekh_exchange_rate_sync",
                policy,
                request
            )
            Log.d("ExchangeRateSyncWorker", "Scheduled daily exchange rate sync.")
        }
        
        fun cancelDailySync(context: Context) {
            val workManager = WorkManager.getInstance(context)
            workManager.cancelUniqueWork("kharchadekh_exchange_rate_sync")
            Log.d("ExchangeRateSyncWorker", "Cancelled daily exchange rate sync.")
        }
    }
}

// Moshi container class for parsing
data class ExchangeRateResponse(
    val result: String,
    val base_code: String,
    val rates: Map<String, Double>
)
