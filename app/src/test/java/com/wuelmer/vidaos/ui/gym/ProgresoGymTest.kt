package com.wuelmer.vidaos.ui.gym

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class ProgresoGymTest {

    // Miércoles 30 de septiembre de 2026; su semana empieza el lunes 28.
    private val hoy = LocalDate.of(2026, 9, 30)
    private val lunes = LocalDate.of(2026, 9, 28)

    // Días (0 = lunes) de la semana que empieza `semanasAtras` semanas antes de la actual.
    private fun semana(semanasAtras: Long, vararg dias: Long): List<LocalDate> =
        dias.map { lunes.minusWeeks(semanasAtras).plusDays(it) }

    // --- Racha (casos pedidos) ---

    @Test
    fun sinSesiones_rachaCero() {
        assertEquals(0, calcularRacha(emptyList(), hoy))
    }

    @Test
    fun semanaEnCursoConUna_noRompeLaRacha() {
        val fechas = semana(0, 0) + semana(1, 0, 2, 4) + semana(2, 1, 3, 5)
        assertEquals(2, calcularRacha(fechas, hoy))
    }

    @Test
    fun semanaPasadaConDos_rompeLaRacha() {
        val fechas = semana(1, 0, 2) + semana(2, 0, 2, 4) + semana(3, 0, 2, 4)
        assertEquals(0, calcularRacha(fechas, hoy))
    }

    @Test
    fun dosSesionesElMismoDia_cuentanComoUna() {
        val fechas = semana(1, 0, 0, 2) // lunes dos veces + miércoles = 2 días
        assertEquals(0, calcularRacha(fechas, hoy))
    }

    @Test
    fun semanaActualConTres_sumaUno() {
        assertEquals(1, calcularRacha(semana(0, 0, 1, 2), hoy))
    }

    @Test
    fun semanaActualConTres_masSemanasPrevias_sumaTodas() {
        val fechas = semana(0, 0, 1, 2) + semana(1, 0, 2, 4) + semana(2, 1, 3, 5) + semana(4, 0, 2, 4)
        assertEquals(3, calcularRacha(fechas, hoy)) // la semana 3 atrás está vacía y corta
    }

    @Test
    fun respetaLaMetaConfigurada() {
        val fechas = semana(1, 0, 2) + semana(2, 1, 3)
        assertEquals(2, calcularRacha(fechas, hoy, meta = 2))
    }

    // --- Semana actual ---

    @Test
    fun progreso_marcaLosDiasDeEstaSemana_sinContarLaAnterior() {
        val p = progresoSemana(semana(0, 0, 0, 2) + semana(1, 4), hoy)
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), p.diasConSesion)
        assertEquals(2, p.hechas)
    }

    @Test
    fun diasDisponibles_noCuentaHoySiYaFuiste() {
        // Miércoles con sesión: quedan jueves a domingo = 4.
        assertEquals(4, progresoSemana(semana(0, 2), hoy).diasDisponibles)
        // Miércoles sin sesión: quedan miércoles a domingo = 5.
        assertEquals(5, progresoSemana(emptyList(), hoy).diasDisponibles)
    }

    // --- Mensajes ---

    private fun mensaje(hoy: LocalDate, vararg dias: Long, racha: Boolean = true): String {
        val previas = if (racha) semana(1, 0, 2, 4) else emptyList()
        return mensajeSemana(progresoSemana(semana(0, *dias) + previas, hoy))
    }

    @Test
    fun mensaje_semanaCumplida() {
        assertEquals("¡Semana cumplida! Todo lo que venga es extra 🎉", mensaje(hoy, 0, 1, 2))
    }

    @Test
    fun mensaje_faltaUna_hastaElDomingo() {
        assertEquals("Te falta 1 sesión, tienes hasta el domingo 💪", mensaje(hoy, 0, 2))
    }

    @Test
    fun mensaje_quedanJustoLosDiasQueFaltan() {
        val sabado = lunes.plusDays(5)
        assertEquals("Quedan 2 días y te faltan 2 sesiones: no te saltes ninguno 💪", mensaje(sabado, 0))
    }

    @Test
    fun mensaje_domingoUltimoDia() {
        val domingo = lunes.plusDays(6)
        assertEquals("Hoy es el último día: te falta 1 sesión 💪", mensaje(domingo, 0, 2))
    }

    @Test
    fun mensaje_yaNoAlcanza() {
        val domingo = lunes.plusDays(6)
        assertEquals(
            "Esta semana no llegas a 3, pero cada sesión cuenta. El lunes empieza otra.",
            mensaje(domingo, 0)
        )
    }

    @Test
    fun mensaje_empiezaTuRacha() {
        assertEquals("Empieza tu racha: 3 sesiones esta semana", mensaje(hoy, racha = false))
    }

    // --- Calendario de constancia ---

    @Test
    fun calendario_16Semanas_terminaEnLaActual_lunesArriba() {
        val cal = calendarioConstancia(emptyList(), hoy)
        assertEquals(16, cal.size)
        assertEquals(lunes, cal.last().dias.first().fecha)
        assertEquals(lunes.minusWeeks(15), cal.first().dias.first().fecha)
        assertTrue(cal.all { s -> s.dias.size == 7 && s.dias.first().fecha.dayOfWeek == DayOfWeek.MONDAY })
    }

    @Test
    fun calendario_marcaSesionesHoyYFuturo() {
        val cal = calendarioConstancia(listOf(7L to lunes, 8L to lunes.minusWeeks(2).plusDays(3)), hoy)
        val actual = cal.last().dias
        assertEquals(7L, actual[0].sesionId)
        assertTrue(actual[2].esHoy)
        assertTrue(actual[3].esFuturo && !actual[2].esFuturo)
        assertEquals(8L, cal[13].dias[3].sesionId)
        assertEquals(2, cal.sumOf { s -> s.dias.count { it.sesionId != null } })
    }

    @Test
    fun calendario_variasSesionesElMismoDia_abreLaUltima() {
        val cal = calendarioConstancia(listOf(3L to lunes, 9L to lunes, 5L to lunes), hoy)
        assertEquals(9L, cal.last().dias[0].sesionId)
    }

    @Test
    fun calendario_rotulaElMesSoloAlCambiar() {
        val cal = calendarioConstancia(emptyList(), hoy)
        assertEquals("jun", cal.first().etiquetaMes) // lunes 15 de junio
        val etiquetas = cal.mapNotNull { it.etiquetaMes }
        assertEquals(listOf("jun", "jul", "ago", "sep"), etiquetas)
    }
}
