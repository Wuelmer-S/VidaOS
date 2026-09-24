package com.wuelmer.vidaos.ui.gym

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wuelmer.vidaos.ui.theme.TextoSuave
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun GymRoute(
    onIniciarSesion: (diaId: Long) -> Unit,
    onSesionClick: (sesionId: Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GymViewModel = viewModel(factory = GymViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    GymScreen(
        uiState = uiState,
        onIniciarSesion = onIniciarSesion,
        onSesionClick = onSesionClick,
        modifier = modifier
    )
}

@Composable
fun GymScreen(
    uiState: GymUiState,
    onIniciarSesion: (diaId: Long) -> Unit,
    onSesionClick: (sesionId: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val formatoFecha = remember {
        DateTimeFormatter.ofPattern("d 'de' MMMM", Locale.forLanguageTag("es-ES"))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Nueva sesión",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium
        )

        uiState.dias.forEach { dia ->
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onIniciarSesion(dia.id) }
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = dia.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Toca para iniciar la sesión",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoSuave
                    )
                }
            }
        }

        Text(
            text = "Últimas sesiones",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        if (uiState.ultimasSesiones.isEmpty()) {
            Text(
                text = "Aún no has registrado sesiones.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextoSuave
            )
        }
        uiState.ultimasSesiones.forEach { sesion ->
            val nombreDia = uiState.dias.firstOrNull { it.id == sesion.diaRutinaId }?.nombre.orEmpty()
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSesionClick(sesion.id) }
            ) {
                Text(
                    text = "$nombreDia · ${sesion.fecha.format(formatoFecha)}",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(18.dp)
                )
            }
        }
    }
}
