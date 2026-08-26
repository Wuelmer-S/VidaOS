package com.wuelmer.vidaos.ui.registrargasto

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wuelmer.vidaos.data.Categoria
import com.wuelmer.vidaos.data.OrigenPago
import com.wuelmer.vidaos.data.TipoCategoria
import com.wuelmer.vidaos.data.TipoMovimiento
import com.wuelmer.vidaos.ui.categorias.AgregarCategoriaDialog
import com.wuelmer.vidaos.ui.theme.ColorGasto
import com.wuelmer.vidaos.ui.theme.ColorIngreso
import com.wuelmer.vidaos.ui.theme.TextoSuave
import com.wuelmer.vidaos.ui.theme.VidaOSTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun RegistrarGastoRoute(
    modifier: Modifier = Modifier,
    viewModel: RegistrarGastoViewModel = viewModel(factory = RegistrarGastoViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.guardadoExitoso) {
        if (uiState.guardadoExitoso) {
            snackbarHostState.showSnackbar("Movimiento guardado")
            viewModel.onGuardadoExitosoConsumido()
        }
    }

    Column(modifier = modifier) {
        SnackbarHost(hostState = snackbarHostState)
        RegistrarGastoScreen(
            uiState = uiState,
            onMontoChange = viewModel::onMontoChange,
            onDescripcionChange = viewModel::onDescripcionChange,
            onFechaChange = viewModel::onFechaChange,
            onOrigenChange = viewModel::onOrigenChange,
            onTipoChange = viewModel::onTipoChange,
            onCategoriaChange = viewModel::onCategoriaChange,
            onAgregarCategoria = viewModel::agregarCategoria,
            onGuardarClick = viewModel::guardar
        )
    }
}

@Composable
fun RegistrarGastoScreen(
    uiState: RegistrarGastoUiState,
    onMontoChange: (String) -> Unit,
    onDescripcionChange: (String) -> Unit,
    onFechaChange: (LocalDate) -> Unit,
    onOrigenChange: (OrigenPago) -> Unit,
    onTipoChange: (TipoMovimiento) -> Unit,
    onCategoriaChange: (Long) -> Unit,
    onAgregarCategoria: (nombre: String, tipo: TipoCategoria) -> Unit = { _, _ -> },
    onGuardarClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mesFormateado = remember {
        LocalDate.now()
            .format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("es-ES")))
            .replaceFirstChar { it.uppercase() }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp, 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Nuevo movimiento",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = mesFormateado,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoSuave
                    )
                }

                TipoSelector(tipoSeleccionado = uiState.tipo, onTipoChange = onTipoChange)

                OutlinedTextField(
                    value = uiState.monto,
                    onValueChange = onMontoChange,
                    label = { Text("Monto") },
                    prefix = { Text("$ ") },
                    textStyle = MaterialTheme.typography.headlineSmall,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = uiState.errorMonto,
                    supportingText = {
                        if (uiState.errorMonto) Text("Ingresa un monto válido")
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = uiState.descripcion,
                    onValueChange = onDescripcionChange,
                    label = { Text("Descripción (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                CategoriaSelector(
                    categorias = uiState.categorias,
                    tipoSeleccionado = uiState.tipo,
                    categoriaSeleccionadaId = uiState.categoriaId,
                    onCategoriaChange = onCategoriaChange,
                    onAgregarCategoria = onAgregarCategoria
                )

                FechaSelector(fecha = uiState.fecha, onFechaChange = onFechaChange)

                OrigenSelector(origenSeleccionado = uiState.origen, onOrigenChange = onOrigenChange)

                Button(
                    onClick = onGuardarClick,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Text("Guardar movimiento")
                }
            }
        }
    }
}

@Composable
private fun colorDeTipo(tipo: TipoMovimiento): Color = when (tipo) {
    TipoMovimiento.GASTO -> ColorGasto
    TipoMovimiento.INGRESO -> ColorIngreso
    TipoMovimiento.PAGO_TARJETA, TipoMovimiento.INTERNO -> MaterialTheme.colorScheme.primary
}

