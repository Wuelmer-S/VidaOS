package com.wuelmer.vidaos.avisos

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.wuelmer.vidaos.VidaOSApplication
import com.wuelmer.vidaos.ui.moto.calcularEstado
import com.wuelmer.vidaos.ui.navegacion.Modulo
import com.wuelmer.vidaos.ui.navegacion.modulosVisibles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

// Recibe las alarmas de los avisos y también el reinicio del teléfono o el cambio de hora
// (Android borra las alarmas al apagarse, así que hay que volver a programarlas).
class AvisosReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as VidaOSApplication
        val pendiente = goAsync()
        app.applicationScope.launch(Dispatchers.IO) {
            try {
                when (intent.action) {
                    ProgramadorAvisos.ACCION_MOTO -> revisarMoto(app)
                    ProgramadorAvisos.ACCION_GASTOS -> revisarGastos(app)
                }
                ProgramadorAvisos.programar(app, horarioAvisos(app.preferencias.preferencias.first()))
            } finally {
                pendiente.finish()
            }
        }
    }

    companion object {
        // Mantenciones al 50 %, al % de aviso del tipo y vencidas; y km sin anotar hace 7 días o más.
        suspend fun revisarMoto(app: VidaOSApplication) {
            val prefs = app.preferencias.preferencias.first()
            if (!prefs.avisosMoto || Modulo.MOTO !in modulosVisibles(prefs.modulosOcultos)) return
            // Sin permiso no se marca nada como avisado: cuando lo dé, le llegan los avisos pendientes.
            if (!Notificador.puedeNotificar(app)) return

            val dao = app.database.motoDao()
            val hoy = LocalDate.now()
            val ultimaLectura = dao.getLecturas().first().firstOrNull()
            val ultimos = dao.getUltimosRegistros().first().associateBy { it.tipoId }
            val estados = dao.getTiposActivos().first().map { tipo ->
                calcularEstado(tipo, ultimos[tipo.id], ultimaLectura?.km, hoy)
            }

            val resultado = avisosPendientes(estados, app.preferencias.getAvisosMotoEnviados())
            resultado.avisos.forEach { Notificador.mantencion(app, it) }
            app.preferencias.setAvisosMotoEnviados(resultado.enviados)

            if (necesitaRecordatorioKm(ultimaLectura?.fecha, hoy)) {
                Notificador.km(app, textoRecordatorioKm(ultimaLectura?.fecha, hoy))
            }
        }

        // Solo si ese día no anotó ningún gasto.
        suspend fun revisarGastos(app: VidaOSApplication) {
            val prefs = app.preferencias.preferencias.first()
            if (!prefs.recordatorioGastos || Modulo.FINANZAS !in modulosVisibles(prefs.modulosOcultos)) return
            if (app.database.movimientoDao().contarGastosDelDia(LocalDate.now()) == 0) Notificador.gastos(app)
        }
    }
}
