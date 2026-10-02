package com.wuelmer.vidaos.ui.moto

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

fun formatearKm(km: Int): String =
    NumberFormat.getIntegerInstance(Locale.forLanguageTag("es-CL")).format(km) + " km"

private fun plural(n: Long, singular: String, plural: String) = "$n ${if (n == 1L) singular else plural}"

// 730 → "2 años", 180 → "6 meses", 7 → "7 días".
fun formatearPeriodo(dias: Int): String = when {
    dias >= 365 && dias % 365 == 0 -> plural(dias / 365L, "año", "años")
    dias >= 30 && dias % 30 == 0 -> plural(dias / 30L, "mes", "meses")
    else -> plural(dias.toLong(), "día", "días")
}

// "Cada 3.000 km o 6 meses"
fun describirIntervalo(cadaKm: Int?, cadaDias: Int?): String {
    val partes = listOfNotNull(cadaKm?.let(::formatearKm), cadaDias?.let(::formatearPeriodo))
    return if (partes.isEmpty()) "Sin intervalo" else "Cada " + partes.joinToString(" o ")
}

// Lo que falta según el lado que manda (km o tiempo).
fun describirRestante(estado: EstadoMantencion): String {
    if (estado.progreso == null) return "Sin registro todavía"
    val km = estado.kmRestantes
    val dias = estado.diasRestantes
    return when {
        estado.mandaKm && km != null -> when {
            km > 0 -> "faltan ${formatearKm(km)}"
            km == 0 -> "toca ahora"
            else -> "te pasaste por ${formatearKm(abs(km))}"
        }
        dias != null -> when {
            dias > 0 -> "faltan " + plural(dias, "día", "días")
            dias == 0L -> "toca hoy"
            else -> "atrasada " + plural(abs(dias), "día", "días")
        }
        else -> ""
    }
}

fun etiquetaSemaforo(semaforo: Semaforo): String = when (semaforo) {
    Semaforo.SIN_REGISTRO -> "Sin registro"
    Semaforo.VERDE -> "OK"
    Semaforo.AMARILLO -> "Pronto"
    Semaforo.ROJO -> "Vencida"
}
