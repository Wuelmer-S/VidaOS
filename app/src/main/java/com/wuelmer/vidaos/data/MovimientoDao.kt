package com.wuelmer.vidaos.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface MovimientoDao {

    @Insert
    suspend fun insert(movimiento: Movimiento): Long

    @Query("SELECT * FROM movimientos ORDER BY fecha DESC, id DESC")
    fun getAll(): Flow<List<Movimiento>>

    @Query(
        "SELECT COALESCE(SUM(monto), 0) FROM movimientos " +
            "WHERE tipo = 'GASTO' AND fecha BETWEEN :desde AND :hasta"
    )
    fun getTotalGastadoEntreFechas(desde: LocalDate, hasta: LocalDate): Flow<Long>
}
