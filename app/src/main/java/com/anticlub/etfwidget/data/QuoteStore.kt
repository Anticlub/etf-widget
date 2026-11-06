package com.anticlub.etfwidget.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

// Un único DataStore para toda la app, "colgado" del Context. Va a nivel de archivo a propósito.
private val Context.dataStore by preferencesDataStore(name = "etf_prefs")

/**
 * Almacén en local. Dos cosas, por claves separadas:
 *  - qué ISIN muestra cada widget (por id de widget)
 *  - la última cotización de cada ISIN (compartida por los widgets que muestren ese ISIN)
 */
class QuoteStore(private val context: Context) {

    // --- ISIN elegido por cada widget ---
    suspend fun widgetIsin(appWidgetId: Int): String? =
        read { it[stringPreferencesKey("isin_$appWidgetId")] }

    suspend fun setWidgetIsin(appWidgetId: Int, isin: String) =
        write { it[stringPreferencesKey("isin_$appWidgetId")] = isin }

    suspend fun removeWidget(appWidgetId: Int) =
        write { it.remove(stringPreferencesKey("isin_$appWidgetId")) }

    // --- Última cotización, por ISIN ---
    suspend fun saveQuote(isin: String, q: Quote) = write { p ->
        p[ks("symbol", isin)] = q.symbol
        p[ks("name", isin)] = q.name
        p[ks("currency", isin)] = q.currency
        p[kd("price", isin)] = q.price
        p[kd("prev", isin)] = q.previousClose
        p[kl("time", isin)] = q.marketTime
        p[kb("open", isin)] = q.isMarketOpen
        p[ks("intraday", isin)] = q.intraday.joinToString(",")
        p.remove(ks("error", isin))   // si hubo éxito, borramos el error anterior
    }

    suspend fun lastQuote(isin: String): Quote? = read { p ->
        val price = p[kd("price", isin)] ?: return@read null
        Quote(
            symbol = p[ks("symbol", isin)] ?: isin,
            name = p[ks("name", isin)] ?: "",
            currency = p[ks("currency", isin)] ?: "",
            price = price,
            previousClose = p[kd("prev", isin)] ?: 0.0,
            marketTime = p[kl("time", isin)] ?: 0L,
            isMarketOpen = p[kb("open", isin)] ?: false,
            intraday = p[ks("intraday", isin)]
                ?.takeIf { it.isNotBlank() }
                ?.split(",")
                ?.mapNotNull { it.toDoubleOrNull() }
                ?: emptyList(),
        )
    }

    suspend fun saveError(isin: String, message: String) =
        write { it[ks("error", isin)] = message }

    suspend fun lastError(isin: String): String? =
        read { it[ks("error", isin)] }

    private suspend fun <T> read(block: (Preferences) -> T): T =
        block(context.dataStore.data.first())

    private suspend fun write(block: (MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }

    companion object {
        const val DEFAULT_ISIN = "IE00B5BMR087"

        // Claves "campo_ISIN" con el tipo correcto.
        private fun ks(field: String, isin: String) = stringPreferencesKey("${field}_$isin")
        private fun kd(field: String, isin: String) = doublePreferencesKey("${field}_$isin")
        private fun kl(field: String, isin: String) = longPreferencesKey("${field}_$isin")
        private fun kb(field: String, isin: String) = booleanPreferencesKey("${field}_$isin")
    }
}
