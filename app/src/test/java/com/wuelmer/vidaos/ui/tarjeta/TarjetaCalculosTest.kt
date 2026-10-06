package com.wuelmer.vidaos.ui.tarjeta

import com.wuelmer.vidaos.data.EstadoCuenta
import com.wuelmer.vidaos.data.Movimiento
import com.wuelmer.vidaos.data.OperacionTarjeta
import com.wuelmer.vidaos.data.OrigenPago
import com.wuelmer.vidaos.data.TipoMovimiento
import com.wuelmer.vidaos.data.TipoOperacionTarjeta
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class TarjetaCalculosTest {

    private val estado = EstadoCuenta(
        id = 1,
        fechaEstado = LocalDate.of(2026, 3, 20),
        periodoDesde = LocalDate.of(2026, 2, 21),
        periodoHasta = LocalDate.of(2026, 3, 20),
        pagarHasta = LocalDate.of(2026, 4, 5),
        totalFacturado = 100_000,
        montoMinimo = 9_000,
        cupoTotal = 800_000,
        cupoUtilizado = 130_000,
        cupoDisponible = 670_000,
        facturadoAnterior = 0,
        proximoDesde = LocalDate.of(2026, 3, 21),
        proximoHasta = LocalDate.of(2026, 4, 20),
        vencimientos = "30000,15000,0,0",
        importadoEl = LocalDate.of(2026, 3, 22)
    )

    private fun mov(fecha: LocalDate, monto: Long, tipo: TipoMovimiento, origen: OrigenPago = OrigenPago.CREDITO) =
        Movimiento(fecha = fecha, monto = monto, descripcion = "", categoriaId = 1, origen = origen, tipo = tipo)

    private fun op(fecha: LocalDate, valor: Long, tipo: TipoOperacionTarjeta, desc: String = "") =
        OperacionTarjeta(
            estadoId = 2, fecha = fecha, descripcion = desc, tipo = tipo, montoOperacion = valor,
            cuotaActual = 1, cuotasTotal = 1, valorCuota = valor, orden = 0
        )

    private val hoy = LocalDate.of(2026, 3, 30)

    @Test
    fun pagos_soloDespuesDeFacturar() {
        val movs = listOf(
            mov(LocalDate.of(2026, 3, 20), 50_000, TipoMovimiento.PAGO_TARJETA),  // mismo día: ya iba en el total
            mov(LocalDate.of(2026, 3, 25), 60_000, TipoMovimiento.PAGO_TARJETA),
            mov(LocalDate.of(2026, 3, 26), 10_000, TipoMovimiento.GASTO)
        )
        assertEquals(60_000L, pagosAnotados(movs, estado, hasta = null))
        assertEquals(0L, pagosAnotados(movs, estado, hasta = LocalDate.of(2026, 3, 24)))
    }

    @Test
    fun estado_pendienteParcialPagadaVencida() {
        assertEquals(EstadoPago.Pendiente(6), estadoPago(estado, 0, null, hoy))
        assertEquals(EstadoPago.Parcial(20_000, 80_000, minimoCubierto = true, dias = 6), estadoPago(estado, 20_000, null, hoy))
        assertEquals(EstadoPago.Parcial(5_000, 95_000, minimoCubierto = false, dias = 6), estadoPago(estado, 5_000, null, hoy))
        assertEquals(EstadoPago.Pagada(100_000, manual = false), estadoPago(estado, 100_000, null, hoy))
        assertEquals(EstadoPago.Vencida(20_000, 80_000), estadoPago(estado, 20_000, null, LocalDate.of(2026, 4, 6)))
    }

    @Test
    fun estado_manualYConfirmada() {
        assertEquals(EstadoPago.Pagada(0, manual = true), estadoPago(estado.copy(pagadaManual = true), 0, null, hoy))
        assertEquals(EstadoPago.Confirmada, estadoPago(estado, 0, pagadoSegunBanco = 100_000, hoy = hoy))
        // El banco informa menos que el total: no se confirma.
        assertEquals(EstadoPago.Pendiente(6), estadoPago(estado, 0, pagadoSegunBanco = 40_000, hoy = hoy))
        assertEquals(EstadoPago.SinDeuda, estadoPago(estado.copy(totalFacturado = -5_000), 0, null, hoy))
    }

    @Test
    fun pagosConfirmados_delaFacturaSiguiente() {
        val ops = listOf(
            op(LocalDate.of(2026, 3, 18), -40_000, TipoOperacionTarjeta.PAGO),  // antes de facturar: era de la anterior
            op(LocalDate.of(2026, 4, 2), -100_000, TipoOperacionTarjeta.PAGO),
            op(LocalDate.of(2026, 4, 3), 5_000, TipoOperacionTarjeta.COMPRA)
        )
        assertEquals(100_000L, pagosConfirmados(estado, ops))
    }

    @Test
    fun periodoActual_sumaGastosCuotasYComision() {
        val movs = listOf(
            mov(LocalDate.of(2026, 3, 20), 7_000, TipoMovimiento.GASTO),                   // período anterior
            mov(LocalDate.of(2026, 3, 21), 10_000, TipoMovimiento.GASTO),
            mov(LocalDate.of(2026, 4, 20), 2_000, TipoMovimiento.GASTO),
            mov(LocalDate.of(2026, 3, 22), 50_000, TipoMovimiento.GASTO, OrigenPago.DEBITO), // no es crédito
            mov(LocalDate.of(2026, 3, 23), 1_000, TipoMovimiento.INGRESO)
        )
        val ops = listOf(op(estado.fechaEstado, 4_500, TipoOperacionTarjeta.CARGO, "COMISION ADMINISTRACION MENSUAL"))
        val p = periodoActual(estado, ops, movs)
        assertEquals(12_000L, p.gastosCredito)
        assertEquals(30_000L, p.cuotas)
        assertEquals(4_500L, p.comision)
        assertEquals(46_500L, p.estimado)
    }

    @Test
    fun cupo_bajaConGastosYSubeConPagos() {
        val movs = listOf(
            mov(LocalDate.of(2026, 3, 25), 70_000, TipoMovimiento.GASTO),
            mov(LocalDate.of(2026, 3, 26), 100_000, TipoMovimiento.PAGO_TARJETA)
        )
        assertEquals(700_000L, cupoDisponibleEstimado(estado, movs))
        // Nunca pasa del cupo total.
        val muchoPago = listOf(mov(LocalDate.of(2026, 3, 26), 900_000, TipoMovimiento.PAGO_TARJETA))
        assertEquals(800_000L, cupoDisponibleEstimado(estado, muchoPago))
    }

    @Test
    fun cupo_yaLaPagueLiberaElTotalFacturado() {
        val movs = listOf(mov(LocalDate.of(2026, 3, 25), 70_000, TipoMovimiento.GASTO))
        val pagada = estado.copy(pagadaManual = true)
        // 670.000 − 70.000 + 100.000 facturado
        assertEquals(700_000L, cupoDisponibleEstimado(pagada, movs))
        // Si además se anotó un pago mayor, cuenta ese.
        val conPago = movs + mov(LocalDate.of(2026, 3, 26), 120_000, TipoMovimiento.PAGO_TARJETA)
        assertEquals(720_000L, cupoDisponibleEstimado(pagada, conPago))
    }

    @Test
    fun vencimientos_porMes() {
        assertEquals(listOf("abr" to 30_000L, "may" to 15_000L, "jun" to 0L, "jul" to 0L), mesesVencimientos(estado))
    }

    @Test
    fun nombres_deCargos() {
        assertEquals("Comisión de administración", nombreCargo("COMISION ADMINISTRACION MENSUAL"))
        assertEquals("Compras internacionales (en dólares)", nombreCargo("TRASPASO DEUDA INTERNACIONAL"))
        assertEquals("Seguro desgravamen", nombreCargo("SEGURO DESGRAVAMEN"))
    }
}
