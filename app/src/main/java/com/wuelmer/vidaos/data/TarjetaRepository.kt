package com.wuelmer.vidaos.data

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

sealed interface ResultadoPdf {
    data class Ok(val factura: FacturaLeida) : ResultadoPdf
    // El PDF tiene clave y no se tiene o la guardada no sirve.
    data class NecesitaClave(val incorrecta: Boolean) : ResultadoPdf
    data class Error(val mensaje: String) : ResultadoPdf
}

// Abre el PDF del estado de cuenta (todo dentro del teléfono, sin internet) y lo pasa al lector.
class TarjetaRepository(
    private val context: Context,
    private val preferencias: PreferenciasRepository
) {
    @Volatile
    private var pdfBoxListo = false

    // clave = null usa la guardada. Si la clave sirve, se guarda para la próxima vez.
    suspend fun leer(uri: Uri, clave: String? = null): ResultadoPdf = withContext(Dispatchers.IO) {
        if (!pdfBoxListo) {
            PDFBoxResourceLoader.init(context.applicationContext)
            pdfBoxListo = true
        }
        val claveUsada = clave ?: preferencias.getClavePdfTarjeta() ?: ""
        val texto = try {
            val entrada = context.contentResolver.openInputStream(uri)
                ?: return@withContext ResultadoPdf.Error("No se pudo abrir el archivo.")
            entrada.use { stream ->
                PDDocument.load(stream, claveUsada).use { doc ->
                    PDFTextStripper().apply { sortByPosition = true }.getText(doc)
                }
            }
        } catch (_: InvalidPasswordException) {
            return@withContext ResultadoPdf.NecesitaClave(incorrecta = clave != null || claveUsada.isNotEmpty())
        } catch (e: IOException) {
            return@withContext ResultadoPdf.Error("No se pudo leer el PDF (${e.message ?: "archivo dañado"}).")
        } catch (_: SecurityException) {
            return@withContext ResultadoPdf.Error("Sin permiso para leer el archivo. Vuelve a compartirlo.")
        }
        if (clave != null) preferencias.setClavePdfTarjeta(clave)
        when (val r = LectorEstadoCuenta.leer(texto)) {
            is ResultadoLectura.Ok -> ResultadoPdf.Ok(r.factura)
            is ResultadoLectura.Error -> ResultadoPdf.Error(r.mensaje)
        }
    }
}
