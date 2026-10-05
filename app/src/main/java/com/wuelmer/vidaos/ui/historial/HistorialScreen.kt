package com.wuelmer.vidaos.ui.historial

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wuelmer.vidaos.data.OrigenPago
import com.wuelmer.vidaos.ui.movimientos.MovimientoRow
import com.wuelmer.vidaos.ui.movimientos.color
import com.wuelmer.vidaos.ui.movimientos.etiqueta
import com.wuelmer.vidaos.ui.movimientos.formatearMonto
import com.wuelmer.vidaos.ui.theme.ColorGasto
import com.wuelmer.vidaos.ui.theme.TextoSuave
import com.wuelmer.vidaos.ui.theme.VidaOSTheme

@Composable
fun HistorialRoute(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onMovimientoClick: (Long) -> Unit = {},
    viewModel: HistorialViewModel = viewModel(factory = HistorialViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    HistorialScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onFiltroChange = viewModel::onFiltroChange,
        onMovimientoClick = onMovimientoClick,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialScreen(
    uiState: HistorialUiState,
    onBackClick: () -> Unit,
    onFiltroChange: (OrigenPago?) -> Unit = {},
    onMovimientoClick: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val filtro = uiState.filtro
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FiltrosOrigen(filtro = filtro, onFiltroChange = onFiltroChange)
            if (filtro != null) GastadoConFiltro(filtro, uiState.gastadoMesFiltro)
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxSize()
            ) {
                if (uiState.movimientos.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (filtro == null) {
                                "Aún no hay movimientos registrados."
                            } else {
                                "No hay movimientos con ${filtro.etiqueta().lowercase()}."
                            },
                            color = TextoSuave,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(18.dp, 16.dp)
                    ) {
                        items(uiState.movimientos, key = { it.movimiento.id }) { item ->
                            MovimientoRow(item, onClick = onMovimientoClick)
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }
        }
    }
}

// Todos · Débito · Crédito · Efectivo. Uno a la vez.
@Composable
private fun FiltrosOrigen(filtro: OrigenPago?, onFiltroChange: (OrigenPago?) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        FilterChip(selected = filtro == null, onClick = { onFiltroChange(null) }, label = { Text("Todos") })
        OrigenPago.entries.forEach { origen ->
            FilterChip(
                selected = filtro == origen,
                onClick = { onFiltroChange(origen) },
                label = { Text(origen.etiqueta()) },
                leadingIcon = {
                    Box(modifier = Modifier.size(8.dp).background(origen.color(), CircleShape))
                }
            )
        }
    }
}

@Composable
private fun GastadoConFiltro(origen: OrigenPago, total: Long) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(18.dp, 14.dp)) {
            Box(modifier = Modifier.size(10.dp).background(origen.color(), CircleShape))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Gastado este mes con ${origen.etiqueta().lowercase()}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextoSuave,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = formatearMonto(total),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = ColorGasto
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HistorialScreenPreview() {
    VidaOSTheme {
        HistorialScreen(uiState = HistorialUiState(filtro = OrigenPago.CREDITO, gastadoMesFiltro = 123_456), onBackClick = {})
    }
}
