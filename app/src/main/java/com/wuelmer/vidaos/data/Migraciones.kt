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

// v3: borrador de la sesión de gym en curso. No toca tablas existentes.
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `borrador_series` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `diaRutinaId` INTEGER NOT NULL, `rutinaEjercicioId` INTEGER NOT NULL, `ejercicioId` INTEGER NOT NULL, `orden` INTEGER NOT NULL, `reps` TEXT NOT NULL, `peso` TEXT NOT NULL, `segundos` TEXT NOT NULL)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_borrador_series_diaRutinaId` ON `borrador_series` (`diaRutinaId`)")
    }
}

// v4: módulo Moto (lecturas de km, plan de mantención y registros). No toca tablas existentes.
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `lecturas_km` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `fecha` INTEGER NOT NULL, `km` INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `tipos_mantencion` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `nombre` TEXT NOT NULL, `icono` TEXT NOT NULL, `cadaKm` INTEGER, `cadaDias` INTEGER, `avisarAlPct` INTEGER NOT NULL, `trabajoId` INTEGER, `activo` INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `registros_mantencion` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `tipoId` INTEGER NOT NULL, `fecha` INTEGER NOT NULL, `km` INTEGER NOT NULL, `costo` INTEGER, `hechoPor` TEXT NOT NULL, `notas` TEXT, `movimientoId` INTEGER, FOREIGN KEY(`tipoId`) REFERENCES `tipos_mantencion`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_registros_mantencion_tipoId` ON `registros_mantencion` (`tipoId`)")

        SeedMoto.insertar(db)
    }
}

// v5: fichas de trabajo de la moto (llaves, torques, materiales y pasos). No toca tablas existentes:
// solo enlaza por nombre los tipos de mantención del plan con su ficha.
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `trabajos` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `nombre` TEXT NOT NULL, `icono` TEXT NOT NULL, `pasos` TEXT NOT NULL, `materiales` TEXT NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `ajustes_trabajo` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `trabajoId` INTEGER NOT NULL, `parte` TEXT NOT NULL, `llave` TEXT, `torqueNm` INTEGER, `verificado` INTEGER NOT NULL, `orden` INTEGER NOT NULL, FOREIGN KEY(`trabajoId`) REFERENCES `trabajos`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_ajustes_trabajo_trabajoId` ON `ajustes_trabajo` (`trabajoId`)")

        SeedFichas.insertar(db)
    }
}

// v6: estados de cuenta de la tarjeta de crédito importados del PDF del banco, con su detalle. No toca tablas existentes.
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `estados_cuenta` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `fechaEstado` INTEGER NOT NULL, `periodoDesde` INTEGER NOT NULL, `periodoHasta` INTEGER NOT NULL, `pagarHasta` INTEGER NOT NULL, `totalFacturado` INTEGER NOT NULL, `montoMinimo` INTEGER NOT NULL, `cupoTotal` INTEGER NOT NULL, `cupoUtilizado` INTEGER NOT NULL, `cupoDisponible` INTEGER NOT NULL, `facturadoAnterior` INTEGER NOT NULL, `proximoDesde` INTEGER, `proximoHasta` INTEGER, `vencimientos` TEXT NOT NULL, `pagadaManual` INTEGER NOT NULL, `importadoEl` INTEGER NOT NULL)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_estados_cuenta_fechaEstado` ON `estados_cuenta` (`fechaEstado`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `operaciones_tarjeta` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `estadoId` INTEGER NOT NULL, `fecha` INTEGER NOT NULL, `descripcion` TEXT NOT NULL, `tipo` TEXT NOT NULL, `montoOperacion` INTEGER NOT NULL, `cuotaActual` INTEGER NOT NULL, `cuotasTotal` INTEGER NOT NULL, `valorCuota` INTEGER NOT NULL, `orden` INTEGER NOT NULL, FOREIGN KEY(`estadoId`) REFERENCES `estados_cuenta`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_operaciones_tarjeta_estadoId` ON `operaciones_tarjeta` (`estadoId`)")
    }
}
