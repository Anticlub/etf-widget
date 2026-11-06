package com.anticlub.etfwidget.data

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.anticlub.etfwidget.widget.QuoteWidget

object QuoteUpdater {
    private val repository: QuoteRepository = TradegateQuoteRepository()

    suspend fun refresh(context: Context) {
        val store = QuoteStore(context)
        try {
            val quote = repository.fetch(store.ticker())
            store.saveQuote(quote)
        } catch (e: Exception) {
            store.saveError(e.message ?: "Error de red")
        }
        QuoteWidget().updateAll(context)   // repinta todos los widgets
    }
}