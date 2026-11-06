package com.anticlub.etfwidget.data

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.anticlub.etfwidget.widget.QuoteWidget

object QuoteUpdater {
    private val repository: QuoteRepository = TradegateQuoteRepository()
    private val yahoo = YahooQuoteRepository()

    // Símbolo de Yahoo para la serie de la sparkline (Tradegate no da la serie).
    // Xetra (.DE) porque es líquida y sí trae barras intradía de 5 min; Stuttgart (.SG) no.
    private const val SPARKLINE_SYMBOL = "SXR8.DE"

    suspend fun refresh(context: Context) {
        val store = QuoteStore(context)
        try {
            val quote = repository.fetch(store.ticker())            // precio en tiempo real
            val series = runCatching { yahoo.intraday(SPARKLINE_SYMBOL) }
                .getOrDefault(emptyList())                          // si falla, sin gráfica
            store.saveQuote(quote.copy(intraday = series))
        } catch (e: Exception) {
            store.saveError(e.message ?: "Error de red")
        }
        QuoteWidget().updateAll(context)   // repinta todos los widgets
    }
}
