package com.wuelmer.vidaos.ui.historial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wuelmer.vidaos.VidaOSApplication
import com.wuelmer.vidaos.data.MovimientoConCategoria
import com.wuelmer.vidaos.data.MovimientoDao
import com.wuelmer.vidaos.data.OrigenPago
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class HistorialUiState(
    val movimientos: List<MovimientoConCategoria> = emptyList(),
    val filtro: OrigenPago? = null,           // null = Todos
    val gastadoMesFiltro: Long = 0
)

// El filtro vive solo mientras la pantalla está abierta: al volver al Historial parte en "Todos".
class HistorialViewModel(movimientoDao: MovimientoDao) : ViewModel() {

    private val filtro = MutableStateFlow<OrigenPago?>(null)

    val uiState: StateFlow<HistorialUiState> = combine(movimientoDao.getAllConCategoria(), filtro) { todos, origen ->
        val filtrados = filtrarPorOrigen(todos, origen)
        HistorialUiState(
            movimientos = filtrados,
            filtro = origen,
            gastadoMesFiltro = gastadoEnElMes(filtrados, LocalDate.now())
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HistorialUiState()
    )

    fun onFiltroChange(origen: OrigenPago?) {
        filtro.value = origen
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as VidaOSApplication
                HistorialViewModel(movimientoDao = application.database.movimientoDao())
            }
        }
    }
}
