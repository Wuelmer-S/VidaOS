package com.wuelmer.vidaos.avisos

import com.wuelmer.vidaos.ui.moto.EstadoMantencion
import com.wuelmer.vidaos.ui.moto.describirRestante
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

// Niveles de aviso de una mantención, de menor a mayor urgencia.
enum class NivelAviso {
    MITAD,      // 50 %: para ir sabiendo
    URGENTE,    // avisarAlPct del tipo (90 % por defecto)
    VENCIDA     // 100 %: se repite cada mañana hasta registrarla
}

data class AvisoMantencion(val estado: EstadoMantencion, val nivel: NivelAviso)

data class ResultadoAvisos(
    val avisos: List<AvisoMantencion>,
    // Lo que queda marcado como ya avisado, para no repetir el 50 % ni el 90 %.
    val enviados: Set<String>
)

const val PCT_MITAD = 50
const val DIAS_SIN_KM = 7L

// null = todavía no toca avisar (o nunca se registró: sin última vez no hay de dónde contar).
fun nivelAviso(estado: EstadoMantencion): NivelAviso? {
    val progreso = estado.progreso ?: return null
    return when {
        progreso >= 1f -> NivelAviso.VENCIDA
        progreso * 100 >= estado.tipo.avisarAlPct -> NivelAviso.URGENTE
        progreso * 100 >= PCT_MITAD -> NivelAviso.MITAD
        else -> null
    }
}

// Clave "tipoId:registroId": al registrar la mantención de nuevo cambia el registro y los avisos vuelven a empezar.
private fun clave(estado: EstadoMantencion): String? =
    estado.ultimo?.let { "${estado.tipo.id}:${it.id}" }

// Decide qué avisar hoy. Si una mantención salta de golpe al 90 % solo se avisa el 90 % (el 50 % queda como visto).
fun avisosPendientes(estados: List<EstadoMantencion>, enviados: Set<String>): ResultadoAvisos {
    val avisos = mutableListOf<AvisoMantencion>()
    val nuevos = mutableSetOf<String>()
    for (estado in estados) {
        val clave = clave(estado) ?: continue
        val nivel = nivelAviso(estado) ?: continue
        val yaAvisado = "$clave:${nivel.name}" in enviados
        if (nivel == NivelAviso.VENCIDA || !yaAvisado) avisos += AvisoMantencion(estado, nivel)
        NivelAviso.entries.filter { it <= nivel }.forEach { nuevos += "$clave:${it.name}" }
    }
    // Lo que no está en `nuevos` era de registros viejos o borrados: se descarta para que no crezca sin fin.
    return ResultadoAvisos(avisos.sortedByDescending { it.nivel }, nuevos)
}

fun necesitaRecordatorioKm(ultimaLectura: LocalDate?, hoy: LocalDate): Boolean =
    ultimaLectura == null || ChronoUnit.DAYS.between(ultimaLectura, hoy) >= DIAS_SIN_KM

fun tituloAviso(aviso: AvisoMantencion): String {
    val tipo = aviso.estado.tipo
    return when (aviso.nivel) {
        NivelAviso.MITAD -> "${tipo.icono} ${tipo.nombre}: vas a la mitad"
        NivelAviso.URGENTE -> "⚠️ ${tipo.nombre}: se acerca"
        NivelAviso.VENCIDA -> "🔴 ${tipo.nombre}: vencida"
    }
}

fun textoAviso(aviso: AvisoMantencion): String {
    val restante = describirRestante(aviso.estado).replaceFirstChar { it.uppercase() }
    return when (aviso.nivel) {
        NivelAviso.MITAD -> "$restante. Por ahora todo bien."
        NivelAviso.URGENTE -> "$restante. Ve preparando lo necesario."
        NivelAviso.VENCIDA -> "$restante. Hazla cuanto antes."
    }
}

fun textoRecordatorioKm(ultimaLectura: LocalDate?, hoy: LocalDate): String =
    if (ultimaLectura == null) {
        "Anota cuántos km marca la moto para calcular tus mantenciones."
    } else {
        "Llevas ${ChronoUnit.DAYS.between(ultimaLectura, hoy)} días sin anotar los km. Míralos antes de salir."
    }

// Próxima vez que toca la hora elegida (minutos desde medianoche): hoy si aún no pasa, si no mañana.
fun proximaEjecucion(ahora: LocalDateTime, minutosDelDia: Int): LocalDateTime {
    val hoy = ahora.toLocalDate().atTime(LocalTime.of(minutosDelDia / 60, minutosDelDia % 60))
    return if (hoy.isAfter(ahora)) hoy else hoy.plusDays(1)
}

// 450 → "07:30"
fun formatearHora(minutosDelDia: Int): String =
    "%02d:%02d".format(minutosDelDia / 60, minutosDelDia % 60)
