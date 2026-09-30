package com.wuelmer.vidaos.ui.gym

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wuelmer.vidaos.data.EjercicioGym
import com.wuelmer.vidaos.data.GymDao
import com.wuelmer.vidaos.data.PreferenciasRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class EvolucionEjercicioUiState(
    val ejercicio: EjercicioGym?,
    val evolucion: EvolucionEjercicio?
)

class EvolucionEjercicioViewModel(
    ejercicioId: Long,
    gymDao: GymDao,
    preferencias: PreferenciasRepository
) : ViewModel() {

    // null mientras carga.
    val uiState: StateFlow<EvolucionEjercicioUiState?> = combine(
        gymDao.getEjercicio(ejercicioId),
        gymDao.getEvolucionEjercicio(ejercicioId),
        preferencias.preferencias
    ) { ejercicio, registros, prefs ->
        EvolucionEjercicioUiState(
            ejercicio = ejercicio,
            evolucion = ejercicio?.let { evolucionEjercicio(it, registros, prefs.unidadPeso) }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )
}
