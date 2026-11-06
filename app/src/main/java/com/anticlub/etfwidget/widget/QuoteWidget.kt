package com.anticlub.etfwidget.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.anticlub.etfwidget.MainActivity
import com.anticlub.etfwidget.data.Quote
import com.anticlub.etfwidget.data.QuoteStore

class QuoteWidget : GlanceAppWidget() {

    // El widget se pinta distinto según el tamaño al que lo estiren.
    override val sizeMode = SizeMode.Responsive(setOf(SMALL, MEDIUM, LARGE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val store = QuoteStore(context)
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val isin = store.widgetIsin(appWidgetId)        // qué ETF muestra ESTE widget
        val quote = isin?.let { store.lastQuote(it) }   // su cotización, sin red
        val error = isin?.let { store.lastError(it) }   // si su última actualización falló

        // La sparkline se dibuja aquí (en segundo plano), no en la UI.
        val sparkline = quote?.intraday
            ?.takeIf { it.size >= 2 }
            ?.let {
                val up = quote.change >= 0
                val color = if (up) 0xFF2E7D32.toInt() else 0xFFC62828.toInt()
                sparklineBitmap(it, 400, 120, color)
            }

        provideContent {
            WidgetContent(quote, error, sparkline, LocalSize.current)
        }
    }

    @SuppressLint("RestrictedApi")
    @Composable
    private fun WidgetContent(quote: Quote?, error: String?, sparkline: Bitmap?, size: DpSize) {
        GlanceTheme {   // da colores que se adaptan a claro/oscuro
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .appWidgetBackground()
                    .background(GlanceTheme.colors.widgetBackground)   // fondo de tarjeta
                    .cornerRadius(16.dp)                               // esquinas redondeadas
                    .padding(12.dp)
                    .clickable(actionStartActivity<MainActivity>()),   // tocar abre la app
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Cabecera: ticker a la izquierda, ↻ a la derecha
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        quote?.name?.takeIf { it.isNotBlank() } ?: quote?.symbol ?: "ETF",
                        modifier = GlanceModifier.defaultWeight(),
                        maxLines = 1,
                        style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onSurfaceVariant),
                    )
                    // Aviso de estado: sin red, o mercado cerrado
                    val status = when {
                        error != null -> "Sin red"
                        quote != null && !quote.isMarketOpen -> "Cerrado"
                        else -> null
                    }
                    if (status != null) {
                        Text(
                            status,
                            modifier = GlanceModifier.padding(end = 6.dp),
                            style = TextStyle(fontSize = 10.sp, color = GlanceTheme.colors.onSurfaceVariant),
                        )
                    }
                    Text(
                        "↻",
                        modifier = GlanceModifier.clickable(actionRunCallback<RefreshAction>()),
                        style = TextStyle(fontSize = 18.sp, color = GlanceTheme.colors.onSurface),
                    )
                }

                if (quote == null) {
                    Text(
                        "Pulsa ↻ para cargar",
                        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant),
                    )
                } else {
                    val sube = quote.change >= 0
                    val signo = if (sube) "+" else ""
                    val color = if (sube) Color(0xFF2E7D32) else Color(0xFFC62828)
                    val precio = "%.2f".format(quote.price)
                    val pct = "%.2f".format(quote.changePct)

                    Text(
                        "$precio ${quote.currency}",
                        style = TextStyle(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlanceTheme.colors.onSurface,
                        ),
                    )
                    Text("$signo$pct %", style = TextStyle(color = ColorProvider(color)))

                    // Sparkline: solo en tamaño mediano y grande
                    if (size.height >= MEDIUM.height && sparkline != null) {
                        val sparkHeight = if (size.height >= LARGE.height) 64.dp else 40.dp
                        Image(
                            provider = ImageProvider(sparkline),
                            contentDescription = "Gráfica del día",
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .height(sparkHeight)
                                .padding(top = 6.dp),
                            contentScale = ContentScale.FillBounds,
                        )
                    }

                    // Cierre anterior: solo en tamaño grande
                    if (size.height >= LARGE.height) {
                        Text(
                            "Cierre: ${"%.2f".format(quote.previousClose)} ${quote.currency}",
                            style = TextStyle(fontSize = 11.sp, color = GlanceTheme.colors.onSurfaceVariant),
                        )
                    }
                }
            }
        }
    }

    companion object {
        private val SMALL = DpSize(120.dp, 48.dp)
        private val MEDIUM = DpSize(180.dp, 110.dp)
        private val LARGE = DpSize(250.dp, 160.dp)
    }
}
