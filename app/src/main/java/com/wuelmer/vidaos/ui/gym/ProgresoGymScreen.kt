package com.wuelmer.vidaos.ui.gym

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wuelmer.vidaos.data.UnidadPeso
import com.wuelmer.vidaos.ui.theme.ColorDestacado
import com.wuelmer.vidaos.ui.theme.Indigo400
import com.wuelmer.vidaos.ui.theme.Indigo600
import com.wuelmer.vidaos.ui.theme.TextoSuave
import java.time.DayOfWeek

@Composable
fun ProgresoGymRoute(
    onSesionClick: (sesionId: Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProgresoGymViewModel = viewModel(factory = ProgresoGymViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    ProgresoGymScreen(uiState = uiState, onSesionClick = onSesionClick, modifier = modifier)
}

// Pestaña de inicio de Gym. Aquí irán también el calendario de constancia y los logros.
@Composable
fun ProgresoGymScreen(
    uiState: ProgresoGymUiState?,
    onSesionClick: (sesionId: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (uiState != null) {
            TarjetaRacha(uiState.semana)
            TarjetaCalendario(uiState.calendario, onSesionClick)
            TarjetaResumen(uiState.resumen, uiState.unidad)
        }
    }
}

private val LETRAS_FILAS = listOf("L", "", "M", "", "V", "", "D")

// Calendario estilo GitHub: una columna por semana (lunes arriba), pintado el día con sesión.
@Composable
private fun TarjetaCalendario(semanas: List<SemanaCalendario>, onSesionClick: (Long) -> Unit) {
    val colorSesion = MaterialTheme.colorScheme.primary
    val colorDescanso = MaterialTheme.colorScheme.outline
    val diasEntrenados = semanas.sumOf { s -> s.dias.count { it.sesionId != null } }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "TUS ÚLTIMAS ${semanas.size} SEMANAS",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextoSuave
            )
            Text(
                text = if (diasEntrenados == 1) "1 día entrenado" else "$diasEntrenados días entrenados",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
            )

            // Rótulos de mes sobre las columnas.
            Row(modifier = Modifier.fillMaxWidth().padding(start = ANCHO_LETRAS)) {
                semanas.forEach { semana ->
                    Box(modifier = Modifier.weight(1f)) {
                        semana.etiquetaMes?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextoSuave,
                                softWrap = false,
                                overflow = TextOverflow.Visible
                            )
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(ESPACIO_CELDA),
                    modifier = Modifier.width(ANCHO_LETRAS)
                ) {
                    LETRAS_FILAS.forEach { letra ->
                        Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.aspectRatio(1f)) {
                            Text(letra, style = MaterialTheme.typography.labelSmall, color = TextoSuave)
                        }
                    }
                }
                semanas.forEach { semana ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(ESPACIO_CELDA),
                        modifier = Modifier.weight(1f).padding(horizontal = ESPACIO_CELDA / 2)
                    ) {
                        semana.dias.forEach { dia ->
                            CeldaDia(dia, colorSesion, colorDescanso, onSesionClick)
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Leyenda(colorDescanso, "descanso")
                Spacer(Modifier.width(12.dp))
                Leyenda(colorSesion, "fuiste")
                Spacer(Modifier.width(12.dp))
                Text("Toca un día para ver la sesión", style = MaterialTheme.typography.labelSmall, color = TextoSuave)
            }
        }
    }
}

// Números que solo suben (aunque se rompa la racha) y logros calculados al vuelo.
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TarjetaResumen(resumen: ResumenGym, unidad: UnidadPeso) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "RESUMEN",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextoSuave
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Estadistica("${resumen.sesionesTotales}", "sesiones totales", Modifier.weight(1f))
                Estadistica("🔥 ${resumen.mejorRacha}", "mejor racha (sem.)", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Estadistica(formatearVolumen(resumen.volumenTotalKg, unidad), "peso total levantado", Modifier.weight(1f))
                Estadistica(
                    formatearPeso(Math.round(resumen.promedioSemanal * 10) / 10.0),
                    "promedio / semana",
                    Modifier.weight(1f)
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                resumen.logros.forEach { InsigniaLogro(it) }
            }

            val proximo = resumen.proximoLogro
            if (proximo == null) {
                Text("¡Conseguiste todos los logros! 🏆", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            } else {
                Text(
                    text = "Próximo logro: ${proximo.titulo} · te " +
                        (if (proximo.faltan == 1) "falta 1" else "faltan ${proximo.faltan}"),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSuave
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.outline)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(proximo.fraccion)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }
}

@Composable
private fun Estadistica(valor: String, etiqueta: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.background)
            .padding(12.dp)
    ) {
        Text(valor, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(etiqueta, style = MaterialTheme.typography.labelSmall, color = TextoSuave)
    }
}

