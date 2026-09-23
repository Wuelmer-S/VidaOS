package com.wuelmer.vidaos.ui.navegacion

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector

enum class Modulo(val ruta: String, val etiqueta: String, val icono: ImageVector) {
    FINANZAS("finanzas", "Finanzas", Icons.Filled.ShoppingCart),
    GYM("gym", "Gym", Icons.Filled.Favorite)
}
