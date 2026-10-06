package com.wuelmer.vidaos.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

// Datos inventados con el mismo formato que entrega PdfBox para el PDF de Banco de Chile / Edwards.
class LectorEstadoCuentaTest {

    private val texto = """
           1 de 3
        ESTADO DE CUENTA NACIONAL DE TARJETA DE CRÉDITO
        NOMBRE DEL TITULAR JUAN PEREZ
        FECHA ESTADO DE CUENTA 20/03/2026
        CUPO TOTAL CUPO UTILIZADO CUPO DISPONIBLE  CAE PREPAGO
          CUPO TOTAL $ 800.000 $ 150.000 $ 650.000
          CUPO TOTAL AVANCE EN EFECTIVO $ 400.000 $ 0 $ 650.000 200,00 %
        Infórmese sobre las entidades autorizadas   PERÍODO FACTURADO 21/02/2026 20/03/2026
        inscritas en los Registros de Emisores.   PAGAR HASTA 05/04/2026
          MONTO FACTURADO A PAGAR (PERÍODO ANTERIOR) A $ 100.000
        1.TOTAL OPERACIONES
        25/02/26 250200000000  MONTO CANCELADO  $        -100.000  $        -100.000 01/01  $        -100.000
                                                 TOTAL PAGOS A LA CUENTA B  $        -100.000
         SANTIAGO 22/02/26 220211111111  SUPERMERCADO UNO       SANTIAGO  $          40.000  $          40.000 01/01  $          40.000
         PAGAR HASTA  MONTO TOTAL FACTURADO A PAGAR  PAGAR HASTA Timbre  MONTO TOTAL FACTURADO A PAGAR
         MONTO MÍNIMO A PAGAR  MONTO CANCELADO  MONTO MÍNIMO A PAGAR  MONTO CANCELADO
         LAS CONDES 10/03/26 100322222222  MERCADOPAGO*TIENDA     LAS CONDES  $          25.500  $          25.500 01/01  $          25.500
                                                 TOTAL TRANSACCIONES EN UNA CUOTA D  $          65.500
         SANTIAGO 15/01/26 150133333333  TALLER DE MOTOS        TASA INT.  0,00%  $          90.000  $          90.000 02/03  $          30.000
                                                 TOTAL TRANSACCIONES EN CUOTAS E  $          30.000
        3.CARGOS, COMISIONES, IMPUESTOS Y ABONOS
        20/03/26 200300000000  COMISION ADMINISTRACION MENSUAL  $           4.500  $           4.500 01/01  $           4.500
        20/03/26 200300000000  INTERESES ROTATIVOS  $              50  $              50 01/01  $              50
         MONTO TOTAL FACTURADO A PAGAR ( A + B +C+D+ E + F +G )  $         100.050 EVOLUCIÓN MONTOS
         MONTO MÍNIMO A PAGAR  $          9.000
        VENCIMIENTO PRÓXIMOS 4 MESES Monto Facturado
        ACTUAL ABRIL MAYO JUNIO JULIO 235.070
         ${'$'}         30.000  ${'$'}         30.000  ${'$'}              0  ${'$'}              0  ${'$'}              0 117.535
         PRÓXIMO PERÍODO DE FACTURACIÓN 21/03/2026 20/04/2026
           1 de 1
        ESTADO DE CUENTA INTERNACIONAL DE TARJETA DE CREDITO
        0909 24011346251100145833594 08/03/26 SERVICIO EXTRANJERO US          10,00          10,00
    """.trimIndent()

    private fun leerOk(): FacturaLeida {
        val r = LectorEstadoCuenta.leer(texto, hoy = LocalDate.of(2026, 3, 22))
        assertTrue("Debió leerse: $r", r is ResultadoLectura.Ok)
        return (r as ResultadoLectura.Ok).factura
    }

    @Test
    fun datosPrincipales() {
        val e = leerOk().estado
        assertEquals(LocalDate.of(2026, 3, 20), e.fechaEstado)
        assertEquals(LocalDate.of(2026, 2, 21), e.periodoDesde)
        assertEquals(LocalDate.of(2026, 3, 20), e.periodoHasta)
        assertEquals(LocalDate.of(2026, 4, 5), e.pagarHasta)
        assertEquals(100_050L, e.totalFacturado)
        assertEquals(9_000L, e.montoMinimo)
        assertEquals(800_000L, e.cupoTotal)
        assertEquals(150_000L, e.cupoUtilizado)
        assertEquals(650_000L, e.cupoDisponible)
        assertEquals(100_000L, e.facturadoAnterior)
        assertEquals(LocalDate.of(2026, 3, 21), e.proximoDesde)
        assertEquals(LocalDate.of(2026, 4, 20), e.proximoHasta)
        assertEquals("30000,0,0,0", e.vencimientos)
    }

    @Test
    fun operaciones_porTipo() {
        val ops = leerOk().operaciones
        assertEquals(
            listOf(
                TipoOperacionTarjeta.PAGO,
                TipoOperacionTarjeta.COMPRA,
                TipoOperacionTarjeta.COMPRA,
                TipoOperacionTarjeta.CUOTA,
                TipoOperacionTarjeta.CARGO,
                TipoOperacionTarjeta.CARGO
            ),
            ops.map { it.tipo }
        )
        val cuota = ops.first { it.tipo == TipoOperacionTarjeta.CUOTA }
        assertEquals("TALLER DE MOTOS", cuota.descripcion)
        assertEquals(90_000L, cuota.montoOperacion)
        assertEquals(2, cuota.cuotaActual)
        assertEquals(3, cuota.cuotasTotal)
        assertEquals(30_000L, cuota.valorCuota)
        assertEquals("SUPERMERCADO UNO SANTIAGO", ops[1].descripcion)
        assertEquals(LocalDate.of(2026, 2, 22), ops[1].fecha)
        assertEquals(-100_000L, ops[0].valorCuota)
    }

    @Test
    fun laSumaCuadraConElTotal() {
        val f = leerOk()
        assertEquals(100_050L, f.sumaCalculada)
        assertTrue(f.cuadra)
    }

    @Test
    fun ignoraLaParteInternacional() {
        assertFalse(leerOk().operaciones.any { it.descripcion.contains("EXTRANJERO") })
    }

    @Test
    fun otroDocumento_daError() {
        assertTrue(LectorEstadoCuenta.leer("CARTOLA CUENTA CORRIENTE") is ResultadoLectura.Error)
    }

    @Test
    fun faltanDatos_daError() {
        val sinTotal = texto.lines().filterNot { it.contains("MONTO TOTAL FACTURADO A PAGAR (") }.joinToString("\n")
        assertTrue(LectorEstadoCuenta.leer(sinTotal) is ResultadoLectura.Error)
    }
}
