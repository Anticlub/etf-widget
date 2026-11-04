# ETF Widget · Plan de desarrollo

Widget de Android que muestra la cotización de un ETF en la pantalla de inicio.
Uso personal (un compañero) + proyecto de portfolio en GitHub.

---

## 1. Qué quiero hacer

- Un **widget** en la pantalla de inicio con la cotización del ETF.
- Una **app mínima** solo para configurar el ticker y probar que funciona.
- **Sin registro, sin backend y sin API key.** Todo en local.
- Se distribuye como **APK** directamente, sin Play Store.

### Datos del ETF

| Campo | Valor |
|---|---|
| Fondo | iShares Core S&P 500 UCITS ETF (Acc) |
| ISIN | `IE00B5BMR087` |
| Ticker | `SXR8` (Xetra, en EUR) |
| Símbolo para la API | `SXR8.DE` |

El sufijo indica la bolsa: `.DE` Xetra, `.AS` Amsterdam, `.L` Londres, `.MI` Milán.

### Qué muestra el widget

Un **único widget** que se pinta distinto según el tamaño al que el usuario lo estire
(Glance `SizeMode.Responsive`). Mismo dato, misma lógica, solo cambia el layout:

| Tamaño | Contenido |
|---|---|
| **Pequeño** (~2×1) | Ticker + precio + % del día coloreado |
| **Mediano** (~4×2) | Lo anterior + **sparkline del día** + hora + botón ↻ |
| **Grande** (~4×3) | Lo anterior + precio en grande, máx/mín del día y cierre anterior |

Elementos comunes:

- Ticker o nombre del ETF
- Precio actual con su divisa
- Variación del día en € y en %, en verde o rojo
- Hora de la última actualización, o "Cerrado" fuera de horario
- Botón ↻ para refrescar al momento
- Al tocarlo, abre la app

### Limitación asumida

No es tiempo real. Android limita el trabajo en segundo plano: el refresco automático será
**cada 15 min** (el mínimo de WorkManager) más el botón manual. Los datos gratuitos ya van
con ~15 min de retraso, así que para un ETF de largo plazo es suficiente.

---

## 2. Cómo lo quiero hacer

### Stack

| Pieza | Tecnología |
|---|---|
| Lenguaje | Kotlin |
| UI de la app | Jetpack Compose + Material 3 |
| Widget | **Glance** (`androidx.glance:glance-appwidget`) |
| Refresco periódico | WorkManager |
| Persistencia | DataStore Preferences |
| Red | `HttpURLConnection` + `org.json` (sin librerías extra para empezar) |
| Datos | Endpoint de gráfico de Yahoo Finance (no oficial, sin key) |

### Arquitectura

```
UI (MainActivity)  ─┐
Widget (Glance)    ─┼─► QuoteUpdater ─► QuoteRepository (interfaz) ─► YahooQuoteRepository
Worker (15 min)    ─┘         │
                              └─► QuoteStore (DataStore) ◄── el widget pinta desde aquí
```

Reglas:
- **El widget nunca hace peticiones de red al pintarse.** Lee lo último guardado en DataStore.
- Quien actualiza (worker, botón ↻ o app) hace: *pedir → guardar → `updateAll()` del widget*.
- El proveedor de datos va detrás de una **interfaz**, para poder cambiarlo si Yahoo falla.

### Paquetes

```
dev.<tunombre>.etfwidget
├── MainActivity.kt
├── data/      Quote, QuoteRepository, YahooQuoteRepository, QuoteStore, QuoteUpdater
├── widget/    QuoteWidget, QuoteWidgetReceiver, RefreshAction
└── work/      QuoteWorker
```

### Git

- Un commit por fase como mínimo, siempre con algo que compile y funcione.
- Mensajes estilo *Conventional Commits*: `feat:`, `fix:`, `chore:`, `docs:`, `refactor:`, `test:`.
- Opcional: una rama por fase (`feat/widget-glance`) y merge a `main` al terminarla.

---

## 3. Fases

Cada fase tiene: objetivo, pasos, pistas, cómo probarlo y commit.

### Fase 0 · Proyecto y repositorio

**Objetivo:** proyecto vacío que arranca y está en GitHub.

- [ ] Android Studio → *New Project* → **Empty Activity** (Compose). minSdk 26.
- [ ] Revisar que el `.gitignore` incluye `local.properties`, `build/`, `.idea/`, `*.jks`.
- [ ] `git init`, crear el repo en GitHub y subirlo.

**Probar:** la app arranca y muestra el "Hello Android".
**Commit:** `chore: proyecto inicial`

---

### Fase 1 · Obtener la cotización

**Objetivo:** pedir el precio de `SXR8.DE` y verlo en pantalla o en Logcat.

- [ ] Añadir el permiso `INTERNET` al manifest.
- [ ] Crear `data class Quote(symbol, name, currency, price, previousClose, marketTime, isMarketOpen)`
      con propiedades calculadas `change` y `changePct`.
