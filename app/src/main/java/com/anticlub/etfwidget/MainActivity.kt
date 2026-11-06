package com.anticlub.etfwidget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.anticlub.etfwidget.data.QuoteUpdater
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
                    HomeScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var msg by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("ETF Widget", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Cada widget muestra el ETF que elijas al añadirlo. Mantén pulsado un hueco de la " +
                "pantalla de inicio → Widgets → ETF Widget, y escribe el ISIN del fondo.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(onClick = {
            scope.launch {
                msg = "Actualizando…"
                QuoteUpdater.refresh(context)
                msg = "Listo"
            }
        }) {
            Text("Refrescar todos los widgets")
        }
        if (msg.isNotEmpty()) {
            Text(msg, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
