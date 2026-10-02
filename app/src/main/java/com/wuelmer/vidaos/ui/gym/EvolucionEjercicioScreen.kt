package com.wuelmer.vidaos.ui.gym

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wuelmer.vidaos.VidaOSApplication
import com.wuelmer.vidaos.ui.theme.ColorGasto
import com.wuelmer.vidaos.ui.theme.ColorIngreso
import com.wuelmer.vidaos.ui.theme.TextoSuave
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun EvolucionEjercicioRoute(
    ejercicioId: Long,
    onBackClick: () -> Unit,
    onSesionClick: (sesionId: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val application = LocalContext.current.applicationContext as VidaOSApplication
    val factory = remember(ejercicioId) {
        viewModelFactory {
            initializer {
                EvolucionEjercicioViewModel(
                    ejercicioId = ejercicioId,
                    gymDao = application.database.gymDao(),
                    preferencias = application.preferencias
                )
            }
        }
    }
    val viewModel: EvolucionEjercicioViewModel = viewModel(key = "evolucion_$ejercicioId", factory = factory)
    val uiState by viewModel.uiState.collectAsState()
    EvolucionEjercicioScreen(uiState, onBackClick, onSesionClick, modifier)
}

private val FORMATO_MES = DateTimeFormatter.ofPattern("MMM yyyy", Locale.forLanguageTag("es-ES"))
private val FORMATO_DIA = DateTimeFormatter.ofPattern("d 'de' MMMM yyyy", Locale.forLanguageTag("es-ES"))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvolucionEjercicioScreen(
    uiState: EvolucionEjercicioUiState?,
    onBackClick: () -> Unit,
    onSesionClick: (sesionId: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(uiState?.ejercicio?.nombre.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { innerPadding ->
        val evolucion = uiState?.evolucion
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when {
                uiState == null -> Text("Cargando...", color = TextoSuave)
                evolucion == null || evolucion.puntos.isEmpty() ->
                    Text("Aún no hay registros de este ejercicio.", color = TextoSuave)
                else -> {
                    TarjetaEvolucion(evolucion)
                    HistorialEvolucion(evolucion, onSesionClick)
                }
            }
        }
    }
}

@Composable
private fun TarjetaEvolucion(evolucion: EvolucionEjercicio) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "EVOLUCIÓN",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextoSuave
            )
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 8.dp)) {
                Text(
                    text = "${formatearPeso(evolucion.actual)} ${evolucion.unidadTexto}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                evolucion.cambio?.let { cambio ->
                    Text(
                        text = textoCambio(cambio, evolucion.unidadTexto),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            cambio > 0 -> ColorIngreso
                            cambio < 0 -> ColorGasto
                            else -> TextoSuave
                        },
                        modifier = Modifier.padding(start = 10.dp, bottom = 4.dp)
                    )
                }
            }
            Text(
                text = "${evolucion.metrica} · desde ${evolucion.primero?.fecha?.format(FORMATO_MES).orEmpty()}",
                style = MaterialTheme.typography.bodySmall,
                color = TextoSuave
            )
            GraficoLinea(
                valores = evolucion.puntos.map { it.valor },
                etiquetaInicio = evolucion.puntos.first().fecha.format(FORMATO_MES),
                etiquetaFin = evolucion.puntos.last().fecha.format(FORMATO_MES),
                modifier = Modifier.fillMaxWidth().height(170.dp).padding(top = 12.dp)
            )
        }
    }
}

// Línea con área sombreada; los círculos marcan cada vez que el valor cambió (los "escalones").
@Composable
private fun GraficoLinea(
    valores: List<Double>,
    etiquetaInicio: String,
    etiquetaFin: String,
    modifier: Modifier = Modifier
) {
    val colorLinea = MaterialTheme.colorScheme.primary
    val colorGuia = MaterialTheme.colorScheme.outline
    val colorFondo = MaterialTheme.colorScheme.surface
    val medidor = rememberTextMeasurer()
    val estiloEje = TextStyle(fontSize = 10.sp, color = TextoSuave)

    Canvas(modifier = modifier) {
        val minimo = valores.min()
        val maximo = valores.max()
        // Con un solo valor (o todos iguales) se abre un margen para no dividir por cero.
        val margen = if (maximo == minimo) maxOf(1.0, maximo * 0.1) else (maximo - minimo) * 0.15
        val bajo = minimo - margen
        val alto = maximo + margen

        val izquierda = 34.dp.toPx()
        val abajo = size.height - 18.dp.toPx()
        val arriba = 6.dp.toPx()
        val ancho = size.width - izquierda - 8.dp.toPx()
        fun x(i: Int) = izquierda + if (valores.size == 1) ancho / 2 else ancho * i / (valores.size - 1)
        fun y(v: Double) = (abajo - (v - bajo) / (alto - bajo) * (abajo - arriba)).toFloat()

        // Guías horizontales con su valor.
        (0..3).forEach { k ->
            val v = bajo + (alto - bajo) * k / 3
            drawLine(colorGuia, Offset(izquierda, y(v)), Offset(size.width, y(v)), strokeWidth = 1.dp.toPx())
            val texto = medidor.measure(formatearPeso(Math.round(v * 10) / 10.0), estiloEje)
            drawText(texto, topLeft = Offset(0f, y(v) - texto.size.height / 2))
        }

        val linea = Path().apply {
            valores.forEachIndexed { i, v -> if (i == 0) moveTo(x(i), y(v)) else lineTo(x(i), y(v)) }
        }
        val area = Path().apply {
            addPath(linea)
            lineTo(x(valores.lastIndex), abajo)
            lineTo(x(0), abajo)
            close()
        }
        drawPath(area, colorLinea.copy(alpha = 0.12f))
        drawPath(linea, colorLinea, style = Stroke(width = 3.dp.toPx(), join = StrokeJoin.Round))

        valores.forEachIndexed { i, v ->
            if (i == 0 || v != valores[i - 1]) {
                drawCircle(colorFondo, radius = 4.5.dp.toPx(), center = Offset(x(i), y(v)))
                drawCircle(colorLinea, radius = 4.5.dp.toPx(), center = Offset(x(i), y(v)), style = Stroke(2.5.dp.toPx()))
            }
        }

        val inicio = medidor.measure(etiquetaInicio, estiloEje)
        val fin = medidor.measure(etiquetaFin, estiloEje)
        drawText(inicio, topLeft = Offset(izquierda, size.height - inicio.size.height))
        if (valores.size > 1 && etiquetaFin != etiquetaInicio) {
            drawText(fin, topLeft = Offset(size.width - fin.size.width, size.height - fin.size.height))
        }
    }
}

// Lista de sesiones (la más reciente arriba); tocar una abre su detalle.
@Composable
private fun HistorialEvolucion(evolucion: EvolucionEjercicio, onSesionClick: (Long) -> Unit) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                text = "POR SESIÓN",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextoSuave,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
            )
            evolucion.puntos.asReversed().forEach { punto ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSesionClick(punto.sesionId) }
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = punto.fecha.format(FORMATO_DIA),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${formatearPeso(punto.valor)} ${evolucion.unidadTexto}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Box(modifier = Modifier.padding(start = 6.dp)) {
                        Text("›", color = TextoSuave)
                    }
                }
            }
        }
    }
}
