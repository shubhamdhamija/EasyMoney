package com.invest.easymoney.worker

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.invest.easymoney.R
import com.invest.easymoney.domain.model.AlertType
import com.invest.easymoney.domain.repository.StockRepository
import com.invest.easymoney.util.Constants
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class AlertCheckWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val repository: StockRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val alerts = repository.getAllAlertsOnce()
            if (alerts.isEmpty()) return Result.success()

            // Group alerts by symbol to minimize API calls
            val symbolGroups = alerts.groupBy { it.symbol }
            val stockResults = repository.fetchStocksForSymbols(symbolGroups.keys.toList())
            val stocks = (stockResults as? com.invest.easymoney.util.Resource.Success)?.data ?: return Result.success()

            stocks.forEach { stock ->
                val stockAlerts = symbolGroups[stock.symbol] ?: return@forEach
                stockAlerts.forEach { alert ->
                    val triggered = when (alert.type) {
                        AlertType.INCREASE -> stock.changePercent >= alert.percentage
                        AlertType.DECREASE -> stock.changePercent <= -alert.percentage
                    }
                    if (triggered) {
                        val direction = if (alert.type == AlertType.INCREASE) "up" else "down"
                        showNotification(
                            title = "🔔 ${stock.symbol} Alert",
                            body = "${stock.symbol} is $direction ${String.format("%.2f", stock.changePercent)}% today!"
                        )
                    }
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun showNotification(title: String, body: String) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val notification = NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
