package com.wuelmer.vidaos.ui.tarjeta

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wuelmer.vidaos.VidaOSApplication
import com.wuelmer.vidaos.data.FacturaLeida
import com.wuelmer.vidaos.data.ResultadoPdf
import com.wuelmer.vidaos.data.TarjetaDao
import com.wuelmer.vidaos.data.TarjetaRepository
import com.wuelmer.vidaos.data.TipoOperacionTarjeta
import com.wuelmer.vidaos.ui.movimientos.formatearMonto
import com.wuelmer.vidaos.ui.theme.ColorGasto
import com.wuelmer.vidaos.ui.theme.ColorIngreso
import com.wuelmer.vidaos.ui.theme.TextoSuave
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface EstadoImportacion {
    data object Leyendo : EstadoImportacion
    data class PideClave(val incorrecta: Boolean) : EstadoImportacion
    // yaExiste: ya hay una factura con esa fecha y se reemplazará.
    data class Lista(val factura: FacturaLeida, val yaExiste: Boolean) : EstadoImportacion
    data class Error(val mensaje: String) : EstadoImportacion
    data object Guardada : EstadoImportacion
}

class ImportarTarjetaViewModel(
    private val uri: Uri,
    private val repositorio: TarjetaRepository,
    private val tarjetaDao: TarjetaDao
) : ViewModel() {

    private val _estado = MutableStateFlow<EstadoImportacion>(EstadoImportacion.Leyendo)
    val estado: StateFlow<EstadoImportacion> = _estado.asStateFlow()

    init {
        leer(clave = null)
    }

    fun leer(clave: String?) {
        _estado.value = EstadoImportacion.Leyendo
        viewModelScope.launch {
            _estado.value = when (val r = repositorio.leer(uri, clave)) {
                is ResultadoPdf.Ok -> EstadoImportacion.Lista(
                    r.factura,
                    yaExiste = tarjetaDao.getEstadoPorFecha(r.factura.estado.fechaEstado) != null
                )
                is ResultadoPdf.NecesitaClave -> EstadoImportacion.PideClave(r.incorrecta)
                is ResultadoPdf.Error -> EstadoImportacion.Error(r.mensaje)
            }
        }
    }

    fun guardar() {
        val lista = _estado.value as? EstadoImportacion.Lista ?: return
        if (!lista.factura.cuadra) return
        viewModelScope.launch {
            // Si se reemplaza, se conserva el "Ya la pagué" que hubiera marcado.
            val anterior = tarjetaDao.getEstadoPorFecha(lista.factura.estado.fechaEstado)
            val estado = lista.factura.estado.copy(pagadaManual = anterior?.pagadaManual ?: false)
            tarjetaDao.guardarFactura(estado, lista.factura.operaciones)
            _estado.value = EstadoImportacion.Guardada
        }
    }

    companion object {
        fun factory(uri: Uri): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as VidaOSApplication
                ImportarTarjetaViewModel(uri, application.tarjeta, application.database.tarjetaDao())
            }
        }
    }
}

@Composable
fun ImportarTarjetaRoute(
    uri: Uri,
    onBackClick: () -> Unit,
    onGuardada: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: ImportarTarjetaViewModel = viewModel(key = "importar_$uri", factory = ImportarTarjetaViewModel.factory(uri))
    val estado by viewModel.estado.collectAsState()
    if (estado is EstadoImportacion.Guardada) {
        androidx.compose.runtime.LaunchedEffect(Unit) { onGuardada() }
    }
    ImportarTarjetaScreen(
        estado = estado,
        onBackClick = onBackClick,
        onClave = viewModel::leer,
        onGuardar = viewModel::guardar,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportarTarjetaScreen(
    estado: EstadoImportacion,
    onBackClick: () -> Unit,
    onClave: (String) -> Unit,
    onGuardar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Importar estado de cuenta") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (estado) {
                EstadoImportacion.Leyendo, EstadoImportacion.Guardada, is EstadoImportacion.PideClave ->
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("Leyendo el PDF...", color = TextoSuave)
                    }
                is EstadoImportacion.Error -> Tarjeta {
                    Text("No se pudo importar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                    Text(estado.mensaje, style = MaterialTheme.typography.bodyMedium, color = TextoSuave, modifier = Modifier.padding(top = 6.dp))
                    OutlinedButton(onClick = onBackClick, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) { Text("Volver") }
                }
                is EstadoImportacion.Lista -> VistaPrevia(estado, onGuardar, onBackClick)
            }
        }
    }

    if (estado is EstadoImportacion.PideClave) DialogoClave(estado.incorrecta, onClave, onBackClick)
}

