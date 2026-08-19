package com.offlinecurrencyconverter.app.widget

import android.content.Context
import com.offlinecurrencyconverter.app.data.local.CurrencyDatabase
import com.offlinecurrencyconverter.app.data.PreferencesManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun database(): CurrencyDatabase
    fun preferencesManager(): PreferencesManager

    companion object {
        fun from(context: Context): WidgetEntryPoint {
            return EntryPointAccessors.fromApplication(
                context.applicationContext,
                WidgetEntryPoint::class.java
            )
        }
    }
}
