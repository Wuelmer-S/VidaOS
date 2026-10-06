package com.wuelmer.vidaos.avisos

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.wuelmer.vidaos.data.Preferencias
import com.wuelmer.vidaos.ui.navegacion.Modulo
import com.wuelmer.vidaos.ui.navegacion.modulosVisibles
import java.time.LocalDateTime
import java.time.ZoneId

// Lo que define cuándo suenan los avisos. Se reprograma solo si esto cambia (no por el tema, etc.).
data class HorarioAvisos(val moto: Int?, val gastos: Int?, val tarjeta: Int? = null)

// null = ese aviso está apagado o su módulo está oculto.
fun horarioAvisos(prefs: Preferencias): HorarioAvisos {
    val visibles = modulosVisibles(prefs.modulosOcultos)
    return HorarioAvisos(
        moto = prefs.horaAvisosMoto.takeIf { prefs.avisosMoto && Modulo.MOTO in visibles },
        gastos = prefs.horaRecordatorioGastos.takeIf { prefs.recordatorioGastos && Modulo.FINANZAS in visibles },
        tarjeta = prefs.horaAvisoPagoTarjeta.takeIf { prefs.avisoPagoTarjeta && Modulo.FINANZAS in visibles }
    )
}

// Deja programada la próxima alarma de cada aviso. Cada alarma, al sonar, programa la del día siguiente.
object ProgramadorAvisos {
    const val ACCION_MOTO = "com.wuelmer.vidaos.AVISOS_MOTO"
    const val ACCION_GASTOS = "com.wuelmer.vidaos.RECORDATORIO_GASTOS"
    const val ACCION_TARJETA = "com.wuelmer.vidaos.AVISO_PAGO_TARJETA"

    // Si el sistema no deja usar alarmas exactas: llega a más tardar 15 min después de la hora.
    private const val VENTANA_MS = 15 * 60 * 1000L

    fun programar(context: Context, horario: HorarioAvisos) {
        programarUno(context, ACCION_MOTO, horario.moto)
        programarUno(context, ACCION_GASTOS, horario.gastos)
        programarUno(context, ACCION_TARJETA, horario.tarjeta)
    }

    private fun programarUno(context: Context, accion: String, minutosDelDia: Int?) {
        val alarmas = context.getSystemService(AlarmManager::class.java)
        val pendiente = PendingIntent.getBroadcast(
            context,
            accion.hashCode(),
            Intent(context, AvisosReceiver::class.java).setAction(accion),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmas.cancel(pendiente)
        if (minutosDelDia == null) return

        val cuando = proximaEjecucion(LocalDateTime.now(), minutosDelDia)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        // Exacta y aunque el teléfono esté en reposo, para que el aviso de la mañana llegue antes de salir.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmas.canScheduleExactAlarms()) {
            alarmas.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cuando, pendiente)
        } else {
            alarmas.setWindow(AlarmManager.RTC_WAKEUP, cuando, VENTANA_MS, pendiente)
        }
    }
}
