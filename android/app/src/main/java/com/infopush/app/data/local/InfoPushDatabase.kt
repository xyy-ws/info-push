package com.infopush.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.infopush.app.data.local.dao.FavoriteDao
import com.infopush.app.data.local.dao.MessageDao
import com.infopush.app.data.local.dao.PreferenceDao
import com.infopush.app.data.local.dao.SourceDao
import com.infopush.app.data.local.entity.FavoriteEntity
import com.infopush.app.data.local.entity.MessageEntity
import com.infopush.app.data.local.entity.PreferenceEntity
import com.infopush.app.data.local.entity.SourceEntity
import com.infopush.app.data.local.entity.SourceItemEntity

@Database(
    entities = [
        SourceEntity::class,
        SourceItemEntity::class,
        FavoriteEntity::class,
        MessageEntity::class,
        PreferenceEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class InfoPushDatabase : RoomDatabase() {
    abstract fun sourceDao(): SourceDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun messageDao(): MessageDao
    abstract fun preferenceDao(): PreferenceDao
}
