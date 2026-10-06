package com.wuelmer.vidaos.ui.movimientos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wuelmer.vidaos.data.Movimiento
import com.wuelmer.vidaos.data.MovimientoConCategoria
import com.wuelmer.vidaos.data.OrigenPago
import com.wuelmer.vidaos.data.TipoCategoria
import com.wuelmer.vidaos.data.TipoMovimiento
import com.wuelmer.vidaos.ui.categorias.AgregarCategoriaDialog
import com.wuelmer.vidaos.ui.tarjeta.CuotaPeriodo
import com.wuelmer.vidaos.ui.theme.ColorGasto
import com.wuelmer.vidaos.ui.theme.OrigenCreditoColor
import com.wuelmer.vidaos.ui.theme.TextoSuave
import com.wuelmer.vidaos.ui.theme.VidaOSTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val LIMITE_ULTIMOS_MOVIMIENTOS = 5

@Composable
fun MovimientosRoute(
    modifier: Modifier = Modifier,
    onVerHistorialClick: () -> Unit = {},
    onMovimientoClick: (Long) -> Unit = {},
    onGestionarCategoriasClick: () -> Unit = {},
    viewModel: MovimientosViewModel = viewModel(factory = MovimientosViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    MovimientosScreen(
        uiState = uiState,
        onVerHistorialClick = onVerHistorialClick,
        onMovimientoClick = onMovimientoClick,
        onAgregarCategoria = viewModel::agregarCategoria,
        onGestionarCategoriasClick = onGestionarCategoriasClick,
        modifier = modifier
    )
}

@Composable
fun MovimientosScreen(
    uiState: MovimientosUiState,
    onVerHistorialClick: () -> Unit = {},
    onMovimientoClick: (Long) -> Unit = {},
    onAgregarCategoria: (nombre: String, tipo: TipoCategoria) -> Unit = { _, _ -> },
    onGestionarCategoriasClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var mostrarDialogoCategoria by remember { mutableStateOf(false) }
    val mesFormateado = remember {
        LocalDate.now()
            .format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("es-ES")))
            .replaceFirstChar { it.uppercase() }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TotalDelMesCard(
            mesFormateado = mesFormateado,
            total = uiState.totalGastadoMes,
            totalCredito = uiState.gastosPorOrigen.firstOrNull { it.origen == OrigenPago.CREDITO }?.total ?: 0L,
            cuotas = uiState.cuotasProximaFactura
        )

        GraficoGastosPorCategoriaCard(
            gastos = uiState.gastosPorCategoria,
            onAgregarCategoriaClick = { mostrarDialogoCategoria = true },
            onGestionarCategoriasClick = onGestionarCategoriasClick
        )

        GraficoGastosPorOrigenCard(gastos = uiState.gastosPorOrigen)

        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (uiState.movimientos.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Aún no hay movimientos registrados.",
                        color = TextoSuave,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Son pocos (los últimos 5): una columna simple, dentro del scroll de la pantalla.
                    Column(modifier = Modifier.padding(18.dp, 16.dp)) {
                        uiState.movimientos.take(LIMITE_ULTIMOS_MOVIMIENTOS).forEach { item ->
                            key(item.movimiento.id) {
                                MovimientoRow(item, onClick = onMovimientoClick)
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                    TextButton(
                        onClick = onVerHistorialClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Ver historial completo")
                    }
                }
            }
        }
    }

    if (mostrarDialogoCategoria) {
        AgregarCategoriaDialog(
            tipoFijo = null,
            onDismiss = { mostrarDialogoCategoria = false },
            onConfirmar = { nombre, tipo ->
                onAgregarCategoria(nombre, tipo)
                mostrarDialogoCategoria = false
            }
        )
    }
}

// Bajo el total, en chico, lo gastado con crédito este mes: para ir viendo cuánto se va a tener que pagar.
// Las compras en cuotas cuentan completas en el total; aparte, una sola línea con las cuotas de la próxima factura.
@Composable
private fun TotalDelMesCard(mesFormateado: String, total: Long, totalCredito: Long, cuotas: List<CuotaPeriodo> = emptyList()) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp, 16.dp)) {
            Text(
                text = "Gastado en $mesFormateado",
                style = MaterialTheme.typography.labelMedium,
                color = TextoSuave
            )
            Text(
                text = formatearMonto(total),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Medium,
                color = ColorGasto
            )
            Text(
                text = "💳 Con crédito: ${formatearMonto(totalCredito)}",
                style = MaterialTheme.typography.bodySmall,
                color = TextoSuave
            )
            if (cuotas.isNotEmpty()) LineaCuotas(cuotas)
        }
    }
}

@Composable
private fun LineaCuotas(cuotas: List<CuotaPeriodo>) {
    var abierta by rememberSaveable { mutableStateOf(false) }
    // La fila genérica de la factura (sin detalle) no es una compra.
    val compras = cuotas.count { it.total > 0 }.coerceAtLeast(1)
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = OrigenCreditoColor.copy(alpha = 0.16f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .clickable { abierta = !abierta }
                .padding(horizontal = 10.dp, vertical = 7.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "🧾 Cuotas en tu próxima factura: ${formatearMonto(cuotas.sumOf { it.valor })} · " +
                        if (compras == 1) "1 compra" else "$compras compras",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
                Text(if (abierta) "▴" else "▾", style = MaterialTheme.typography.bodySmall)
            }
            if (abierta) {
                cuotas.forEach { c ->
                    Row(modifier = Modifier.padding(top = 4.dp)) {
                        Text(
                            text = if (c.total > 0) "${c.descripcion} ${c.numero}/${c.total}" else c.descripcion,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextoSuave,
                            modifier = Modifier.weight(1f)
                        )
                        Text(formatearMonto(c.valor), style = MaterialTheme.typography.bodySmall, color = TextoSuave)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MovimientosScreenPreview() {
    VidaOSTheme {
        MovimientosScreen(
            uiState = MovimientosUiState(
                totalGastadoMes = 312400,
                movimientos = listOf(
                    MovimientoConCategoria(
                        movimiento = Movimiento(
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
                    MovimientoConCategoria(
                        movimiento = Movimiento(
                            id = 2,
                            fecha = LocalDate.now(),
                            monto = 650000,
                            descripcion = "Sueldo",
                            categoriaId = 2,
                            origen = OrigenPago.DEBITO,
                            tipo = TipoMovimiento.INGRESO
                        ),
                        categoriaNombre = "Sueldo",
                        categoriaColor = "#FFA26B"
                    )
                )
            )
        )
    }
}
