package com.wuelmer.vidaos.ui.gym

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wuelmer.vidaos.VidaOSApplication
import com.wuelmer.vidaos.data.EjercicioGym
import com.wuelmer.vidaos.data.TipoEjercicio
import com.wuelmer.vidaos.data.UnidadPeso
import com.wuelmer.vidaos.ui.theme.ColorIngreso
import com.wuelmer.vidaos.ui.theme.TextoSuave
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun SesionGymRoute(
    diaId: Long,
    sesionId: Long?,
    onBackClick: () -> Unit,
    onGuardado: () -> Unit,
    modifier: Modifier = Modifier
) {
    val application = LocalContext.current.applicationContext as VidaOSApplication
    val factory = remember(diaId, sesionId) {
        viewModelFactory {
            initializer {
                SesionGymViewModel(
                    diaRutinaId = diaId,
                    sesionId = sesionId,
                    database = application.database,
                    preferencias = application.preferencias,
                    appScope = application.applicationScope
                )
            }
        }
    }
    val viewModel: SesionGymViewModel = viewModel(key = "sesion_gym_${diaId}_$sesionId", factory = factory)
    val uiState by viewModel.uiState.collectAsState()

    SesionGymScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onEjercicioElegido = viewModel::onEjercicioElegido,
        onRepsChange = viewModel::onRepsChange,
        onPesoChange = viewModel::onPesoChange,
        onSegundosChange = viewModel::onSegundosChange,
        onAgregarSerie = viewModel::onAgregarSerie,
        onEliminarSerie = viewModel::onEliminarSerie,
        onGuardarClick = { viewModel.guardar(onGuardado) },
        onDescartarClick = viewModel::descartarBorrador,
        onFechaChange = viewModel::onFechaChange,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SesionGymScreen(
    uiState: SesionGymUiState,
    onBackClick: () -> Unit,
    onEjercicioElegido: (rutinaEjercicioId: Long, ejercicioId: Long) -> Unit,
    onRepsChange: (rutinaEjercicioId: Long, indice: Int, valor: String) -> Unit,
    onPesoChange: (rutinaEjercicioId: Long, indice: Int, valor: String) -> Unit,
    onSegundosChange: (rutinaEjercicioId: Long, indice: Int, valor: String) -> Unit,
    onAgregarSerie: (rutinaEjercicioId: Long) -> Unit,
    onEliminarSerie: (rutinaEjercicioId: Long, indice: Int) -> Unit,
    onGuardarClick: () -> Unit,
    onDescartarClick: () -> Unit,
    onFechaChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    var confirmarDescarte by remember { mutableStateOf(false) }
    var elegirFecha by remember { mutableStateOf(false) }
    val fechaFormateada = remember(uiState.fecha) {
        uiState.fecha
            .format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale.forLanguageTag("es-ES")))
            .replaceFirstChar { it.uppercase() }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.editando) "Editar · ${uiState.nombreDia}" else uiState.nombreDia) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (uiState.cargando) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("Cargando...", color = TextoSuave)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { elegirFecha = true }
                    .padding(vertical = 4.dp)
            ) {
                Icon(
                    Icons.Filled.DateRange,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 6.dp)
                )
                Text(text = fechaFormateada, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = " · cambiar",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (uiState.recuperada) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
                    ) {
                        Text(
                            text = "Retomaste la sesión en curso.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { confirmarDescarte = true }) { Text("Descartar") }
                    }
                }
            }

            uiState.ejercicios.forEach { ejercicio ->
                EjercicioCard(
                    ejercicio = ejercicio,
                    mostrarErrores = uiState.mostrarErrores,
                    onEjercicioElegido = { onEjercicioElegido(ejercicio.rutinaEjercicioId, it) },
                    onRepsChange = { i, v -> onRepsChange(ejercicio.rutinaEjercicioId, i, v) },
                    onPesoChange = { i, v -> onPesoChange(ejercicio.rutinaEjercicioId, i, v) },
                    onSegundosChange = { i, v -> onSegundosChange(ejercicio.rutinaEjercicioId, i, v) },
                    onAgregarSerie = { onAgregarSerie(ejercicio.rutinaEjercicioId) },
                    onEliminarSerie = { i -> onEliminarSerie(ejercicio.rutinaEjercicioId, i) }
                )
            }

            if (uiState.errorSinSeries) {
                Text(
                    text = "Registra al menos una serie para guardar la sesión.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (uiState.mostrarErrores) {
                Text(
                    text = "Completa o vacía las series marcadas en rojo.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Button(
                onClick = onGuardarClick,
                enabled = !uiState.guardando,
                shape = RoundedCornerShape(50),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (uiState.editando) "Guardar cambios" else "Guardar sesión")
            }
            if (!uiState.editando) {
                Text(
                    text = "Lo que escribes se guarda solo; si sales, al volver sigue aquí.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSuave,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }

    if (elegirFecha) {
        SelectorFecha(
            fecha = uiState.fecha,
            onConfirmar = {
                elegirFecha = false
                onFechaChange(it)
            },
            onCancelar = { elegirFecha = false }
        )
    }

    if (confirmarDescarte) {
        AlertDialog(
            onDismissRequest = { confirmarDescarte = false },
            title = { Text("¿Descartar la sesión en curso?") },
            text = { Text("Se borrará lo que llevas escrito en este día. Las sesiones ya guardadas no se tocan.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarDescarte = false
                    onDescartarClick()
                }) { Text("Descartar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmarDescarte = false }) { Text("Cancelar") }
            }
        )
    }
}

// El DatePicker trabaja en milisegundos UTC; se convierte con epochDay para no correr la fecha por la zona horaria.
// No deja elegir fechas futuras. También lo usa el módulo Moto.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SelectorFecha(
    fecha: LocalDate,
    onConfirmar: (LocalDate) -> Unit,
    onCancelar: () -> Unit
) {
    val hoy = remember { LocalDate.now() }
    val estado = rememberDatePickerState(
        initialSelectedDateMillis = fecha.toEpochDay() * MILIS_POR_DIA,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis / MILIS_POR_DIA <= hoy.toEpochDay()
            override fun isSelectableYear(year: Int) = year <= hoy.year
        }
    )
    DatePickerDialog(
        onDismissRequest = onCancelar,
        confirmButton = {
            TextButton(onClick = {
                estado.selectedDateMillis?.let { onConfirmar(LocalDate.ofEpochDay(it / MILIS_POR_DIA)) } ?: onCancelar()
            }) { Text("Aceptar") }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    ) {
        DatePicker(state = estado)
    }
}

private const val MILIS_POR_DIA = 86_400_000L

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EjercicioCard(
    ejercicio: EjercicioSesionUi,
    mostrarErrores: Boolean,
    onEjercicioElegido: (Long) -> Unit,
    onRepsChange: (Int, String) -> Unit,
    onPesoChange: (Int, String) -> Unit,
    onSegundosChange: (Int, String) -> Unit,
    onAgregarSerie: () -> Unit,
    onEliminarSerie: (Int) -> Unit
) {
    val elegido = ejercicio.elegido

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = elegido.nombre,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = textoObjetivo(ejercicio),
                style = MaterialTheme.typography.bodySmall,
                color = TextoSuave
            )
            if (ejercicio.ultimaVez.isNotEmpty()) {
                Text(
                    text = "Última vez: ${textoUltimaVez(elegido, ejercicio.ultimaVez, ejercicio.unidad)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSuave
                )
            }
            ejercicio.sugerencia?.let { sugerencia ->
                val subir = sugerencia is Sugerencia.SubirPeso || sugerencia is Sugerencia.RangoCompletado
                Text(
                    text = textoSugerencia(sugerencia, ejercicio.unidad),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (subir) ColorIngreso else MaterialTheme.colorScheme.onSurface
                )
            }

            if (ejercicio.opciones.size > 1) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ejercicio.opciones.forEach { opcion ->
                        FilterChip(
                            selected = opcion.id == ejercicio.elegidoId,
                            onClick = { onEjercicioElegido(opcion.id) },
                            label = { Text(opcion.nombre) }
                        )
                    }
                }
            }

            ejercicio.inputs.forEachIndexed { indice, input ->
                key(input.id) {
                    SerieDeslizable(
                        puedeEliminar = ejercicio.inputs.size > 1,
                        onEliminar = { onEliminarSerie(indice) }
                    ) {
                        SerieFila(
                            indice = indice,
                            input = input,
                            ejercicio = elegido,
                            unidad = ejercicio.unidad,
                            error = mostrarErrores && input.evaluar(elegido.tipo) is ResultadoSerie.Invalida,
                            onRepsChange = { onRepsChange(indice, it) },
                            onPesoChange = { onPesoChange(indice, it) },
                            onSegundosChange = { onSegundosChange(indice, it) }
                        )
                    }
                }
            }

            TextButton(onClick = onAgregarSerie) {
                Text("+ Serie")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SerieDeslizable(
    puedeEliminar: Boolean,
    onEliminar: () -> Unit,
    content: @Composable () -> Unit
) {
    val estado = rememberSwipeToDismissBoxState()
    LaunchedEffect(estado.currentValue) {
        if (estado.currentValue == SwipeToDismissBoxValue.EndToStart) onEliminar()
    }

    SwipeToDismissBox(
        state = estado,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = puedeEliminar,
        backgroundContent = {
            val progreso = if (estado.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                estado.progress.coerceIn(0f, 1f)
            } else {
                0f
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.error.copy(alpha = progreso)),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier
                        .padding(end = 20.dp)
                        .alpha((progreso * 3f).coerceIn(0f, 1f))
                )
            }
        }
    ) {
        Surface(color = MaterialTheme.colorScheme.surface) { content() }
    }
}

