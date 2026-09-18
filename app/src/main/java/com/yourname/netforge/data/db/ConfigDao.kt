package com.yourname.netforge.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ConfigDao {

    @Query("SELECT * FROM configs ORDER BY created_at DESC")
    fun getAllConfigs(): Flow<List<ConfigEntity>>

    @Query("SELECT * FROM configs WHERE id = :id LIMIT 1")
    suspend fun getConfigById(id: Long): ConfigEntity?

    @Query("SELECT * FROM configs WHERE is_active = 1 LIMIT 1")
    suspend fun getActiveConfig(): ConfigEntity?

    @Query("SELECT * FROM configs WHERE is_active = 1 LIMIT 1")
    fun getActiveConfigFlow(): Flow<ConfigEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: ConfigEntity): Long

    @Update
    suspend fun updateConfig(config: ConfigEntity)

    @Query("UPDATE configs SET is_active = 0")
    suspend fun clearAllActive()

    @Query("UPDATE configs SET is_active = 1 WHERE id = :id")
    suspend fun setActiveConfig(id: Long)

    @Query("UPDATE configs SET name = :newName WHERE id = :id")
    suspend fun renameConfig(id: Long, newName: String)

    @Delete
    suspend fun deleteConfig(config: ConfigEntity)

    @Query("DELETE FROM configs WHERE id = :id")
    suspend fun deleteConfigById(id: Long)
}
