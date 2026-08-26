package com.wuelmer.vidaos.ui.historial

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wuelmer.vidaos.data.MovimientoConCategoria
import com.wuelmer.vidaos.ui.movimientos.MovimientoRow
import com.wuelmer.vidaos.ui.theme.TextoSuave
import com.wuelmer.vidaos.ui.theme.VidaOSTheme

@Composable
fun HistorialRoute(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onMovimientoClick: (Long) -> Unit = {},
    viewModel: HistorialViewModel = viewModel(factory = HistorialViewModel.Factory)
) {
    val movimientos by viewModel.movimientos.collectAsState()
    HistorialScreen(
        movimientos = movimientos,
        onBackClick = onBackClick,
        onMovimientoClick = onMovimientoClick,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialScreen(
    movimientos: List<MovimientoConCategoria>,
    onBackClick: () -> Unit,
    onMovimientoClick: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Historial completo") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxSize()
            ) {
                if (movimientos.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Aún no hay movimientos registrados.",
                            color = TextoSuave,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(18.dp, 16.dp)
                    ) {
                        items(movimientos, key = { it.movimiento.id }) { item ->
                            MovimientoRow(item, onClick = onMovimientoClick)
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HistorialScreenPreview() {
    VidaOSTheme {
        HistorialScreen(movimientos = emptyList(), onBackClick = {})
    }
}
