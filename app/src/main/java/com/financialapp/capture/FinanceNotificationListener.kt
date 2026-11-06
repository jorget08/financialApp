package com.financialapp.capture

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.financialapp.data.local.AppDatabase
import com.financialapp.data.local.entities.MovementType
import com.financialapp.data.local.entities.PendingMovementEntity
import com.financialapp.domain.engine.NotificationParser
import com.financialapp.util.CurrencyFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.security.MessageDigest

/**
 * Lee únicamente las notificaciones de las apps elegidas (por defecto Nequi y Falabella) y deja
 * el movimiento en una bandeja para confirmar. Todo ocurre en el teléfono: el texto no sale de
 * la app y se borra al confirmar o descartar.
 */
class FinanceNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn ?: return
        if (notification.packageName == packageName) return
        if (notification.isOngoing) return
        if (notification.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val prefs = AppPreferences(this)
        if (!prefs.captureEnabled) return

        val label = appLabel(notification.packageName)
        if (!matchesKeywords(notification.packageName, label, prefs.keywordList())) return

        val extras = notification.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString()
        val parsed = NotificationParser.parse(title, text) ?: return

        val rawText = listOfNotNull(title, text).joinToString(" • ")
        val dedupeKey = sha256("${notification.packageName}|$rawText")
        val postedAt = notification.postTime

        scope.launch {
            val db = AppDatabase.getInstance(applicationContext)
            val dao = db.pendingMovementDao()
            // Algunas apps vuelven a publicar la misma notificación al actualizarla
            if (dao.countRecent(dedupeKey, postedAt - DUPLICATE_WINDOW_MS) > 0) return@launch
            dao.insert(
                PendingMovementEntity(
                    sourceLabel = label,
                    rawText = rawText,
                    amount = parsed.amount,
                    type = parsed.type,
                    merchant = parsed.merchant,
                    suggestedCategoryName = parsed.suggestedCategoryName,
                    postedAt = postedAt,
                    dedupeKey = dedupeKey
                )
            )
            val amountText = parsed.amount?.let { CurrencyFormatter.format(it) } ?: "Monto por revisar"
            val direction = if (parsed.type == MovementType.INCOME) "Entró" else "Movimiento"
            AppNotifier.pendingMovement(
                applicationContext,
                "$direction en $label: $amountText",
                "Toca para confirmarlo y elegir la categoría."
            )
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun appLabel(packageName: String): String = runCatching {
        val info = packageManager.getApplicationInfo(packageName, 0)
        packageManager.getApplicationLabel(info).toString()
    }.getOrDefault(packageName)

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }

    companion object {
        private const val DUPLICATE_WINDOW_MS = 5 * 60 * 1000L

        fun matchesKeywords(packageName: String, label: String, keywords: List<String>): Boolean {
            val haystack = "${packageName.lowercase()} ${label.lowercase()}"
            return keywords.any { haystack.contains(it) }
        }
    }
}
