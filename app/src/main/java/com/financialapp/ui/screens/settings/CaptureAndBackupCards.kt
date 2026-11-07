package com.financialapp.ui.screens.settings

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.financialapp.capture.AppPreferences
import com.financialapp.capture.FinanceNotificationListener
import com.financialapp.data.backup.BackupFile
import com.financialapp.data.backup.BackupManager
import com.financialapp.ui.components.FinanceCard
import com.financialapp.ui.components.FinanceTextField
import com.financialapp.ui.theme.*
import kotlinx.coroutines.launch
import java.time.LocalDate

private fun hasListenerAccess(context: Context): Boolean =
    NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

private fun openAppDetails(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
    )
}

@Composable
fun CaptureCard(preferences: AppPreferences) {
    val context = LocalContext.current
    var listenerGranted by remember { mutableStateOf(hasListenerAccess(context)) }
    var notificationsAllowed by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    var enabled by remember { mutableStateOf(preferences.captureEnabled) }
    var reminder by remember { mutableStateOf(preferences.dailyReminderEnabled) }
    var keywords by remember { mutableStateOf(preferences.captureKeywords) }

    // Los permisos se conceden fuera de la app: se revisan de nuevo al volver
    LifecycleResumeEffect(Unit) {
        listenerGranted = hasListenerAccess(context)
        notificationsAllowed = NotificationManagerCompat.from(context).areNotificationsEnabled()
        if (listenerGranted) {
            runCatching {
                android.service.notification.NotificationListenerService.requestRebind(
                    ComponentName(context, FinanceNotificationListener::class.java)
                )
            }
        }
        onPauseOrDispose { }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationsAllowed = granted
    }

    FinanceCard {
        Text("Captura automática", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Lee las notificaciones de las apps de abajo y deja cada movimiento por confirmar. Todo queda en tu teléfono: el texto no se envía a ningún lado y se borra al confirmarlo. Los códigos de seguridad se ignoran.",
            fontSize = 11.sp,
            color = TextSecondary,
            lineHeight = 15.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        ToggleRow("Activar captura", enabled) {
            enabled = it
            preferences.captureEnabled = it
        }
        StatusRow("Acceso a notificaciones", listenerGranted, "Conceder") {
            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        StatusRow("Permiso para avisarte", notificationsAllowed, "Permitir") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                openAppDetails(context)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        FinanceTextField(
            keywords,
            {
                keywords = it
                preferences.captureKeywords = it
            },
            "Apps a vigilar (separadas por coma)"
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text("Para Xiaomi / HyperOS", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = TextPrimary)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            "HyperOS cierra apps en segundo plano. En la info de la app activa \"Inicio automático\" y pon Ahorro de batería en \"Sin restricciones\", o la captura y los recordatorios pueden fallar.",
            fontSize = 11.sp,
            color = TextSecondary,
            lineHeight = 15.sp
        )
        TextButton(onClick = { openAppDetails(context) }) { Text("Abrir info de la app", fontSize = 12.sp, color = AccentBlue) }
        HorizontalDivider(color = DarkBorder)
        Spacer(modifier = Modifier.height(6.dp))
        ToggleRow("Recordatorio de la noche (8:30 p. m.)", reminder) {
            reminder = it
            preferences.dailyReminderEnabled = it
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, fontSize = 13.sp, color = TextPrimary, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun StatusRow(label: String, ok: Boolean, action: String, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 13.sp, color = TextPrimary)
            Text(if (ok) "Activo" else "Falta activarlo", fontSize = 11.sp, color = if (ok) AccentPrimaryLight else AccentOrange)
        }
        if (!ok) {
            TextButton(onClick = onClick) { Text(action, fontSize = 12.sp, color = AccentBlue) }
        }
    }
}

@Composable
fun BackupCard(backupManager: BackupManager) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var toRestore by remember { mutableStateOf<BackupFile?>(null) }

    fun toast(text: String) = Toast.makeText(context, text, Toast.LENGTH_LONG).show()

    val exportJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            runCatching { backupManager.exportJson(uri) }
                .onSuccess { toast("Copia guardada") }
                .onFailure { toast("No se pudo guardar la copia") }
        }
    }
    val exportCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) scope.launch {
            runCatching { backupManager.exportCsv(uri) }
                .onSuccess { toast("Archivo guardado") }
                .onFailure { toast("No se pudo guardar el archivo") }
        }
    }
    val pickRestore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            backupManager.read(uri)
                .onSuccess { toRestore = it }
                .onFailure { toast(it.message ?: "El archivo no es una copia válida") }
        }
    }

    FinanceCard {
        Text("Copia de seguridad", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "La app guarda una copia interna cada noche, pero se pierde si desinstalas o cambias de teléfono. Guarda una en Drive o en tu correo de vez en cuando. No incluye tus API Keys.",
            fontSize = 11.sp,
            color = TextSecondary,
            lineHeight = 15.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { exportJson.launch("finanzas_${LocalDate.now()}.json") },
                colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary),
                modifier = Modifier.weight(1f)
            ) { Text("Guardar copia", fontSize = 12.sp) }
            OutlinedButton(
                onClick = { exportCsv.launch("movimientos_${LocalDate.now()}.csv") },
                modifier = Modifier.weight(1f)
            ) { Text("Exportar CSV", fontSize = 12.sp, color = TextPrimary) }
        }
        TextButton(onClick = { pickRestore.launch(arrayOf("application/json", "application/octet-stream", "text/plain")) }) {
            Text("Restaurar desde una copia", fontSize = 12.sp, color = AccentBlue)
        }
    }

    toRestore?.let { backup ->
        AlertDialog(
            onDismissRequest = { toRestore = null },
            title = { Text("¿Reemplazar tus datos?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Esta copia tiene ${backup.transactions.size} movimientos. Reemplazará todo lo que hay ahora en la app. Antes de hacerlo se guarda una copia interna de lo actual.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        toRestore = null
                        scope.launch {
                            runCatching { backupManager.restore(backup) }
                                .onSuccess { toast("Datos restaurados") }
                                .onFailure { toast(it.message ?: "No se pudo restaurar; tus datos no cambiaron") }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                ) { Text("Reemplazar") }
            },
            dismissButton = {
                TextButton(onClick = { toRestore = null }) { Text("Cancelar", color = TextSecondary) }
            },
            containerColor = DarkSurface
        )
    }
}
