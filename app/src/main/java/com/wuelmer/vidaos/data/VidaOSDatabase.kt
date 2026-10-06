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
        SerieGym::class,
        BorradorSerie::class,
        LecturaKm::class,
        TipoMantencion::class,
        RegistroMantencion::class,
        Trabajo::class,
        AjusteTrabajo::class,
        EstadoCuenta::class,
        OperacionTarjeta::class
    ],
    version = 7,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class VidaOSDatabase : RoomDatabase() {

    abstract fun movimientoDao(): MovimientoDao
    abstract fun categoriaDao(): CategoriaDao
    abstract fun gymDao(): GymDao
    abstract fun motoDao(): MotoDao
    abstract fun tarjetaDao(): TarjetaDao

    companion object {
        const val NOMBRE_ARCHIVO = "vidaos.db"

        @Volatile
        private var INSTANCE: VidaOSDatabase? = null

        fun getInstance(context: Context, scope: CoroutineScope): VidaOSDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VidaOSDatabase::class.java,
                    NOMBRE_ARCHIVO
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
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
                SeedMoto.insertar(db)
                SeedFichas.insertar(db)
                INSTANCE?.let { database ->
                    scope.launch {
                        database.categoriaDao().insertAll(CategoriasIniciales.lista)
                    }
                }
            }
        }
    }
}
