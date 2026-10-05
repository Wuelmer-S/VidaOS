package com.wuelmer.vidaos.ui.configuracion

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wuelmer.vidaos.data.RANGO_META_SEMANAL
import com.wuelmer.vidaos.data.Tema
import com.wuelmer.vidaos.data.UnidadPeso
import com.wuelmer.vidaos.ui.navegacion.Modulo
import com.wuelmer.vidaos.ui.navegacion.modulosVisibles
import com.wuelmer.vidaos.ui.navegacion.puedeOcultar
import com.wuelmer.vidaos.ui.theme.TextoSuave
import java.time.LocalDate

@Composable
fun ConfiguracionRoute(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ConfiguracionViewModel = viewModel(factory = ConfiguracionViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val mensajeRespaldo by viewModel.mensajeRespaldo.collectAsState()
    val procesandoRespaldo by viewModel.procesandoRespaldo.collectAsState()
    ConfiguracionScreen(
        uiState = uiState,
        mensajeRespaldo = mensajeRespaldo,
        procesandoRespaldo = procesandoRespaldo,
        onExportar = viewModel::exportarRespaldo,
        onRestaurar = viewModel::restaurarRespaldo,
        onMensajeRespaldoVisto = viewModel::onMensajeRespaldoVisto,
        onBackClick = onBackClick,
        onTemaChange = viewModel::onTemaChange,
        onModuloVisibleChange = viewModel::onModuloVisibleChange,
        onUnidadPesoChange = viewModel::onUnidadPesoChange,
        onMetaSemanalChange = viewModel::onMetaSemanalChange,
        onAvisosMotoChange = viewModel::onAvisosMotoChange,
        onHoraAvisosMotoChange = viewModel::onHoraAvisosMotoChange,
        onRecordatorioGastosChange = viewModel::onRecordatorioGastosChange,
        onHoraRecordatorioGastosChange = viewModel::onHoraRecordatorioGastosChange,
        onRevisarAvisos = viewModel::revisarAvisosAhora,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfiguracionScreen(
    uiState: ConfiguracionUiState,
    mensajeRespaldo: String?,
    procesandoRespaldo: Boolean,
    onExportar: (Uri) -> Unit,
    onRestaurar: (Uri) -> Unit,
    onMensajeRespaldoVisto: () -> Unit,
    onBackClick: () -> Unit,
    onTemaChange: (Tema) -> Unit,
    onModuloVisibleChange: (Modulo, Boolean) -> Unit,
    onUnidadPesoChange: (UnidadPeso) -> Unit,
    onMetaSemanalChange: (Int) -> Unit,
    onAvisosMotoChange: (Boolean) -> Unit,
    onHoraAvisosMotoChange: (Int) -> Unit,
    onRecordatorioGastosChange: (Boolean) -> Unit,
    onHoraRecordatorioGastosChange: (Int) -> Unit,
    onRevisarAvisos: () -> Unit,
    modifier: Modifier = Modifier
) {
    var respaldoARestaurar by remember { mutableStateOf<Uri?>(null) }
    val exportarLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri -> uri?.let(onExportar) }
    val restaurarLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> respaldoARestaurar = uri }

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
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Meta semanal", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = "Sesiones por semana para sumar a la racha",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextoSuave
                        )
                    }
                    val meta = uiState.metaSemanal
                    TextButton(
                        onClick = { onMetaSemanalChange(meta - 1) },
                        enabled = !uiState.cargando && meta > RANGO_META_SEMANAL.first
                    ) { Text("−", style = MaterialTheme.typography.titleLarge) }
                    Text(
                        text = "$meta",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(
                        onClick = { onMetaSemanalChange(meta + 1) },
                        enabled = !uiState.cargando && meta < RANGO_META_SEMANAL.last
                    ) { Text("+", style = MaterialTheme.typography.titleLarge) }
                }
            }

            SeccionAvisos(
                uiState = uiState,
                onAvisosMotoChange = onAvisosMotoChange,
                onHoraAvisosMotoChange = onHoraAvisosMotoChange,
                onRecordatorioGastosChange = onRecordatorioGastosChange,
                onHoraRecordatorioGastosChange = onHoraRecordatorioGastosChange,
                onRevisarAhora = onRevisarAvisos
            )

            Text(
                text = "Respaldo",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 10.dp)
            )
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Guarda una copia de Finanzas y Gym en un archivo (por ejemplo en Drive o Descargas). " +
                            "Si cambias de teléfono o borras los datos de la app, puedes restaurarla.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoSuave
                    )
                    OutlinedButton(
                        onClick = { exportarLauncher.launch("vidaos-respaldo-${LocalDate.now()}.db") },
                        enabled = !procesandoRespaldo,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Exportar respaldo") }
                    OutlinedButton(
                        onClick = { restaurarLauncher.launch(arrayOf("*/*")) },
                        enabled = !procesandoRespaldo,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Restaurar respaldo") }
                    if (procesandoRespaldo) {
                        Text("Procesando...", style = MaterialTheme.typography.bodySmall, color = TextoSuave)
                    }
                }
            }
        }
    }

    DialogosRespaldo(
        respaldoARestaurar = respaldoARestaurar,
        mensajeRespaldo = mensajeRespaldo,
        onConfirmarRestaurar = { uri ->
            respaldoARestaurar = null
            onRestaurar(uri)
        },
        onCancelarRestaurar = { respaldoARestaurar = null },
        onMensajeVisto = onMensajeRespaldoVisto
    )
}

@Composable
private fun DialogosRespaldo(
    respaldoARestaurar: Uri?,
    mensajeRespaldo: String?,
    onConfirmarRestaurar: (Uri) -> Unit,
    onCancelarRestaurar: () -> Unit,
    onMensajeVisto: () -> Unit
) {
    if (respaldoARestaurar != null) {
        AlertDialog(
            onDismissRequest = onCancelarRestaurar,
            title = { Text("¿Restaurar este respaldo?") },
            text = {
                Text(
                    "Se reemplazarán TODOS tus datos actuales de Finanzas y Gym por los del archivo. " +
                        "Si no estás seguro, exporta primero un respaldo de lo que tienes ahora. La app se reiniciará."
                )
            },
            confirmButton = {
                TextButton(onClick = { onConfirmarRestaurar(respaldoARestaurar) }) { Text("Restaurar") }
            },
            dismissButton = {
                TextButton(onClick = onCancelarRestaurar) { Text("Cancelar") }
            }
        )
    }
    if (mensajeRespaldo != null) {
        AlertDialog(
            onDismissRequest = onMensajeVisto,
            text = { Text(mensajeRespaldo) },
            confirmButton = { TextButton(onClick = onMensajeVisto) { Text("OK") } }
        )
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
