package com.wuelmer.vidaos.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MotoDao {

    // Lecturas de km: la más nueva primero (a igual fecha, la última anotada).
    @Query("SELECT * FROM lecturas_km ORDER BY fecha DESC, id DESC")
    fun getLecturas(): Flow<List<LecturaKm>>

    @Insert
    suspend fun insertLectura(lectura: LecturaKm): Long

    @Update
    suspend fun updateLectura(lectura: LecturaKm)

    @Delete
    suspend fun deleteLectura(lectura: LecturaKm)

    @Query("SELECT * FROM tipos_mantencion WHERE activo = 1 ORDER BY id")
    fun getTiposActivos(): Flow<List<TipoMantencion>>

    @Insert
    suspend fun insertTipo(tipo: TipoMantencion): Long

    @Update
    suspend fun updateTipo(tipo: TipoMantencion)

    @Delete
    suspend fun deleteTipo(tipo: TipoMantencion)

    // Último registro de cada tipo (el de fecha más nueva; a igual fecha, el de más km).
    @Query(
        """
        SELECT r.* FROM registros_mantencion r
        WHERE r.id = (
            SELECT r2.id FROM registros_mantencion r2
            WHERE r2.tipoId = r.tipoId
            ORDER BY r2.fecha DESC, r2.km DESC, r2.id DESC
            LIMIT 1
        )
        """
    )
    fun getUltimosRegistros(): Flow<List<RegistroMantencion>>

    @Query("SELECT * FROM registros_mantencion WHERE tipoId = :tipoId ORDER BY fecha DESC, km DESC, id DESC")
    fun getRegistrosDeTipo(tipoId: Long): Flow<List<RegistroMantencion>>

    @Insert
    suspend fun insertRegistro(registro: RegistroMantencion): Long

    @Update
    suspend fun updateRegistro(registro: RegistroMantencion)

    @Delete
    suspend fun deleteRegistro(registro: RegistroMantencion)
}
