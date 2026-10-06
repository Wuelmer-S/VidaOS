package com.wuelmer.vidaos.ui.tarjeta

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wuelmer.vidaos.data.EstadoCuenta
import com.wuelmer.vidaos.data.OperacionTarjeta
import com.wuelmer.vidaos.ui.movimientos.formatearMonto
import com.wuelmer.vidaos.ui.theme.ColorDestacado
import com.wuelmer.vidaos.ui.theme.ColorGasto
import com.wuelmer.vidaos.ui.theme.ColorIngreso
import com.wuelmer.vidaos.ui.theme.OrigenCreditoColor
import com.wuelmer.vidaos.ui.theme.TextoSuave
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val formatoDia = DateTimeFormatter.ofPattern("EEE d MMM", Locale.forLanguageTag("es-CL"))
private val formatoCorto = DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("es-CL"))
private val formatoFactura = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("es-CL"))

internal fun LocalDate.corta(): String = format(formatoCorto).replace(".", "")
internal fun LocalDate.larga(): String = format(formatoFactura).replace(".", "")

@Composable
fun TarjetaRoute(
    onImportar: (Uri) -> Unit,
    onFacturaClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TarjetaViewModel = viewModel(factory = TarjetaViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val elegirPdf = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(onImportar)
    }
    TarjetaScreen(
        uiState = uiState,
        onImportarClick = { elegirPdf.launch(arrayOf("application/pdf")) },
        onFacturaClick = onFacturaClick,
        onMarcarPagada = viewModel::marcarPagada,
        modifier = modifier
    )
}

@Composable
fun TarjetaScreen(
    uiState: TarjetaUiState,
    onImportarClick: () -> Unit,
    onFacturaClick: (Long) -> Unit,
    onMarcarPagada: (EstadoCuenta, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val ultima = uiState.ultima
        when {
            uiState.cargando -> Unit
            ultima == null -> SinFacturas(onImportarClick)
            else -> {
                TarjetaAPagar(ultima, onMarcarPagada)
                uiState.periodo?.let { TarjetaPeriodo(it) }
                TarjetaCupo(ultima.estado, uiState.cupoDisponible)
                TarjetaCuotas(uiState.cuotas, uiState.meses)
                if (uiState.cargos.isNotEmpty()) TarjetaCargos(ultima.estado, uiState.cargos)
                TarjetaFacturas(uiState.facturas, onFacturaClick, onImportarClick)
            }
        }
    }
}

@Composable
internal fun Tarjeta(contenido: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp, 16.dp)) { contenido() }
    }
}

@Composable
internal fun Etiqueta(texto: String) {
    Text(
        text = texto.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = TextoSuave
    )
}

@Composable
internal fun Fila(izquierda: String, derecha: String, colorDerecha: Color = MaterialTheme.colorScheme.onSurface) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
    ) {
        Text(izquierda, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(derecha, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = colorDerecha)
    }
}

@Composable
internal fun Chip(texto: String, color: Color) {
    Text(
        text = texto,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.3f))
            .padding(horizontal = 10.dp, vertical = 3.dp)
    )
}

