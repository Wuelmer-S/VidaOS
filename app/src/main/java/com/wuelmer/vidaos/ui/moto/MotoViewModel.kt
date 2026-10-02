package com.wuelmer.vidaos.ui.moto

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wuelmer.vidaos.VidaOSApplication
import com.wuelmer.vidaos.data.LecturaKm
import com.wuelmer.vidaos.data.MotoDao
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class MotoUiState(
    val ultimaLectura: LecturaKm?,
    val ritmoKmDia: Double?,
    val estados: List<EstadoMantencion>
) {
    // La de mayor % de vida usada; null si no hay ninguna registrada.
    val masUrgente: EstadoMantencion? get() = estados.firstOrNull { it.progreso != null }
}

class MotoViewModel(private val motoDao: MotoDao) : ViewModel() {

    // null mientras carga, para no mostrar "sin km" un instante.
    val uiState: StateFlow<MotoUiState?> = combine(
        motoDao.getLecturas(),
        motoDao.getTiposActivos(),
        motoDao.getUltimosRegistros()
    ) { lecturas, tipos, ultimos ->
        val hoy = LocalDate.now()
        val ultimaLectura = lecturas.firstOrNull()
        val ultimoPorTipo = ultimos.associateBy { it.tipoId }
        MotoUiState(
            ultimaLectura = ultimaLectura,
            ritmoKmDia = ritmoKmPorDia(lecturas, hoy),
            estados = ordenarPorUrgencia(
                tipos.map { calcularEstado(it, ultimoPorTipo[it.id], ultimaLectura?.km, hoy) }
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )

    fun anotarKm(km: Int) {
        viewModelScope.launch {
            motoDao.insertLectura(LecturaKm(fecha = LocalDate.now(), km = km))
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as VidaOSApplication
                MotoViewModel(motoDao = application.database.motoDao())
            }
        }
    }
}
