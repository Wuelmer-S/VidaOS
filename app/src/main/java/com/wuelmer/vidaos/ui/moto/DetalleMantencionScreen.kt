package com.wuelmer.vidaos.ui.moto

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wuelmer.vidaos.data.HechoPor
import com.wuelmer.vidaos.data.RegistroMantencion
import com.wuelmer.vidaos.data.TipoMantencion
import com.wuelmer.vidaos.ui.theme.TextoSuave
import java.time.LocalDate

@Composable
fun DetalleMantencionRoute(
    tipoId: Long,
    onBackClick: () -> Unit,
    onVerFicha: (trabajoId: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: DetalleMantencionViewModel = viewModel(
        key = "detalle_mantencion_$tipoId",
        factory = DetalleMantencionViewModel.factory(tipoId)
    )
    val uiState by viewModel.uiState.collectAsState()
    DetalleMantencionScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onGuardarRegistro = viewModel::guardarRegistro,
        onEliminarRegistro = viewModel::eliminarRegistro,
        onActualizarTipo = viewModel::actualizarTipo,
        onVerFicha = onVerFicha,
        onEliminarTipo = { tipo -> viewModel.eliminarTipo(tipo, onEliminado = onBackClick) },
        modifier = modifier
    )
}

// Qué se está editando en un diálogo: nada, un registro nuevo, uno existente o el tipo.
private sealed interface Edicion {
    data object Ninguna : Edicion
    data object NuevoRegistro : Edicion
    data class Registro(val registro: RegistroMantencion) : Edicion
    data object Tipo : Edicion
    data object BorrarTipo : Edicion
    data class BorrarRegistro(val registro: RegistroMantencion) : Edicion
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleMantencionScreen(
    uiState: DetalleMantencionUiState,
    onBackClick: () -> Unit,
    onGuardarRegistro: (RegistroMantencion?, LocalDate, Int, HechoPor, String?) -> Unit,
    onEliminarRegistro: (RegistroMantencion) -> Unit,
    onActualizarTipo: (TipoMantencion, IntervaloValido, String, Long?) -> Unit,
    onVerFicha: (trabajoId: Long) -> Unit,
    onEliminarTipo: (TipoMantencion) -> Unit,
    modifier: Modifier = Modifier
) {
    var edicion by remember { mutableStateOf<Edicion>(Edicion.Ninguna) }
    val estado = uiState.estado

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(estado?.tipo?.nombre ?: "Mantención") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (estado != null) {
                        IconButton(onClick = { edicion = Edicion.Tipo }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Editar mantención")
                        }
                        IconButton(onClick = { edicion = Edicion.BorrarTipo }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Eliminar mantención")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (uiState.cargando || estado == null) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text(
                    text = if (uiState.cargando) "Cargando..." else "Esta mantención ya no existe.",
                    color = TextoSuave,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TarjetaEstado(estado)
            Button(
                onClick = { edicion = Edicion.NuevoRegistro },
                shape = RoundedCornerShape(50),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Registrar mantención")
            }
            uiState.trabajo?.let { trabajo ->
                OutlinedButton(
                    onClick = { onVerFicha(trabajo.id) },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("${trabajo.icono} ¿Qué necesito? Ver ficha")
                }
            }
            TarjetaHistorial(uiState.registros, onRegistroClick = { edicion = Edicion.Registro(it) })
        }
    }

    if (estado == null) return
    when (val e = edicion) {
        Edicion.Ninguna -> Unit
        Edicion.NuevoRegistro, is Edicion.Registro -> {
            val inicial = (e as? Edicion.Registro)?.registro
            DialogoRegistro(
                nombreTipo = estado.tipo.nombre,
                inicial = inicial,
                kmSugerido = uiState.kmActual,
                onDismiss = { edicion = Edicion.Ninguna },
                onGuardar = { fecha, km, hechoPor, notas ->
                    onGuardarRegistro(inicial, fecha, km, hechoPor, notas)
                    edicion = Edicion.Ninguna
                },
                onEliminar = inicial?.let { r -> { edicion = Edicion.BorrarRegistro(r) } }
            )
        }
        Edicion.Tipo -> DialogoTipo(
            inicial = estado.tipo,
            trabajos = uiState.trabajos,
            onDismiss = { edicion = Edicion.Ninguna },
            onGuardar = { intervalo, icono, trabajoId ->
                onActualizarTipo(estado.tipo, intervalo, icono, trabajoId)
                edicion = Edicion.Ninguna
            }
        )
        Edicion.BorrarTipo -> {
            val n = uiState.registros.size
            Confirmacion(
                titulo = "¿Eliminar \"${estado.tipo.nombre}\"?",
                texto = if (n == 0) "Se quitará del plan de mantención."
                else "Se quitará del plan y se borrará su historial (" +
                    (if (n == 1) "1 registro" else "$n registros") + "). Esta acción no se puede deshacer.",
                onConfirmar = {
                    edicion = Edicion.Ninguna
                    onEliminarTipo(estado.tipo)
                },
                onCancelar = { edicion = Edicion.Ninguna }
            )
        }
        is Edicion.BorrarRegistro -> Confirmacion(
            titulo = "¿Eliminar este registro?",
            texto = "${formatearFecha(e.registro.fecha)} · ${formatearKm(e.registro.km)}. Esta acción no se puede deshacer.",
            onConfirmar = {
                onEliminarRegistro(e.registro)
                edicion = Edicion.Ninguna
            },
            onCancelar = { edicion = Edicion.Ninguna }
        )
    }
}

@Composable
internal fun Confirmacion(titulo: String, texto: String, onConfirmar: () -> Unit, onCancelar: () -> Unit, boton: String = "Eliminar") {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(titulo) },
        text = { Text(texto) },
        confirmButton = { TextButton(onClick = onConfirmar) { Text(boton) } },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
}

