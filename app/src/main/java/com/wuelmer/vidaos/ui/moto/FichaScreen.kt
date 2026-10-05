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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
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
import com.wuelmer.vidaos.ui.theme.TextoSuave
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FichaUiState(
    val cargando: Boolean = true,
    val trabajo: Trabajo? = null,      // null cuando ya cargó = la ficha fue borrada
    val ajustes: List<AjusteTrabajo> = emptyList()
)

class FichaViewModel(private val trabajoId: Long, private val motoDao: MotoDao) : ViewModel() {

    val uiState: StateFlow<FichaUiState> = combine(
        motoDao.getTrabajo(trabajoId),
        motoDao.getAjustesDeTrabajo(trabajoId)
    ) { trabajo, ajustes ->
        FichaUiState(cargando = false, trabajo = trabajo, ajustes = ajustes)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FichaUiState()
    )

    fun actualizarTrabajo(nombre: String, icono: String, pasos: String, materiales: String) {
        val trabajo = uiState.value.trabajo ?: return
        viewModelScope.launch {
            motoDao.updateTrabajo(trabajo.copy(nombre = nombre, icono = icono, pasos = pasos, materiales = materiales))
        }
    }

    fun eliminarTrabajo(onEliminado: () -> Unit) {
        val trabajo = uiState.value.trabajo ?: return
        viewModelScope.launch {
            motoDao.deleteTrabajo(trabajo)
            onEliminado()
        }
    }

    // inicial = null agrega un perno al final; si no, lo actualiza.
    fun guardarAjuste(inicial: AjusteTrabajo?, parte: String, llave: String?, torqueNm: Int?, verificado: Boolean) {
        viewModelScope.launch {
            if (inicial == null) {
                val orden = (uiState.value.ajustes.maxOfOrNull { it.orden } ?: -1) + 1
                motoDao.insertAjuste(
                    AjusteTrabajo(trabajoId = trabajoId, parte = parte, llave = llave, torqueNm = torqueNm, verificado = verificado, orden = orden)
                )
            } else {
                motoDao.updateAjuste(inicial.copy(parte = parte, llave = llave, torqueNm = torqueNm, verificado = verificado))
            }
        }
    }

    // La medida que el usuario usó de verdad: queda como verificada y reemplaza la pista de foros.
    fun guardarMedida(ajuste: AjusteTrabajo, medida: String) {
        val limpia = normalizarMedida(medida)
        if (limpia.isEmpty()) return
        viewModelScope.launch { motoDao.updateAjuste(ajuste.copy(llave = limpia, verificado = true)) }
    }

    fun eliminarAjuste(ajuste: AjusteTrabajo) {
        viewModelScope.launch { motoDao.deleteAjuste(ajuste) }
    }

    companion object {
        fun factory(trabajoId: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as VidaOSApplication
                FichaViewModel(trabajoId = trabajoId, motoDao = application.database.motoDao())
            }
        }
    }
}

@Composable
fun FichaRoute(
    trabajoId: Long,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: FichaViewModel = viewModel(key = "ficha_$trabajoId", factory = FichaViewModel.factory(trabajoId))
    val uiState by viewModel.uiState.collectAsState()
    FichaScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onActualizarTrabajo = viewModel::actualizarTrabajo,
        onEliminarTrabajo = { viewModel.eliminarTrabajo(onEliminado = onBackClick) },
        onGuardarAjuste = viewModel::guardarAjuste,
        onGuardarMedida = viewModel::guardarMedida,
        onEliminarAjuste = viewModel::eliminarAjuste,
        modifier = modifier
    )
}

