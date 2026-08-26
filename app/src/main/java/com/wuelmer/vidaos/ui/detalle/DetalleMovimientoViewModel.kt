package com.wuelmer.vidaos.ui.detalle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wuelmer.vidaos.data.MovimientoConCategoria
import com.wuelmer.vidaos.data.MovimientoDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DetalleMovimientoViewModel(
    movimientoId: Long,
    private val movimientoDao: MovimientoDao
) : ViewModel() {

    val movimiento: StateFlow<MovimientoConCategoria?> = movimientoDao.getConCategoriaPorId(movimientoId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    fun eliminar(onEliminado: () -> Unit) {
        val actual = movimiento.value ?: return
        viewModelScope.launch {
            movimientoDao.delete(actual.movimiento)
            onEliminado()
        }
    }
}
