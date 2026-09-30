package com.wuelmer.vidaos.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
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
    val metaSemanal: Int = META_SEMANAL_DEFECTO
)

const val META_SEMANAL_DEFECTO = 3
val RANGO_META_SEMANAL = 1..7

private val Context.dataStore by preferencesDataStore(name = "preferencias")

// Preferencias del usuario (fuera de Room: no son datos del dominio y no requieren migraciones).
class PreferenciasRepository(context: Context) {

    private val dataStore = context.applicationContext.dataStore
    private val claveTema = stringPreferencesKey("tema")
    private val claveModulosOcultos = stringSetPreferencesKey("modulos_ocultos")
    private val claveUnidadPeso = stringPreferencesKey("unidad_peso")
    private val claveMetaSemanal = intPreferencesKey("meta_semanal")

    val preferencias: Flow<Preferencias> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            Preferencias(
                tema = prefs[claveTema]?.let { valor -> Tema.entries.firstOrNull { it.name == valor } } ?: Tema.SISTEMA,
                modulosOcultos = prefs[claveModulosOcultos].orEmpty(),
                unidadPeso = prefs[claveUnidadPeso]?.let { valor -> UnidadPeso.entries.firstOrNull { it.name == valor } }
                    ?: UnidadPeso.KG,
                metaSemanal = (prefs[claveMetaSemanal] ?: META_SEMANAL_DEFECTO).coerceIn(RANGO_META_SEMANAL)
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
}
