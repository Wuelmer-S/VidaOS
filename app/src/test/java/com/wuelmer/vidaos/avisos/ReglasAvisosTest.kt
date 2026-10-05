package com.wuelmer.vidaos.avisos

import com.wuelmer.vidaos.data.RegistroMantencion
import com.wuelmer.vidaos.data.TipoMantencion
import com.wuelmer.vidaos.ui.moto.calcularEstado
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class ReglasAvisosTest {

    private val hoy = LocalDate.of(2026, 10, 5)
    private val aceite = TipoMantencion(id = 1, nombre = "Aceite", icono = "🛢️", cadaKm = 3000, cadaDias = null)

    // Registro hecho a los 40.000 km: el km actual define cuánto se avanzó.
    private fun estado(kmActual: Int, registroId: Long = 10, tipo: TipoMantencion = aceite) =
        calcularEstado(tipo, RegistroMantencion(id = registroId, tipoId = tipo.id, fecha = hoy, km = 40_000), kmActual, hoy)

    @Test
    fun nivel_segunProgreso() {
        assertNull(nivelAviso(estado(41_000)))                       // 33 %
        assertEquals(NivelAviso.MITAD, nivelAviso(estado(41_500)))   // 50 %
        assertEquals(NivelAviso.URGENTE, nivelAviso(estado(42_700))) // 90 %
        assertEquals(NivelAviso.VENCIDA, nivelAviso(estado(43_000))) // 100 %
    }

    @Test
    fun nivel_sinRegistroNoAvisa() {
        assertNull(nivelAviso(calcularEstado(aceite, null, 50_000, hoy)))
    }

    @Test
    fun nivel_usaElPctDeAvisoDelTipo() {
        val tipo = aceite.copy(avisarAlPct = 80)
        assertEquals(NivelAviso.URGENTE, nivelAviso(estado(42_400, tipo = tipo))) // 80 %
    }

    @Test
    fun mitad_seAvisaUnaSolaVez() {
        val primera = avisosPendientes(listOf(estado(41_600)), emptySet())
        assertEquals(listOf(NivelAviso.MITAD), primera.avisos.map { it.nivel })

        val segunda = avisosPendientes(listOf(estado(41_700)), primera.enviados)
        assertTrue(segunda.avisos.isEmpty())
    }

    @Test
    fun salto_directoAl90_noAvisaEl50() {
        val r = avisosPendientes(listOf(estado(42_800)), emptySet())
        assertEquals(listOf(NivelAviso.URGENTE), r.avisos.map { it.nivel })
        assertTrue("1:10:MITAD" in r.enviados)

        // Después del 50 % avisado, llegar al 90 % sí avisa.
        val despues = avisosPendientes(listOf(estado(42_800)), setOf("1:10:MITAD"))
        assertEquals(listOf(NivelAviso.URGENTE), despues.avisos.map { it.nivel })
    }

    @Test
    fun vencida_seRepiteCadaDia() {
        val primera = avisosPendientes(listOf(estado(43_200)), emptySet())
        val segunda = avisosPendientes(listOf(estado(43_300)), primera.enviados)
        assertEquals(listOf(NivelAviso.VENCIDA), segunda.avisos.map { it.nivel })
    }

    @Test
    fun registrarDeNuevo_reiniciaLosAvisos() {
        val viejo = avisosPendientes(listOf(estado(41_600, registroId = 10)), emptySet())
        // Nuevo registro (id 11): a los mismos km relativos vuelve a avisar el 50 %, y se olvida lo del registro viejo.
        val nuevo = avisosPendientes(listOf(estado(41_600, registroId = 11)), viejo.enviados)
        assertEquals(listOf(NivelAviso.MITAD), nuevo.avisos.map { it.nivel })
        assertEquals(setOf("1:11:MITAD"), nuevo.enviados)
    }

    @Test
    fun avisos_lasMasUrgentesPrimero() {
        val cadena = TipoMantencion(id = 2, nombre = "Cadena", icono = "⛓️", cadaKm = 500, cadaDias = null)
        val r = avisosPendientes(listOf(estado(41_500), estado(40_600, tipo = cadena)), emptySet())
        assertEquals(listOf(NivelAviso.VENCIDA, NivelAviso.MITAD), r.avisos.map { it.nivel })
    }

    @Test
    fun textos() {
        val aviso = AvisoMantencion(estado(42_700), NivelAviso.URGENTE)
        assertEquals("⚠️ Aceite: se acerca", tituloAviso(aviso))
        assertEquals("Faltan 300 km. Ve preparando lo necesario.", textoAviso(aviso))
    }

    @Test
    fun recordatorioKm_a7Dias() {
        assertTrue(necesitaRecordatorioKm(null, hoy))
        assertFalse(necesitaRecordatorioKm(hoy.minusDays(6), hoy))
        assertTrue(necesitaRecordatorioKm(hoy.minusDays(7), hoy))
        assertEquals(
            "Llevas 9 días sin anotar los km. Míralos antes de salir.",
            textoRecordatorioKm(hoy.minusDays(9), hoy)
        )
    }

    @Test
    fun proximaEjecucion_hoySiNoPasoSinoManana() {
        val antes = LocalDateTime.of(2026, 10, 5, 6, 0)
        assertEquals(LocalDateTime.of(2026, 10, 5, 7, 30), proximaEjecucion(antes, 7 * 60 + 30))
        val justo = LocalDateTime.of(2026, 10, 5, 7, 30)
        assertEquals(LocalDateTime.of(2026, 10, 6, 7, 30), proximaEjecucion(justo, 7 * 60 + 30))
        val noche = LocalDateTime.of(2026, 10, 5, 21, 0)
        assertEquals(LocalDateTime.of(2026, 10, 6, 20, 0), proximaEjecucion(noche, 20 * 60))
    }

    @Test
    fun formatoHora() {
        assertEquals("07:30", formatearHora(450))
        assertEquals("20:00", formatearHora(1200))
    }
}
