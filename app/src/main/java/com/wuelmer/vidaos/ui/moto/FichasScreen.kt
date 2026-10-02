package com.wuelmer.vidaos.ui.moto

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wuelmer.vidaos.VidaOSApplication
import com.wuelmer.vidaos.data.AjusteTrabajo
import com.wuelmer.vidaos.data.MotoDao
import com.wuelmer.vidaos.data.Trabajo
import com.wuelmer.vidaos.ui.theme.ColorDestacado
import com.wuelmer.vidaos.ui.theme.ColorIngreso
import com.wuelmer.vidaos.ui.theme.Indigo100
import com.wuelmer.vidaos.ui.theme.TextoSuave
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FichaResumen(val trabajo: Trabajo, val ajustes: List<AjusteTrabajo>)

class FichasViewModel(private val motoDao: MotoDao) : ViewModel() {

    val fichas: StateFlow<List<FichaResumen>?> = combine(
        motoDao.getTrabajos(),
        motoDao.getAjustes()
    ) { trabajos, ajustes ->
        val porTrabajo = ajustes.groupBy { it.trabajoId }
        trabajos.map { FichaResumen(it, porTrabajo[it.id].orEmpty()) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )

    fun crear(nombre: String, icono: String, pasos: String, materiales: String) {
        viewModelScope.launch {
            motoDao.insertTrabajo(Trabajo(nombre = nombre, icono = icono, pasos = pasos, materiales = materiales))
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as VidaOSApplication
                FichasViewModel(motoDao = application.database.motoDao())
            }
        }
    }
}

@Composable
fun FichasRoute(
    onFichaClick: (trabajoId: Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FichasViewModel = viewModel(factory = FichasViewModel.Factory)
) {
    val fichas by viewModel.fichas.collectAsState()
    FichasScreen(fichas = fichas, onFichaClick = onFichaClick, onCrear = viewModel::crear, modifier = modifier)
}

// Pestaña "Fichas" de Moto: "¿qué necesito para...?" cada trabajo que hace el usuario.
@Composable
fun FichasScreen(
    fichas: List<FichaResumen>?,
    onFichaClick: (Long) -> Unit,
    onCrear: (String, String, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var creando by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (fichas == null) return@Column
        Text(
            text = "¿Qué necesito para…?",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "Llaves, torques, materiales y pasos de cada trabajo. Las medidas con ⚠ son de foros: " +
                "cuando las pruebes con tus dados, márcalas como verificadas.",
            style = MaterialTheme.typography.bodySmall,
            color = TextoSuave
        )
        fichas.forEach { ficha ->
            val pendientes = ficha.ajustes.count(::porConfirmar)
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onFichaClick(ficha.trabajo.id) }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(44.dp).clip(CircleShape).background(Indigo100)
                    ) {
                        Text(ficha.trabajo.icono, fontSize = 20.sp)
                    }
                    Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(ficha.trabajo.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                        Text(resumenAjustes(ficha.ajustes), style = MaterialTheme.typography.bodySmall, color = TextoSuave)
                    }
                    if (ficha.ajustes.isNotEmpty()) {
                        Text(
                            text = if (pendientes == 0) "✓" else "⚠",
                            color = if (pendientes == 0) ColorIngreso else ColorDestacado,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        TextButton(onClick = { creando = true }) { Text("+ Nueva ficha") }
    }

    if (creando) {
        DialogoTrabajo(
            inicial = null,
            onDismiss = { creando = false },
            onGuardar = { nombre, icono, pasos, materiales ->
                onCrear(nombre, icono, pasos, materiales)
                creando = false
            }
        )
    }
}
