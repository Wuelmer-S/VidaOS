package com.wuelmer.vidaos.ui.gym

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wuelmer.vidaos.ui.theme.ColorDestacado
import com.wuelmer.vidaos.ui.theme.Indigo400
import com.wuelmer.vidaos.ui.theme.Indigo600
import com.wuelmer.vidaos.ui.theme.TextoSuave
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun GymRoute(
    onIniciarSesion: (diaId: Long) -> Unit,
    onSesionClick: (sesionId: Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GymViewModel = viewModel(factory = GymViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    GymScreen(
        uiState = uiState,
        onIniciarSesion = onIniciarSesion,
        onSesionClick = onSesionClick,
        modifier = modifier
    )
}

@Composable
fun GymScreen(
    uiState: GymUiState,
    onIniciarSesion: (diaId: Long) -> Unit,
    onSesionClick: (sesionId: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val formatoFecha = remember {
        DateTimeFormatter.ofPattern("d 'de' MMMM", Locale.forLanguageTag("es-ES"))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        uiState.progreso?.let { TarjetaRacha(it) }

        Text(
            text = "Nueva sesión",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium
        )

        uiState.dias.forEach { dia ->
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onIniciarSesion(dia.id) }
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = dia.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    val enCurso = dia.id in uiState.diasConBorrador
                    Text(
                        text = if (enCurso) "Sesión en curso · toca para continuar" else "Toca para iniciar la sesión",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (enCurso) MaterialTheme.colorScheme.primary else TextoSuave,
                        fontWeight = if (enCurso) FontWeight.Medium else null
                    )
                }
            }
        }

        Text(
            text = "Últimas sesiones",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        if (uiState.ultimasSesiones.isEmpty()) {
            Text(
                text = "Aún no has registrado sesiones.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextoSuave
            )
        }
        uiState.ultimasSesiones.forEach { sesion ->
            val nombreDia = uiState.dias.firstOrNull { it.id == sesion.diaRutinaId }?.nombre.orEmpty()
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSesionClick(sesion.id) }
            ) {
                Text(
                    text = "$nombreDia · ${sesion.fecha.format(formatoFecha)}",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(18.dp)
                )
            }
        }
    }
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
