package com.anticlub.etfwidget.data

data class Quote (
    val symbol: String,
    val name: String,
    val currency: String,
    val price: Double,
    val previousClose: Double,
    val marketTime: Long,
    val isMarketOpen: Boolean,
){
    val change: Double get() = price - previousClose
    val changePct: Double
        get() = if (previousClose != 0.0) change / previousClose * 100 else 0.0
}