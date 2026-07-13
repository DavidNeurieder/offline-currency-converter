package com.offlinecurrencyconverter.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.offlinecurrencyconverter.app.R
import com.offlinecurrencyconverter.app.data.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CurrencyWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_UPDATE_WIDGET) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, CurrencyWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }

    companion object {
        const val ACTION_UPDATE_WIDGET = "com.offlinecurrencyconverter.app.ACTION_UPDATE_WIDGET"

        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_currency_converter)
            val preferencesManager = PreferencesManager(context)

            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            scope.launch {
                try {
                    val sourceCurrency = preferencesManager.sourceCurrency.first() ?: "USD"
                    val targetCurrency = preferencesManager.targetCurrency.first() ?: "EUR"

                    views.setTextViewText(R.id.widget_source_currency, sourceCurrency)
                    views.setTextViewText(R.id.widget_target_currency, targetCurrency)
                    views.setTextViewText(R.id.widget_rate, "Tap to convert")

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                } catch (e: Exception) {
                    views.setTextViewText(R.id.widget_rate, "--")
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }
    }
}
