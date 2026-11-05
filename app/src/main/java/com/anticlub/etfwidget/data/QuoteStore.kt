package com.anticlub.etfwidget.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

// Un único DataStore para toda la app, "colgado" del Context. Va a nivel de archivo a propósito.
private val Context.dataStore by preferencesDataStore(name = "etf_prefs")

class QuoteStore(private val context: Context) {

    // --- Ticker (ISIN) elegido ---
    suspend fun ticker(): String {
        val prefs = context.dataStore.data.first()
        return prefs[TICKER] ?: DEFAULT_ISIN
    }

    suspend fun setTicker(isin: String) {
        context.dataStore.edit { prefs -> prefs[TICKER] = isin }
    }

    // --- Última cotización ---
    suspend fun saveQuote(q: Quote) {
        context.dataStore.edit { prefs ->
            prefs[SYMBOL] = q.symbol
            prefs[NAME] = q.name
            prefs[CURRENCY] = q.currency
            prefs[PRICE] = q.price
            prefs[PREV_CLOSE] = q.previousClose
            prefs[MARKET_TIME] = q.marketTime
            prefs[IS_OPEN] = q.isMarketOpen
            prefs.remove(ERROR)          // si hubo éxito, borramos el error anterior
        }
    }

    suspend fun lastQuote(): Quote? {
        val prefs = context.dataStore.data.first()
        val price = prefs[PRICE] ?: return null   // sin precio guardado => no hay cotización
        return Quote(
            symbol = prefs[SYMBOL] ?: "",
            name = prefs[NAME] ?: "",
            currency = prefs[CURRENCY] ?: "",
            price = price,
            previousClose = prefs[PREV_CLOSE] ?: 0.0,
            marketTime = prefs[MARKET_TIME] ?: 0L,
            isMarketOpen = prefs[IS_OPEN] ?: false,
        )
    }

    // --- Último error (para mostrar "sin conexión" sin perder el dato viejo) ---
    suspend fun saveError(message: String) {
        context.dataStore.edit { prefs -> prefs[ERROR] = message }
    }

    suspend fun lastError(): String? {
        return context.dataStore.data.first()[ERROR]
    }

    companion object {
        const val DEFAULT_ISIN = "IE00B5BMR087"

        private val TICKER = stringPreferencesKey("ticker")
        private val SYMBOL = stringPreferencesKey("symbol")
        private val NAME = stringPreferencesKey("name")
        private val CURRENCY = stringPreferencesKey("currency")
        private val PRICE = doublePreferencesKey("price")
        private val PREV_CLOSE = doublePreferencesKey("prev_close")
        private val MARKET_TIME = longPreferencesKey("market_time")
        private val IS_OPEN = booleanPreferencesKey("is_open")
        private val ERROR = stringPreferencesKey("error")
    }
}