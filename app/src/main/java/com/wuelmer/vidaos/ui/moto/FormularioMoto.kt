package com.wuelmer.vidaos.ui.moto

import java.time.LocalDate

// Unidad para escribir el intervalo de tiempo. Mismas equivalencias que formatearPeriodo (mes = 30, año = 365).
enum class UnidadTiempo(val dias: Int, val etiqueta: String) {
    DIAS(1, "Días"),
    MESES(30, "Meses"),
    ANIOS(365, "Años")
}

// 730 → (2, AÑOS), 180 → (6, MESES), 45 → (45, DÍAS). Para precargar el formulario al editar.
fun descomponerDias(dias: Int): Pair<Int, UnidadTiempo> = when {
    dias >= 365 && dias % 365 == 0 -> dias / 365 to UnidadTiempo.ANIOS
    dias >= 30 && dias % 30 == 0 -> dias / 30 to UnidadTiempo.MESES
    else -> dias to UnidadTiempo.DIAS
}

// Texto vacío = no vence por ese lado. "0" o texto inválido = error.
sealed interface CampoNumero {
    data object Vacio : CampoNumero
    data object Invalido : CampoNumero
    data class Valor(val n: Int) : CampoNumero
}

fun leerPositivo(texto: String): CampoNumero {
    val limpio = texto.trim()
    if (limpio.isEmpty()) return CampoNumero.Vacio
    val n = limpio.toIntOrNull() ?: return CampoNumero.Invalido
    return if (n > 0) CampoNumero.Valor(n) else CampoNumero.Invalido
}

data class IntervaloValido(val nombre: String, val cadaKm: Int?, val cadaDias: Int?)

// Devuelve el intervalo listo para guardar, o el mensaje de error para mostrar.
fun validarTipo(nombre: String, kmTexto: String, tiempoTexto: String, unidad: UnidadTiempo): Result<IntervaloValido> {
    val limpio = nombre.trim()
    if (limpio.isEmpty()) return Result.failure(IllegalArgumentException("Ponle un nombre"))
    val km = leerPositivo(kmTexto)
    val tiempo = leerPositivo(tiempoTexto)
    if (km == CampoNumero.Invalido) return Result.failure(IllegalArgumentException("Los km deben ser un número mayor que 0"))
    if (tiempo == CampoNumero.Invalido) return Result.failure(IllegalArgumentException("El tiempo debe ser un número mayor que 0"))
    val cadaKm = (km as? CampoNumero.Valor)?.n
    val cadaDias = (tiempo as? CampoNumero.Valor)?.n?.times(unidad.dias)
    if (cadaKm == null && cadaDias == null) {
        return Result.failure(IllegalArgumentException("Indica cada cuántos km, cada cuánto tiempo o ambos"))
    }
    return Result.success(IntervaloValido(limpio, cadaKm, cadaDias))
}

// Una mantención hecha hoy con más km de los anotados también actualiza el odómetro.
// Con fecha pasada no: esa lectura quedaría "más vieja" que la actual y confundiría el historial.
fun debeActualizarOdometro(km: Int, fecha: LocalDate, kmActual: Int?, hoy: LocalDate): Boolean =
    fecha == hoy && (kmActual == null || km > kmActual)
