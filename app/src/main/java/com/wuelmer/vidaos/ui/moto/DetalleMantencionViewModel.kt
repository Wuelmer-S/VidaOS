package com.wuelmer.vidaos.ui.moto

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wuelmer.vidaos.VidaOSApplication
import com.wuelmer.vidaos.data.HechoPor
import com.wuelmer.vidaos.data.LecturaKm
import com.wuelmer.vidaos.data.MotoDao
import com.wuelmer.vidaos.data.RegistroMantencion
import com.wuelmer.vidaos.data.TipoMantencion
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DetalleMantencionUiState(
    val cargando: Boolean = true,
    val estado: EstadoMantencion? = null,   // null cuando ya cargó = el tipo fue borrado
    val registros: List<RegistroMantencion> = emptyList(),
    val kmActual: Int? = null
)

class DetalleMantencionViewModel(
    private val tipoId: Long,
    private val motoDao: MotoDao
) : ViewModel() {

    val uiState: StateFlow<DetalleMantencionUiState> = combine(
        motoDao.getTipo(tipoId),
        motoDao.getRegistrosDeTipo(tipoId),
        motoDao.getLecturas()
    ) { tipo, registros, lecturas ->
        val kmActual = lecturas.firstOrNull()?.km
        DetalleMantencionUiState(
            cargando = false,
            estado = tipo?.let { calcularEstado(it, registros.firstOrNull(), kmActual, LocalDate.now()) },
            registros = registros,
            kmActual = kmActual
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DetalleMantencionUiState()
    )

    // inicial = null crea un registro nuevo; si no, lo actualiza.
    fun guardarRegistro(inicial: RegistroMantencion?, fecha: LocalDate, km: Int, hechoPor: HechoPor, notas: String?) {
        viewModelScope.launch {
            if (inicial == null) {
                motoDao.insertRegistro(RegistroMantencion(tipoId = tipoId, fecha = fecha, km = km, hechoPor = hechoPor, notas = notas))
            } else {
                motoDao.updateRegistro(inicial.copy(fecha = fecha, km = km, hechoPor = hechoPor, notas = notas))
            }
            val hoy = LocalDate.now()
            if (debeActualizarOdometro(km, fecha, uiState.value.kmActual, hoy)) {
                motoDao.insertLectura(LecturaKm(fecha = hoy, km = km))
            }
        }
    }

    fun eliminarRegistro(registro: RegistroMantencion) {
        viewModelScope.launch { motoDao.deleteRegistro(registro) }
    }

    fun actualizarTipo(tipo: TipoMantencion, intervalo: IntervaloValido, icono: String) {
        viewModelScope.launch {
            motoDao.updateTipo(
                tipo.copy(nombre = intervalo.nombre, icono = icono, cadaKm = intervalo.cadaKm, cadaDias = intervalo.cadaDias)
            )
        }
    }

    // Borra también su historial (ForeignKey CASCADE).
    fun eliminarTipo(tipo: TipoMantencion, onEliminado: () -> Unit) {
        viewModelScope.launch {
            motoDao.deleteTipo(tipo)
            onEliminado()
        }
    }

    companion object {
        fun factory(tipoId: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as VidaOSApplication
                DetalleMantencionViewModel(tipoId = tipoId, motoDao = application.database.motoDao())
            }
        }
    }
}
