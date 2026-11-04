package com.anticlub.etfwidget.data

import org.junit.Assert.assertEquals
import org.junit.Test

class TradegateParseTest {

    @Test
    fun `precio y cierre cuando vienen como numeros`() {
        val json = """{ "last": 740.66, "close": 729.74 }"""
        val q = parseTradegateQuote(json, "IE00B5BMR087")
        assertEquals(740.66, q.price, 0.001)
        assertEquals(729.74, q.previousClose, 0.001)
    }

    @Test
    fun `precio como texto con coma alemana`() {
        val json = """{ "last": "740,60", "close": "729,74" }"""
        val q = parseTradegateQuote(json, "IE00B5BMR087")
        assertEquals(740.60, q.price, 0.001)
    }

    @Test
    fun `texto con punto decimal NO se corrompe (el bug que arreglamos)`() {
        // Antes del fix, replace(".","") convertía "740.66" en 74066.0
        val json = """{ "last": "740.66", "close": 729.74 }"""
        val q = parseTradegateQuote(json, "IE00B5BMR087")
        assertEquals(740.66, q.price, 0.001)
    }

    @Test
    fun `texto con separador de miles aleman`() {
        val json = """{ "last": "1.234,56", "close": 1200.0 }"""
        val q = parseTradegateQuote(json, "IE00B5BMR087")
        assertEquals(1234.56, q.price, 0.001)
    }

    @Test
    fun `changePct se calcula a partir de precio y cierre`() {
        val json = """{ "last": 110.0, "close": 100.0 }"""
        val q = parseTradegateQuote(json, "IE00B5BMR087")
        assertEquals(10.0, q.changePct, 0.001)   // (110-100)/100 * 100 = 10%
    }
}