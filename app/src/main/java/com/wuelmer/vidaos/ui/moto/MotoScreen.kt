package com.wuelmer.vidaos.ui.moto

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wuelmer.vidaos.data.LecturaKm
import com.wuelmer.vidaos.ui.theme.ColorDestacado
import com.wuelmer.vidaos.ui.theme.ColorGasto
import com.wuelmer.vidaos.ui.theme.ColorIngreso
import com.wuelmer.vidaos.ui.theme.Indigo400
import com.wuelmer.vidaos.ui.theme.Indigo600
import com.wuelmer.vidaos.ui.theme.TextoSuave
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun MotoRoute(
    modifier: Modifier = Modifier,
    viewModel: MotoViewModel = viewModel(factory = MotoViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    MotoScreen(uiState = uiState, onAnotarKm = viewModel::anotarKm, modifier = modifier)
}

@Composable
fun MotoScreen(
    uiState: MotoUiState?,
    onAnotarKm: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var mostrarDialogoKm by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (uiState != null) {
            TarjetaTablero(uiState, onActualizarKm = { mostrarDialogoKm = true })
            TarjetaPlan(uiState.estados)
        }
    }

    if (mostrarDialogoKm && uiState != null) {
        DialogoKm(
            kmAnterior = uiState.ultimaLectura?.km,
            onDismiss = { mostrarDialogoKm = false },
            onGuardar = { km ->
                onAnotarKm(km)
                mostrarDialogoKm = false
            }
        )
    }
}

@Composable
private fun colorSemaforo(semaforo: Semaforo): Color = when (semaforo) {
    Semaforo.SIN_REGISTRO -> MaterialTheme.colorScheme.outline
    Semaforo.VERDE -> ColorIngreso
    Semaforo.AMARILLO -> ColorDestacado
    Semaforo.ROJO -> ColorGasto
}

private fun hace(fecha: LocalDate, hoy: LocalDate = LocalDate.now()): String =
    when (val dias = ChronoUnit.DAYS.between(fecha, hoy)) {
        0L -> "anotado hoy"
        1L -> "anotado ayer"
        else -> "anotado hace $dias días"
    }

// Tarjeta principal: lo que marca el odómetro y lo próximo que toca (como la racha en Gym).
@Composable
private fun TarjetaTablero(uiState: MotoUiState, onActualizarKm: () -> Unit) {
    val blanco = Color.White
    val lectura: LecturaKm? = uiState.ultimaLectura
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(Indigo600, Indigo400)))
            .padding(18.dp)
    ) {
        Text(
            text = "YAMAHA FZ 3.0",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = blanco.copy(alpha = 0.8f)
        )
        if (lectura == null) {
            Text(
                text = "¿Cuántos km marca hoy?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = blanco,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = "Con el km actual la app calcula cuánto falta para cada mantención.",
                style = MaterialTheme.typography.bodyMedium,
                color = blanco.copy(alpha = 0.85f)
            )
        } else {
            Text(
                text = formatearKm(lectura.km),
                fontSize = 44.sp,
                fontWeight = FontWeight.ExtraBold,
                color = blanco,
                lineHeight = 48.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
            val ritmo = uiState.ritmoKmDia?.let { " · ~${Math.round(it)} km/día" }.orEmpty()
            Text(
                text = hace(lectura.fecha) + ritmo,
                style = MaterialTheme.typography.bodyMedium,
                color = blanco.copy(alpha = 0.85f)
            )
        }

        uiState.masUrgente?.let { estado ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(blanco.copy(alpha = 0.16f))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text("LO PRÓXIMO", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = blanco.copy(alpha = 0.8f))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                ) {
                    Text(
                        text = "${estado.tipo.icono} ${estado.tipo.nombre}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = blanco,
                        modifier = Modifier.weight(1f)
                    )
                    Text(textoProximo(estado, uiState.ritmoKmDia), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = blanco)
                }
                BarraProgreso(
                    fraccion = estado.progreso ?: 0f,
                    color = if (estado.semaforo == Semaforo.ROJO) ColorGasto else ColorDestacado,
                    fondo = blanco.copy(alpha = 0.25f),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        Button(
            onClick = onActualizarKm,
            colors = ButtonDefaults.buttonColors(containerColor = blanco, contentColor = Indigo600),
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp)
        ) {
            Text(if (lectura == null) "Anotar km" else "Actualizar km", fontWeight = FontWeight.SemiBold)
        }
    }
}

