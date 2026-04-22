package com.offlinecurrencyconverter.app.data.repository

import com.offlinecurrencyconverter.app.data.local.dao.CurrencyDao
import com.offlinecurrencyconverter.app.data.local.dao.RecentConversionDao
import com.offlinecurrencyconverter.app.data.local.entity.RecentConversionEntity
import com.offlinecurrencyconverter.app.domain.model.ConversionResult
import com.offlinecurrencyconverter.app.domain.model.Currency
import com.offlinecurrencyconverter.app.domain.repository.RecentConversionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RecentConversionRepositoryImpl @Inject constructor(
    private val recentConversionDao: RecentConversionDao,
    private val currencyDao: CurrencyDao
) : RecentConversionRepository {

    override fun getRecentConversions(limit: Int): Flow<List<ConversionResult>> {
        return recentConversionDao.getRecentConversions(limit).map { entities ->
            entities.mapNotNull { entity ->
                val sourceCurrency = currencyDao.getCurrencyByCode(entity.sourceCurrencyCode)
                val targetCurrency = currencyDao.getCurrencyByCode(entity.targetCurrencyCode)

                if (sourceCurrency != null && targetCurrency != null) {
                    ConversionResult(
                        sourceAmount = entity.sourceAmount,
                        sourceCurrency = Currency(
                            code = sourceCurrency.code,
                            name = sourceCurrency.name,
                            symbol = sourceCurrency.symbol,
                            isSelectedForOffline = sourceCurrency.isSelectedForOffline
                        ),
                        targetAmount = entity.targetAmount,
                        targetCurrency = Currency(
                            code = targetCurrency.code,
                            name = targetCurrency.name,
                            symbol = targetCurrency.symbol,
                            isSelectedForOffline = targetCurrency.isSelectedForOffline
                        ),
                        rate = entity.rate,
                        timestamp = entity.timestamp
                    )
                } else null
            }
        }
    }

    override suspend fun saveConversion(conversion: ConversionResult) {
        recentConversionDao.insertConversion(conversion.toEntity())
        recentConversionDao.trimOldConversions()
    }

    override suspend fun clearHistory() {
        recentConversionDao.deleteAll()
    }

    private fun ConversionResult.toEntity(): RecentConversionEntity = RecentConversionEntity(
        sourceAmount = sourceAmount,
        sourceCurrencyCode = sourceCurrency.code,
        targetAmount = targetAmount,
        targetCurrencyCode = targetCurrency.code,
        rate = rate,
        timestamp = timestamp
    )
}
