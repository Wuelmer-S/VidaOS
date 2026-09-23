package com.wuelmer.vidaos.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Database(
    entities = [
        Movimiento::class,
        Categoria::class,
        EjercicioGym::class,
        Rutina::class,
        DiaRutina::class,
        RutinaEjercicio::class,
        SesionGym::class,
        SerieGym::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class VidaOSDatabase : RoomDatabase() {

    abstract fun movimientoDao(): MovimientoDao
    abstract fun categoriaDao(): CategoriaDao
    abstract fun gymDao(): GymDao

    companion object {
        @Volatile
        private var INSTANCE: VidaOSDatabase? = null

        fun getInstance(context: Context, scope: CoroutineScope): VidaOSDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VidaOSDatabase::class.java,
                    "vidaos.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .addCallback(SeedCategoriasCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class SeedCategoriasCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                SeedGym.insertar(db)
                INSTANCE?.let { database ->
                    scope.launch {
                        database.categoriaDao().insertAll(CategoriasIniciales.lista)
                    }
                }
            }
        }
    }
}
