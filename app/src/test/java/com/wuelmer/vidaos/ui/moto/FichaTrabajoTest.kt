package com.wuelmer.vidaos.ui.moto

import com.wuelmer.vidaos.data.AjusteTrabajo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FichaTrabajoTest {

    private fun ajuste(llave: String?, torque: Int?, verificado: Boolean = false, parte: String = "Tapón") =
        AjusteTrabajo(trabajoId = 1, parte = parte, llave = llave, torqueNm = torque, verificado = verificado)

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
    fun describir_verificadoMuestraLoQueUso() {
        assertEquals("Usaste 17 mm · 20 N·m", describirAjuste(ajuste("17 mm", 20, verificado = true)))
        assertEquals("Usaste 8 mm", describirAjuste(ajuste("8 mm", null, verificado = true)))
    }

    @Test
    fun describir_porConfirmarSoloTorque() {
        assertEquals("Torque 20 N·m", describirAjuste(ajuste("Dado 17 mm", 20)))
        assertEquals("Torque 59 N·m", describirAjuste(ajuste(null, 59)))
        assertEquals("Sin torque indicado", describirAjuste(ajuste(null, null)))
    }

    @Test
    fun referencia_soloSiNoEstaVerificada() {
        assertEquals("Dado 17 mm", referenciaLlave(ajuste("Dado 17 mm", 20)))
        assertNull(referenciaLlave(ajuste("17 mm", 20, verificado = true)))
        assertNull(referenciaLlave(ajuste(null, 20)))
    }

    @Test
    fun normalizarMedida_agregaMmSiSoloEsNumero() {
        assertEquals("17 mm", normalizarMedida(" 17 "))
        assertEquals("10,5 mm", normalizarMedida("10,5"))
        assertEquals("Dado 17 mm", normalizarMedida("Dado 17 mm"))
        assertEquals("1/2 pulgada", normalizarMedida("1/2 pulgada"))
    }

    @Test
    fun notaPaso_buscaElPernoSinImportarMayusculas() {
        val ajustes = listOf(
            ajuste("17 mm", 20, verificado = true, parte = "Tapón de drenaje"),
            ajuste(null, 59, parte = "Tuerca del eje trasero")
        )
        assertEquals("🔧 Usaste 17 mm", notaPaso("Sacar el tapón de drenaje y escurrir", ajustes))
        assertEquals("🔧 Medida: anótala arriba en Llaves y torques", notaPaso("Aflojar la tuerca del eje trasero", ajustes))
        assertNull(notaPaso("Secar bien con un paño", ajustes))
    }

    @Test
    fun resumen() {
        assertEquals("Sin pernos que ajustar", resumenAjustes(emptyList()))
        assertEquals("1 perno · 1 por anotar", resumenAjustes(listOf(ajuste("17 mm", 20))))
        assertEquals(
            "2 pernos · medidas anotadas",
            resumenAjustes(listOf(ajuste("17 mm", 20, true), ajuste("8 mm", 10, true)))
        )
    }
}
