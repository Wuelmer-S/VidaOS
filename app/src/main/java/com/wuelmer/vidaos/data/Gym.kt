package com.wuelmer.vidaos.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

enum class TipoEjercicio {
    CON_PESO,
    PESO_CORPORAL,
    TIEMPO
}

enum class ZonaEjercicio {
    INFERIOR,
    SUPERIOR,
    CORE
}

@Entity(tableName = "ejercicios_gym")
data class EjercicioGym(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nombre: String,
    val tipo: TipoEjercicio,
    val zona: ZonaEjercicio,
    val unilateral: Boolean = false
)

@Entity(tableName = "rutinas")
data class Rutina(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nombre: String
)

@Entity(
    tableName = "dias_rutina",
    foreignKeys = [
        ForeignKey(
            entity = Rutina::class,
            parentColumns = ["id"],
            childColumns = ["rutinaId"]
        )
    ],
    indices = [Index("rutinaId")]
)
data class DiaRutina(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val rutinaId: Long,
    val numero: Int,
    val nombre: String
)

// Para ejercicios de tipo TIEMPO, objetivoMin/objetivoMax son segundos; en el resto, repeticiones.
@Entity(
    tableName = "rutina_ejercicios",
    foreignKeys = [
        ForeignKey(
            entity = DiaRutina::class,
            parentColumns = ["id"],
            childColumns = ["diaRutinaId"]
        ),
        ForeignKey(
            entity = EjercicioGym::class,
            parentColumns = ["id"],
            childColumns = ["ejercicioId"]
        ),
        ForeignKey(
            entity = EjercicioGym::class,
            parentColumns = ["id"],
            childColumns = ["alternativaEjercicioId"]
        )
    ],
    indices = [Index("diaRutinaId"), Index("ejercicioId"), Index("alternativaEjercicioId")]
)
data class RutinaEjercicio(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val diaRutinaId: Long,
    val ejercicioId: Long,
    val alternativaEjercicioId: Long? = null,
    val orden: Int,
    val series: Int,
    val objetivoMin: Int,
    val objetivoMax: Int,
    val descansoSegundos: Int
)

@Entity(
    tableName = "sesiones_gym",
    foreignKeys = [
        ForeignKey(
            entity = DiaRutina::class,
            parentColumns = ["id"],
            childColumns = ["diaRutinaId"]
        )
    ],
    indices = [Index("diaRutinaId")]
)
data class SesionGym(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fecha: LocalDate,
    val diaRutinaId: Long
)

// pesoKg siempre en kg; la conversión a lb ocurre solo en la UI.
@Entity(
    tableName = "series_gym",
    foreignKeys = [
        ForeignKey(
            entity = SesionGym::class,
            parentColumns = ["id"],
            childColumns = ["sesionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = EjercicioGym::class,
            parentColumns = ["id"],
            childColumns = ["ejercicioId"]
        )
    ],
    indices = [Index("sesionId"), Index("ejercicioId")]
)
data class SerieGym(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sesionId: Long,
    val ejercicioId: Long,
    val orden: Int,
    val repeticiones: Int? = null,
    val segundos: Int? = null,
    val pesoKg: Double? = null
)
