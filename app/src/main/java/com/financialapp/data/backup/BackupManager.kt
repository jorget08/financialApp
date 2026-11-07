package com.financialapp.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.financialapp.data.local.AppDatabase
import com.financialapp.data.local.entities.AccountEntity
import com.financialapp.data.local.entities.BudgetEntity
import com.financialapp.data.local.entities.CategoryEntity
import com.financialapp.data.local.entities.GoalEntity
import com.financialapp.data.local.entities.RecurringBillEntity
import com.financialapp.data.local.entities.SavingsAccountEntity
import com.financialapp.data.local.entities.SavingsDepositEntity
import com.financialapp.data.local.entities.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDate

/**
 * Las claves de API y los textos de notificaciones por confirmar nunca entran en la copia.
 */
@Serializable
data class BackupFile(
    val backupVersion: Int = BackupCodec.CURRENT_VERSION,
    val exportedAt: Long = 0L,
    val categories: List<CategoryEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val accounts: List<AccountEntity> = emptyList(),
    val budgets: List<BudgetEntity> = emptyList(),
    val bills: List<RecurringBillEntity> = emptyList(),
    val goals: List<GoalEntity> = emptyList(),
    val savingsAccounts: List<SavingsAccountEntity> = emptyList(),
    val savingsDeposits: List<SavingsDepositEntity> = emptyList()
)

object BackupCodec {
    const val CURRENT_VERSION = 1

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = true }

    fun encode(backup: BackupFile): String = json.encodeToString(backup)

    /** Un archivo inválido debe fallar aquí, antes de tocar los datos actuales. */
    fun decode(text: String): Result<BackupFile> = runCatching {
        val backup = json.decodeFromString<BackupFile>(text)
        validate(backup)?.let { throw IllegalArgumentException(it) }
        backup
    }

    fun validate(backup: BackupFile): String? {
        if (backup.backupVersion > CURRENT_VERSION) return "La copia es de una versión más nueva de la app."
        if (backup.categories.isEmpty()) return "El archivo no parece una copia de esta app."
        val categoryIds = backup.categories.map { it.id }.toSet()
        if (backup.transactions.any { it.categoryId !in categoryIds }) return "La copia tiene movimientos sin categoría."
        if (backup.bills.any { it.categoryId !in categoryIds }) return "La copia tiene pagos fijos sin categoría."
        if (backup.budgets.any { it.categoryId !in categoryIds }) return "La copia tiene presupuestos sin categoría."
        val savingsIds = backup.savingsAccounts.map { it.id }.toSet()
        if (backup.savingsDeposits.any { it.accountId !in savingsIds }) return "La copia tiene depósitos sin cuenta."
        if (backup.transactions.any { !it.amount.isFinite() }) return "La copia tiene montos inválidos."
        return null
    }

    fun toCsv(transactions: List<TransactionEntity>, categoryNames: Map<String, String>, accountNames: Map<String, String>): String {
        val header = "fecha,tipo,monto,categoria,cuenta,persona,observacion,origen"
        val rows = transactions.sortedBy { it.date }.map { t ->
            listOf(
                java.time.Instant.ofEpochMilli(t.date).toString(),
                t.type,
                t.amount.toString(),
                categoryNames[t.categoryId].orEmpty(),
                accountNames[t.accountId].orEmpty(),
                t.personName.orEmpty(),
                t.note.orEmpty(),
                t.source
            ).joinToString(",") { csvCell(it) }
        }
        return (listOf(header) + rows).joinToString("\n")
    }

    private fun csvCell(value: String): String {
        // Un apóstrofo evita que una hoja de cálculo ejecute el texto como fórmula
        val safe = if (value.firstOrNull() in listOf('=', '+', '@')) "'$value" else value
        return "\"${safe.replace("\"", "\"\"")}\""
    }
}

class BackupManager(private val context: Context, private val db: AppDatabase) {

    suspend fun snapshot(): BackupFile = withContext(Dispatchers.IO) {
        val dao = db.backupDao()
        BackupFile(
            exportedAt = System.currentTimeMillis(),
            categories = dao.categories(),
            transactions = dao.transactions(),
            accounts = db.accountDao().all(),
            budgets = db.budgetDao().all(),
            bills = db.billDao().all(),
            goals = db.goalDao().all(),
            savingsAccounts = dao.savingsAccounts(),
            savingsDeposits = dao.savingsDeposits()
        )
    }

    suspend fun exportJson(target: Uri) = writeTo(target, BackupCodec.encode(snapshot()))

    suspend fun exportCsv(target: Uri) {
        val data = snapshot()
        writeTo(
            target,
            BackupCodec.toCsv(
                data.transactions,
                data.categories.associate { it.id to it.name },
                data.accounts.associate { it.id to it.name }
            )
        )
    }

    private suspend fun writeTo(target: Uri, content: String) = withContext(Dispatchers.IO) {
        val stream = context.contentResolver.openOutputStream(target, "wt")
            ?: throw IllegalStateException("No se pudo abrir el archivo de destino.")
        stream.use { it.write(content.toByteArray()) }
    }

    /** Lee y valida sin modificar nada. */
    suspend fun read(source: Uri): Result<BackupFile> = withContext(Dispatchers.IO) {
        val text = runCatching {
            context.contentResolver.openInputStream(source)?.use { it.readBytes().decodeToString() }
                ?: throw IllegalStateException("No se pudo abrir el archivo.")
        }
        text.fold(onSuccess = { BackupCodec.decode(it) }, onFailure = { Result.failure(it) })
    }

    /** Reemplaza todo dentro de una transacción: si algo falla, los datos actuales quedan intactos. */
    suspend fun restore(backup: BackupFile) = withContext(Dispatchers.IO) {
        BackupCodec.validate(backup)?.let { throw IllegalArgumentException(it) }
        writeAutomatic("antes_de_restaurar")
        val dao = db.backupDao()
        db.withTransaction {
            dao.clearBills()
            dao.clearBudgets()
            dao.clearTransactions()
            dao.clearCategories()
            dao.clearAccounts()
            dao.clearGoals()
            dao.clearSavingsDeposits()
            dao.clearSavingsAccounts()
            dao.insertCategories(backup.categories)
            dao.insertAccounts(backup.accounts)
            dao.insertTransactions(backup.transactions)
            dao.insertBudgets(backup.budgets)
            dao.insertBills(backup.bills)
            dao.insertGoals(backup.goals)
            dao.insertSavingsAccounts(backup.savingsAccounts)
            dao.insertSavingsDeposits(backup.savingsDeposits)
        }
        AppDatabase.ensureDefaultCategories(db)
    }

    /** Copia diaria interna: una por día de la semana, así siempre hay 7 días hacia atrás. */
    suspend fun writeDaily(today: LocalDate = LocalDate.now()) =
        writeAutomatic("dia_${today.dayOfWeek.value}")

    private suspend fun writeAutomatic(name: String) = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "auto_backups").apply { mkdirs() }
        File(dir, "$name.json").writeText(BackupCodec.encode(snapshot()))
    }
}
