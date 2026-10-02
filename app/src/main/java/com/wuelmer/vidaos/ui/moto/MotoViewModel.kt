package com.wuelmer.vidaos.ui.moto

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wuelmer.vidaos.VidaOSApplication
import com.wuelmer.vidaos.data.LecturaKm
import com.wuelmer.vidaos.data.MotoDao
import com.wuelmer.vidaos.data.RegistroMantencion
import com.wuelmer.vidaos.data.TipoMantencion
import com.wuelmer.vidaos.data.Trabajo
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class MotoUiState(
    val ultimaLectura: LecturaKm?,
    val ritmoKmDia: Double?,
    val estados: List<EstadoMantencion>,
    val trabajos: List<Trabajo> = emptyList()
) {
    // La de mayor % de vida usada; null si no hay ninguna registrada.
    val masUrgente: EstadoMantencion? get() = estados.firstOrNull { it.progreso != null }
}

class MotoViewModel(private val motoDao: MotoDao) : ViewModel() {

    // null mientras carga, para no mostrar "sin km" un instante.
    val uiState: StateFlow<MotoUiState?> = combine(
        motoDao.getLecturas(),
        motoDao.getTiposActivos(),
        motoDao.getUltimosRegistros(),
        motoDao.getTrabajos()
    ) { lecturas, tipos, ultimos, trabajos ->
        val hoy = LocalDate.now()
        val ultimaLectura = lecturas.firstOrNull()
        val ultimoPorTipo = ultimos.associateBy { it.tipoId }
        MotoUiState(
            ultimaLectura = ultimaLectura,
            ritmoKmDia = ritmoKmPorDia(lecturas, hoy),
            estados = ordenarPorUrgencia(
                tipos.map { calcularEstado(it, ultimoPorTipo[it.id], ultimaLectura?.km, hoy) }
            ),
            trabajos = trabajos
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )

    fun anotarKm(fecha: LocalDate, km: Int) {
        viewModelScope.launch {
            motoDao.insertLectura(LecturaKm(fecha = fecha, km = km))
        }
    }

    // Registro rápido con la fecha de hoy y el km actual. Sin km anotado no hace nada (la UI lo pide antes).
    fun hechoHoy(tipo: TipoMantencion) {
        val km = uiState.value?.ultimaLectura?.km ?: return
        viewModelScope.launch {
            motoDao.insertRegistro(RegistroMantencion(tipoId = tipo.id, fecha = LocalDate.now(), km = km))
        }
    }

    fun crearTipo(intervalo: IntervaloValido, icono: String, trabajoId: Long?) {
        viewModelScope.launch {
            motoDao.insertTipo(
                TipoMantencion(
                    nombre = intervalo.nombre,
                    icono = icono,
                    cadaKm = intervalo.cadaKm,
                    cadaDias = intervalo.cadaDias,
                    trabajoId = trabajoId
                )
            )
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