@Composable
private fun VistaPrevia(lista: EstadoImportacion.Lista, onGuardar: () -> Unit, onCancelar: () -> Unit) {
    val f = lista.factura
    val e = f.estado
    val ops = f.operaciones
    fun suma(tipo: TipoOperacionTarjeta) = ops.filter { it.tipo == tipo }
    Tarjeta {
        Text("Banco de Chile / Edwards · crédito", style = MaterialTheme.typography.bodySmall, color = TextoSuave)
        Fila("Facturación", e.fechaEstado.larga())
        Fila("Período", "${e.periodoDesde.corta()} → ${e.periodoHasta.corta()}")
        Fila("Total a pagar", formatearMonto(e.totalFacturado))
        Fila("Pagar hasta", e.pagarHasta.larga())
        val compras = suma(TipoOperacionTarjeta.COMPRA)
        Fila("Compras", "${compras.size} · ${formatearMonto(compras.sumOf { it.valorCuota })}")
        val cuotas = suma(TipoOperacionTarjeta.CUOTA)
        Fila("En cuotas", "${cuotas.size} · ${formatearMonto(cuotas.sumOf { it.valorCuota })}")
        Fila("Cargos del banco", formatearMonto(suma(TipoOperacionTarjeta.CARGO).sumOf { it.valorCuota }))
        Fila("Pagos del período", formatearMonto(suma(TipoOperacionTarjeta.PAGO).sumOf { it.valorCuota }), ColorIngreso)
        if (f.cuadra) {
            Text("✓ Los números cuadran con el total de la factura", style = MaterialTheme.typography.bodySmall, color = ColorIngreso, modifier = Modifier.padding(top = 6.dp))
        } else {
            Text(
                "⚠ La suma da ${formatearMonto(f.sumaCalculada)} y el banco dice ${formatearMonto(e.totalFacturado)}. " +
                    "No se guarda para no mostrarte datos equivocados.",
                style = MaterialTheme.typography.bodySmall,
                color = ColorGasto,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        if (lista.yaExiste) {
            Text("Ya importaste esta factura: se reemplazará.", style = MaterialTheme.typography.bodySmall, color = TextoSuave, modifier = Modifier.padding(top = 4.dp))
        }
        Button(onClick = onGuardar, enabled = f.cuadra, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Text(if (lista.yaExiste) "Reemplazar factura" else "Guardar factura")
        }
        OutlinedButton(onClick = onCancelar, modifier = Modifier.fillMaxWidth()) { Text("Cancelar") }
    }
}

@Composable
private fun DialogoClave(incorrecta: Boolean, onClave: (String) -> Unit, onCancelar: () -> Unit) {
    var clave by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Clave del PDF") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (incorrecta) "Esa clave no abrió el PDF. Inténtalo de nuevo." else
                        "El banco protege el estado de cuenta con una clave. Se guarda solo en este teléfono.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (incorrecta) ColorGasto else TextoSuave
                )
                OutlinedTextField(
                    value = clave,
                    onValueChange = { clave = it.take(20) },
                    label = { Text("Clave") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = { TextButton(onClick = { onClave(clave) }, enabled = clave.isNotBlank()) { Text("Abrir") } },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
}
