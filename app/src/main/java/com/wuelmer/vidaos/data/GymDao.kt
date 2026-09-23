package com.wuelmer.vidaos.data

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

data class EjercicioDelDia(
    @Embedded val objetivo: RutinaEjercicio,
    @Relation(parentColumn = "ejercicioId", entityColumn = "id")
    val ejercicio: EjercicioGym,
    @Relation(parentColumn = "alternativaEjercicioId", entityColumn = "id")
    val alternativa: EjercicioGym?
)

@Dao
interface GymDao {

    @Query("SELECT * FROM dias_rutina WHERE rutinaId = :rutinaId ORDER BY numero")
    fun getDiasDeRutina(rutinaId: Long): Flow<List<DiaRutina>>

    @Transaction
    @Query("SELECT * FROM rutina_ejercicios WHERE diaRutinaId = :diaRutinaId ORDER BY orden")
    suspend fun getEjerciciosDelDia(diaRutinaId: Long): List<EjercicioDelDia>

    @Insert
    suspend fun insertSesion(sesion: SesionGym): Long

    @Insert
    suspend fun insertSeries(series: List<SerieGym>)

    @Query("SELECT * FROM sesiones_gym ORDER BY fecha DESC, id DESC")
    fun getSesiones(): Flow<List<SesionGym>>

    @Query("SELECT * FROM series_gym WHERE sesionId = :sesionId ORDER BY orden")
    suspend fun getSeriesDeSesion(sesionId: Long): List<SerieGym>

    @Query("DELETE FROM sesiones_gym WHERE id = :sesionId")
    suspend fun deleteSesion(sesionId: Long)
}
