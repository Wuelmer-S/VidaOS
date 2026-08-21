package com.wuelmer.vidaos.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

enum class OrigenPago {
    DEBITO,
    CREDITO
}

enum class TipoMovimiento {
    GASTO,
    INGRESO,
    PAGO_TARJETA,
    INTERNO
}

enum class TipoPrestamo {
    PRESTAMO,
    DEVOLUCION
}

@Entity(
    tableName = "movimientos",
    foreignKeys = [
        ForeignKey(
            entity = Categoria::class,
            parentColumns = ["id"],
            childColumns = ["categoriaId"]
        )
    ],
    indices = [Index("categoriaId")]
)
data class Movimiento(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fecha: LocalDate,
    val monto: Long,
    val descripcion: String,
    val categoriaId: Long,
    val origen: OrigenPago,
    val tipo: TipoMovimiento,
    val esPrestamo: TipoPrestamo? = null,
    val observaciones: String? = null
)
