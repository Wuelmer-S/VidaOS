package com.wuelmer.vidaos.ui.movimientos

import com.wuelmer.vidaos.data.GastoPorCategoria
import com.wuelmer.vidaos.data.GastoPorOrigen
import com.wuelmer.vidaos.data.MovimientoConCategoria
import com.wuelmer.vidaos.ui.tarjeta.CuotaPeriodo

data class MovimientosUiState(
    val movimientos: List<MovimientoConCategoria> = emptyList(),
    val totalGastadoMes: Long = 0,
    val gastosPorCategoria: List<GastoPorCategoria> = emptyList(),
    val gastosPorOrigen: List<GastoPorOrigen> = emptyList(),
    // Cuotas que entran en la próxima factura de la tarjeta (de facturas importadas y compras anotadas).
    val cuotasProximaFactura: List<CuotaPeriodo> = emptyList()
)
