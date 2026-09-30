package com.wuelmer.vidaos.data

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

data class EjercicioDelDia(
    @Embedded val objetivo: RutinaEjercicio,
    @Relation(parentColumn = "ejercicioId", entityColumn = "id")
    val ejercicio: EjercicioGym,
    @Relation(parentColumn = "alternativaEjercicioId", entityColumn = "id")
    val alternativa: EjercicioGym?
)

// Mejor marca de un ejercicio en una sesión (máximos de sus series).
data class PuntoEjercicio(
    val sesionId: Long,
    val fecha: LocalDate,
    val pesoMaxKg: Double?,
    val repsMax: Int?,
    val segundosMax: Int?
)

data class SerieConEjercicio(
    @Embedded val serie: SerieGym,
    @Relation(parentColumn = "ejercicioId", entityColumn = "id")
    val ejercicio: EjercicioGym
)

@Dao
interface GymDao {

    @Query("SELECT * FROM dias_rutina WHERE rutinaId = :rutinaId ORDER BY numero")
    fun getDiasDeRutina(rutinaId: Long): Flow<List<DiaRutina>>

    @Query("SELECT * FROM dias_rutina WHERE id = :diaRutinaId")
    suspend fun getDia(diaRutinaId: Long): DiaRutina?

    @Query("SELECT * FROM sesiones_gym ORDER BY fecha DESC, id DESC LIMIT :limite")
    fun getUltimasSesiones(limite: Int): Flow<List<SesionGym>>

    @Query("SELECT * FROM sesiones_gym WHERE id = :sesionId")
    fun getSesion(sesionId: Long): Flow<SesionGym?>

    @Query("SELECT * FROM sesiones_gym WHERE id = :sesionId")
    suspend fun getSesionPorId(sesionId: Long): SesionGym?

    @Update
    suspend fun updateSesion(sesion: SesionGym)

    @Query("DELETE FROM series_gym WHERE sesionId = :sesionId")
    suspend fun deleteSeriesDeSesion(sesionId: Long)

    @Query(
        "SELECT d.* FROM dias_rutina d " +
            "JOIN sesiones_gym s ON s.diaRutinaId = d.id WHERE s.id = :sesionId"
    )
    fun getDiaDeSesion(sesionId: Long): Flow<DiaRutina?>

    @Transaction
    @Query("SELECT * FROM series_gym WHERE sesionId = :sesionId ORDER BY orden")
    fun getSeriesConEjercicio(sesionId: Long): Flow<List<SerieConEjercicio>>

    @Transaction
    @Query("SELECT * FROM rutina_ejercicios WHERE diaRutinaId = :diaRutinaId ORDER BY orden")
    suspend fun getEjerciciosDelDia(diaRutinaId: Long): List<EjercicioDelDia>

    // Series del ejercicio en la sesión más reciente que lo incluye.
    @Query(
        "SELECT * FROM series_gym WHERE ejercicioId = :ejercicioId AND sesionId = (" +
            "SELECT s.id FROM sesiones_gym s JOIN series_gym sg ON sg.sesionId = s.id " +
            "WHERE sg.ejercicioId = :ejercicioId ORDER BY s.fecha DESC, s.id DESC LIMIT 1" +
            ") ORDER BY orden"
    )
    suspend fun getSeriesUltimaVez(ejercicioId: Long): List<SerieGym>

    @Insert
    suspend fun insertSesion(sesion: SesionGym): Long

    @Insert
    suspend fun insertSeries(series: List<SerieGym>)

    @Query("SELECT * FROM sesiones_gym ORDER BY fecha DESC, id DESC")
    fun getSesiones(): Flow<List<SesionGym>>

    @Query("SELECT * FROM ejercicios_gym WHERE id = :ejercicioId")
    fun getEjercicio(ejercicioId: Long): Flow<EjercicioGym?>

    @Query(
        "SELECT s.id AS sesionId, s.fecha AS fecha, MAX(sg.pesoKg) AS pesoMaxKg, " +
            "MAX(sg.repeticiones) AS repsMax, MAX(sg.segundos) AS segundosMax " +
            "FROM series_gym sg JOIN sesiones_gym s ON s.id = sg.sesionId " +
            "WHERE sg.ejercicioId = :ejercicioId GROUP BY s.id ORDER BY s.fecha, s.id"
    )
    fun getEvolucionEjercicio(ejercicioId: Long): Flow<List<PuntoEjercicio>>

    // Ejercicios con al menos una serie registrada, para elegir cuál ver en Progreso.
    @Query(
        "SELECT * FROM ejercicios_gym WHERE id IN (SELECT DISTINCT ejercicioId FROM series_gym) ORDER BY nombre"
    )
    fun getEjerciciosConRegistros(): Flow<List<EjercicioGym>>

    // Peso total levantado (kg × reps de cada serie con peso). En unilaterales cuenta un lado.
    @Query("SELECT COALESCE(SUM(pesoKg * repeticiones), 0) FROM series_gym WHERE pesoKg IS NOT NULL AND repeticiones IS NOT NULL")
    fun getVolumenTotalKg(): Flow<Double>

    @Query("SELECT * FROM series_gym WHERE sesionId = :sesionId ORDER BY orden")
    suspend fun getSeriesDeSesion(sesionId: Long): List<SerieGym>

    @Query("DELETE FROM sesiones_gym WHERE id = :sesionId")
    suspend fun deleteSesion(sesionId: Long)

    @Query("SELECT * FROM borrador_series WHERE diaRutinaId = :diaRutinaId ORDER BY rutinaEjercicioId, orden")
    suspend fun getBorrador(diaRutinaId: Long): List<BorradorSerie>

    @Query("SELECT DISTINCT diaRutinaId FROM borrador_series")
    fun getDiasConBorrador(): Flow<List<Long>>

    @Insert
    suspend fun insertBorrador(series: List<BorradorSerie>)

    @Query("DELETE FROM borrador_series WHERE diaRutinaId = :diaRutinaId")
    suspend fun deleteBorrador(diaRutinaId: Long)
}
