package com.wuelmer.vidaos

import android.app.Application
import com.wuelmer.vidaos.avisos.Notificador
import com.wuelmer.vidaos.avisos.ProgramadorAvisos
import com.wuelmer.vidaos.avisos.horarioAvisos
import com.wuelmer.vidaos.data.PreferenciasRepository
import com.wuelmer.vidaos.data.RespaldoRepository
import com.wuelmer.vidaos.data.VidaOSDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class VidaOSApplication : Application() {
    // Para trabajo que debe terminar aunque se cierre la pantalla que lo inició.
    val applicationScope = CoroutineScope(SupervisorJob())
    val database: VidaOSDatabase by lazy { VidaOSDatabase.getInstance(this, applicationScope) }
    val preferencias: PreferenciasRepository by lazy { PreferenciasRepository(this) }
    val respaldo: RespaldoRepository by lazy { RespaldoRepository(this, database) }

    override fun onCreate() {
        super.onCreate()
        Notificador.crearCanales(this)
        // Al arrancar y cada vez que cambian las horas, los interruptores o los módulos visibles.
        applicationScope.launch {
            preferencias.preferencias
                .map(::horarioAvisos)
                .distinctUntilChanged()
                .collect { ProgramadorAvisos.programar(this@VidaOSApplication, it) }
        }
    }
}
