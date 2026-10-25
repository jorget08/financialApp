package com.financialapp.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.financialapp.data.local.entities.SavingsAccountEntity;
import com.financialapp.data.local.entities.SavingsDepositEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class SavingsDao_Impl implements SavingsDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<SavingsAccountEntity> __insertionAdapterOfSavingsAccountEntity;

  private final EntityInsertionAdapter<SavingsDepositEntity> __insertionAdapterOfSavingsDepositEntity;

  private final EntityDeletionOrUpdateAdapter<SavingsDepositEntity> __deletionAdapterOfSavingsDepositEntity;

  private final EntityDeletionOrUpdateAdapter<SavingsAccountEntity> __updateAdapterOfSavingsAccountEntity;

  public SavingsDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfSavingsAccountEntity = new EntityInsertionAdapter<SavingsAccountEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `savings_accounts` (`id`,`name`,`bankName`,`teaRate`,`currency`,`initialBalance`,`plannedMonthlyDeposit`,`startDate`,`createdAt`) VALUES (?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SavingsAccountEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getBankName());
        statement.bindDouble(4, entity.getTeaRate());
        statement.bindString(5, entity.getCurrency());
        statement.bindDouble(6, entity.getInitialBalance());
        statement.bindDouble(7, entity.getPlannedMonthlyDeposit());
        statement.bindLong(8, entity.getStartDate());
        statement.bindLong(9, entity.getCreatedAt());
      }
    };
    this.__insertionAdapterOfSavingsDepositEntity = new EntityInsertionAdapter<SavingsDepositEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `savings_deposits` (`id`,`accountId`,`amount`,`depositDate`,`isSimulated`,`note`) VALUES (?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SavingsDepositEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getAccountId());
        statement.bindDouble(3, entity.getAmount());
        statement.bindLong(4, entity.getDepositDate());
        final int _tmp = entity.isSimulated() ? 1 : 0;
        statement.bindLong(5, _tmp);
        if (entity.getNote() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getNote());
        }
      }
    };
    this.__deletionAdapterOfSavingsDepositEntity = new EntityDeletionOrUpdateAdapter<SavingsDepositEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `savings_deposits` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SavingsDepositEntity entity) {
        statement.bindString(1, entity.getId());
      }
    };
    this.__updateAdapterOfSavingsAccountEntity = new EntityDeletionOrUpdateAdapter<SavingsAccountEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `savings_accounts` SET `id` = ?,`name` = ?,`bankName` = ?,`teaRate` = ?,`currency` = ?,`initialBalance` = ?,`plannedMonthlyDeposit` = ?,`startDate` = ?,`createdAt` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SavingsAccountEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getBankName());
        statement.bindDouble(4, entity.getTeaRate());
        statement.bindString(5, entity.getCurrency());
        statement.bindDouble(6, entity.getInitialBalance());
        statement.bindDouble(7, entity.getPlannedMonthlyDeposit());
        statement.bindLong(8, entity.getStartDate());
        statement.bindLong(9, entity.getCreatedAt());
        statement.bindString(10, entity.getId());
      }
    };
  }

  @Override
  public Object insertAccount(final SavingsAccountEntity account,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfSavingsAccountEntity.insert(account);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertDeposit(final SavingsDepositEntity deposit,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfSavingsDepositEntity.insert(deposit);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteDeposit(final SavingsDepositEntity deposit,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfSavingsDepositEntity.handle(deposit);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateAccount(final SavingsAccountEntity account,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfSavingsAccountEntity.handle(account);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<SavingsAccountEntity>> getAllAccounts() {
    final String _sql = "SELECT * FROM savings_accounts ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"savings_accounts"}, new Callable<List<SavingsAccountEntity>>() {
      @Override
      @NonNull
      public List<SavingsAccountEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfBankName = CursorUtil.getColumnIndexOrThrow(_cursor, "bankName");
          final int _cursorIndexOfTeaRate = CursorUtil.getColumnIndexOrThrow(_cursor, "teaRate");
          final int _cursorIndexOfCurrency = CursorUtil.getColumnIndexOrThrow(_cursor, "currency");
          final int _cursorIndexOfInitialBalance = CursorUtil.getColumnIndexOrThrow(_cursor, "initialBalance");
          final int _cursorIndexOfPlannedMonthlyDeposit = CursorUtil.getColumnIndexOrThrow(_cursor, "plannedMonthlyDeposit");
          final int _cursorIndexOfStartDate = CursorUtil.getColumnIndexOrThrow(_cursor, "startDate");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<SavingsAccountEntity> _result = new ArrayList<SavingsAccountEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SavingsAccountEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpBankName;
            _tmpBankName = _cursor.getString(_cursorIndexOfBankName);
            final double _tmpTeaRate;
            _tmpTeaRate = _cursor.getDouble(_cursorIndexOfTeaRate);
            final String _tmpCurrency;
            _tmpCurrency = _cursor.getString(_cursorIndexOfCurrency);
            final double _tmpInitialBalance;
            _tmpInitialBalance = _cursor.getDouble(_cursorIndexOfInitialBalance);
            final double _tmpPlannedMonthlyDeposit;
            _tmpPlannedMonthlyDeposit = _cursor.getDouble(_cursorIndexOfPlannedMonthlyDeposit);
            final long _tmpStartDate;
            _tmpStartDate = _cursor.getLong(_cursorIndexOfStartDate);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new SavingsAccountEntity(_tmpId,_tmpName,_tmpBankName,_tmpTeaRate,_tmpCurrency,_tmpInitialBalance,_tmpPlannedMonthlyDeposit,_tmpStartDate,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getPrimaryAccount(final Continuation<? super SavingsAccountEntity> $completion) {
    final String _sql = "SELECT * FROM savings_accounts LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<SavingsAccountEntity>() {
      @Override
      @Nullable
      public SavingsAccountEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfBankName = CursorUtil.getColumnIndexOrThrow(_cursor, "bankName");
          final int _cursorIndexOfTeaRate = CursorUtil.getColumnIndexOrThrow(_cursor, "teaRate");
          final int _cursorIndexOfCurrency = CursorUtil.getColumnIndexOrThrow(_cursor, "currency");
          final int _cursorIndexOfInitialBalance = CursorUtil.getColumnIndexOrThrow(_cursor, "initialBalance");
          final int _cursorIndexOfPlannedMonthlyDeposit = CursorUtil.getColumnIndexOrThrow(_cursor, "plannedMonthlyDeposit");
          final int _cursorIndexOfStartDate = CursorUtil.getColumnIndexOrThrow(_cursor, "startDate");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final SavingsAccountEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpBankName;
            _tmpBankName = _cursor.getString(_cursorIndexOfBankName);
            final double _tmpTeaRate;
            _tmpTeaRate = _cursor.getDouble(_cursorIndexOfTeaRate);
            final String _tmpCurrency;
            _tmpCurrency = _cursor.getString(_cursorIndexOfCurrency);
            final double _tmpInitialBalance;
            _tmpInitialBalance = _cursor.getDouble(_cursorIndexOfInitialBalance);
            final double _tmpPlannedMonthlyDeposit;
            _tmpPlannedMonthlyDeposit = _cursor.getDouble(_cursorIndexOfPlannedMonthlyDeposit);
            final long _tmpStartDate;
            _tmpStartDate = _cursor.getLong(_cursorIndexOfStartDate);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _result = new SavingsAccountEntity(_tmpId,_tmpName,_tmpBankName,_tmpTeaRate,_tmpCurrency,_tmpInitialBalance,_tmpPlannedMonthlyDeposit,_tmpStartDate,_tmpCreatedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<SavingsAccountEntity> getPrimaryAccountFlow() {
    final String _sql = "SELECT * FROM savings_accounts LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"savings_accounts"}, new Callable<SavingsAccountEntity>() {
      @Override
      @Nullable
      public SavingsAccountEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfBankName = CursorUtil.getColumnIndexOrThrow(_cursor, "bankName");
          final int _cursorIndexOfTeaRate = CursorUtil.getColumnIndexOrThrow(_cursor, "teaRate");
          final int _cursorIndexOfCurrency = CursorUtil.getColumnIndexOrThrow(_cursor, "currency");
          final int _cursorIndexOfInitialBalance = CursorUtil.getColumnIndexOrThrow(_cursor, "initialBalance");
          final int _cursorIndexOfPlannedMonthlyDeposit = CursorUtil.getColumnIndexOrThrow(_cursor, "plannedMonthlyDeposit");
          final int _cursorIndexOfStartDate = CursorUtil.getColumnIndexOrThrow(_cursor, "startDate");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final SavingsAccountEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpBankName;
            _tmpBankName = _cursor.getString(_cursorIndexOfBankName);
            final double _tmpTeaRate;
            _tmpTeaRate = _cursor.getDouble(_cursorIndexOfTeaRate);
            final String _tmpCurrency;
            _tmpCurrency = _cursor.getString(_cursorIndexOfCurrency);
            final double _tmpInitialBalance;
            _tmpInitialBalance = _cursor.getDouble(_cursorIndexOfInitialBalance);
            final double _tmpPlannedMonthlyDeposit;
            _tmpPlannedMonthlyDeposit = _cursor.getDouble(_cursorIndexOfPlannedMonthlyDeposit);
            final long _tmpStartDate;
            _tmpStartDate = _cursor.getLong(_cursorIndexOfStartDate);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _result = new SavingsAccountEntity(_tmpId,_tmpName,_tmpBankName,_tmpTeaRate,_tmpCurrency,_tmpInitialBalance,_tmpPlannedMonthlyDeposit,_tmpStartDate,_tmpCreatedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<SavingsDepositEntity>> getDepositsForAccount(final String accountId) {
    final String _sql = "SELECT * FROM savings_deposits WHERE accountId = ? ORDER BY depositDate ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, accountId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"savings_deposits"}, new Callable<List<SavingsDepositEntity>>() {
      @Override
      @NonNull
      public List<SavingsDepositEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
          final int _cursorIndexOfDepositDate = CursorUtil.getColumnIndexOrThrow(_cursor, "depositDate");
          final int _cursorIndexOfIsSimulated = CursorUtil.getColumnIndexOrThrow(_cursor, "isSimulated");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final List<SavingsDepositEntity> _result = new ArrayList<SavingsDepositEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SavingsDepositEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpAccountId;
            _tmpAccountId = _cursor.getString(_cursorIndexOfAccountId);
            final double _tmpAmount;
            _tmpAmount = _cursor.getDouble(_cursorIndexOfAmount);
            final long _tmpDepositDate;
            _tmpDepositDate = _cursor.getLong(_cursorIndexOfDepositDate);
            final boolean _tmpIsSimulated;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsSimulated);
            _tmpIsSimulated = _tmp != 0;
            final String _tmpNote;
            if (_cursor.isNull(_cursorIndexOfNote)) {
              _tmpNote = null;
            } else {
              _tmpNote = _cursor.getString(_cursorIndexOfNote);
            }
            _item = new SavingsDepositEntity(_tmpId,_tmpAccountId,_tmpAmount,_tmpDepositDate,_tmpIsSimulated,_tmpNote);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getDepositsListForAccount(final String accountId,
      final Continuation<? super List<SavingsDepositEntity>> $completion) {
    final String _sql = "SELECT * FROM savings_deposits WHERE accountId = ? ORDER BY depositDate ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, accountId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<SavingsDepositEntity>>() {
      @Override
      @NonNull
      public List<SavingsDepositEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "amount");
          final int _cursorIndexOfDepositDate = CursorUtil.getColumnIndexOrThrow(_cursor, "depositDate");
          final int _cursorIndexOfIsSimulated = CursorUtil.getColumnIndexOrThrow(_cursor, "isSimulated");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final List<SavingsDepositEntity> _result = new ArrayList<SavingsDepositEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SavingsDepositEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpAccountId;
            _tmpAccountId = _cursor.getString(_cursorIndexOfAccountId);
            final double _tmpAmount;
            _tmpAmount = _cursor.getDouble(_cursorIndexOfAmount);
            final long _tmpDepositDate;
            _tmpDepositDate = _cursor.getLong(_cursorIndexOfDepositDate);
            final boolean _tmpIsSimulated;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsSimulated);
            _tmpIsSimulated = _tmp != 0;
            final String _tmpNote;
            if (_cursor.isNull(_cursorIndexOfNote)) {
              _tmpNote = null;
            } else {
              _tmpNote = _cursor.getString(_cursorIndexOfNote);
            }
            _item = new SavingsDepositEntity(_tmpId,_tmpAccountId,_tmpAmount,_tmpDepositDate,_tmpIsSimulated,_tmpNote);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
