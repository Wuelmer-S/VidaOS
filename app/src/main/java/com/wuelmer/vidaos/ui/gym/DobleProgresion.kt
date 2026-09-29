package com.wuelmer.vidaos.ui.gym

import com.wuelmer.vidaos.data.EjercicioGym
import com.wuelmer.vidaos.data.SerieGym
import com.wuelmer.vidaos.data.TipoEjercicio
import com.wuelmer.vidaos.data.ZonaEjercicio

sealed interface Sugerencia {
    data class SubirPeso(val pesoKg: Double) : Sugerencia
    data class MantenerPeso(val pesoKg: Double) : Sugerencia
    object RangoCompletado : Sugerencia
}

// Incremento mínimo del rango acordado; ajustable cuando exista Configuración.
fun incrementoKg(zona: ZonaEjercicio): Double = when (zona) {
    ZonaEjercicio.INFERIOR -> 2.5
    ZonaEjercicio.SUPERIOR, ZonaEjercicio.CORE -> 1.0
}

/**
 * Doble progresión: se sube peso solo si se completaron al menos las series objetivo,
 * todas en el tope del rango y con el mismo peso. Devuelve null si no hay nada que sugerir.
 */
fun sugerir(
    ejercicio: EjercicioGym,
    seriesObjetivo: Int,
    objetivoMax: Int,
    ultimaVez: List<SerieGym>
): Sugerencia? {
    if (ultimaVez.isEmpty()) return null
    val completas = ultimaVez.size >= seriesObjetivo

    return when (ejercicio.tipo) {
        TipoEjercicio.CON_PESO -> {
            val pesos = ultimaVez.mapNotNull { it.pesoKg }
            if (pesos.isEmpty()) return null
            val pesoMaximo = pesos.max()
            val mismoPeso = pesos.size == ultimaVez.size && pesos.distinct().size == 1
            val enTope = ultimaVez.all { (it.repeticiones ?: 0) >= objetivoMax }
            if (completas && mismoPeso && enTope) {
                // Redondeo a 2 decimales para evitar restos de coma flotante (p. ej. 10,2 + 1).
                Sugerencia.SubirPeso(Math.round((pesoMaximo + incrementoKg(ejercicio.zona)) * 100) / 100.0)
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

fun formatearPeso(pesoKg: Double?): String {
    if (pesoKg == null) return "-"
    return if (pesoKg % 1.0 == 0.0) pesoKg.toInt().toString() else pesoKg.toString().replace('.', ',')
}

fun textoUltimaVez(ejercicio: EjercicioGym, series: List<SerieGym>): String {
    val lado = if (ejercicio.unilateral) "/lado" else ""
    return when (ejercicio.tipo) {
        TipoEjercicio.CON_PESO -> {
            val pesos = series.map { it.pesoKg }.distinct()
            if (pesos.size == 1) {
                series.joinToString(" / ") { "${it.repeticiones}" } + " reps$lado · ${formatearPeso(pesos.first())} kg"
            } else {
                series.joinToString(" / ") { "${it.repeticiones}×${formatearPeso(it.pesoKg)}" } + " kg"
            }
        }
        TipoEjercicio.PESO_CORPORAL -> series.joinToString(" / ") { "${it.repeticiones}" } + " reps$lado"
        TipoEjercicio.TIEMPO -> series.joinToString(" / ") { "${it.segundos}" } + " s"
    }
}
