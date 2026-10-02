package com.wuelmer.vidaos.ui.moto

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wuelmer.vidaos.VidaOSApplication
import com.wuelmer.vidaos.data.LecturaKm
import com.wuelmer.vidaos.data.MotoDao
import com.wuelmer.vidaos.ui.theme.TextoSuave
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class LecturasKmViewModel(private val motoDao: MotoDao) : ViewModel() {

    val lecturas: StateFlow<List<LecturaKm>?> = motoDao.getLecturas().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )

    fun actualizar(lectura: LecturaKm, fecha: LocalDate, km: Int) {
        viewModelScope.launch { motoDao.updateLectura(lectura.copy(fecha = fecha, km = km)) }
    }

    fun eliminar(lectura: LecturaKm) {
        viewModelScope.launch { motoDao.deleteLectura(lectura) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as VidaOSApplication
                LecturasKmViewModel(motoDao = application.database.motoDao())
            }
        }
    }
}

@Composable
fun LecturasKmRoute(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LecturasKmViewModel = viewModel(factory = LecturasKmViewModel.Factory)
) {
    val lecturas by viewModel.lecturas.collectAsState()
    LecturasKmScreen(
        lecturas = lecturas,
        onBackClick = onBackClick,
        onActualizar = viewModel::actualizar,
        onEliminar = viewModel::eliminar,
        modifier = modifier
    )
}

// Historial del odómetro, para corregir una lectura mal tipeada.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LecturasKmScreen(
    lecturas: List<LecturaKm>?,
    onBackClick: () -> Unit,
    onActualizar: (LecturaKm, LocalDate, Int) -> Unit,
    onEliminar: (LecturaKm) -> Unit,
    modifier: Modifier = Modifier
) {
    var editando by remember { mutableStateOf<LecturaKm?>(null) }
    var borrando by remember { mutableStateOf<LecturaKm?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Historial de km") },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (lecturas == null) return@Column
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)) {
                    if (lecturas.isEmpty()) {
                        Text(
                            text = "Aún no hay lecturas.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextoSuave,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                    lecturas.forEachIndexed { i, lectura ->
                        if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { editando = lectura }
                                .padding(vertical = 14.dp)
                        ) {
                            Text(formatearFecha(lectura.fecha), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            Text(formatearKm(lectura.km), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
            if (lecturas.isNotEmpty()) {
                Text(
                    text = "Toca una lectura para corregirla o borrarla. La más nueva es el km actual de la moto.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextoSuave
                )
            }
        }
    }

    editando?.let { lectura ->
        DialogoLecturaKm(
            inicial = lectura,
            kmAnterior = null,
            onDismiss = { editando = null },
            onGuardar = { fecha, km ->
                onActualizar(lectura, fecha, km)
                editando = null
            },
            onEliminar = {
                editando = null
                borrando = lectura
            }
        )
    }
    borrando?.let { lectura ->
        Confirmacion(
            titulo = "¿Eliminar esta lectura?",
            texto = "${formatearFecha(lectura.fecha)} · ${formatearKm(lectura.km)}. Los registros de mantención no se borran.",
            onConfirmar = {
                onEliminar(lectura)
                borrando = null
            },
            onCancelar = { borrando = null }
        )
    }
}
