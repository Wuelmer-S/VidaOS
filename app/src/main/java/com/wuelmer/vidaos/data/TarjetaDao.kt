package com.wuelmer.vidaos.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface TarjetaDao {

    // La más nueva primero.
    @Query("SELECT * FROM estados_cuenta ORDER BY fechaEstado DESC")
    fun getEstados(): Flow<List<EstadoCuenta>>

    @Query("SELECT * FROM estados_cuenta WHERE id = :id")
    fun getEstado(id: Long): Flow<EstadoCuenta?>

    @Query("SELECT * FROM estados_cuenta WHERE fechaEstado = :fecha")
    suspend fun getEstadoPorFecha(fecha: LocalDate): EstadoCuenta?

    @Query("SELECT * FROM operaciones_tarjeta ORDER BY estadoId, orden")
    fun getOperaciones(): Flow<List<OperacionTarjeta>>

    @Query("SELECT * FROM operaciones_tarjeta WHERE estadoId = :estadoId ORDER BY orden")
    fun getOperacionesDe(estadoId: Long): Flow<List<OperacionTarjeta>>

    @Insert
    suspend fun insertEstado(estado: EstadoCuenta): Long

    @Insert
    suspend fun insertOperaciones(operaciones: List<OperacionTarjeta>)

    @Update
    suspend fun updateEstado(estado: EstadoCuenta)

    // Las operaciones se borran solas (CASCADE).
    @Delete
    suspend fun deleteEstado(estado: EstadoCuenta)

    // Si ya existía una factura con la misma fecha se reemplaza (volver a importar el mismo PDF corregido).
    @Transaction
    suspend fun guardarFactura(estado: EstadoCuenta, operaciones: List<OperacionTarjeta>): Long {
        getEstadoPorFecha(estado.fechaEstado)?.let { deleteEstado(it) }
        val id = insertEstado(estado)
        insertOperaciones(operaciones.map { it.copy(estadoId = id) })
        return id
    }
}
