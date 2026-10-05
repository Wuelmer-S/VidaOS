package com.wuelmer.vidaos.ui.historial

import com.wuelmer.vidaos.data.Movimiento
import com.wuelmer.vidaos.data.MovimientoConCategoria
import com.wuelmer.vidaos.data.OrigenPago
import com.wuelmer.vidaos.data.TipoMovimiento
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class FiltroOrigenTest {

    private val hoy = LocalDate.of(2026, 10, 5)

    private fun mov(
        id: Long,
        monto: Long,
        origen: OrigenPago,
        tipo: TipoMovimiento = TipoMovimiento.GASTO,
        fecha: LocalDate = hoy
    ) = MovimientoConCategoria(
        movimiento = Movimiento(
            id = id, fecha = fecha, monto = monto, descripcion = "", categoriaId = 1, origen = origen, tipo = tipo
        ),
        categoriaNombre = "Comida",
        categoriaColor = null
    )

    private val movimientos = listOf(
        mov(1, 10_000, OrigenPago.CREDITO),
        mov(2, 5_000, OrigenPago.DEBITO),
        mov(3, 2_000, OrigenPago.CREDITO, fecha = LocalDate.of(2026, 9, 30)),  // mes anterior
        mov(4, 500_000, OrigenPago.CREDITO, tipo = TipoMovimiento.INGRESO),   // no es gasto
        mov(5, 3_000, OrigenPago.EFECTIVO)
    )

    @Test
    fun todos_noFiltra() {
        assertEquals(movimientos, filtrarPorOrigen(movimientos, null))
    }

    @Test
    fun filtra_porOrigen() {
        assertEquals(listOf(1L, 3L, 4L), filtrarPorOrigen(movimientos, OrigenPago.CREDITO).map { it.movimiento.id })
        assertEquals(listOf(5L), filtrarPorOrigen(movimientos, OrigenPago.EFECTIVO).map { it.movimiento.id })
    }

    @Test
    fun gastado_soloGastosDelMes() {
        assertEquals(10_000L, gastadoEnElMes(filtrarPorOrigen(movimientos, OrigenPago.CREDITO), hoy))
        assertEquals(18_000L, gastadoEnElMes(movimientos, hoy))
        assertEquals(0L, gastadoEnElMes(emptyList(), hoy))
    }
}
