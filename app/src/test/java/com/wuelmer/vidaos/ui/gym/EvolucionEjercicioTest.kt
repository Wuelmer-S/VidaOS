package com.wuelmer.vidaos.ui.gym

import com.wuelmer.vidaos.data.EjercicioGym
import com.wuelmer.vidaos.data.PuntoEjercicio
import com.wuelmer.vidaos.data.TipoEjercicio
import com.wuelmer.vidaos.data.UnidadPeso
import com.wuelmer.vidaos.data.ZonaEjercicio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class EvolucionEjercicioTest {

    private val press = EjercicioGym(2, "Press banca", TipoEjercicio.CON_PESO, ZonaEjercicio.SUPERIOR)
    private val dominadas = EjercicioGym(9, "Dominadas", TipoEjercicio.PESO_CORPORAL, ZonaEjercicio.SUPERIOR)
    private val plancha = EjercicioGym(7, "Plancha", TipoEjercicio.TIEMPO, ZonaEjercicio.CORE)

    private fun punto(id: Long, dia: Int, kg: Double? = null, reps: Int? = null, seg: Int? = null) =
        PuntoEjercicio(id, LocalDate.of(2026, 6, 1).plusDays(dia.toLong()), kg, reps, seg)

    @Test
    fun conPeso_cambioDesdeLaPrimera() {
        val e = evolucionEjercicio(press, listOf(punto(1, 0, 50.0), punto(2, 7, 55.0), punto(3, 14, 62.5)), UnidadPeso.KG)
        assertEquals(listOf(50.0, 55.0, 62.5), e.puntos.map { it.valor })
        assertEquals(62.5, e.actual!!, 0.0)
        assertEquals(12.5, e.cambio!!, 0.0)
        assertEquals("kg", e.unidadTexto)
    }

    @Test
    fun conPeso_enLibras_convierteSoloAlMostrar() {
        val e = evolucionEjercicio(press, listOf(punto(1, 0, UnidadPeso.LB.aKg(135.0)), punto(2, 7, UnidadPeso.LB.aKg(145.0))), UnidadPeso.LB)
        assertEquals(listOf(135.0, 145.0), e.puntos.map { it.valor })
        assertEquals(10.0, e.cambio!!, 0.0)
        assertEquals("lb", e.unidadTexto)
    }

    @Test
    fun ordenaPorFecha_yDescartaSesionesSinDato() {
        val e = evolucionEjercicio(press, listOf(punto(3, 14, 60.0), punto(1, 0, 50.0), punto(2, 7, null)), UnidadPeso.KG)
        assertEquals(listOf(1L, 3L), e.puntos.map { it.sesionId })
    }

    @Test
    fun pesoCorporal_usaRepsMaximas() {
        val e = evolucionEjercicio(dominadas, listOf(punto(1, 0, reps = 6), punto(2, 7, reps = 9)), UnidadPeso.KG)
        assertEquals("reps", e.unidadTexto)
        assertEquals(3.0, e.cambio!!, 0.0)
    }

    @Test
    fun tiempo_usaSegundos() {
        val e = evolucionEjercicio(plancha, listOf(punto(1, 0, seg = 30), punto(2, 7, seg = 45)), UnidadPeso.KG)
        assertEquals("s", e.unidadTexto)
        assertEquals(15.0, e.cambio!!, 0.0)
    }

    @Test
    fun unaSolaSesion_sinCambio() {
        assertNull(evolucionEjercicio(press, listOf(punto(1, 0, 50.0)), UnidadPeso.KG).cambio)
    }

    @Test
    fun textoCambio_subeBajaIgual() {
        assertEquals("▲ +12,5 kg", textoCambio(12.5, "kg"))
        assertEquals("▼ −2,5 kg", textoCambio(-2.5, "kg"))
        assertEquals("= sin cambio", textoCambio(0.0, "kg"))
    }
}