// "faltan 420 km · ~9 días" cuando hay ritmo suficiente para estimar.
private fun textoProximo(estado: EstadoMantencion, ritmo: Double?): String {
    val base = describirRestante(estado)
    val km = estado.kmRestantes
    val estimado = if (estado.mandaKm && km != null) diasEstimados(km, ritmo) else null
    return if (estimado != null) "$base · ~$estimado días" else base
}

// Todas las mantenciones del plan, de la más urgente a la menos.
@Composable
private fun TarjetaPlan(estados: List<EstadoMantencion>) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "PLAN DE MANTENCIÓN",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextoSuave
            )
            if (estados.any { it.progreso == null }) {
                Text(
                    text = "Las que dicen \"Sin registro\" empiezan a contar cuando anotes la última vez que las hiciste.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSuave,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            estados.forEachIndexed { i, estado ->
                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                FilaMantencion(estado)
            }
        }
    }
}

@Composable
private fun FilaMantencion(estado: EstadoMantencion) {
    val color = colorSemaforo(estado.semaforo)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.18f))
        ) {
            Text(estado.tipo.icono, fontSize = 18.sp)
        }
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = estado.tipo.nombre,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f, fill = false)
                )
                EtiquetaEstado(estado.semaforo, color)
            }
            Text(
                text = describirIntervalo(estado.tipo.cadaKm, estado.tipo.cadaDias) + " · " + describirRestante(estado),
                style = MaterialTheme.typography.bodySmall,
                color = TextoSuave
            )
            if (estado.progreso != null) {
                BarraProgreso(
                    fraccion = estado.progreso,
                    color = color,
                    fondo = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun EtiquetaEstado(semaforo: Semaforo, color: Color) {
    Text(
        text = etiquetaSemaforo(semaforo),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = if (semaforo == Semaforo.SIN_REGISTRO) TextoSuave else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .padding(start = 8.dp)
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = if (semaforo == Semaforo.SIN_REGISTRO) 0.6f else 0.3f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

@Composable
private fun BarraProgreso(fraccion: Float, color: Color, fondo: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(50))
            .background(fondo)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraccion.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(RoundedCornerShape(50))
                .background(color)
        )
    }
}

// Teclado numérico directo: anotar el km tiene que tomar pocos segundos.
@Composable
private fun DialogoKm(kmAnterior: Int?, onDismiss: () -> Unit, onGuardar: (Int) -> Unit) {
    var texto by remember { mutableStateOf("") }
    val km = texto.toIntOrNull()
    // Bajar de km casi siempre es un error de tipeo (editar y borrar lecturas llega en el próximo corte).
    val menorQueAnterior = km != null && kmAnterior != null && km < kmAnterior
    val foco = remember { FocusRequester() }
    LaunchedEffect(Unit) { foco.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("¿Cuántos km marca?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = texto,
                    onValueChange = { nuevo -> texto = nuevo.filter(Char::isDigit).take(7) },
                    label = { Text("Kilómetros") },
                    singleLine = true,
                    isError = menorQueAnterior,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.focusRequester(foco)
                )
                when {
                    menorQueAnterior -> Text(
                        text = "Es menos que la última lectura (${formatearKm(kmAnterior!!)}).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    kmAnterior != null -> Text(
                        text = "Última lectura: ${formatearKm(kmAnterior)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoSuave
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { km?.let(onGuardar) }, enabled = km != null && !menorQueAnterior) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
