package com.wuelmer.vidaos.ui.moto

import com.wuelmer.vidaos.data.AjusteTrabajo

// Pasos y materiales se guardan como texto, uno por línea. Las líneas vacías se ignoran.
fun lineas(texto: String): List<String> =
    texto.lines().map { it.trim() }.filter { it.isNotEmpty() }

// Por confirmar = sin medida de llave o con una medida que el usuario todavía no probó.
fun porConfirmar(ajuste: AjusteTrabajo): Boolean = ajuste.llave.isNullOrBlank() || !ajuste.verificado

// Verificado: "Usaste 17 mm · 20 N·m". Por confirmar: solo el torque, porque la medida la anota el usuario.
fun describirAjuste(ajuste: AjusteTrabajo): String {
    val torque = ajuste.torqueNm?.let { "$it N·m" }
    if (porConfirmar(ajuste)) return torque?.let { "Torque $it" } ?: "Sin torque indicado"
    return listOfNotNull("Usaste ${ajuste.llave}", torque).joinToString(" · ")
}

// La medida que venía de foros (sin probar) se muestra solo como pista mientras el usuario no anote la suya.
fun referenciaLlave(ajuste: AjusteTrabajo): String? =
    ajuste.llave?.takeIf { it.isNotBlank() && !ajuste.verificado }

// "17" → "17 mm". Si escribe algo más ("Dado 17 mm", "1/2 pulgada") se guarda tal cual.
fun normalizarMedida(texto: String): String {
    val limpio = texto.trim()
    return if (limpio.matches(Regex("""\d+([.,]\d+)?"""))) "$limpio mm" else limpio
}

// Nota bajo un paso del tutorial que menciona un perno de la ficha (p. ej. "Sacar el tapón de drenaje").
fun notaPaso(paso: String, ajustes: List<AjusteTrabajo>): String? {
    val ajuste = ajustes.firstOrNull { it.parte.isNotBlank() && paso.contains(it.parte, ignoreCase = true) }
        ?: return null
    return if (porConfirmar(ajuste)) {
        "🔧 Medida: anótala arriba en Llaves y torques"
    } else {
        "🔧 Usaste ${ajuste.llave}"
    }
}

// Resumen para la lista de fichas: "2 pernos · 1 por anotar"
fun resumenAjustes(ajustes: List<AjusteTrabajo>): String {
    if (ajustes.isEmpty()) return "Sin pernos que ajustar"
    val total = if (ajustes.size == 1) "1 perno" else "${ajustes.size} pernos"
    val pendientes = ajustes.count(::porConfirmar)
    return if (pendientes == 0) "$total · medidas anotadas" else "$total · $pendientes por anotar"
}
