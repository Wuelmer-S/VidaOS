package com.wuelmer.vidaos.ui.movimientos

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wuelmer.vidaos.data.GastoPorCategoria
import com.wuelmer.vidaos.data.GastoPorOrigen
import com.wuelmer.vidaos.data.MovimientoConCategoria
import com.wuelmer.vidaos.data.OrigenPago
import com.wuelmer.vidaos.data.TipoMovimiento
import com.wuelmer.vidaos.ui.theme.ColorGasto
import com.wuelmer.vidaos.ui.theme.ColorIngreso
import com.wuelmer.vidaos.ui.theme.OrigenCreditoColor
import com.wuelmer.vidaos.ui.theme.OrigenDebitoColor
import com.wuelmer.vidaos.ui.theme.OrigenEfectivoColor
import com.wuelmer.vidaos.ui.theme.TextoSuave
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun MovimientoRow(item: MovimientoConCategoria, onClick: (Long) -> Unit = {}) {
    val esIngreso = item.movimiento.tipo == TipoMovimiento.INGRESO
    val colorMonto = if (esIngreso) ColorIngreso else ColorGasto
    val signo = if (esIngreso) "+" else "−"
    val color = parseColorOrDefault(item.categoriaColor, MaterialTheme.colorScheme.primary)
    val formatterFecha = remember { DateTimeFormatter.ofPattern("dd MMM", Locale.forLanguageTag("es-ES")) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(item.movimiento.id) }
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

internal fun formatearMonto(monto: Long): String {
    val formatter = NumberFormat.getIntegerInstance(Locale.forLanguageTag("es-CL"))
    return "$${formatter.format(monto)}"
}

internal fun parseColorOrDefault(hex: String?, default: Color): Color {
    if (hex.isNullOrBlank()) return default
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (error: IllegalArgumentException) {
        default
    }
}

@Composable
internal fun GraficoGastosPorCategoriaCard(
    gastos: List<GastoPorCategoria>,
    modifier: Modifier = Modifier,
    onAgregarCategoriaClick: () -> Unit = {},
    onGestionarCategoriasClick: () -> Unit = {}
) {
    val colorPorDefecto = MaterialTheme.colorScheme.primary
    val total = gastos.sumOf { it.total }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp, 16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "En qué se te fue la plata",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextoSuave,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onGestionarCategoriasClick, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = "Gestionar categorías",
                        tint = TextoSuave
                    )
                }
                IconButton(onClick = onAgregarCategoriaClick, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "Agregar categoría",
                        tint = TextoSuave
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Canvas(modifier = Modifier.size(96.dp)) {
                    if (total <= 0L) {
                        drawArc(
                            color = colorPorDefecto.copy(alpha = 0.15f),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = size.minDimension * 0.22f)
                        )
                        return@Canvas
                    }
                    var anguloInicio = -90f
                    val grosor = size.minDimension * 0.22f
                    gastos.forEach { gasto ->
                        val barrido = 360f * gasto.total / total
                        drawArc(
                            color = parseColorOrDefault(gasto.categoriaColor, colorPorDefecto),
                            startAngle = anguloInicio,
                            sweepAngle = barrido,
                            useCenter = false,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = grosor)
                        )
                        anguloInicio += barrido
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (gastos.isEmpty()) {
                        Text(
                            text = "Aún no hay gastos este mes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextoSuave
                        )
                    } else {
                        gastos.forEach { gasto ->
                            LeyendaCategoria(gasto = gasto, total = total)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LeyendaCategoria(gasto: GastoPorCategoria, total: Long) {
    val color = parseColorOrDefault(gasto.categoriaColor, MaterialTheme.colorScheme.primary)
    val porcentaje = if (total > 0) (gasto.total * 100 / total).toInt() else 0

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(shape = CircleShape, color = color, modifier = Modifier.size(10.dp)) {}
        Text(
            text = "${gasto.categoriaNombre} · $porcentaje%",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
    }
}

internal fun OrigenPago.color(): Color = when (this) {
    OrigenPago.DEBITO -> OrigenDebitoColor
    OrigenPago.CREDITO -> OrigenCreditoColor
    OrigenPago.EFECTIVO -> OrigenEfectivoColor
}

internal fun OrigenPago.etiqueta(): String = when (this) {
    OrigenPago.DEBITO -> "Débito"
    OrigenPago.CREDITO -> "Crédito"
    OrigenPago.EFECTIVO -> "Efectivo"
}

@Composable
internal fun GraficoGastosPorOrigenCard(gastos: List<GastoPorOrigen>, modifier: Modifier = Modifier) {
    val totalesPorOrigen = remember(gastos) {
        OrigenPago.entries.associateWith { origen -> gastos.firstOrNull { it.origen == origen }?.total ?: 0L }
    }
    val total = totalesPorOrigen.values.sum()
    val colorVacio = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp, 16.dp)) {
            Text(
                text = "Cómo gastaste la plata",
                style = MaterialTheme.typography.labelMedium,
                color = TextoSuave
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Canvas(modifier = Modifier.size(96.dp)) {
                    val grosor = size.minDimension * 0.22f
                    if (total <= 0L) {
                        drawArc(
                            color = colorVacio,
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = grosor)
                        )
                        return@Canvas
                    }
                    var anguloInicio = -90f
                    OrigenPago.entries.forEach { origen ->
                        val monto = totalesPorOrigen.getValue(origen)
                        if (monto <= 0L) return@forEach
                        val barrido = 360f * monto / total
                        drawArc(
                            color = origen.color(),
                            startAngle = anguloInicio,
                            sweepAngle = barrido,
                            useCenter = false,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = grosor)
                        )
                        anguloInicio += barrido
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (total <= 0L) {
                        Text(
                            text = "Aún no hay gastos este mes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextoSuave
                        )
                    } else {
                        OrigenPago.entries.forEach { origen ->
                            LeyendaOrigen(origen = origen, monto = totalesPorOrigen.getValue(origen), total = total)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LeyendaOrigen(origen: OrigenPago, monto: Long, total: Long) {
    val porcentaje = if (total > 0) (monto * 100 / total).toInt() else 0

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(shape = CircleShape, color = origen.color(), modifier = Modifier.size(10.dp)) {}
        Text(
            text = "${origen.etiqueta()} · $porcentaje%",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
    }
}
