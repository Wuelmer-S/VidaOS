package com.wuelmer.vidaos.ui.gym

import com.wuelmer.vidaos.data.EjercicioGym
import com.wuelmer.vidaos.data.PuntoEjercicio
import com.wuelmer.vidaos.data.TipoEjercicio
import com.wuelmer.vidaos.data.UnidadPeso
import java.time.LocalDate

data class PuntoEvolucion(val sesionId: Long, val fecha: LocalDate, val valor: Double)

/**
 * Evolución de un ejercicio, sesión a sesión, ya en la unidad a mostrar: peso máximo en
 * ejercicios con peso (kg o lb según preferencia), reps máximas en peso corporal y segundos en tiempo.
 */
data class EvolucionEjercicio(
    val puntos: List<PuntoEvolucion>,
    val unidadTexto: String,
    val metrica: String
) {
    val actual: Double? get() = puntos.lastOrNull()?.valor
    val primero: PuntoEvolucion? get() = puntos.firstOrNull()

    // Cambio desde la primera sesión; null si hay menos de dos para comparar.
    val cambio: Double?
        get() = if (puntos.size < 2) null else Math.round((puntos.last().valor - puntos.first().valor) * 100) / 100.0
}

fun evolucionEjercicio(ejercicio: EjercicioGym, registros: List<PuntoEjercicio>, unidad: UnidadPeso): EvolucionEjercicio {
    val (valor: (PuntoEjercicio) -> Double?, unidadTexto, metrica) = when (ejercicio.tipo) {
        TipoEjercicio.CON_PESO -> Triple({ p: PuntoEjercicio -> p.pesoMaxKg?.let(unidad::desdeKg) }, unidad.simbolo, "peso máximo")
        TipoEjercicio.PESO_CORPORAL -> Triple({ p: PuntoEjercicio -> p.repsMax?.toDouble() }, "reps", "reps máximas")
        TipoEjercicio.TIEMPO -> Triple({ p: PuntoEjercicio -> p.segundosMax?.toDouble() }, "s", "tiempo máximo")
    }
    val puntos = registros
        .sortedWith(compareBy({ it.fecha }, { it.sesionId }))
        .mapNotNull { p -> valor(p)?.let { PuntoEvolucion(p.sesionId, p.fecha, it) } }
    return EvolucionEjercicio(puntos, unidadTexto, metrica)
}

fun textoCambio(cambio: Double, unidadTexto: String): String = when {
    cambio > 0 -> "▲ +${formatearPeso(cambio)} $unidadTexto"
    cambio < 0 -> "▼ −${formatearPeso(-cambio)} $unidadTexto"
    else -> "= sin cambio"
}
