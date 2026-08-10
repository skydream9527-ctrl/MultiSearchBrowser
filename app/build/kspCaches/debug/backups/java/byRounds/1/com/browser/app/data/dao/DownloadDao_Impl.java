package com.browser.app.data.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.browser.app.data.entity.DownloadEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Long;
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
public final class DownloadDao_Impl implements DownloadDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<DownloadEntity> __insertionAdapterOfDownloadEntity;

  private final EntityDeletionOrUpdateAdapter<DownloadEntity> __updateAdapterOfDownloadEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteById;

  private final SharedSQLiteStatement __preparedStmtOfDeleteByDownloadId;

  public DownloadDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfDownloadEntity = new EntityInsertionAdapter<DownloadEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `downloads` (`id`,`downloadId`,`title`,`url`,`mimetype`,`localUri`,`status`,`totalBytes`,`downloadedBytes`,`timestamp`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final DownloadEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getDownloadId());
        statement.bindString(3, entity.getTitle());
        statement.bindString(4, entity.getUrl());
        if (entity.getMimetype() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getMimetype());
        }
        if (entity.getLocalUri() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getLocalUri());
        }
        statement.bindLong(7, entity.getStatus());
        statement.bindLong(8, entity.getTotalBytes());
        statement.bindLong(9, entity.getDownloadedBytes());
        statement.bindLong(10, entity.getTimestamp());
      }
    };
    this.__updateAdapterOfDownloadEntity = new EntityDeletionOrUpdateAdapter<DownloadEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `downloads` SET `id` = ?,`downloadId` = ?,`title` = ?,`url` = ?,`mimetype` = ?,`localUri` = ?,`status` = ?,`totalBytes` = ?,`downloadedBytes` = ?,`timestamp` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final DownloadEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getDownloadId());
        statement.bindString(3, entity.getTitle());
        statement.bindString(4, entity.getUrl());
        if (entity.getMimetype() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getMimetype());
        }
        if (entity.getLocalUri() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getLocalUri());
        }
        statement.bindLong(7, entity.getStatus());
        statement.bindLong(8, entity.getTotalBytes());
        statement.bindLong(9, entity.getDownloadedBytes());
        statement.bindLong(10, entity.getTimestamp());
        statement.bindLong(11, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM downloads WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteByDownloadId = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM downloads WHERE downloadId = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final DownloadEntity download,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfDownloadEntity.insertAndReturnId(download);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final DownloadEntity download,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfDownloadEntity.handle(download);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteById(final long id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteById.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteById.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteByDownloadId(final long downloadId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteByDownloadId.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, downloadId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteByDownloadId.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<DownloadEntity>> getAllDownloads() {
    final String _sql = "SELECT * FROM downloads ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"downloads"}, new Callable<List<DownloadEntity>>() {
      @Override
      @NonNull
      public List<DownloadEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDownloadId = CursorUtil.getColumnIndexOrThrow(_cursor, "downloadId");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "url");
          final int _cursorIndexOfMimetype = CursorUtil.getColumnIndexOrThrow(_cursor, "mimetype");
          final int _cursorIndexOfLocalUri = CursorUtil.getColumnIndexOrThrow(_cursor, "localUri");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfTotalBytes = CursorUtil.getColumnIndexOrThrow(_cursor, "totalBytes");
          final int _cursorIndexOfDownloadedBytes = CursorUtil.getColumnIndexOrThrow(_cursor, "downloadedBytes");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final List<DownloadEntity> _result = new ArrayList<DownloadEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DownloadEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpDownloadId;
            _tmpDownloadId = _cursor.getLong(_cursorIndexOfDownloadId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpUrl;
            _tmpUrl = _cursor.getString(_cursorIndexOfUrl);
            final String _tmpMimetype;
            if (_cursor.isNull(_cursorIndexOfMimetype)) {
              _tmpMimetype = null;
            } else {
              _tmpMimetype = _cursor.getString(_cursorIndexOfMimetype);
            }
            final String _tmpLocalUri;
            if (_cursor.isNull(_cursorIndexOfLocalUri)) {
              _tmpLocalUri = null;
            } else {
              _tmpLocalUri = _cursor.getString(_cursorIndexOfLocalUri);
            }
            final int _tmpStatus;
            _tmpStatus = _cursor.getInt(_cursorIndexOfStatus);
            final long _tmpTotalBytes;
            _tmpTotalBytes = _cursor.getLong(_cursorIndexOfTotalBytes);
            final long _tmpDownloadedBytes;
            _tmpDownloadedBytes = _cursor.getLong(_cursorIndexOfDownloadedBytes);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            _item = new DownloadEntity(_tmpId,_tmpDownloadId,_tmpTitle,_tmpUrl,_tmpMimetype,_tmpLocalUri,_tmpStatus,_tmpTotalBytes,_tmpDownloadedBytes,_tmpTimestamp);
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
  public Object getByDownloadId(final long downloadId,
      final Continuation<? super DownloadEntity> $completion) {
    final String _sql = "SELECT * FROM downloads WHERE downloadId = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, downloadId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<DownloadEntity>() {
      @Override
      @Nullable
      public DownloadEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDownloadId = CursorUtil.getColumnIndexOrThrow(_cursor, "downloadId");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "url");
          final int _cursorIndexOfMimetype = CursorUtil.getColumnIndexOrThrow(_cursor, "mimetype");
          final int _cursorIndexOfLocalUri = CursorUtil.getColumnIndexOrThrow(_cursor, "localUri");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfTotalBytes = CursorUtil.getColumnIndexOrThrow(_cursor, "totalBytes");
          final int _cursorIndexOfDownloadedBytes = CursorUtil.getColumnIndexOrThrow(_cursor, "downloadedBytes");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final DownloadEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpDownloadId;
            _tmpDownloadId = _cursor.getLong(_cursorIndexOfDownloadId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpUrl;
            _tmpUrl = _cursor.getString(_cursorIndexOfUrl);
            final String _tmpMimetype;
            if (_cursor.isNull(_cursorIndexOfMimetype)) {
              _tmpMimetype = null;
            } else {
              _tmpMimetype = _cursor.getString(_cursorIndexOfMimetype);
            }
            final String _tmpLocalUri;
            if (_cursor.isNull(_cursorIndexOfLocalUri)) {
              _tmpLocalUri = null;
            } else {
              _tmpLocalUri = _cursor.getString(_cursorIndexOfLocalUri);
            }
            final int _tmpStatus;
            _tmpStatus = _cursor.getInt(_cursorIndexOfStatus);
            final long _tmpTotalBytes;
            _tmpTotalBytes = _cursor.getLong(_cursorIndexOfTotalBytes);
            final long _tmpDownloadedBytes;
            _tmpDownloadedBytes = _cursor.getLong(_cursorIndexOfDownloadedBytes);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            _result = new DownloadEntity(_tmpId,_tmpDownloadId,_tmpTitle,_tmpUrl,_tmpMimetype,_tmpLocalUri,_tmpStatus,_tmpTotalBytes,_tmpDownloadedBytes,_tmpTimestamp);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
