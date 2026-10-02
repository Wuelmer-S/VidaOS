package com.wuelmer.vidaos.ui.moto

import com.wuelmer.vidaos.data.AjusteTrabajo

// Pasos y materiales se guardan como texto, uno por línea. Las líneas vacías se ignoran.
fun lineas(texto: String): List<String> =
    texto.lines().map { it.trim() }.filter { it.isNotEmpty() }

// Por confirmar = sin medida de llave o con una medida que el usuario todavía no probó.
fun porConfirmar(ajuste: AjusteTrabajo): Boolean = ajuste.llave.isNullOrBlank() || !ajuste.verificado

// "Llave o dado 17 mm · 20 N·m" / "Llave por medir · 59 N·m"
fun describirAjuste(ajuste: AjusteTrabajo): String {
    val llave = ajuste.llave?.takeIf { it.isNotBlank() } ?: "Llave por medir"
    return listOfNotNull(llave, ajuste.torqueNm?.let { "$it N·m" }).joinToString(" · ")
}

// Resumen para la lista de fichas: "2 pernos · 1 por confirmar"
fun resumenAjustes(ajustes: List<AjusteTrabajo>): String {
    if (ajustes.isEmpty()) return "Sin pernos que ajustar"
    val total = if (ajustes.size == 1) "1 perno" else "${ajustes.size} pernos"
    val pendientes = ajustes.count(::porConfirmar)
    return if (pendientes == 0) "$total · todo verificado" else "$total · $pendientes por confirmar"
}
