package com.wuelmer.vidaos.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

@Dao
interface CategoriaDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(categorias: List<Categoria>)

    @Insert
    suspend fun insert(categoria: Categoria): Long

    @Delete
    suspend fun delete(categoria: Categoria)

    @Query("SELECT * FROM categorias ORDER BY nombre ASC")
    fun getAll(): Flow<List<Categoria>>

    suspend fun agregar(nombre: String, tipo: TipoCategoria): Categoria {
        val coloresEnUso = getAll().first().mapNotNull { it.color }
        val nueva = Categoria(
            nombre = nombre,
            tipo = tipo,
            color = AsignadorColorCategoria.siguienteColor(coloresEnUso)
        )
        val id = insert(nueva)
        return nueva.copy(id = id)
    }
}
