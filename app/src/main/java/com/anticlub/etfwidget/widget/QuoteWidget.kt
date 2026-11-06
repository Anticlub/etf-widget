package com.anticlub.etfwidget.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.anticlub.etfwidget.data.Quote
import com.anticlub.etfwidget.data.QuoteStore

class QuoteWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val store = QuoteStore(context)
        val quote = store.lastQuote()          // lee lo último guardado, sin red
        provideContent {
            WidgetContent(quote)
        }
    }

    @Composable
    private fun WidgetContent(quote: Quote?) {
        Column(
            modifier = GlanceModifier.fillMaxSize().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (quote == null) {
                Text("Pulsa ↻ para cargar")
            } else {
                val sube = quote.change >= 0
                val signo = if (sube) "+" else ""
                val color = if (sube) Color(0xFF2E7D32) else Color(0xFFC62828)
                val precio = "%.2f".format(quote.price)
                val pct = "%.2f".format(quote.changePct)

                Text(quote.symbol, style = TextStyle(fontSize = 12.sp))
                Text(
                    "$precio ${quote.currency}",
                    style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold),
                )
                Text(
                    "$signo$pct %",
                    style = TextStyle(color = ColorProvider(color)),
                )
            }
        }
    }
}