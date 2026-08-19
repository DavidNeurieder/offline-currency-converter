package com.offlinecurrencyconverter.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.offlinecurrencyconverter.app.MainActivity
import com.offlinecurrencyconverter.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.DecimalFormat

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
            val senderPackage = intent.`package`
            if (senderPackage != null && senderPackage != context.packageName) return

            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, CurrencyWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }

    companion object {
        const val ACTION_UPDATE_WIDGET = "com.offlinecurrencyconverter.app.ACTION_UPDATE_WIDGET"

        private val rateFormat = DecimalFormat("#,##0.00")

        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_currency_converter)

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_rate, pendingIntent)

            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                try {
                    val entryPoint = WidgetEntryPoint.from(context)
                    val preferencesManager = entryPoint.preferencesManager()
                    val database = entryPoint.database()

                    val sourceCode = preferencesManager.sourceCurrency.first() ?: "USD"
                    val targetCode = preferencesManager.targetCurrency.first() ?: "EUR"

                    val dao = database.exchangeRateDao()
                    val rateValue = when {
                        sourceCode == targetCode -> 1.0
                        else -> {
                            val direct = dao.getRate(sourceCode, targetCode)
                            if (direct != null) {
                                direct.rate
                            } else {
                                val eurToSource = dao.getRate("EUR", sourceCode)
                                val eurToTarget = dao.getRate("EUR", targetCode)
                                when {
                                    eurToSource != null && eurToTarget != null ->
                                        eurToTarget.rate / eurToSource.rate
                                    sourceCode == "EUR" && eurToTarget != null ->
                                        eurToTarget.rate
                                    targetCode == "EUR" && eurToSource != null ->
                                        1.0 / eurToSource.rate
                                    else -> null
                                }
                            }
                        }
                    }

                    if (rateValue != null) {
                        views.setTextViewText(
                            R.id.widget_rate,
                            "1 $sourceCode = ${rateFormat.format(rateValue)} $targetCode"
                        )
                    } else {
                        views.setTextViewText(R.id.widget_rate, "1 $sourceCode = -- $targetCode")
                    }

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                } catch (e: Exception) {
                    views.setTextViewText(R.id.widget_rate, "--")
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, CurrencyWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            for (appWidgetId in appWidgetIds) {
                updateWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }
}