@Composable
private fun SinFacturas(onImportarClick: () -> Unit) {
    Tarjeta {
        Text("💳 Tu tarjeta de crédito", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
        Text(
            text = "Importa el estado de cuenta que te manda el banco por correo (PDF) para ver cuánto debes, " +
                "hasta cuándo pagar, tus cuotas y lo que te cobra el banco.\n\n" +
                "Desde el correo: abre el PDF → Compartir → vidaOS. O elígelo aquí si ya está descargado.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextoSuave,
            modifier = Modifier.padding(top = 8.dp)
        )
        OutlinedButton(onClick = onImportarClick, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
            Text("📄 Importar estado de cuenta")
        }
    }
}

// Texto y color del chip según el estado del pago.
internal fun chipPago(pago: EstadoPago): Pair<String, Color> = when (pago) {
    EstadoPago.SinDeuda -> "✓ Sin deuda" to ColorIngreso
    EstadoPago.Confirmada -> "✓ Confirmada por el banco" to ColorIngreso
    is EstadoPago.Pagada -> "✓ Pagada" to ColorIngreso
    is EstadoPago.Parcial -> "Pago parcial" to ColorDestacado
    is EstadoPago.Pendiente -> when {
        pago.dias == 0L -> "Vence hoy" to ColorGasto
        pago.dias <= 3 -> "Faltan ${pago.dias} días" to ColorGasto
        pago.dias <= 7 -> "Faltan ${pago.dias} días" to ColorDestacado
        else -> "Faltan ${pago.dias} días" to OrigenCreditoColor
    }
    is EstadoPago.Vencida -> "Vencida" to ColorGasto
}

@Composable
private fun TarjetaAPagar(factura: FacturaResumen, onMarcarPagada: (EstadoCuenta, Boolean) -> Unit) {
    val e = factura.estado
    val pago = factura.pago
    Tarjeta {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f)) { Etiqueta("Factura del ${e.fechaEstado.corta()}") }
            val (texto, color) = chipPago(pago)
            Chip(texto, color)
        }
        Text(
            text = formatearMonto(e.totalFacturado),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 4.dp)
        )
        Text(
            "Pagar hasta el ${e.pagarHasta.format(formatoDia).replace(".", "")}",
            style = MaterialTheme.typography.bodySmall,
            color = TextoSuave
        )
        Spacer(Modifier.height(6.dp))
        when (pago) {
            is EstadoPago.Pagada ->
                if (pago.manual) {
                    Fila("Marcada como pagada", "✓", ColorIngreso)
                } else {
                    Fila("Pagos anotados desde el ${e.fechaEstado.corta()}", formatearMonto(pago.pagado), ColorIngreso)
                }
            is EstadoPago.Parcial -> {
                Fila("Pagaste", formatearMonto(pago.pagado), ColorIngreso)
                Fila("Falta", formatearMonto(pago.falta), ColorGasto)
            }
            is EstadoPago.Vencida -> Fila("Falta pagar", formatearMonto(pago.falta), ColorGasto)
            else -> Unit
        }
        if (pago !is EstadoPago.Confirmada && pago !is EstadoPago.SinDeuda) {
            val minimo = if (pago is EstadoPago.Parcial && pago.minimoCubierto) "${formatearMonto(e.montoMinimo)} ✓" else formatearMonto(e.montoMinimo)
            Fila("Monto mínimo", minimo)
        }
        when (pago) {
            is EstadoPago.Pagada -> {
                Text(
                    text = "⏳ Falta la confirmación del banco: llega con la próxima factura" +
                        (e.proximoHasta?.let { " (${it.corta()})" } ?: "") + ".",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSuave,
                    modifier = Modifier.padding(top = 4.dp)
                )
                if (pago.manual) {
                    TextButton(onClick = { onMarcarPagada(e, false) }) { Text("Desmarcar") }
                }
            }
            is EstadoPago.Pendiente, is EstadoPago.Parcial, is EstadoPago.Vencida -> {
                Text(
                    text = "Anota tus pagos en Registrar → \"Pago tarjeta\" y aquí se van sumando.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSuave,
                    modifier = Modifier.padding(top = 4.dp)
                )
                TextButton(onClick = { onMarcarPagada(e, true) }) { Text("Ya la pagué") }
            }
            else -> Unit
        }
    }
}

@Composable
private fun TarjetaPeriodo(periodo: PeriodoActual) {
    Tarjeta {
        Etiqueta("Período actual · ${periodo.desde.corta()} → ${periodo.hasta?.corta() ?: "?"}")
        Text(
            text = formatearMonto(periodo.estimado),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 4.dp)
        )
        Text("Estimado de tu próxima factura", style = MaterialTheme.typography.bodySmall, color = TextoSuave)
        Spacer(Modifier.height(6.dp))
        Fila("Gastos con crédito anotados", formatearMonto(periodo.gastosCredito))
        Fila("Cuotas que entran", formatearMonto(periodo.cuotas))
        if (periodo.comision > 0) Fila("Comisión mensual (aprox.)", formatearMonto(periodo.comision))
        periodo.hasta?.let { hasta ->
            val dias = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), hasta)
            if (dias >= 0) {
                Text("Factura el ${hasta.corta()} · faltan $dias días", style = MaterialTheme.typography.bodySmall, color = TextoSuave)
            }
        }
    }
}

