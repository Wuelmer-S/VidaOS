package com.wuelmer.vidaos.ui.gym

import com.wuelmer.vidaos.data.EjercicioGym
import com.wuelmer.vidaos.data.SerieGym
import com.wuelmer.vidaos.data.TipoEjercicio
import com.wuelmer.vidaos.data.UnidadPeso
import com.wuelmer.vidaos.data.ZonaEjercicio

// Los pesos de una sugerencia ya vienen en la unidad elegida por el usuario.
sealed interface Sugerencia {
    data class SubirPeso(val peso: Double) : Sugerencia
    data class MantenerPeso(val peso: Double) : Sugerencia
    object RangoCompletado : Sugerencia
}

private const val KG_POR_LB = 0.45359237

private fun redondear(valor: Double): Double = Math.round(valor * 100) / 100.0

// En la base todo está en kg; se redondea a 2 decimales para que 135 lb se vuelva a mostrar como 135.
fun UnidadPeso.desdeKg(kg: Double): Double = when (this) {
    UnidadPeso.KG -> kg
    UnidadPeso.LB -> redondear(kg / KG_POR_LB)
}

fun UnidadPeso.aKg(valor: Double): Double = when (this) {
    UnidadPeso.KG -> valor
    UnidadPeso.LB -> valor * KG_POR_LB
}

val UnidadPeso.simbolo: String
    get() = when (this) {
        UnidadPeso.KG -> "kg"
        UnidadPeso.LB -> "lb"
    }

// Incremento mínimo del rango acordado para cada unidad.
fun incremento(zona: ZonaEjercicio, unidad: UnidadPeso): Double = when (unidad) {
    UnidadPeso.KG -> if (zona == ZonaEjercicio.INFERIOR) 2.5 else 1.0
    UnidadPeso.LB -> if (zona == ZonaEjercicio.INFERIOR) 5.0 else 2.5
}

/**
 * Doble progresión: se sube peso solo si se completaron al menos las series objetivo,
 * todas en el tope del rango y con el mismo peso. Devuelve null si no hay nada que sugerir.
 */
fun sugerir(
    ejercicio: EjercicioGym,
    seriesObjetivo: Int,
    objetivoMax: Int,
    ultimaVez: List<SerieGym>,
    unidad: UnidadPeso = UnidadPeso.KG
): Sugerencia? {
    if (ultimaVez.isEmpty()) return null
    val completas = ultimaVez.size >= seriesObjetivo

    return when (ejercicio.tipo) {
        TipoEjercicio.CON_PESO -> {
            val pesos = ultimaVez.mapNotNull { it.pesoKg?.let(unidad::desdeKg) }
            if (pesos.isEmpty()) return null
            val pesoMaximo = pesos.max()
            val mismoPeso = pesos.size == ultimaVez.size && pesos.distinct().size == 1
            val enTope = ultimaVez.all { (it.repeticiones ?: 0) >= objetivoMax }
            if (completas && mismoPeso && enTope) {
                // Redondeo para evitar restos de coma flotante (p. ej. 10,2 + 1).
                Sugerencia.SubirPeso(redondear(pesoMaximo + incremento(ejercicio.zona, unidad)))
            } else {
                Sugerencia.MantenerPeso(pesoMaximo)
            }
        }
        TipoEjercicio.PESO_CORPORAL ->
            if (completas && ultimaVez.all { (it.repeticiones ?: 0) >= objetivoMax }) Sugerencia.RangoCompletado else null
        TipoEjercicio.TIEMPO ->
            if (completas && ultimaVez.all { (it.segundos ?: 0) >= objetivoMax }) Sugerencia.RangoCompletado else null
    }
}

fun formatearPeso(peso: Double?): String {
    if (peso == null) return "-"
    return if (peso % 1.0 == 0.0) peso.toInt().toString() else peso.toString().replace('.', ',')
}

// Peso guardado en kg, mostrado en la unidad elegida.
fun formatearPesoKg(pesoKg: Double?, unidad: UnidadPeso): String =
    formatearPeso(pesoKg?.let(unidad::desdeKg))

fun textoUltimaVez(ejercicio: EjercicioGym, series: List<SerieGym>, unidad: UnidadPeso = UnidadPeso.KG): String {
    val lado = if (ejercicio.unilateral) "/lado" else ""
    return when (ejercicio.tipo) {
        TipoEjercicio.CON_PESO -> {
            val pesos = series.map { it.pesoKg }.distinct()
            if (pesos.size == 1) {
                series.joinToString(" / ") { "${it.repeticiones}" } +
                    " reps$lado · ${formatearPesoKg(pesos.first(), unidad)} ${unidad.simbolo}"
            } else {
                series.joinToString(" / ") { "${it.repeticiones}×${formatearPesoKg(it.pesoKg, unidad)}" } +
                    " ${unidad.simbolo}"
            }
        }
        TipoEjercicio.PESO_CORPORAL -> series.joinToString(" / ") { "${it.repeticiones}" } + " reps$lado"
        TipoEjercicio.TIEMPO -> series.joinToString(" / ") { "${it.segundos}" } + " s"
    }
}
