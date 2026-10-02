package com.wuelmer.vidaos.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
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

    @Query("SELECT * FROM tipos_mantencion WHERE id = :tipoId")
    fun getTipo(tipoId: Long): Flow<TipoMantencion?>

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

    @Query("SELECT * FROM trabajos ORDER BY id")
    fun getTrabajos(): Flow<List<Trabajo>>

    @Query("SELECT * FROM trabajos WHERE id = :trabajoId")
    fun getTrabajo(trabajoId: Long): Flow<Trabajo?>

    @Insert
    suspend fun insertTrabajo(trabajo: Trabajo): Long

    @Update
    suspend fun updateTrabajo(trabajo: Trabajo)

    @Delete
    suspend fun deleteTrabajoSolo(trabajo: Trabajo)

    // trabajoId en tipos_mantencion no es ForeignKey (se agregó antes que la tabla): se suelta a mano.
    @Query("UPDATE tipos_mantencion SET trabajoId = NULL WHERE trabajoId = :trabajoId")
    suspend fun desvincularTrabajo(trabajoId: Long)

    // Los ajustes se borran solos (CASCADE).
    @Transaction
    suspend fun deleteTrabajo(trabajo: Trabajo) {
        desvincularTrabajo(trabajo.id)
        deleteTrabajoSolo(trabajo)
    }

    @Query("SELECT * FROM ajustes_trabajo ORDER BY trabajoId, orden, id")
    fun getAjustes(): Flow<List<AjusteTrabajo>>

    @Query("SELECT * FROM ajustes_trabajo WHERE trabajoId = :trabajoId ORDER BY orden, id")
    fun getAjustesDeTrabajo(trabajoId: Long): Flow<List<AjusteTrabajo>>

    @Insert
    suspend fun insertAjuste(ajuste: AjusteTrabajo): Long

    @Update
    suspend fun updateAjuste(ajuste: AjusteTrabajo)

    @Delete
    suspend fun deleteAjuste(ajuste: AjusteTrabajo)
}
