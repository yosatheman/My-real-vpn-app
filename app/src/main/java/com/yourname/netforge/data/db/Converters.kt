package com.yourname.netforge.data.db

import androidx.room.TypeConverter
import com.yourname.netforge.domain.model.TunnelMode

class Converters {
    @TypeConverter
    fun fromTunnelMode(mode: TunnelMode): String = mode.id

    @TypeConverter
    fun toTunnelMode(value: String): TunnelMode = TunnelMode.fromString(value)
}
