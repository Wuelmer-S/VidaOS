package com.wuelmer.vidaos.ui.navegacion

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector

enum class Modulo(val ruta: String, val etiqueta: String) {
    FINANZAS("finanzas", "Finanzas"),
    GYM("gym", "Gym"),
    MOTO("moto", "Moto");

    // Getter (no propiedad del enum) para no construir íconos al cargar la clase, p. ej. en tests.
    val icono: ImageVector
        get() = when (this) {
            FINANZAS -> Icons.Filled.ShoppingCart
            GYM -> Icons.Filled.Favorite
            MOTO -> Icons.Filled.Build
        }
}

// Módulos a mostrar según los ocultos guardados. Nunca devuelve una lista vacía.
fun modulosVisibles(ocultos: Set<String>): List<Modulo> =
    Modulo.entries.filter { it.name !in ocultos }.ifEmpty { Modulo.entries }

// Siempre debe quedar al menos un módulo visible.
fun puedeOcultar(modulo: Modulo, ocultos: Set<String>): Boolean {
    val visibles = modulosVisibles(ocultos)
    return modulo !in visibles || visibles.size > 1
}
