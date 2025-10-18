package io.mrarm.irc.storage.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.RawQuery;
import androidx.sqlite.db.SimpleSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteQuery;

import java.util.List;

@Dao
public interface MessageLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(MessageLogEntity entity);

    @RawQuery(observedEntities = MessageLogEntity.class)
    List<MessageLogEntity> query(SupportSQLiteQuery query);

    default List<MessageLogEntity> query(SimpleSQLiteQuery query) {
        return query((SupportSQLiteQuery) query);
    }

    @Query("DELETE FROM messages")
    void clear();
}
