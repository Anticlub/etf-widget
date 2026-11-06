package com.anticlub.etfwidget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.anticlub.etfwidget.data.QuoteStore
import com.anticlub.etfwidget.data.QuoteUpdater
import com.anticlub.etfwidget.ui.theme.ETFWidgetTheme
import com.anticlub.etfwidget.work.QuoteWorker
import kotlinx.coroutines.launch

// Pantalla que Android abre al colocar un widget: elige el ISIN de ESE widget.
class ConfigureActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Si el usuario sale sin guardar, el widget no se añade.
        setResult(Activity.RESULT_CANCELED)

        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        enableEdgeToEdge()
        setContent {
            ETFWidgetTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    ConfigureScreen(
                        modifier = Modifier.padding(padding),
                        onSave = { isin -> saveAndFinish(appWidgetId, isin) },
                    )
                }
            }
        }
    }

    private fun saveAndFinish(appWidgetId: Int, isin: String) {
        lifecycleScope.launch {
            val ctx = applicationContext
            QuoteStore(ctx).setWidgetIsin(appWidgetId, isin)   // este widget muestra este ISIN
            runCatching { QuoteUpdater.refreshOne(ctx, isin) }  // guarda ya su cotización
            // Repinta de forma fiable una vez el widget está enlazado (unos segundos después).
            QuoteWorker.runOnce(ctx)

            setResult(
                Activity.RESULT_OK,
                Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
            )
            finish()
        }
    }
}

@Composable
private fun ConfigureScreen(modifier: Modifier = Modifier, onSave: (String) -> Unit) {
    var isin by remember { mutableStateOf(QuoteStore.DEFAULT_ISIN) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Elige el ETF de este widget", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Introduce el ISIN del fondo (p. ej. IE00B5BMR087, el iShares Core S&P 500).",
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedTextField(
            value = isin,
            onValueChange = { isin = it.trim() },
            label = { Text("ISIN") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = { onSave(isin) }, enabled = isin.isNotBlank()) {
            Text("Añadir widget")
        }
    }
}
