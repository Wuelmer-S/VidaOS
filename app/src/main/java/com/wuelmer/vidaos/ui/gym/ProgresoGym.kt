package com.wuelmer.vidaos.ui.gym

import com.wuelmer.vidaos.data.META_SEMANAL_DEFECTO
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Racha = semanas seguidas (lunes a domingo) en que se llegó a la meta de sesiones.
 * La semana en curso suma si ya cumplió, pero no rompe la racha mientras no termine.
 * Varias sesiones el mismo día cuentan como una.
 */
fun calcularRacha(fechas: List<LocalDate>, hoy: LocalDate, meta: Int = META_SEMANAL_DEFECTO): Int {
    val porSemana = fechas.distinct()
        .groupingBy { it.with(DayOfWeek.MONDAY) }
        .eachCount()
    val semanaActual = hoy.with(DayOfWeek.MONDAY)
    var racha = 0
    if ((porSemana[semanaActual] ?: 0) >= meta) racha++
    var semana = semanaActual.minusWeeks(1)
    while ((porSemana[semana] ?: 0) >= meta) {
        racha++
        semana = semana.minusWeeks(1)
    }
    return racha
}

data class ProgresoSemana(
    val racha: Int,
    val meta: Int,
    val hoy: DayOfWeek,
    val diasConSesion: Set<DayOfWeek>
) {
    val hechas: Int get() = diasConSesion.size
    val faltan: Int get() = (meta - hechas).coerceAtLeast(0)
    val cumplida: Boolean get() = hechas >= meta

    // Días que aún sirven para entrenar: de hoy al domingo, sin contar hoy si ya fuiste.
    val diasDisponibles: Int
        get() = DayOfWeek.entries.count { it >= hoy && it !in diasConSesion }
}

fun progresoSemana(fechas: List<LocalDate>, hoy: LocalDate, meta: Int = META_SEMANAL_DEFECTO): ProgresoSemana {
    val lunes = hoy.with(DayOfWeek.MONDAY)
    val dias = fechas.filter { it.with(DayOfWeek.MONDAY) == lunes }.map { it.dayOfWeek }.toSet()
    return ProgresoSemana(
        racha = calcularRacha(fechas, hoy, meta),
        meta = meta,
        hoy = hoy.dayOfWeek,
        diasConSesion = dias
    )
}

private fun sesiones(n: Int) = if (n == 1) "1 sesión" else "$n sesiones"

fun mensajeSemana(p: ProgresoSemana): String {
    val faltan = p.faltan
    val disponibles = p.diasDisponibles
    val falta = if (faltan == 1) "Te falta" else "Te faltan"
    return when {
        p.cumplida -> "¡Semana cumplida! Todo lo que venga es extra 🎉"
        faltan > disponibles ->
            "Esta semana no llegas a ${p.meta}, pero cada sesión cuenta. El lunes empieza otra."
        faltan == disponibles && disponibles == 1 && p.hoy == DayOfWeek.SUNDAY ->
            "Hoy es el último día: te falta 1 sesión 💪"
        faltan == disponibles -> {
            val quedan = if (disponibles == 1) "Queda 1 día" else "Quedan $disponibles días"
            "$quedan y ${falta.lowercase()} ${sesiones(faltan)}: no te saltes ninguno 💪"
        }
        p.racha == 0 && p.hechas == 0 -> "Empieza tu racha: ${sesiones(p.meta)} esta semana"
        else -> "$falta ${sesiones(faltan)}, tienes hasta el domingo 💪"
    }
}

// --- Calendario de constancia ---

const val SEMANAS_CALENDARIO = 16

data class DiaCalendario(
    val fecha: LocalDate,
    // Sesión a abrir al tocar el día (si hubo varias ese día, la última registrada).
    val sesionId: Long?,
    val esHoy: Boolean,
    val esFuturo: Boolean
)

data class SemanaCalendario(
    val dias: List<DiaCalendario>,
    // Mes a rotular sobre la columna: solo en la primera semana y cuando cambia el mes.
    val etiquetaMes: String?
)

private val MESES = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")

/** Las últimas [semanas] semanas terminando en la actual; cada semana va de lunes a domingo. */
fun calendarioConstancia(
    sesiones: List<Pair<Long, LocalDate>>,
    hoy: LocalDate,
    semanas: Int = SEMANAS_CALENDARIO
): List<SemanaCalendario> {
    val sesionPorFecha = sesiones.groupBy({ it.second }, { it.first }).mapValues { (_, ids) -> ids.max() }
    val primerLunes = hoy.with(DayOfWeek.MONDAY).minusWeeks((semanas - 1).toLong())
    var mesAnterior: Int? = null
    return (0 until semanas).map { w ->
        val lunes = primerLunes.plusWeeks(w.toLong())
        val mes = lunes.monthValue
        val etiqueta = if (mes != mesAnterior) MESES[mes - 1] else null
        mesAnterior = mes
        SemanaCalendario(
            dias = (0L..6L).map { d ->
                val fecha = lunes.plusDays(d)
                DiaCalendario(
                    fecha = fecha,
                    sesionId = sesionPorFecha[fecha],
                    esHoy = fecha == hoy,
                    esFuturo = fecha > hoy
                )
            },
            etiquetaMes = etiqueta
        )
    }
}
