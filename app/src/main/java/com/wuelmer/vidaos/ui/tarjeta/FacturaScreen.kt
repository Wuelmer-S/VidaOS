package com.wuelmer.vidaos.ui.tarjeta

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wuelmer.vidaos.VidaOSApplication
import com.wuelmer.vidaos.data.EstadoCuenta
import com.wuelmer.vidaos.data.OperacionTarjeta
import com.wuelmer.vidaos.data.TarjetaDao
import com.wuelmer.vidaos.data.TipoOperacionTarjeta
import com.wuelmer.vidaos.ui.movimientos.formatearMonto
import com.wuelmer.vidaos.ui.theme.ColorGasto
import com.wuelmer.vidaos.ui.theme.ColorIngreso
import com.wuelmer.vidaos.ui.theme.TextoSuave
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FacturaUiState(
    val cargando: Boolean = true,
    val estado: EstadoCuenta? = null,     // null cuando ya cargó = fue borrada
    val operaciones: List<OperacionTarjeta> = emptyList()
)

class FacturaViewModel(estadoId: Long, private val tarjetaDao: TarjetaDao) : ViewModel() {

    val uiState: StateFlow<FacturaUiState> = combine(
        tarjetaDao.getEstado(estadoId),
        tarjetaDao.getOperacionesDe(estadoId)
    ) { estado, ops -> FacturaUiState(false, estado, ops) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FacturaUiState())

    fun eliminar(onEliminada: () -> Unit) {
        val estado = uiState.value.estado ?: return
        viewModelScope.launch {
            tarjetaDao.deleteEstado(estado)
            onEliminada()
        }
    }

    companion object {
        fun factory(estadoId: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as VidaOSApplication
                FacturaViewModel(estadoId, application.database.tarjetaDao())
            }
        }
    }
}

@Composable
fun FacturaRoute(estadoId: Long, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel: FacturaViewModel = viewModel(key = "factura_$estadoId", factory = FacturaViewModel.factory(estadoId))
    val uiState by viewModel.uiState.collectAsState()
    FacturaScreen(uiState, onBackClick, onEliminar = { viewModel.eliminar(onEliminada = onBackClick) }, modifier)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacturaScreen(
    uiState: FacturaUiState,
    onBackClick: () -> Unit,
    onEliminar: () -> Unit,
    modifier: Modifier = Modifier
) {
    var confirmarBorrado by remember { mutableStateOf(false) }
    val e = uiState.estado
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(e?.let { "Factura ${it.fechaEstado.larga()}" } ?: "Factura") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (e != null) {
                        IconButton(onClick = { confirmarBorrado = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Eliminar factura")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (e == null) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text(if (uiState.cargando) "Cargando..." else "Esta factura ya no existe.", color = TextoSuave)
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Tarjeta {
                Fila("Período", "${e.periodoDesde.corta()} → ${e.periodoHasta.corta()}")
                Fila("Total a pagar", formatearMonto(e.totalFacturado))
                Fila("Monto mínimo", formatearMonto(e.montoMinimo))
                Fila("Pagar hasta", e.pagarHasta.larga())
                Fila("Facturado el período anterior", formatearMonto(e.facturadoAnterior))
            }
            SeccionOperaciones("Pagos", uiState.operaciones.filter { it.tipo == TipoOperacionTarjeta.PAGO })
            SeccionOperaciones("Compras", uiState.operaciones.filter { it.tipo == TipoOperacionTarjeta.COMPRA })
            SeccionOperaciones("En cuotas", uiState.operaciones.filter { it.tipo == TipoOperacionTarjeta.CUOTA })
            SeccionOperaciones("Cargos del banco", uiState.operaciones.filter { it.tipo == TipoOperacionTarjeta.CARGO })
        }
    }

    if (confirmarBorrado && e != null) {
        AlertDialog(
            onDismissRequest = { confirmarBorrado = false },
            title = { Text("¿Eliminar esta factura?") },
            text = { Text("Se borra la factura del ${e.fechaEstado.larga()} con su detalle. Puedes volver a importar el PDF cuando quieras.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarBorrado = false
                    onEliminar()
                }) { Text("Eliminar", color = ColorGasto) }
            },
            dismissButton = { TextButton(onClick = { confirmarBorrado = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun SeccionOperaciones(titulo: String, ops: List<OperacionTarjeta>) {
    if (ops.isEmpty()) return
    Tarjeta {
        Row {
            Box(Modifier.weight(1f)) { Etiqueta("$titulo (${ops.size})") }
            Text(formatearMonto(ops.sumOf { it.valorCuota }), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
        ops.forEachIndexed { i, op ->
            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (op.tipo == TipoOperacionTarjeta.CARGO) nombreCargo(op.descripcion) else op.descripcion,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    val detalle = op.fecha.corta() +
                        if (op.cuotasTotal > 1) " · cuota ${op.cuotaActual}/${op.cuotasTotal} de ${formatearMonto(op.montoOperacion)}" else ""
                    Text(detalle, style = MaterialTheme.typography.bodySmall, color = TextoSuave)
                }
                Text(
                    formatearMonto(op.valorCuota),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (op.valorCuota < 0) ColorIngreso else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
