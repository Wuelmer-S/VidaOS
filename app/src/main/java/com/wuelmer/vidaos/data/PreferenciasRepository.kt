package com.wuelmer.vidaos.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
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

private val Context.dataStore by preferencesDataStore(name = "preferencias")

// Preferencias del usuario (fuera de Room: no son datos del dominio y no requieren migraciones).
class PreferenciasRepository(context: Context) {

    private val dataStore = context.applicationContext.dataStore
    private val claveTema = stringPreferencesKey("tema")

    val tema: Flow<Tema> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            prefs[claveTema]?.let { valor -> Tema.entries.firstOrNull { it.name == valor } } ?: Tema.SISTEMA
        }

    suspend fun setTema(tema: Tema) {
        dataStore.edit { it[claveTema] = tema.name }
    }
}
