package com.wuelmer.vidaos.ui.gym

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wuelmer.vidaos.data.EjercicioGym
import com.wuelmer.vidaos.data.GymDao
import com.wuelmer.vidaos.data.PreferenciasRepository
import com.wuelmer.vidaos.data.SerieGym
import com.wuelmer.vidaos.data.SesionGym
import com.wuelmer.vidaos.data.UnidadPeso
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GrupoEjercicioDetalle(
    val ejercicio: EjercicioGym,
    val series: List<SerieGym>
)

data class DetalleSesionUiState(
    val cargando: Boolean = true,
    val sesion: SesionGym? = null,
    val nombreDia: String = "",
    val grupos: List<GrupoEjercicioDetalle> = emptyList(),
    val unidad: UnidadPeso = UnidadPeso.KG
)

class DetalleSesionGymViewModel(
    private val sesionId: Long,
    private val gymDao: GymDao,
    preferencias: PreferenciasRepository
) : ViewModel() {

    val uiState: StateFlow<DetalleSesionUiState> = combine(
        gymDao.getSesion(sesionId),
        gymDao.getDiaDeSesion(sesionId),
        gymDao.getSeriesConEjercicio(sesionId),
        preferencias.preferencias
    ) { sesion, dia, series, prefs ->
        DetalleSesionUiState(
            cargando = false,
            sesion = sesion,
            nombreDia = dia?.nombre.orEmpty(),
            grupos = series
                .groupBy { it.ejercicio.id }
                .values
                .map { grupo -> GrupoEjercicioDetalle(grupo.first().ejercicio, grupo.map { it.serie }) },
            unidad = prefs.unidadPeso
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DetalleSesionUiState()
    )

    fun eliminar(onEliminada: () -> Unit) {
        viewModelScope.launch {
            gymDao.deleteSesion(sesionId)
            onEliminada()
        }
    }
}
