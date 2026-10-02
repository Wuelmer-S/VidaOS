package com.wuelmer.vidaos.data

import androidx.sqlite.db.SupportSQLiteDatabase

// Plan de mantención de la Yamaha FZ 3.0 según el manual (y lo que el usuario ya hace).
// Es solo el punto de partida: cada intervalo se podrá editar desde la app.
object SeedMoto {

    private class Plan(val nombre: String, val icono: String, val cadaKm: Int?, val cadaDias: Int?)

    private val plan = listOf(
        // El aceite se degrada aunque la moto ande poco: 3.000 km o 6 meses, lo que ocurra primero.
        Plan("Aceite", "🛢️", 3000, 180),
        // El usuario cambia el filtro aparte, cada 4.000 km.
        Plan("Filtro de aceite", "🧴", 4000, null),
        // Incluye limpiar y ajustar la holgura (30–40 mm); también después de lluvia o lavado.
        Plan("Lubricar cadena", "⛓️", 500, null),
        Plan("Filtro de aire", "🌬️", 16000, null),
        Plan("Bujía", "⚡", 16000, null),
        Plan("Líquido de frenos", "🛑", null, 730),
        Plan("Mangueras de freno", "🛑", null, 1460),
        Plan("Presión de neumáticos", "🛞", null, 7)
    )

    fun insertar(db: SupportSQLiteDatabase) {
        plan.forEach { p ->
            db.execSQL(
                "INSERT INTO tipos_mantencion (nombre, icono, cadaKm, cadaDias, avisarAlPct, trabajoId, activo) VALUES (?, ?, ?, ?, 90, NULL, 1)",
                arrayOf<Any?>(p.nombre, p.icono, p.cadaKm, p.cadaDias)
            )
        }
    }
}
