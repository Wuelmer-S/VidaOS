package com.wuelmer.vidaos.data

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

data class MovimientoConCategoria(
    @Embedded val movimiento: Movimiento,
    val categoriaNombre: String,
    val categoriaColor: String?
)

@Dao
interface MovimientoDao {

    @Insert
    suspend fun insert(movimiento: Movimiento): Long

    @Query("SELECT * FROM movimientos ORDER BY fecha DESC, id DESC")
    fun getAll(): Flow<List<Movimiento>>

    @Query(
        "SELECT m.*, c.nombre AS categoriaNombre, c.color AS categoriaColor " +
            "FROM movimientos m INNER JOIN categorias c ON m.categoriaId = c.id " +
            "ORDER BY m.fecha DESC, m.id DESC"
    )
    fun getAllConCategoria(): Flow<List<MovimientoConCategoria>>

    @Query(
        "SELECT COALESCE(SUM(monto), 0) FROM movimientos " +
            "WHERE tipo = 'GASTO' AND fecha BETWEEN :desde AND :hasta"
    )
    fun getTotalGastadoEntreFechas(desde: LocalDate, hasta: LocalDate): Flow<Long>
}
