package com.anticlub.etfwidget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.anticlub.etfwidget.data.QuoteStore
import com.anticlub.etfwidget.data.TradegateQuoteRepository
import com.anticlub.etfwidget.data.YahooQuoteRepository
import com.anticlub.etfwidget.ui.theme.ETFWidgetTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ETFWidgetTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    QuoteTester(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ETFWidgetTheme {
        Greeting("Android")
    }
}

@Composable
fun QuoteTester(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val repository = remember { TradegateQuoteRepository() }
    val store = remember { QuoteStore(context) }
    var result by remember { mutableStateOf("Pulsa un botón") }

    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Button(onClick = {
            scope.launch {
                result = try {
                    val q = repository.fetch(store.ticker())   // usa el ticker guardado
                    store.saveQuote(q)                         // <-- GUARDA en disco
                    "RED:\n${q.name}\n${q.price} ${q.currency}\n${q.changePct}%"
                } catch (e: Exception) {
                    store.saveError(e.message ?: "error")
                    "Error: ${e.message}"
                }
            }
        }) {
            Text("Cargar (red)")
        }

        Button(onClick = {
            scope.launch {
                val q = store.lastQuote()                      // <-- LEE de disco, sin red
                result = if (q != null) {
                    "GUARDADO (sin red):\n${q.name}\n${q.price} ${q.currency}\n${q.changePct}%"
                } else {
                    "No hay nada guardado todavía"
                }
            }
        }) {
            Text("Leer guardado (sin red)")
        }

        Text(result)
    }
}