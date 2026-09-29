package com.wuelmer.vidaos.ui.configuracion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wuelmer.vidaos.data.Tema
import com.wuelmer.vidaos.ui.theme.TextoSuave

@Composable
fun ConfiguracionRoute(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ConfiguracionViewModel = viewModel(factory = ConfiguracionViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    ConfiguracionScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onTemaChange = viewModel::onTemaChange,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfiguracionScreen(
    uiState: ConfiguracionUiState,
    onBackClick: () -> Unit,
    onTemaChange: (Tema) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Configuración") },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Apariencia",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp).selectableGroup()) {
                    Tema.entries.forEach { tema ->
                        OpcionTema(
                            tema = tema,
                            seleccionado = !uiState.cargando && tema == uiState.tema,
                            onClick = { onTemaChange(tema) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OpcionTema(tema: Tema, seleccionado: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = seleccionado, onClick = onClick, role = Role.RadioButton)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        RadioButton(selected = seleccionado, onClick = null)
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(text = etiquetaTema(tema), style = MaterialTheme.typography.bodyLarge)
            if (tema == Tema.SISTEMA) {
                Text(
                    text = "Usa el modo claro u oscuro del teléfono",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSuave
                )
            }
        }
    }
}

private fun etiquetaTema(tema: Tema): String = when (tema) {
    Tema.SISTEMA -> "Según el sistema"
    Tema.CLARO -> "Claro"
    Tema.OSCURO -> "Oscuro"
}
