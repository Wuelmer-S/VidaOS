package com.wuelmer.vidaos.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoriaDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(categorias: List<Categoria>)

    @Query("SELECT * FROM categorias ORDER BY nombre ASC")
    fun getAll(): Flow<List<Categoria>>
}
