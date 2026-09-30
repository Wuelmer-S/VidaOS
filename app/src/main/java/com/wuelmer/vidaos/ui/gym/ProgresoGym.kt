package com.wuelmer.vidaos.ui.gym

import com.wuelmer.vidaos.data.META_SEMANAL_DEFECTO
import com.wuelmer.vidaos.data.UnidadPeso
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale

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

// --- Resumen y logros ---

enum class TipoLogro { SESIONES, SEMANAS_SEGUIDAS }

data class Logro(
    val tipo: TipoLogro,
    val objetivo: Int,
    val icono: String,
    val conseguido: Boolean,
    // Avance hacia el objetivo: sesiones totales, o la racha actual para los de semanas seguidas.
    val actual: Int
) {
    val titulo: String
        get() = when (tipo) {
            TipoLogro.SESIONES -> "$objetivo sesiones"
            TipoLogro.SEMANAS_SEGUIDAS -> "$objetivo sem. seguidas"
        }
    val faltan: Int get() = (objetivo - actual).coerceAtLeast(0)
    val fraccion: Float get() = (actual.toFloat() / objetivo).coerceIn(0f, 1f)
}

data class ResumenGym(
    val sesionesTotales: Int,
    val mejorRacha: Int,
    val volumenTotalKg: Double,
    val promedioSemanal: Double,
    val logros: List<Logro>
) {
    // El logro pendiente más cercano a cumplirse; null si ya están todos.
    val proximoLogro: Logro?
        get() = logros.filter { !it.conseguido }.maxByOrNull { it.fraccion }
}

private val LOGROS_SESIONES = listOf(10 to "🥉", 25 to "🥈", 50 to "🥇", 100 to "💯")
private val LOGROS_SEMANAS = listOf(4 to "📅", 8 to "🔥", 12 to "🏆")

/** Mayor cantidad de semanas seguidas cumpliendo la meta en todo el historial. */
fun mejorRacha(fechas: List<LocalDate>, meta: Int = META_SEMANAL_DEFECTO): Int {
    val cumplidas = fechas.distinct()
        .groupingBy { it.with(DayOfWeek.MONDAY) }
        .eachCount()
        .filterValues { it >= meta }
        .keys
        .sorted()
    var mejor = 0
    var actual = 0
    var anterior: LocalDate? = null
    cumplidas.forEach { lunes ->
        actual = if (anterior != null && lunes == anterior!!.plusWeeks(1)) actual + 1 else 1
        mejor = maxOf(mejor, actual)
        anterior = lunes
    }
    return mejor
}

/**
 * [fechas] trae una fecha por sesión (con repetidas si hubo varias el mismo día): los totales
 * cuentan sesiones, mientras que la racha cuenta días. El promedio va desde la semana de la
 * primera sesión hasta la actual, ambas incluidas.
 */
fun resumenGym(
    fechas: List<LocalDate>,
    volumenTotalKg: Double,
    hoy: LocalDate,
    meta: Int = META_SEMANAL_DEFECTO
): ResumenGym {
    val total = fechas.size
    val mejor = mejorRacha(fechas, meta)
    val rachaActual = calcularRacha(fechas, hoy, meta)
    val semanas = fechas.minOrNull()?.let { primera ->
        ChronoUnit.WEEKS.between(primera.with(DayOfWeek.MONDAY), hoy.with(DayOfWeek.MONDAY)) + 1
    } ?: 0L
    val logros = LOGROS_SESIONES.map { (n, icono) ->
        Logro(TipoLogro.SESIONES, n, icono, conseguido = total >= n, actual = total)
    } + LOGROS_SEMANAS.map { (n, icono) ->
        Logro(TipoLogro.SEMANAS_SEGUIDAS, n, icono, conseguido = mejor >= n, actual = rachaActual)
    }
    return ResumenGym(
        sesionesTotales = total,
        mejorRacha = mejor,
        volumenTotalKg = volumenTotalKg,
        promedioSemanal = if (semanas > 0) total.toDouble() / semanas else 0.0,
        logros = logros
    )
}

// Peso total en la unidad elegida: toneladas si pasa de 1000 kg; en libras, con separador de miles.
fun formatearVolumen(volumenKg: Double, unidad: UnidadPeso): String = when (unidad) {
    UnidadPeso.KG ->
        if (volumenKg >= 1000) "${formatearPeso(Math.round(volumenKg / 100) / 10.0)} t"
        else "${Math.round(volumenKg)} kg"
    UnidadPeso.LB -> "%,d lb".format(Locale.forLanguageTag("es-ES"), Math.round(unidad.desdeKg(volumenKg)))
}
