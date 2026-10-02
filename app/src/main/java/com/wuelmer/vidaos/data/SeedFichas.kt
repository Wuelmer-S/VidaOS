package com.wuelmer.vidaos.data

import androidx.sqlite.db.SupportSQLiteDatabase

// Fichas de los trabajos que el usuario hace solo en su Yamaha FZ 3.0.
// Torques: manual del propietario FZ-S FI V3.0. Medidas de llave: foros de dueños, sin confirmar
// (verificado = false) hasta que el usuario las pruebe con su juego de dados.
object SeedFichas {

    private class Ajuste(val parte: String, val llave: String?, val torqueNm: Int?)

    private class Ficha(
        val nombre: String,
        val icono: String,
        val pasos: List<String>,
        val materiales: List<String>,
        val ajustes: List<Ajuste>,
        val tipo: String?       // nombre del tipo de mantención del plan que se enlaza a esta ficha
    )

    private val fichas = listOf(
        Ficha(
            nombre = "Cambio de aceite",
            icono = "🛢️",
            pasos = listOf(
                "Calentar el motor 2–3 minutos y apagarlo",
                "Moto derecha en suelo plano y bandeja bajo el tapón de drenaje",
                "Sacar el tapón de llenado (arriba) para que drene mejor",
                "Sacar el tapón de drenaje y dejar escurrir 5–10 minutos",
                "Limpiar el tapón, poner golilla nueva y apretarlo a 20 N·m",
                "Cargar 1,0 L de aceite (1,1 L si también cambias el filtro)",
                "Encender 1 minuto, apagar, esperar y revisar el nivel con la moto derecha"
            ),
            materiales = listOf(
                "Aceite 10W-40 JASO MA: 1,0 L (1,1 L con filtro)",
                "Golilla nueva del tapón de drenaje",
                "Bandeja de al menos 1,5 L",
                "Embudo, paño y guantes"
            ),
            ajustes = listOf(Ajuste("Tapón de drenaje", "Llave o dado 17 mm", 20)),
            tipo = "Aceite"
        ),
        Ficha(
            nombre = "Cambio de filtro de aceite",
            icono = "🧴",
            pasos = listOf(
                "Hacerlo junto con el cambio de aceite, con el aceite ya drenado",
                "Sacar los pernos de la tapa del filtro",
                "Sacar el filtro viejo fijándote en cómo estaba puesto",
                "Revisar el O-ring de la tapa y limpiar el asiento",
                "Poner el filtro nuevo en la misma posición",
                "Cerrar la tapa y apretar los pernos a 10 N·m, de a poco y en cruz",
                "Cargar 1,1 L de aceite en total y revisar el nivel"
            ),
            materiales = listOf(
                "Filtro de aceite nuevo",
                "O-ring de la tapa (si está duro o aplastado)",
                "0,1 L extra de aceite"
            ),
            ajustes = listOf(Ajuste("Pernos de la tapa del filtro", "Dado 8 mm", 10)),
            tipo = "Filtro de aceite"
        ),
        Ficha(
            nombre = "Limpiar y lubricar cadena",
            icono = "⛓️",
            pasos = listOf(
                "Levantar la rueda trasera (caballete) o ir moviendo la moto",
                "Aplicar limpiador y cepillar la cadena mientras giras la rueda a mano",
                "Secar bien con un paño",
                "Aplicar lubricante por la cara interna de la cadena, girando la rueda",
                "Esperar 10–15 minutos y quitar el exceso con el paño",
                "Repetir cada 500 km y después de lluvia o lavado"
            ),
            materiales = listOf(
                "Limpiador de cadena",
                "Cepillo de cadena",
                "Lubricante de cadena",
                "Paño"
            ),
            ajustes = emptyList(),
            tipo = "Lubricar cadena"
        ),
        Ficha(
            nombre = "Tensar cadena",
            icono = "🔧",
            pasos = listOf(
                "Medir la holgura a mitad del tramo de abajo: debe ser 30–40 mm",
                "Aflojar la tuerca del eje trasero",
                "Aflojar las contratuercas de los tensores (ambos lados)",
                "Girar los tensores lo mismo en ambos lados, guiándote por las marcas",
                "Volver a medir la holgura: 30–40 mm",
                "Apretar las contratuercas a 14 N·m",
                "Apretar la tuerca del eje trasero a 59 N·m",
                "Medir la holgura una última vez"
            ),
            materiales = listOf("Regla o huincha para medir la holgura"),
            ajustes = listOf(
                Ajuste("Tuerca del eje trasero", null, 59),
                Ajuste("Contratuercas de los tensores", null, 14)
            ),
            tipo = null
        )
    )

    fun insertar(db: SupportSQLiteDatabase) {
        fichas.forEachIndexed { i, f ->
            val trabajoId = i + 1L
            db.execSQL(
                "INSERT INTO trabajos (id, nombre, icono, pasos, materiales) VALUES (?, ?, ?, ?, ?)",
                arrayOf<Any?>(trabajoId, f.nombre, f.icono, f.pasos.joinToString("\n"), f.materiales.joinToString("\n"))
            )
            f.ajustes.forEachIndexed { orden, a ->
                db.execSQL(
                    "INSERT INTO ajustes_trabajo (trabajoId, parte, llave, torqueNm, verificado, orden) VALUES (?, ?, ?, ?, 0, ?)",
                    arrayOf<Any?>(trabajoId, a.parte, a.llave, a.torqueNm, orden)
                )
            }
            // Por nombre: si el usuario renombró el tipo, simplemente queda sin enlazar (se puede elegir después).
            f.tipo?.let {
                db.execSQL(
                    "UPDATE tipos_mantencion SET trabajoId = ? WHERE nombre = ? AND trabajoId IS NULL",
                    arrayOf<Any?>(trabajoId, it)
                )
            }
        }
    }
}
