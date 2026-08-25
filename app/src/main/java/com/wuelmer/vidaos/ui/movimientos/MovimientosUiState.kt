package com.wuelmer.vidaos.ui.movimientos

import com.wuelmer.vidaos.data.MovimientoConCategoria

data class MovimientosUiState(
    val movimientos: List<MovimientoConCategoria> = emptyList(),
    val totalGastadoMes: Long = 0
)
