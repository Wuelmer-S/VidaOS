package com.wuelmer.vidaos.ui.moto

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.wuelmer.vidaos.data.AjusteTrabajo
import com.wuelmer.vidaos.data.HechoPor
import com.wuelmer.vidaos.data.LecturaKm
import com.wuelmer.vidaos.data.RegistroMantencion
import com.wuelmer.vidaos.data.TipoMantencion
import com.wuelmer.vidaos.data.Trabajo
import com.wuelmer.vidaos.ui.gym.SelectorFecha
import com.wuelmer.vidaos.ui.theme.ColorGasto
import com.wuelmer.vidaos.ui.theme.TextoSuave
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val formatoFecha = DateTimeFormatter.ofPattern("d 'de' MMMM yyyy", Locale.forLanguageTag("es-ES"))

fun formatearFecha(fecha: LocalDate): String =
    if (fecha == LocalDate.now()) "Hoy" else fecha.format(formatoFecha)

private fun soloDigitos(texto: String) = texto.filter(Char::isDigit).take(7)

// Fila "Fecha: 2 de octubre 2026 · Cambiar" con el selector que no deja elegir fechas futuras.
@Composable
private fun CampoFecha(fecha: LocalDate, onCambiar: (LocalDate) -> Unit) {
    var elegir by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Fecha", style = MaterialTheme.typography.labelMedium, color = TextoSuave)
            Text(formatearFecha(fecha), style = MaterialTheme.typography.bodyLarge)
        }
        TextButton(onClick = { elegir = true }) { Text("Cambiar") }
    }
    if (elegir) {
        SelectorFecha(
            fecha = fecha,
            onConfirmar = {
                onCambiar(it)
                elegir = false
            },
            onCancelar = { elegir = false }
        )
    }
}

// Anotar o corregir una lectura del odómetro. Teclado numérico directo: tiene que tomar pocos segundos.
// kmAnterior: si el km nuevo es menor, se pide confirmar (casi siempre es un error de tipeo).
@Composable
fun DialogoLecturaKm(
    inicial: LecturaKm?,
    kmAnterior: Int?,
    onDismiss: () -> Unit,
    onGuardar: (fecha: LocalDate, km: Int) -> Unit,
    onEliminar: (() -> Unit)? = null
) {
    var texto by remember { mutableStateOf(inicial?.km?.toString().orEmpty()) }
    var fecha by remember { mutableStateOf(inicial?.fecha ?: LocalDate.now()) }
    val km = texto.toIntOrNull()?.takeIf { it > 0 }
    val menorQueAnterior = km != null && kmAnterior != null && km < kmAnterior
    val foco = remember { FocusRequester() }
    LaunchedEffect(Unit) { foco.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (inicial == null) "¿Cuántos km marca?" else "Editar lectura") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = texto,
                    onValueChange = { texto = soloDigitos(it) },
                    label = { Text("Kilómetros") },
                    singleLine = true,
                    isError = menorQueAnterior,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().focusRequester(foco)
                )
                when {
                    menorQueAnterior -> Text(
                        text = "Es menos que la última lectura (${formatearKm(kmAnterior!!)}). ¿Seguro?",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    kmAnterior != null -> Text(
                        text = "Última lectura: ${formatearKm(kmAnterior)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoSuave
                    )
                }
                CampoFecha(fecha) { fecha = it }
            }
        },
        confirmButton = {
            TextButton(onClick = { km?.let { onGuardar(fecha, it) } }, enabled = km != null) {
                Text(if (menorQueAnterior) "Guardar igual" else "Guardar")
            }
        },
        dismissButton = {
            Row {
                if (onEliminar != null) {
                    TextButton(onClick = onEliminar) { Text("Eliminar", color = ColorGasto) }
                }
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        }
    )
}