private sealed interface EdicionFicha {
    data object Ninguna : EdicionFicha
    data object Trabajo : EdicionFicha
    data object BorrarTrabajo : EdicionFicha
    data object NuevoAjuste : EdicionFicha
    data class Ajuste(val ajuste: AjusteTrabajo) : EdicionFicha
    data class BorrarAjuste(val ajuste: AjusteTrabajo) : EdicionFicha
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FichaScreen(
    uiState: FichaUiState,
    onBackClick: () -> Unit,
    onActualizarTrabajo: (String, String, String, String) -> Unit,
    onEliminarTrabajo: () -> Unit,
    onGuardarAjuste: (AjusteTrabajo?, String, String?, Int?, Boolean) -> Unit,
    onGuardarMedida: (AjusteTrabajo, String) -> Unit,
    onEliminarAjuste: (AjusteTrabajo) -> Unit,
    modifier: Modifier = Modifier
) {
    var edicion by remember { mutableStateOf<EdicionFicha>(EdicionFicha.Ninguna) }
    val trabajo = uiState.trabajo

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(trabajo?.let { "${it.icono} ${it.nombre}" } ?: "Ficha") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (trabajo != null) {
                        IconButton(onClick = { edicion = EdicionFicha.Trabajo }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Editar ficha")
                        }
                        IconButton(onClick = { edicion = EdicionFicha.BorrarTrabajo }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Eliminar ficha")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (uiState.cargando || trabajo == null) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text(
                    text = if (uiState.cargando) "Cargando..." else "Esta ficha ya no existe.",
                    color = TextoSuave,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TarjetaAjustes(
                ajustes = uiState.ajustes,
                onAjusteClick = { edicion = EdicionFicha.Ajuste(it) },
                onGuardarMedida = onGuardarMedida,
                onNuevoClick = { edicion = EdicionFicha.NuevoAjuste }
            )
            val materiales = lineas(trabajo.materiales)
            if (materiales.isNotEmpty()) TarjetaMateriales(materiales)
            val pasos = lineas(trabajo.pasos)
            if (pasos.isNotEmpty()) TarjetaPasos(trabajo.id, pasos, uiState.ajustes)
        }
    }

    if (trabajo == null) return
    val cerrar = { edicion = EdicionFicha.Ninguna }
    when (val e = edicion) {
        EdicionFicha.Ninguna -> Unit
        EdicionFicha.Trabajo -> DialogoTrabajo(
            inicial = trabajo,
            onDismiss = cerrar,
            onGuardar = { nombre, icono, pasos, materiales ->
                onActualizarTrabajo(nombre, icono, pasos, materiales)
                cerrar()
            }
        )
        EdicionFicha.BorrarTrabajo -> Confirmacion(
            titulo = "¿Eliminar \"${trabajo.nombre}\"?",
            texto = "Se borrarán la ficha y sus pernos. Las mantenciones enlazadas quedarán sin ficha, pero no se borra su historial.",
            onConfirmar = {
                cerrar()
                onEliminarTrabajo()
            },
            onCancelar = cerrar
        )
        EdicionFicha.NuevoAjuste, is EdicionFicha.Ajuste -> {
            val inicial = (e as? EdicionFicha.Ajuste)?.ajuste
            DialogoAjuste(
                inicial = inicial,
                onDismiss = cerrar,
                onGuardar = { parte, llave, torque, verificado ->
                    onGuardarAjuste(inicial, parte, llave, torque, verificado)
                    cerrar()
                },
                onEliminar = inicial?.let { a -> { edicion = EdicionFicha.BorrarAjuste(a) } }
            )
        }
        is EdicionFicha.BorrarAjuste -> Confirmacion(
            titulo = "¿Eliminar \"${e.ajuste.parte}\"?",
            texto = "Se quitará de la ficha.",
            onConfirmar = {
                onEliminarAjuste(e.ajuste)
                cerrar()
            },
            onCancelar = cerrar
        )
    }
}

@Composable
private fun Tarjeta(titulo: String, contenido: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextoSuave
            )
            contenido()
        }
    }
}

@Composable
private fun TarjetaAjustes(
    ajustes: List<AjusteTrabajo>,
    onAjusteClick: (AjusteTrabajo) -> Unit,
    onGuardarMedida: (AjusteTrabajo, String) -> Unit,
    onNuevoClick: () -> Unit
) {
    Tarjeta("🔧 LLAVES Y TORQUES") {
        if (ajustes.isEmpty()) {
            Text(
                text = "Este trabajo no tiene pernos que ajustar.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextoSuave,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        ajustes.forEachIndexed { i, ajuste ->
            if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            val pendiente = porConfirmar(ajuste)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAjusteClick(ajuste) }
                    .padding(vertical = 12.dp)
            ) {
                Text(ajuste.parte, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(describirAjuste(ajuste), style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = if (pendiente) "⚠ Medida por anotar" else "✓ Medida tuya",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clip(RoundedCornerShape(50))
                        .background((if (pendiente) ColorDestacado else ColorIngreso).copy(alpha = 0.3f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
                if (pendiente) CampoMedida(ajuste, onGuardar = { onGuardarMedida(ajuste, it) })
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        TextButton(onClick = onNuevoClick, modifier = Modifier.padding(top = 4.dp)) { Text("+ Agregar perno") }
    }
}

// El usuario escribe la llave o dado que usó en su moto. La medida de foros queda solo como pista.
@Composable
private fun CampoMedida(ajuste: AjusteTrabajo, onGuardar: (String) -> Unit) {
    var medida by rememberSaveable(ajuste.id) { mutableStateOf("") }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
        OutlinedTextField(
            value = medida,
            onValueChange = { medida = it.take(40) },
            label = { Text("Ingresa la medida que usaste") },
            placeholder = { Text("Ej.: 17") },
            supportingText = referenciaLlave(ajuste)?.let { ref -> { Text("Referencia (sin confirmar): $ref") } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (medida.isNotBlank()) onGuardar(medida) }),
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = { onGuardar(medida) }, enabled = medida.isNotBlank()) { Text("Guardar") }
    }
}

@Composable
private fun TarjetaMateriales(materiales: List<String>) {
    Tarjeta("🛒 MATERIALES") {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp)) {
            materiales.forEach { Text("•  $it", style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

// Checklist para ir marcando mientras se trabaja. No se guarda en la base: solo dura mientras la pantalla está abierta.
@Composable
private fun TarjetaPasos(trabajoId: Long, pasos: List<String>, ajustes: List<AjusteTrabajo>) {
    var hechos by rememberSaveable(trabajoId) { mutableStateOf(setOf<Int>()) }
    Tarjeta("✅ PASOS") {
        pasos.forEachIndexed { i, paso ->
            val hecho = i in hechos
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { hechos = if (hecho) hechos - i else hechos + i }
            ) {
                Checkbox(checked = hecho, onCheckedChange = { hechos = if (it) hechos + i else hechos - i })
                Column {
                    Text(
                        text = "${i + 1}. $paso",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (hecho) TextoSuave else MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (hecho) TextDecoration.LineThrough else null
                    )
                    notaPaso(paso, ajustes)?.let { nota ->
                        Text(
                            text = nota,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = TextoSuave
                        )
                    }
                }
            }
        }
        if (hechos.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Text(
                    text = if (hechos.size == pasos.size) "¡Listo! 🎉" else "${hechos.size} de ${pasos.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoSuave,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { hechos = emptySet() }) { Text("Reiniciar") }
            }
        }
    }
}
