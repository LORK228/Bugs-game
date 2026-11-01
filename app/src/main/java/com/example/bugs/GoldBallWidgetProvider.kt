package com.example.bugs

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import com.example.bugs.network.CbrNetworkClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class GoldBallWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateAllWidgets(context, appWidgetManager)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_FORCE_UPDATE) {
            updateAllWidgets(context)
        }
    }

    companion object {
        const val ACTION_FORCE_UPDATE = "com.example.bugs.ACTION_FORCE_UPDATE"

        fun updateAllWidgets(context: Context, manager: AppWidgetManager? = null) {
            val appWidgetManager = manager ?: AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, GoldBallWidgetProvider::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)

            if (allWidgetIds.isEmpty()) return

            CoroutineScope(Dispatchers.IO).launch {
                val goldPrice = CbrNetworkClient.fetchGoldPrice()
                Log.d("CBR_API", "Обновление виджета новым значением: $goldPrice")

                val priceText = String.format(Locale.US, "%.2f", goldPrice)

                withContext(Dispatchers.Main) {
                    for (widgetId in allWidgetIds) {
                        val views = RemoteViews(context.packageName, R.layout.widget_gold_ball)
                        views.setTextViewText(R.id.tv_gold_price, priceText)

                        // Нажатие на виджет запускает немедленное обновление
                        val clickIntent = Intent(context, GoldBallWidgetProvider::class.java).apply {
                            action = ACTION_FORCE_UPDATE
                        }
                        val pendingIntent = PendingIntent.getBroadcast(
                            context,
                            widgetId,
                            clickIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(R.id.widget_ball_root, pendingIntent)

                        appWidgetManager.updateAppWidget(widgetId, views)
                    }
                }
            }
        }
    }
}