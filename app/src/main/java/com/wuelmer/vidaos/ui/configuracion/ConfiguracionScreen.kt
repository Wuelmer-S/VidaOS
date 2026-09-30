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
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.material3.Switch
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
import com.wuelmer.vidaos.data.UnidadPeso
import com.wuelmer.vidaos.ui.navegacion.Modulo
import com.wuelmer.vidaos.ui.navegacion.modulosVisibles
import com.wuelmer.vidaos.ui.navegacion.puedeOcultar
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
        onModuloVisibleChange = viewModel::onModuloVisibleChange,
        onUnidadPesoChange = viewModel::onUnidadPesoChange,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfiguracionScreen(
    uiState: ConfiguracionUiState,
    onBackClick: () -> Unit,
    onTemaChange: (Tema) -> Unit,
    onModuloVisibleChange: (Modulo, Boolean) -> Unit,
    onUnidadPesoChange: (UnidadPeso) -> Unit,
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
                        OpcionRadio(
                            titulo = etiquetaTema(tema),
                            descripcion = if (tema == Tema.SISTEMA) "Usa el modo claro u oscuro del teléfono" else null,
                            seleccionado = !uiState.cargando && tema == uiState.tema,
                            onClick = { onTemaChange(tema) }
                        )
                    }
                }
            }

            Text(
                text = "Módulos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 10.dp)
            )
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    val visibles = modulosVisibles(uiState.modulosOcultos)
                    Modulo.entries.forEach { modulo ->
                        val visible = modulo in visibles
                        OpcionModulo(
                            modulo = modulo,
                            visible = visible,
                            habilitado = !uiState.cargando && (!visible || puedeOcultar(modulo, uiState.modulosOcultos)),
                            onChange = { onModuloVisibleChange(modulo, it) }
                        )
                    }
                }
            }
            Text(
                text = "Ocultar un módulo no borra sus datos. Siempre queda al menos uno visible.",
                style = MaterialTheme.typography.bodySmall,
                color = TextoSuave
            )

            Text(
                text = "Gym",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 10.dp)
            )
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp).selectableGroup()) {
                    UnidadPeso.entries.forEach { unidad ->
                        OpcionRadio(
                            titulo = etiquetaUnidad(unidad),
                            descripcion = null,
                            seleccionado = !uiState.cargando && unidad == uiState.unidadPeso,
                            onClick = { onUnidadPesoChange(unidad) }
                        )
                    }
                }
            }
            Text(
                text = "Los pesos se guardan siempre en kg; cambiar la unidad no modifica tus registros.",
                style = MaterialTheme.typography.bodySmall,
                color = TextoSuave
            )
        }
    }
}

@Composable
private fun OpcionRadio(titulo: String, descripcion: String?, seleccionado: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = seleccionado, onClick = onClick, role = Role.RadioButton)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        RadioButton(selected = seleccionado, onClick = null)
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(text = titulo, style = MaterialTheme.typography.bodyLarge)
            if (descripcion != null) {
                Text(
                    text = descripcion,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSuave
                )
            }
        }
    }
}

@Composable
private fun OpcionModulo(
    modulo: Modulo,
    visible: Boolean,
    habilitado: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = visible, enabled = habilitado, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Icon(modulo.icono, contentDescription = null, tint = TextoSuave)
        Text(
            text = modulo.etiqueta,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f).padding(start = 12.dp)
        )
        Switch(checked = visible, onCheckedChange = null, enabled = habilitado)
    }
}

private fun etiquetaUnidad(unidad: UnidadPeso): String = when (unidad) {
    UnidadPeso.KG -> "Kilogramos (kg)"
    UnidadPeso.LB -> "Libras (lb)"
}

private fun etiquetaTema(tema: Tema): String = when (tema) {
    Tema.SISTEMA -> "Según el sistema"
    Tema.CLARO -> "Claro"
    Tema.OSCURO -> "Oscuro"
}