// Registrar (inicial = null) o editar una vez que se hizo una mantención.
@Composable
fun DialogoRegistro(
    nombreTipo: String,
    inicial: RegistroMantencion?,
    kmSugerido: Int?,
    onDismiss: () -> Unit,
    onGuardar: (fecha: LocalDate, km: Int, hechoPor: HechoPor, notas: String?) -> Unit,
    onEliminar: (() -> Unit)? = null
) {
    var fecha by remember { mutableStateOf(inicial?.fecha ?: LocalDate.now()) }
    var kmTexto by remember { mutableStateOf((inicial?.km ?: kmSugerido)?.toString().orEmpty()) }
    var hechoPor by remember { mutableStateOf(inicial?.hechoPor ?: HechoPor.YO) }
    var notas by remember { mutableStateOf(inicial?.notas.orEmpty()) }
    val km = kmTexto.toIntOrNull()?.takeIf { it > 0 }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (inicial == null) nombreTipo else "Editar · $nombreTipo") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                CampoFecha(fecha) { fecha = it }
                OutlinedTextField(
                    value = kmTexto,
                    onValueChange = { kmTexto = soloDigitos(it) },
                    label = { Text("Km al hacerla") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = hechoPor == HechoPor.YO, onClick = { hechoPor = HechoPor.YO }, label = { Text("La hice yo") })
                    FilterChip(selected = hechoPor == HechoPor.TALLER, onClick = { hechoPor = HechoPor.TALLER }, label = { Text("Taller") })
                }
                OutlinedTextField(
                    value = notas,
                    onValueChange = { notas = it.take(200) },
                    label = { Text("Notas (opcional)") },
                    placeholder = { Text("Ej.: Motul 10W-40") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { km?.let { onGuardar(fecha, it, hechoPor, notas.trim().ifEmpty { null }) } },
                enabled = km != null
            ) { Text("Guardar") }
        },
        dismissButton = {
            Row {
                if (onEliminar != null) {
                    TextButton(onClick = onEliminar) { Text("Eliminar", color = ColorGasto) }
                }
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        }
    )
}

private val ICONOS = listOf("🔧", "🛢️", "🧴", "⛓️", "🌬️", "⚡", "🛑", "🛞", "🔋", "💡", "🧽", "🪛")

