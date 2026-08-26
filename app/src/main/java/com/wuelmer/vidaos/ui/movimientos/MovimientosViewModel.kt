package com.wuelmer.vidaos.ui.movimientos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wuelmer.vidaos.VidaOSApplication
import com.wuelmer.vidaos.data.CategoriaDao
import com.wuelmer.vidaos.data.MovimientoDao
import com.wuelmer.vidaos.data.TipoCategoria
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class MovimientosViewModel(
    movimientoDao: MovimientoDao,
    private val categoriaDao: CategoriaDao
) : ViewModel() {

    val uiState: StateFlow<MovimientosUiState> = combine(
        movimientoDao.getAllConCategoria(),
        movimientoDao.getTotalGastadoEntreFechas(inicioDeMes(), finDeMes()),
        movimientoDao.getGastosPorCategoriaEntreFechas(inicioDeMes(), finDeMes()),
        movimientoDao.getGastosPorOrigenEntreFechas(inicioDeMes(), finDeMes())
    ) { movimientos, total, gastosPorCategoria, gastosPorOrigen ->
        MovimientosUiState(
            movimientos = movimientos,
            totalGastadoMes = total,
            gastosPorCategoria = gastosPorCategoria,
            gastosPorOrigen = gastosPorOrigen
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MovimientosUiState()
    )

    private fun inicioDeMes(): LocalDate = LocalDate.now().withDayOfMonth(1)

    private fun finDeMes(): LocalDate {
        val hoy = LocalDate.now()
        return hoy.withDayOfMonth(hoy.lengthOfMonth())
    }

    fun agregarCategoria(nombre: String, tipo: TipoCategoria) {
        if (nombre.isBlank()) return
        viewModelScope.launch {
            categoriaDao.agregar(nombre, tipo)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as VidaOSApplication
                MovimientosViewModel(
                    movimientoDao = application.database.movimientoDao(),
                    categoriaDao = application.database.categoriaDao()
                )
            }
        }
    }
}
