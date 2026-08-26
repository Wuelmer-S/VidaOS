package com.wuelmer.vidaos.ui.categorias

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wuelmer.vidaos.data.TipoCategoria

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgregarCategoriaDialog(
    tipoFijo: TipoCategoria?,
    onDismiss: () -> Unit,
    onConfirmar: (nombre: String, tipo: TipoCategoria) -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var tipoSeleccionado by remember { mutableStateOf(tipoFijo ?: TipoCategoria.GASTO) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva categoría") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier
                )

                if (tipoFijo == null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = tipoSeleccionado == TipoCategoria.GASTO,
                            onClick = { tipoSeleccionado = TipoCategoria.GASTO },
                            label = { Text("Gasto") }
                        )
                        FilterChip(
                            selected = tipoSeleccionado == TipoCategoria.INGRESO,
                            onClick = { tipoSeleccionado = TipoCategoria.INGRESO },
                            label = { Text("Ingreso") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirmar(nombre.trim(), tipoSeleccionado) },
                enabled = nombre.isNotBlank()
            ) {
                Text("Agregar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
