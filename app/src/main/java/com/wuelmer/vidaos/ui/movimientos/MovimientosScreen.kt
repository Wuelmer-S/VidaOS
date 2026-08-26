package com.wuelmer.vidaos.ui.movimientos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.wuelmer.vidaos.ui.theme.ColorGasto
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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TotalDelMesCard(mesFormateado = mesFormateado, total = uiState.totalGastadoMes)

        GraficoGastosPorCategoriaCard(
            gastos = uiState.gastosPorCategoria,
            onAgregarCategoriaClick = { mostrarDialogoCategoria = true },
            onGestionarCategoriasClick = onGestionarCategoriasClick
        )

        GraficoGastosPorOrigenCard(gastos = uiState.gastosPorOrigen)

        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxSize()
        ) {
            if (uiState.movimientos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Aún no hay movimientos registrados.",
                        color = TextoSuave,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp, 16.dp)
                    ) {
                        items(
                            uiState.movimientos.take(LIMITE_ULTIMOS_MOVIMIENTOS),
                            key = { it.movimiento.id }
                        ) { item ->
                            MovimientoRow(item, onClick = onMovimientoClick)
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
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

@Composable
private fun TotalDelMesCard(mesFormateado: String, total: Long) {
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
