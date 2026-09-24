package com.wuelmer.vidaos.ui.gym

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.wuelmer.vidaos.VidaOSApplication
import com.wuelmer.vidaos.data.DiaRutina
import com.wuelmer.vidaos.data.GymDao
import com.wuelmer.vidaos.data.SeedGym
import com.wuelmer.vidaos.data.SesionGym
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class GymUiState(
    val dias: List<DiaRutina> = emptyList(),
    val ultimasSesiones: List<SesionGym> = emptyList()
)

private const val LIMITE_ULTIMAS_SESIONES = 5

class GymViewModel(gymDao: GymDao) : ViewModel() {

    val uiState: StateFlow<GymUiState> = combine(
        gymDao.getDiasDeRutina(SeedGym.RUTINA_INICIAL_ID),
        gymDao.getUltimasSesiones(LIMITE_ULTIMAS_SESIONES)
    ) { dias, sesiones ->
        GymUiState(dias = dias, ultimasSesiones = sesiones)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = GymUiState()
    )

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as VidaOSApplication
                GymViewModel(gymDao = application.database.gymDao())
            }
        }
    }
}
