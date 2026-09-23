package com.wuelmer.vidaos.data

import androidx.sqlite.db.SupportSQLiteDatabase

object SeedGym {

    private const val GRANDE = 150
    private const val PEQUENO = 75

    private class Ej(
        val nombre: String,
        val tipo: TipoEjercicio,
        val zona: ZonaEjercicio,
        val unilateral: Boolean = false
    )

    private val ejercicios = listOf(
        Ej("Sentadilla con barra", TipoEjercicio.CON_PESO, ZonaEjercicio.INFERIOR),
        Ej("Press banca", TipoEjercicio.CON_PESO, ZonaEjercicio.SUPERIOR),
        Ej("Remo con barra", TipoEjercicio.CON_PESO, ZonaEjercicio.SUPERIOR),
        Ej("Press militar con mancuernas", TipoEjercicio.CON_PESO, ZonaEjercicio.SUPERIOR),
        Ej("Curl femoral en máquina", TipoEjercicio.CON_PESO, ZonaEjercicio.INFERIOR),
        Ej("Curl de bíceps con barra", TipoEjercicio.CON_PESO, ZonaEjercicio.SUPERIOR),
        Ej("Plancha", TipoEjercicio.TIEMPO, ZonaEjercicio.CORE),
        Ej("Peso muerto rumano", TipoEjercicio.CON_PESO, ZonaEjercicio.INFERIOR),
        Ej("Dominadas", TipoEjercicio.PESO_CORPORAL, ZonaEjercicio.SUPERIOR),
        Ej("Jalón al pecho", TipoEjercicio.CON_PESO, ZonaEjercicio.SUPERIOR),
        Ej("Press inclinado con mancuernas", TipoEjercicio.CON_PESO, ZonaEjercicio.SUPERIOR),
        Ej("Sentadilla búlgara", TipoEjercicio.CON_PESO, ZonaEjercicio.INFERIOR, unilateral = true),
        Ej("Elevaciones laterales", TipoEjercicio.CON_PESO, ZonaEjercicio.SUPERIOR),
        Ej("Extensión de tríceps en polea", TipoEjercicio.CON_PESO, ZonaEjercicio.SUPERIOR),
        Ej("Prensa de piernas", TipoEjercicio.CON_PESO, ZonaEjercicio.INFERIOR),
        Ej("Fondos en paralelas", TipoEjercicio.PESO_CORPORAL, ZonaEjercicio.SUPERIOR),
        Ej("Press banca con mancuernas", TipoEjercicio.CON_PESO, ZonaEjercicio.SUPERIOR),
        Ej("Remo con mancuerna a una mano", TipoEjercicio.CON_PESO, ZonaEjercicio.SUPERIOR, unilateral = true),
        Ej("Hip thrust", TipoEjercicio.CON_PESO, ZonaEjercicio.INFERIOR),
        Ej("Face pull", TipoEjercicio.CON_PESO, ZonaEjercicio.SUPERIOR),
        Ej("Elevación de talones", TipoEjercicio.CON_PESO, ZonaEjercicio.INFERIOR),
        Ej("Curl martillo", TipoEjercicio.CON_PESO, ZonaEjercicio.SUPERIOR)
    )

    private fun idDe(nombre: String): Long =
        ejercicios.indexOfFirst { it.nombre == nombre }.also { require(it >= 0) { nombre } } + 1L

    private class Fila(
        val ejercicio: String,
        val series: Int,
        val min: Int,
        val max: Int,
        val descanso: Int,
        val alternativa: String? = null
    )

    private val dias: List<Pair<String, List<Fila>>> = listOf(
        "Día 1" to listOf(
            Fila("Sentadilla con barra", 4, 6, 8, GRANDE),
            Fila("Press banca", 4, 6, 8, GRANDE),
            Fila("Remo con barra", 3, 8, 10, GRANDE),
            Fila("Press militar con mancuernas", 3, 8, 10, GRANDE),
            Fila("Curl femoral en máquina", 3, 10, 12, PEQUENO),
            Fila("Curl de bíceps con barra", 2, 10, 12, PEQUENO),
            Fila("Plancha", 3, 30, 45, PEQUENO)
        ),
        "Día 2" to listOf(
            Fila("Peso muerto rumano", 4, 6, 8, GRANDE),
            Fila("Dominadas", 4, 6, 10, GRANDE, alternativa = "Jalón al pecho"),
            Fila("Press inclinado con mancuernas", 3, 8, 10, GRANDE),
            Fila("Sentadilla búlgara", 3, 8, 10, GRANDE),
            Fila("Elevaciones laterales", 3, 12, 15, PEQUENO),
            Fila("Extensión de tríceps en polea", 2, 10, 12, PEQUENO)
        ),
        "Día 3" to listOf(
            Fila("Prensa de piernas", 4, 8, 10, GRANDE),
            Fila("Fondos en paralelas", 3, 8, 10, GRANDE, alternativa = "Press banca con mancuernas"),
            Fila("Remo con mancuerna a una mano", 3, 8, 10, GRANDE),
            Fila("Hip thrust", 3, 8, 10, GRANDE),
            Fila("Face pull", 3, 12, 15, PEQUENO),
            Fila("Elevación de talones", 3, 12, 15, PEQUENO),
            Fila("Curl martillo", 2, 10, 12, PEQUENO)
        )
    )

    fun insertar(db: SupportSQLiteDatabase) {
        ejercicios.forEachIndexed { i, e ->
            db.execSQL(
                "INSERT INTO ejercicios_gym (id, nombre, tipo, zona, unilateral) VALUES (?, ?, ?, ?, ?)",
                arrayOf<Any?>(i + 1L, e.nombre, e.tipo.name, e.zona.name, if (e.unilateral) 1 else 0)
            )
        }

        db.execSQL("INSERT INTO rutinas (id, nombre) VALUES (?, ?)", arrayOf<Any?>(1L, "Full Body 3 días"))

        dias.forEachIndexed { d, (nombreDia, filas) ->
            val diaId = d + 1L
            db.execSQL(
                "INSERT INTO dias_rutina (id, rutinaId, numero, nombre) VALUES (?, ?, ?, ?)",
                arrayOf<Any?>(diaId, 1L, d + 1, nombreDia)
            )
            filas.forEachIndexed { orden, f ->
                db.execSQL(
                    "INSERT INTO rutina_ejercicios (diaRutinaId, ejercicioId, alternativaEjercicioId, orden, series, objetivoMin, objetivoMax, descansoSegundos) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    arrayOf<Any?>(diaId, idDe(f.ejercicio), f.alternativa?.let(::idDe), orden + 1, f.series, f.min, f.max, f.descanso)
                )
            }
        }
    }
}
