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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
    val repository = remember { TradegateQuoteRepository() }
    var result by remember { mutableStateOf("Pulsa el botón") }

    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Button(onClick = {
            scope.launch {
                result = try {
                    val q = repository.fetch("IE00B5BMR087")
                    "${q.name}\n${q.price} ${q.currency}\n${q.changePct}%"
                } catch (e: Exception) {
                    "Error: ${e.message}"
                }
            }
        }) {
            Text("Cargar cotización")
        }
        Text(result)
    }
}