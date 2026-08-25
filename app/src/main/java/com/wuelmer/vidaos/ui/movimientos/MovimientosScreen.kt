package com.wuelmer.vidaos.ui.movimientos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wuelmer.vidaos.data.Categoria
import com.wuelmer.vidaos.data.Movimiento
import com.wuelmer.vidaos.data.MovimientoConCategoria
import com.wuelmer.vidaos.data.OrigenPago
import com.wuelmer.vidaos.data.TipoCategoria
import com.wuelmer.vidaos.data.TipoMovimiento
import com.wuelmer.vidaos.ui.theme.ColorGasto
import com.wuelmer.vidaos.ui.theme.ColorIngreso
import com.wuelmer.vidaos.ui.theme.TextoSuave
import com.wuelmer.vidaos.ui.theme.VidaOSTheme
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun MovimientosRoute(
    modifier: Modifier = Modifier,
    viewModel: MovimientosViewModel = viewModel(factory = MovimientosViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    MovimientosScreen(uiState = uiState, modifier = modifier)
}

@Composable
fun MovimientosScreen(
    uiState: MovimientosUiState,
    modifier: Modifier = Modifier
) {
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
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp, 16.dp)
                ) {
                    items(uiState.movimientos, key = { it.movimiento.id }) { item ->
                        MovimientoRow(item)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
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

@Composable
private fun MovimientoRow(item: MovimientoConCategoria) {
    val esIngreso = item.movimiento.tipo == TipoMovimiento.INGRESO
    val colorMonto = if (esIngreso) ColorIngreso else ColorGasto
    val signo = if (esIngreso) "+" else "−"
    val color = parseColorOrDefault(item.categoriaColor, MaterialTheme.colorScheme.primary)
    val formatterFecha = remember { DateTimeFormatter.ofPattern("dd MMM", Locale.forLanguageTag("es-ES")) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(shape = CircleShape, color = color.copy(alpha = 0.15f), modifier = Modifier.size(36.dp)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Surface(shape = CircleShape, color = color, modifier = Modifier.size(10.dp)) {}
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.movimiento.descripcion.ifBlank { item.categoriaNombre },
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "${item.categoriaNombre} · ${item.movimiento.fecha.format(formatterFecha)}",
                style = MaterialTheme.typography.bodySmall,
                color = TextoSuave
            )
        }

        Text(
            text = "$signo${formatearMonto(item.movimiento.monto)}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = colorMonto
        )
    }
}

private fun formatearMonto(monto: Long): String {
    val formatter = NumberFormat.getIntegerInstance(Locale.forLanguageTag("es-CL"))
    return "$${formatter.format(monto)}"
}

private fun parseColorOrDefault(hex: String?, default: Color): Color {
    if (hex.isNullOrBlank()) return default
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (error: IllegalArgumentException) {
        default
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
