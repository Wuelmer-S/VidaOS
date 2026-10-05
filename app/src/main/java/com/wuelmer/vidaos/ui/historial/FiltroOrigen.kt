package com.wuelmer.vidaos.ui.historial

import com.wuelmer.vidaos.data.MovimientoConCategoria
import com.wuelmer.vidaos.data.OrigenPago
import com.wuelmer.vidaos.data.TipoMovimiento
import java.time.LocalDate
import java.time.YearMonth

// origen = null es "Todos".
fun filtrarPorOrigen(movimientos: List<MovimientoConCategoria>, origen: OrigenPago?): List<MovimientoConCategoria> =
    if (origen == null) movimientos else movimientos.filter { it.movimiento.origen == origen }

// Suma solo los gastos del mes de `hoy` (los ingresos, pagos de tarjeta e internos no cuentan como gasto).
fun gastadoEnElMes(movimientos: List<MovimientoConCategoria>, hoy: LocalDate): Long {
    val mes = YearMonth.from(hoy)
    return movimientos
        .filter { it.movimiento.tipo == TipoMovimiento.GASTO && YearMonth.from(it.movimiento.fecha) == mes }
        .sumOf { it.movimiento.monto }
}
