package com.anticlub.etfwidget.data

interface QuoteRepository {
    suspend fun fetch(symbol: String): Quote
}