package com.anticlub.etfwidget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.anticlub.etfwidget.data.Quote
import com.anticlub.etfwidget.data.QuoteStore
import com.anticlub.etfwidget.data.QuoteUpdater
import com.anticlub.etfwidget.data.TradegateQuoteRepository
import com.anticlub.etfwidget.ui.theme.ETFWidgetTheme
import com.anticlub.etfwidget.work.QuoteWorker
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        QuoteWorker.schedule(this)   // programa el refresco automático al abrir la app
        setContent {
            ETFWidgetTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ConfigScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun ConfigScreen(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val repository = remember { TradegateQuoteRepository() }
    val store = remember { QuoteStore(context) }

    var isin by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var quote by remember { mutableStateOf<Quote?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    // Al abrir la pantalla, rellenamos el campo con el ticker ya guardado.
    LaunchedEffect(Unit) {
        isin = store.ticker()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("ETF Widget", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Introduce el ISIN del ETF (p. ej. IE00B5BMR087, el iShares Core S&P 500).",
            style = MaterialTheme.typography.bodyMedium,
        )

        OutlinedTextField(
            value = isin,
            onValueChange = { isin = it.trim() },
            label = { Text("ISIN") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            onClick = {
                scope.launch {
                    loading = true
                    error = null
                    store.setTicker(isin)
                    QuoteUpdater.refresh(context)   // pide, guarda Y repinta el widget
                    quote = store.lastQuote()       // leemos de vuelta para mostrarlo en la pantalla
                    error = store.lastError()
                    loading = false
                }
            },
            enabled = isin.isNotBlank() && !loading,
        ) {
            Text("Guardar y probar")
        }

        when {
            loading -> CircularProgressIndicator()
            error != null -> Text("Error: $error", color = MaterialTheme.colorScheme.error)
            quote != null -> {
                val q = quote!!
                Text(q.name, style = MaterialTheme.typography.titleMedium)
                Text("${q.price} ${q.currency}", style = MaterialTheme.typography.headlineSmall)
                val signo = if (q.change >= 0) "+" else ""
                Text("$signo${"%.2f".format(q.changePct)} %")
            }
        }
    }
}