// Crear (inicial = null) o editar una mantención del plan.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DialogoTipo(
    inicial: TipoMantencion?,
    trabajos: List<Trabajo>,
    onDismiss: () -> Unit,
    onGuardar: (IntervaloValido, icono: String, trabajoId: Long?) -> Unit
) {
    val tiempoInicial = inicial?.cadaDias?.let(::descomponerDias)
    var nombre by remember { mutableStateOf(inicial?.nombre.orEmpty()) }
    var icono by remember { mutableStateOf(inicial?.icono ?: ICONOS.first()) }
    var kmTexto by remember { mutableStateOf(inicial?.cadaKm?.toString().orEmpty()) }
    var tiempoTexto by remember { mutableStateOf(tiempoInicial?.first?.toString().orEmpty()) }
    var unidad by remember { mutableStateOf(tiempoInicial?.second ?: UnidadTiempo.MESES) }
    var mostrarError by remember { mutableStateOf(false) }
    var trabajoId by remember { mutableStateOf(inicial?.trabajoId?.takeIf { id -> trabajos.any { it.id == id } }) }
    val resultado = validarTipo(nombre, kmTexto, tiempoTexto, unidad)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (inicial == null) "Nueva mantención" else "Editar mantención") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it.take(40) },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Ícono", style = MaterialTheme.typography.labelMedium, color = TextoSuave)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ICONOS.forEach { e ->
                        FilterChip(selected = icono == e, onClick = { icono = e }, label = { Text(e) })
                    }
                }
                Text(
                    text = "Vence por lo que ocurra primero. Deja vacío lo que no aplique.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSuave
                )
                OutlinedTextField(
                    value = kmTexto,
                    onValueChange = { kmTexto = soloDigitos(it) },
                    label = { Text("Cada cuántos km") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = tiempoTexto,
                    onValueChange = { tiempoTexto = soloDigitos(it).take(4) },
                    label = { Text("Cada cuánto tiempo") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    UnidadTiempo.entries.forEach { u ->
                        FilterChip(selected = unidad == u, onClick = { unidad = u }, label = { Text(u.etiqueta) })
                    }
                }
                if (trabajos.isNotEmpty()) {
                    Text("Ficha de trabajo", style = MaterialTheme.typography.labelMedium, color = TextoSuave)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(selected = trabajoId == null, onClick = { trabajoId = null }, label = { Text("Ninguna") })
                        trabajos.forEach { t ->
                            FilterChip(selected = trabajoId == t.id, onClick = { trabajoId = t.id }, label = { Text("${t.icono} ${t.nombre}") })
                        }
                    }
                }
                if (mostrarError) {
                    resultado.exceptionOrNull()?.let {
                        Text(it.message.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                resultado.onSuccess { onGuardar(it, icono, trabajoId) }.onFailure { mostrarError = true }
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}


// Crear (inicial = null) o editar una ficha de trabajo. Pasos y materiales: uno por línea.
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DialogoTrabajo(
    inicial: Trabajo?,
    onDismiss: () -> Unit,
    onGuardar: (nombre: String, icono: String, pasos: String, materiales: String) -> Unit
) {
    var nombre by remember { mutableStateOf(inicial?.nombre.orEmpty()) }
    var icono by remember { mutableStateOf(inicial?.icono ?: ICONOS.first()) }
    var materiales by remember { mutableStateOf(inicial?.materiales.orEmpty()) }
    var pasos by remember { mutableStateOf(inicial?.pasos.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (inicial == null) "Nueva ficha" else "Editar ficha") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it.take(40) },
                    label = { Text("Trabajo") },
                    placeholder = { Text("Ej.: Cambiar bujía") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ICONOS.forEach { e ->
                        FilterChip(selected = icono == e, onClick = { icono = e }, label = { Text(e) })
                    }
                }
                OutlinedTextField(
                    value = materiales,
                    onValueChange = { materiales = it.take(1000) },
                    label = { Text("Materiales (uno por línea)") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = pasos,
                    onValueChange = { pasos = it.take(3000) },
                    label = { Text("Pasos (uno por línea)") },
                    minLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onGuardar(nombre.trim(), icono, lineas(pasos).joinToString("\n"), lineas(materiales).joinToString("\n")) },
                enabled = nombre.isNotBlank()
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

// Crear (inicial = null) o editar un perno de la ficha: qué parte, con qué llave y a qué torque.
@Composable
fun DialogoAjuste(
    inicial: AjusteTrabajo?,
    onDismiss: () -> Unit,
    onGuardar: (parte: String, llave: String?, torqueNm: Int?, verificado: Boolean) -> Unit,
    onEliminar: (() -> Unit)? = null
) {
    var parte by remember { mutableStateOf(inicial?.parte.orEmpty()) }
    var llave by remember { mutableStateOf(inicial?.llave.orEmpty()) }
    var torque by remember { mutableStateOf(inicial?.torqueNm?.toString().orEmpty()) }
    var verificado by remember { mutableStateOf(inicial?.verificado ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (inicial == null) "Nuevo perno" else "Editar perno") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = parte,
                    onValueChange = { parte = it.take(60) },
                    label = { Text("Parte") },
                    placeholder = { Text("Ej.: Tapón de drenaje") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = llave,
                    onValueChange = { llave = it.take(40) },
                    label = { Text("Llave o dado") },
                    placeholder = { Text("Ej.: Dado 17 mm") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = torque,
                    onValueChange = { torque = soloDigitos(it).take(3) },
                    label = { Text("Torque en N·m (opcional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Lo verifiqué en mi moto", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = "Probé que esa llave calza en este perno",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextoSuave
                        )
                    }
                    Switch(checked = verificado, onCheckedChange = { verificado = it }, enabled = llave.isNotBlank())
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val llaveLimpia = llave.trim().ifEmpty { null }
                    onGuardar(parte.trim(), llaveLimpia, torque.toIntOrNull()?.takeIf { it > 0 }, verificado && llaveLimpia != null)
                },
                enabled = parte.isNotBlank()
            ) { Text("Guardar") }
        },
        dismissButton = {
            Row {
                if (onEliminar != null) {
                    TextButton(onClick = onEliminar) { Text("Eliminar", color = ColorGasto) }
                }
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        }
    )
}
