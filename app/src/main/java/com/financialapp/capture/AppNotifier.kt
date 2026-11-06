package com.financialapp.capture

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.financialapp.R
import com.financialapp.ui.MainActivity

/** Avisos propios de la app. Si el permiso no está concedido simplemente no se muestran. */
object AppNotifier {

    private const val CHANNEL_PENDING = "pending_movements"
    private const val CHANNEL_REMINDERS = "reminders"
    private const val ID_PENDING = 1001
    const val ID_DAILY = 1002
    const val ID_BILLS = 1003
    const val ID_RECONCILE = 1004
    private const val ID_BUDGET_BASE = 2000

    const val EXTRA_OPEN_ROUTE = "open_route"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_PENDING, "Movimientos por confirmar", NotificationManager.IMPORTANCE_DEFAULT)
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_REMINDERS, "Recordatorios y presupuestos", NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    fun pendingMovement(context: Context, title: String, text: String) =
        show(context, CHANNEL_PENDING, ID_PENDING, title, text, "pending")

    fun reminder(context: Context, id: Int, title: String, text: String, route: String) =
        show(context, CHANNEL_REMINDERS, id, title, text, route)

    fun budget(context: Context, categoryId: String, title: String, text: String) =
        show(context, CHANNEL_REMINDERS, ID_BUDGET_BASE + (categoryId.hashCode() and 0xFFF), title, text, "budgets")

    private fun show(context: Context, channel: String, id: Int, title: String, text: String, route: String) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPEN_ROUTE, route)
        }
        val pending = PendingIntent.getActivity(
            context, id, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        try {
            manager.notify(id, notification)
        } catch (_: SecurityException) {
            // El usuario retiró el permiso entre la verificación y el aviso
        }
    }
}
