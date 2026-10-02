package com.wuelmer.vidaos.ui.moto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FormularioMotoTest {

    private val hoy = LocalDate.of(2026, 10, 2)

    @Test
    fun descomponer_usaLaUnidadMasGrandeExacta() {
        assertEquals(2 to UnidadTiempo.ANIOS, descomponerDias(730))
        assertEquals(6 to UnidadTiempo.MESES, descomponerDias(180))
        assertEquals(7 to UnidadTiempo.DIAS, descomponerDias(7))
        assertEquals(45 to UnidadTiempo.DIAS, descomponerDias(45))
    }

    @Test
    fun descomponerYVolver_daLosMismosDias() {
        listOf(7, 30, 180, 365, 730, 1460, 400).forEach { dias ->
            val (n, unidad) = descomponerDias(dias)
            assertEquals(dias, n * unidad.dias)
        }
    }

    @Test
    fun leerPositivo_casos() {
        assertEquals(CampoNumero.Vacio, leerPositivo("  "))
        assertEquals(CampoNumero.Invalido, leerPositivo("0"))
        assertEquals(CampoNumero.Invalido, leerPositivo("abc"))
        assertEquals(CampoNumero.Valor(3000), leerPositivo(" 3000 "))
    }

    @Test
    fun tipo_soloKm() {
        val r = validarTipo(" Cadena ", "500", "", UnidadTiempo.MESES).getOrThrow()
        assertEquals(IntervaloValido("Cadena", 500, null), r)
    }

    @Test
    fun tipo_kmYMeses_convierteADias() {
        val r = validarTipo("Aceite", "3000", "6", UnidadTiempo.MESES).getOrThrow()
        assertEquals(180, r.cadaDias)
    }

    @Test
    fun tipo_sinNombre_oSinIntervalo_esError() {
        assertTrue(validarTipo("  ", "500", "", UnidadTiempo.DIAS).isFailure)
        assertTrue(validarTipo("Lavado", "", "", UnidadTiempo.DIAS).isFailure)
        assertTrue(validarTipo("Lavado", "0", "", UnidadTiempo.DIAS).isFailure)
    }

    @Test
    fun odometro_soloSiEsHoyYConMasKm() {
        assertTrue(debeActualizarOdometro(41800, hoy, 41736, hoy))
        assertTrue(debeActualizarOdometro(41800, hoy, null, hoy))
        assertFalse(debeActualizarOdometro(41736, hoy, 41736, hoy))
        assertFalse(debeActualizarOdometro(41800, hoy.minusDays(1), 41736, hoy))
    }
}
