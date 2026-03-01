package com.whoistalking.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RemoteKeyDao {
    @Query("SELECT * FROM remote_keys WHERE sessionId = :sessionId")
    suspend fun get(sessionId: String): RemoteKeyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: RemoteKeyEntity)

    @Query("DELETE FROM remote_keys WHERE sessionId = :sessionId")
    suspend fun clear(sessionId: String)
}
