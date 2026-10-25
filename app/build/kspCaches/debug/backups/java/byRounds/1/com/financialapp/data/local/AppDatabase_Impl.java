package com.financialapp.data.local;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.financialapp.data.local.dao.CategoryDao;
import com.financialapp.data.local.dao.CategoryDao_Impl;
import com.financialapp.data.local.dao.SavingsDao;
import com.financialapp.data.local.dao.SavingsDao_Impl;
import com.financialapp.data.local.dao.TransactionDao;
import com.financialapp.data.local.dao.TransactionDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile TransactionDao _transactionDao;

  private volatile CategoryDao _categoryDao;

  private volatile SavingsDao _savingsDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(2) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `categories` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `iconName` TEXT NOT NULL, `colorHex` TEXT NOT NULL, `type` TEXT NOT NULL, `displayOrder` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `transactions` (`id` TEXT NOT NULL, `amount` REAL NOT NULL, `type` TEXT NOT NULL, `categoryId` TEXT NOT NULL, `note` TEXT, `date` INTEGER NOT NULL, `source` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_date` ON `transactions` (`date`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_categoryId` ON `transactions` (`categoryId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `savings_accounts` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `bankName` TEXT NOT NULL, `teaRate` REAL NOT NULL, `currency` TEXT NOT NULL, `initialBalance` REAL NOT NULL, `plannedMonthlyDeposit` REAL NOT NULL, `startDate` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `savings_deposits` (`id` TEXT NOT NULL, `accountId` TEXT NOT NULL, `amount` REAL NOT NULL, `depositDate` INTEGER NOT NULL, `isSimulated` INTEGER NOT NULL, `note` TEXT, PRIMARY KEY(`id`), FOREIGN KEY(`accountId`) REFERENCES `savings_accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_savings_deposits_accountId` ON `savings_deposits` (`accountId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_savings_deposits_depositDate` ON `savings_deposits` (`depositDate`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '3a877921b01210221036ad193be1e02e')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `categories`");
        db.execSQL("DROP TABLE IF EXISTS `transactions`");
        db.execSQL("DROP TABLE IF EXISTS `savings_accounts`");
        db.execSQL("DROP TABLE IF EXISTS `savings_deposits`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        db.execSQL("PRAGMA foreign_keys = ON");
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsCategories = new HashMap<String, TableInfo.Column>(6);
        _columnsCategories.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCategories.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCategories.put("iconName", new TableInfo.Column("iconName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCategories.put("colorHex", new TableInfo.Column("colorHex", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCategories.put("type", new TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCategories.put("displayOrder", new TableInfo.Column("displayOrder", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCategories = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCategories = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoCategories = new TableInfo("categories", _columnsCategories, _foreignKeysCategories, _indicesCategories);
        final TableInfo _existingCategories = TableInfo.read(db, "categories");
        if (!_infoCategories.equals(_existingCategories)) {
          return new RoomOpenHelper.ValidationResult(false, "categories(com.financialapp.data.local.entities.CategoryEntity).\n"
                  + " Expected:\n" + _infoCategories + "\n"
                  + " Found:\n" + _existingCategories);
        }
        final HashMap<String, TableInfo.Column> _columnsTransactions = new HashMap<String, TableInfo.Column>(8);
        _columnsTransactions.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("amount", new TableInfo.Column("amount", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("type", new TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("categoryId", new TableInfo.Column("categoryId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("note", new TableInfo.Column("note", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("date", new TableInfo.Column("date", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("source", new TableInfo.Column("source", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysTransactions = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysTransactions.add(new TableInfo.ForeignKey("categories", "RESTRICT", "NO ACTION", Arrays.asList("categoryId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesTransactions = new HashSet<TableInfo.Index>(2);
        _indicesTransactions.add(new TableInfo.Index("index_transactions_date", false, Arrays.asList("date"), Arrays.asList("ASC")));
        _indicesTransactions.add(new TableInfo.Index("index_transactions_categoryId", false, Arrays.asList("categoryId"), Arrays.asList("ASC")));
        final TableInfo _infoTransactions = new TableInfo("transactions", _columnsTransactions, _foreignKeysTransactions, _indicesTransactions);
        final TableInfo _existingTransactions = TableInfo.read(db, "transactions");
        if (!_infoTransactions.equals(_existingTransactions)) {
          return new RoomOpenHelper.ValidationResult(false, "transactions(com.financialapp.data.local.entities.TransactionEntity).\n"
                  + " Expected:\n" + _infoTransactions + "\n"
                  + " Found:\n" + _existingTransactions);
        }
        final HashMap<String, TableInfo.Column> _columnsSavingsAccounts = new HashMap<String, TableInfo.Column>(9);
        _columnsSavingsAccounts.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSavingsAccounts.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSavingsAccounts.put("bankName", new TableInfo.Column("bankName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSavingsAccounts.put("teaRate", new TableInfo.Column("teaRate", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSavingsAccounts.put("currency", new TableInfo.Column("currency", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSavingsAccounts.put("initialBalance", new TableInfo.Column("initialBalance", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSavingsAccounts.put("plannedMonthlyDeposit", new TableInfo.Column("plannedMonthlyDeposit", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSavingsAccounts.put("startDate", new TableInfo.Column("startDate", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSavingsAccounts.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSavingsAccounts = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesSavingsAccounts = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoSavingsAccounts = new TableInfo("savings_accounts", _columnsSavingsAccounts, _foreignKeysSavingsAccounts, _indicesSavingsAccounts);
        final TableInfo _existingSavingsAccounts = TableInfo.read(db, "savings_accounts");
        if (!_infoSavingsAccounts.equals(_existingSavingsAccounts)) {
          return new RoomOpenHelper.ValidationResult(false, "savings_accounts(com.financialapp.data.local.entities.SavingsAccountEntity).\n"
                  + " Expected:\n" + _infoSavingsAccounts + "\n"
                  + " Found:\n" + _existingSavingsAccounts);
        }
        final HashMap<String, TableInfo.Column> _columnsSavingsDeposits = new HashMap<String, TableInfo.Column>(6);
        _columnsSavingsDeposits.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSavingsDeposits.put("accountId", new TableInfo.Column("accountId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSavingsDeposits.put("amount", new TableInfo.Column("amount", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSavingsDeposits.put("depositDate", new TableInfo.Column("depositDate", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSavingsDeposits.put("isSimulated", new TableInfo.Column("isSimulated", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSavingsDeposits.put("note", new TableInfo.Column("note", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSavingsDeposits = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysSavingsDeposits.add(new TableInfo.ForeignKey("savings_accounts", "CASCADE", "NO ACTION", Arrays.asList("accountId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesSavingsDeposits = new HashSet<TableInfo.Index>(2);
        _indicesSavingsDeposits.add(new TableInfo.Index("index_savings_deposits_accountId", false, Arrays.asList("accountId"), Arrays.asList("ASC")));
        _indicesSavingsDeposits.add(new TableInfo.Index("index_savings_deposits_depositDate", false, Arrays.asList("depositDate"), Arrays.asList("ASC")));
        final TableInfo _infoSavingsDeposits = new TableInfo("savings_deposits", _columnsSavingsDeposits, _foreignKeysSavingsDeposits, _indicesSavingsDeposits);
        final TableInfo _existingSavingsDeposits = TableInfo.read(db, "savings_deposits");
        if (!_infoSavingsDeposits.equals(_existingSavingsDeposits)) {
          return new RoomOpenHelper.ValidationResult(false, "savings_deposits(com.financialapp.data.local.entities.SavingsDepositEntity).\n"
                  + " Expected:\n" + _infoSavingsDeposits + "\n"
                  + " Found:\n" + _existingSavingsDeposits);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "3a877921b01210221036ad193be1e02e", "1d7d89e9c540ca2907e6b33099edecac");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "categories","transactions","savings_accounts","savings_deposits");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    final boolean _supportsDeferForeignKeys = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
    try {
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = FALSE");
      }
      super.beginTransaction();
      if (_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA defer_foreign_keys = TRUE");
      }
      _db.execSQL("DELETE FROM `transactions`");
      _db.execSQL("DELETE FROM `categories`");
      _db.execSQL("DELETE FROM `savings_accounts`");
      _db.execSQL("DELETE FROM `savings_deposits`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = TRUE");
      }
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(TransactionDao.class, TransactionDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(CategoryDao.class, CategoryDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(SavingsDao.class, SavingsDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public TransactionDao transactionDao() {
    if (_transactionDao != null) {
      return _transactionDao;
    } else {
      synchronized(this) {
        if(_transactionDao == null) {
          _transactionDao = new TransactionDao_Impl(this);
        }
        return _transactionDao;
      }
    }
  }

  @Override
  public CategoryDao categoryDao() {
    if (_categoryDao != null) {
      return _categoryDao;
    } else {
      synchronized(this) {
        if(_categoryDao == null) {
          _categoryDao = new CategoryDao_Impl(this);
        }
        return _categoryDao;
      }
    }
  }

  @Override
  public SavingsDao savingsDao() {
    if (_savingsDao != null) {
      return _savingsDao;
    } else {
      synchronized(this) {
        if(_savingsDao == null) {
          _savingsDao = new SavingsDao_Impl(this);
        }
        return _savingsDao;
      }
    }
  }
}
