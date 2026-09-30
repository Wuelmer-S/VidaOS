package com.wuelmer.vidaos.ui.gym

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wuelmer.vidaos.VidaOSApplication
import com.wuelmer.vidaos.data.GymDao
import com.wuelmer.vidaos.data.PreferenciasRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

class ProgresoGymViewModel(gymDao: GymDao, preferencias: PreferenciasRepository) : ViewModel() {

    // null mientras carga, para no mostrar una racha 0 falsa un instante.
    val progreso: StateFlow<ProgresoSemana?> = combine(
        gymDao.fechas(),
        preferencias.preferencias
    ) { fechas, prefs ->
        progresoSemana(fechas, LocalDate.now(), prefs.metaSemanal)
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
