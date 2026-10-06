package com.wuelmer.vidaos.ui.tarjeta

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wuelmer.vidaos.VidaOSApplication
import com.wuelmer.vidaos.data.EstadoCuenta
import com.wuelmer.vidaos.data.MovimientoDao
import com.wuelmer.vidaos.data.OperacionTarjeta
import com.wuelmer.vidaos.data.TarjetaDao
import com.wuelmer.vidaos.data.TipoOperacionTarjeta
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class FacturaResumen(val estado: EstadoCuenta, val pago: EstadoPago)

data class TarjetaUiState(
    val cargando: Boolean = true,
    // La factura más nueva (null = todavía no se importa ninguna).
    val ultima: FacturaResumen? = null,
    val periodo: PeriodoActual? = null,
    val cupoDisponible: Long = 0,
    // Cuotas que entran en la próxima factura.
    val cuotas: List<CuotaPeriodo> = emptyList(),
    val meses: List<Pair<String, Long>> = emptyList(),
    val cargos: List<OperacionTarjeta> = emptyList(),
    val facturas: List<FacturaResumen> = emptyList()
)

class TarjetaViewModel(
    private val tarjetaDao: TarjetaDao,
    movimientoDao: MovimientoDao
) : ViewModel() {

    val uiState: StateFlow<TarjetaUiState> = combine(
        tarjetaDao.getEstados(),
        tarjetaDao.getOperaciones(),
        movimientoDao.getAll()
    ) { estados, operaciones, movimientos ->
        val hoy = LocalDate.now()
        val opsPorEstado = operaciones.groupBy { it.estadoId }
        // estados viene del más nuevo al más viejo: la "siguiente" de cada factura es la anterior en la lista.
        val facturas = estados.mapIndexed { i, estado ->
            val siguiente = estados.getOrNull(i - 1)
            val pagadoBanco = siguiente?.let { pagosConfirmados(estado, opsPorEstado[it.id].orEmpty()) }
            val pagadoApp = pagosAnotados(movimientos, estado, hasta = siguiente?.fechaEstado)
            FacturaResumen(estado, estadoPago(estado, pagadoApp, pagadoBanco, hoy))
        }
        val ultima = facturas.firstOrNull()
        val opsUltima = ultima?.let { opsPorEstado[it.estado.id] }.orEmpty()
        TarjetaUiState(
            cargando = false,
            ultima = ultima,
            periodo = ultima?.let { periodoActual(it.estado, opsUltima, movimientos, hoy) },
            cupoDisponible = ultima?.let { cupoDisponibleEstimado(it.estado, movimientos) } ?: 0,
            cuotas = ultima?.let { cuotasDelPeriodo(it.estado, opsUltima, movimientos, hoy) }.orEmpty(),
            meses = ultima?.let { mesesVencimientos(it.estado, movimientos) }.orEmpty(),
            cargos = opsUltima.filter { it.tipo == TipoOperacionTarjeta.CARGO },
            facturas = facturas
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TarjetaUiState()
    )

    // "Ya la pagué" para cuando el pago no se anotó en la app. Se puede deshacer.
    fun marcarPagada(estado: EstadoCuenta, pagada: Boolean) {
        viewModelScope.launch { tarjetaDao.updateEstado(estado.copy(pagadaManual = pagada)) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as VidaOSApplication
                TarjetaViewModel(
                    tarjetaDao = application.database.tarjetaDao(),
                    movimientoDao = application.database.movimientoDao()
                )
            }
        }
    }
}
