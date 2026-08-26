package com.wuelmer.vidaos.ui.historial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wuelmer.vidaos.VidaOSApplication
import com.wuelmer.vidaos.data.MovimientoConCategoria
import com.wuelmer.vidaos.data.MovimientoDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HistorialViewModel(movimientoDao: MovimientoDao) : ViewModel() {

    val movimientos: StateFlow<List<MovimientoConCategoria>> = movimientoDao.getAllConCategoria()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as VidaOSApplication
                HistorialViewModel(movimientoDao = application.database.movimientoDao())
            }
        }
    }
}
