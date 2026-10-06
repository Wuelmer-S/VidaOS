package com.wuelmer.vidaos.ui.registrargasto

import com.wuelmer.vidaos.data.Movimiento
import com.wuelmer.vidaos.data.OrigenPago
import com.wuelmer.vidaos.data.TipoMovimiento
import com.wuelmer.vidaos.data.TipoPrestamo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class EditarMovimientoTest {

    private val original = Movimiento(
        id = 7,
        fecha = LocalDate.of(2026, 3, 6),
        monto = 1_100,
        descripcion = "comida",
        categoriaId = 1,
        origen = OrigenPago.CREDITO,
        tipo = TipoMovimiento.GASTO,
        esPrestamo = TipoPrestamo.PRESTAMO,
        observaciones = "nota"
    )

    @Test
    fun conMovimiento_rellenaElFormulario() {
        val estado = RegistrarGastoUiState().conMovimiento(original)
        assertEquals("1100", estado.monto)
        assertEquals("comida", estado.descripcion)
        assertEquals(LocalDate.of(2026, 3, 6), estado.fecha)
        assertEquals(OrigenPago.CREDITO, estado.origen)
        assertEquals(TipoMovimiento.GASTO, estado.tipo)
        assertEquals(1L, estado.categoriaId)
        assertTrue(estado.editando)
    }

    @Test
    fun editadoCon_mantieneIdYLoQueElFormularioNoMuestra() {
        val estado = RegistrarGastoUiState().conMovimiento(original).copy(
            fecha = LocalDate.of(2026, 3, 2),
            descripcion = "farmacia",
            origen = OrigenPago.DEBITO
        )
        val editado = original.editadoCon(estado, monto = 4_990, categoriaId = 7)
        assertEquals(
            original.copy(
                fecha = LocalDate.of(2026, 3, 2),
                monto = 4_990,
                descripcion = "farmacia",
                categoriaId = 7,
                origen = OrigenPago.DEBITO
            ),
            editado
        )
    }
}