@Composable
private fun InsigniaLogro(logro: Logro) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(64.dp).alpha(if (logro.conseguido) 1f else 0.45f)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(50.dp)
                .then(if (logro.conseguido) Modifier.border(3.dp, ColorDestacado, CircleShape) else Modifier)
                .clip(CircleShape)
                .background(if (logro.conseguido) ColorDestacado.copy(alpha = 0.25f) else MaterialTheme.colorScheme.outline)
        ) {
            Text(logro.icono, fontSize = 22.sp)
        }
        Text(
            text = logro.titulo,
            style = MaterialTheme.typography.labelSmall,
            color = if (logro.conseguido) MaterialTheme.colorScheme.onSurface else TextoSuave,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

private val ANCHO_LETRAS = 14.dp
private val ESPACIO_CELDA = 3.dp

@Composable
private fun CeldaDia(dia: DiaCalendario, colorSesion: Color, colorDescanso: Color, onSesionClick: (Long) -> Unit) {
    val forma = RoundedCornerShape(3.dp)
    val color = when {
        dia.esFuturo -> Color.Transparent
        dia.sesionId != null -> colorSesion
        else -> colorDescanso
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .then(if (dia.esHoy) Modifier.border(2.dp, ColorDestacado, forma) else Modifier)
            .clip(forma)
            .background(color)
            .then(if (dia.sesionId != null) Modifier.clickable { onSesionClick(dia.sesionId) } else Modifier)
    )
}

@Composable
private fun Leyenda(color: Color, texto: String) {
    Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(color))
    Text(
        text = texto,
        style = MaterialTheme.typography.labelSmall,
        color = TextoSuave,
        modifier = Modifier.padding(start = 4.dp)
    )
}

private val LETRAS_DIA = listOf("L", "M", "M", "J", "V", "S", "D")

// Tarjeta principal de "Tu progreso": racha de semanas y avance de la semana actual.
@Composable
private fun TarjetaRacha(progreso: ProgresoSemana) {
    val blanco = Color.White
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(Indigo600, Indigo400)))
            .padding(18.dp)
    ) {
        Text(
            text = "TU PROGRESO",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = blanco.copy(alpha = 0.8f)
        )
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            Text(text = "🔥", fontSize = 40.sp)
            Column(modifier = Modifier.padding(start = 12.dp)) {
                if (progreso.racha > 0) {
                    Text(text = "${progreso.racha}", fontSize = 52.sp, fontWeight = FontWeight.ExtraBold, color = blanco, lineHeight = 52.sp)
                    Text(
                        text = (if (progreso.racha == 1) "semana seguida" else "semanas seguidas") + " cumpliendo ${progreso.meta}×",
                        style = MaterialTheme.typography.bodyMedium,
                        color = blanco.copy(alpha = 0.85f)
                    )
                } else {
                    Text(text = "Empieza tu racha", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = blanco)
                    Text(
                        text = "Llega a ${progreso.meta} sesiones en una semana",
                        style = MaterialTheme.typography.bodyMedium,
                        color = blanco.copy(alpha = 0.85f)
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(top = 18.dp)
        ) {
            DayOfWeek.entries.forEachIndexed { i, dia ->
                DiaSemana(letra = LETRAS_DIA[i], fuiste = dia in progreso.diasConSesion, esHoy = dia == progreso.hoy)
            }
        }

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(blanco.copy(alpha = 0.16f))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text("Esta semana", style = MaterialTheme.typography.bodyMedium, color = blanco)
            Text("${progreso.hechas} / ${progreso.meta}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = blanco)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .height(8.dp)
                .clip(RoundedCornerShape(50))
                .background(blanco.copy(alpha = 0.25f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((progreso.hechas.toFloat() / progreso.meta).coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(ColorDestacado)
            )
        }
        Text(
            text = mensajeSemana(progreso),
            style = MaterialTheme.typography.bodyMedium,
            color = blanco.copy(alpha = 0.9f),
            modifier = Modifier.padding(top = 10.dp)
        )
    }
}

@Composable
private fun DiaSemana(letra: String, fuiste: Boolean, esHoy: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .then(
                    if (esHoy) Modifier.drawBehind {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.9f),
                            radius = size.minDimension / 2 + 3.dp.toPx(),
                            style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
                        )
                    } else Modifier
                )
                .clip(CircleShape)
                .background(if (fuiste) Color.White else Color.White.copy(alpha = 0.18f))
        ) {
            if (fuiste) Text("✓", color = Indigo600, fontWeight = FontWeight.Bold)
        }
        Text(letra, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}
