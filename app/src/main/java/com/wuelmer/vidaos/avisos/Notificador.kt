package com.wuelmer.vidaos.avisos

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.wuelmer.vidaos.MainActivity
import com.wuelmer.vidaos.R
import com.wuelmer.vidaos.ui.navegacion.Modulo

// Muestra las notificaciones. Al tocarlas se abre la app en el módulo (y la pantalla) que corresponde.
object Notificador {
    const val EXTRA_MODULO = "abrir_modulo"
    const val EXTRA_RUTA = "abrir_ruta"

    private const val CANAL_MOTO = "mantenciones_moto"
    private const val CANAL_GASTOS = "recordatorio_gastos"
    private const val CANAL_TARJETA = "pago_tarjeta"
    private const val ID_KM = 1
    private const val ID_GASTOS = 2
    private const val ID_TARJETA = 3
    // Una notificación por mantención: id = base + tipoId, así la de mañana reemplaza a la de hoy.
    private const val ID_BASE_MANTENCION = 1000

    fun crearCanales(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CANAL_MOTO, "Mantenciones moto", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Mantenciones que se acercan o vencieron, y km sin anotar"
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(CANAL_GASTOS, "Recordatorio de gastos", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Aviso en la noche si no anotaste gastos ese día"
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(CANAL_TARJETA, "Pago de la tarjeta", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Aviso los 3 días antes de la fecha de pago si la factura no está pagada"
            }
        )
    }

    // false si el usuario no dio permiso o bloqueó las notificaciones de la app.
    fun puedeNotificar(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun mantencion(context: Context, aviso: AvisoMantencion) {
        val tipoId = aviso.estado.tipo.id
        mostrar(
            context = context,
            id = ID_BASE_MANTENCION + tipoId.toInt(),
            canal = CANAL_MOTO,
            titulo = tituloAviso(aviso),
            texto = textoAviso(aviso),
            modulo = Modulo.MOTO,
            ruta = "moto_mantencion/$tipoId"
        )
    }

    fun km(context: Context, texto: String) {
        mostrar(context, ID_KM, CANAL_MOTO, "🏍️ Anota los km", texto, Modulo.MOTO, "moto_km")
    }

    fun gastos(context: Context) {
        mostrar(
            context = context,
            id = ID_GASTOS,
            canal = CANAL_GASTOS,
            titulo = "💸 ¿Gastaste algo hoy?",
            texto = "Todavía no anotas gastos de hoy. Tómate un minuto antes de que se te olviden.",
            modulo = Modulo.FINANZAS,
            ruta = null
        )
    }

    fun tarjeta(context: Context, texto: String) {
        mostrar(context, ID_TARJETA, CANAL_TARJETA, "💳 Paga la tarjeta", texto, Modulo.FINANZAS, "tarjeta")
    }

    private fun mostrar(
        context: Context,
        id: Int,
        canal: String,
        titulo: String,
        texto: String,
        modulo: Modulo,
        ruta: String?
    ) {
        if (!puedeNotificar(context)) return
        val abrir = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_MODULO, modulo.name)
            putExtra(EXTRA_RUTA, ruta)
        }
        val pendiente = PendingIntent.getActivity(
            context, id, abrir, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notificacion = NotificationCompat.Builder(context, canal)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setContentIntent(pendiente)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, notificacion)
        } catch (_: SecurityException) {
            // Permiso revocado justo entre la revisión y el aviso: no hay nada que hacer.
        }
    }
}
