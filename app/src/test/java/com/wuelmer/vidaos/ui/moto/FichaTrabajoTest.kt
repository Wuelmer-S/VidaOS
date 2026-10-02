package com.wuelmer.vidaos.ui.moto

import com.wuelmer.vidaos.data.AjusteTrabajo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FichaTrabajoTest {

    private fun ajuste(llave: String?, torque: Int?, verificado: Boolean = false) =
        AjusteTrabajo(trabajoId = 1, parte = "Tapón", llave = llave, torqueNm = torque, verificado = verificado)

    @Test
    fun lineas_ignoraVaciasYEspacios() {
        assertEquals(listOf("Uno", "Dos"), lineas("  Uno \n\n   \nDos\n"))
        assertEquals(emptyList<String>(), lineas(""))
    }

    @Test
    fun porConfirmar_sinLlaveONoVerificado() {
        assertTrue(porConfirmar(ajuste(null, 59, verificado = true)))
        assertTrue(porConfirmar(ajuste("17 mm", 20)))
        assertFalse(porConfirmar(ajuste("17 mm", 20, verificado = true)))
    }

    @Test
    fun describir() {
        assertEquals("Dado 17 mm · 20 N·m", describirAjuste(ajuste("Dado 17 mm", 20)))
        assertEquals("Llave por medir · 59 N·m", describirAjuste(ajuste(null, 59)))
        assertEquals("Dado 8 mm", describirAjuste(ajuste("Dado 8 mm", null)))
    }

    @Test
    fun resumen() {
        assertEquals("Sin pernos que ajustar", resumenAjustes(emptyList()))
        assertEquals("1 perno · 1 por confirmar", resumenAjustes(listOf(ajuste("17 mm", 20))))
        assertEquals(
            "2 pernos · todo verificado",
            resumenAjustes(listOf(ajuste("17 mm", 20, true), ajuste("8 mm", 10, true)))
        )
    }
}
