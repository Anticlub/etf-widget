package com.anticlub.etfwidget.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class TradegateQuoteRepository : QuoteRepository {

    override suspend fun fetch(isin: String): Quote = withContext(Dispatchers.IO) {
        val url = URL("$BASE_URL$isin")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("User-Agent", "Mozilla/5.0")
            connectTimeout = 10_000
            readTimeout = 10_000
        }

        val body = try {
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }

        parseTradegateQuote(body, isin)
    }

    private companion object {
        const val BASE_URL = "https://www.tradegatebsx.com/refresh.php?isin="
    }
}

// Función pura: del JSON de Tradegate a un Quote.
fun parseTradegateQuote(json: String, isin: String): Quote {
    val obj = JSONObject(json)
    return Quote(
        symbol = isin,
        name = "iShares Core S&P 500 UCITS ETF",   // Tradegate no manda el nombre
        currency = "EUR",                           // Tradegate siempre cotiza en €
        price = obj.number("last"),              // último precio negociado
        previousClose = obj.number("close"),     // cierre anterior → para el %
        marketTime = System.currentTimeMillis() / 1000,  // el dato es "de ahora"
        isMarketOpen = isTradegateOpen(),
    )
}

// Tradegate negocia L-V de 08:00 a 22:00 (hora de Alemania).
private fun isTradegateOpen(): Boolean {
    val berlin = ZonedDateTime.now(ZoneId.of("Europe/Berlin"))
    if (berlin.dayOfWeek == DayOfWeek.SATURDAY || berlin.dayOfWeek == DayOfWeek.SUNDAY) return false
    val now = berlin.toLocalTime()
    return now >= LocalTime.of(8, 0) && now < LocalTime.of(22, 0)
}

// Tradegate manda números a veces como 740.66 y a veces como "740,60" (formato alemán).
// Esta función los normaliza a Double en ambos casos, sin reventar.
private fun JSONObject.number(key: String): Double {
    val raw = get(key)
    return when (raw) {
        is Number -> raw.toDouble()
        is String -> raw.trim()
            .replace("+", "")
            .replace(".", "")    // separador de miles alemán, fuera
            .replace(",", ".")   // coma decimal -> punto
            .toDouble()
        else -> throw JSONException("Campo '$key' no numérico: $raw")
    }
}