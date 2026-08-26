package com.wuelmer.vidaos.data

import android.graphics.Color as AndroidColor

object AsignadorColorCategoria {

    // Misma paleta fija de CategoriasIniciales (registro/Ideas/paleta_colores_app.html).
    private val paletaBase = listOf("#7B61FF", "#ED4C8B", "#37D5D6", "#FF7A59", "#FFA26B")
    private const val ANGULO_AUREO = 137.508f
    private const val SATURACION = 0.65f
    private const val BRILLO = 0.95f

    fun siguienteColor(coloresEnUso: List<String>): String {
        val usados = coloresEnUso.map { it.uppercase() }.toSet()

        paletaBase.firstOrNull { it.uppercase() !in usados }?.let { return it }

        var indiceExtra = 0
        var candidato: String
        do {
            candidato = colorGenerado(indiceExtra)
            indiceExtra++
        } while (candidato.uppercase() in usados)
        return candidato
    }

    // Cuando se agota la paleta fija, rota el matiz en pasos de ángulo áureo (~137.5°)
    // manteniendo saturación/brillo constantes: los colores se distinguen entre sí sin
    // que ninguno contraste "demasiado fuerte" frente a los demás.
    private fun colorGenerado(indiceExtra: Int): String {
        val huePartida = 24f
        val hue = (huePartida + ANGULO_AUREO * (indiceExtra + 1)) % 360f
        val colorInt = AndroidColor.HSVToColor(floatArrayOf(hue, SATURACION, BRILLO))
        return String.format("#%06X", 0xFFFFFF and colorInt)
    }
}
