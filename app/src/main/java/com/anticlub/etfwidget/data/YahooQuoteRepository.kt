package com.anticlub.etfwidget.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class YahooQuoteRepository : QuoteRepository {

    override suspend fun fetch(symbol: String): Quote = withContext(Dispatchers.IO){
        val url = URL("$BASE_URL$symbol?interval=5m&range=1d")

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

        parseQuote(body)
    }

    // Serie de cierres del día (para la sparkline). Tradegate no la da; Yahoo sí.
    suspend fun intraday(symbol: String): List<Double> = withContext(Dispatchers.IO) {
        val url = URL("$BASE_URL$symbol?interval=5m&range=1d")

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

        parseIntraday(body)
    }

    // Resuelve un ISIN a (símbolo de Yahoo, nombre del fondo) usando el buscador de Yahoo.
    // Necesario porque Yahoo trabaja con tickers, no ISINs; y nos da el nombre real del fondo.
    suspend fun resolve(isin: String): Pair<String, String>? = withContext(Dispatchers.IO) {
        val url = URL("$SEARCH_URL$isin&quotesCount=6")

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

        parseSearch(body)
    }

    private companion object {
        const val BASE_URL = "https://query1.finance.yahoo.com/v8/finance/chart/"
        const val SEARCH_URL = "https://query1.finance.yahoo.com/v1/finance/search?q="
    }
}

// Función pura: del resultado de búsqueda saca (símbolo, nombre).
// Prefiere un símbolo de Xetra (.DE) porque es el que tiene serie intradía para la sparkline.
fun parseSearch(json: String): Pair<String, String>? {
    val quotes = JSONObject(json).optJSONArray("quotes") ?: return null
    if (quotes.length() == 0) return null

    var best: JSONObject? = null
    for (i in 0 until quotes.length()) {
        val q = quotes.getJSONObject(i)
        if (q.optString("symbol").endsWith(".DE")) {
            best = q
            break
        }
        if (best == null) best = q
    }

    val chosen = best ?: return null
    val symbol = chosen.optString("symbol")
    if (symbol.isBlank()) return null
    val name = chosen.optString("longname", chosen.optString("shortname", symbol))
    return symbol to name
}

// Función pura: extrae los cierres de 5 min del JSON de Yahoo, descartando los null.
fun parseIntraday(json: String): List<Double> {
    val closes = JSONObject(json)
        .getJSONObject("chart")
        .getJSONArray("result")
        .getJSONObject(0)
        .getJSONObject("indicators")
        .getJSONArray("quote")
        .getJSONObject(0)
        .getJSONArray("close")

    return buildList {
        for (i in 0 until closes.length()) {
            if (!closes.isNull(i)) add(closes.getDouble(i))
        }
    }
}

fun parseQuote(json: String): Quote {
    val meta = JSONObject(json)
        .getJSONObject("chart")
        .getJSONArray("result")
        .getJSONObject(0)
        .getJSONObject("meta")

    val now = System.currentTimeMillis() / 1000
    val regular = meta.optJSONObject("currentTradingPeriod")?.optJSONObject("regular")
    val start = regular?.optLong("start") ?: 0L
    val end = regular?.optLong("end") ?: 0L

    return Quote(
        symbol = meta.optString("symbol"),
        name = meta.optString("longName", meta.optString("shortName", meta.optString("symbol"))),
        currency = meta.optString("currency"),
        price = meta.optDouble("regularMarketPrice"),
        previousClose = meta.optDouble("chartPreviousClose"),
        marketTime = meta.optLong("regularMarketTime"),
        isMarketOpen = now in start until end,
    )
}