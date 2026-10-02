package com.wuelmer.vidaos.ui.gym

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wuelmer.vidaos.VidaOSApplication
import com.wuelmer.vidaos.data.EjercicioGym
import com.wuelmer.vidaos.data.GymDao
import com.wuelmer.vidaos.data.PreferenciasRepository
import com.wuelmer.vidaos.data.UnidadPeso
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class ProgresoGymUiState(
    val semana: ProgresoSemana,
    val calendario: List<SemanaCalendario>,
    val resumen: ResumenGym,
    val unidad: UnidadPeso,
    val ejercicios: List<EjercicioGym>
)

class ProgresoGymViewModel(gymDao: GymDao, preferencias: PreferenciasRepository) : ViewModel() {

    // null mientras carga, para no mostrar una racha 0 falsa un instante.
    val uiState: StateFlow<ProgresoGymUiState?> = combine(
        gymDao.getSesiones(),
        gymDao.getVolumenTotalKg(),
        gymDao.getEjerciciosConRegistros(),
        preferencias.preferencias
    ) { sesiones, volumenKg, ejercicios, prefs ->
        val hoy = LocalDate.now()
        val fechas = sesiones.map { it.fecha }
        ProgresoGymUiState(
            semana = progresoSemana(fechas, hoy, prefs.metaSemanal),
            calendario = calendarioConstancia(sesiones.map { it.id to it.fecha }, hoy),
            resumen = resumenGym(fechas, volumenKg, hoy, prefs.metaSemanal),
            unidad = prefs.unidadPeso,
            ejercicios = ejercicios
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as VidaOSApplication
                ProgresoGymViewModel(gymDao = application.database.gymDao(), preferencias = application.preferencias)
            }
        }
    }
}