- [ ] Crear `interface QuoteRepository { suspend fun fetch(symbol: String): Quote }`.
- [ ] Implementar `YahooQuoteRepository` en `Dispatchers.IO`.

**Pistas:**
- URL: `https://query1.finance.yahoo.com/v8/finance/chart/SXR8.DE?interval=5m&range=1d`
- Pon una cabecera `User-Agent` (p. ej. `Mozilla/5.0`) o puede responder con error.
- Prueba antes la URL en el navegador para ver el JSON.
- Campos útiles dentro de `chart.result[0].meta`:
  - `regularMarketPrice` → precio
  - `chartPreviousClose` → cierre anterior (para el % del día)
  - `currency`, `symbol`, `shortName` / `longName`
  - `regularMarketTime` → epoch en segundos
  - `currentTradingPeriod.regular.start` / `.end` → para saber si el mercado está abierto
- Usa `runCatching` para no reventar sin red.

**Probar:** un botón temporal que llame a `fetch` y muestre el precio. Compáralo con el de tu bróker.
**Commit:** `feat: repositorio de cotizaciones con Yahoo Finance`

---

### Fase 2 · Guardar en local

**Objetivo:** que el ticker elegido y la última cotización sobrevivan al cerrar la app.

- [ ] Añadir `androidx.datastore:datastore-preferences`.
- [ ] Crear `QuoteStore` con:
  - `ticker()` / `setTicker()` (por defecto `SXR8.DE`)
  - `saveQuote()` / `lastQuote()`
  - `saveError()` / `lastError()`

**Pistas:** `preferencesDataStore("nombre")` como extensión de `Context`; claves con
`stringPreferencesKey`, `doublePreferencesKey`, etc.; lectura puntual con `data.first()`.

**Probar:** cargar el precio, matar la app, volver a abrir y ver que sigue ahí sin red.
**Commit:** `feat: persistencia con DataStore`

---

### Fase 3 · Pantalla de configuración

**Objetivo:** una pantalla Compose sencilla para elegir y probar el ticker.

- [ ] Título y texto de ayuda con los sufijos de bolsa.
- [ ] `OutlinedTextField` con el ticker y un botón **Guardar y probar**.
- [ ] Mostrar el resultado (nombre, precio, %) o el error.
- [ ] Estado de carga mientras pide los datos.

**Probar:** con `SXR8.DE` sale el precio; con un ticker inventado sale un error claro.
**Commit:** `feat: pantalla de configuración`

---

### Fase 4 · Widget estático

**Objetivo:** que aparezca un widget en la pantalla de inicio, aunque sea con texto fijo.

- [ ] Añadir `androidx.glance:glance-appwidget`.
- [ ] Clase `QuoteWidget : GlanceAppWidget` con `provideGlance` → `provideContent { ... }`.
- [ ] Clase `QuoteWidgetReceiver : GlanceAppWidgetReceiver`.
- [ ] Registrar el receiver en el manifest con la acción `APPWIDGET_UPDATE` y el `meta-data`.
- [ ] Crear `res/xml/quote_widget_info.xml` (`appwidget-provider`) con tamaños mínimos,
      `targetCellWidth/Height`, `resizeMode` y `updatePeriodMillis="0"`.

**Pistas:** `initialLayout="@layout/glance_default_loading_layout"` lo da Glance.
En Glance se usa `GlanceModifier`, no `Modifier`, y sus propios `Column`, `Row` y `Text`
(paquete `androidx.glance.*`, cuidado con los imports de Compose).

**Probar:** mantener pulsado el escritorio → Widgets → aparece y se añade.
**Commit:** `feat: widget Glance básico`

---

### Fase 5 · Widget con datos reales

**Objetivo:** el widget pinta la cotización guardada.

- [ ] Crear `QuoteUpdater.refresh(context)`: pedir → guardar (o guardar error) →
      `QuoteWidget().updateAll(context)`.
- [ ] En `provideGlance`, leer de `QuoteStore` **antes** de `provideContent`.
- [ ] Diseño: ticker pequeño, precio grande, variación coloreada y hora.
- [ ] Estado vacío: "Pulsa ↻ para cargar".

**Pistas:** `GlanceTheme { }` da colores dinámicos y modo oscuro automático
(`GlanceTheme.colors.widgetBackground`, `onSurface`…). Para verde y rojo fijos usa
`ColorProvider(Color(...))`. Formato de moneda: `NumberFormat.getCurrencyInstance(Locale("es","ES"))`.

**Probar:** cambiar el ticker desde la app y ver que el widget se actualiza solo.
**Commit:** `feat: widget muestra la cotización`

---

### Fase 6 · Botón de refresco y abrir la app

**Objetivo:** interacción con el widget.