@Composable
private fun TipoSelector(
    tipoSeleccionado: TipoMovimiento,
    onTipoChange: (TipoMovimiento) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TipoMovimiento.entries.forEach { tipo ->
            val seleccionado = tipo == tipoSeleccionado
            FilterChip(
                selected = seleccionado,
                onClick = { onTipoChange(tipo) },
                label = { Text(tipo.etiqueta()) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = colorDeTipo(tipo),
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}

private fun TipoMovimiento.etiqueta(): String = when (this) {
    TipoMovimiento.GASTO -> "Gasto"
    TipoMovimiento.INGRESO -> "Ingreso"
    TipoMovimiento.PAGO_TARJETA -> "Pago tarjeta"
    TipoMovimiento.INTERNO -> "Interno"
}

@Composable
private fun CategoriaSelector(
    categorias: List<Categoria>,
    tipoSeleccionado: TipoMovimiento,
    categoriaSeleccionadaId: Long?,
    onCategoriaChange: (Long) -> Unit,
    onAgregarCategoria: (nombre: String, tipo: TipoCategoria) -> Unit
) {
    var mostrarDialogo by remember { mutableStateOf(false) }
    val tipoCategoria = tipoSeleccionado.categoriaCorrespondiente()
    val categoriasVisibles = if (tipoCategoria == null) {
        categorias
    } else {
        categorias.filter { it.tipo == tipoCategoria }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "Categoría", style = MaterialTheme.typography.labelMedium, color = TextoSuave)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categoriasVisibles.forEach { categoria ->
                val color = parseColorOrDefault(categoria.color, MaterialTheme.colorScheme.primary)
                val seleccionada = categoria.id == categoriaSeleccionadaId
                FilterChip(
                    selected = seleccionada,
                    onClick = { onCategoriaChange(categoria.id) },
                    label = { Text(categoria.nombre) },
                    leadingIcon = {
                        Surface(
                            shape = CircleShape,
                            color = color,
                            modifier = Modifier.size(8.dp)
                        ) {}
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = color,
                        selectedLabelColor = Color.White
                    )
                )
            }

            AssistChip(
                onClick = { mostrarDialogo = true },
                label = { Text("Nueva") },
                leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) }
            )
        }
    }

    if (mostrarDialogo) {
        AgregarCategoriaDialog(
            tipoFijo = tipoCategoria,
            onDismiss = { mostrarDialogo = false },
            onConfirmar = { nombre, tipo ->
                onAgregarCategoria(nombre, tipo)
                mostrarDialogo = false
            }
        )
    }
}

private fun parseColorOrDefault(hex: String?, default: Color): Color {
    if (hex.isNullOrBlank()) return default
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (error: IllegalArgumentException) {
        default
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrigenSelector(
    origenSeleccionado: OrigenPago,
    onOrigenChange: (OrigenPago) -> Unit
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        OrigenPago.entries.forEachIndexed { index, origen ->
            SegmentedButton(
                selected = origen == origenSeleccionado,
                onClick = { onOrigenChange(origen) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = OrigenPago.entries.size)
            ) {
                Text(origen.etiqueta())
            }
        }
    }
}

private fun OrigenPago.etiqueta(): String = when (this) {
    OrigenPago.DEBITO -> "Débito"
    OrigenPago.CREDITO -> "Crédito"
    OrigenPago.EFECTIVO -> "Efectivo"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FechaSelector(
    fecha: LocalDate,
    onFechaChange: (LocalDate) -> Unit
) {
    var mostrarDatePicker by rememberSaveable { mutableStateOf(false) }
    val formatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(text = "Fecha", style = MaterialTheme.typography.labelMedium, color = TextoSuave)
            Text(text = fecha.format(formatter), style = MaterialTheme.typography.bodyLarge)
        }
        TextButton(onClick = { mostrarDatePicker = true }) {
            Text("Cambiar fecha")
        }
    }

    if (mostrarDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = fecha.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { mostrarDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val nuevaFecha = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        onFechaChange(nuevaFecha)
                    }
                    mostrarDatePicker = false
                }) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RegistrarGastoScreenPreview() {
    VidaOSTheme {
        RegistrarGastoScreen(
            uiState = RegistrarGastoUiState(
                categorias = listOf(
                    Categoria(id = 1, nombre = "Comida", tipo = com.wuelmer.vidaos.data.TipoCategoria.GASTO, color = "#7B61FF"),
                    Categoria(id = 2, nombre = "Bencina", tipo = com.wuelmer.vidaos.data.TipoCategoria.GASTO, color = "#37D5D6")
                ),
                categoriaId = 1
            ),
            onMontoChange = {},
            onDescripcionChange = {},
            onFechaChange = {},
            onOrigenChange = {},
            onTipoChange = {},
            onCategoriaChange = {},
            onGuardarClick = {}
        )
    }
}
