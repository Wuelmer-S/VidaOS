package com.wuelmer.vidaos.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

enum class Tema {
    SISTEMA,
    CLARO,
    OSCURO
}

enum class UnidadPeso {
    KG,
    LB
}

// Se guardan los módulos ocultos (no los activos) para que un módulo nuevo aparezca visible por defecto.
data class Preferencias(
    val tema: Tema = Tema.SISTEMA,
    val modulosOcultos: Set<String> = emptySet(),
    val unidadPeso: UnidadPeso = UnidadPeso.KG,
    val metaSemanal: Int = META_SEMANAL_DEFECTO,
    val avisosMoto: Boolean = true,
    val horaAvisosMoto: Int = HORA_AVISOS_MOTO_DEFECTO,
    val recordatorioGastos: Boolean = true,
    val horaRecordatorioGastos: Int = HORA_RECORDATORIO_GASTOS_DEFECTO,
    val avisoPagoTarjeta: Boolean = true,
    val horaAvisoPagoTarjeta: Int = HORA_AVISO_PAGO_TARJETA_DEFECTO
)

const val META_SEMANAL_DEFECTO = 3
val RANGO_META_SEMANAL = 1..7

// Horas en minutos desde medianoche: 07:30 antes de salir en la moto, 20:00 para anotar los gastos del día.
const val HORA_AVISOS_MOTO_DEFECTO = 7 * 60 + 30
const val HORA_RECORDATORIO_GASTOS_DEFECTO = 20 * 60
// 09:00: con tiempo para pagar la tarjeta durante el día.
const val HORA_AVISO_PAGO_TARJETA_DEFECTO = 9 * 60
private val RANGO_MINUTOS_DIA = 0 until 24 * 60

private val Context.dataStore by preferencesDataStore(name = "preferencias")

// Preferencias del usuario (fuera de Room: no son datos del dominio y no requieren migraciones).
class PreferenciasRepository(context: Context) {

    private val dataStore = context.applicationContext.dataStore
    private val claveTema = stringPreferencesKey("tema")
    private val claveModulosOcultos = stringSetPreferencesKey("modulos_ocultos")
    private val claveUnidadPeso = stringPreferencesKey("unidad_peso")
    private val claveMetaSemanal = intPreferencesKey("meta_semanal")
    private val claveAvisosMoto = booleanPreferencesKey("avisos_moto")
    private val claveHoraAvisosMoto = intPreferencesKey("hora_avisos_moto")
    private val claveRecordatorioGastos = booleanPreferencesKey("recordatorio_gastos")
    private val claveHoraRecordatorioGastos = intPreferencesKey("hora_recordatorio_gastos")
    private val claveAvisoPagoTarjeta = booleanPreferencesKey("aviso_pago_tarjeta")
    private val claveHoraAvisoPagoTarjeta = intPreferencesKey("hora_aviso_pago_tarjeta")
    private val claveAvisosMotoEnviados = stringSetPreferencesKey("avisos_moto_enviados")
    private val claveClavePdfTarjeta = stringPreferencesKey("clave_pdf_tarjeta")

    val preferencias: Flow<Preferencias> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            Preferencias(
                tema = prefs[claveTema]?.let { valor -> Tema.entries.firstOrNull { it.name == valor } } ?: Tema.SISTEMA,
                modulosOcultos = prefs[claveModulosOcultos].orEmpty(),
                unidadPeso = prefs[claveUnidadPeso]?.let { valor -> UnidadPeso.entries.firstOrNull { it.name == valor } }
                    ?: UnidadPeso.KG,
                metaSemanal = (prefs[claveMetaSemanal] ?: META_SEMANAL_DEFECTO).coerceIn(RANGO_META_SEMANAL),
                avisosMoto = prefs[claveAvisosMoto] ?: true,
                horaAvisosMoto = (prefs[claveHoraAvisosMoto] ?: HORA_AVISOS_MOTO_DEFECTO).coerceIn(RANGO_MINUTOS_DIA),
                recordatorioGastos = prefs[claveRecordatorioGastos] ?: true,
                horaRecordatorioGastos = (prefs[claveHoraRecordatorioGastos] ?: HORA_RECORDATORIO_GASTOS_DEFECTO)
                    .coerceIn(RANGO_MINUTOS_DIA),
                avisoPagoTarjeta = prefs[claveAvisoPagoTarjeta] ?: true,
                horaAvisoPagoTarjeta = (prefs[claveHoraAvisoPagoTarjeta] ?: HORA_AVISO_PAGO_TARJETA_DEFECTO)
                    .coerceIn(RANGO_MINUTOS_DIA)
            )
        }

    suspend fun setTema(tema: Tema) {
        dataStore.edit { it[claveTema] = tema.name }
    }

    suspend fun setUnidadPeso(unidad: UnidadPeso) {
        dataStore.edit { it[claveUnidadPeso] = unidad.name }
    }

    suspend fun setMetaSemanal(meta: Int) {
        dataStore.edit { it[claveMetaSemanal] = meta.coerceIn(RANGO_META_SEMANAL) }
    }

    suspend fun setModuloOculto(modulo: String, oculto: Boolean) {
        dataStore.edit { prefs ->
            val actuales = prefs[claveModulosOcultos].orEmpty()
            prefs[claveModulosOcultos] = if (oculto) actuales + modulo else actuales - modulo
        }
    }

    suspend fun setAvisosMoto(activos: Boolean) {
        dataStore.edit { it[claveAvisosMoto] = activos }
    }

    suspend fun setHoraAvisosMoto(minutos: Int) {
        dataStore.edit { it[claveHoraAvisosMoto] = minutos.coerceIn(RANGO_MINUTOS_DIA) }
    }

    suspend fun setRecordatorioGastos(activo: Boolean) {
        dataStore.edit { it[claveRecordatorioGastos] = activo }
    }

    suspend fun setHoraRecordatorioGastos(minutos: Int) {
        dataStore.edit { it[claveHoraRecordatorioGastos] = minutos.coerceIn(RANGO_MINUTOS_DIA) }
    }

    suspend fun setAvisoPagoTarjeta(activo: Boolean) {
        dataStore.edit { it[claveAvisoPagoTarjeta] = activo }
    }

    suspend fun setHoraAvisoPagoTarjeta(minutos: Int) {
        dataStore.edit { it[claveHoraAvisoPagoTarjeta] = minutos.coerceIn(RANGO_MINUTOS_DIA) }
    }

    // Avisos de mantención ya enviados ("tipoId:registroId:NIVEL"). No va en Preferencias: no lo muestra ninguna pantalla.
    suspend fun getAvisosMotoEnviados(): Set<String> =
        dataStore.data.catch { emit(emptyPreferences()) }.first()[claveAvisosMotoEnviados].orEmpty()

    suspend fun setAvisosMotoEnviados(enviados: Set<String>) {
        dataStore.edit { it[claveAvisosMotoEnviados] = enviados }
    }

    // Clave del PDF del estado de cuenta (la pide el banco). Queda solo en el teléfono, en los datos privados de la app.
    suspend fun getClavePdfTarjeta(): String? =
        dataStore.data.catch { emit(emptyPreferences()) }.first()[claveClavePdfTarjeta]

    suspend fun setClavePdfTarjeta(clave: String) {
        dataStore.edit { it[claveClavePdfTarjeta] = clave }
    }
}
