package com.anticlub.etfwidget.widget

import android.annotation.SuppressLint
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
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.action.actionStartActivity
import com.anticlub.etfwidget.MainActivity
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxWidth

class QuoteWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val store = QuoteStore(context)
        val quote = store.lastQuote()          // lee lo último guardado, sin red
        provideContent {
            WidgetContent(quote)
        }
    }

    @SuppressLint("RestrictedApi")
    @Composable
    private fun WidgetContent(quote: Quote?) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(12.dp)
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Cabecera: ticker a la izquierda, ↻ a la derecha
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    quote?.symbol ?: "ETF",
                    modifier = GlanceModifier.defaultWeight(),   // empuja el ↻ a la derecha
                    style = TextStyle(fontSize = 12.sp),
                )
                Text(
                    "↻",
                    modifier = GlanceModifier.clickable(actionRunCallback<RefreshAction>()),
                    style = TextStyle(fontSize = 18.sp),
                )
            }

            if (quote == null) {
                Text("Pulsa ↻ para cargar")
            } else {
                val sube = quote.change >= 0
                val signo = if (sube) "+" else ""
                val color = if (sube) Color(0xFF2E7D32) else Color(0xFFC62828)
                val precio = "%.2f".format(quote.price)
                val pct = "%.2f".format(quote.changePct)

                Text(
                    "$precio ${quote.currency}",
                    style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold),
                )
                Text("$signo$pct %", style = TextStyle(color = ColorProvider(color)))
            }
        }
    }
}