package com.wuelmer.vidaos.ui.registrargasto

import com.wuelmer.vidaos.data.Categoria
import com.wuelmer.vidaos.data.OrigenPago
import com.wuelmer.vidaos.data.TipoCategoria
import com.wuelmer.vidaos.data.TipoMovimiento
import java.time.LocalDate

data class RegistrarGastoUiState(
    val monto: String = "",
    val descripcion: String = "",
    val fecha: LocalDate = LocalDate.now(),
    val origen: OrigenPago = OrigenPago.DEBITO,
    val tipo: TipoMovimiento = TipoMovimiento.GASTO,
    val categoriaId: Long? = null,
    val categorias: List<Categoria> = emptyList(),
    val errorMonto: Boolean = false,
    val guardadoExitoso: Boolean = false
)

fun TipoMovimiento.categoriaCorrespondiente(): TipoCategoria? = when (this) {
    TipoMovimiento.GASTO -> TipoCategoria.GASTO
    TipoMovimiento.INGRESO -> TipoCategoria.INGRESO
    TipoMovimiento.PAGO_TARJETA, TipoMovimiento.INTERNO -> null
}
