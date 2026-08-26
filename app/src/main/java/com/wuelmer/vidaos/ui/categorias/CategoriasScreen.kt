package com.wuelmer.vidaos.ui.categorias

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wuelmer.vidaos.data.Categoria
import com.wuelmer.vidaos.data.TipoCategoria
import com.wuelmer.vidaos.ui.movimientos.parseColorOrDefault
import com.wuelmer.vidaos.ui.theme.TextoSuave
import com.wuelmer.vidaos.ui.theme.VidaOSTheme
import kotlinx.coroutines.launch

@Composable
fun CategoriasRoute(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CategoriasViewModel = viewModel(factory = CategoriasViewModel.Factory)
) {
    val categorias by viewModel.categorias.collectAsState()
    CategoriasScreen(
        categorias = categorias,
        onBackClick = onBackClick,
        onEliminar = viewModel::eliminar,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriasScreen(
    categorias: List<Categoria>,
    onBackClick: () -> Unit,
    onEliminar: (Categoria, (Boolean) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    var categoriaAEliminar by remember { mutableStateOf<Categoria?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Categorías") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val gastos = categorias.filter { it.tipo == TipoCategoria.GASTO }
            val ingresos = categorias.filter { it.tipo == TipoCategoria.INGRESO }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item { SeccionTitulo("Gastos") }
                items(gastos, key = { it.id }) { categoria ->
                    CategoriaRow(categoria = categoria, onEliminarClick = { categoriaAEliminar = categoria })
                }
                item { SeccionTitulo("Ingresos") }
                items(ingresos, key = { it.id }) { categoria ->
                    CategoriaRow(categoria = categoria, onEliminarClick = { categoriaAEliminar = categoria })
                }
            }
        }
    }

    categoriaAEliminar?.let { categoria ->
        AlertDialog(
            onDismissRequest = { categoriaAEliminar = null },
            title = { Text("¿Eliminar \"${categoria.nombre}\"?") },
            text = { Text("Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    onEliminar(categoria) { exito ->
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                if (exito) {
                                    "Categoría eliminada"
                                } else {
                                    "No se puede eliminar: tiene movimientos asociados"
                                }
                            )
                        }
                    }
                    categoriaAEliminar = null
                }) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { categoriaAEliminar = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun SeccionTitulo(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.labelMedium,
        color = TextoSuave,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun CategoriaRow(categoria: Categoria, onEliminarClick: () -> Unit) {
    val color = parseColorOrDefault(categoria.color, MaterialTheme.colorScheme.primary)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(shape = CircleShape, color = color, modifier = Modifier.size(12.dp)) {}
            Text(
                text = categoria.nombre,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onEliminarClick) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Eliminar categoría",
                    tint = TextoSuave
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CategoriasScreenPreview() {
    VidaOSTheme {
        CategoriasScreen(
            categorias = listOf(
                Categoria(id = 1, nombre = "Comida", tipo = TipoCategoria.GASTO, color = "#7B61FF"),
                Categoria(id = 2, nombre = "Sueldo", tipo = TipoCategoria.INGRESO, color = "#FFA26B")
            ),
            onBackClick = {},
            onEliminar = { _, _ -> }
        )
    }
}
