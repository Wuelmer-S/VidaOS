package com.wuelmer.vidaos.ui.registrargasto

import com.wuelmer.vidaos.data.Categoria
import com.wuelmer.vidaos.data.Movimiento
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
    val guardadoExitoso: Boolean = false,
    // true = el formulario está editando un movimiento que ya existe.
    val editando: Boolean = false
)

// Rellena el formulario con un movimiento guardado, para editarlo.
fun RegistrarGastoUiState.conMovimiento(movimiento: Movimiento): RegistrarGastoUiState = copy(
    monto = movimiento.monto.toString(),
    descripcion = movimiento.descripcion,
    fecha = movimiento.fecha,
    origen = movimiento.origen,
    tipo = movimiento.tipo,
    categoriaId = movimiento.categoriaId,
    errorMonto = false,
    editando = true
)

// Aplica lo del formulario sobre el original: mantiene el id y lo que el formulario no muestra (préstamo, observaciones).
fun Movimiento.editadoCon(estado: RegistrarGastoUiState, monto: Long, categoriaId: Long): Movimiento = copy(
    fecha = estado.fecha,
    monto = monto,
    descripcion = estado.descripcion,
    categoriaId = categoriaId,
    origen = estado.origen,
    tipo = estado.tipo
)

fun TipoMovimiento.categoriaCorrespondiente(): TipoCategoria? = when (this) {
    TipoMovimiento.GASTO -> TipoCategoria.GASTO
    TipoMovimiento.INGRESO -> TipoCategoria.INGRESO
    TipoMovimiento.PAGO_TARJETA, TipoMovimiento.INTERNO -> null
}