@Composable
private fun TarjetaEstado(estado: EstadoMantencion) {
    val color = colorSemaforo(estado.semaforo)
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(52.dp).clip(CircleShape).background(color.copy(alpha = 0.18f))
                ) {
                    Text(estado.tipo.icono, fontSize = 24.sp)
                }
                Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                    Text(
                        text = describirRestante(estado).replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = describirIntervalo(estado.tipo.cadaKm, estado.tipo.cadaDias),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextoSuave
                    )
                }
                EtiquetaEstado(estado.semaforo, color)
            }
            if (estado.progreso != null) {
                BarraProgreso(fraccion = estado.progreso, color = color, fondo = MaterialTheme.colorScheme.outline)
            }
            Text(
                text = estado.ultimo?.let { "Última vez: ${formatearFecha(it.fecha)} a los ${formatearKm(it.km)}" }
                    ?: "Registra la última vez que la hiciste para que empiece a contar.",
                style = MaterialTheme.typography.bodySmall,
                color = TextoSuave
            )
        }
    }
}

@Composable
private fun TarjetaHistorial(registros: List<RegistroMantencion>, onRegistroClick: (RegistroMantencion) -> Unit) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "HISTORIAL",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextoSuave
            )
            if (registros.isEmpty()) {
                Text(
                    text = "Aún no hay registros.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextoSuave,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            registros.forEachIndexed { i, registro ->
                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onRegistroClick(registro) }
                        .padding(vertical = 12.dp)
                ) {
                    Row {
                        Text(
                            text = formatearFecha(registro.fecha),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(formatearKm(registro.km), style = MaterialTheme.typography.bodyLarge)
                    }
                    val quien = if (registro.hechoPor == HechoPor.YO) "La hice yo" else "Taller"
                    Text(
                        text = listOfNotNull(quien, registro.notas).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoSuave
                    )
                }
            }
            if (registros.isNotEmpty()) {
                Text(
                    text = "Toca un registro para editarlo o borrarlo",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextoSuave,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