@Composable
private fun TarjetaCupo(estado: EstadoCuenta, disponible: Long) {
    Tarjeta {
        Etiqueta("Cupo")
        val usado = (estado.cupoTotal - disponible).coerceAtLeast(0)
        val fraccion = if (estado.cupoTotal > 0) usado.toFloat() / estado.cupoTotal else 0f
        Box(
            modifier = Modifier
                .padding(top = 10.dp, bottom = 6.dp)
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.outline)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraccion.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(OrigenCreditoColor)
            )
        }
        Row {
            Text("Usado ${formatearMonto(usado)}", style = MaterialTheme.typography.bodySmall, color = TextoSuave, modifier = Modifier.weight(1f))
            Text("Disponible ", style = MaterialTheme.typography.bodySmall, color = TextoSuave)
            Text(formatearMonto(disponible), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }
        Text(
            "Cupo total ${formatearMonto(estado.cupoTotal)} · estimado con lo anotado desde el ${estado.fechaEstado.corta()}",
            style = MaterialTheme.typography.bodySmall,
            color = TextoSuave
        )
    }
}

@Composable
private fun TarjetaCuotas(cuotas: List<CuotaPeriodo>, meses: List<Pair<String, Long>>) {
    Tarjeta {
        Etiqueta("Cuotas en tu próxima factura")
        if (cuotas.isEmpty()) {
            Text("No tienes compras en cuotas.", style = MaterialTheme.typography.bodyMedium, color = TextoSuave, modifier = Modifier.padding(top = 6.dp))
        }
        cuotas.forEachIndexed { i, c ->
            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Row {
                    Text(c.descripcion, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text(formatearMonto(c.valor), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
                // total = 0: solo se conoce el monto que vence (la factura no traía el detalle).
                if (c.total > 0) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                        repeat(c.total) { n ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (n < c.numero) OrigenCreditoColor else MaterialTheme.colorScheme.outline)
                            )
                        }
                    }
                    Text(
                        "Cuota ${c.numero} de ${c.total} · total ${formatearMonto(c.montoCompra)} · " +
                            if (c.quedan == 0) "última cuota" else "después quedan ${c.quedan}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoSuave
                    )
                }
            }
        }
        val conMonto = meses.filter { it.second > 0 }
        if (conMonto.isNotEmpty()) {
            Text(
                "Próximos meses: " + conMonto.joinToString(" · ") { (mes, monto) -> "$mes ${formatearMonto(monto)}" },
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun TarjetaCargos(estado: EstadoCuenta, cargos: List<OperacionTarjeta>) {
    Tarjeta {
        Etiqueta("Lo que te cobró el banco (${estado.fechaEstado.corta()})")
        Spacer(Modifier.height(4.dp))
        cargos.forEach { c ->
            val esCompra = c.descripcion.contains("TRASPASO DEUDA INTERNACIONAL")
            Fila(nombreCargo(c.descripcion), formatearMonto(c.valorCuota), if (esCompra) MaterialTheme.colorScheme.onSurface else ColorGasto)
        }
        cargos.firstOrNull { it.descripcion.contains("COMISION") }?.let { comision ->
            Text(
                "💡 La comisión suma ≈ ${formatearMonto(comision.valorCuota * 12)} al año.",
                style = MaterialTheme.typography.bodySmall,
                color = TextoSuave,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun TarjetaFacturas(facturas: List<FacturaResumen>, onFacturaClick: (Long) -> Unit, onImportarClick: () -> Unit) {
    Tarjeta {
        Etiqueta("Facturas")
        facturas.forEach { f ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onFacturaClick(f.estado.id) }
                    .padding(vertical = 10.dp)
            ) {
                Text(f.estado.fechaEstado.larga(), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text(formatearMonto(f.estado.totalFacturado), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Spacer(Modifier.width(8.dp))
                val (texto, color) = chipPago(f.pago)
                Chip(if (texto.startsWith("✓")) "✓" else texto, color)
            }
        }
        OutlinedButton(onClick = onImportarClick, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
            Text("📄 Importar estado de cuenta")
        }
    }
}
