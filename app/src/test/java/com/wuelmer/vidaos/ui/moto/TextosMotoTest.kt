package com.wuelmer.vidaos.ui.moto

import com.wuelmer.vidaos.data.RegistroMantencion
import com.wuelmer.vidaos.data.TipoMantencion
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class TextosMotoTest {

    private val hoy = LocalDate.of(2026, 10, 2)
    private val aceite = TipoMantencion(id = 1, nombre = "Aceite", icono = "🛢️", cadaKm = 3000, cadaDias = 180)

    @Test
    fun km_conSeparadorDeMiles() {
        assertEquals("41.736 km", formatearKm(41736))
    }

    @Test
    fun periodos() {
        assertEquals("2 años", formatearPeriodo(730))
        assertEquals("1 año", formatearPeriodo(365))
        assertEquals("6 meses", formatearPeriodo(180))
        assertEquals("7 días", formatearPeriodo(7))
        assertEquals("1 día", formatearPeriodo(1))
    }

    @Test
    fun intervalo() {
        assertEquals("Cada 3.000 km o 6 meses", describirIntervalo(3000, 180))
        assertEquals("Cada 500 km", describirIntervalo(500, null))
        assertEquals("Cada 2 años", describirIntervalo(null, 730))
    }

    @Test
    fun restante_porKmYPasado() {
        val registro = RegistroMantencion(tipoId = 1, fecha = hoy, km = 40000)
        assertEquals("faltan 420 km", describirRestante(calcularEstado(aceite, registro, 42580, hoy)))
        assertEquals("te pasaste por 50 km", describirRestante(calcularEstado(aceite, registro, 43050, hoy)))
    }

    @Test
    fun restante_porTiempo() {
        val registro = RegistroMantencion(tipoId = 1, fecha = hoy.minusDays(190), km = 40000)
        assertEquals("atrasada 10 días", describirRestante(calcularEstado(aceite, registro, 40100, hoy)))
    }

    @Test
    fun restante_sinRegistro() {
        assertEquals("Sin registro todavía", describirRestante(calcularEstado(aceite, null, 41736, hoy)))
    }
}
