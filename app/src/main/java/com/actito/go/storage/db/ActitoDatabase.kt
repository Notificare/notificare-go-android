package com.actito.go.storage.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.actito.go.storage.db.converters.DateConverter
import com.actito.go.storage.db.dao.CartEntriesDao
import com.actito.go.storage.db.dao.ProductsDao
import com.actito.go.storage.db.entities.CartEntryEntity
import com.actito.go.storage.db.entities.ProductEntity

@Database(
    entities = [
        CartEntryEntity::class,
        ProductEntity::class,
    ],
    version = 3
)
@TypeConverters(
    DateConverter::class,
)
abstract class ActitoDatabase : RoomDatabase() {
    abstract fun cartEntries(): CartEntriesDao

    abstract fun products(): ProductsDao
}
