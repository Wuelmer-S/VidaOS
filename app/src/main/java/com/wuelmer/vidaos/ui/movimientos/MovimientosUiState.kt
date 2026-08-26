package com.wuelmer.vidaos.ui.movimientos

import com.wuelmer.vidaos.data.GastoPorCategoria
import com.wuelmer.vidaos.data.GastoPorOrigen
import com.wuelmer.vidaos.data.MovimientoConCategoria

data class MovimientosUiState(
    val movimientos: List<MovimientoConCategoria> = emptyList(),
    val totalGastadoMes: Long = 0,
    val gastosPorCategoria: List<GastoPorCategoria> = emptyList(),
    val gastosPorOrigen: List<GastoPorOrigen> = emptyList()
)
