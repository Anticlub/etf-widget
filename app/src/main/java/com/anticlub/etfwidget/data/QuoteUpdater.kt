package com.anticlub.etfwidget.data

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import com.anticlub.etfwidget.widget.QuoteWidget

object QuoteUpdater {
    private val tradegate: QuoteRepository = TradegateQuoteRepository()
    private val yahoo = YahooQuoteRepository()

    // Refresca todos los ISIN que haya en pantalla (uno por cada widget, sin repetir).
    suspend fun refresh(context: Context) {
        val store = QuoteStore(context)
        val manager = GlanceAppWidgetManager(context)
        val isins = manager.getGlanceIds(QuoteWidget::class.java)
            .mapNotNull { store.widgetIsin(manager.getAppWidgetId(it)) }
            .distinct()

        for (isin in isins) refreshOne(store, isin)
        QuoteWidget().updateAll(context)
    }

    // Refresca un ISIN concreto (lo usa la pantalla de configuración al añadir un widget).
    suspend fun refreshOne(context: Context, isin: String) = refreshOne(QuoteStore(context), isin)

    private suspend fun refreshOne(store: QuoteStore, isin: String) {
        try {
            val quote = tradegate.fetch(isin)                      // precio en tiempo real
            val resolved = runCatching { yahoo.resolve(isin) }.getOrNull()  // (símbolo, nombre)
            val series = resolved
                ?.let { runCatching { yahoo.intraday(it.first) }.getOrDefault(emptyList()) }
                ?: emptyList()
            store.saveQuote(
                isin,
                quote.copy(
                    name = resolved?.second ?: quote.name,   // nombre real si Yahoo lo da
                    intraday = series,                       // serie para la sparkline
                ),
            )
        } catch (e: Exception) {
            store.saveError(isin, e.message ?: "Error de red")
        }
    }
}
