package com.wuelmer.vidaos.ui.gym

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wuelmer.vidaos.VidaOSApplication
import com.wuelmer.vidaos.data.SerieGym
import com.wuelmer.vidaos.data.TipoEjercicio
import com.wuelmer.vidaos.ui.theme.ColorGasto
import com.wuelmer.vidaos.ui.theme.TextoSuave
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DetalleSesionGymRoute(
    sesionId: Long,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val application = LocalContext.current.applicationContext as VidaOSApplication
    val factory = remember(sesionId) {
        viewModelFactory {
            initializer {
                DetalleSesionGymViewModel(sesionId = sesionId, gymDao = application.database.gymDao())
            }
        }
    }
    val viewModel: DetalleSesionGymViewModel = viewModel(key = "detalle_sesion_gym_$sesionId", factory = factory)
    val uiState by viewModel.uiState.collectAsState()

    DetalleSesionGymScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onEliminarConfirmado = { viewModel.eliminar(onEliminada = onBackClick) },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleSesionGymScreen(
    uiState: DetalleSesionUiState,
    onBackClick: () -> Unit,
    onEliminarConfirmado: () -> Unit,
    modifier: Modifier = Modifier
) {
    var mostrarConfirmacion by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(uiState.nombreDia.ifEmpty { "Sesión de gym" }) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { innerPadding ->
        val sesion = uiState.sesion
        if (uiState.cargando || sesion == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (uiState.cargando) "Cargando..." else "Esta sesión ya no existe.",
                    color = TextoSuave,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            return@Scaffold
        }

        val fecha = remember(sesion.fecha) {
            sesion.fecha
                .format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM yyyy", Locale.forLanguageTag("es-ES")))
                .replaceFirstChar { it.uppercase() }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(text = fecha, style = MaterialTheme.typography.bodyMedium, color = TextoSuave)

            uiState.grupos.forEach { grupo ->
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = grupo.ejercicio.nombre,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        grupo.series.forEachIndexed { i, serie ->
                            Text(
                                text = "Serie ${i + 1}: ${textoSerie(serie, grupo.ejercicio.tipo, grupo.ejercicio.unilateral)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            Button(
                onClick = { mostrarConfirmacion = true },
                colors = ButtonDefaults.buttonColors(containerColor = ColorGasto),
                shape = RoundedCornerShape(50),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Eliminar sesión")
            }
        }
    }

    if (mostrarConfirmacion) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacion = false },
            title = { Text("¿Eliminar esta sesión?") },
            text = { Text("Se borrarán la sesión y todas sus series. Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    mostrarConfirmacion = false
                    onEliminarConfirmado()
                }) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmacion = false }) { Text("Cancelar") }
            }
        )
    }
}

private fun textoSerie(serie: SerieGym, tipo: TipoEjercicio, unilateral: Boolean): String {
    val lado = if (unilateral) "/lado" else ""
    return when (tipo) {
        TipoEjercicio.CON_PESO -> "${serie.repeticiones} reps$lado × ${formatearPeso(serie.pesoKg)} kg"
        TipoEjercicio.PESO_CORPORAL -> "${serie.repeticiones} reps$lado"
        TipoEjercicio.TIEMPO -> "${serie.segundos} s"
    }
}

private fun formatearPeso(pesoKg: Double?): String {
    if (pesoKg == null) return "-"
    return if (pesoKg % 1.0 == 0.0) pesoKg.toInt().toString() else pesoKg.toString().replace('.', ',')
}
