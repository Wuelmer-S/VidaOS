package com.wuelmer.vidaos.ui.navegacion

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun SelectorModulos(
    modulos: List<Modulo>,
    moduloActual: Modulo?,
    onModuloClick: (Modulo) -> Unit
) {
    NavigationBar {
        modulos.forEach { modulo ->
            NavigationBarItem(
                selected = modulo == moduloActual,
                onClick = { onModuloClick(modulo) },
                icon = { Icon(modulo.icono, contentDescription = modulo.etiqueta) },
                label = { Text(modulo.etiqueta) }
            )
        }
    }
}
