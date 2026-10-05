package com.wuelmer.vidaos.ui.configuracion

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.wuelmer.vidaos.avisos.Notificador
import com.wuelmer.vidaos.avisos.formatearHora
import com.wuelmer.vidaos.ui.navegacion.Modulo
import com.wuelmer.vidaos.ui.navegacion.modulosVisibles
import com.wuelmer.vidaos.ui.theme.ColorGasto
import com.wuelmer.vidaos.ui.theme.TextoSuave

// Qué hora se está editando en el selector.
private enum class HoraAviso { MOTO, GASTOS }

// Avisos de la moto (mañana) y recordatorio de gastos (noche). Solo aparecen los de módulos visibles.
@Composable
fun SeccionAvisos(
    uiState: ConfiguracionUiState,
    onAvisosMotoChange: (Boolean) -> Unit,
    onHoraAvisosMotoChange: (Int) -> Unit,
    onRecordatorioGastosChange: (Boolean) -> Unit,
    onHoraRecordatorioGastosChange: (Int) -> Unit,
    onRevisarAhora: () -> Unit
) {
    val visibles = modulosVisibles(uiState.modulosOcultos)
    val conMoto = Modulo.MOTO in visibles
    val conFinanzas = Modulo.FINANZAS in visibles
    if (!conMoto && !conFinanzas) return

    val permitido = notificacionesPermitidas()
    var editando by remember { mutableStateOf<HoraAviso?>(null) }

    Text(
        text = "Avisos",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(top = 10.dp)
    )
    if (!permitido) AvisoSinPermiso()
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            if (conMoto) {
                OpcionAviso(
                    titulo = "Avisos de la moto",
                    descripcion = "Mantenciones al 50 %, al 90 % y vencidas, y km sin anotar hace 7 días",
                    activo = uiState.avisosMoto,
                    hora = uiState.horaAvisosMoto,
                    habilitado = !uiState.cargando,
                    onActivoChange = onAvisosMotoChange,
                    onHoraClick = { editando = HoraAviso.MOTO }
                )
            }
            if (conMoto && conFinanzas) HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            if (conFinanzas) {
                OpcionAviso(
                    titulo = "Recordatorio de gastos",
                    descripcion = "Solo si ese día todavía no anotaste ningún gasto",
                    activo = uiState.recordatorioGastos,
                    hora = uiState.horaRecordatorioGastos,
                    habilitado = !uiState.cargando,
                    onActivoChange = onRecordatorioGastosChange,
                    onHoraClick = { editando = HoraAviso.GASTOS }
                )
            }
        }
    }
    OutlinedButton(
        onClick = onRevisarAhora,
        enabled = permitido && !uiState.cargando,
        modifier = Modifier.fillMaxWidth()
    ) { Text("Revisar ahora") }
    Text(
        text = "Hace la misma revisión que a la hora programada. Un aviso del 50 % o del 90 % llega una sola vez; " +
            "el de vencida se repite cada mañana hasta que la registres.",
        style = MaterialTheme.typography.bodySmall,
        color = TextoSuave
    )

    when (editando) {
        HoraAviso.MOTO -> DialogoHora(
            titulo = "Hora de los avisos de la moto",
            inicial = uiState.horaAvisosMoto,
            onDismiss = { editando = null },
            onGuardar = {
                onHoraAvisosMotoChange(it)
                editando = null
            }
        )
        HoraAviso.GASTOS -> DialogoHora(
            titulo = "Hora del recordatorio de gastos",
            inicial = uiState.horaRecordatorioGastos,
            onDismiss = { editando = null },
            onGuardar = {
                onHoraRecordatorioGastosChange(it)
                editando = null
            }
        )
        null -> Unit
    }
}

@Composable
private fun OpcionAviso(
    titulo: String,
    descripcion: String,
    activo: Boolean,
    hora: Int,
    habilitado: Boolean,
    onActivoChange: (Boolean) -> Unit,
    onHoraClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 6.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.bodyLarge)
            Text(descripcion, style = MaterialTheme.typography.bodySmall, color = TextoSuave)
            TextButton(onClick = onHoraClick, enabled = habilitado && activo) {
                Text("🕒 ${formatearHora(hora)}", style = MaterialTheme.typography.titleMedium)
            }
        }
        Switch(checked = activo, onCheckedChange = onActivoChange, enabled = habilitado)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogoHora(titulo: String, inicial: Int, onDismiss: () -> Unit, onGuardar: (Int) -> Unit) {
    val estado = rememberTimePickerState(initialHour = inicial / 60, initialMinute = inicial % 60, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titulo) },
        text = { TimePicker(state = estado) },
        confirmButton = {
            TextButton(onClick = { onGuardar(estado.hour * 60 + estado.minute) }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun AvisoSinPermiso() {
    val context = LocalContext.current
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = ColorGasto.copy(alpha = 0.12f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Las notificaciones de vidaOS están desactivadas, así que no te llegará ningún aviso.",
                style = MaterialTheme.typography.bodyMedium
            )
            TextButton(onClick = {
                context.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                )
            }) { Text("Activarlas en Ajustes") }
        }
    }
}

// Se vuelve a mirar cada vez que la pantalla vuelve al frente (p. ej. al regresar de los Ajustes del teléfono).
@Composable
private fun notificacionesPermitidas(): Boolean {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var permitido by remember { mutableStateOf(Notificador.puedeNotificar(context)) }
    DisposableEffect(lifecycleOwner) {
        val observador = LifecycleEventObserver { _, evento ->
            if (evento == Lifecycle.Event.ON_RESUME) permitido = Notificador.puedeNotificar(context)
        }
        lifecycleOwner.lifecycle.addObserver(observador)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observador) }
    }
    return permitido
}
