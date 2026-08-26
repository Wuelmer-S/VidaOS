package com.wuelmer.vidaos.data

import androidx.room.Dao
import androidx.room.Delete
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

data class GastoPorCategoria(
    val categoriaId: Long,
    val categoriaNombre: String,
    val categoriaColor: String?,
    val total: Long
)

data class GastoPorOrigen(
    val origen: OrigenPago,
    val total: Long
)

@Dao
interface MovimientoDao {

    @Insert
    suspend fun insert(movimiento: Movimiento): Long

    @Delete
    suspend fun delete(movimiento: Movimiento)

    @Query("SELECT * FROM movimientos ORDER BY fecha DESC, id DESC")
    fun getAll(): Flow<List<Movimiento>>

    @Query(
        "SELECT m.*, c.nombre AS categoriaNombre, c.color AS categoriaColor " +
            "FROM movimientos m INNER JOIN categorias c ON m.categoriaId = c.id " +
            "ORDER BY m.fecha DESC, m.id DESC"
    )
    fun getAllConCategoria(): Flow<List<MovimientoConCategoria>>

    @Query(
        "SELECT m.*, c.nombre AS categoriaNombre, c.color AS categoriaColor " +
            "FROM movimientos m INNER JOIN categorias c ON m.categoriaId = c.id " +
            "WHERE m.id = :id"
    )
    fun getConCategoriaPorId(id: Long): Flow<MovimientoConCategoria?>

    @Query(
        "SELECT COALESCE(SUM(monto), 0) FROM movimientos " +
            "WHERE tipo = 'GASTO' AND fecha BETWEEN :desde AND :hasta"
    )
    fun getTotalGastadoEntreFechas(desde: LocalDate, hasta: LocalDate): Flow<Long>

    @Query(
        "SELECT c.id AS categoriaId, c.nombre AS categoriaNombre, c.color AS categoriaColor, " +
            "SUM(m.monto) AS total " +
            "FROM movimientos m INNER JOIN categorias c ON m.categoriaId = c.id " +
            "WHERE m.tipo = 'GASTO' AND m.fecha BETWEEN :desde AND :hasta " +
            "GROUP BY c.id ORDER BY total DESC"
    )
    fun getGastosPorCategoriaEntreFechas(desde: LocalDate, hasta: LocalDate): Flow<List<GastoPorCategoria>>

    @Query(
        "SELECT origen, SUM(monto) AS total FROM movimientos " +
            "WHERE tipo = 'GASTO' AND fecha BETWEEN :desde AND :hasta " +
            "GROUP BY origen ORDER BY total DESC"
    )
    fun getGastosPorOrigenEntreFechas(desde: LocalDate, hasta: LocalDate): Flow<List<GastoPorOrigen>>

    @Query("SELECT COUNT(*) FROM movimientos WHERE categoriaId = :categoriaId")
    suspend fun contarPorCategoria(categoriaId: Long): Int
}
