package com.wuelmer.vidaos.ui.configuracion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wuelmer.vidaos.VidaOSApplication
import com.wuelmer.vidaos.data.PreferenciasRepository
import com.wuelmer.vidaos.data.Tema
import com.wuelmer.vidaos.ui.navegacion.Modulo
import com.wuelmer.vidaos.ui.navegacion.puedeOcultar
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ConfiguracionUiState(
    val cargando: Boolean = true,
    val tema: Tema = Tema.SISTEMA,
    val modulosOcultos: Set<String> = emptySet()
)

class ConfiguracionViewModel(private val preferencias: PreferenciasRepository) : ViewModel() {

    val uiState: StateFlow<ConfiguracionUiState> = preferencias.preferencias
        .map { ConfiguracionUiState(cargando = false, tema = it.tema, modulosOcultos = it.modulosOcultos) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ConfiguracionUiState()
        )

    fun onTemaChange(tema: Tema) {
        viewModelScope.launch { preferencias.setTema(tema) }
    }

    fun onModuloVisibleChange(modulo: Modulo, visible: Boolean) {
        if (!visible && !puedeOcultar(modulo, uiState.value.modulosOcultos)) return
        viewModelScope.launch { preferencias.setModuloOculto(modulo.name, oculto = !visible) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as VidaOSApplication
                ConfiguracionViewModel(preferencias = application.preferencias)
            }
        }
    }
}
