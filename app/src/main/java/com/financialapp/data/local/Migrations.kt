package com.financialapp.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Solo agrega columnas, tablas y categorías: ningún dato existente se modifica ni se borra. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `transactions` ADD COLUMN `accountId` TEXT")
        db.execSQL("ALTER TABLE `transactions` ADD COLUMN `toAccountId` TEXT")
        db.execSQL("ALTER TABLE `transactions` ADD COLUMN `personName` TEXT")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_accountId` ON `transactions` (`accountId`)")

        db.execSQL("CREATE TABLE IF NOT EXISTS `accounts` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, `openingBalance` REAL NOT NULL, `openingDate` INTEGER NOT NULL, `captureKeyword` TEXT, `archived` INTEGER NOT NULL, `lastReconciledAt` INTEGER, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `budgets` (`categoryId` TEXT NOT NULL, `monthlyLimit` REAL NOT NULL, PRIMARY KEY(`categoryId`), FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        db.execSQL("CREATE TABLE IF NOT EXISTS `recurring_bills` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `amount` REAL NOT NULL, `dayOfMonth` INTEGER NOT NULL, `categoryId` TEXT NOT NULL, `accountId` TEXT, `lastPaidMonth` TEXT, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_bills_categoryId` ON `recurring_bills` (`categoryId`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `goals` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `targetAmount` REAL NOT NULL, `savedAmount` REAL NOT NULL, `targetDate` INTEGER, `priority` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `pending_movements` (`id` TEXT NOT NULL, `sourceLabel` TEXT NOT NULL, `rawText` TEXT NOT NULL, `amount` REAL, `type` TEXT, `merchant` TEXT, `suggestedCategoryName` TEXT NOT NULL, `postedAt` INTEGER NOT NULL, `dedupeKey` TEXT NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_pending_movements_dedupeKey` ON `pending_movements` (`dedupeKey`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `ai_suggestions` (`id` TEXT NOT NULL, `topic` TEXT NOT NULL, `question` TEXT NOT NULL, `content` TEXT NOT NULL, `sources` TEXT NOT NULL, `providerModel` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")

        db.execSQL("INSERT OR IGNORE INTO `categories` (`id`, `name`, `iconName`, `colorHex`, `type`, `displayOrder`) VALUES ('cat_comida', 'Comida', 'restaurant', '#FF5722', 'EXPENSE', 7)")
        db.execSQL("INSERT OR IGNORE INTO `categories` (`id`, `name`, `iconName`, `colorHex`, `type`, `displayOrder`) VALUES ('cat_sys_transfer', 'Transferencia', 'swap', '#58A6FF', 'SYSTEM', 0)")
        db.execSQL("INSERT OR IGNORE INTO `categories` (`id`, `name`, `iconName`, `colorHex`, `type`, `displayOrder`) VALUES ('cat_sys_adjustment', 'Ajuste de saldo', 'tune', '#8B949E', 'SYSTEM', 0)")
        db.execSQL("INSERT OR IGNORE INTO `categories` (`id`, `name`, `iconName`, `colorHex`, `type`, `displayOrder`) VALUES ('cat_sys_loan', 'Préstamo', 'handshake', '#F0883E', 'SYSTEM', 0)")
        db.execSQL("INSERT OR IGNORE INTO `categories` (`id`, `name`, `iconName`, `colorHex`, `type`, `displayOrder`) VALUES ('cat_sys_savings', 'Ahorro', 'savings', '#BC8CFF', 'SYSTEM', 0)")
    }
}