@Composable
private fun SerieFila(
    indice: Int,
    input: SerieInput,
    ejercicio: EjercicioGym,
    unidad: UnidadPeso,
    error: Boolean,
    onRepsChange: (String) -> Unit,
    onPesoChange: (String) -> Unit,
    onSegundosChange: (String) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "${indice + 1}",
            style = MaterialTheme.typography.bodyMedium,
            color = TextoSuave,
            modifier = Modifier.width(20.dp)
        )
        if (ejercicio.tipo == TipoEjercicio.TIEMPO) {
            OutlinedTextField(
                value = input.segundos,
                onValueChange = onSegundosChange,
                label = { Text("Segundos") },
                singleLine = true,
                isError = error,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        } else {
            OutlinedTextField(
                value = input.reps,
                onValueChange = onRepsChange,
                label = { Text(if (ejercicio.unilateral) "Reps/lado" else "Reps") },
                singleLine = true,
                isError = error,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            if (ejercicio.tipo == TipoEjercicio.CON_PESO) {
                OutlinedTextField(
                    value = input.peso,
                    onValueChange = onPesoChange,
                    label = { Text("Peso (${unidad.simbolo})") },
                    singleLine = true,
                    isError = error,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private fun textoSugerencia(sugerencia: Sugerencia, unidad: UnidadPeso): String = when (sugerencia) {
    is Sugerencia.SubirPeso -> "Sube a ${formatearPeso(sugerencia.peso)} ${unidad.simbolo}"
    is Sugerencia.MantenerPeso -> "Mantén ${formatearPeso(sugerencia.peso)} ${unidad.simbolo}"
    Sugerencia.RangoCompletado -> "Rango completado"
}

private fun textoObjetivo(ejercicio: EjercicioSesionUi): String {
    val rango = if (ejercicio.objetivoMin == ejercicio.objetivoMax) {
        "${ejercicio.objetivoMin}"
    } else {
        "${ejercicio.objetivoMin}-${ejercicio.objetivoMax}"
    }
    val unidad = if (ejercicio.elegido.tipo == TipoEjercicio.TIEMPO) "s" else "reps"
    val lado = if (ejercicio.elegido.unilateral) " por lado" else ""
    val descanso = "${ejercicio.descansoSegundos / 60}:${"%02d".format(ejercicio.descansoSegundos % 60)}"
    return "${ejercicio.series} × $rango $unidad$lado · descanso $descanso"
}
