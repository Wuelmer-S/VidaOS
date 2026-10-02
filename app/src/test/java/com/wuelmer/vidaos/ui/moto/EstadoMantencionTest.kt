package com.wuelmer.vidaos.ui.moto

import com.wuelmer.vidaos.data.LecturaKm
import com.wuelmer.vidaos.data.RegistroMantencion
import com.wuelmer.vidaos.data.TipoMantencion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class EstadoMantencionTest {

    private val hoy = LocalDate.of(2026, 10, 2)
    private val aceite = TipoMantencion(id = 1, nombre = "Aceite", icono = "🛢️", cadaKm = 3000, cadaDias = 180)
    private val frenos = TipoMantencion(id = 2, nombre = "Frenos", icono = "🛑", cadaKm = null, cadaDias = 730)

    private fun registro(tipo: TipoMantencion, km: Int, fecha: LocalDate) =
        RegistroMantencion(tipoId = tipo.id, fecha = fecha, km = km)

    @Test
    fun sinRegistro_quedaSinRegistroEnVezDeVencida() {
        val estado = calcularEstado(aceite, null, kmActual = 41736, hoy = hoy)
        assertNull(estado.progreso)
        assertEquals(Semaforo.SIN_REGISTRO, estado.semaforo)
    }

    @Test
    fun recienHecha_estaVerdeYFaltaTodo() {
        val estado = calcularEstado(aceite, registro(aceite, 41736, hoy), kmActual = 41736, hoy = hoy)
        assertEquals(0f, estado.progreso!!, 0.0001f)
        assertEquals(3000, estado.kmRestantes)
        assertEquals(180L, estado.diasRestantes)
        assertEquals(Semaforo.VERDE, estado.semaforo)
    }

    @Test
    fun porKm_mandaLoQueOcurraPrimero() {
        // 2.400 km de 3.000 (80 %) en 30 días de 180 (17 %): manda el km.
        val estado = calcularEstado(aceite, registro(aceite, 40000, hoy.minusDays(30)), kmActual = 42400, hoy = hoy)
        assertEquals(0.8f, estado.progreso!!, 0.0001f)
        assertEquals(600, estado.kmRestantes)
        assertEquals(Semaforo.AMARILLO, estado.semaforo)
    }

    @Test
    fun porTiempo_venceAunqueAndePoco() {
        val estado = calcularEstado(aceite, registro(aceite, 40000, hoy.minusDays(200)), kmActual = 40500, hoy = hoy)
        assertEquals(200f / 180f, estado.progreso!!, 0.0001f)
        assertEquals(-20L, estado.diasRestantes)
        assertEquals(Semaforo.ROJO, estado.semaforo)
    }

    @Test
    fun soloPorTiempo_noUsaKm() {
        val estado = calcularEstado(frenos, registro(frenos, 30000, hoy.minusDays(365)), kmActual = 41736, hoy = hoy)
        assertNull(estado.kmRestantes)
        assertEquals(365L, estado.diasRestantes)
        assertEquals(0.5f, estado.progreso!!, 0.001f)
    }

    @Test
    fun sinKmActual_usaSoloElTiempo() {
        val estado = calcularEstado(aceite, registro(aceite, 40000, hoy.minusDays(90)), kmActual = null, hoy = hoy)
        assertNull(estado.kmRestantes)
        assertEquals(0.5f, estado.progreso!!, 0.0001f)
    }

    @Test
    fun kmActualMenorQueElRegistro_cuentaComoCero() {
        val estado = calcularEstado(aceite, registro(aceite, 42000, hoy), kmActual = 41736, hoy = hoy)
        assertEquals(3000, estado.kmRestantes)
    }

    @Test
    fun justoAlLimite_esRojo() {
        assertEquals(Semaforo.ROJO, semaforo(1f))
        assertEquals(Semaforo.AMARILLO, semaforo(0.7f))
        assertEquals(Semaforo.VERDE, semaforo(0.69f))
    }

    @Test
    fun ordenar_vencidasArribaYSinRegistroAlFinal() {
        val sinRegistro = calcularEstado(frenos, null, 41736, hoy)
        val verde = calcularEstado(aceite, registro(aceite, 41000, hoy), 41736, hoy)
        val rojo = calcularEstado(aceite.copy(id = 3, cadaKm = 500), registro(aceite, 41000, hoy), 41736, hoy)
        assertEquals(listOf(rojo, verde, sinRegistro), ordenarPorUrgencia(listOf(sinRegistro, verde, rojo)))
    }

    @Test
    fun ritmo_conUnaSolaLectura_esNull() {
        assertNull(ritmoKmPorDia(listOf(LecturaKm(fecha = hoy, km = 41736)), hoy))
    }

    @Test
    fun ritmo_mismoDia_esNull() {
        val lecturas = listOf(LecturaKm(fecha = hoy, km = 41700), LecturaKm(fecha = hoy, km = 41736))
        assertNull(ritmoKmPorDia(lecturas, hoy))
    }

    @Test
    fun ritmo_ignoraLecturasDeHaceMasDe30Dias() {
        val lecturas = listOf(
            LecturaKm(fecha = hoy.minusDays(60), km = 38000),
            LecturaKm(fecha = hoy.minusDays(10), km = 41236),
            LecturaKm(fecha = hoy, km = 41736)
        )
        assertEquals(50.0, ritmoKmPorDia(lecturas, hoy)!!, 0.0001)
    }

    @Test
    fun diasEstimados_redondeaHaciaArriba() {
        assertEquals(9L, diasEstimados(420, 50.0))
        assertNull(diasEstimados(420, null))
        assertNull(diasEstimados(-50, 50.0))
    }
}
