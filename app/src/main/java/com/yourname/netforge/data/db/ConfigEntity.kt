package com.yourname.netforge.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "configs")
data class ConfigEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "version")
    val version: Int = 1,

    @ColumnInfo(name = "host")
    val host: String,

    @ColumnInfo(name = "port")
    val port: Int,

    @ColumnInfo(name = "mode")
    val mode: String,

    /**
     * Complete config INI or sensitive payload fields encrypted on device using AES-256-GCM via Android Keystore
     */
    @ColumnInfo(name = "encrypted_data")
    val encryptedData: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = false
)
