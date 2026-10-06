package com.wuelmer.vidaos.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

enum class TipoOperacionTarjeta {
    PAGO,       // "MONTO CANCELADO": abonos a la tarjeta (montos negativos)
    COMPRA,     // compra en una cuota
    CUOTA,      // compra en cuotas: cuenta solo el valor de la cuota del mes
    CARGO       // comisiones, intereses, impuestos, traspaso de la deuda internacional
}

// Un estado de cuenta (factura) de la tarjeta de crédito, importado desde el PDF del banco.
// Montos en pesos (CLP). vencimientos: cuotas de los próximos 4 meses separadas por coma ("23334,0,0,0").
@Entity(tableName = "estados_cuenta", indices = [Index(value = ["fechaEstado"], unique = true)])
data class EstadoCuenta(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fechaEstado: LocalDate,
    val periodoDesde: LocalDate,
    val periodoHasta: LocalDate,
    val pagarHasta: LocalDate,
    val totalFacturado: Long,
    val montoMinimo: Long,
    val cupoTotal: Long,
    val cupoUtilizado: Long,
    val cupoDisponible: Long,
    val facturadoAnterior: Long,
    val proximoDesde: LocalDate?,
    val proximoHasta: LocalDate?,
    val vencimientos: String,
    // El usuario marcó "Ya la pagué" (para cuando no anotó el pago en la app).
    val pagadaManual: Boolean = false,
    val importadoEl: LocalDate
)

// Cada línea del detalle de la factura.
@Entity(
    tableName = "operaciones_tarjeta",
    foreignKeys = [
        ForeignKey(
            entity = EstadoCuenta::class,
            parentColumns = ["id"],
            childColumns = ["estadoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("estadoId")]
)
data class OperacionTarjeta(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val estadoId: Long,
    val fecha: LocalDate,
    val descripcion: String,
    val tipo: TipoOperacionTarjeta,
    val montoOperacion: Long,
    val cuotaActual: Int,
    val cuotasTotal: Int,
    // Lo que esta línea suma a la factura (en una compra en cuotas, solo la cuota del mes).
    val valorCuota: Long,
    val orden: Int
)
