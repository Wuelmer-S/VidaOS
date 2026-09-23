package com.wuelmer.vidaos.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `ejercicios_gym` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `nombre` TEXT NOT NULL, `tipo` TEXT NOT NULL, `zona` TEXT NOT NULL, `unilateral` INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `rutinas` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `nombre` TEXT NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `dias_rutina` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `rutinaId` INTEGER NOT NULL, `numero` INTEGER NOT NULL, `nombre` TEXT NOT NULL, FOREIGN KEY(`rutinaId`) REFERENCES `rutinas`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_dias_rutina_rutinaId` ON `dias_rutina` (`rutinaId`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `rutina_ejercicios` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `diaRutinaId` INTEGER NOT NULL, `ejercicioId` INTEGER NOT NULL, `alternativaEjercicioId` INTEGER, `orden` INTEGER NOT NULL, `series` INTEGER NOT NULL, `objetivoMin` INTEGER NOT NULL, `objetivoMax` INTEGER NOT NULL, `descansoSegundos` INTEGER NOT NULL, FOREIGN KEY(`diaRutinaId`) REFERENCES `dias_rutina`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION , FOREIGN KEY(`ejercicioId`) REFERENCES `ejercicios_gym`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION , FOREIGN KEY(`alternativaEjercicioId`) REFERENCES `ejercicios_gym`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_rutina_ejercicios_diaRutinaId` ON `rutina_ejercicios` (`diaRutinaId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_rutina_ejercicios_ejercicioId` ON `rutina_ejercicios` (`ejercicioId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_rutina_ejercicios_alternativaEjercicioId` ON `rutina_ejercicios` (`alternativaEjercicioId`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `sesiones_gym` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `fecha` INTEGER NOT NULL, `diaRutinaId` INTEGER NOT NULL, FOREIGN KEY(`diaRutinaId`) REFERENCES `dias_rutina`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_sesiones_gym_diaRutinaId` ON `sesiones_gym` (`diaRutinaId`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `series_gym` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sesionId` INTEGER NOT NULL, `ejercicioId` INTEGER NOT NULL, `orden` INTEGER NOT NULL, `repeticiones` INTEGER, `segundos` INTEGER, `pesoKg` REAL, FOREIGN KEY(`sesionId`) REFERENCES `sesiones_gym`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`ejercicioId`) REFERENCES `ejercicios_gym`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_series_gym_sesionId` ON `series_gym` (`sesionId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_series_gym_ejercicioId` ON `series_gym` (`ejercicioId`)")

        SeedGym.insertar(db)
    }
}
