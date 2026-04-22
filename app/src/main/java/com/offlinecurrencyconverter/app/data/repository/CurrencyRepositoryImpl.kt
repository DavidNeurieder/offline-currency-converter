package com.offlinecurrencyconverter.app.data.repository

import com.offlinecurrencyconverter.app.data.local.dao.CurrencyDao
import com.offlinecurrencyconverter.app.data.local.entity.CurrencyEntity
import com.offlinecurrencyconverter.app.data.remote.api.FrankfurterApi
import com.offlinecurrencyconverter.app.domain.model.Currency
import com.offlinecurrencyconverter.app.domain.repository.CurrencyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CurrencyRepositoryImpl @Inject constructor(
    private val currencyDao: CurrencyDao,
    private val frankfurterApi: FrankfurterApi
) : CurrencyRepository {

    override fun getAllCurrencies(): Flow<List<Currency>> {
        return currencyDao.getAllCurrencies().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getSelectedCurrencies(): Flow<List<Currency>> {
        return currencyDao.getSelectedCurrencies().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getCurrencyByCode(code: String): Currency? {
        return currencyDao.getCurrencyByCode(code)?.toDomain()
    }

    override suspend fun updateCurrencySelection(code: String, selected: Boolean) {
        currencyDao.updateSelection(code, selected)
    }

    override suspend fun initializeCurrencies(currencies: List<Currency>) {
        currencyDao.insertCurrencies(currencies.map { it.toEntity() })
    }

    override suspend fun clearAllCurrencies() {
        currencyDao.deleteAll()
    }

    override suspend fun hasCurrencies(): Boolean {
        return currencyDao.getCount() > 0
    }

    override suspend fun fetchAndSaveCurrenciesFromApi(): Result<Unit> {
        return try {
            val response = frankfurterApi.getCurrencies()
            if (response.isSuccessful) {
                val currencies = response.body() ?: emptyList()
                val entities = currencies.map { item ->
                    CurrencyEntity(
                        code = item.isoCode,
                        name = item.name,
                        symbol = item.symbol ?: "",
                        isoNumeric = item.isoNumeric,
                        startDate = item.startDate,
                        endDate = item.endDate,
                        isSelectedForOffline = item.isoCode in DEFAULT_SELECTED_CURRENCIES,
                        flagUrl = getFlagUrl(item.isoCode)
                    )
                }
                currencyDao.deleteAll()
                currencyDao.insertCurrencies(entities)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to fetch currencies: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun CurrencyEntity.toDomain(): Currency = Currency(
        code = code,
        name = name,
        symbol = symbol,
        isSelectedForOffline = isSelectedForOffline,
        flagUrl = flagUrl
    )

    private fun Currency.toEntity(): CurrencyEntity = CurrencyEntity(
        code = code,
        name = name,
        symbol = symbol,
        isSelectedForOffline = isSelectedForOffline,
        flagUrl = flagUrl
    )

    companion object {
        private val DEFAULT_SELECTED_CURRENCIES = setOf("EUR", "GBP", "USD")
        private const val FLAG_CDN_BASE = "https://flagcdn.com/w80"

        private val COUNTRY_CODES = mapOf(
            "AED" to "ae", "AFN" to "af", "ALL" to "al", "AMD" to "am",
            "ANG" to "cw", "AOA" to "ao", "ARS" to "ar", "AUD" to "au",
            "AWG" to "aw", "AZN" to "az", "BAM" to "ba", "BBD" to "bb",
            "BDT" to "bd", "BGN" to "bg", "BHD" to "bh", "BIF" to "bi",
            "BMD" to "bm", "BND" to "bn", "BOB" to "bo", "BRL" to "br",
            "BSD" to "bs", "BTN" to "bt", "BWP" to "bw", "BYN" to "by",
            "BZD" to "bz", "CAD" to "ca", "CDF" to "cd", "CHF" to "ch",
            "CLP" to "cl", "CNY" to "cn", "COP" to "co", "CRC" to "cr",
            "CUP" to "cu", "CVE" to "cv", "CZK" to "cz", "DJF" to "dj",
            "DKK" to "dk", "DOP" to "do", "DZD" to "dz", "EGP" to "eg",
            "ERN" to "er", "ETB" to "et", "EUR" to "eu", "FJD" to "fj",
            "FKP" to "fk", "GBP" to "gb", "GEL" to "ge", "GHS" to "gh",
            "GIP" to "gi", "GMD" to "gm", "GNF" to "gn", "GTQ" to "gt",
            "GYD" to "gy", "HKD" to "hk", "HNL" to "hn", "HRK" to "hr",
            "HTG" to "ht", "HUF" to "hu", "IDR" to "id", "ILS" to "il",
            "INR" to "in", "IQD" to "iq", "IRR" to "ir", "ISK" to "is",
            "JMD" to "jm", "JOD" to "jo", "JPY" to "jp", "KES" to "ke",
            "KGS" to "kg", "KHR" to "kh", "KMF" to "km", "KPW" to "kp",
            "KRW" to "kr", "KWD" to "kw", "KYD" to "ky", "KZT" to "kz",
            "LAK" to "la", "LBP" to "lb", "LKR" to "lk", "LRD" to "lr",
            "LSL" to "ls", "LYD" to "ly", "MAD" to "ma", "MDL" to "md",
            "MGA" to "mg", "MKD" to "mk", "MMK" to "mm", "MNT" to "mn",
            "MOP" to "mo", "MRU" to "mr", "MUR" to "mu", "MVR" to "mv",
            "MWK" to "mw", "MXN" to "mx", "MYR" to "my", "MZN" to "mz",
            "NAD" to "na", "NGN" to "ng", "NIO" to "ni", "NOK" to "no",
            "NPR" to "np", "NZD" to "nz", "OMR" to "om", "PAB" to "pa",
            "PEN" to "pe", "PGK" to "pg", "PHP" to "ph", "PKR" to "pk",
            "PLN" to "pl", "PYG" to "py", "QAR" to "qa", "RON" to "ro",
            "RSD" to "rs", "RUB" to "ru", "RWF" to "rw", "SAR" to "sa",
            "SBD" to "sb", "SCR" to "sc", "SDG" to "sd", "SEK" to "se",
            "SGD" to "sg", "SHP" to "sh", "SLE" to "sl", "SLL" to "sl",
            "SOS" to "so", "SRD" to "sr", "SSP" to "ss", "STN" to "st",
            "SYP" to "sy", "SZL" to "sz", "THB" to "th", "TJS" to "tj",
            "TMT" to "tm", "TND" to "tn", "TOP" to "to", "TRY" to "tr",
            "TTD" to "tt", "TWD" to "tw", "TZS" to "tz", "UAH" to "ua",
            "UGX" to "ug", "USD" to "us", "UYU" to "uy", "UZS" to "uz",
            "VES" to "ve", "VND" to "vn", "VUV" to "vu", "WST" to "ws",
            "XAF" to "cm", "XCD" to "ag", "XOF" to "sn", "XPF" to "pf",
            "YER" to "ye", "ZAR" to "za", "ZMW" to "zm", "ZWL" to "zw"
        )

        private fun getFlagUrl(currencyCode: String): String? {
            val countryCode = COUNTRY_CODES[currencyCode] ?: return null
            return "$FLAG_CDN_BASE/$countryCode.png"
        }
    }
}
