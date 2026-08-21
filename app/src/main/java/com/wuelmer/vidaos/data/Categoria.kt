package com.wuelmer.vidaos.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TipoCategoria {
    GASTO,
    INGRESO
}

@Entity(tableName = "categorias")
data class Categoria(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nombre: String,
    val tipo: TipoCategoria,
    val color: String? = null
)