- [ ] `RefreshAction : ActionCallback` que llama a `QuoteUpdater.refresh`.
- [ ] Botón ↻ con `GlanceModifier.clickable(actionRunCallback<RefreshAction>())`.
- [ ] Todo el widget con `clickable(actionStartActivity<MainActivity>())`.

**Probar:** pulsar ↻ y ver cambiar la hora; tocar el resto y que se abra la app.
**Commit:** `feat: refresco manual desde el widget`

---

### Fase 7 · Refresco automático

**Objetivo:** que se actualice solo cada 15 min.

- [ ] Añadir `androidx.work:work-runtime-ktx`.
- [ ] `QuoteWorker : CoroutineWorker` que llama a `QuoteUpdater.refresh`;
      `Result.retry()` si falla.
- [ ] `schedule()` con `PeriodicWorkRequestBuilder(15, MINUTES)`, restricción de red y
      `enqueueUniquePeriodicWork(..., KEEP, ...)`.
- [ ] Programarlo en `onEnabled` del receiver y al abrir la app; cancelarlo en `onDisabled`.

**Probar:** *App Inspection → Background Task Inspector* en Android Studio para ver el worker,
o dejarlo un rato y mirar la hora del widget.
**Commit:** `feat: refresco periódico con WorkManager`

---

### Fase 7.5 · Tamaños responsive y sparkline

**Objetivo:** el toque visual para el portfolio. El widget se adapta al tamaño y el mediano
muestra una mini-gráfica del día. Es la parte con más gancho y no cambia la arquitectura.

- [ ] Guardar la serie del día en `Quote`: `data class Quote(..., intraday: List<Double>)`
      con los `close` del JSON (ya los pides con `interval=5m`).
- [ ] En el parseo, leer `chart.result[0].indicators.quote[0].close` (y `timestamp`),
      descartando los `null` de tramos sin negociación.
- [ ] Dibujar el sparkline en un `Bitmap` con un `Canvas` de Android (`drawPath`),
      coloreado según suba o baje el día. Guardarlo o pasarlo al widget y mostrarlo con
      `Image(ImageProvider(bitmap), ...)`.
- [ ] Definir los layouts por tamaño con `SizeMode.Responsive(setOf(SMALL, MEDIUM, LARGE))`
      y un `@Composable` por tamaño (pequeño, mediano con sparkline, grande con máx/mín).

**Pistas:** Glance **no** tiene `Canvas` propio, por eso el sparkline va por `Bitmap` de
Android y luego `Image`. Mantén el dibujo en una función pura (recibe `List<Double>` y
tamaño, devuelve `Bitmap`) para poder testearla y no ensuciar el widget. Con `SizeMode.Responsive`,
`LocalSize.current` te da el tamaño real para elegir layout.

**Probar:** añadir el widget pequeño y estirarlo; ver que aparece la gráfica en el mediano
y los máx/mín en el grande.
**Commit:** `feat: tamaños responsive y sparkline del día`

---

### Fase 8 · Pulido

- [ ] Mostrar "Cerrado" fuera del horario de Xetra.
- [ ] Si falla la red, seguir mostrando lo último con un aviso "sin conexión".
- [ ] Revisar modo oscuro y distintos tamaños de widget.
- [ ] Icono propio de la app.
- [ ] Pedir al compañero que quite la optimización de batería si su móvil es Xiaomi, Huawei o Samsung.

**Commit:** `fix:` / `refactor:` según toque.

---

### Fase 9 · Entrega y portfolio

- [ ] **README**: qué hace, capturas o GIF del widget, stack y sección de *decisiones técnicas*
      (por qué no es tiempo real, por qué el widget no hace red, por qué la interfaz del repositorio).
- [ ] Generar **APK firmado**: *Build → Generate Signed App Bundle / APK*.
      El `.jks` **nunca** se sube a git.
- [ ] Pasarle el APK al compañero (o usar Firebase App Distribution para enviarle actualizaciones).
- [ ] Crear un *release* `v1.0` en GitHub con el APK adjunto.

**Commit:** `docs: README con capturas y decisiones técnicas`

---

## 4. Extras que sí vale la pena hacer

Suman en el portfolio y encajan en la arquitectura sin inflarla:

- [ ] **Tests unitarios** del parseo del JSON, del cálculo del % y del dibujo del sparkline.
      Separa cada uno en una función pura para poder testearlo.
- [ ] **GitHub Actions** que compile y pase los tests en cada push.

## 5. Próximos pasos (no para la v1)

Mencionarlos en el README como evolución demuestra criterio y saber dónde parar. No los
implementes de inicio: para un widget de un solo ETF son sobre-ingeniería.

- [ ] **Varios ETFs**: cada widget elige el suyo con una actividad de configuración de Glance.
- [ ] Introducir el **ISIN** y resolverlo a ticker con el buscador de Yahoo u OpenFIGI.
- [ ] Migrar la red a Retrofit/Ktor y añadir Hilt para la inyección de dependencias.
