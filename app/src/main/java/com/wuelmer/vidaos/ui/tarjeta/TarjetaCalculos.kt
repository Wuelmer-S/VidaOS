package com.wuelmer.vidaos.ui.tarjeta

import com.wuelmer.vidaos.data.EstadoCuenta
import com.wuelmer.vidaos.data.Movimiento
import com.wuelmer.vidaos.data.OperacionTarjeta
import com.wuelmer.vidaos.data.OrigenPago
import com.wuelmer.vidaos.data.TipoMovimiento
import com.wuelmer.vidaos.data.TipoOperacionTarjeta
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

// Estado del pago de una factura, de mejor a peor.
sealed interface EstadoPago {
    data object SinDeuda : EstadoPago                                 // total ≤ 0 (pagaste de más antes de facturar)
    data object Confirmada : EstadoPago                               // la factura siguiente trae pagos que cubren el total
    data class Pagada(val pagado: Long, val manual: Boolean) : EstadoPago
    data class Parcial(val pagado: Long, val falta: Long, val minimoCubierto: Boolean, val dias: Long) : EstadoPago
    data class Pendiente(val dias: Long) : EstadoPago
    data class Vencida(val pagado: Long, val falta: Long) : EstadoPago
}

// Pagos a la tarjeta anotados en la app ("Pago tarjeta") después de la fecha de facturación.
// Los anteriores ya vienen descontados dentro del total de esa factura.
fun pagosAnotados(movimientos: List<Movimiento>, estado: EstadoCuenta, hasta: LocalDate?): Long =
    movimientos
        .filter { it.tipo == TipoMovimiento.PAGO_TARJETA && it.fecha.isAfter(estado.fechaEstado) }
        .filter { hasta == null || !it.fecha.isAfter(hasta) }
        .sumOf { it.monto }

// Pagos que informa el banco en la factura siguiente, hechos después de facturar esta.
fun pagosConfirmados(estado: EstadoCuenta, opsSiguiente: List<OperacionTarjeta>): Long =
    -opsSiguiente
        .filter { it.tipo == TipoOperacionTarjeta.PAGO && it.fecha.isAfter(estado.fechaEstado) }
        .sumOf { it.valorCuota }

fun estadoPago(
    estado: EstadoCuenta,
    pagadoEnApp: Long,
    // null = todavía no llega la factura siguiente
    pagadoSegunBanco: Long?,
    hoy: LocalDate
): EstadoPago {
    val total = estado.totalFacturado
    if (total <= 0) return EstadoPago.SinDeuda
    if (pagadoSegunBanco != null && pagadoSegunBanco >= total) return EstadoPago.Confirmada
    if (estado.pagadaManual) return EstadoPago.Pagada(pagadoEnApp, manual = true)
    if (pagadoEnApp >= total) return EstadoPago.Pagada(pagadoEnApp, manual = false)
    val dias = ChronoUnit.DAYS.between(hoy, estado.pagarHasta)
    val falta = total - pagadoEnApp
    return when {
        dias < 0 -> EstadoPago.Vencida(pagadoEnApp, falta)
        pagadoEnApp > 0 -> EstadoPago.Parcial(pagadoEnApp, falta, pagadoEnApp >= estado.montoMinimo, dias)
        else -> EstadoPago.Pendiente(dias)
    }
}

data class PeriodoActual(
    val desde: LocalDate,
    val hasta: LocalDate?,
    val gastosCredito: Long,
    val cuotas: Long,
    val comision: Long
) {
    val estimado: Long get() = gastosCredito + cuotas + comision
}

// El período que se está gastando ahora: desde el día siguiente a la última facturación hasta la próxima.
fun periodoActual(ultimo: EstadoCuenta, opsUltimo: List<OperacionTarjeta>, movimientos: List<Movimiento>): PeriodoActual {
    val desde = ultimo.proximoDesde ?: ultimo.fechaEstado.plusDays(1)
    val hasta = ultimo.proximoHasta
    val gastos = movimientos
        .filter { it.tipo == TipoMovimiento.GASTO && it.origen == OrigenPago.CREDITO }
        .filter { !it.fecha.isBefore(desde) && (hasta == null || !it.fecha.isAfter(hasta)) }
        .sumOf { it.monto }
    return PeriodoActual(
        desde = desde,
        hasta = hasta,
        gastosCredito = gastos,
        cuotas = vencimientos(ultimo).firstOrNull() ?: 0,
        comision = opsUltimo.filter { it.tipo == TipoOperacionTarjeta.CARGO && it.descripcion.contains("COMISION") }
            .sumOf { it.valorCuota }
    )
}

// Cupo disponible de la factura, ajustado con lo anotado después: los gastos con crédito lo bajan y los pagos lo suben.
// Con "Ya la pagué" se cuenta pagado al menos el total facturado (ese pago no está anotado en la app).
fun cupoDisponibleEstimado(ultimo: EstadoCuenta, movimientos: List<Movimiento>): Long {
    val despues = movimientos.filter { it.fecha.isAfter(ultimo.fechaEstado) }
    val gastos = despues.filter { it.tipo == TipoMovimiento.GASTO && it.origen == OrigenPago.CREDITO }.sumOf { it.monto }
    val anotados = despues.filter { it.tipo == TipoMovimiento.PAGO_TARJETA }.sumOf { it.monto }
    val pagos = if (ultimo.pagadaManual) maxOf(anotados, ultimo.totalFacturado) else anotados
    return (ultimo.cupoDisponible - gastos + pagos).coerceIn(0, ultimo.cupoTotal)
}

fun vencimientos(estado: EstadoCuenta): List<Long> =
    estado.vencimientos.split(",").mapNotNull { it.trim().toLongOrNull() }

// "oct $23.334" para los 4 meses siguientes a la facturación (los que tengan monto).
fun mesesVencimientos(estado: EstadoCuenta): List<Pair<String, Long>> =
    vencimientos(estado).mapIndexed { i, monto ->
        estado.fechaEstado.plusMonths(i + 1L).month.getDisplayName(TextStyle.SHORT, Locale.forLanguageTag("es-CL"))
            .trimEnd('.') to monto
    }

// Nombres más claros para los cargos del banco.
fun nombreCargo(descripcion: String): String = when {
    descripcion.contains("TRASPASO DEUDA INTERNACIONAL") -> "Compras internacionales (en dólares)"
    descripcion.contains("COMISION") -> "Comisión de administración"
    descripcion.contains("INTERESES") -> "Intereses"
    descripcion.contains("IMPUESTO") -> "Impuesto"
    else -> descripcion.lowercase().replaceFirstChar { it.uppercase() }
}
