package com.offlinecurrencyconverter.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.room.Room
import com.offlinecurrencyconverter.app.MainActivity
import com.offlinecurrencyconverter.app.R
import com.offlinecurrencyconverter.app.data.PreferencesManager
import com.offlinecurrencyconverter.app.data.local.CurrencyDatabase
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
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, CurrencyWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }

    companion object {
        const val ACTION_UPDATE_WIDGET = "com.offlinecurrencyconverter.app.ACTION_UPDATE_WIDGET"

        private val rateFormat = DecimalFormat("#,##0.00####")

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
                    val preferencesManager = PreferencesManager(context)
                    val database = Room.databaseBuilder(
                        context,
                        CurrencyDatabase::class.java,
                        "offline_currency_converter_db"
                    )
                        .addMigrations(CurrencyDatabase.MIGRATION_4_5)
                        .addMigrations(CurrencyDatabase.MIGRATION_5_6)
                        .build()

                    val sourceCode = preferencesManager.sourceCurrency.first() ?: "USD"
                    val targetCode = preferencesManager.targetCurrency.first() ?: "EUR"

                    val rate = database.exchangeRateDao().getRate(sourceCode, targetCode)

                    if (rate != null) {
                        views.setTextViewText(
                            R.id.widget_rate,
                            "1 $sourceCode = ${rateFormat.format(rate.rate)} $targetCode"
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
    }
}
