package com.wuelmer.vidaos.ui.movimientos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wuelmer.vidaos.VidaOSApplication
import com.wuelmer.vidaos.data.CategoriaDao
import com.wuelmer.vidaos.data.MovimientoDao
import com.wuelmer.vidaos.data.TarjetaDao
import com.wuelmer.vidaos.data.TipoCategoria
import com.wuelmer.vidaos.ui.tarjeta.cuotasDelPeriodo
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class MovimientosViewModel(
    movimientoDao: MovimientoDao,
    tarjetaDao: TarjetaDao,
    private val categoriaDao: CategoriaDao
) : ViewModel() {

    // estados viene del más nuevo al más viejo.
    private val cuotas = combine(
        tarjetaDao.getEstados(),
        tarjetaDao.getOperaciones(),
        movimientoDao.getAll()
    ) { estados, operaciones, movimientos ->
        val ultimo = estados.firstOrNull()
        cuotasDelPeriodo(ultimo, operaciones.filter { it.estadoId == ultimo?.id }, movimientos, LocalDate.now())
    }

    val uiState: StateFlow<MovimientosUiState> = combine(
        movimientoDao.getAllConCategoria(),
        movimientoDao.getTotalGastadoEntreFechas(inicioDeMes(), finDeMes()),
        movimientoDao.getGastosPorCategoriaEntreFechas(inicioDeMes(), finDeMes()),
        movimientoDao.getGastosPorOrigenEntreFechas(inicioDeMes(), finDeMes()),
        cuotas
    ) { movimientos, total, gastosPorCategoria, gastosPorOrigen, cuotas ->
        MovimientosUiState(
            movimientos = movimientos,
            totalGastadoMes = total,
            gastosPorCategoria = gastosPorCategoria,
            gastosPorOrigen = gastosPorOrigen,
            cuotasProximaFactura = cuotas
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
                    tarjetaDao = application.database.tarjetaDao(),
                    categoriaDao = application.database.categoriaDao()
                )
            }
        }
    }
}
