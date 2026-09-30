package com.wuelmer.vidaos.data

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.sqlite.db.SimpleSQLiteQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

sealed interface ResultadoRespaldo {
    object Ok : ResultadoRespaldo
    data class Error(val mensaje: String) : ResultadoRespaldo
}

/**
 * Respaldo = copia completa del archivo SQLite de Room (Finanzas y Gym). Las preferencias
 * (tema, módulos, unidad) viven en DataStore y no se incluyen.
 */
class RespaldoRepository(
    private val context: Context,
    private val database: VidaOSDatabase
) {

    suspend fun exportar(destino: Uri): ResultadoRespaldo = withContext(Dispatchers.IO) {
        runCatching {
            // Vuelca el WAL al archivo principal para que la copia tenga todo lo último.
            database.query(SimpleSQLiteQuery("PRAGMA wal_checkpoint(FULL)")).use { it.moveToFirst() }
            val salida = context.contentResolver.openOutputStream(destino, "w")
                ?: error("No se pudo abrir el archivo de destino.")
            salida.use { out -> archivoBase().inputStream().use { it.copyTo(out) } }
            ResultadoRespaldo.Ok
        }.getOrElse { ResultadoRespaldo.Error("No se pudo exportar: ${it.message}") }
    }

    /**
     * Valida el archivo y, si sirve, reemplaza la base actual y reinicia la app.
     * Solo vuelve (con Error) si algo falla antes de tocar la base actual.
     */
    suspend fun restaurar(origen: Uri): ResultadoRespaldo = withContext(Dispatchers.IO) {
        val temporal = File(context.cacheDir, "respaldo_a_restaurar.db")
        try {
            val entrada = context.contentResolver.openInputStream(origen)
                ?: return@withContext ResultadoRespaldo.Error("No se pudo abrir el archivo.")
            entrada.use { input -> temporal.outputStream().use { input.copyTo(it) } }

            val versionActual = database.openHelper.readableDatabase.version
            validar(temporal, versionActual)?.let { return@withContext ResultadoRespaldo.Error(it) }

            database.close()
            val base = archivoBase()
            listOf(base, File(base.path + "-wal"), File(base.path + "-shm")).forEach { it.delete() }
            temporal.copyTo(base, overwrite = true)
            reiniciarApp()
            ResultadoRespaldo.Ok
        } finally {
            temporal.delete()
        }
    }

    // Devuelve el motivo si el archivo no es un respaldo de vidaOS utilizable, o null si sirve.
    private fun validar(archivo: File, versionActual: Int): String? {
        val db = runCatching {
            SQLiteDatabase.openDatabase(archivo.path, null, SQLiteDatabase.OPEN_READONLY)
        }.getOrNull() ?: return "El archivo no es un respaldo de vidaOS."
        return db.use {
            val tablas = it.rawQuery("SELECT name FROM sqlite_master WHERE type = 'table'", null).use { c ->
                buildSet { while (c.moveToNext()) add(c.getString(0)) }
            }
            when {
                "movimientos" !in tablas || "categorias" !in tablas -> "El archivo no es un respaldo de vidaOS."
                it.version > versionActual -> "El respaldo es de una versión más nueva de la app. Actualiza la app primero."
                it.version < 1 -> "El respaldo está dañado."
                else -> null // Si es más antiguo, Room lo migra al abrir.
            }
        }
    }

    private fun archivoBase(): File = context.getDatabasePath(VidaOSDatabase.NOMBRE_ARCHIVO)

    // La base quedó cerrada: hay que arrancar un proceso nuevo para que Room la vuelva a abrir.
    private fun reiniciarApp() {
        val lanzador = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
        val intent = Intent.makeRestartActivityTask(lanzador.component)
        context.startActivity(intent)
        Runtime.getRuntime().exit(0)
    }
}
