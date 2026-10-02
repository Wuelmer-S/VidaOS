package com.wuelmer.vidaos.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

enum class HechoPor {
    YO,
    TALLER
}

// Cada vez que el usuario anota lo que marca el odómetro. La última es el km actual.
@Entity(tableName = "lecturas_km")
data class LecturaKm(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fecha: LocalDate,
    val km: Int
)

// Una mantención del plan. Vence por km, por tiempo o por lo que ocurra primero (null = no vence por ese lado).
@Entity(tableName = "tipos_mantencion")
data class TipoMantencion(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nombre: String,
    val icono: String,
    val cadaKm: Int?,
    val cadaDias: Int?,
    val avisarAlPct: Int = 90,
    val trabajoId: Long? = null,
    val activo: Boolean = true
)

// Una vez que se hizo una mantención. El último registro de cada tipo define cuándo vuelve a tocar.
// costo (CLP) y movimientoId quedan listos para enlazar con Finanzas más adelante.
@Entity(
    tableName = "registros_mantencion",
    foreignKeys = [
        ForeignKey(
            entity = TipoMantencion::class,
            parentColumns = ["id"],
            childColumns = ["tipoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tipoId")]
)
data class RegistroMantencion(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tipoId: Long,
    val fecha: LocalDate,
    val km: Int,
    val costo: Long? = null,
    val hechoPor: HechoPor = HechoPor.YO,
    val notas: String? = null,
    val movimientoId: Long? = null
)
