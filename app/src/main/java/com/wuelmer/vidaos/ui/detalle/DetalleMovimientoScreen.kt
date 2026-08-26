package com.wuelmer.vidaos.ui.detalle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wuelmer.vidaos.VidaOSApplication
import com.wuelmer.vidaos.data.MovimientoConCategoria
import com.wuelmer.vidaos.data.OrigenPago
import com.wuelmer.vidaos.data.TipoMovimiento
import com.wuelmer.vidaos.ui.movimientos.formatearMonto
import com.wuelmer.vidaos.ui.movimientos.parseColorOrDefault
import com.wuelmer.vidaos.ui.theme.ColorGasto
import com.wuelmer.vidaos.ui.theme.ColorIngreso
import com.wuelmer.vidaos.ui.theme.TextoSuave
import com.wuelmer.vidaos.ui.theme.VidaOSTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DetalleMovimientoRoute(
    movimientoId: Long,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val application = LocalContext.current.applicationContext as VidaOSApplication
    val factory = remember(movimientoId) {
        viewModelFactory {
            initializer {
                DetalleMovimientoViewModel(
                    movimientoId = movimientoId,
                    movimientoDao = application.database.movimientoDao()
                )
            }
        }
    }
    val viewModel: DetalleMovimientoViewModel = viewModel(factory = factory)
    val movimiento by viewModel.movimiento.collectAsState()

    DetalleMovimientoScreen(
        item = movimiento,
        onBackClick = onBackClick,
        onEliminarConfirmado = { viewModel.eliminar(onEliminado = onBackClick) },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleMovimientoScreen(
    item: MovimientoConCategoria?,
    onBackClick: () -> Unit,
    onEliminarConfirmado: () -> Unit,
    modifier: Modifier = Modifier
) {
    var mostrarConfirmacion by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Detalle del movimiento") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (item == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Este movimiento ya no existe.",
                        color = TextoSuave,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                DetalleCard(item)

                Button(
                    onClick = { mostrarConfirmacion = true },
                    colors = ButtonDefaults.buttonColors(containerColor = ColorGasto),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null)
                    Text(text = "Eliminar movimiento", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }

    if (mostrarConfirmacion) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacion = false },
            title = { Text("¿Eliminar este movimiento?") },
            text = { Text("Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    mostrarConfirmacion = false
                    onEliminarConfirmado()
                }) {
                    Text("Eliminar", color = ColorGasto)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmacion = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun DetalleCard(item: MovimientoConCategoria) {
    val esIngreso = item.movimiento.tipo == TipoMovimiento.INGRESO
    val colorMonto = if (esIngreso) ColorIngreso else ColorGasto
    val signo = if (esIngreso) "+" else "−"
    val colorCategoria = parseColorOrDefault(item.categoriaColor, MaterialTheme.colorScheme.primary)
    val formatterFecha = remember { DateTimeFormatter.ofPattern("dd 'de' MMMM, yyyy", Locale.forLanguageTag("es-ES")) }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "$signo${formatearMonto(item.movimiento.monto)}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Medium,
                color = colorMonto
            )

            Etiqueta(titulo = "Tipo", valor = item.movimiento.tipo.etiqueta())

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(shape = CircleShape, color = colorCategoria, modifier = Modifier.size(10.dp)) {}
                Etiqueta(titulo = "Categoría", valor = item.categoriaNombre)
            }

            Etiqueta(titulo = "Fecha", valor = item.movimiento.fecha.format(formatterFecha))
            Etiqueta(
                titulo = "Descripción",
                valor = item.movimiento.descripcion.ifBlank { "Sin descripción" }
            )
            Etiqueta(titulo = "Origen", valor = item.movimiento.origen.etiqueta())

            item.movimiento.observaciones?.takeIf { it.isNotBlank() }?.let { observaciones ->
                Etiqueta(titulo = "Observaciones", valor = observaciones)
            }
        }
    }
}

@Composable
private fun Etiqueta(titulo: String, valor: String) {
    Column {
        Text(text = titulo, style = MaterialTheme.typography.labelMedium, color = TextoSuave)
        Text(text = valor, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun TipoMovimiento.etiqueta(): String = when (this) {
    TipoMovimiento.GASTO -> "Gasto"
    TipoMovimiento.INGRESO -> "Ingreso"
    TipoMovimiento.PAGO_TARJETA -> "Pago tarjeta"
    TipoMovimiento.INTERNO -> "Interno"
}

private fun OrigenPago.etiqueta(): String = when (this) {
    OrigenPago.DEBITO -> "Débito"
    OrigenPago.CREDITO -> "Crédito"
    OrigenPago.EFECTIVO -> "Efectivo"
}

@Preview(showBackground = true)
@Composable
private fun DetalleMovimientoScreenPreview() {
    VidaOSTheme {
        DetalleMovimientoScreen(
            item = MovimientoConCategoria(
                movimiento = com.wuelmer.vidaos.data.Movimiento(
                    id = 1,
                    fecha = LocalDate.now(),
                    monto = 48500,
                    descripcion = "Supermercado",
                    categoriaId = 1,
                    origen = OrigenPago.DEBITO,
                    tipo = TipoMovimiento.GASTO
                ),
                categoriaNombre = "Comida",
                categoriaColor = "#7B61FF"
            ),
            onBackClick = {},
            onEliminarConfirmado = {}
        )
    }
}
