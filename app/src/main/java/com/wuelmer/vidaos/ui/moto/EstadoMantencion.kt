package com.wuelmer.vidaos.ui.moto

import com.wuelmer.vidaos.data.LecturaKm
import com.wuelmer.vidaos.data.RegistroMantencion
import com.wuelmer.vidaos.data.TipoMantencion
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ceil

enum class Semaforo {
    SIN_REGISTRO,
    VERDE,
    AMARILLO,
    ROJO
}

// progreso: 0 = recién hecha, 1 = vencida (puede pasar de 1). null si nunca se registró:
// sin saber cuándo fue la última vez no hay de dónde contar, y marcarla vencida asustaría por nada.
data class EstadoMantencion(
    val tipo: TipoMantencion,
    val ultimo: RegistroMantencion?,
    val progreso: Float?,
    val kmRestantes: Int?,
    val diasRestantes: Long?,
    // true si lo que más avanzó es el km (define qué se muestra: "faltan X km" o "faltan N días").
    val mandaKm: Boolean = false
) {
    val semaforo: Semaforo get() = semaforo(progreso)
}

// Regla: vence por lo que ocurra primero, km o tiempo.
fun calcularEstado(
    tipo: TipoMantencion,
    ultimo: RegistroMantencion?,
    kmActual: Int?,
    hoy: LocalDate
): EstadoMantencion {
    if (ultimo == null) return EstadoMantencion(tipo, null, null, null, null)

    // Si el km actual es menor que el del registro (lectura vieja o mal tipeada), se cuenta como 0.
    val kmDesde = kmActual?.let { (it - ultimo.km).coerceAtLeast(0) }
    val diasDesde = ChronoUnit.DAYS.between(ultimo.fecha, hoy).coerceAtLeast(0)

    val pKm = if (tipo.cadaKm != null && kmDesde != null) kmDesde.toFloat() / tipo.cadaKm else null
    val pDias = tipo.cadaDias?.let { diasDesde.toFloat() / it }

    return EstadoMantencion(
        tipo = tipo,
        ultimo = ultimo,
        progreso = listOfNotNull(pKm, pDias).maxOrNull() ?: 0f,
        kmRestantes = if (tipo.cadaKm != null && kmDesde != null) tipo.cadaKm - kmDesde else null,
        diasRestantes = tipo.cadaDias?.let { it - diasDesde },
        mandaKm = pKm != null && (pDias == null || pKm >= pDias)
    )
}

fun semaforo(progreso: Float?): Semaforo = when {
    progreso == null -> Semaforo.SIN_REGISTRO
    progreso < 0.7f -> Semaforo.VERDE
    progreso < 1f -> Semaforo.AMARILLO
    else -> Semaforo.ROJO
}

// Lo más urgente arriba; las nunca registradas al final, en el orden del plan.
fun ordenarPorUrgencia(estados: List<EstadoMantencion>): List<EstadoMantencion> =
    estados.sortedWith(compareBy<EstadoMantencion> { it.progreso == null }.thenByDescending { it.progreso ?: 0f })

private const val DIAS_RITMO = 30L

// Km por día según las lecturas de los últimos 30 días. null si no hay al menos 2 en días distintos.
fun ritmoKmPorDia(lecturas: List<LecturaKm>, hoy: LocalDate): Double? {
    val recientes = lecturas.filter { !it.fecha.isBefore(hoy.minusDays(DIAS_RITMO)) }
    val primera = recientes.minWithOrNull(compareBy<LecturaKm> { it.fecha }.thenBy { it.km }) ?: return null
    val ultima = recientes.maxWithOrNull(compareBy<LecturaKm> { it.fecha }.thenBy { it.km }) ?: return null
    val dias = ChronoUnit.DAYS.between(primera.fecha, ultima.fecha)
    if (dias <= 0) return null
    return (ultima.km - primera.km).coerceAtLeast(0).toDouble() / dias
}

// Cuántos días faltan para recorrer los km restantes a este ritmo. null si no se puede estimar.
fun diasEstimados(kmRestantes: Int, ritmo: Double?): Long? {
    if (ritmo == null || ritmo <= 0.0 || kmRestantes <= 0) return null
    return ceil(kmRestantes / ritmo).toLong()
}
